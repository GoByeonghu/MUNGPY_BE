package com.sfz.mungpy.service;

import com.sfz.mungpy.dto.DogMatchDto;
import com.sfz.mungpy.dto.DogSpecificDto;
import com.sfz.mungpy.entity.Dog;
import com.sfz.mungpy.exception.DogNotFoundException;
import com.sfz.mungpy.repository.DogRepository;
import lombok.AllArgsConstructor;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.client.RestClient;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.io.InputStream;
import java.util.*;

@Slf4j
@Service
@RequiredArgsConstructor
public class DogService {
    private final DogRepository dogRepository;

    // 전화번호, 주소, 이름, 대표자, 대표자 번호

    private static final int CANDIDATES = 10;
    private static final int PERSONALITIES = 6;

    @AllArgsConstructor
    private class MatchingPriority {
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
        String dogImage = requestImageAnalyzation(image, matchingPriorities);

        Long dogId = 1L; // 임시

        return dogRepository.findById(dogId)
                .orElseThrow(DogNotFoundException::new)
                .toMatchDto();
    }

    private String requestImageAnalyzation(MultipartFile image, PriorityQueue<MatchingPriority> matchingPriorities) {
        RestClient restClient = RestClient.builder()
                .baseUrl("https://42ec-123-214-153-130.ngrok-free.app/")
                .build();

        Map<String, Object> paramMap = new HashMap<>();
        paramMap.put("image", encodeFileToBase64(image));
        paramMap.put("list", matchingPriorities.stream().map(o -> o.point).toList());

        return restClient.post()
                .uri("/find_similar_dogs")
                .contentType(MediaType.APPLICATION_JSON)
                .body(paramMap)
                .retrieve()
                .toEntity(String.class)
                .getBody();
    }

    private String encodeFileToBase64(MultipartFile file) {
        try (InputStream inputStream = file.getInputStream()) {
            byte[] bytes = inputStream.readAllBytes();
            return Base64.getEncoder().encodeToString(bytes);
        } catch (IOException e) {
            throw new RuntimeException("Failed to encode file to Base64", e);
        }
    }

    @Transactional
    public DogSpecificDto showDog(Long dogId) {
        return dogRepository.findById(dogId)
                .orElseThrow(DogNotFoundException::new)
                .toDogSpecificDto();
    }
}