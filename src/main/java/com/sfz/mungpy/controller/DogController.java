package com.sfz.mungpy.controller;

import com.sfz.mungpy.dto.DogMatchDto;
import com.sfz.mungpy.dto.DogSpecificDto;
import com.sfz.mungpy.dto.UserInfomation;
import com.sfz.mungpy.exception.DogNotFoundException;
import com.sfz.mungpy.service.DogService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.util.List;

@Slf4j
@RestController
@RequiredArgsConstructor
@RequestMapping("/dog")
public class DogController {
    private final DogService dogService;

    @PostMapping
    public ResponseEntity<?> getDog(@ModelAttribute UserInfomation userInfomation) {
        String jsonMessage = "{\"message\":";

        List<String> personality = userInfomation.getPersonality();
        if (personality == null || personality.isEmpty()) {
            jsonMessage += "\"사용자 성향 데이터가 존재하지 않습니다.\"";
            return ResponseEntity.badRequest().body(jsonMessage);
        }

        if (personality.size() != 6) {
            jsonMessage += "\"사용자 성향 데이터의 갯수는 6개여야 합니다.\"";
            return ResponseEntity.badRequest().body(jsonMessage);
        }

        MultipartFile image = userInfomation.getImage();
        if (image == null || image.isEmpty()) {
            jsonMessage += "\"사용자 이미지가 존재하지 않습니다.\"";
            return ResponseEntity.badRequest().body(jsonMessage);
        }

        DogMatchDto dogMatchDto;
        try {
            dogMatchDto = dogService.matchDog(personality, image);
        } catch (DogNotFoundException e) {
            return ResponseEntity.notFound().build();
        }

        return ResponseEntity.ok(dogMatchDto);
    }

    @GetMapping("/{dogId}")
    public ResponseEntity<?> getDogById(@PathVariable Long dogId) {
        if (dogId == null) {
            String jsonMessage = "{\"message\":\"강아지 아이디가 올바르지 않습니다.\"";
            return ResponseEntity.badRequest().body(jsonMessage);
        }

        DogSpecificDto dogSpecificDto;
        try {
            dogSpecificDto = dogService.showDog(dogId);
        } catch (DogNotFoundException e) {
            return ResponseEntity.notFound().build();
        }

        return ResponseEntity.ok().body(dogSpecificDto);
    }
}
