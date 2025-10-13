package com.mss301.mindmapservice.dto.request;

import java.time.LocalDateTime;

import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

import com.mss301.mindmapservice.entity.MindmapShare.Permission;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class MindmapShareRequest {

    @NotNull(message = "Mindmap ID is required")
    private Long mindmapId;

    private Long sharedWithUserId;

    @Size(max = 50, message = "Share code must not exceed 50 characters")
    private String shareCode;

    @NotNull(message = "Permission is required")
    private Permission permission;

    private LocalDateTime expiresAt;
    private Boolean isActive = true;
}
