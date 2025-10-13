package com.mss301.mediaservice.dto.response;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class UploadResponse {
    
    private MediaResponse file;
    private List<MediaResponse> files; // For multiple uploads
    private String message;
    private boolean success;
}
