package com.mss301.mindmapservice.dto.request;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class AiGenerateMindmapRequest {

    @NotBlank(message = "Topic is required")
    @Size(max = 255, message = "Topic must not exceed 255 characters")
    private String topic;

    @Size(max = 1000, message = "Description must not exceed 1000 characters")
    private String description;

    @NotBlank(message = "Grade is required")
    @Size(max = 50, message = "Grade must not exceed 50 characters")
    private String grade;

    @NotBlank(message = "Subject is required")
    @Size(max = 100, message = "Subject must not exceed 100 characters")
    private String subject;

    @NotNull(message = "AI provider is required")
    private AiProvider aiProvider;

    private String aiModel;

    @Size(max = 2000, message = "Additional context must not exceed 2000 characters")
    private String additionalContext;

    private Integer maxNodes = 20;
    private Integer maxDepth = 3;
    private Boolean includeExamples = true;
    private Boolean includeExercises = true;
    private Boolean includeFormulas = true;

    public enum AiProvider {
        MISTRAL("Mistral"),
        GEMINI("Gemini");

        private final String displayName;

        AiProvider(String displayName) {
            this.displayName = displayName;
        }

        public String getDisplayName() {
            return displayName;
        }
    }
}
