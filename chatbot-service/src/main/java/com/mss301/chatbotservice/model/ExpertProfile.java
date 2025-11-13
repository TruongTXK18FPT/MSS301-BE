package com.mss301.chatbotservice.model;

import com.mss301.chatbotservice.enums.LLMProvider;
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

    @Column(name = "prompt_config", columnDefinition = "TEXT")
    private String promptConfig;

    @Enumerated(EnumType.STRING)
    @Column(name = "llm_provider")
    private LLMProvider llmProvider;

    @Column(name = "use_rag")
    private Boolean useRag = false; // Sử dụng RAG service để chat theo giáo trình

    @Column(name = "active")
    private Boolean active;
}
