package com.mss301.documentservice.service.google.impl;

import com.mss301.documentservice.client.GoogleFileSearchClient;
import com.mss301.documentservice.dto.google.*;
import com.mss301.documentservice.service.google.GoogleFileSearchService;
import feign.RetryableException;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import java.io.*;
import java.net.HttpURLConnection;
import java.net.SocketTimeoutException;
import java.net.URL;
import java.nio.file.Files;
import java.util.Collections;
import java.util.List;

@Service
@RequiredArgsConstructor
@Slf4j
public class GoogleFileSearchServiceImpl implements GoogleFileSearchService {

    private final GoogleFileSearchClient googleFileSearchClient;

    @Value("${google.api.key}")
    private String apiKey;

    @Value("${google.api.base-url:https://generativelanguage.googleapis.com}")
    private String baseUrl;

    @Override
    public FileSearchStoreResponse createFileSearchStore(String displayName) {
        log.info("Creating File Search Store with display name: {}", displayName);

        CreateFileSearchStoreRequest request = CreateFileSearchStoreRequest.builder()
                .displayName(displayName)
                .build();

        FileSearchStoreResponse response = googleFileSearchClient.createFileSearchStore(apiKey, request);
        log.info("File Search Store created successfully: {}", response.getName());

        return response;
    }

    @Override
    public void uploadFileToStore(String storeName, MultipartFile file) throws IOException {
        log.info("Uploading file {} to store: {}", file.getOriginalFilename(), storeName);

        byte[] fileData = file.getBytes();
        String mimeType = file.getContentType();
        if (mimeType == null) {
            mimeType = "application/pdf";
        }

        uploadFileData(storeName, fileData, mimeType);
    }

    @Override
    public void uploadFileToStore(String storeName, File file) throws IOException {
        log.info("Uploading file {} to store: {}", file.getName(), storeName);

        byte[] fileData = Files.readAllBytes(file.toPath());
        String mimeType = Files.probeContentType(file.toPath());
        if (mimeType == null) {
            mimeType = "application/pdf";
        }

        uploadFileData(storeName, fileData, mimeType);
    }

    private void uploadFileData(String storeName, byte[] fileData, String mimeType) throws IOException {
        // Step 1: Initiate resumable upload
        String uploadUrl = initiateResumableUpload(storeName, fileData.length, mimeType);
        log.info("Received upload URL: {}", uploadUrl);

        // Step 2: Upload file data
        uploadToGoogleStorage(uploadUrl, fileData);
        log.info("File uploaded successfully to store: {}", storeName);
    }

    private String initiateResumableUpload(String storeName, long fileSize, String mimeType) throws IOException {
        String urlString = baseUrl + "/upload/v1beta/" + storeName + ":uploadToFileSearchStore?key=" + apiKey;
        URL url = new URL(urlString);
        HttpURLConnection conn = (HttpURLConnection) url.openConnection();

        try {
            conn.setRequestMethod("POST");
            conn.setRequestProperty("X-Goog-Upload-Protocol", "resumable");
            conn.setRequestProperty("X-Goog-Upload-Command", "start");
            conn.setRequestProperty("X-Goog-Upload-Header-Content-Length", String.valueOf(fileSize));
            conn.setRequestProperty("X-Goog-Upload-Header-Content-Type", mimeType);
            conn.setRequestProperty("Content-Type", "application/json");
            conn.setDoOutput(true);

            // Send empty JSON body
            try (OutputStream os = conn.getOutputStream()) {
                os.write("{}".getBytes());
            }

            int responseCode = conn.getResponseCode();
            log.debug("Initiate upload response code: {}", responseCode);

            if (responseCode != 200) {
                String errorResponse = readErrorResponse(conn);
                throw new IOException("Failed to initiate upload. Response code: " + responseCode + ", Error: " + errorResponse);
            }

            String uploadUrl = conn.getHeaderField("X-Goog-Upload-URL");
            if (uploadUrl == null) {
                throw new IOException("No upload URL received in response headers");
            }

            return uploadUrl;
        } finally {
            conn.disconnect();
        }
    }

    private void uploadToGoogleStorage(String uploadUrl, byte[] data) throws IOException {
        URL url = new URL(uploadUrl);
        HttpURLConnection conn = (HttpURLConnection) url.openConnection();

        try {
            conn.setRequestMethod("POST");
            conn.setRequestProperty("Content-Length", String.valueOf(data.length));
            conn.setRequestProperty("X-Goog-Upload-Offset", "0");
            conn.setRequestProperty("X-Goog-Upload-Command", "upload, finalize");
            conn.setDoOutput(true);

            try (OutputStream os = conn.getOutputStream()) {
                os.write(data);
            }

            int responseCode = conn.getResponseCode();
            log.debug("Upload response code: {}", responseCode);

            if (responseCode != 200) {
                String errorResponse = readErrorResponse(conn);
                throw new IOException("Failed to upload file. Response code: " + responseCode + ", Error: " + errorResponse);
            }
        } finally {
            conn.disconnect();
        }
    }

    private String readErrorResponse(HttpURLConnection conn) throws IOException {
        InputStream errorStream = conn.getErrorStream();
        if (errorStream == null) {
            return "No error details available";
        }

        try (BufferedReader reader = new BufferedReader(new InputStreamReader(errorStream))) {
            StringBuilder response = new StringBuilder();
            String line;
            while ((line = reader.readLine()) != null) {
                response.append(line);
            }
            return response.toString();
        }
    }

    @Override
    public List<FileSearchStoreResponse> listFileSearchStores() {
        log.info("Listing all File Search Stores");

        try {
            ListFileSearchStoresResponse response = googleFileSearchClient.listFileSearchStores(apiKey);
            if (response != null && response.getFileSearchStores() != null) {
                log.info("Found {} File Search Stores", response.getFileSearchStores().size());
                return response.getFileSearchStores();
            }
            return Collections.emptyList();
        } catch (Exception e) {
            // Xử lý timeout và network errors gracefully
            String errorMessage = e.getMessage() != null ? e.getMessage().toLowerCase() : "";
            if (errorMessage.contains("timeout") || errorMessage.contains("connect timed out") || 
                errorMessage.contains("socket timeout") || e instanceof SocketTimeoutException ||
                e instanceof RetryableException) {
                log.warn("Timeout or network error when listing File Search Stores. " +
                        "This may be due to network issues or Google API being unavailable. " +
                        "Returning empty list to allow graceful degradation. Error: {}", e.getMessage());
                return Collections.emptyList();
            }
            // Các lỗi khác vẫn throw để caller biết
            log.error("Error listing File Search Stores", e);
            throw new RuntimeException("Failed to list File Search Stores: " + e.getMessage(), e);
        }
    }

    @Override
    public FileSearchStoreResponse getFileSearchStore(String storeName) {
        log.info("Getting File Search Store: {}", storeName);

        try {
            return googleFileSearchClient.getFileSearchStore(storeName, apiKey);
        } catch (Exception e) {
            log.error("Error getting File Search Store: {}", storeName, e);
            throw new RuntimeException("Failed to get File Search Store: " + e.getMessage(), e);
        }
    }

    @Override
    public void deleteFileSearchStore(String storeName) {
        log.info("Deleting File Search Store: {}", storeName);

        try {
            googleFileSearchClient.deleteFileSearchStore(storeName, apiKey);
            log.info("File Search Store deleted successfully: {}", storeName);
        } catch (Exception e) {
            log.error("Error deleting File Search Store: {}", storeName, e);
            throw new RuntimeException("Failed to delete File Search Store: " + e.getMessage(), e);
        }
    }
}

