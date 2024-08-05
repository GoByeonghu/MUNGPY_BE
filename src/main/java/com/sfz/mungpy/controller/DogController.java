package com.sfz.mungpy.controller;

import com.sfz.mungpy.dto.DogDto;
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
    public ResponseEntity<?> getDog(@RequestParam(required = false) List<String> personality,
                                    @RequestParam(required = false) MultipartFile image) {


        String jsonMessage = "{\"message\":";

        if (personality == null || personality.isEmpty()) {
            jsonMessage += "\"사용자 성향 데이터가 올바르지 않습니다.\"";
            return ResponseEntity.badRequest().body(jsonMessage);
        }

        if (image == null || image.isEmpty()) {
            jsonMessage += "\"사용자 이미지가 올바르지 않습니다.\"";
            return ResponseEntity.badRequest().body(jsonMessage);
        }

        log.info(personality.toString());
        log.info(image.getOriginalFilename());

        DogDto dogDto;
        try {
            dogDto = dogService.matchDog(personality, image);
        } catch (DogNotFoundException e) {
            return ResponseEntity.notFound().build();
        }

        return ResponseEntity.ok(dogDto);
    }
}
