package com.sfz.mungpy.entity;

import jakarta.persistence.*;
import lombok.*;

@Entity
@Table(name = "shelters")
@Getter
@Setter
@ToString
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class Shelter {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "protect_place")
    private String protectPlace;

    @Column(name = "protect_telno")
    private String protectTelno;

//    @Column(name = "protect_email")
//    private String protectEmail;
}
