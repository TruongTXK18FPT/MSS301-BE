package com.mss301.mediaservice.dto.response;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class MediaResponse {
    
    private String publicId;
    private String url;
    private String secureUrl;
    private String format;
    private String resourceType; // image, video, raw
    private Long bytes;
    private Integer width;
    private Integer height;
    private String folder;
    private LocalDateTime uploadedAt;
    private String originalFilename;
}
