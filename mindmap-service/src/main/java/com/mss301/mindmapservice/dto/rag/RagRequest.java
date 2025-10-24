package com.mss301.mindmapservice.dto.rag;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class RagRequest {

    private String queryText;
    private String mode;
    private String llmProvider;
    private String documentId;
    private String chapterId;
    private String lessonId;

    @Builder.Default
    private Boolean useSemantic = true;

    @Builder.Default
    private Integer topK = 5;
}
