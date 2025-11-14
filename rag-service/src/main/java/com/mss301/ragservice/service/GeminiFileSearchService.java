package com.mss301.ragservice.service;

import java.io.*;
import java.net.HttpURLConnection;
import java.net.URL;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import com.google.gson.*;
import com.mss301.ragservice.dto.response.FileSearchQueryResponse;
import com.mss301.ragservice.dto.response.FileSearchStoreResponse;
import com.mss301.ragservice.exception.RagServiceException;

import lombok.extern.slf4j.Slf4j;

@Slf4j
@Service
public class GeminiFileSearchService {

    @Value("${gemini.file-search.api-key}")
    private String apiKey;

    @Value("${gemini.file-search.model:gemini-2.5-flash}")
    private String model;

    private static final String BASE_URL = "https://generativelanguage.googleapis.com";

    /**
     * Lấy danh sách các File Search Stores
     */
    public List<FileSearchStoreResponse> listFileSearchStores() {
        log.info("Fetching list of file search stores");
        try {
            URL url = new URL(BASE_URL + "/v1beta/fileSearchStores?key=" + apiKey);
            HttpURLConnection conn = (HttpURLConnection) url.openConnection();
            conn.setRequestMethod("GET");

            String response = readResponse(conn);
            JsonObject jsonResponse = JsonParser.parseString(response).getAsJsonObject();

            List<FileSearchStoreResponse> stores = new ArrayList<>();
            if (jsonResponse.has("fileSearchStores")) {
                JsonArray storesArray = jsonResponse.getAsJsonArray("fileSearchStores");
                log.info("Found {} file search stores", storesArray.size());

                for (int i = 0; i < storesArray.size(); i++) {
                    JsonObject store = storesArray.get(i).getAsJsonObject();
                    String name = store.get("name").getAsString();
                    String displayName = store.has("displayName")
                        ? store.get("displayName").getAsString()
                        : name;

                    stores.add(new FileSearchStoreResponse(name, displayName));
                }
            } else {
                log.info("No file search stores found");
            }

            return stores;

        } catch (Exception e) {
            log.error("Error fetching file search stores", e);
            throw new RagServiceException("Failed to fetch file search stores: " + e.getMessage(), e);
        }
    }

    /**
     * Thực hiện query với File Search
     */
    public FileSearchQueryResponse queryWithFileSearch(String storeName, String query) {
        log.info("Querying file search store: {} with query: {} using model: {}", storeName, query, model);
        try {
            URL url = new URL(BASE_URL + "/v1beta/models/" + model + ":generateContent?key=" + apiKey);
            HttpURLConnection conn = (HttpURLConnection) url.openConnection();
            conn.setRequestMethod("POST");
            conn.setRequestProperty("Content-Type", "application/json");
            conn.setDoOutput(true);

            // Xây dựng request body
            JsonObject body = new JsonObject();

            // Contents
            JsonArray contents = new JsonArray();
            JsonObject content = new JsonObject();
            JsonArray parts = new JsonArray();
            JsonObject part = new JsonObject();
            part.addProperty("text", query);
            parts.add(part);
            content.add("parts", parts);
            contents.add(content);
            body.add("contents", contents);

            // Tools with file search
            JsonArray tools = new JsonArray();
            JsonObject tool = new JsonObject();
            JsonObject fileSearch = new JsonObject();
            JsonArray storeNames = new JsonArray();
            storeNames.add(storeName);
            fileSearch.add("file_search_store_names", storeNames);
            tool.add("file_search", fileSearch);
            tools.add(tool);
            body.add("tools", tools);

            // Gửi request
            try (OutputStream os = conn.getOutputStream()) {
                os.write(body.toString().getBytes());
            }

            String response = readResponse(conn);
            String answer = parseResponse(response);

            log.info("Successfully got response from file search");

            return new FileSearchQueryResponse(
                query,
                answer,
                storeName,
                LocalDateTime.now()
            );

        } catch (Exception e) {
            log.error("Error querying file search", e);
            throw new RagServiceException("Failed to query file search: " + e.getMessage(), e);
        }
    }

    /**
     * Parse response từ Gemini API
     */
    private String parseResponse(String response) {
        try {
            JsonObject jsonResponse = JsonParser.parseString(response).getAsJsonObject();

            if (jsonResponse.has("candidates")) {
                JsonArray candidates = jsonResponse.getAsJsonArray("candidates");
                if (candidates.size() > 0) {
                    JsonObject candidate = candidates.get(0).getAsJsonObject();
                    if (candidate.has("content")) {
                        JsonObject contentObj = candidate.getAsJsonObject("content");
                        if (contentObj.has("parts")) {
                            JsonArray partsArr = contentObj.getAsJsonArray("parts");
                            if (partsArr.size() > 0) {
                                JsonObject partObj = partsArr.get(0).getAsJsonObject();
                                if (partObj.has("text")) {
                                    return partObj.get("text").getAsString();
                                }
                            }
                        }
                    }
                }
            }

            // Nếu không parse được, trả về raw response
            log.warn("Could not parse response, returning raw response");
            return response;

        } catch (Exception e) {
            log.error("Error parsing response", e);
            return response;
        }
    }

    /**
     * Đọc response từ HTTP connection
     */
    private String readResponse(HttpURLConnection conn) throws IOException {
        int responseCode = conn.getResponseCode();
        InputStream is = (responseCode >= 200 && responseCode < 300)
            ? conn.getInputStream()
            : conn.getErrorStream();

        try (BufferedReader br = new BufferedReader(new InputStreamReader(is))) {
            StringBuilder response = new StringBuilder();
            String line;
            while ((line = br.readLine()) != null) {
                response.append(line);
            }

            String result = response.toString();

            // Log error nếu có
            if (responseCode >= 400) {
                log.error("HTTP Error {}: {}", responseCode, result);

                // Xử lý riêng cho 404 - Store không tồn tại
                if (responseCode == 404) {
                    String userFriendlyMessage = "File Search Store không tồn tại trên Google. " +
                        "Store có thể đã bị xóa. Vui lòng upload lại document để tạo store mới.";
                    throw new IOException("HTTP Error 404: " + userFriendlyMessage);
                }

                // Xử lý riêng cho 429 quota error
                if (responseCode == 429) {
                    String userFriendlyMessage = "Đã vượt quá giới hạn API Gemini. " +
                        "Vui lòng đợi 1-2 phút hoặc thử đổi model khác. " +
                        "Xem thêm tại: FIX_429_ERROR.md";
                    throw new IOException("HTTP Error 429: " + userFriendlyMessage);
                }

                throw new IOException("HTTP Error " + responseCode + ": " + result);
            }

            return result;
        }
    }
}

