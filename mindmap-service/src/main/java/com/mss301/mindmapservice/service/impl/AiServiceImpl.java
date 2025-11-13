package com.mss301.mindmapservice.service.impl;

import java.time.Duration;
import java.time.LocalDateTime;
import java.util.*;
import java.util.stream.Collectors;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.reactive.function.client.WebClient;
import org.springframework.web.reactive.function.client.WebClientResponseException;

import com.fasterxml.jackson.core.io.JsonEOFException;
import com.fasterxml.jackson.core.JsonParseException;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.mss301.mindmapservice.dto.ai.*;
import com.mss301.mindmapservice.dto.request.AiGenerateMindmapRequest;
import com.mss301.mindmapservice.dto.response.AiGenerateMindmapResponse;
import com.mss301.mindmapservice.entity.*;
import com.mss301.mindmapservice.repository.*;
import com.mss301.mindmapservice.service.AiService;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@Service
@RequiredArgsConstructor
@Slf4j
public class AiServiceImpl implements AiService {

    private final WebClient.Builder webClientBuilder;
    private final ObjectMapper objectMapper;
    private final MindmapRepository mindmapRepository;
    private final MindmapNodeRepository mindmapNodeRepository;
    private final MindmapEdgeRepository mindmapEdgeRepository;
    private final ConceptRepository conceptRepository;
    private final FormulaRepository formulaRepository;
    private final ExerciseRepository exerciseRepository;

    @Value("${ai.gemini.api-key:AIzaSyDtwnAbU30A1r_ixXtuZLZusEcmLa8tzNg}")
    private String geminiApiKey;

    @Value("${ai.gemini.base-url:https://generativelanguage.googleapis.com/v1beta}")
    private String geminiBaseUrl;

    @Value("${ai.gemini.models.primary:gemini-2.5-flash}")
    private String geminiPrimaryModel;
    
    @Value("${ai.gemini.models.fallback-1:gemini-1.5-flash}")
    private String geminiFallback1Model;
    
    @Value("${ai.gemini.models.fallback-2:gemini-2.0-flash}")
    private String geminiFallback2Model;

    @Value("${ai.service.max-retries:3}")
    private int maxRetries;

    @Value("${ai.service.retry-delay:1000}")
    private long retryDelay;

    @Override
    @Transactional
    public AiGenerateMindmapResponse generateMindmap(AiGenerateMindmapRequest request, Long userId) {
        long startTime = System.currentTimeMillis();
        log.info("Generating mindmap with Direct AI (Gemini) for user: {} with topic: {}", userId, request.getTopic());

        try {
            // 1. Create mindmap entity
            Mindmap mindmap = createMindmapEntity(request, userId);
            Mindmap savedMindmap = mindmapRepository.save(mindmap);
            log.info("Created mindmap entity with ID: {}", savedMindmap.getId());

            // 2. Build comprehensive prompt
            String prompt = buildComprehensiveMindmapPrompt(request);
            log.debug("Built prompt with length: {} characters", prompt.length());

            // 3. Call Gemini API
            String aiJsonResponse = callGeminiApi(prompt);
            log.info("Received AI response with length: {} characters", aiJsonResponse.length());
            log.debug("AI response (first 500 chars): {}", aiJsonResponse.substring(0, Math.min(500, aiJsonResponse.length())));

            // 4. Parse nodes from AI response
            List<MindmapNode> nodes = parseNodesFromAiResponse(savedMindmap, aiJsonResponse);
            log.info("Parsed {} nodes from AI response", nodes.size());

            // 5. Save nodes to get IDs
            List<MindmapNode> savedNodes = mindmapNodeRepository.saveAll(nodes);
            mindmapNodeRepository.flush();
            log.info("Saved {} nodes to database", savedNodes.size());

            // 6. Set parent relationships based on level
            setParentRelationships(savedNodes);
            mindmapNodeRepository.saveAll(savedNodes);
            mindmapNodeRepository.flush();
            log.info("Updated parent relationships for nodes");

            // 7. Generate edges from parent relationships
            List<MindmapEdge> edges = generateEdgesFromNodes(savedMindmap, savedNodes);
            mindmapEdgeRepository.saveAll(edges);
            mindmapEdgeRepository.flush();
            log.info("Generated and saved {} edges", edges.size());

            // 8. Parse and save concepts, formulas, exercises from AI response
            parseAndSaveRelatedEntities(aiJsonResponse, savedNodes);

            long processingTime = System.currentTimeMillis() - startTime;
            log.info("Mindmap generation completed in {}ms with {} nodes and {} edges", 
                processingTime, savedNodes.size(), edges.size());

            return AiGenerateMindmapResponse.builder()
                    .mindmapId(savedMindmap.getId())
                    .title(savedMindmap.getTitle())
                    .description(savedMindmap.getDescription())
                    .aiProvider("gemini")
                    .aiModel(request.getAiModel() != null ? request.getAiModel() : geminiPrimaryModel)
                    .status("SUCCESS")
                    .nodesGenerated(savedNodes.size())
                    .edgesGenerated(edges.size())
                    .createdAt(LocalDateTime.now())
                    .build();

        } catch (Exception e) {
            log.error("Failed to generate mindmap with Direct AI: {}", e.getMessage(), e);
            return AiGenerateMindmapResponse.builder()
                    .status("FAILED")
                    .errorMessage(e.getMessage())
                    .createdAt(LocalDateTime.now())
                    .build();
        }
    }

    @Override
    public List<MindmapNode> generateNodes(Mindmap mindmap, AiGenerateMindmapRequest request) {
        // This method is deprecated - use generateMindmap() instead
        log.warn("generateNodes() is deprecated, use generateMindmap() for full generation");
        return new ArrayList<>();
    }

    @Override
    public List<MindmapEdge> generateEdges(Mindmap mindmap, List<MindmapNode> nodes, AiGenerateMindmapRequest request) {
        // This method is deprecated - use generateMindmap() instead
        log.warn("generateEdges() is deprecated, use generateMindmap() for full generation");
        return new ArrayList<>();
    }

    @Override
    public List<String> getAvailableModels(String provider) {
        if ("gemini".equalsIgnoreCase(provider)) {
            return Arrays.asList(
                geminiPrimaryModel,      // gemini-2.5-flash
                geminiFallback1Model,    // gemini-1.5-flash
                geminiFallback2Model     // gemini-2.0-flash
            );
        }
        return new ArrayList<>();
    }

    @Override
    public boolean isServiceHealthy(String provider) {
        if (!"gemini".equalsIgnoreCase(provider)) {
            return false;
        }

        try {
            String testPrompt = "Test: Reply with 'OK'";
            callGeminiApi(testPrompt);
            return true;
        } catch (Exception e) {
            log.warn("Gemini service health check failed", e);
            return false;
        }
    }

    /**
     * Generate exercises for a specific mindmap node using Gemini AI
     */
    public List<Exercise> generateExercisesForNode(Long nodeId, String topic, String difficulty,
                                                   String cognitiveLevel, Integer numberOfExercises, Long userId) {
        log.info("Generating {} exercises for node: {} with topic: {}", numberOfExercises, nodeId, topic);

        try {
            // 1. Verify node exists
            MindmapNode node = mindmapNodeRepository.findById(nodeId)
                    .orElseThrow(() -> new RuntimeException("Node not found with id: " + nodeId));

            // 2. Build comprehensive prompt
            String prompt = buildExerciseGenerationPrompt(topic, difficulty, cognitiveLevel, numberOfExercises, node);
            log.debug("Built exercise prompt with length: {} characters", prompt.length());

            // 3. Call Gemini API
            String aiJsonResponse = callGeminiApi(prompt);
            log.info("Received AI response with length: {} characters", aiJsonResponse.length());

            // 4. Parse exercises from response
            List<Exercise> exercises = parseExercisesFromAiResponse(aiJsonResponse, nodeId, userId);
            log.info("Parsed {} exercises from AI response", exercises.size());

            // 5. Save exercises
            List<Exercise> savedExercises = exerciseRepository.saveAll(exercises);
            exerciseRepository.flush();
            log.info("Saved {} exercises to database", savedExercises.size());

            return savedExercises;

        } catch (Exception e) {
            log.error("Failed to generate exercises for node {}: {}", nodeId, e.getMessage(), e);
            throw new RuntimeException("Failed to generate exercises: " + e.getMessage());
        }
    }

    /**
     * Build comprehensive prompt for exercise generation
     */
    private String buildExerciseGenerationPrompt(String topic, String difficulty, String cognitiveLevel,
                                                  Integer numberOfExercises, MindmapNode node) {
        StringBuilder prompt = new StringBuilder();

        // System instruction
        prompt.append("Bạn là một chuyên gia giáo dục toán học với hơn 20 năm kinh nghiệm giảng dạy tại Việt Nam. ");
        prompt.append("Nhiệm vụ của bạn là tạo bài tập toán học chi tiết và phù hợp dựa trên yêu cầu.\n\n");

        // Requirements
        prompt.append("YÊU CẦU BÀI TẬP:\n");
        prompt.append("- Chủ đề: ").append(topic).append("\n");
        prompt.append("- Số lượng bài: ").append(numberOfExercises).append("\n");
        prompt.append("- Độ khó: ").append(difficulty).append(" (EASY/MEDIUM/HARD/VERY_HARD)\n");
        prompt.append("- Mức nhận thức: ").append(cognitiveLevel).append(" (RECOGNITION/COMPREHENSION/APPLICATION/ADVANCED_APPLICATION)\n");
        prompt.append("- Ngữ cảnh: ").append(node.getTitle()).append("\n\n");

        // Guidelines
        prompt.append("HƯỚNG DẪN CHI TIẾT:\n");
        prompt.append("1. QUESTION (Câu hỏi): Viết rõ ràng, cụ thể, không mơ hồ. Câu hỏi phải sử dụng các con số thực tế, ví dụ cụ thể.\n");
        prompt.append("2. ANSWER (Đáp án): Đáp án ngắn gọn, chính xác (1-2 dòng). VD: 5, 3.5, √2, v.v.\n");
        prompt.append("3. SOLUTION (Lời giải): Giải thích chi tiết từng bước (150-300 từ), rõ ràng để học sinh hiểu được cách giải.\n");
        prompt.append("   - Bước 1: Xác định điều gì được cho\n");
        prompt.append("   - Bước 2: Áp dụng công thức/quy tắc nào\n");
        prompt.append("   - Bước 3: Tính toán từng bước\n");
        prompt.append("   - Bước 4: Kết luận/kiểm tra đáp án\n");
        prompt.append("4. HINTS (Gợi ý): 2-3 gợi ý hữu ích để hướng dẫn học sinh (không phải đáp án sẵn).\n");
        prompt.append("5. ESTIMATED_TIME: Thời gian ước tính để làm bài (5-30 phút).\n\n");

        // JSON Structure
        prompt.append("ĐỊNH DẠNG JSON XUẤT RA (BẮTBUỘC):\n");
        prompt.append("{\n");
        prompt.append("  \"exercises\": [\n");
        prompt.append("    {\n");
        prompt.append("      \"question\": \"Câu hỏi bài tập cụ thể và rõ ràng\",\n");
        prompt.append("      \"answer\": \"Đáp án ngắn gọn (vd: 5, 3.5, √2, 10cm², ...)\",\n");
        prompt.append("      \"solution\": \"Lời giải chi tiết từng bước 150-300 từ giải thích cách làm\",\n");
        prompt.append("      \"difficulty\": \"").append(difficulty.toLowerCase()).append("\",\n");
        prompt.append("      \"cognitiveLevel\": \"").append(cognitiveLevel.toLowerCase()).append("\",\n");
        prompt.append("      \"hints\": [\"Gợi ý 1\", \"Gợi ý 2\", \"Gợi ý 3\"],\n");
        prompt.append("      \"estimatedTime\": 15\n");
        prompt.append("    }\n");
        prompt.append("  ]\n");
        prompt.append("}\n\n");

        // Quality Rules
        prompt.append("TIÊU CHÍ CHẤT LƯỢNG:\n");
        prompt.append("1. Mỗi câu hỏi phải độc lập, không phụ thuộc vào câu hỏi khác\n");
        prompt.append("2. Câu hỏi phải phù hợp với độ khó đã yêu cầu\n");
        prompt.append("3. Lời giải phải đủ chi tiết để học sinh tự học được\n");
        prompt.append("4. Không sử dụng dấu ba chấm (...) trong nội dung\n");
        prompt.append("5. Dùng các ký hiệu toán học chuẩn (², √, ∫, Σ, v.v.)\n");
        prompt.append("6. Bao gồm các ví dụ số thực tế, không chỉ lý thuyết\n\n");

        prompt.append("CHỈ TRẢ VỀ JSON THUẦN TÚY, KHÔNG CÓ TEXT GIẢI THÍCH THÊM, KHÔNG CÓ MARKDOWN CODE BLOCK.\n");

        return prompt.toString();
    }

