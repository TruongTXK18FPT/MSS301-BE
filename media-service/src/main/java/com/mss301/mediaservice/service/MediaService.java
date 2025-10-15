package com.mss301.mediaservice.service;

import com.mss301.mediaservice.dto.request.UploadRequest;
import com.mss301.mediaservice.dto.response.MediaResponse;
import com.mss301.mediaservice.dto.response.UploadResponse;
import org.springframework.web.multipart.MultipartFile;

import java.util.List;

public interface MediaService {
    
    UploadResponse uploadFile(MultipartFile file, UploadRequest request, Long userId);
    UploadResponse uploadMultipleFiles(List<MultipartFile> files, UploadRequest request, Long userId);
    MediaResponse getFileInfo(String publicId);
    boolean deleteFile(String publicId);
    List<MediaResponse> getFilesByFolder(String folder);
}
