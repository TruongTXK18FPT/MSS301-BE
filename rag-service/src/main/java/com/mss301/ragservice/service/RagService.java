package com.mss301.ragservice.service;

import com.mss301.ragservice.client.RetrievalServiceClient;
import com.mss301.ragservice.dto.external.RetrievalRequest;
import com.mss301.ragservice.dto.external.RetrievalResponse;
import com.mss301.ragservice.dto.request.RagRequest;
import com.mss301.ragservice.dto.response.RagResponse;
import com.mss301.ragservice.exception.RagServiceException;
import com.mss301.ragservice.service.llm.LLMService;
import com.mss301.ragservice.service.llm.LLMServiceFactory;
import com.mss301.ragservice.strategy.ResponseStrategy;
import com.mss301.ragservice.strategy.ResponseStrategyFactory;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.List;

@Slf4j
@Service
@RequiredArgsConstructor
public class RagService {

    private final RetrievalServiceClient retrievalClient;
    private final LLMServiceFactory llmServiceFactory;
    private final ResponseStrategyFactory strategyFactory;
    private final DocumentContextService contextService;


    public RagResponse processQuery(RagRequest request) {
        log.info("Processing RAG query - Mode: {}, Provider: {}, Query: '{}'",
                request.getMode(),
                request.getLlmProvider(),
                request.getQueryText());

        try {
            RetrievalResponse retrievalResponse = retrieveDocuments(request);

            String context = contextService.buildContext(retrievalResponse.getResults());

            LLMService llmService = llmServiceFactory.getLLMService(request.getLlmProvider());

            String llmResponse = llmService.generateResponse(
                    request.getQueryText(),
                    context,
                    request.getMode()
            );

            ResponseStrategy strategy = strategyFactory.getStrategy(request.getMode());
            Object formattedResponse = strategy.formatResponse(llmResponse, retrievalResponse.getResults());

            RagResponse response = new RagResponse(
                    request.getMode(),
                    request.getLlmProvider(),
                    request.getQueryText(),
                    formattedResponse,
                    LocalDateTime.now(),
                    retrievalResponse.getTotalResults()
            );

            log.info("RAG query processed successfully. Results used: {}",
                    retrievalResponse.getTotalResults());

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
}