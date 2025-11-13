package com.mss301.ragservice.dto.request;

import lombok.Data;

@Data
public class FileSearchQueryRequest {
    private String fileStoreName;
    private String query;
}