    /**
     * Generate concepts for a specific mindmap node using Gemini AI
     */
    @Override
    public List<Concept> generateConceptsForNode(Long nodeId, String topic, Integer numberOfConcepts, Long userId) {
        log.info("Generating {} concepts for node: {} with topic: {}", numberOfConcepts, nodeId, topic);

        try {
            // 1. Verify node exists
            MindmapNode node = mindmapNodeRepository.findById(nodeId)
                    .orElseThrow(() -> new RuntimeException("Node not found with id: " + nodeId));

            // 2. Build comprehensive prompt
            String prompt = buildConceptGenerationPrompt(topic, numberOfConcepts, node);
            log.debug("Built concept prompt with length: {} characters", prompt.length());

            // 3. Call Gemini API
            String aiJsonResponse = callGeminiApi(prompt);
            log.info("Received AI response with length: {} characters", aiJsonResponse.length());

            // 4. Parse concepts from response
            List<Concept> concepts = parseConceptsFromAiResponse(aiJsonResponse, nodeId, userId);
            log.info("Parsed {} concepts from AI response", concepts.size());

            // 5. Save concepts
            List<Concept> savedConcepts = conceptRepository.saveAll(concepts);
            conceptRepository.flush();
            log.info("Saved {} concepts to database", savedConcepts.size());

            return savedConcepts;

        } catch (Exception e) {
            log.error("Failed to generate concepts for node {}: {}", nodeId, e.getMessage(), e);
            throw new RuntimeException("Failed to generate concepts: " + e.getMessage());
        }
    }

    /**
     * Generate formulas for a specific mindmap node using Gemini AI
     */
    @Override
    public List<Formula> generateFormulasForNode(Long nodeId, String topic, Integer numberOfFormulas, Long userId) {
        log.info("Generating {} formulas for node: {} with topic: {}", numberOfFormulas, nodeId, topic);

        try {
            // 1. Verify node exists
            MindmapNode node = mindmapNodeRepository.findById(nodeId)
                    .orElseThrow(() -> new RuntimeException("Node not found with id: " + nodeId));

            // 2. Build comprehensive prompt
            String prompt = buildFormulaGenerationPrompt(topic, numberOfFormulas, node);
            log.debug("Built formula prompt with length: {} characters", prompt.length());

            // 3. Call Gemini API
            String aiJsonResponse = callGeminiApi(prompt);
            log.info("Received AI response with length: {} characters", aiJsonResponse.length());

            // 4. Parse formulas from response
            List<Formula> formulas = parseFormulasFromAiResponse(aiJsonResponse, nodeId, userId);
            log.info("Parsed {} formulas from AI response", formulas.size());

            // 5. Save formulas
            List<Formula> savedFormulas = formulaRepository.saveAll(formulas);
            formulaRepository.flush();
            log.info("Saved {} formulas to database", savedFormulas.size());

            return savedFormulas;

        } catch (Exception e) {
            log.error("Failed to generate formulas for node {}: {}", nodeId, e.getMessage(), e);
            throw new RuntimeException("Failed to generate formulas: " + e.getMessage());
        }
    }

    /**
     * Build comprehensive prompt for concept generation
     */
    private String buildConceptGenerationPrompt(String topic, Integer numberOfConcepts, MindmapNode node) {
        StringBuilder prompt = new StringBuilder();

        // System instruction
        prompt.append("Bạn là một chuyên gia giáo dục toán học với hơn 20 năm kinh nghiệm giảng dạy tại Việt Nam. ");
        prompt.append("Nhiệm vụ của bạn là tạo các khái niệm toán học chi tiết và rõ ràng dựa trên yêu cầu.\n\n");

        // Requirements
        prompt.append("YÊU CẦU KHÁI NIỆM:\n");
        prompt.append("- Chủ đề: ").append(topic).append("\n");
        prompt.append("- Số lượng khái niệm: ").append(numberOfConcepts).append("\n");
        prompt.append("- Ngữ cảnh: ").append(node.getTitle()).append("\n\n");

        // Guidelines
        prompt.append("HƯỚNG DẪN CHI TIẾT:\n");
        prompt.append("1. NAME (Tên khái niệm): Tên khái niệm ngắn gọn, dễ hiểu (2-5 từ).\n");
        prompt.append("2. DEFINITION (Định nghĩa): Định nghĩa chính xác, khoa học của khái niệm (50-100 từ).\n");
        prompt.append("3. EXPLANATION (Giải thích): Giải thích chi tiết, dễ hiểu cho học sinh (150-300 từ).\n");
        prompt.append("   - Giải thích bằng ngôn ngữ đơn giản\n");
        prompt.append("   - Nêu rõ ý nghĩa và vai trò của khái niệm\n");
        prompt.append("   - Liên hệ với các khái niệm khác nếu có\n");
        prompt.append("4. EXAMPLES (Ví dụ): 2-3 ví dụ cụ thể, dễ hiểu minh họa cho khái niệm.\n");
        prompt.append("5. KEY_POINTS (Điểm quan trọng): 3-5 điểm quan trọng cần nhớ về khái niệm.\n");
        prompt.append("6. COMMON_MISTAKES (Sai lầm thường gặp): 2-3 sai lầm học sinh hay mắc phải.\n");
        prompt.append("7. TIPS (Mẹo ghi nhớ): 1-2 mẹo giúp học sinh ghi nhớ dễ dàng.\n\n");

        // JSON Structure
        prompt.append("ĐỊNH DẠNG JSON XUẤT RA (BẮT BUỘC):\n");
        prompt.append("{\n");
        prompt.append("  \"concepts\": [\n");
        prompt.append("    {\n");
        prompt.append("      \"name\": \"Tên khái niệm\",\n");
        prompt.append("      \"definition\": \"Định nghĩa chính xác, khoa học 50-100 từ\",\n");
        prompt.append("      \"explanation\": \"Giải thích chi tiết, dễ hiểu 150-300 từ\",\n");
        prompt.append("      \"examples\": [\"Ví dụ 1 cụ thể\", \"Ví dụ 2 cụ thể\", \"Ví dụ 3 cụ thể\"],\n");
        prompt.append("      \"keyPoints\": [\"Điểm quan trọng 1\", \"Điểm quan trọng 2\", \"Điểm quan trọng 3\"],\n");
        prompt.append("      \"commonMistakes\": [\"Sai lầm 1\", \"Sai lầm 2\"],\n");
        prompt.append("      \"tips\": \"Mẹo ghi nhớ hữu ích\"\n");
        prompt.append("    }\n");
        prompt.append("  ]\n");
        prompt.append("}\n\n");

        // Quality Rules
        prompt.append("TIÊU CHÍ CHẤT LƯỢNG:\n");
        prompt.append("1. Định nghĩa phải chính xác về mặt toán học\n");
        prompt.append("2. Giải thích phải dễ hiểu, phù hợp với học sinh\n");
        prompt.append("3. Ví dụ phải cụ thể, có số liệu thực tế\n");
        prompt.append("4. Không sử dụng dấu ba chấm (...) trong nội dung\n");
        prompt.append("5. Dùng các ký hiệu toán học chuẩn (², √, ∫, Σ, v.v.)\n");
        prompt.append("6. Mỗi trường phải có nội dung đầy đủ, không để trống\n\n");

        prompt.append("CHỈ TRẢ VỀ JSON THUẦN TÚY, KHÔNG CÓ TEXT GIẢI THÍCH THÊM, KHÔNG CÓ MARKDOWN CODE BLOCK.\n");

        return prompt.toString();
    }

    /**
     * Build comprehensive prompt for formula generation
     */
    private String buildFormulaGenerationPrompt(String topic, Integer numberOfFormulas, MindmapNode node) {
        StringBuilder prompt = new StringBuilder();

        // System instruction
        prompt.append("Bạn là một chuyên gia giáo dục toán học với hơn 20 năm kinh nghiệm giảng dạy tại Việt Nam. ");
        prompt.append("Nhiệm vụ của bạn là tạo các công thức toán học chi tiết và rõ ràng dựa trên yêu cầu.\n\n");

        // Requirements
        prompt.append("YÊU CẦU CÔNG THỨC:\n");
        prompt.append("- Chủ đề: ").append(topic).append("\n");
        prompt.append("- Số lượng công thức: ").append(numberOfFormulas).append("\n");
        prompt.append("- Ngữ cảnh: ").append(node.getTitle()).append("\n\n");

        // Guidelines
        prompt.append("HƯỚNG DẪN CHI TIẾT:\n");
        prompt.append("1. NAME (Tên công thức): Tên công thức ngắn gọn (3-7 từ). VD: 'Công thức tính diện tích hình tròn'\n");
        prompt.append("2. FORMULA_TEXT (Công thức dạng text): Viết công thức dạng text đơn giản. VD: 'S = π × r²'\n");
        prompt.append("3. FORMULA_LATEX (Công thức LaTeX): Viết công thức dạng LaTeX. VD: 'S = \\pi r^2'\n");
        prompt.append("4. VARIABLES (Biến số): Giải thích ý nghĩa từng biến trong công thức.\n");
        prompt.append("   VD: 'S: Diện tích hình tròn (đơn vị: cm², m²)\\nr: Bán kính hình tròn (đơn vị: cm, m)\\nπ: Hằng số Pi ≈ 3.14159'\n");
        prompt.append("5. DESCRIPTION (Mô tả): Giải thích công thức, khi nào sử dụng (100-200 từ).\n");
        prompt.append("6. USAGE_EXAMPLE (Ví dụ áp dụng): Ví dụ cụ thể với số liệu thực tế (100-150 từ).\n");
        prompt.append("   - Cho biết dữ liệu đầu vào\n");
        prompt.append("   - Áp dụng công thức từng bước\n");
        prompt.append("   - Tính toán và đưa ra kết quả\n\n");

        // JSON Structure
        prompt.append("ĐỊNH DẠNG JSON XUẤT RA (BẮT BUỘC):\n");
        prompt.append("{\n");
        prompt.append("  \"formulas\": [\n");
        prompt.append("    {\n");
        prompt.append("      \"name\": \"Tên công thức ngắn gọn\",\n");
        prompt.append("      \"formulaText\": \"Công thức dạng text (VD: S = π × r²)\",\n");
        prompt.append("      \"formulaLatex\": \"Công thức LaTeX (VD: S = \\\\pi r^2)\",\n");
        prompt.append("      \"variables\": \"Giải thích từng biến\\nr: bán kính\\nS: diện tích\",\n");
        prompt.append("      \"description\": \"Mô tả và hướng dẫn sử dụng 100-200 từ\",\n");
        prompt.append("      \"usageExample\": \"Ví dụ cụ thể với số liệu và cách tính 100-150 từ\"\n");
        prompt.append("    }\n");
        prompt.append("  ]\n");
        prompt.append("}\n\n");

        // Quality Rules
        prompt.append("TIÊU CHÍ CHẤT LƯỢNG:\n");
        prompt.append("1. Công thức phải chính xác về mặt toán học\n");
        prompt.append("2. LaTeX phải đúng cú pháp, render được\n");
        prompt.append("3. Giải thích biến số phải đầy đủ, rõ ràng\n");
        prompt.append("4. Ví dụ áp dụng phải có số liệu cụ thể\n");
        prompt.append("5. Không sử dụng dấu ba chấm (...) trong nội dung\n");
        prompt.append("6. Mỗi trường phải có nội dung đầy đủ, không để trống\n\n");

        prompt.append("CHỈ TRẢ VỀ JSON THUẦN TÚY, KHÔNG CÓ TEXT GIẢI THÍCH THÊM, KHÔNG CÓ MARKDOWN CODE BLOCK.\n");

        return prompt.toString();
    }

    /**
     * Parse concepts from AI JSON response
     */
    private List<Concept> parseConceptsFromAiResponse(String aiJsonResponse, Long nodeId, Long userId) {
        List<Concept> concepts = new ArrayList<>();

        try {
            log.info("Parsing concepts from AI response");
            JsonNode root = objectMapper.readTree(aiJsonResponse);

            JsonNode conceptsArray = root.path("concepts");
            if (!conceptsArray.isArray()) {
                log.warn("No 'concepts' array found in AI response");
                return concepts;
            }

            int orderIndex = 0;
            for (JsonNode conceptNode : conceptsArray) {
                try {
                    Concept concept = new Concept();
                    concept.setNodeId(nodeId);

                    // Required fields
                    String name = conceptNode.path("name").asText("").trim();
                    String definition = conceptNode.path("definition").asText("").trim();
                    String explanation = conceptNode.path("explanation").asText("").trim();

                    if (name.isEmpty() || definition.isEmpty()) {
                        log.warn("Concept missing name or definition, skipping");
                        continue;
                    }

                    concept.setName(name);
                    concept.setDefinition(definition);
                    concept.setExplanation(explanation.isEmpty() ? definition : explanation);

                    // Parse examples array
                    JsonNode examples = conceptNode.path("examples");
                    if (examples.isArray() && examples.size() > 0) {
                        StringBuilder examplesText = new StringBuilder();
                        for (JsonNode example : examples) {
                            examplesText.append("• ").append(example.asText()).append("\n");
                        }
                        concept.setExamples(examplesText.toString().trim());
                    }

                    // Parse key points array
                    JsonNode keyPoints = conceptNode.path("keyPoints");
                    if (keyPoints.isArray() && keyPoints.size() > 0) {
                        StringBuilder keyPointsText = new StringBuilder();
                        for (JsonNode point : keyPoints) {
                            keyPointsText.append("✓ ").append(point.asText()).append("\n");
                        }
                        concept.setKeyPoints(keyPointsText.toString().trim());
                    }

                    // Parse common mistakes array
                    JsonNode commonMistakes = conceptNode.path("commonMistakes");
                    if (commonMistakes.isArray() && commonMistakes.size() > 0) {
                        StringBuilder mistakesText = new StringBuilder();
                        for (JsonNode mistake : commonMistakes) {
                            mistakesText.append("⚠ ").append(mistake.asText()).append("\n");
                        }
                        concept.setCommonMistakes(mistakesText.toString().trim());
                    }

                    // Tips
                    String tips = conceptNode.path("tips").asText("").trim();
                    if (!tips.isEmpty()) {
                        concept.setTips("💡 " + tips);
                    }

                    // Set defaults
                    concept.setOrderIndex(orderIndex++);
                    // createdAt and updatedAt are set automatically by @PrePersist

                    concepts.add(concept);
                    log.debug("Parsed concept: {} (definition length: {} chars)", 
                            concept.getName(), concept.getDefinition().length());

                } catch (Exception e) {
                    log.warn("Failed to parse concept: {}", e.getMessage());
                }
            }

            log.info("Successfully parsed {} concepts from AI response", concepts.size());

        } catch (Exception e) {
            log.error("Failed to parse concepts from AI response: {}", e.getMessage(), e);
        }

        return concepts;
    }

