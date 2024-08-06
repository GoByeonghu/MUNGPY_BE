package com.sfz.mungpy.dto;

import com.fasterxml.jackson.annotation.JsonFormat;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;
import java.util.List;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class DogSpecific {
    private int age;
    private String sex;
    private String kind;
    private String name;
    private String image;
    private List<String> personality;
    private String rescuePlace;
    private String protectPlace;
    private String protectTelno;
    @JsonFormat(shape = JsonFormat.Shape.STRING, pattern = "yyyy-MM-dd")
    private LocalDateTime expirationDate;

    private double latitude;
    private double longitude;
}
