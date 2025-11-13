package com.mss301.documentservice.client;

import com.mss301.documentservice.dto.google.*;
import feign.Headers;
import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.*;

/**
 * Feign client for Google Gemini File Search API
 */
@FeignClient(
        name = "google-file-search",
        url = "${google.api.base-url:https://generativelanguage.googleapis.com}",
        configuration = GoogleFileSearchClientConfig.class)
public interface GoogleFileSearchClient {

    @PostMapping("/v1beta/fileSearchStores?key={apiKey}")
    @Headers("Content-Type: application/json")
    FileSearchStoreResponse createFileSearchStore(
            @PathVariable("apiKey") String apiKey,
            @RequestBody CreateFileSearchStoreRequest request);

    @GetMapping("/v1beta/fileSearchStores?key={apiKey}")
    ListFileSearchStoresResponse listFileSearchStores(@PathVariable("apiKey") String apiKey);

    @GetMapping("/v1beta/{storeName}?key={apiKey}")
    FileSearchStoreResponse getFileSearchStore(
            @PathVariable("storeName") String storeName,
            @PathVariable("apiKey") String apiKey);

    @DeleteMapping("/v1beta/{storeName}?key={apiKey}&force=true")
    void deleteFileSearchStore(
            @PathVariable("storeName") String storeName,
            @PathVariable("apiKey") String apiKey);

    @PostMapping("/upload/v1beta/{storeName}:uploadToFileSearchStore?key={apiKey}")
    @Headers({
        "X-Goog-Upload-Protocol: resumable",
        "X-Goog-Upload-Command: start",
        "Content-Type: application/json"
    })
    InitiateUploadResponse initiateResumableUpload(
            @PathVariable("storeName") String storeName,
            @PathVariable("apiKey") String apiKey,
            @RequestHeader("X-Goog-Upload-Header-Content-Length") long contentLength,
            @RequestHeader("X-Goog-Upload-Header-Content-Type") String contentType,
            @RequestBody String emptyBody);
}