    /**
     * Parse formulas from AI JSON response
     */
    private List<Formula> parseFormulasFromAiResponse(String aiJsonResponse, Long nodeId, Long userId) {
        List<Formula> formulas = new ArrayList<>();

        try {
            log.info("Parsing formulas from AI response");
            JsonNode root = objectMapper.readTree(aiJsonResponse);

            JsonNode formulasArray = root.path("formulas");
            if (!formulasArray.isArray()) {
                log.warn("No 'formulas' array found in AI response");
                return formulas;
            }

            int orderIndex = 0;
            boolean isFirst = true;
            for (JsonNode formulaNode : formulasArray) {
                try {
                    Formula formula = new Formula();
                    formula.setNodeId(nodeId);

                    // Required fields
                    String name = formulaNode.path("name").asText("").trim();
                    String formulaText = formulaNode.path("formulaText").asText("").trim();
                    String formulaLatex = formulaNode.path("formulaLatex").asText("").trim();

                    if (name.isEmpty() || (formulaText.isEmpty() && formulaLatex.isEmpty())) {
                        log.warn("Formula missing name or formula text/latex, skipping");
                        continue;
                    }

                    formula.setName(name);
                    formula.setFormulaText(formulaText.isEmpty() ? formulaLatex : formulaText);
                    formula.setFormulaLatex(formulaLatex.isEmpty() ? formulaText : formulaLatex);

                    // Variables
                    String variables = formulaNode.path("variables").asText("").trim();
                    formula.setVariables(variables);

                    // Description
                    String description = formulaNode.path("description").asText("").trim();
                    formula.setDescription(description);

                    // Usage example
                    String usageExample = formulaNode.path("usageExample").asText("").trim();
                    formula.setUsageExample(usageExample);

                    // Set first formula as primary
                    formula.setIsPrimary(isFirst);
                    isFirst = false;

                    // Set defaults
                    formula.setOrderIndex(orderIndex++);
                    // createdAt and updatedAt are set automatically by @PrePersist

                    formulas.add(formula);
                    log.debug("Parsed formula: {} (text: {})", formula.getName(), formula.getFormulaText());

                } catch (Exception e) {
                    log.warn("Failed to parse formula: {}", e.getMessage());
                }
            }

            log.info("Successfully parsed {} formulas from AI response", formulas.size());

        } catch (Exception e) {
            log.error("Failed to parse formulas from AI response: {}", e.getMessage(), e);
        }

        return formulas;
    }

