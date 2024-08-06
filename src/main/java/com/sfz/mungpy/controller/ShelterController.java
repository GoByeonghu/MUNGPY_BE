package com.sfz.mungpy.controller;

import com.sfz.mungpy.entity.Shelter;
import com.sfz.mungpy.service.ShelterService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Optional;

@RestController
@RequestMapping("/shelter")
public class ShelterController {

    @Autowired
    private ShelterService shelterService;

    // 모든 보호소 목록 조회
    @GetMapping
    public ResponseEntity<List<Shelter>> getAllShelters() {
        List<Shelter> shelters = shelterService.getAllShelters();
        return new ResponseEntity<>(shelters, HttpStatus.OK);
    }

    // 특정 보호소 조회
    @GetMapping("/{id}")
    public ResponseEntity<Shelter> getShelterById(@PathVariable Long id) {
        Optional<Shelter> shelter = shelterService.getShelterById(id);
        return shelter.map(ResponseEntity::ok).orElseGet(() -> ResponseEntity.notFound().build());
    }

    // 보호소 추가
    @PostMapping
    public ResponseEntity<Shelter> addShelter(@RequestBody Shelter shelter) {
        Shelter newShelter = shelterService.addShelter(shelter);
        return new ResponseEntity<>(newShelter, HttpStatus.CREATED);
    }
}

