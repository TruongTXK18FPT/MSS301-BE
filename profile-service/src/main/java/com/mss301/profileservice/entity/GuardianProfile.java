package com.mss301.profileservice.entity;

import jakarta.persistence.*;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Entity
@Table(name = "guardian_profiles")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class GuardianProfile {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "user_id", nullable = false, unique = true)
    private Long userId;

    @Column(name = "relationship")
    private String relationship;

    @Column(name = "phone_alt", length = 15)
    private String phoneAlt;
}
