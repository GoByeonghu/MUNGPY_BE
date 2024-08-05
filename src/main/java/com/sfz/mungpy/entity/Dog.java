package com.sfz.mungpy.entity;

import com.sfz.mungpy.dto.DogDto;
import jakarta.persistence.*;
import lombok.*;

@Entity
@Table(name = "dogs")
@Getter
@ToString
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class Dog {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    private String name;
    private String image;
    private String summary;
    private String description;

    public DogDto toDto() {
        return DogDto.builder()
                .name(name)
                .image(image)
                .summary(summary)
                .description(description)
                .build();
    }
}