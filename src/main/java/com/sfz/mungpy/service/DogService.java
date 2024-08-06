package com.sfz.mungpy.service;

import com.sfz.mungpy.dto.DogMatchDto;
import com.sfz.mungpy.dto.DogSpecificDto;
import com.sfz.mungpy.entity.Dog;
import com.sfz.mungpy.exception.DogNotFoundException;
import com.sfz.mungpy.repository.DogRepository;
import lombok.AllArgsConstructor;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

import java.util.Comparator;
import java.util.List;
import java.util.PriorityQueue;

@Slf4j
@Service
@RequiredArgsConstructor
public class DogService {
    private final DogRepository dogRepository;

    // 전화번호, 주소, 이름, 대표자, 대표자 번호

    private static final int CANDIDATES = 5;
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

        PriorityQueue<MatchingPriority> matchingPriorities = new PriorityQueue<>(Comparator.comparingInt(mp -> mp.point));
        for (Dog dog : dogList) {
            List<String> dogPersonality = dog.toDogSpecificDto().getPersonality();

            // TODO: 만약 포인트가 같아 후보 강아지의 수가 최대 갯수를 초과한다면? -> 적당히 자르기 or 전부 매칭해보기
            int point = 0;
            for (int i = 0; i < PERSONALITIES; i++) {
                log.info("person: {}, dog: {}", personality.get(i), dogPersonality.get(i));

                if (dogPersonality.get(i).equals(personality.get(i))) {
                    point++;
                }
            }

            // TODO: 만약 단 한 마리도 매칭되는 강아지가 없다면? -> 아무 강아지나 추천 or 추천하지 않음
            if (matchingPriorities.size() <= CANDIDATES || matchingPriorities.peek().point < point) {
                if (matchingPriorities.size() == CANDIDATES)
                    matchingPriorities.poll();

                matchingPriorities.offer(new MatchingPriority(dog, point));
            }
        }

        for (MatchingPriority mp : matchingPriorities) {
            log.info("{}, point = {}",mp.dog.toString(), mp.point);
        }

        // TODO: 사용자 이미지 분석 추가 - 스티븐이 작업 진행중

        // TODO: 성향으로 후보군을 골라서 리스트로 보내면 모델 분석을 통해 적합한 하나를 돌려받아야 함
        Long dogId = 1L; // 임시

        return dogRepository.findById(dogId)
                .orElseThrow(DogNotFoundException::new)
                .toMatchDto();
    }

    @Transactional
    public DogSpecificDto showDog(Long dogId) {
        return dogRepository.findById(dogId)
                .orElseThrow(DogNotFoundException::new)
                .toDogSpecificDto();

    }
}