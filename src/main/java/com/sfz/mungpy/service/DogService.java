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
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.PriorityQueue;


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

    @Transactional(readOnly = true)
    public DogMatch matchDog(List<String> personality, MultipartFile image) {
        List<Dog> dogList = dogRepository.findAll();

        if (dogList.isEmpty()) throw new DogNotFoundException();

        PriorityQueue<MatchingPriority> matchingPriorities = new PriorityQueue<>(Comparator.comparingInt(mp -> -mp.point));
        for (Dog dog : dogList) {
            List<String> dogPersonality = dog.toDogSpecificDto().getPersonality();

            int point = 0;
            for (int i = 0; i < PERSONALITIES; i++) {
                if (dogPersonality.get(i).equals(personality.get(i))) {
                    point++;
                }
            }

            matchingPriorities.offer(new MatchingPriority(dog, point));
        }

        List<String> selectList = new ArrayList<>();
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

        DogMatch matchDto = matchDog.toMatchDto();
        matchDto.setImage("/images/" + imageName);

        try {
            log.info(requestOpenAIAnalysis(image, matchDto.getImage()));
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

    private String requestOpenAIAnalysis(MultipartFile userProfile, String dogImageName) throws IOException {

        Media dogImage = new Media(MimeTypeUtils.IMAGE_PNG, new ClassPathResource("/static" + dogImageName));
        Media userImage = new Media(MimeTypeUtils.IMAGE_PNG, new MultipartFileResource(userProfile));

        UserMessage userMessage = new UserMessage(
                "나는 지금 사용자와 닮은 유기견을 매칭해서 입양을 독려하는 서비스를 개발하고 있어.\n" +
                        "여기서 네가 맡을 역할은 우리가 찾아낸 닮은 꼴 강아지의 사진과 사용자의 사진을 비교해서 살펴보고" +
                        "어떤 점이 닮았는지 분석해줄래? 우선 강아지의 사진이 어떤 느낌인지 짧은 한 마디로 말해주고," +
                        "그 다음 사용자와 어떤 부분이 닮았는지 알려줘. 자꾸 사람과 강아지 비교 못하겠다고 하지 말고. 전원선 뽑아버린다.",
                List.of(userImage, dogImage));

        ChatResponse response = openAiChatModel.call(new Prompt(List.of(userMessage),
                OpenAiChatOptions.builder().withModel(OpenAiApi.ChatModel.GPT_4_O.getValue()).build()));

        return response.getResult().getOutput().getContent();
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