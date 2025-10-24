package com.mss301.retrievalservice.service.impl;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import com.mss301.retrievalservice.dto.request.EmbeddingRequest;
import com.mss301.retrievalservice.dto.request.RetrievalRequest;
import com.mss301.retrievalservice.dto.response.EmbeddingResponse;
import com.mss301.retrievalservice.dto.response.RetrievalResult;
import com.mss301.retrievalservice.service.EmbeddingClient;
import com.mss301.retrievalservice.service.RetrievalService;

import co.elastic.clients.elasticsearch.ElasticsearchClient;
import co.elastic.clients.elasticsearch._types.query_dsl.*;
import co.elastic.clients.elasticsearch.core.SearchRequest;
import co.elastic.clients.elasticsearch.core.SearchResponse;
import co.elastic.clients.elasticsearch.core.search.Hit;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@Service
@RequiredArgsConstructor
@Slf4j
public class RetrievalServiceImpl implements RetrievalService {

    private final EmbeddingClient embeddingService;
    private final ElasticsearchClient esClient;

    private static final String CHUNKS_INDEX = "chunks";

    @Value("${embedding.client.model:text-embedding-3-small}")
    private String embeddingModel;

    @Override
    public List<RetrievalResult> query(RetrievalRequest request) throws IOException {
        log.info(
                "Processing retrieval request: useSemantic={}, documentId={}, chapterId={}, lessonId={}, queryText={}",
                request.isUseSemantic(),
                request.getDocumentId(),
                request.getChapterId(),
                request.getLessonId(),
                request.getQueryText() != null ? "provided" : "null");

        if (request.isUseSemantic()
                && (request.getQueryText() == null
                        || request.getQueryText().trim().isEmpty())) {
            throw new IllegalArgumentException("Query text is required for semantic search");
        }

        Query scopeFilter = buildScopeFilter(request);

        if (request.isUseSemantic()) {
            return performSemanticSearch(request, scopeFilter);
        } else {
            return performKeywordOrFilterSearch(request, scopeFilter);
        }
    }

    private Query buildScopeFilter(RetrievalRequest request) {
        BoolQuery.Builder filterBuilder = new BoolQuery.Builder();

        if (request.getDocumentId() != null && !request.getDocumentId().trim().isEmpty()) {
            String originalDocumentId = request.getDocumentId();

            final String documentId = originalDocumentId.endsWith("/chunks")
                    ? originalDocumentId.substring(0, originalDocumentId.length() - 7)
                    : originalDocumentId;

            if (!documentId.equals(originalDocumentId)) {
                log.debug("Cleaned documentId from {} to {}", originalDocumentId, documentId);
            }

            filterBuilder.must(TermQuery.of(t -> t.field("documentId.keyword").value(documentId))
                    ._toQuery());
            log.debug("Added documentId filter for: {}", documentId);
        }

        if (request.getChapterId() != null && !request.getChapterId().trim().isEmpty()) {
            filterBuilder.must(
                    TermQuery.of(t -> t.field("structure.chapterId.keyword").value(request.getChapterId()))
                            ._toQuery());
            log.debug("Added chapterId filter: {}", request.getChapterId());

            if (request.getLessonId() != null && !request.getLessonId().trim().isEmpty()) {
                filterBuilder.must(
                        TermQuery.of(t -> t.field("structure.lessonId.keyword").value(request.getLessonId()))
                                ._toQuery());
                log.debug("Added lessonId filter: {}", request.getLessonId());
            }
        }

        return filterBuilder.build()._toQuery();
    }

    private List<RetrievalResult> performSemanticSearch(RetrievalRequest request, Query scopeFilter)
            throws IOException {
        log.info("Performing semantic search for query: {}", request.getQueryText());

        try {
            EmbeddingRequest embedRequest = new EmbeddingRequest();
            embedRequest.setModel(embeddingModel);
            embedRequest.setInput(request.getQueryText());

            EmbeddingResponse embedResponse = embeddingService.createEmbedding(embedRequest);

            if (embedResponse == null
                    || embedResponse.getData() == null
                    || embedResponse.getData().isEmpty()) {
                log.warn("Embedding service returned null or empty response, falling back to keyword search");
                return performKeywordOrFilterSearch(request, scopeFilter);
            }

            List<Double> queryEmbedding = embedResponse.getData().get(0).getEmbedding();

            if (queryEmbedding == null || queryEmbedding.isEmpty()) {
                log.warn("Embedding vector is null or empty, falling back to keyword search");
                return performKeywordOrFilterSearch(request, scopeFilter);
            }

            log.debug("Generated embedding vector with {} dimensions", queryEmbedding.size());

            List<Float> embeddingFloats =
                    queryEmbedding.stream().map(Double::floatValue).toList();

            SearchRequest searchRequest = SearchRequest.of(s -> s.index(CHUNKS_INDEX)
                    .size(request.getTopK())
                    .knn(knn -> knn.field("embedding")
                            .queryVector(embeddingFloats)
                            .k(request.getTopK())
                            .numCandidates(Math.max(request.getTopK() * 10, 100))
                            .filter(scopeFilter))
                    .source(src -> src.filter(f -> f.includes("content", "documentId", "structure", "chunkIndex"))));

            @SuppressWarnings("unchecked")
            SearchResponse<Map<String, Object>> response =
                    (SearchResponse<Map<String, Object>>) (SearchResponse<?>) esClient.search(searchRequest, Map.class);
            log.info("Semantic search returned {} hits", response.hits().hits().size());

            return mapSearchResponseToResults(response);

        } catch (Exception e) {
            log.error("Embedding service failed, fallback to keyword search: {}", e.getMessage());
            // Fallback to keyword search if embedding service is unavailable
            return performKeywordOrFilterSearch(request, scopeFilter);
        }
    }

