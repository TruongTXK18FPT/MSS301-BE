package com.mss301.ragservice.service;

import java.time.LocalDateTime;
import java.util.List;

import org.springframework.stereotype.Service;

import com.mss301.ragservice.client.RetrievalServiceClient;
import com.mss301.ragservice.dto.external.RetrievalRequest;
import com.mss301.ragservice.dto.external.RetrievalResponse;
import com.mss301.ragservice.dto.request.RagRequest;
import com.mss301.ragservice.dto.response.RagResponse;
import com.mss301.ragservice.enums.LLMProvider;
import com.mss301.ragservice.exception.RagServiceException;
import com.mss301.ragservice.service.llm.LLMService;
import com.mss301.ragservice.service.llm.LLMServiceFactory;
import com.mss301.ragservice.strategy.ResponseStrategy;
import com.mss301.ragservice.strategy.ResponseStrategyFactory;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@Slf4j
@Service
@RequiredArgsConstructor
public class RagService {

    private final RetrievalServiceClient retrievalClient;
    private final LLMServiceFactory llmServiceFactory;
    private final ResponseStrategyFactory strategyFactory;
    private final DocumentContextService contextService;
    private final GeminiFileSearchService fileSearchService;

    public RagResponse processQuery(RagRequest request) {
        log.info(
                "Processing RAG query - Mode: {}, Provider: {}, Query: '{}', UseDocuments: {}, FileStoreName: {}",
                request.getMode(),
                request.getLlmProvider(),
                request.getQueryText(),
                request.getUseDocuments(),
                request.getFileStoreName());

        try {
            // Kiểm tra nếu có fileStoreName, sử dụng Google File Search
            if (request.getFileStoreName() != null && !request.getFileStoreName().isBlank()) {
                log.info("Using Google File Search with store: {}", request.getFileStoreName());
                try {
                    return processFileSearchQuery(request);
                } catch (RagServiceException e) {
                    // Nếu file search store không tồn tại (404), trả về thông báo rõ ràng
                    String errorMsg = e.getMessage() != null ? e.getMessage() : "";
                    if (errorMsg.contains("404") || errorMsg.contains("không tồn tại") || 
                        errorMsg.contains("does not exist") || errorMsg.contains("not found")) {
                        log.warn("File Search Store '{}' không tồn tại trên Google. Store có thể đã bị xóa.", 
                            request.getFileStoreName());
                        
                        // Trả về response với thông báo rõ ràng thay vì fallback
                        java.util.Map<String, Object> errorContent = new java.util.HashMap<>();
                        errorContent.put("answer", 
                            "Xin lỗi, tài liệu này không còn khả dụng trên Google File Search. " +
                            "Store có thể đã bị xóa. Vui lòng upload lại document để sử dụng tính năng chat theo tài liệu.");
                        errorContent.put("sources", List.of());
                        
                        return new RagResponse(
                            request.getMode(),
                            request.getLlmProvider(),
                            request.getQueryText(),
                            errorContent,
                            LocalDateTime.now(),
                            0);
                    } else {
                        // Các lỗi khác, throw lại
                        throw e;
                    }
                }
            }

            // Only retrieve documents if explicitly requested
            RetrievalResponse retrievalResponse = null;
            String context = "";

            if (Boolean.TRUE.equals(request.getUseDocuments())) {
                log.info("Retrieving documents for context (useDocuments=true)");
                retrievalResponse = retrieveDocuments(request);
                context = contextService.buildContext(retrievalResponse.getResults());
            } else {
                log.info("Skipping document retrieval (useDocuments=false or not set)");
                // Create empty retrieval response
                retrievalResponse = new RetrievalResponse();
                retrievalResponse.setResults(List.of());
                retrievalResponse.setTotalResults(0);
            }

            // Use fallback-enabled LLM service
            LLMService llmService;
            try {
                llmService = llmServiceFactory.getLLMServiceWithFallback(request.getLlmProvider());
            } catch (Exception e) {
                log.error("All LLM services failed, cannot process query", e);
                throw new RagServiceException("No LLM service available: " + e.getMessage(), e);
            }

            String llmResponse;
            try {
                llmResponse = llmService.generateResponse(request.getQueryText(), context, request.getMode());
            } catch (Exception e) {
                // If we get 429 error, try fallback manually
                if (e.getMessage() != null && (e.getMessage().contains("429") || e.getMessage().contains("capacity exceeded"))) {
                    log.warn("Got 429 error from {}, trying Gemini fallback", request.getLlmProvider());
                    try {
                        llmService = llmServiceFactory.getLLMService(LLMProvider.GEMINI);
                        llmResponse = llmService.generateResponse(request.getQueryText(), context, request.getMode());
                    } catch (Exception fallbackEx) {
                        log.error("Fallback to Gemini also failed", fallbackEx);
                        throw new RagServiceException("All LLM providers failed: " + fallbackEx.getMessage(), fallbackEx);
                    }
                } else {
                    throw e;
                }
            }

            ResponseStrategy strategy = strategyFactory.getStrategy(request.getMode());
            Object formattedResponse = strategy.formatResponse(llmResponse, retrievalResponse.getResults());

            RagResponse response = new RagResponse(
                    request.getMode(),
                    request.getLlmProvider(),
                    request.getQueryText(),
                    formattedResponse,
                    LocalDateTime.now(),
                    retrievalResponse.getTotalResults());

            log.info("RAG query processed successfully. Results used: {}", retrievalResponse.getTotalResults());

            return response;

        } catch (Exception e) {
            log.error("Error processing RAG query", e);
            throw new RagServiceException("Failed to process RAG query: " + e.getMessage(), e);
        }
    }

