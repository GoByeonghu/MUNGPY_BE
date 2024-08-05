package com.sfz.mungpy.service;

import com.sfz.mungpy.repository.DogRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class DogService {
    private final DogRepository dogRepository;

}
