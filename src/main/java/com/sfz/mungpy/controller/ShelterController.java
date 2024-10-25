package com.sfz.mungpy.controller;

import com.sfz.mungpy.dto.ShelterInformation;
import com.sfz.mungpy.dto.ShelterRegister;
import com.sfz.mungpy.exception.ShelterNotFoundException;
import com.sfz.mungpy.service.ShelterService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/shelter")
@RequiredArgsConstructor
public class ShelterController {
    private final ShelterService shelterService;

    // 모든 보호소 목록 조회
    @GetMapping
    public ResponseEntity<?> getAllShelters() {
        List<ShelterInformation> shelters;
        try {
            shelters = shelterService.getAllShelters();
        } catch (ShelterNotFoundException e) {
            return ResponseEntity.notFound().build();
        }

        return ResponseEntity.ok(shelters);
    }

    // 특정 보호소 조회
    @GetMapping("/{shelterId}")
    public ResponseEntity<?> getShelterById(@PathVariable Long shelterId) {
        ShelterInformation shelter;
        try {
            shelter = shelterService.getShelterById(shelterId);
        } catch (ShelterNotFoundException e) {
            return ResponseEntity.notFound().build();
        }

        return ResponseEntity.ok(shelter);
    }

    // 보호소 추가
    @PostMapping
    public ResponseEntity<?> addShelter(@ModelAttribute ShelterRegister register) {
        if (register == null)
            return ResponseEntity.badRequest().build();

        try {
            shelterService.addShelter(register);
        } catch (Exception e) {
            return ResponseEntity.internalServerError().build();
        }

        return ResponseEntity.ok().build();
    }
}

