package com.mss301.mediaservice.service.impl;

import com.cloudinary.Cloudinary;
import com.cloudinary.utils.ObjectUtils;
import com.mss301.mediaservice.dto.request.UploadRequest;
import com.mss301.mediaservice.dto.response.MediaResponse;
import com.mss301.mediaservice.dto.response.UploadResponse;
import com.mss301.mediaservice.service.MediaService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

@Service
@RequiredArgsConstructor
@Slf4j
public class MediaServiceImpl implements MediaService {

    private final Cloudinary cloudinary;

    @Override
    public UploadResponse uploadFile(MultipartFile file, UploadRequest request, Long userId) {
        try {
            Map<String, Object> uploadParams = buildUploadParams(request, userId);
            Map<String, Object> result = cloudinary.uploader().upload(file.getBytes(), uploadParams);
            
            MediaResponse mediaResponse = buildMediaResponse(result);
            
            return UploadResponse.builder()
                    .file(mediaResponse)
                    .message("File uploaded successfully")
                    .success(true)
                    .build();
                    
        } catch (IOException e) {
            log.error("Error uploading file: {}", e.getMessage());
            return UploadResponse.builder()
                    .message("Failed to upload file: " + e.getMessage())
                    .success(false)
                    .build();
        }
    }

    @Override
    public UploadResponse uploadMultipleFiles(List<MultipartFile> files, UploadRequest request, Long userId) {
        List<MediaResponse> uploadedFiles = new ArrayList<>();
        List<String> errors = new ArrayList<>();
        
        for (MultipartFile file : files) {
            try {
                Map<String, Object> uploadParams = buildUploadParams(request, userId);
                Map<String, Object> result = cloudinary.uploader().upload(file.getBytes(), uploadParams);
                uploadedFiles.add(buildMediaResponse(result));
            } catch (IOException e) {
                log.error("Error uploading file {}: {}", file.getOriginalFilename(), e.getMessage());
                errors.add("Failed to upload " + file.getOriginalFilename() + ": " + e.getMessage());
            }
        }
        
        return UploadResponse.builder()
                .files(uploadedFiles)
                .message(errors.isEmpty() ? "All files uploaded successfully" : 
                        "Some files failed to upload: " + String.join(", ", errors))
                .success(errors.isEmpty())
                .build();
    }

    @Override
    public MediaResponse getFileInfo(String publicId) {
        try {
            Map<String, Object> result = cloudinary.api().resource(publicId, ObjectUtils.emptyMap());
            return buildMediaResponse(result);
        } catch (Exception e) {
            log.error("Error getting file info for publicId {}: {}", publicId, e.getMessage());
            return null;
        }
    }

    @Override
    public boolean deleteFile(String publicId) {
        try {
            Map<String, Object> result = cloudinary.uploader().destroy(publicId, ObjectUtils.emptyMap());
            return (Boolean) result.get("result");
        } catch (Exception e) {
            log.error("Error deleting file with publicId {}: {}", publicId, e.getMessage());
            return false;
        }
    }

    @Override
    public List<MediaResponse> getFilesByFolder(String folder) {
        try {
            Map<String, Object> result = cloudinary.api().resources(ObjectUtils.asMap(
                    "type", "upload",
                    "prefix", folder,
                    "max_results", 100
            ));
            List<Map<String, Object>> resources = (List<Map<String, Object>>) result.get("resources");
            
            List<MediaResponse> mediaResponses = new ArrayList<>();
            for (Map<String, Object> resource : resources) {
                mediaResponses.add(buildMediaResponse(resource));
            }
            
            return mediaResponses;
        } catch (Exception e) {
            log.error("Error getting files from folder {}: {}", folder, e.getMessage());
            return new ArrayList<>();
        }
    }

    private Map<String, Object> buildUploadParams(UploadRequest request, Long userId) {
        Map<String, Object> params = ObjectUtils.asMap(
                "folder", request.getFolder() + "/user_" + userId,
                "public_id", null // Let Cloudinary generate unique ID
        );
        
        if (request.getTransformation() != null) {
            params.put("transformation", request.getTransformation());
        }
        
        if (request.getFormat() != null) {
            params.put("format", request.getFormat());
        }
        
        if (request.getQuality() != null) {
            params.put("quality", request.getQuality());
        }
        
        if (request.getEager() != null && request.getEager()) {
            params.put("eager", request.getTransformation());
        }
        
        return params;
    }

    private MediaResponse buildMediaResponse(Map<String, Object> result) {
        return MediaResponse.builder()
                .publicId((String) result.get("public_id"))
                .url((String) result.get("url"))
                .secureUrl((String) result.get("secure_url"))
                .format((String) result.get("format"))
                .resourceType((String) result.get("resource_type"))
                .bytes(result.get("bytes") != null ? ((Number) result.get("bytes")).longValue() : null)
                .width(result.get("width") != null ? ((Number) result.get("width")).intValue() : null)
                .height(result.get("height") != null ? ((Number) result.get("height")).intValue() : null)
                .folder((String) result.get("folder"))
                .uploadedAt(LocalDateTime.now())
                .originalFilename((String) result.get("original_filename"))
                .build();
    }
}
