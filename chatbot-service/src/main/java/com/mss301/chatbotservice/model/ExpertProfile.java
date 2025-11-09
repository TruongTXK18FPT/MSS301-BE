package com.mss301.chatbotservice.model;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Table(name = "expert_profiles")
@Entity
@Data
@NoArgsConstructor
@AllArgsConstructor
public class ExpertProfile {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "expert_profiles_id")
    private Long id;

    @Column(name = "code")
    private String code;

    @Column(name = "name")
    private String name;

    @Column(name = "description")
    private String description;

    @Column(name = "prompt_config")
    private String promptConfig;

    @Column(name = "active")
    private Boolean active;

}
