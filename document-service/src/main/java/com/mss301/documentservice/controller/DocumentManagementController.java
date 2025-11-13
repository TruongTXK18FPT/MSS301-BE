package com.mss301.documentservice.controller;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.stream.Collectors;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import com.mss301.documentservice.dto.ApiResponse;
import com.mss301.documentservice.dto.google.FileSearchStoreResponse;
import com.mss301.documentservice.dto.response.*;
import com.mss301.documentservice.entity.Chunk;
import com.mss301.documentservice.entity.Document;
import com.mss301.documentservice.entity.ProcessingJob;
import com.mss301.documentservice.entity.enums.DocumentStatus;
import com.mss301.documentservice.service.chunk.ChunkingService;
import com.mss301.documentservice.service.document.DocumentService;

import io.swagger.v3.oas.annotations.Operation;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@RestController
@RequestMapping("/api/v1/documents")
@RequiredArgsConstructor
@Slf4j
// @CrossOrigin removed - CORS is handled by gateway-service
// Adding @CrossOrigin here causes duplicate CORS headers
public class DocumentManagementController {

    private final DocumentService documentService;
    private final ChunkingService chunkingService;

    @PostMapping("/upload")
    @Operation(summary = "Upload a PDF document", description = "Uploads a PDF document and initiates processing.")
    public ResponseEntity<ApiResponse<DocumentResponseDto>> uploadPdf(
            @RequestParam("file") MultipartFile file,
            @RequestParam(value = "title", required = false) String title,
            @RequestParam(value = "description", required = false) String description,
            Authentication authentication) {

        try {
            Long userId = getUserIdFromAuthentication(authentication);
            log.info("User {} uploading document: {}", userId, file.getOriginalFilename());

            Document document = documentService.uploadPdf(file, title, description);
            DocumentResponseDto responseDto = DocumentResponseDto.fromEntity(document);

            return ResponseEntity.ok(ApiResponse.success("PDF uploaded successfully", responseDto));

        } catch (IllegalArgumentException e) {
            return ResponseEntity.badRequest().body(ApiResponse.error(e.getMessage()));
        } catch (Exception e) {
            log.error("Error uploading PDF", e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(ApiResponse.error("Failed to upload PDF: " + e.getMessage()));
        }
    }

    @PostMapping("/{documentId}/process")
    @Operation(
            summary = "Trigger document processing",
            description = "Triggers processing for the specified document by its ID.")
    public ResponseEntity<ApiResponse<DocumentResponseDto.ProcessingJobDto>> triggerProcessing(
            @PathVariable String documentId,
            Authentication authentication) {
        try {
            Long userId = getUserIdFromAuthentication(authentication);
            log.info("User {} triggering processing for document: {}", userId, documentId);

            ProcessingJob job = documentService.triggerProcessing(documentId);
            DocumentResponseDto.ProcessingJobDto jobDto = DocumentResponseDto.fromEntity(job);

            return ResponseEntity.ok(ApiResponse.success("Processing started successfully", jobDto));

        } catch (IllegalArgumentException | IllegalStateException e) {
            return ResponseEntity.badRequest().body(ApiResponse.error(e.getMessage()));
        } catch (Exception e) {
            log.error("Error triggering processing for document: {}", documentId, e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(ApiResponse.error("Failed to start processing: " + e.getMessage()));
        }
    }

    @GetMapping
    @Operation(summary = "Get all documents", description = "Retrieves all documents, optionally filtered by status.")
    public ResponseEntity<ApiResponse<DocumentListDto>> getAllDocuments(
            @RequestParam(value = "status", required = false) String status,
            Authentication authentication) {

        try {
            Long userId = getUserIdFromAuthentication(authentication);
            log.info("User {} fetching all documents with status: {}", userId, status);

            List<Document> documents;

            if (status != null) {
                DocumentStatus documentStatus = DocumentStatus.valueOf(status.toUpperCase());
                documents = documentService.getDocumentsByStatus(documentStatus);
            } else {
                documents = documentService.getAllDocuments();
            }

            List<DocumentResponseDto> documentDtos =
                    documents.stream().map(DocumentResponseDto::fromEntity).collect(Collectors.toList());

            DocumentListDto documentListDto = DocumentListDto.of(documentDtos);
            return ResponseEntity.ok(ApiResponse.success(documentListDto));

        } catch (IllegalArgumentException e) {
            return ResponseEntity.badRequest()
                    .body(ApiResponse.error(
                            "Invalid status: " + status + ". Valid values: " + String.join(", ", getStatusNames())));
        } catch (Exception e) {
            log.error("Error fetching documents", e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(ApiResponse.error("Failed to fetch documents: " + e.getMessage()));
        }
    }

    @GetMapping("/{documentId}/status")
    @Operation(
            summary = "Get document processing status",
            description = "Retrieves the processing status for the specified document by its ID.")
    public ResponseEntity<ApiResponse<ProcessingStatusDto>> getProcessingStatus(
            @PathVariable String documentId,
            Authentication authentication) {
        try {
            Long userId = getUserIdFromAuthentication(authentication);
            log.info("User {} fetching processing status for document: {}", userId, documentId);

            Optional<ProcessingJob> jobOpt = documentService.getProcessingStatus(documentId);

            ProcessingStatusDto statusDto;
            if (jobOpt.isPresent()) {
                DocumentResponseDto.ProcessingJobDto jobDto = DocumentResponseDto.fromEntity(jobOpt.get());
                statusDto = ProcessingStatusDto.withJob(jobDto);
            } else {
                statusDto = ProcessingStatusDto.noJob();
            }

            String message = jobOpt.isPresent() ? null : "No processing job found for this document";
            return ResponseEntity.ok(ApiResponse.success(message, statusDto));

        } catch (Exception e) {
            log.error("Error fetching processing status for document: {}", documentId, e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(ApiResponse.error("Failed to fetch processing status: " + e.getMessage()));
        }
    }

    @DeleteMapping("/{documentId}")
    @Operation(
            summary = "Delete a document",
            description = "Deletes the specified document and all associated data by its ID.")
    public ResponseEntity<ApiResponse<Void>> deleteDocument(
            @PathVariable String documentId,
            Authentication authentication) {
        try {
            Long userId = getUserIdFromAuthentication(authentication);
            log.info("User {} deleting document: {}", userId, documentId);

            documentService.deleteDocument(documentId);
            return ResponseEntity.ok(ApiResponse.success("Document deleted successfully", null));

        } catch (IllegalArgumentException e) {
            return ResponseEntity.notFound().build();
        } catch (Exception e) {
            log.error("Error deleting document: {}", documentId, e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(ApiResponse.error("Failed to delete document: " + e.getMessage()));
        }
    }

    @GetMapping("/statuses")
    @Operation(summary = "Get available document statuses", description = "Retrieves all possible document statuses.")
    public ResponseEntity<ApiResponse<String[]>> getAvailableStatuses() {
        return ResponseEntity.ok(ApiResponse.success(getStatusNames()));
    }

    @GetMapping("/{documentId}")
    @Operation(
            summary = "Get document by ID",
            description = "Retrieves the specified document by its ID, including processing status if available.")
    public ResponseEntity<ApiResponse<DocumentResponseDto>> getDocumentById(
            @PathVariable String documentId,
            Authentication authentication) {
        try {
            Long userId = getUserIdFromAuthentication(authentication);
            log.info("User {} fetching document: {}", userId, documentId);

            Optional<Document> documentOpt = documentService.getDocumentById(documentId);

            if (documentOpt.isEmpty()) {
                return ResponseEntity.notFound().build();
            }

            Document document = documentOpt.get();
            DocumentResponseDto responseDto = DocumentResponseDto.fromEntity(document);

            Optional<ProcessingJob> jobOpt = documentService.getProcessingStatus(documentId);
            if (jobOpt.isPresent()) {
                responseDto.setProcessingJob(DocumentResponseDto.fromEntity(jobOpt.get()));
            }

            return ResponseEntity.ok(ApiResponse.success(responseDto));

        } catch (Exception e) {
            log.error("Error fetching document: {}", documentId, e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(ApiResponse.error("Failed to fetch document: " + e.getMessage()));
        }
    }

    @GetMapping("/{documentId}/chunks")
    @Operation(
            summary = "Get document chunks",
            description =
                    "Retrieves chunks for the specified document by its ID, with optional filtering by chapter and lesson, and pagination.")
    public ResponseEntity<ApiResponse<PaginatedChunksDto>> getDocumentChunks(
            @PathVariable String documentId,
            @RequestParam(value = "chapter", required = false) Integer chapterNumber,
            @RequestParam(value = "lesson", required = false) Integer lessonNumber,
            @RequestParam(value = "page", defaultValue = "0") int page,
            @RequestParam(value = "size", defaultValue = "20") int size,
            Authentication authentication) {

        try {
            Long userId = getUserIdFromAuthentication(authentication);
            log.info("User {} fetching chunks for document: {}", userId, documentId);

            List<Chunk> chunks;

            if (chapterNumber != null && lessonNumber != null) {
                chunks = chunkingService.findByDocumentIdAndStructure_ChapterNumberAndStructure_LessonNumber(
                        documentId, chapterNumber, lessonNumber);
            } else if (chapterNumber != null) {
                chunks = chunkingService.findByDocumentIdAndStructure_ChapterNumber(documentId, chapterNumber);
            } else {
                chunks = chunkingService.findByDocumentIdOrderByChunkIndex(documentId);
            }

            int start = page * size;
            int end = Math.min(start + size, chunks.size());
            List<Chunk> paginatedChunks = chunks.subList(start, end);

            List<ChunkDto> chunkDtos =
                    paginatedChunks.stream().map(ChunkDto::fromEntity).collect(Collectors.toList());

            PaginationDto pagination = PaginationDto.of(page, size, chunks.size());

            PaginatedChunksDto response = PaginatedChunksDto.of(chunkDtos, pagination);
            return ResponseEntity.ok(ApiResponse.success(response));

        } catch (Exception e) {
            log.error("Error getting chunks for document: {}", documentId, e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(ApiResponse.error("Failed to get chunks: " + e.getMessage()));
        }
    }

    @GetMapping("/{documentId}/structure")
    @Operation(
            summary = "Get document structure",
            description =
                    "Retrieves the hierarchical structure of the specified document by its ID, including chapters and lessons.")
    public ResponseEntity<ApiResponse<DocumentStructureDto>> getDocumentStructure(
            @PathVariable String documentId,
            Authentication authentication) {
        try {
            Long userId = getUserIdFromAuthentication(authentication);
            log.info("User {} fetching structure for document: {}", userId, documentId);

            List<Chunk> chunks = chunkingService.findByDocumentIdOrderByChunkIndex(documentId);

            Map<Integer, Map<String, Object>> chapters = new HashMap<>();

            for (Chunk chunk : chunks) {
                if (chunk.getStructure() != null && chunk.getStructure().getChapterNumber() != null) {
                    chapters.computeIfAbsent(chunk.getStructure().getChapterNumber(), k -> {
                        Map<String, Object> chapter = new HashMap<>();
                        chapter.put("number", chunk.getStructure().getChapterNumber());
                        chapter.put("title", chunk.getStructure().getChapterTitle());
                        chapter.put("lessons", new HashMap<Integer, Map<String, Object>>());
                        return chapter;
                    });

                    if (chunk.getStructure().getLessonNumber() != null) {
                        @SuppressWarnings("unchecked")
                        Map<Integer, Map<String, Object>> lessons = (Map<Integer, Map<String, Object>>)
                                chapters.get(chunk.getStructure().getChapterNumber())
                                        .get("lessons");

                        lessons.computeIfAbsent(chunk.getStructure().getLessonNumber(), k -> {
                            Map<String, Object> lesson = new HashMap<>();
                            lesson.put("number", chunk.getStructure().getLessonNumber());
                            lesson.put("title", chunk.getStructure().getLessonTitle());
                            lesson.put("id", chunk.getStructure().getLessonId());
                            lesson.put("chunkCount", 0);
                            return lesson;
                        });

                        Map<String, Object> lesson =
                                lessons.get(chunk.getStructure().getLessonNumber());
                        lesson.put("chunkCount", (Integer) lesson.get("chunkCount") + 1);
                    }
                }
            }

            List<DocumentStructureDto.ChapterDto> structuredChapters = chapters.values().stream()
                    .sorted((a, b) -> Integer.compare((Integer) a.get("number"), (Integer) b.get("number")))
                    .map(chapterMap -> {
                        @SuppressWarnings("unchecked")
                        Map<Integer, Map<String, Object>> lessons =
                                (Map<Integer, Map<String, Object>>) chapterMap.get("lessons");

                        List<DocumentStructureDto.LessonDto> lessonDtos = lessons.values().stream()
                                .sorted((a, b) -> Integer.compare((Integer) a.get("number"), (Integer) b.get("number")))
                                .map(lessonMap -> DocumentStructureDto.LessonDto.builder()
                                        .number((Integer) lessonMap.get("number"))
                                        .title((String) lessonMap.get("title"))
                                        .id((String) lessonMap.get("id"))
                                        .chunkCount((Integer) lessonMap.get("chunkCount"))
                                        .build())
                                .collect(Collectors.toList());

                        return DocumentStructureDto.ChapterDto.builder()
                                .number((Integer) chapterMap.get("number"))
                                .title((String) chapterMap.get("title"))
                                .lessons(lessonDtos)
                                .build();
                    })
                    .collect(Collectors.toList());

            DocumentStructureDto structureDto = DocumentStructureDto.builder()
                    .documentId(documentId)
                    .totalChunks(chunks.size())
                    .structure(structuredChapters)
                    .build();

            return ResponseEntity.ok(ApiResponse.success(structureDto));

        } catch (Exception e) {
            log.error("Error getting document structure: {}", documentId, e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(ApiResponse.error("Failed to get document structure: " + e.getMessage()));
        }
    }

    @GetMapping("/chunks/{chunkId}")
    @Operation(summary = "Get chunk by ID", description = "Retrieves the specified chunk by its ID.")
    public ResponseEntity<ApiResponse<ChunkDto>> getChunkById(
            @PathVariable String chunkId,
            Authentication authentication) {
        try {
            Long userId = getUserIdFromAuthentication(authentication);
            log.info("User {} fetching chunk: {}", userId, chunkId);

            Optional<Chunk> chunkOpt = chunkingService.findById(chunkId);

            if (chunkOpt.isEmpty()) {
                return ResponseEntity.notFound().build();
            }

            Chunk chunk = chunkOpt.get();
            ChunkDto chunkDto = ChunkDto.fromEntity(chunk);

            return ResponseEntity.ok(ApiResponse.success(chunkDto));

        } catch (Exception e) {
            log.error("Error fetching chunk: {}", chunkId, e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(ApiResponse.error("Failed to fetch chunk: " + e.getMessage()));
        }
    }

    @GetMapping("/{documentId}/chunks/search")
    @Operation(
            summary = "Search document chunks",
            description = "Searches for chunks within the specified document by its ID that match the given query.")
    public ResponseEntity<ApiResponse<ChunkSearchDto>> searchChunks(
            @PathVariable String documentId,
            @RequestParam("q") String query,
            Authentication authentication) {

        try {
            Long userId = getUserIdFromAuthentication(authentication);
            log.info("User {} searching chunks in document: {} with query: {}", userId, documentId, query);

            List<Chunk> allChunks = chunkingService.findByDocumentIdOrderByChunkIndex(documentId);

            // Simple text search (you can enhance this with Elasticsearch full-text search)
            List<Chunk> matchingChunks = allChunks.stream()
                    .filter(chunk -> chunk.getContent().toLowerCase().contains(query.toLowerCase()))
                    .collect(Collectors.toList());

            ChunkSearchDto searchResult = ChunkSearchDto.of(query, matchingChunks);
            return ResponseEntity.ok(ApiResponse.success(searchResult));

        } catch (Exception e) {
            log.error("Error searching chunks for document: {}", documentId, e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(ApiResponse.error("Failed to search chunks: " + e.getMessage()));
        }
    }

    @GetMapping("/{documentId}/toc-analysis")
    @Operation(
            summary = "Analyze Table of Contents",
            description = "Analyzes the document to extract and summarize its table of contents.")
    public ResponseEntity<ApiResponse<TocAnalysisDto>> getTableOfContentsAnalysis(
            @PathVariable String documentId,
            Authentication authentication) {
        try {
            Long userId = getUserIdFromAuthentication(authentication);
            log.info("User {} analyzing table of contents for document: {}", userId, documentId);

            List<Chunk> chunks = chunkingService.findByDocumentIdOrderByChunkIndex(documentId);

            if (chunks.isEmpty()) {
                TocAnalysisDto emptyAnalysis = TocAnalysisDto.of(documentId, 0, "");
                return ResponseEntity.ok(ApiResponse.success("No content found for this document", emptyAnalysis));
            }

            String fullContent = chunks.stream().map(Chunk::getContent).collect(Collectors.joining("\n\n"));

            String preview = fullContent.length() > 1000 ? fullContent.substring(0, 1000) + "..." : fullContent;

            TocAnalysisDto analysisDto = TocAnalysisDto.of(documentId, fullContent.length(), preview);
            return ResponseEntity.ok(ApiResponse.success(analysisDto));

        } catch (Exception e) {
            log.error("Error analyzing table of contents for document: {}", documentId, e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(ApiResponse.error("Failed to analyze table of contents: " + e.getMessage()));
        }
    }

    @GetMapping("/google/file-search-stores")
    @Operation(
            summary = "List all Google File Search Stores",
            description = "Retrieves all File Search Stores from Google (uploaded documents)")
    public ResponseEntity<ApiResponse<List<FileSearchStoreResponse>>> listGoogleFileSearchStores(
            Authentication authentication) {
        try {
            Long userId = getUserIdFromAuthentication(authentication);
            log.info("User {} listing Google File Search Stores", userId);

            List<FileSearchStoreResponse> stores = documentService.listGoogleFileSearchStores();
            return ResponseEntity.ok(ApiResponse.success("Retrieved " + stores.size() + " File Search Stores", stores));

        } catch (Exception e) {
            log.error("Error listing Google File Search Stores", e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(ApiResponse.error("Failed to list File Search Stores: " + e.getMessage()));
        }
    }

    @GetMapping("/google/file-search-stores/{storeName}")
    @Operation(
            summary = "Get Google File Search Store details",
            description = "Retrieves details of a specific File Search Store by its name")
    public ResponseEntity<ApiResponse<FileSearchStoreResponse>> getGoogleFileSearchStore(
            @PathVariable String storeName,
            Authentication authentication) {
        try {
            Long userId = getUserIdFromAuthentication(authentication);
            log.info("User {} getting Google File Search Store: {}", userId, storeName);

            FileSearchStoreResponse store = documentService.getGoogleFileSearchStore(storeName);
            return ResponseEntity.ok(ApiResponse.success(store));

        } catch (Exception e) {
            log.error("Error getting Google File Search Store: {}", storeName, e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(ApiResponse.error("Failed to get File Search Store: " + e.getMessage()));
        }
    }

    private String[] getStatusNames() {
        DocumentStatus[] statuses = DocumentStatus.values();
        String[] names = new String[statuses.length];
        for (int i = 0; i < statuses.length; i++) {
            names[i] = statuses[i].name();
        }
        return names;
    }

    private Long getUserIdFromAuthentication(Authentication authentication) {
        if (authentication == null || authentication.getPrincipal() == null) {
            throw new RuntimeException("User not authenticated");
        }

        // Extract user ID from JWT token claims
        try {
            return Long.parseLong(authentication.getName());
        } catch (NumberFormatException e) {
            throw new RuntimeException("Invalid user ID in token");
        }
    }
}
