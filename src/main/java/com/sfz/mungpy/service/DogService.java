package com.sfz.mungpy.service;

import com.sfz.mungpy.dto.DogDto;
import com.sfz.mungpy.exception.DogNotFoundException;
import com.sfz.mungpy.repository.DogRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

import java.util.List;

@Service
@RequiredArgsConstructor
public class DogService {
    private final DogRepository dogRepository;

    @Transactional
    public DogDto matchDog(List<String> personality, MultipartFile image) {
        // TODO: 사용자 성향 분석 추가

        // TODO: 사용자 이미지 분석 추가

        return dogRepository.findById(1L)
                .orElseThrow(DogNotFoundException::new)
                .toDto();
    }
}
