package com.sfz.mungpy.controller;

import com.sfz.mungpy.dto.DogMatch;
import com.sfz.mungpy.dto.DogSpecific;
import com.sfz.mungpy.dto.UserInfomation;
import com.sfz.mungpy.exception.DogNotFoundException;
import com.sfz.mungpy.exception.ShelterNotFoundException;
import com.sfz.mungpy.service.DogService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.core.io.ResourceLoader;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Slf4j
@RestController
@RequiredArgsConstructor
@RequestMapping("/dog")
public class DogController {
    private final DogService dogService;
    private final ResourceLoader resourceLoader;

    @PostMapping
    public ResponseEntity<?> getDog(@ModelAttribute UserInfomation userInfomation) {
        String jsonMessage = "{\"message\":";

        List<String> personality = userInfomation.getPersonality();
        if (personality == null || personality.isEmpty()) {
            jsonMessage += "\"사용자 성향 데이터가 존재하지 않습니다.\"";
            return ResponseEntity.badRequest().body(jsonMessage);
        }

        List<String> personalityList = userInfomation.getPersonality();
        for (int i = 0; i < personalityList.size(); i++) {
            log.info("{}: {}", i, personalityList.get(i));
        }
        log.info("imageName: {}", userInfomation.getImage().getOriginalFilename());

        if (personality.size() != 6) {
            jsonMessage += "\"사용자 성향 데이터의 갯수는 6개여야 합니다.\"";
            return ResponseEntity.badRequest().body(jsonMessage);
        }

        MultipartFile image = userInfomation.getImage();
        if (image == null || image.isEmpty()) {
            jsonMessage += "\"사용자 이미지가 존재하지 않습니다.\"";
            return ResponseEntity.badRequest().body(jsonMessage);
        }

        DogMatch dogMatch;
        try {
            dogMatch = dogService.matchDog(personality, image);
        } catch (DogNotFoundException e) {
            return ResponseEntity.notFound().build();
        }

        log.info(dogMatch.toString());

        return ResponseEntity.ok(dogMatch);
    }

    @GetMapping("/{dogId}")
    public ResponseEntity<?> getDogById(@PathVariable Long dogId) {
        if (dogId == null) {
            String jsonMessage = "{\"message\":\"강아지 아이디가 올바르지 않습니다.\"";
            return ResponseEntity.badRequest().body(jsonMessage);
        }

        DogSpecific dogSpecific;
        try {
            dogSpecific = dogService.showDog(dogId);
        } catch (DogNotFoundException | ShelterNotFoundException e) {
            return ResponseEntity.notFound().build();
        }

        return ResponseEntity.ok().body(dogSpecific);
    }
}
