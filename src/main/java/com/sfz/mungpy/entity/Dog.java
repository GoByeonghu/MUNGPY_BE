package com.sfz.mungpy.entity;

import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
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
    private Long id;
}