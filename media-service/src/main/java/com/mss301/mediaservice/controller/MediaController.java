package com.mss301.mediaservice.controller;

import com.mss301.mediaservice.dto.request.UploadRequest;
import com.mss301.mediaservice.dto.response.MediaResponse;
import com.mss301.mediaservice.dto.response.UploadResponse;
import com.mss301.mediaservice.service.MediaService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
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
    public ResponseEntity<UploadResponse> uploadFile(
            @RequestParam("file") MultipartFile file,
            @Valid @ModelAttribute UploadRequest request,
            Authentication authentication) {
        
        Long userId = Long.parseLong(authentication.getName());
        UploadResponse response = mediaService.uploadFile(file, request, userId);
        return ResponseEntity.ok(response);
    }

    @PostMapping("/upload-multiple")
    @Operation(summary = "Upload multiple files")
    public ResponseEntity<UploadResponse> uploadMultipleFiles(
            @RequestParam("files") List<MultipartFile> files,
            @Valid @ModelAttribute UploadRequest request,
            Authentication authentication) {
        
        Long userId = Long.parseLong(authentication.getName());
        UploadResponse response = mediaService.uploadMultipleFiles(files, request, userId);
        return ResponseEntity.ok(response);
    }

    @GetMapping("/info/{publicId}")
    @Operation(summary = "Get file information by public ID")
    public ResponseEntity<MediaResponse> getFileInfo(@PathVariable String publicId) {
        MediaResponse response = mediaService.getFileInfo(publicId);
        if (response != null) {
            return ResponseEntity.ok(response);
        } else {
            return ResponseEntity.notFound().build();
        }
    }

    @DeleteMapping("/{publicId}")
    @Operation(summary = "Delete a file by public ID")
    public ResponseEntity<String> deleteFile(@PathVariable String publicId) {
        boolean deleted = mediaService.deleteFile(publicId);
        if (deleted) {
            return ResponseEntity.ok("File deleted successfully");
        } else {
            return ResponseEntity.badRequest().body("Failed to delete file");
        }
    }

    @GetMapping("/folder/{folder}")
    @Operation(summary = "Get all files in a folder")
    public ResponseEntity<List<MediaResponse>> getFilesByFolder(@PathVariable String folder) {
        List<MediaResponse> files = mediaService.getFilesByFolder(folder);
        return ResponseEntity.ok(files);
    }
}