    /**
     * Build comprehensive system prompt for mindmap generation
     */
    private String buildComprehensiveMindmapPrompt(AiGenerateMindmapRequest request) {
        StringBuilder prompt = new StringBuilder();
        
        // System instruction
        prompt.append("Bạn là một chuyên gia giáo dục toán học với hơn 20 năm kinh nghiệm giảng dạy tại Việt Nam. ");
        prompt.append("Bạn am hiểu sâu sắc toàn bộ chương trình Toán từ lớp 1 đến lớp 12 theo chuẩn Bộ Giáo dục và Đào tạo (MOET) ");
        prompt.append("và kiến thức toán học quốc tế. Nhiệm vụ của bạn là tạo mindmap Toán học chi tiết dựa trên kiến thức tổng quát, ");
        prompt.append("không giới hạn bởi tài liệu cụ thể.\n\n");
        
        // User requirements
        prompt.append("YÊU CẦU CỦA NGƯỜI DÙNG:\n");
        prompt.append("Tạo mindmap chi tiết về '").append(request.getTopic()).append("' ");
        prompt.append("cho học sinh lớp ").append(request.getGrade()).append(". ");
        prompt.append("Yêu cầu: TỐI THIỂU 10-15 nodes (4-5 branches, mỗi branch có 2-3 subBranches). ");
        prompt.append("Mỗi node phải có content đầy đủ 100-300 từ với emoji, công thức toán học (², ³, √, Δ), ");
        prompt.append("ví dụ cụ thể, và bài tập có đáp án. ");
        prompt.append("KHÔNG viết nội dung chung chung hoặc '...'.\n\n");
        
        if (request.getDescription() != null && !request.getDescription().isEmpty()) {
            prompt.append("MÔ TẢ BỔ SUNG: ").append(request.getDescription()).append("\n\n");
        }
        
        prompt.append("LỚP HỌC: ").append(request.getGrade()).append("\n");
        prompt.append("MÔN HỌC: ").append(request.getSubject()).append("\n\n");
        
        // Math knowledge by grade
        prompt.append(getMathKnowledgeByGrade());
        
        // Structure requirements with DETAILED entity schemas
        prompt.append("\n╔═══════════════════════════════════════════════════════════════╗\n");
        prompt.append("║  ⚠️  CẢNH BÁO NGHIÊM KHẮC - BẮT BUỘC PHẢI TUÂN THỦ  ⚠️      ║\n");
        prompt.append("╚═══════════════════════════════════════════════════════════════╝\n\n");
        
        prompt.append("🚫 TUYỆT ĐỐI KHÔNG được:\n");
        prompt.append("❌ Thiếu bất kỳ field nào trong entity data\n");
        prompt.append("❌ Để trống hoặc dùng \"...\" trong bất kỳ field nào\n");
        prompt.append("❌ Viết nội dung chung chung, phải cụ thể và chi tiết\n");
        prompt.append("❌ Tạo tất cả nodes là CONCEPT - phải đa dạng CONCEPT/FORMULA/EXERCISE\n\n");
        
        prompt.append("✅ BẮT BUỘC phải:\n");
        prompt.append("✓ MỖI node phải có đầy đủ entity data theo đúng nodeType\n");
        prompt.append("✓ Concept: definition phải 50-100 từ, explanation phải 100-200 từ, examples phải có ít nhất 2 ví dụ cụ thể\n");
        prompt.append("✓ Formula: description phải 80-150 từ, usageExample phải có số liệu cụ thể và tính toán chi tiết\n");
        prompt.append("✓ Exercise: question phải rõ ràng có số liệu, solution phải 100-200 từ giải thích từng bước\n\n");
        
        prompt.append("📊 PHÂN BỐ LOẠI NODE BẮT BUỘC:\n");
        prompt.append("- Root node: nodeType=\"CONCEPT\" (node gốc - chủ đề chính)\n");
        prompt.append("- Branches chính (4-5 nodes): 100% nodeType=\"CONCEPT\" (định nghĩa tổng quan)\n");
        prompt.append("- Sub-branches (8-12 nodes): PHẢI pha trộn:\n");
        prompt.append("  • 30% CONCEPT (khái niệm chi tiết)\n");
        prompt.append("  • 35% FORMULA (công thức, định lý)\n");
        prompt.append("  • 35% EXERCISE (bài tập thực hành)\n\n");
        
        prompt.append("🎯 CÁCH PHÂN BỐ CỤ THỂ:\n");
        prompt.append("- Mỗi branch chính có 2-3 sub-branches\n");
        prompt.append("- Sub-branch thứ 1: nodeType=\"CONCEPT\" (giải thích khái niệm)\n");
        prompt.append("- Sub-branch thứ 2: nodeType=\"FORMULA\" (công thức liên quan)\n");
        prompt.append("- Sub-branch thứ 3 (nếu có): nodeType=\"EXERCISE\" (bài tập áp dụng)\n\n");
        
        prompt.append("═══════════════════════════════════════════════════════════════\n");
        prompt.append("📋 CHUẨN ENTITY DATA CHO TỪNG NODE TYPE:\n");
        prompt.append("═══════════════════════════════════════════════════════════════\n\n");
        
        prompt.append("🔷 NODE TYPE = \"CONCEPT\" - PHẢI có object \"concept\":\n");
        prompt.append("{\n");
        prompt.append("  \"name\": \"Tên khái niệm ngắn gọn (3-7 từ)\",\n");
        prompt.append("  \"definition\": \"Định nghĩa chính xác, khoa học 50-150 từ. VD: Phương trình bậc hai là phương trình có dạng ax² + bx + c = 0 trong đó a, b, c là các hệ số thực và a ≠ 0. Đây là dạng phương trình cơ bản trong đại số, có nhiều ứng dụng trong toán học và thực tế.\",\n");
        prompt.append("  \"explanation\": \"Giải thích chi tiết 150-300 từ với emoji và ví dụ. VD: 📐 Phương trình bậc hai xuất hiện khi ta cần tìm giá trị x làm cho biểu thức bậc hai bằng 0. Phương trình này có thể có 0, 1 hoặc 2 nghiệm tuỳ thuộc vào giá trị delta (Δ = b² - 4ac). Khi Δ > 0 có 2 nghiệm phân biệt, Δ = 0 có nghiệm kép, Δ < 0 vô nghiệm. Trong thực tế, phương trình bậc hai dùng để tính quỹ đạo vật thể, tối ưu hoá lợi nhuận, thiết kế cầu đường...\",\n");
        prompt.append("  \"examples\": [\n");
        prompt.append("    \"• Phương trình x² - 5x + 6 = 0 có a=1, b=-5, c=6. Delta = 25-24 = 1 > 0 nên có 2 nghiệm x₁=2, x₂=3\",\n");
        prompt.append("    \"• Phương trình 2x² + 3x - 5 = 0 giải bằng công thức nghiệm: x = (-3 ± √49)/4 = (-3 ± 7)/4\",\n");
        prompt.append("    \"• Ứng dụng thực tế: Tính thời gian vật rơi tự do h = ½gt² khi biết độ cao\"\n");
        prompt.append("  ],\n");
        prompt.append("  \"keyPoints\": [\n");
        prompt.append("    \"✓ Điều kiện: a ≠ 0, nếu a=0 thì là phương trình bậc nhất\",\n");
        prompt.append("    \"✓ Delta (Δ) quyết định số nghiệm: Δ>0 (2 nghiệm), Δ=0 (1 nghiệm), Δ<0 (vô nghiệm)\",\n");
        prompt.append("    \"✓ Công thức nghiệm: x = (-b ± √Δ)/(2a)\",\n");
        prompt.append("    \"✓ Hệ thức Vi-et: x₁ + x₂ = -b/a; x₁ × x₂ = c/a\"\n");
        prompt.append("  ],\n");
        prompt.append("  \"commonMistakes\": [\n");
        prompt.append("    \"⚠ Quên kiểm tra điều kiện a ≠ 0\",\n");
        prompt.append("    \"⚠ Tính sai delta: nhầm công thức b² - 4ac\",\n");
        prompt.append("    \"⚠ Kết luận vội vô nghiệm khi delta âm mà không xét số phức\"\n");
        prompt.append("  ],\n");
        prompt.append("  \"tips\": \"💡 Ghi nhớ: 'Delta Dương - 2 nghiệm Đẹp, Delta 0 - nghiệm Kép, Delta Âm - Vô nghiệm'\"\n");
        prompt.append("}\n\n");
        
        prompt.append("🔶 NODE TYPE = \"FORMULA\" - PHẢI có array \"formulas\" (ít nhất 1 công thức):\n");
        prompt.append("[\n");
        prompt.append("  {\n");
        prompt.append("    \"name\": \"Công thức nghiệm phương trình bậc hai\",\n");
        prompt.append("    \"formulaText\": \"x = (-b ± √(b² - 4ac)) / (2a)\",\n");
        prompt.append("    \"formulaLatex\": \"x = \\\\frac{-b \\\\pm \\\\sqrt{b^2 - 4ac}}{2a}\",\n");
        prompt.append("    \"variables\": \"a: Hệ số bậc hai (a ≠ 0), đơn vị phụ thuộc bài toán\\nb: Hệ số bậc nhất, cùng đơn vị với a×x\\nc: Hệ số tự do, số thực bất kỳ\\nx: Nghiệm cần tìm, giá trị làm cho phương trình bằng 0\\nΔ (Delta): Biệt thức Δ = b² - 4ac, quyết định số nghiệm\",\n");
        prompt.append("    \"description\": \"Công thức nghiệm tổng quát để giải phương trình bậc hai ax² + bx + c = 0. Công thức này được suy ra bằng phương pháp hoàn thành bình phương. Trước tiên chia cả hai vế cho a (vì a≠0), sau đó chuyển vế và hoàn thành bình phương để tách x ra ngoài. Dấu ± trong công thức cho ta hai nghiệm x₁ (dùng +) và x₂ (dùng -). Điều kiện để có nghiệm thực là Δ ≥ 0.\",\n");
        prompt.append("    \"usageExample\": \"VÍ DỤ CỤ THỂ: Giải phương trình 2x² - 7x + 3 = 0\\n\\nBước 1: Xác định hệ số\\n- a = 2, b = -7, c = 3\\n\\nBước 2: Tính delta\\n- Δ = b² - 4ac = (-7)² - 4(2)(3) = 49 - 24 = 25\\n\\nBước 3: Vì Δ = 25 > 0, phương trình có 2 nghiệm phân biệt\\n\\nBước 4: Áp dụng công thức nghiệm\\n- x₁ = (7 + √25)/(2×2) = (7 + 5)/4 = 12/4 = 3\\n- x₂ = (7 - √25)/(2×2) = (7 - 5)/4 = 2/4 = 0.5\\n\\nVậy phương trình có 2 nghiệm: x₁ = 3 và x₂ = 0.5\"\n");
        prompt.append("  }\n");
        prompt.append("]\n\n");
        
        prompt.append("🔷 NODE TYPE = \"EXERCISE\" - PHẢI có array \"exercises\" (1-3 bài tập):\n");
        prompt.append("[\n");
        prompt.append("  {\n");
        prompt.append("    \"question\": \"Giải phương trình sau và biện luận số nghiệm: 3x² - 12x + 9 = 0\",\n");
        prompt.append("    \"answer\": \"x₁ = 3, x₂ = 1\",\n");
        prompt.append("    \"solution\": \"📝 GIẢI CHI TIẾT:\\n\\nBước 1️⃣: Xác định các hệ số\\n- Phương trình có dạng: ax² + bx + c = 0\\n- So sánh: a = 3, b = -12, c = 9\\n- Kiểm tra: a = 3 ≠ 0 ✓ (đúng là phương trình bậc hai)\\n\\nBước 2️⃣: Tính biệt thức Delta (Δ)\\n- Δ = b² - 4ac\\n- Δ = (-12)² - 4(3)(9)\\n- Δ = 144 - 108\\n- Δ = 36\\n\\nBước 3️⃣: Biện luận số nghiệm\\n- Vì Δ = 36 > 0\\n- Phương trình có 2 nghiệm phân biệt\\n\\nBước 4️⃣: Tính nghiệm theo công thức\\n- x = (-b ± √Δ) / (2a)\\n- x = (12 ± √36) / (2×3)\\n- x = (12 ± 6) / 6\\n\\nNghiệm 1: x₁ = (12 + 6)/6 = 18/6 = 3\\nNghiệm 2: x₂ = (12 - 6)/6 = 6/6 = 1\\n\\nBước 5️⃣: Kiểm tra (thay vào phương trình)\\nVới x = 3: 3(3)² - 12(3) + 9 = 27 - 36 + 9 = 0 ✓\\nVới x = 1: 3(1)² - 12(1) + 9 = 3 - 12 + 9 = 0 ✓\\n\\n🎯 KẾT LUẬN: Phương trình có 2 nghiệm phân biệt x₁ = 3 và x₂ = 1\",\n");
        prompt.append("    \"difficulty\": \"medium\",\n");
        prompt.append("    \"cognitiveLevel\": \"application\",\n");
        prompt.append("    \"hints\": [\"💡 Gợi ý 1: Tính delta trước để biết số nghiệm\", \"💡 Gợi ý 2: Áp dụng công thức x = (-b ± √Δ)/(2a)\", \"💡 Gợi ý 3: Nhớ kiểm tra lại đáp án bằng cách thế vào phương trình\"],\n");
        prompt.append("    \"estimatedTime\": 10\n");
        prompt.append("  }\n");
        prompt.append("]\n\n");
        
        prompt.append("═══════════════════════════════════════════════════════════════\n");
        prompt.append("📐 CẤU TRÚC JSON MINDMAP HOÀN CHỈNH:\n");
        prompt.append("═══════════════════════════════════════════════════════════════\n\n");
        
        prompt.append("⚠️ LƯU Ý QUAN TRỌNG VỀ KÝ HIỆU TOÁN HỌC:\n");
        prompt.append("1. Trong field \"title\" và \"content\": CHỈ dùng ký hiệu Unicode\n");
        prompt.append("   ✓ ĐÚNG: √(A²), x², x³, Δ, π, ∑, ∫, ≤, ≥, ≠, ±, ×, ÷\n");
        prompt.append("   ✗ SAI: \\sqrt{A^2}, x^2 (LaTeX syntax), \\frac{a}{b}\n");
        prompt.append("2. LaTeX syntax CHỈ dùng trong field \"formulaLatex\" của entity Formula\n");
        prompt.append("3. Nếu không chắc Unicode, dùng text mô tả: \"căn bậc hai của A\"\n\n");
        
        prompt.append("{\n");
        prompt.append("  \"centralTopic\": \"").append(request.getTopic()).append("\",\n");
        prompt.append("  \"branches\": [\n");
        prompt.append("    {\n");
        prompt.append("      \"title\": \"Định nghĩa và khái niệm cơ bản\",\n");
        prompt.append("      \"content\": \"Nội dung giới thiệu tổng quan về chủ đề với 100-300 từ, bao gồm emoji 📐 và ký hiệu Unicode x² √ Δ\",\n");
        prompt.append("      \"nodeType\": \"CONCEPT\",\n");
        prompt.append("      \"concept\": {\n");
        prompt.append("        \"name\": \"Tên khái niệm chính\",\n");
        prompt.append("        \"definition\": \"Định nghĩa khoa học chính xác 50-150 từ, không được thiếu, không được dùng ...\",\n");
        prompt.append("        \"explanation\": \"Giải thích chi tiết 150-300 từ với emoji, ví dụ cụ thể, ứng dụng thực tế\",\n");
        prompt.append("        \"examples\": [\"Ví dụ 1 cụ thể với số liệu\", \"Ví dụ 2 thực tế có tính toán\", \"Ví dụ 3 ứng dụng\"],\n");
        prompt.append("        \"keyPoints\": [\"Điểm quan trọng 1\", \"Điểm quan trọng 2\", \"Điểm quan trọng 3\"],\n");
        prompt.append("        \"commonMistakes\": [\"Sai lầm thường gặp 1\", \"Sai lầm 2\"],\n");
        prompt.append("        \"tips\": \"Mẹo ghi nhớ hữu ích\"\n");
        prompt.append("      },\n");
        prompt.append("      \"subBranches\": [\n");
        prompt.append("        {\n");
        prompt.append("          \"title\": \"Khái niệm chi tiết\",\n");
        prompt.append("          \"content\": \"Nội dung giải thích khái niệm chi tiết 100-300 từ\",\n");
        prompt.append("          \"nodeType\": \"CONCEPT\",\n");
        prompt.append("          \"concept\": {\n");
        prompt.append("            \"name\": \"Tên khái niệm con\",\n");
        prompt.append("            \"definition\": \"Định nghĩa chi tiết 50-150 từ\",\n");
        prompt.append("            \"explanation\": \"Giải thích với ví dụ 150-300 từ\",\n");
        prompt.append("            \"examples\": [\"Ví dụ cụ thể 1\", \"Ví dụ 2\"],\n");
        prompt.append("            \"keyPoints\": [\"Điểm quan trọng 1\", \"Điểm 2\"],\n");
        prompt.append("            \"commonMistakes\": [\"Sai lầm 1\"],\n");
        prompt.append("            \"tips\": \"Mẹo ghi nhớ\"\n");
        prompt.append("          }\n");
        prompt.append("        },\n");
        prompt.append("        {\n");
        prompt.append("          \"title\": \"Công thức √(a² + b²)\",\n");
        prompt.append("          \"content\": \"Nội dung giải thích công thức 100-300 từ với cách suy ra và ứng dụng\",\n");
        prompt.append("          \"nodeType\": \"FORMULA\",\n");
        prompt.append("          \"formulas\": [\n");
        prompt.append("            {\n");
        prompt.append("              \"name\": \"Tên công thức đầy đủ\",\n");
        prompt.append("              \"formulaText\": \"Công thức dạng text: a² + b² = c²\",\n");
        prompt.append("              \"formulaLatex\": \"a^2 + b^2 = c^2\",\n");
        prompt.append("              \"variables\": \"a: giải thích biến a\\nb: giải thích biến b\\nc: giải thích biến c\",\n");
        prompt.append("              \"description\": \"Mô tả công thức chi tiết 100-200 từ: nguồn gốc, cách suy ra, điều kiện áp dụng\",\n");
        prompt.append("              \"usageExample\": \"Ví dụ áp dụng CỤ THỂ với số liệu:\\nCho a=3, b=4\\nÁp dụng công thức: c² = 3² + 4² = 9 + 16 = 25\\nVậy c = √25 = 5\"\n");
        prompt.append("            }\n");
        prompt.append("          ]\n");
        prompt.append("        },\n");
        prompt.append("        {\n");
        prompt.append("          \"title\": \"Bài tập thực hành\",\n");
        prompt.append("          \"content\": \"Hướng dẫn cách làm dạng bài tập này 100-300 từ\",\n");
        prompt.append("          \"nodeType\": \"EXERCISE\",\n");
        prompt.append("          \"exercises\": [\n");
        prompt.append("            {\n");
        prompt.append("              \"question\": \"Câu hỏi bài tập CỤ THỂ với số liệu rõ ràng\",\n");
        prompt.append("              \"answer\": \"Đáp án ngắn gọn (số hoặc biểu thức)\",\n");
        prompt.append("              \"solution\": \"Lời giải CHI TIẾT 100-200 từ:\\nBước 1: ...\\nBước 2: ...\\nBước 3: ...\\nKết luận: ...\",\n");
        prompt.append("              \"difficulty\": \"easy\",\n");
        prompt.append("              \"cognitiveLevel\": \"application\",\n");
        prompt.append("              \"hints\": [\"Gợi ý 1\", \"Gợi ý 2\"],\n");
        prompt.append("              \"estimatedTime\": 10\n");
        prompt.append("            }\n");
        prompt.append("          ]\n");
        prompt.append("        }\n");
        prompt.append("      ]\n");
        prompt.append("    }\n");
        prompt.append("  ]\n");
        prompt.append("}\n\n");
        
        // Professional rules with STRONG EMPHASIS
        prompt.append("═══════════════════════════════════════════════════════════════\n");
        prompt.append("⚡ QUY TẮC BẮT BUỘC - KHÔNG ĐƯỢC VI PHẠM:\n");
        prompt.append("═══════════════════════════════════════════════════════════════\n\n");
        
        prompt.append("1️⃣ ENTITY DATA ĐẦY ĐỦ:\n");
        prompt.append("   ✓ MỖI node PHẢI có đầy đủ entity data theo đúng nodeType\n");
        prompt.append("   ✓ CONCEPT: definition 30-50 từ, explanation 50-80 từ, ít nhất 2 examples ngắn gọn\n");
        prompt.append("   ✓ FORMULA: description 40-60 từ, usageExample phải có số liệu và tính toán\n");
        prompt.append("   ✓ EXERCISE: solution 60-100 từ giải chi tiết từng bước\n");
        prompt.append("   ❌ KHÔNG được để trống, dùng \"...\", hoặc viết \"Nội dung sẽ được bổ sung\"\n\n");
        
        prompt.append("2️⃣ ĐA DẠNG NODE TYPE:\n");
        prompt.append("   ✓ Branches chính: 100% CONCEPT (4-5 nodes)\n");
        prompt.append("   ✓ Sub-branches: 30% CONCEPT + 35% FORMULA + 35% EXERCISE (12-15 nodes)\n");
        prompt.append("   ❌ KHÔNG tạo tất cả nodes là CONCEPT\n\n");
        
        prompt.append("3️⃣ SỐ LƯỢNG VÀ ĐỘ SÂU:\n");
        prompt.append("   ✓ Tối thiểu 10-15 nodes (4-5 branches × 2-3 sub-branches mỗi branch)\n");
        prompt.append("   ✓ Content mỗi node: 50-150 từ ngắn gọn với emoji và ký hiệu toán học\n");
        prompt.append("   ✓ Mỗi khái niệm quan trọng phải có công thức và bài tập đi kèm\n\n");
        
        prompt.append("4️⃣ CHẤT LƯỢNG NỘI DUNG:\n");
        prompt.append("   ✓ Definition/Explanation: Súc tích, khoa học, chính xác\n");
        prompt.append("   ✓ Examples: Cụ thể với số liệu, không chung chung\n");
        prompt.append("   ✓ Formula: Có cả text và LaTeX, giải thích ngắn gọn biến số\n");
        prompt.append("   ✓ Exercise: Câu hỏi rõ ràng, lời giải từng bước, có kiểm tra\n");
        prompt.append("   ✓ Sử dụng emoji phù hợp: 📐 🔢 ✏️ 🎯 💡 ⚠️ ✓\n\n");
        
        prompt.append("5️⃣ PHẠM VI KIẾN THỨC:\n");
        prompt.append("   ✓ Tự do mở rộng từ kiến thức tổng quát, không giới hạn bởi tài liệu\n");
        prompt.append("   ✓ Đào sâu chi tiết với nhiều góc nhìn, ví dụ thực tế\n");
        prompt.append("   ✓ Phù hợp với lớp học: Lớp ").append(request.getGrade()).append("\n\n");
        
        prompt.append("═══════════════════════════════════════════════════════════════\n");
        prompt.append("🎯 LƯU Ý QUAN TRỌNG CUỐI CÙNG:\n");
        prompt.append("═══════════════════════════════════════════════════════════════\n\n");
        
        prompt.append("📌 KÝ HIỆU TOÁN HỌC - TUYỆT ĐỐI PHẢI TUÂN THỦ:\n");
        prompt.append("    ✅ Trong \"title\", \"content\": CHỈ dùng ký hiệu Unicode\n");
        prompt.append("       VD: √(A²) = |A|, x² + y² = z², Δ = b² - 4ac, π ≈ 3.14\n");
        prompt.append("    ❌ TUYỆT ĐỐI KHÔNG dùng LaTeX syntax trong title/content\n");
        prompt.append("       VD SAI: \\sqrt{A^2}, \\frac{a}{b}, x^2 (phải viết x²)\n");
        prompt.append("    ℹ️  LaTeX CHỈ dùng trong field \"formulaLatex\" của entity Formula\n\n");
        
        prompt.append("⚠️  Nếu bất kỳ node nào THIẾU entity data hoặc có data không đầy đủ,\n");
        prompt.append("    hệ thống SẼ TỰ ĐỘNG TẠO DEFAULT DATA và đánh dấu là LOW QUALITY.\n");
        prompt.append("    Điều này làm giảm giá trị của mindmap!\n\n");
        
        prompt.append("✅  Hãy đảm bảo TỪNG NODE đều có entity data ĐẦY ĐỦ, SÚCÍCH:\n");
        prompt.append("    - Concept: name + definition (30-50 từ) + explanation (50-80 từ) + examples (2-3 ngắn) + keyPoints + commonMistakes + tips\n");
        prompt.append("    - Formula: name + formulaText + formulaLatex + variables + description (40-60 từ) + usageExample (với số liệu)\n");
        prompt.append("    - Exercise: question + answer + solution (60-100 từ chi tiết) + difficulty + cognitiveLevel + hints + estimatedTime\n\n");
        
        prompt.append("🔥 QUAN TRỌNG NHẤT:\n");
        prompt.append("    CHỈ TRẢ VỀ JSON THUẦN TÚY, KHÔNG CÓ TEXT GIẢI THÍCH THÊM, KHÔNG CÓ MARKDOWN CODE BLOCK ```json.\n");
        prompt.append("    JSON phải VALID và COMPLETE, kiểm tra kỹ trước khi trả về!\n\n");
        
        return prompt.toString();
    }
    
