package com.sfz.mungpy.service;

import com.sfz.mungpy.entity.Shelter;
import com.sfz.mungpy.repository.ShelterRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Optional;

@Service
public class ShelterService {

    @Autowired
    private ShelterRepository shelterRepository;

    public List<Shelter> getAllShelters() {
        return shelterRepository.findAll();
    }

    public Optional<Shelter> getShelterById(Long id) {
        return shelterRepository.findById(id);
    }

    public Shelter addShelter(Shelter shelter) {
        return shelterRepository.save(shelter);
    }
}