    private RetrievalResponse retrieveDocuments(RagRequest request) throws RagServiceException {
        log.debug("Retrieving results for query: '{}'", request.getQueryText());

        RetrievalRequest retrievalRequest = RetrievalRequest.builder()
                .documentId(request.getDocumentId())
                .chapterId(request.getChapterId())
                .lessonId(request.getLessonId())
                .queryText(request.getQueryText())
                .useSemantic(request.isUseSemantic())
                .topK(request.getTopK() != null ? request.getTopK() : 7)
                .build();

        List<RetrievalResponse> responseList = retrievalClient.searchChunks(retrievalRequest);

        if (responseList == null || responseList.isEmpty()) {
            log.warn("No responses retrieved for query: '{}'", request.getQueryText());
            // Return empty response
            RetrievalResponse emptyResponse = new RetrievalResponse();
            emptyResponse.setResults(List.of());
            emptyResponse.setTotalResults(0);
            return emptyResponse;
        }

        RetrievalResponse response = responseList.get(0);

        if (response.getResults() == null || response.getResults().isEmpty()) {
            log.warn("No results retrieved for query: '{}'", request.getQueryText());
            return response;
        }

        log.info("Retrieved {} results from retrieval service", response.getTotalResults());
        return response;
    }

    /**
     * Xử lý query với Google File Search
     */
    private RagResponse processFileSearchQuery(RagRequest request) {
        try {
            log.info("Processing file search query for store: {}", request.getFileStoreName());
            
            // Gọi Gemini File Search Service
            com.mss301.ragservice.dto.response.FileSearchQueryResponse fileSearchResponse = 
                fileSearchService.queryWithFileSearch(request.getFileStoreName(), request.getQueryText());
            
            // Format response để phù hợp với RAG response format
            // Tạo một map chứa answer và sources (rỗng vì file-search không trả về sources chi tiết)
            java.util.Map<String, Object> formattedContent = new java.util.HashMap<>();
            formattedContent.put("answer", fileSearchResponse.getAnswer());
            formattedContent.put("sources", List.of()); // File search không có sources chi tiết như retrieval
            
            // Tạo empty retrieval response
            RetrievalResponse retrievalResponse = new RetrievalResponse();
            retrievalResponse.setResults(List.of());
            retrievalResponse.setTotalResults(0);
            
            RagResponse response = new RagResponse(
                    request.getMode(),
                    request.getLlmProvider(),
                    request.getQueryText(),
                    formattedContent,
                    LocalDateTime.now(),
                    0);
            
            log.info("File search query processed successfully");
            return response;
            
        } catch (Exception e) {
            log.error("Error processing file search query for store: {}", request.getFileStoreName(), e);
            
            // Nếu store không tồn tại (404) hoặc lỗi tương tự
            String errorMessage = e.getMessage() != null ? e.getMessage().toLowerCase() : "";
            if (errorMessage.contains("404") || errorMessage.contains("not found") || 
                errorMessage.contains("does not exist") || errorMessage.contains("không tồn tại")) {
                log.warn("File Search Store '{}' does not exist on Google. Store may have been deleted. " +
                        "Consider removing googleFileSearchStoreName from document.", request.getFileStoreName());
                
                // Throw exception với message rõ ràng
                throw new RagServiceException(
                    "HTTP Error 404: File Search Store không tồn tại trên Google. " +
                    "Store có thể đã bị xóa. Vui lòng upload lại document để tạo store mới.", e);
            }
            
            // Các lỗi khác, throw như bình thường
            throw new RagServiceException("Failed to process file search query: " + e.getMessage(), e);
        }
    }
}
