package com.sfz.mungpy.service;

import com.sfz.mungpy.dto.DogMatch;
import com.sfz.mungpy.dto.DogSpecific;
import com.sfz.mungpy.dto.KakaoMapResponse;
import com.sfz.mungpy.dto.PythonResponse;
import com.sfz.mungpy.entity.Dog;
import com.sfz.mungpy.exception.DogNotFoundException;
import com.sfz.mungpy.exception.ShelterNotFoundException;
import com.sfz.mungpy.repository.DogRepository;
import lombok.AllArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.ai.chat.messages.Media;
import org.springframework.ai.chat.messages.SystemMessage;
import org.springframework.ai.chat.messages.UserMessage;
import org.springframework.ai.chat.model.ChatResponse;
import org.springframework.ai.chat.prompt.Prompt;
import org.springframework.ai.openai.OpenAiChatModel;
import org.springframework.ai.openai.OpenAiChatOptions;
import org.springframework.ai.openai.api.OpenAiApi;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.io.ByteArrayResource;
import org.springframework.core.io.ClassPathResource;
import org.springframework.http.*;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.LinkedMultiValueMap;
import org.springframework.util.MimeTypeUtils;
import org.springframework.util.MultiValueMap;
import org.springframework.web.client.RestTemplate;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.util.*;


@Slf4j
@Service
public class DogService {
    private final String kakaoKey;
    private final DogRepository dogRepository;
    private final OpenAiChatModel openAiChatModel;

    public DogService(@Value("${kakaoapi.secret-key}") String kakaoKey,
                      DogRepository dogRepository, OpenAiChatModel openAiChatModel) {

        this.kakaoKey = kakaoKey;
        this.dogRepository = dogRepository;
        this.openAiChatModel = openAiChatModel;
    }

    private static final int CANDIDATES = 10;
    private static final int PERSONALITIES = 6;

    @AllArgsConstructor
    private static class MatchingPriority {
        private Dog dog;
        private int point;
    }

    @Transactional
    public DogMatch matchDog(List<String> personality, MultipartFile image) {
        List<Dog> dogList = dogRepository.findAll();

        log.info("dogList: {}, size: {}", dogList.toString(), dogList.size());

        if (dogList.isEmpty()) throw new DogNotFoundException();

        PriorityQueue<MatchingPriority> matchingPriorities = new PriorityQueue<>(Comparator.comparingInt(mp -> -mp.point));
        for (Dog dog : dogList) {
            List<String> dogPersonality = dog.toDogSpecificDto().getPersonality();

            log.info("dogPersonality: {}, size: {}", dog.toDogSpecificDto().getPersonality().toString(), dog.toDogSpecificDto().getPersonality().size());

            int point = 0;
            for (int i = 0; i < PERSONALITIES; i++) {
                if (dogPersonality.get(i).equals(personality.get(i))) {
                    point++;
                }
            }

            matchingPriorities.offer(new MatchingPriority(dog, point));
        }

        List<String> selectList = new ArrayList<>();

        log.info("selectList: {}, size: {}", selectList.toString(), selectList.size());

        while (!matchingPriorities.isEmpty()) {
            MatchingPriority mp = matchingPriorities.poll();

            if (selectList.size() < CANDIDATES) {
                selectList.add(mp.dog.getImage());
            }

            if (selectList.size() == CANDIDATES) {
                while (!matchingPriorities.isEmpty() && mp.point == matchingPriorities.peek().point) {
                    selectList.add(matchingPriorities.poll().dog.getImage());
                }
            }
        }

        String imageName = requestImageModelAnalysis(image, selectList);

        log.info(imageName);

        Dog matchDog = dogRepository.findByImage(imageName)
                .orElseThrow(DogNotFoundException::new);

        log.info(matchDog.toString());

        DogMatch matchDto = matchDog.toMatchDto();
        matchDto.setImage("/images/" + imageName);

        log.info(matchDto.toString());

        Map<String, String> resultMap;
        try {
            resultMap = requestOpenAIAnalysis(matchDog.getPersonality(), personality.toString(), matchDto.getImage());

            String description = resultMap.get("description");
            String matchReason = resultMap.get("matchReason");

            log.info("dc: {}, mr: {}", description, matchReason);

            matchDto.setDescription(description);
            matchDto.setMatchReason(matchReason);
            matchDog.updateAnalysis(description, matchReason);
        } catch (IOException e) {
            throw new RuntimeException(e);
        }

        return matchDto;
    }

