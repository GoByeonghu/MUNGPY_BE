package com.sfz.mungpy.entity;

import com.sfz.mungpy.dto.ShelterInformation;
import jakarta.persistence.*;
import lombok.*;

@Entity
@Table(name = "shelters")
@Getter
@ToString
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class Shelter {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    private String name; // 업체 이름
    private String address;
    private String telno;
    private String delegate; // 대표자 이름
    @Enumerated(EnumType.STRING)
    private ShelterType type;

    public ShelterInformation toDto() {
        return ShelterInformation.builder()
                .name(name)
                .address(address)
                .telno(telno)
                .build();
    }
}