    /**
     * Get math knowledge structure by grade
     */
    private String getMathKnowledgeByGrade() {
        StringBuilder knowledge = new StringBuilder();
        knowledge.append("KIẾN THỨC TOÁN HỌC VIỆT NAM:\n\n");
        
        knowledge.append("**LỚP 1-5 (Tiểu học):**\n");
        knowledge.append("- Số tự nhiên, phép cộng trừ nhân chia\n");
        knowledge.append("- Phân số, số thập phân\n");
        knowledge.append("- Hình học cơ bản: điểm, đoạn thẳng, góc, tam giác, tứ giác, hình tròn\n");
        knowledge.append("- Đo lường: độ dài, khối lượng, thời gian, diện tích, chu vi, thể tích\n\n");
        
        knowledge.append("**LỚP 6-9 (THCS):**\n");
        knowledge.append("- Số nguyên, số hữu tỷ, số vô tỷ, căn bậc hai\n");
        knowledge.append("- Đại số: biểu thức, phương trình, hệ phương trình, bất phương trình\n");
        knowledge.append("- Hình học: tam giác, tứ giác, đường tròn, góc, định lý Pythagore\n");
        knowledge.append("- Thống kê, xác suất cơ bản\n\n");
        
        knowledge.append("**LỚP 10-12 (THPT):**\n");
        knowledge.append("- Hàm số, phương trình, bất phương trình\n");
        knowledge.append("- Lượng giác, dãy số, giới hạn, đạo hàm, tích phân\n");
        knowledge.append("- Hình học: vectơ, tọa độ, đường thẳng, mặt phẳng, khối đa diện\n");
        knowledge.append("- Số phức, xác suất thống kê nâng cao\n\n");
        
        return knowledge.toString();
    }

    /**
     * Call Gemini API with retry logic and automatic fallback
     */
    private String callGeminiApi(String prompt) {
        // Try primary model (Gemini 2.5 Flash)
        try {
            log.info("Attempting to call Gemini API with PRIMARY model: {}", geminiPrimaryModel);
            String response = callGeminiWithRetry(prompt, geminiPrimaryModel);
            log.info("Successfully generated mindmap with PRIMARY model: {}", geminiPrimaryModel);
            return response;
        } catch (Exception e) {
            log.warn("Primary model {} failed: {}. Trying fallback model 1...", geminiPrimaryModel, e.getMessage());
            
            // Try fallback 1 (Gemini 1.5 Flash)
            try {
                log.info("Attempting to call Gemini API with FALLBACK-1 model: {}", geminiFallback1Model);
                String response = callGeminiWithRetry(prompt, geminiFallback1Model);
                log.info("Successfully generated mindmap with FALLBACK-1 model: {}", geminiFallback1Model);
                return response;
            } catch (Exception e2) {
                log.warn("Fallback model 1 {} failed: {}. Trying fallback model 2...", geminiFallback1Model, e2.getMessage());
                
                // Try fallback 2 (Gemini 2.0 Flash)
                try {
                    log.info("Attempting to call Gemini API with FALLBACK-2 model: {}", geminiFallback2Model);
                    String response = callGeminiWithRetry(prompt, geminiFallback2Model);
                    log.info("Successfully generated mindmap with FALLBACK-2 model: {}", geminiFallback2Model);
                    return response;
                } catch (Exception e3) {
                    log.error("All Gemini models failed. Primary: {}, Fallback1: {}, Fallback2: {}", 
                              e.getMessage(), e2.getMessage(), e3.getMessage());
                    throw new RuntimeException("All Gemini API models failed: " + e3.getMessage());
                }
            }
        }
    }

    /**
     * Call Gemini API with specific model
     */
    private String callGeminiWithRetry(String prompt, String model) {
        GeminiRequest request = GeminiRequest.builder()
                .contents(List.of(GeminiRequest.GeminiContent.builder()
                        .parts(List.of(GeminiRequest.GeminiPart.builder()
                                .text(prompt)
                                .build()))
                        .role("user")
                        .build()))
                .generationConfig(GeminiRequest.GeminiGenerationConfig.builder()
                        .maxOutputTokens(16000)  // Reduced to 16k to prevent truncation while keeping quality
                        .temperature(0.7)
                        .topP(0.95)
                        .topK(40.0)
                        .build())
                .build();

        WebClient webClient = webClientBuilder
                .baseUrl(geminiBaseUrl)
                .defaultHeader(HttpHeaders.CONTENT_TYPE, MediaType.APPLICATION_JSON_VALUE)
                .codecs(configurer -> configurer
                        .defaultCodecs()
                        .maxInMemorySize(16 * 1024 * 1024)) // 16MB buffer for large responses
                .build();

        log.debug("Calling Gemini API with model: {} (timeout: 5 minutes)", model);
        
        GeminiResponse response = webClient
                .post()
                .uri("/models/{model}:generateContent?key={apiKey}", model, geminiApiKey)
                .bodyValue(request)
                .retrieve()
                .bodyToMono(GeminiResponse.class)
                .timeout(Duration.ofMinutes(5)) // 5 minutes timeout for mindmap generation
                .block();

        if (response != null && response.getCandidates() != null && !response.getCandidates().isEmpty()) {
            String text = response.getCandidates().get(0)
                    .getContent()
                    .getParts()
                    .get(0)
                    .getText();
            
            // Clean up markdown code blocks if present
            text = text.replaceAll("```json\\s*", "").replaceAll("```\\s*$", "").trim();
            
            return text;
        }

        throw new RuntimeException("No response from Gemini API");
    }

    /**
     * Create mindmap entity from request
     */
    private Mindmap createMindmapEntity(AiGenerateMindmapRequest request, Long userId) {
        Mindmap mindmap = new Mindmap();
        mindmap.setTitle(request.getTopic());
        mindmap.setDescription(request.getDescription());
        mindmap.setUserId(userId);
        mindmap.setGrade(request.getGrade());
        mindmap.setSubject(request.getSubject());
        
        // Set visibility from request or default to CLASSROOM
        if (request.getVisibility() != null) {
            try {
                mindmap.setVisibility(Mindmap.Visibility.valueOf(request.getVisibility()));
                mindmap.setIsPublic(request.getVisibility().equals("PUBLIC"));
            } catch (IllegalArgumentException e) {
                mindmap.setVisibility(Mindmap.Visibility.CLASSROOM);
                mindmap.setIsPublic(false);
            }
        } else {
            mindmap.setVisibility(Mindmap.Visibility.CLASSROOM);
            mindmap.setIsPublic(false);
        }
        
        mindmap.setIsAiGenerated(true);
        mindmap.setAiProvider("gemini");
        mindmap.setAiModel(request.getAiModel() != null ? request.getAiModel() : geminiPrimaryModel);
        mindmap.setCreatedAt(LocalDateTime.now());
        mindmap.setUpdatedAt(LocalDateTime.now());
        return mindmap;
    }

    /**
     * Clean JSON string by properly escaping newlines and control characters within string values.
     * This fixes issues where AI returns JSON with unescaped newlines inside string literals,
     * which causes Jackson parser to fail with "Illegal unquoted character (CTRL-CHAR, code 10)"
     * 
     * Strategy:
     * 1. Find all string literals (text between quotes that are not escaped)
     * 2. Within each string literal, replace unescaped newlines with \\n
     * 3. Also escape other control characters like tabs, carriage returns
     * 
     * @param jsonString The raw JSON string from AI
     * @return Cleaned JSON string with properly escaped control characters
     */
    private String cleanJsonString(String jsonString) {
        if (jsonString == null || jsonString.isEmpty()) {
            return jsonString;
        }

        StringBuilder result = new StringBuilder(jsonString.length());
        boolean inString = false;
        boolean escaped = false;
        
        for (int i = 0; i < jsonString.length(); i++) {
            char c = jsonString.charAt(i);
            
            if (escaped) {
                // If previous char was backslash, keep current char as-is
                result.append(c);
                escaped = false;
                continue;
            }
            
            if (c == '\\') {
                // Mark that next char is escaped
                result.append(c);
                escaped = true;
                continue;
            }
            
            if (c == '"') {
                // Toggle string context
                inString = !inString;
                result.append(c);
                continue;
            }
            
            // If we're inside a string literal, escape control characters
            if (inString) {
                switch (c) {
                    case '\n':
                        result.append("\\n");
                        break;
                    case '\r':
                        result.append("\\r");
                        break;
                    case '\t':
                        result.append("\\t");
                        break;
                    case '\b':
                        result.append("\\b");
                        break;
                    case '\f':
                        result.append("\\f");
                        break;
                    default:
                        // Keep all other characters including Unicode
                        result.append(c);
                }
            } else {
                // Outside string literals, keep everything as-is
                result.append(c);
            }
        }
        
        log.debug("Cleaned JSON string: {} chars -> {} chars", jsonString.length(), result.length());
        return result.toString();
    }

    /**
     * Parse nodes from AI JSON response
     */
    private List<MindmapNode> parseNodesFromAiResponse(Mindmap mindmap, String aiJsonResponse) {
        List<MindmapNode> nodes = new ArrayList<>();

        try {
            log.info("Parsing AI response JSON for mindmap: {}", mindmap.getId());
            
            // Clean JSON: Escape unescaped newlines and control characters in string values
            String cleanedJson = cleanJsonString(aiJsonResponse);
            
            JsonNode root = objectMapper.readTree(cleanedJson);

            // Create central topic node (level 0)
            MindmapNode centralNode = new MindmapNode();
            centralNode.setMindmapId(mindmap.getId());
            centralNode.setTitle(root.path("centralTopic").asText(mindmap.getTitle()));
            centralNode.setContent("🎯 Chủ đề trung tâm: " + mindmap.getTitle());
            centralNode.setNodeType(MindmapNode.NodeType.ROOT);  // Node trung tâm là ROOT
            centralNode.setLevel(0);
            centralNode.setPositionX(0.0);
            centralNode.setPositionY(0.0);
            centralNode.setOrderIndex(0);
            centralNode.setParentNodeId(null);
            centralNode.setCreatedAt(LocalDateTime.now());
            centralNode.setUpdatedAt(LocalDateTime.now());
            nodes.add(centralNode);

            // Parse branches (level 1)
            JsonNode branches = root.path("branches");
            if (branches.isArray() && branches.size() > 0) {
                log.info("Found {} main branches to parse", branches.size());
                int branchIndex = 0;
                
                for (JsonNode branch : branches) {
                    // Create branch node (level 1)
                    MindmapNode branchNode = parseBranchNode(mindmap, branch, 1, branchIndex);
                    nodes.add(branchNode);

                    // Parse sub-branches (level 2)
                    JsonNode subBranches = branch.path("subBranches");
                    if (subBranches.isArray() && subBranches.size() > 0) {
                        log.debug("Branch {} has {} sub-branches", branchIndex, subBranches.size());
                        int subIndex = 0;
                        for (JsonNode subBranch : subBranches) {
                            MindmapNode subNode = parseSubBranchNode(mindmap, subBranch, 2, branchIndex, subIndex);
                            nodes.add(subNode);
                            subIndex++;
                        }
                    }
                    branchIndex++;
                }
            } else {
                log.warn("No branches found in AI response");
            }

            log.info("Successfully parsed {} nodes from AI response", nodes.size());

            // Fallback if not enough nodes
            if (nodes.size() < 5) {
                log.error("Only {} node(s) created, creating fallback nodes", nodes.size());
                return createFallbackNodes(mindmap);
            }

            return nodes;

        } catch (Exception e) {
            log.error("Failed to parse AI response: {}", e.getMessage(), e);
            return createFallbackNodes(mindmap);
        }
    }

