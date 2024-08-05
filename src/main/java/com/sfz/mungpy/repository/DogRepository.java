package com.sfz.mungpy.repository;

import com.sfz.mungpy.entity.Dog;
import org.springframework.data.jpa.repository.JpaRepository;

public interface DogRepository extends JpaRepository<Dog, Long> {
}