    private String requestImageModelAnalysis(MultipartFile image, List<String> selectList) {
        RestTemplate restTemplate = new RestTemplate();

        MultiValueMap<String, Object> bodyMap = new LinkedMultiValueMap<>();
        bodyMap.add("image", new MultipartFileResource(image));
        bodyMap.add("list", selectList.toString());

        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.MULTIPART_FORM_DATA);

        HttpEntity<MultiValueMap<String, Object>> requestEntity = new HttpEntity<>(bodyMap, headers);

        String url = "https://74c1-123-214-153-130.ngrok-free.app/find_similar_dogs";
        ResponseEntity<PythonResponse> response = restTemplate.exchange(
                url,
                HttpMethod.POST,
                requestEntity,
                PythonResponse.class
        );

        PythonResponse responseBody = response.getBody();
        if (responseBody == null) throw new DogNotFoundException();

        log.info(responseBody.getData());

        return responseBody.getData();
    }

    private Map<String, String> requestOpenAIAnalysis(String dogPersonality, String userPersonality, String dogImageName) throws IOException {

        Media dogImage = new Media(MimeTypeUtils.IMAGE_PNG, new ClassPathResource("/static" + dogImageName));

        String order = "너는 유기견 매칭 서비스의 분석 담당관이야." +
                "너의 일은 강아지 이미지를 분석해서 요약하고, 사용자와 강아지의 성향을 비교해서 둘이 어울리는 이유를 생성해주는 거야";
        SystemMessage systemMessage = new SystemMessage(order);

        UserMessage userMessage = new UserMessage(
                "너가 할 일은 두 가지야. 첫 번째, 강아지 사진을 분석해서 분위기나 외형을 고려해서 한 마디로 표현해줘." +
                        "그리고 이 강아지의 성향은 " + dogPersonality + "인데, 이걸 한 마디에 반영해줘. 예를 들면 순둥순둥한 방콕러 같이." +
                        "두 번째, 주인의 성향은 " + userPersonality + "인데, 위의 강아지의 성향과 주인의 성향을 분석해서" +
                        "이 둘이 왜 어울리는지 감성적으로 3줄 요약해줘. 같은 말을 반복하면 안되고 나에게 응답할 때는 {첫 번째 응답 결과}/{두 번째 응답 결과} 형식으로 해줘." +
                        "그리고 사용자를 부를 때는 당신 이라고 해줘. 예를 들면 당신에게 이 아이를 추천한 이유는 처럼",
                List.of(dogImage));

        ChatResponse response = openAiChatModel.call(new Prompt(List.of(systemMessage, userMessage),
                OpenAiChatOptions.builder().withModel(OpenAiApi.ChatModel.GPT_4_O.getValue()).build()));

        String[] result = response.getResult().getOutput().getContent().split("/");

        Map<String, String> resultMap = new HashMap<>();
        resultMap.put("description", result[0].trim());
        resultMap.put("matchReason", result[1].trim());

        return resultMap;
    }

    private static class MultipartFileResource extends ByteArrayResource {
        private final MultipartFile file;

        MultipartFileResource(MultipartFile file) {
            super(toByteArray(file));
            this.file = file;
        }

        @Override
        public String getFilename() {
            return file.getOriginalFilename();
        }

        private static byte[] toByteArray(MultipartFile file) {
            try {
                return file.getBytes();
            } catch (IOException e) {
                throw new RuntimeException("Failed to read bytes from MultipartFile", e);
            }
        }
    }

    @Transactional(readOnly = true)
    public DogSpecific showDog(Long dogId) {
        DogSpecific dogSpecific = dogRepository.findById(dogId)
                .orElseThrow(DogNotFoundException::new)
                .toDogSpecificDto();

        log.info(dogSpecific.toString());

        dogSpecific.setImage("/images/" + dogSpecific.getImage());

        RestTemplate restTemplate = new RestTemplate();

        String url = "https://dapi.kakao.com/v2/local/search/keyword?size=1";
        url += "&query=" + dogSpecific.getProtectPlace();
        log.info(url);

        HttpHeaders headers = new HttpHeaders();
        headers.set("Authorization", "KakaoAK " + kakaoKey);

        HttpEntity<MultiValueMap<String, Object>> requestEntity = new HttpEntity<>(headers);
        KakaoMapResponse.Document kakaoMap = restTemplate.exchange(url, HttpMethod.GET, requestEntity, KakaoMapResponse.class)
                .getBody().getDocuments().get(0);

        if (kakaoMap == null) throw new ShelterNotFoundException();

        dogSpecific.setLongitude(Double.parseDouble(kakaoMap.getX()));
        dogSpecific.setLatitude(Double.parseDouble(kakaoMap.getY()));

        return dogSpecific;
    }
}