    /**
     * Parse branch node (level 1)
     */
    private MindmapNode parseBranchNode(Mindmap mindmap, JsonNode branch, int level, int index) {
        MindmapNode node = new MindmapNode();
        node.setMindmapId(mindmap.getId());
        node.setTitle(branch.path("title").asText("Branch " + index));

        // Get content from description field
        String content = branch.path("description").asText("");
        if (content.isEmpty() || content.equals("...")) {
            content = branch.path("content").asText("Nội dung chi tiết cho " + branch.path("title").asText());
        }
        node.setContent(content);

        node.setNodeType(parseNodeType(branch.path("nodeType").asText(""), "Branch node"));
        node.setLevel(level);
        node.setOrderIndex(index);

        // Position nodes in a circle around center
        double angle = (2 * Math.PI * index) / 8.0;
        double radius = 300.0;
        node.setPositionX(radius * Math.cos(angle));
        node.setPositionY(radius * Math.sin(angle));

        node.setParentNodeId(null);  // Will be set later
        node.setCreatedAt(LocalDateTime.now());
        node.setUpdatedAt(LocalDateTime.now());

        return node;
    }

    /**
     * Parse sub-branch node (level 2)
     */
    private MindmapNode parseSubBranchNode(Mindmap mindmap, JsonNode subBranch, int level, int branchIndex, int subIndex) {
        MindmapNode node = new MindmapNode();
        node.setMindmapId(mindmap.getId());
        node.setTitle(subBranch.path("title").asText("Sub-branch " + subIndex));

        // Get content
        String content = subBranch.path("content").asText("");
        if (content.isEmpty() || content.equals("...")) {
            content = subBranch.path("description").asText("Nội dung chi tiết cho " + subBranch.path("title").asText());
        }
        node.setContent(content);

        node.setNodeType(parseNodeType(subBranch.path("nodeType").asText(""), "Sub-branch node"));
        node.setLevel(level);
        node.setOrderIndex(subIndex);

        // Position sub-nodes around their parent
        double parentAngle = (2 * Math.PI * branchIndex) / 8.0;
        double subAngle = parentAngle + (subIndex - 1) * 0.3;
        double radius = 500.0;
        node.setPositionX(radius * Math.cos(subAngle));
        node.setPositionY(radius * Math.sin(subAngle));

        node.setParentNodeId(null);  // Will be set later
        node.setCreatedAt(LocalDateTime.now());
        node.setUpdatedAt(LocalDateTime.now());

        return node;
    }

    /**
     * Parse node type from string with validation
     */
    private MindmapNode.NodeType parseNodeType(String typeStr, String nodeContext) {
        if (typeStr == null || typeStr.isEmpty()) {
            log.warn("Missing nodeType for {}, defaulting to CONCEPT", nodeContext);
            return MindmapNode.NodeType.CONCEPT;
        }
        try {
            return MindmapNode.NodeType.valueOf(typeStr.toUpperCase());
        } catch (IllegalArgumentException e) {
            log.error("Invalid nodeType '{}' for {}, defaulting to CONCEPT", typeStr, nodeContext);
            return MindmapNode.NodeType.CONCEPT;
        }
    }

    /**
     * Create fallback nodes if AI generation fails
     * Creates diverse node types instead of all CONCEPT
     */
    private List<MindmapNode> createFallbackNodes(Mindmap mindmap) {
        log.warn("Creating fallback nodes for mindmap: {}", mindmap.getId());
        List<MindmapNode> nodes = new ArrayList<>();

        // Central node
        MindmapNode root = new MindmapNode();
        root.setMindmapId(mindmap.getId());
        root.setTitle(mindmap.getTitle());
        root.setContent("🎯 Chủ đề: " + mindmap.getTitle() +
            (mindmap.getDescription() != null && !mindmap.getDescription().isEmpty() ?
            "\n📝 " + mindmap.getDescription() : ""));
        root.setNodeType(MindmapNode.NodeType.CONCEPT);
        root.setLevel(0);
        root.setPositionX(0.0);
        root.setPositionY(0.0);
        root.setOrderIndex(0);
        root.setParentNodeId(null);
        root.setCreatedAt(LocalDateTime.now());
        root.setUpdatedAt(LocalDateTime.now());
        nodes.add(root);

        // Create 6 branch nodes with DIVERSE NODE TYPES
        // Type distribution: 2 CONCEPT, 2 FORMULA, 2 EXERCISE
        MindmapNode.NodeType[] nodeTypes = {
            MindmapNode.NodeType.CONCEPT,
            MindmapNode.NodeType.FORMULA,
            MindmapNode.NodeType.EXERCISE,
            MindmapNode.NodeType.CONCEPT,
            MindmapNode.NodeType.FORMULA,
            MindmapNode.NodeType.EXERCISE
        };

        String[] contentPrefixes = {
            "📚 Định nghĩa và khái niệm cơ bản về",
            "📐 Công thức và cách tính toán trong",
            "✏️ Bài tập thực hành và ứng dụng của",
            "🎯 Các khái niệm nâng cao liên quan đến",
            "🔢 Công thức tính toán mở rộng cho",
            "📝 Các dạng bài tập khác nhau trong"
        };

        for (int i = 0; i < 6; i++) {
            MindmapNode branch = new MindmapNode();
            branch.setMindmapId(mindmap.getId());

            // Dynamic title based on node type
            String nodeTypeStr = nodeTypes[i].toString();
            String title = getNodeTypeTitle(nodeTypeStr, i, mindmap.getTitle());
            branch.setTitle(title);

            // Content with proper prefix based on node type
            String content = contentPrefixes[i] + " " + mindmap.getTitle() +
                ". Nội dung chi tiết sẽ được tạo khi AI generation thành công.";
            branch.setContent(content);

            branch.setNodeType(nodeTypes[i]);
            branch.setLevel(1);
            branch.setOrderIndex(i);

            double angle = (2 * Math.PI * i) / 6.0;
            double radius = 300.0;
            branch.setPositionX(radius * Math.cos(angle));
            branch.setPositionY(radius * Math.sin(angle));

            branch.setParentNodeId(null);
            branch.setCreatedAt(LocalDateTime.now());
            branch.setUpdatedAt(LocalDateTime.now());
            nodes.add(branch);
        }

        return nodes;
    }

    /**
     * Generate appropriate title based on node type
     */
    private String getNodeTypeTitle(String nodeType, int index, String topicTitle) {
        switch (nodeType) {
            case "CONCEPT":
                return "Khái niệm " + (index / 2 + 1) + " của " + topicTitle;
            case "FORMULA":
                return "Công thức " + (index / 2 + 1) + " trong " + topicTitle;
            case "EXERCISE":
                return "Bài tập " + (index / 2 + 1) + " về " + topicTitle;
            default:
                return "Nội dung " + (index + 1) + " của " + topicTitle;
        }
    }

    /**
     * Set parent relationships based on node levels
     */
    private void setParentRelationships(List<MindmapNode> nodes) {
        // Find root node (level 0)
        MindmapNode rootNode = nodes.stream()
                .filter(n -> n.getLevel() == 0)
                .findFirst()
                .orElse(null);

        if (rootNode == null) {
            log.warn("No root node found, cannot set parent relationships");
            return;
        }

        // Get level 1 nodes (branches)
        List<MindmapNode> level1Nodes = nodes.stream()
                .filter(n -> n.getLevel() == 1)
                .collect(Collectors.toList());

        // Set level 1 nodes' parent to root
        for (MindmapNode level1Node : level1Nodes) {
            level1Node.setParentNodeId(rootNode.getId());
        }

        // Set level 2+ nodes' parent to closest level 1 node
        List<MindmapNode> deeperNodes = nodes.stream()
                .filter(n -> n.getLevel() >= 2)
                .collect(Collectors.toList());

        for (MindmapNode deeperNode : deeperNodes) {
            MindmapNode closestParent = findClosestLevel1Node(deeperNode, level1Nodes);
            if (closestParent != null) {
                deeperNode.setParentNodeId(closestParent.getId());
            } else {
                // Fallback to root if no level 1 nodes
                deeperNode.setParentNodeId(rootNode.getId());
            }
        }

        log.info("Set parent relationships for {} nodes", nodes.size());
    }

    /**
     * Find closest level 1 node by Euclidean distance
     */
    private MindmapNode findClosestLevel1Node(MindmapNode targetNode, List<MindmapNode> level1Nodes) {
        if (level1Nodes.isEmpty()) return null;

        MindmapNode closest = level1Nodes.get(0);
        double minDistance = calculateDistance(targetNode, closest);

        for (MindmapNode level1Node : level1Nodes) {
            double distance = calculateDistance(targetNode, level1Node);
            if (distance < minDistance) {
                minDistance = distance;
                closest = level1Node;
            }
        }

        return closest;
    }

    /**
     * Calculate Euclidean distance between two nodes
     */
    private double calculateDistance(MindmapNode node1, MindmapNode node2) {
        double dx = node1.getPositionX() - node2.getPositionX();
        double dy = node1.getPositionY() - node2.getPositionY();
        return Math.sqrt(dx * dx + dy * dy);
    }

    /**
     * Generate edges from node parent relationships
     */
    private List<MindmapEdge> generateEdgesFromNodes(Mindmap mindmap, List<MindmapNode> nodes) {
        List<MindmapEdge> edges = new ArrayList<>();

        for (MindmapNode node : nodes) {
            if (node.getParentNodeId() != null) {
                MindmapEdge edge = new MindmapEdge();
                edge.setMindmapId(mindmap.getId());
                edge.setFromNodeId(node.getParentNodeId());
                edge.setToNodeId(node.getId());
                edge.setRelationshipType("hierarchical");
                edge.setIsDirected(true);
                edge.setCreatedAt(LocalDateTime.now());
                edge.setUpdatedAt(LocalDateTime.now());
                edges.add(edge);
            }
        }

        return edges;
    }

