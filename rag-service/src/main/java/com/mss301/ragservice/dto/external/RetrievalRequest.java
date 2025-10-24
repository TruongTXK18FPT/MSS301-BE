package com.mss301.ragservice.dto.external;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class RetrievalRequest {
    private String documentId;
    private String chapterId;
    private String lessonId;
    private String queryText;
    private boolean useSemantic;
    private int topK;
}
