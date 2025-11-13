package com.mss301.documentservice.service.google;

import com.mss301.documentservice.dto.google.FileSearchStoreResponse;
import org.springframework.web.multipart.MultipartFile;

import java.io.File;
import java.io.IOException;
import java.util.List;

/**
 * Service interface for interacting with Google File Search Store
 */
public interface GoogleFileSearchService {

    /**
     * Create a new File Search Store
     * @param displayName Display name for the store (e.g., textbook title)
     * @return FileSearchStoreResponse containing store details
     */
    FileSearchStoreResponse createFileSearchStore(String displayName);

    /**
     * Upload a file to a specific File Search Store
     * @param storeName The name of the store
     * @param file The file to upload
     * @throws IOException if upload fails
     */
    void uploadFileToStore(String storeName, MultipartFile file) throws IOException;

    /**
     * Upload a file to a specific File Search Store from file path
     * @param storeName The name of the store
     * @param file The file to upload
     * @throws IOException if upload fails
     */
    void uploadFileToStore(String storeName, File file) throws IOException;

    /**
     * List all File Search Stores
     * @return List of FileSearchStoreResponse
     */
    List<FileSearchStoreResponse> listFileSearchStores();

    /**
     * Get details of a specific File Search Store
     * @param storeName The name of the store
     * @return FileSearchStoreResponse
     */
    FileSearchStoreResponse getFileSearchStore(String storeName);

    /**
     * Delete a File Search Store
     * @param storeName The name of the store to delete
     */
    void deleteFileSearchStore(String storeName);
}

