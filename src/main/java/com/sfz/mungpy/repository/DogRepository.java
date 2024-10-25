package com.sfz.mungpy.repository;

import com.sfz.mungpy.entity.Dog;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface DogRepository extends JpaRepository<Dog, Long> {
    Optional<Dog> findByImage(String image);
}
