package com.sfz.mungpy.dto;

import com.sfz.mungpy.entity.Shelter;
import com.sfz.mungpy.entity.ShelterType;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@AllArgsConstructor
@NoArgsConstructor
public class ShelterRegister {
    private String name;
    private String address;
    private String telno;
    private String delegate;
    private ShelterType type;

    public Shelter toEntity() {
        return Shelter.builder()
                .name(name)
                .address(address)
                .telno(telno)
                .delegate(delegate)
                .type(type)
                .build();
    }
}