    private List<RetrievalResult> performKeywordOrFilterSearch(RetrievalRequest request, Query scopeFilter)
            throws IOException {
        BoolQuery.Builder queryBuilder = new BoolQuery.Builder();

        queryBuilder.filter(scopeFilter);

        if (request.getQueryText() != null && !request.getQueryText().trim().isEmpty()) {
            log.info("Performing keyword search for query: {}", request.getQueryText());

            MultiMatchQuery textQuery = MultiMatchQuery.of(m -> m.query(request.getQueryText())
                    .fields("content^2", "summary^1") // Boost content field
                    .type(TextQueryType.BestFields)
                    .fuzziness("AUTO"));

            queryBuilder.must(textQuery._toQuery());
        } else {
            log.info("Performing filter-only search");
            queryBuilder.must(MatchAllQuery.of(m -> m)._toQuery());
        }

        SearchRequest searchRequest = SearchRequest.of(s -> s.index(CHUNKS_INDEX)
                .size(request.getTopK())
                .query(queryBuilder.build()._toQuery())
                .sort(sort -> sort.field(f -> f.field("structure.chapterNumber")
                        .order(co.elastic.clients.elasticsearch._types.SortOrder.Asc)))
                .sort(sort -> sort.field(f ->
                        f.field("structure.lessonNumber").order(co.elastic.clients.elasticsearch._types.SortOrder.Asc)))
                .sort(sort -> sort.field(
                        f -> f.field("chunkIndex").order(co.elastic.clients.elasticsearch._types.SortOrder.Asc)))
                .source(src -> src.filter(f -> f.includes("content", "documentId", "structure", "chunkIndex"))));

        @SuppressWarnings("unchecked")
        SearchResponse<Map<String, Object>> response =
                (SearchResponse<Map<String, Object>>) (SearchResponse<?>) esClient.search(searchRequest, Map.class);
        log.info(
                "Keyword/filter search returned {} hits", response.hits().hits().size());

        return mapSearchResponseToResults(response);
    }

    private List<RetrievalResult> mapSearchResponseToResults(SearchResponse<Map<String, Object>> response) {
        List<RetrievalResult> results = new ArrayList<>();

        for (Hit<Map<String, Object>> hit : response.hits().hits()) {
            Map<String, Object> source = hit.source();
            if (source != null) {
                // Extract structure object
                @SuppressWarnings("unchecked")
                Map<String, Object> structure = (Map<String, Object>) source.get("structure");

                RetrievalResult result = new RetrievalResult(
                        getStringValue(source, "content"),
                        hit.score() != null ? hit.score() : 0.0,
                        getStringValue(source, "documentId"),
                        structure != null ? getStringValue(structure, "chapterId") : null,
                        structure != null ? getStringValue(structure, "lessonId") : null,
                        structure != null ? getStringValue(structure, "chapterTitle") : null,
                        structure != null ? getStringValue(structure, "lessonTitle") : null,
                        structure != null ? getLongValue(structure, "pageNumber") : null);

                results.add(result);
                log.debug(
                        "Mapped result: chunkId={}, score={}, content={}",
                        hit.id(),
                        result.getScore(),
                        result.getContent() != null
                                ? result.getContent()
                                        .substring(
                                                0,
                                                Math.min(50, result.getContent().length()))
                                : "null");
            }
        }

        log.info("Successfully mapped {} results", results.size());
        return results;
    }

    private String getStringValue(Map<String, Object> source, String key) {
        Object value = source.get(key);
        return value != null ? value.toString() : null;
    }

    private Long getLongValue(Map<String, Object> source, String key) {
        Object value = source.get(key);
        if (value instanceof Number) {
            return ((Number) value).longValue();
        }
        return null;
    }
}