    /**
     * Parse and save concepts, formulas, exercises from AI JSON response
     * Transactional to ensure data consistency - all entities save or none
     */
    @Transactional
    private void parseAndSaveRelatedEntities(String aiJsonResponse, List<MindmapNode> nodes) {
        JsonNode root = null;
        
        // Log first 1000 chars of AI response for debugging
        log.info("=== AI RESPONSE DEBUG ===");
        log.info("Total AI response length: {} characters", aiJsonResponse.length());
        log.info("First 1000 chars: {}", aiJsonResponse.substring(0, Math.min(1000, aiJsonResponse.length())));
        
        try {
            // Clean JSON: Escape unescaped newlines and control characters in string values
            String cleanedJson = cleanJsonString(aiJsonResponse);
            
            // Try to parse the JSON response
            root = objectMapper.readTree(cleanedJson);
            log.info("✓ Successfully parsed AI JSON response");
        } catch (JsonEOFException e) {
            log.error("❌ JSON response is truncated or malformed at line {}, column {}: {}", 
                e.getLocation() != null ? e.getLocation().getLineNr() : "?",
                e.getLocation() != null ? e.getLocation().getColumnNr() : "?",
                e.getMessage());
            log.warn("⚠️ Skipping entity parsing due to malformed JSON response - This means nodes will have NO detailed entity data");
            return;
        } catch (JsonParseException e) {
            // Log more context around the error location
            int lineNum = e.getLocation() != null ? (int) e.getLocation().getLineNr() : -1;
            int colNum = e.getLocation() != null ? (int) e.getLocation().getColumnNr() : -1;
            
            log.error("❌ JSON Parse Error at line {}, column {}: {}", lineNum, colNum, e.getMessage());
            
            // Log snippet around error location for debugging
            if (lineNum > 0 && colNum > 0) {
                String[] lines = aiJsonResponse.split("\n");
                if (lineNum <= lines.length) {
                    int startLine = Math.max(0, lineNum - 3);
                    int endLine = Math.min(lines.length, lineNum + 2);
                    
                    StringBuilder context = new StringBuilder("\n=== JSON CONTEXT AROUND ERROR ===\n");
                    for (int i = startLine; i < endLine; i++) {
                        String marker = (i == lineNum - 1) ? " >>> ERROR HERE >>>" : "";
                        context.append(String.format("Line %d: %s%s\n", i + 1, lines[i], marker));
                    }
                    context.append("=================================");
                    log.error(context.toString());
                }
            }
            
            log.warn("⚠️ Skipping entity parsing due to JSON parse error - This means nodes will have NO detailed entity data");
            
            // FALLBACK: Create default entities for all existing nodes
            log.info("🔧 Attempting to create fallback entities from node content...");
            createFallbackEntitiesForNodes(nodes);
            return;
        } catch (Exception e) {
            log.error("❌ Failed to parse JSON response: {}", e.getMessage());
            log.warn("⚠️ Skipping entity parsing due to JSON parse error - This means nodes will have NO detailed entity data");
            
            // FALLBACK: Create default entities for all existing nodes
            log.info("🔧 Attempting to create fallback entities from node content...");
            createFallbackEntitiesForNodes(nodes);
            return;
        }

        try {
            JsonNode branches = root.path("branches");

            if (!branches.isArray()) {
                log.warn("⚠️ No branches array found for parsing related entities - AI response structure is wrong");
                log.debug("Root keys available: {}", root.fieldNames());
                return;
            }

            log.info("Found {} branches in AI response for parsing", branches.size());
            int nodeIndex = 1; // Skip root node at index 0
            int entitiesParsed = 0;

            for (JsonNode branch : branches) {
                // Parse branch-level entities BASED ON NODE TYPE
                if (nodeIndex < nodes.size()) {
                    MindmapNode branchNode = nodes.get(nodeIndex);
                    log.debug("Parsing branch node #{}: '{}' (type={})", nodeIndex, branchNode.getTitle(), branchNode.getNodeType());
                    parseEntitiesForNodeType(branch, branchNode);
                    entitiesParsed++;
                    nodeIndex++;
                }

                // Parse sub-branch-level entities BASED ON NODE TYPE
                JsonNode subBranches = branch.path("subBranches");
                if (subBranches.isArray()) {
                    log.debug("Found {} sub-branches to parse", subBranches.size());
                    for (JsonNode subBranch : subBranches) {
                        if (nodeIndex < nodes.size()) {
                            MindmapNode subNode = nodes.get(nodeIndex);
                            log.debug("Parsing sub-branch node #{}: '{}' (type={})", nodeIndex, subNode.getTitle(), subNode.getNodeType());
                            parseEntitiesForNodeType(subBranch, subNode);
                            entitiesParsed++;
                            nodeIndex++;
                        }
                    }
                }
            }

            log.info("✓ Successfully parsed and saved related entities for {} nodes", entitiesParsed);

        } catch (Exception e) {
            log.error("❌ Failed to parse related entities: {}", e.getMessage(), e);
            log.error("This means some nodes may not have their concept/formula/exercise data!");
        }
    }

    /**
     * Parse entities based on node type - only parse relevant entity data
     */
    private void parseEntitiesForNodeType(JsonNode jsonNode, MindmapNode node) {
        log.debug("Parsing entities for node '{}' with type={}", node.getTitle(), node.getNodeType());
        
        switch (node.getNodeType()) {
            case CONCEPT:
                parseConcepts(jsonNode, node);
                break;
            case FORMULA:
                parseFormulas(jsonNode, node);
                break;
            case EXERCISE:
                parseExercises(jsonNode, node);
                break;
            default:
                log.warn("Unknown node type {} for node '{}'", node.getNodeType(), node.getTitle());
        }
    }

    /**
     * Parse concepts from JSON node
     */
    private void parseConcepts(JsonNode jsonNode, MindmapNode node) {
        try {
            JsonNode conceptNode = jsonNode.path("concept");
            if (!conceptNode.isMissingNode() && !conceptNode.isNull() && hasValidConceptData(conceptNode)) {
                Concept concept = new Concept();
                concept.setNodeId(node.getId());
                concept.setName(conceptNode.path("name").asText(node.getTitle()));

                // Get definition - prefer from concept object, fallback to node content
                String definition = conceptNode.path("definition").asText("");
                if (definition.isEmpty() || definition.equals("...")) {
                    definition = node.getContent() != null ? node.getContent() : node.getTitle();
                }
                concept.setDefinition(definition);

                // Get explanation - prefer from concept object, fallback to node content
                String explanation = conceptNode.path("explanation").asText("");
                if (explanation.isEmpty() || explanation.equals("...")) {
                    explanation = node.getContent() != null ? node.getContent() : "";
                }
                concept.setExplanation(explanation);

                // Parse examples (array or single string)
                JsonNode examples = conceptNode.path("examples");
                if (examples.isArray()) {
                    StringBuilder examplesText = new StringBuilder();
                    for (JsonNode example : examples) {
                        examplesText.append("• ").append(example.asText()).append("\n");
                    }
                    concept.setExamples(examplesText.toString());
                } else if (examples.isTextual()) {
                    concept.setExamples(examples.asText());
                }

                // Parse key points
                JsonNode keyPoints = conceptNode.path("keyPoints");
                if (keyPoints.isArray()) {
                    StringBuilder keyPointsText = new StringBuilder();
                    for (JsonNode point : keyPoints) {
                        keyPointsText.append("→ ").append(point.asText()).append("\n");
                    }
                    concept.setKeyPoints(keyPointsText.toString());
                } else if (keyPoints.isTextual()) {
                    concept.setKeyPoints(keyPoints.asText());
                }

                // Parse common mistakes
                JsonNode commonMistakes = conceptNode.path("commonMistakes");
                if (commonMistakes.isArray()) {
                    StringBuilder mistakesText = new StringBuilder();
                    for (JsonNode mistake : commonMistakes) {
                        mistakesText.append("✗ ").append(mistake.asText()).append("\n");
                    }
                    concept.setCommonMistakes(mistakesText.toString());
                } else if (commonMistakes.isTextual()) {
                    concept.setCommonMistakes(commonMistakes.asText());
                }

                // Parse tips
                concept.setTips(conceptNode.path("tips").asText(""));

                // Parse prerequisites
                concept.setPrerequisites(conceptNode.path("prerequisites").asText(""));

                // Parse related concepts
                concept.setRelatedConcepts(conceptNode.path("relatedConcepts").asText(""));

                concept.setCreatedAt(LocalDateTime.now());
                concept.setUpdatedAt(LocalDateTime.now());
                conceptRepository.save(concept);
                log.debug("Saved concept for node: {} (definition: {} chars, explanation: {} chars, keyPoints: {} chars)",
                    node.getTitle(), concept.getDefinition().length(), concept.getExplanation().length(),
                    concept.getKeyPoints() != null ? concept.getKeyPoints().length() : 0);
            } else {
                // FALLBACK: Create default concept from node content when AI data is missing
                log.warn("⚠️  Node '{}' (type=CONCEPT) missing entity data from AI - Creating DEFAULT CONCEPT as fallback",
                    node.getTitle());
                
                Concept defaultConcept = createDefaultConcept(node);
                conceptRepository.save(defaultConcept);
                
                log.info("✓ Created DEFAULT concept for node '{}' (marked as AI_INCOMPLETE)", node.getTitle());
            }
        } catch (Exception e) {
            log.warn("Failed to parse concept for node {}: {}", node.getTitle(), e.getMessage());
        }
    }

    /**
     * Validate that concept has actual content (not just empty placeholders)
     * Now lenient - allow if concept object exists, will fallback to node content
     */
    private boolean hasValidConceptData(JsonNode conceptNode) {
        // Just check if concept object is not completely empty
        // Content will come from either concept object or node.content fallback
        return conceptNode.has("name") ||
               conceptNode.has("definition") ||
               conceptNode.has("explanation");
    }

    /**
     * Parse formulas from JSON node
     */
    private void parseFormulas(JsonNode jsonNode, MindmapNode node) {
        try {
            JsonNode formulas = jsonNode.path("formulas");
            if (formulas.isArray() && formulas.size() > 0) {
                int savedCount = 0;
                for (JsonNode formulaNode : formulas) {
                    // Validate formula has actual content
                    if (!hasValidFormulaData(formulaNode)) {
                        log.warn("Formula in node '{}' has incomplete data, skipping", node.getTitle());
                        continue;
                    }

                    Formula formula = new Formula();
                    formula.setNodeId(node.getId());
                    formula.setName(formulaNode.path("name").asText(""));
                    formula.setFormulaText(formulaNode.path("formulaText").asText(""));
                    formula.setFormulaLatex(formulaNode.path("formulaLatex").asText(""));

                    // Get description - prefer from formula object, fallback to node content
                    String description = formulaNode.path("description").asText("");
                    if (description.isEmpty() || description.equals("...")) {
                        description = node.getContent() != null ? node.getContent() : "";
                    }
                    formula.setDescription(description);

                    // Parse variables - handle both string and array format
                    JsonNode variables = formulaNode.path("variables");
                    if (variables.isArray()) {
                        StringBuilder varsText = new StringBuilder();
                        for (JsonNode var : variables) {
                            varsText.append(var.path("symbol").asText())
                                   .append(": ")
                                   .append(var.path("meaning").asText())
                                   .append("\n");
                        }
                        formula.setVariables(varsText.toString());
                    } else if (variables.isTextual()) {
                        formula.setVariables(variables.asText());
                    }

                    formula.setUsageExample(formulaNode.path("usageExample").asText(""));

                    // Parse conditions
                    formula.setConditions(formulaNode.path("conditions").asText(""));

                    // Set as primary formula if only one formula for this node
                    formula.setIsPrimary(savedCount == 0);

                    formula.setCreatedAt(LocalDateTime.now());
                    formula.setUpdatedAt(LocalDateTime.now());
                    formulaRepository.save(formula);
                    log.debug("Saved formula '{}' for node: {} (text: {} chars, description: {} chars)",
                        formula.getName(), node.getTitle(),
                        formula.getFormulaText().length(), formula.getDescription().length());
                    savedCount++;
                }
                log.debug("Saved {} formula(s) for node: {}", savedCount, node.getTitle());
            } else {
                if (node.getNodeType() == MindmapNode.NodeType.FORMULA) {
                    // FALLBACK: Create default formula when AI data is missing
                    log.warn("⚠️  Node '{}' (type=FORMULA) missing entity data from AI - Creating DEFAULT FORMULA as fallback",
                        node.getTitle());
                    
                    Formula defaultFormula = createDefaultFormula(node);
                    formulaRepository.save(defaultFormula);
                    
                    log.info("✓ Created DEFAULT formula for node '{}' (marked as AI_INCOMPLETE)", node.getTitle());
                }
            }
        } catch (Exception e) {
            log.warn("Failed to parse formulas for node {}: {}", node.getTitle(), e.getMessage());
        }
    }

    /**
     * Validate that formula has actual content (not placeholders)
     * Now lenient - allow if formula object exists, will fallback to node content
     */
    private boolean hasValidFormulaData(JsonNode formulaNode) {
        // Just check if formula object is not completely empty
        // Content will come from either formula object or node.content fallback
        return formulaNode.has("name") ||
               formulaNode.has("formulaText") ||
               formulaNode.has("formulaLatex");
    }

    /**
     * Parse exercises from JSON node
     */
    private void parseExercises(JsonNode jsonNode, MindmapNode node) {
        try {
            JsonNode exercises = jsonNode.path("exercises");
            if (exercises.isArray() && exercises.size() > 0) {
                int savedCount = 0;
                for (JsonNode exerciseNode : exercises) {
                    // Validate exercise has actual content
                    if (!hasValidExerciseData(exerciseNode)) {
                        log.warn("Exercise in node '{}' has incomplete data, skipping", node.getTitle());
                        continue;
                    }

                    Exercise exercise = new Exercise();
                    exercise.setNodeId(node.getId());
                    exercise.setQuestion(exerciseNode.path("question").asText(""));
                    exercise.setAnswer(exerciseNode.path("answer").asText(""));

                    // Get solution - prefer from exercise object, fallback to node content
                    String solution = exerciseNode.path("solution").asText("");
                    if (solution.isEmpty() || solution.equals("...")) {
                        solution = node.getContent() != null ? node.getContent() : "";
                    }
                    exercise.setSolution(solution);

                    // Parse difficulty
                    String difficulty = exerciseNode.path("difficulty").asText("medium");
                    try {
                        exercise.setDifficulty(Exercise.DifficultyLevel.valueOf(difficulty.toUpperCase()));
                    } catch (Exception e) {
                        log.debug("Invalid difficulty value '{}', defaulting to MEDIUM", difficulty);
                        exercise.setDifficulty(Exercise.DifficultyLevel.MEDIUM);
                    }

                    // Parse cognitive level - handle both English and Vietnamese
                    String cognitiveLevelStr = exerciseNode.path("cognitiveLevel").asText("understand");
                    Exercise.CognitiveLevel cognitiveLevel;
                    switch (cognitiveLevelStr.toLowerCase()) {
                        case "remember":
                        case "nhận biết":
                            cognitiveLevel = Exercise.CognitiveLevel.RECOGNITION;
                            break;
                        case "understand":
                        case "thông hiểu":
                            cognitiveLevel = Exercise.CognitiveLevel.COMPREHENSION;
                            break;
                        case "apply":
                        case "vận dụng":
                            cognitiveLevel = Exercise.CognitiveLevel.APPLICATION;
                            break;
                        case "analyze":
                        case "vận dụng cao":
                            cognitiveLevel = Exercise.CognitiveLevel.ADVANCED_APPLICATION;
                            break;
                        default:
                            cognitiveLevel = Exercise.CognitiveLevel.COMPREHENSION;
                    }
                    exercise.setCognitiveLevel(cognitiveLevel);

                    // Parse hints - handle both string and array
                    JsonNode hints = exerciseNode.path("hints");
                    if (hints.isArray()) {
                        StringBuilder hintsText = new StringBuilder();
                        for (JsonNode hint : hints) {
                            hintsText.append("💡 ").append(hint.asText()).append("\n");
                        }
                        exercise.setHints(hintsText.toString());
                    } else if (hints.isTextual()) {
                        exercise.setHints(hints.asText());
                    }

                    // Parse estimated time
                    if (exerciseNode.has("estimatedTime")) {
                        exercise.setEstimatedTime(exerciseNode.path("estimatedTime").asInt(10));
                    } else {
                        exercise.setEstimatedTime(10); // Default 10 minutes
                    }

                    // Mark as active
                    exercise.setIsActive(true);

                    // Set created by system user (will be -1 for AI-generated)
                    exercise.setCreatedBy(-1L); // System user ID for AI-generated content

                    exercise.setCreatedAt(LocalDateTime.now());
                    exercise.setUpdatedAt(LocalDateTime.now());
                    exerciseRepository.save(exercise);
                    log.debug("Saved exercise for node: {} (question: {} chars, solution: {} chars, difficulty: {}, cogLevel: {})",
                        node.getTitle(), exercise.getQuestion().length(),
                        exercise.getSolution() != null ? exercise.getSolution().length() : 0,
                        exercise.getDifficulty(), exercise.getCognitiveLevel());
                    savedCount++;
                }
                log.debug("Saved {} exercise(s) for node: {}", savedCount, node.getTitle());
            } else {
                if (node.getNodeType() == MindmapNode.NodeType.EXERCISE) {
                    // FALLBACK: Create default exercise when AI data is missing
                    log.warn("⚠️  Node '{}' (type=EXERCISE) missing entity data from AI - Creating DEFAULT EXERCISE as fallback",
                        node.getTitle());
                    
                    Exercise defaultExercise = createDefaultExercise(node);
                    exerciseRepository.save(defaultExercise);
                    
                    log.info("✓ Created DEFAULT exercise for node '{}' (marked as AI_INCOMPLETE)", node.getTitle());
                }
            }
        } catch (Exception e) {
            log.warn("Failed to parse exercises for node {}: {}", node.getTitle(), e.getMessage());
        }
    }

