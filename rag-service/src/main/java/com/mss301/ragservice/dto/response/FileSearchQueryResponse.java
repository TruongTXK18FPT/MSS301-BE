package com.mss301.ragservice.dto.response;

import java.time.LocalDateTime;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class FileSearchQueryResponse {
    private String query;
    private String answer;
    private String fileStoreName;
    private LocalDateTime timestamp;
}

