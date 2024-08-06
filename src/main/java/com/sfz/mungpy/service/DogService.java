package com.sfz.mungpy.service;

import com.sfz.mungpy.dto.DogMatchDto;
import com.sfz.mungpy.dto.DogSpecificDto;
import com.sfz.mungpy.dto.KakaoMapResponse;
import com.sfz.mungpy.entity.Dog;
import com.sfz.mungpy.exception.DogNotFoundException;
import com.sfz.mungpy.exception.ShelterNotFoundException;
import com.sfz.mungpy.repository.DogRepository;
import lombok.AllArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.io.ByteArrayResource;
import org.springframework.http.*;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.LinkedMultiValueMap;
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
    private final String secretKey;
    private final DogRepository dogRepository;

    public DogService(@Value("${kakaoapi.secret-key}") String secretKey, DogRepository dogRepository) {
        this.secretKey = secretKey;
        this.dogRepository = dogRepository;
    }

    // 전화번호, 주소, 이름, 대표자, 대표자 번호

    private static final int CANDIDATES = 10;
    private static final int PERSONALITIES = 6;

    @AllArgsConstructor
    private static class MatchingPriority {
        private Dog dog;
        private int point;
    }

    @Transactional(readOnly = true)
    public DogMatchDto matchDog(List<String> personality, MultipartFile image) {
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

        // TODO: 파이썬 서버와 연결 필요
        String dogImage = requestImageAnalyzation(image, selectList);

        Long dogId = 1L; // 임시

        return dogRepository.findById(dogId)
                .orElseThrow(DogNotFoundException::new)
                .toMatchDto();
    }

    private String requestImageAnalyzation(MultipartFile image, List<String> selectList) {
        RestTemplate restTemplate = new RestTemplate();


        MultiValueMap<String, Object> bodyMap = new LinkedMultiValueMap<>();
        bodyMap.add("image", new MultipartFileResource(image));
        bodyMap.add("list", selectList.toString());

        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.MULTIPART_FORM_DATA);

        HttpEntity<MultiValueMap<String, Object>> requestEntity = new HttpEntity<>(bodyMap, headers);

        String url = "https://74c1-123-214-153-130.ngrok-free.app/find_similar_dogs";
        ResponseEntity<String> response = restTemplate.exchange(url, HttpMethod.POST, requestEntity, String.class);

        return response.getBody();
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

    @Transactional
    public DogSpecificDto showDog(Long dogId) {
        DogSpecificDto dogSpecificDto = dogRepository.findById(dogId)
                .orElseThrow(DogNotFoundException::new)
                .toDogSpecificDto();

        RestTemplate restTemplate = new RestTemplate();

        String url = "https://dapi.kakao.com/v2/local/search/keyword?size=1";
        url += "&query=" + dogSpecificDto.getProtectPlace();
        log.info(url);

        HttpHeaders headers = new HttpHeaders();
        headers.set("Authorization", "KakaoAK " + secretKey);

        HttpEntity<MultiValueMap<String, Object>> requestEntity = new HttpEntity<>(headers);
        KakaoMapResponse.Document kakaoMap = restTemplate.exchange(url, HttpMethod.GET, requestEntity, KakaoMapResponse.class)
                .getBody().getDocuments().get(0);

        if (kakaoMap == null) throw new ShelterNotFoundException();

        log.info(kakaoMap.toString());

        dogSpecificDto.setLongitude(Double.parseDouble(kakaoMap.getX()));
        dogSpecificDto.setLatitude(Double.parseDouble(kakaoMap.getY()));

        return dogSpecificDto;
    }
}