    /**
     * Validate that exercise has actual content (not placeholders)
     * Now lenient - allow if exercise object exists, will fallback to node content
     */
    private boolean hasValidExerciseData(JsonNode exerciseNode) {
        // Just check if exercise object is not completely empty
        // Content will come from either exercise object or node.content fallback
        return exerciseNode.has("question") ||
               exerciseNode.has("answer") ||
               exerciseNode.has("solution");
    }

    /**
     * Create default concept when AI data is missing or incomplete
     * Uses node.content and node.title as fallback
     */
    private Concept createDefaultConcept(MindmapNode node) {
        Concept concept = new Concept();
        concept.setNodeId(node.getId());
        concept.setName(node.getTitle());
        
        // Use node content as definition
        String content = node.getContent() != null && !node.getContent().trim().isEmpty() 
            ? node.getContent() 
            : "Đây là khái niệm về " + node.getTitle() + ". Nội dung chi tiết sẽ được bổ sung.";
        
        concept.setDefinition(content);
        concept.setExplanation("📝 " + content + "\n\n⚠️ Lưu ý: Đây là nội dung mặc định được tạo tự động do AI không cung cấp đầy đủ dữ liệu concept. Vui lòng cập nhật hoặc regenerate để có nội dung chi tiết hơn.");
        concept.setExamples("• Ví dụ sẽ được bổ sung\n⚠️ Đây là default data - cần regenerate");
        concept.setKeyPoints("✓ Điểm quan trọng sẽ được bổ sung\n⚠️ Default data");
        concept.setCommonMistakes("⚠️ Sai lầm thường gặp sẽ được bổ sung\n⚠️ Default data");
        concept.setTips("💡 Mẹo ghi nhớ sẽ được bổ sung (Default data)");
        
        concept.setCreatedAt(LocalDateTime.now());
        concept.setUpdatedAt(LocalDateTime.now());
        
        return concept;
    }

    /**
     * Create default formula when AI data is missing or incomplete
     */
    private Formula createDefaultFormula(MindmapNode node) {
        Formula formula = new Formula();
        formula.setNodeId(node.getId());
        formula.setName(node.getTitle());
        
        String content = node.getContent() != null && !node.getContent().trim().isEmpty()
            ? node.getContent()
            : "Công thức " + node.getTitle();
        
        formula.setFormulaText(content);
        formula.setFormulaLatex(content);
        formula.setVariables("⚠️ Biến số sẽ được giải thích chi tiết khi regenerate");
        formula.setDescription("📐 " + content + "\n\n⚠️ Lưu ý: Đây là công thức mặc định được tạo tự động do AI không cung cấp đầy đủ dữ liệu. Vui lòng regenerate để có công thức LaTeX chính xác và giải thích chi tiết.");
        formula.setUsageExample("VD: Ví dụ áp dụng sẽ được bổ sung\n⚠️ Default data - cần regenerate");
        formula.setIsPrimary(true);
        formula.setOrderIndex(0);
        
        formula.setCreatedAt(LocalDateTime.now());
        formula.setUpdatedAt(LocalDateTime.now());
        
        return formula;
    }

    /**
     * Create default exercise when AI data is missing or incomplete
     */
    private Exercise createDefaultExercise(MindmapNode node) {
        Exercise exercise = new Exercise();
        exercise.setNodeId(node.getId());
        
        String content = node.getContent() != null && !node.getContent().trim().isEmpty()
            ? node.getContent()
            : node.getTitle();
        
        exercise.setQuestion("❓ Câu hỏi về " + node.getTitle() + "\n⚠️ Đây là default question - cần regenerate để có bài tập cụ thể");
        exercise.setAnswer("Đáp án sẽ được bổ sung (Default)");
        exercise.setSolution("📝 Lời giải chi tiết:\n\n" + content + "\n\n⚠️ Lưu ý: Đây là bài tập mặc định được tạo tự động do AI không cung cấp đầy đủ dữ liệu. Vui lòng regenerate để có bài tập với câu hỏi cụ thể, lời giải từng bước chi tiết.");
        exercise.setDifficulty(Exercise.DifficultyLevel.MEDIUM);
        exercise.setCognitiveLevel(Exercise.CognitiveLevel.COMPREHENSION);
        exercise.setHints("💡 Gợi ý sẽ được bổ sung\n⚠️ Default data");
        exercise.setEstimatedTime(10);
        exercise.setOrderIndex(0);
        exercise.setIsActive(true);
        exercise.setCreatedBy(-1L); // System generated
        exercise.setCreatedAt(LocalDateTime.now());
        exercise.setUpdatedAt(LocalDateTime.now());
        
        return exercise;
    }

    /**
     * Create fallback entities for all nodes when JSON parsing fails.
     * This ensures that even if AI JSON is malformed, nodes still have basic entity data.
     * 
     * @param nodes List of nodes that were created but have no entities
     */
    private void createFallbackEntitiesForNodes(List<MindmapNode> nodes) {
        if (nodes == null || nodes.isEmpty()) {
            log.warn("No nodes provided for fallback entity creation");
            return;
        }

        int conceptCount = 0;
        int formulaCount = 0;
        int exerciseCount = 0;

        for (MindmapNode node : nodes) {
            // Skip root node (level 0 or ROOT type)
            if (node.getLevel() == 0 || node.getNodeType() == MindmapNode.NodeType.ROOT) {
                continue;
            }

            try {
                switch (node.getNodeType()) {
                    case CONCEPT:
                        Concept concept = createDefaultConcept(node);
                        conceptRepository.save(concept);
                        conceptCount++;
                        log.debug("Created fallback concept for node: {}", node.getTitle());
                        break;
                        
                    case FORMULA:
                        Formula formula = createDefaultFormula(node);
                        formulaRepository.save(formula);
                        formulaCount++;
                        log.debug("Created fallback formula for node: {}", node.getTitle());
                        break;
                        
                    case EXERCISE:
                        Exercise exercise = createDefaultExercise(node);
                        exerciseRepository.save(exercise);
                        exerciseCount++;
                        log.debug("Created fallback exercise for node: {}", node.getTitle());
                        break;
                        
                    default:
                        log.warn("Unknown node type {} for node '{}', skipping fallback entity", 
                            node.getNodeType(), node.getTitle());
                }
            } catch (Exception e) {
                log.error("Failed to create fallback entity for node {}: {}", node.getTitle(), e.getMessage());
            }
        }

        log.info("✓ Created {} fallback entities: {} concepts, {} formulas, {} exercises",
            (conceptCount + formulaCount + exerciseCount), conceptCount, formulaCount, exerciseCount);
    }

    /**
     * Parse exercises from AI JSON response for exercise generation
     */
    private List<Exercise> parseExercisesFromAiResponse(String aiJsonResponse, Long nodeId, Long userId) {
        List<Exercise> exercises = new ArrayList<>();

        try {
            log.info("Parsing exercises from AI response");
            
            // Clean JSON: Escape unescaped newlines and control characters
            String cleanedJson = cleanJsonString(aiJsonResponse);
            
            JsonNode root = objectMapper.readTree(cleanedJson);

            JsonNode exercisesArray = root.path("exercises");
            if (!exercisesArray.isArray()) {
                log.warn("No 'exercises' array found in AI response");
                return exercises;
            }

            int orderIndex = 0;
            for (JsonNode exerciseNode : exercisesArray) {
                try {
                    Exercise exercise = new Exercise();
                    exercise.setNodeId(nodeId);

                    // Required fields
                    String question = exerciseNode.path("question").asText("").trim();
                    String answer = exerciseNode.path("answer").asText("").trim();
                    String solution = exerciseNode.path("solution").asText("").trim();

                    if (question.isEmpty() || answer.isEmpty()) {
                        log.warn("Exercise missing question or answer, skipping");
                        continue;
                    }

                    exercise.setQuestion(question);
                    exercise.setAnswer(answer);
                    exercise.setSolution(solution.isEmpty() ? answer : solution);

                    // Parse difficulty
                    String difficulty = exerciseNode.path("difficulty").asText("medium").toUpperCase();
                    try {
                        exercise.setDifficulty(Exercise.DifficultyLevel.valueOf(difficulty));
                    } catch (Exception e) {
                        log.debug("Invalid difficulty '{}', defaulting to MEDIUM", difficulty);
                        exercise.setDifficulty(Exercise.DifficultyLevel.MEDIUM);
                    }

                    // Parse cognitive level
                    String cognitiveLevelStr = exerciseNode.path("cognitiveLevel").asText("comprehension").toLowerCase();
                    Exercise.CognitiveLevel cognitiveLevel;
                    switch (cognitiveLevelStr) {
                        case "recognition":
                        case "nhận biết":
                            cognitiveLevel = Exercise.CognitiveLevel.RECOGNITION;
                            break;
                        case "comprehension":
                        case "thông hiểu":
                            cognitiveLevel = Exercise.CognitiveLevel.COMPREHENSION;
                            break;
                        case "application":
                        case "vận dụng":
                            cognitiveLevel = Exercise.CognitiveLevel.APPLICATION;
                            break;
                        case "advanced_application":
                        case "vận dụng cao":
                            cognitiveLevel = Exercise.CognitiveLevel.ADVANCED_APPLICATION;
                            break;
                        default:
                            cognitiveLevel = Exercise.CognitiveLevel.COMPREHENSION;
                    }
                    exercise.setCognitiveLevel(cognitiveLevel);

                    // Parse hints
                    JsonNode hints = exerciseNode.path("hints");
                    if (hints.isArray()) {
                        StringBuilder hintsText = new StringBuilder();
                        for (JsonNode hint : hints) {
                            hintsText.append("💡 ").append(hint.asText()).append("\n");
                        }
                        exercise.setHints(hintsText.toString());
                    } else if (hints.isTextual()) {
                        exercise.setHints(hints.asText());
                    }

                    // Parse estimated time
                    if (exerciseNode.has("estimatedTime")) {
                        exercise.setEstimatedTime(exerciseNode.path("estimatedTime").asInt(10));
                    } else {
                        exercise.setEstimatedTime(10);
                    }

                    // Set defaults
                    exercise.setOrderIndex(orderIndex++);
                    exercise.setIsActive(true);
                    exercise.setCreatedBy(userId);
                    exercise.setCreatedAt(LocalDateTime.now());
                    exercise.setUpdatedAt(LocalDateTime.now());

                    exercises.add(exercise);
                    log.debug("Parsed exercise: {} (difficulty: {}, cogLevel: {})",
                            exercise.getQuestion().substring(0, Math.min(50, exercise.getQuestion().length())),
                            exercise.getDifficulty(), exercise.getCognitiveLevel());

                } catch (Exception e) {
                    log.warn("Failed to parse exercise: {}", e.getMessage());
                }
            }

            log.info("Successfully parsed {} exercises from AI response", exercises.size());
            return exercises;

        } catch (Exception e) {
            log.error("Error parsing exercises JSON: {}", e.getMessage(), e);
            return exercises;
        }
    }
}
