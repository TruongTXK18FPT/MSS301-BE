package com.mss301.mediaservice.controller;

import com.mss301.mediaservice.dto.request.UploadRequest;
import com.mss301.mediaservice.dto.response.ApiResponse;
import com.mss301.mediaservice.dto.response.MediaResponse;
import com.mss301.mediaservice.dto.response.UploadResponse;
import com.mss301.mediaservice.service.MediaService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.util.List;

@RestController
@RequestMapping("/api/v1/media")
@RequiredArgsConstructor
@Tag(name = "Media Management")
public class MediaController {

    private final MediaService mediaService;

    @PostMapping("/upload")
    @Operation(summary = "Upload a single file")
    public ResponseEntity<ApiResponse<UploadResponse>> uploadFile(
            @RequestParam("file") MultipartFile file,
            @RequestParam(value = "folder", defaultValue = "general") String folder,
            @RequestParam(value = "quality", required = false) Integer quality,
            @AuthenticationPrincipal(errorOnInvalidType = false) Jwt jwt) {
        
        // Extract userId from JWT - either from userId claim or subject
        Long userId = 1L; // Default fallback
        if (jwt != null) {
            try {
                Object userIdClaim = jwt.getClaim("userId");
                if (userIdClaim != null) {
                    userId = Long.valueOf(userIdClaim.toString());
                } else if (jwt.getSubject() != null) {
                    userId = Long.valueOf(jwt.getSubject());
                }
            } catch (Exception e) {
                // Use default userId if parsing fails
            }
        }
        
        UploadRequest request = UploadRequest.builder()
                .folder(folder)
                .quality(quality)
                .build();
        
        UploadResponse response = mediaService.uploadFile(file, request, userId);
        return ResponseEntity.ok(ApiResponse.success("File uploaded successfully", response));
    }

    @PostMapping("/upload-multiple")
    @Operation(summary = "Upload multiple files")
    public ResponseEntity<ApiResponse<UploadResponse>> uploadMultipleFiles(
            @RequestParam("files") List<MultipartFile> files,
            @RequestParam(value = "folder", defaultValue = "general") String folder,
            @RequestParam(value = "quality", required = false) Integer quality,
            @AuthenticationPrincipal(errorOnInvalidType = false) Jwt jwt) {
        
        // Extract userId from JWT - either from userId claim or subject
        Long userId = 1L; // Default fallback
        if (jwt != null) {
            try {
                Object userIdClaim = jwt.getClaim("userId");
                if (userIdClaim != null) {
                    userId = Long.valueOf(userIdClaim.toString());
                } else if (jwt.getSubject() != null) {
                    userId = Long.valueOf(jwt.getSubject());
                }
            } catch (Exception e) {
                // Use default userId if parsing fails
            }
        }
        
        UploadRequest request = UploadRequest.builder()
                .folder(folder)
                .quality(quality)
                .build();
        
        UploadResponse response = mediaService.uploadMultipleFiles(files, request, userId);
        return ResponseEntity.ok(ApiResponse.success("Files uploaded successfully", response));
    }

    @GetMapping("/info/{publicId}")
    @Operation(summary = "Get file information by public ID")
    public ResponseEntity<ApiResponse<MediaResponse>> getFileInfo(@PathVariable String publicId) {
        MediaResponse response = mediaService.getFileInfo(publicId);
        if (response != null) {
            return ResponseEntity.ok(ApiResponse.success(response));
        } else {
            return ResponseEntity.notFound().build();
        }
    }

    @DeleteMapping("/{publicId}")
    @Operation(summary = "Delete a file by public ID")
    public ResponseEntity<ApiResponse<String>> deleteFile(@PathVariable String publicId) {
        boolean deleted = mediaService.deleteFile(publicId);
        if (deleted) {
            return ResponseEntity.ok(ApiResponse.success("File deleted successfully", "Deleted"));
        } else {
            return ResponseEntity.badRequest()
                    .body(ApiResponse.error("DELETE_FAILED", "Failed to delete file"));
        }
    }

    @GetMapping("/folder/{folder}")
    @Operation(summary = "Get all files in a folder")
    public ResponseEntity<ApiResponse<List<MediaResponse>>> getFilesByFolder(@PathVariable String folder) {
        List<MediaResponse> files = mediaService.getFilesByFolder(folder);
        return ResponseEntity.ok(ApiResponse.success(files));
    }
}
