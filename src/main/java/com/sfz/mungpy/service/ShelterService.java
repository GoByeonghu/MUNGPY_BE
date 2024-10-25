package com.sfz.mungpy.service;

import com.sfz.mungpy.dto.ShelterInformation;
import com.sfz.mungpy.dto.ShelterRegister;
import com.sfz.mungpy.entity.Shelter;
import com.sfz.mungpy.exception.ShelterNotFoundException;
import com.sfz.mungpy.repository.ShelterRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
@RequiredArgsConstructor
public class ShelterService {
    private final ShelterRepository shelterRepository;

    @Transactional(readOnly = true)
    public List<ShelterInformation> getAllShelters() {
        List<Shelter> shelters = shelterRepository.findAll();

        if (shelters.isEmpty()) throw new ShelterNotFoundException();

        return shelters.stream()
                .map(Shelter::toDto)
                .toList();
    }

    @Transactional(readOnly = true)
    public ShelterInformation getShelterById(Long shelterId) {
        return shelterRepository.findById(shelterId).orElseThrow(ShelterNotFoundException::new).toDto();
    }

    @Transactional
    public void addShelter(ShelterRegister register) {
        shelterRepository.save(register.toEntity());
    }
}
