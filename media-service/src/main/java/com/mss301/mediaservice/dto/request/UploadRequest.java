package com.mss301.mediaservice.dto.request;

import jakarta.validation.constraints.NotBlank;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class UploadRequest {
    
    @NotBlank(message = "Folder is required")
    private String folder; // e.g., "assignments", "quizzes", "profiles"
    
    private String transformation; // e.g., "w_500,h_500,c_fill" for image resizing
    private String format; // e.g., "jpg", "png", "webp"
    private Integer quality; // 1-100 for image quality
    private Boolean eager; // Apply transformations immediately
}
