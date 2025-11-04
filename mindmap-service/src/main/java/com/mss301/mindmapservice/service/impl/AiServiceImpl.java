package com.mss301.mindmapservice.service.impl;

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
        prompt.append("Yêu cầu: TỐI THIỂU 15-20 nodes (4-5 branches, mỗi branch có 3-4 subBranches). ");
        prompt.append("Mỗi node phải có content đầy đủ 200-500 từ với emoji, công thức toán học (², ³, √, Δ), ");
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
        prompt.append("\nCẤU TRÚC JSON MINDMAP YÊU CẦU:\n\n");
        prompt.append("QUAN TRỌNG: Mỗi node PHẢI có nodeType phù hợp và dữ liệu entity tương ứng:\n");
        prompt.append("- nodeType=\"CONCEPT\": PHẢI có object \"concept\" với: name, definition (bắt buộc), explanation (chi tiết 200-500 từ), examples (array), keyPoints (array), commonMistakes (array)\n");
        prompt.append("- nodeType=\"FORMULA\": PHẢI có array \"formulas\" với: name, formulaText, formulaLatex, description (chi tiết công thức), usageExample (ví dụ áp dụng), variables (giải thích từng biến số)\n");
        prompt.append("- nodeType=\"EXERCISE\": PHẢI có array \"exercises\" với: question (bắt buộc), answer (đáp án), solution (lời giải chi tiết 100+ từ), difficulty, cognitiveLevel, hints\n\n");
        
        prompt.append("YÊU CẦU ĐA DẠNG LOẠI NODE: Không tạo tất cả các node là CONCEPT!\n");
        prompt.append("- Branches chính (4-5 nodes): Dùng nodeType=\"CONCEPT\" cho định nghĩa tổng quan\n");
        prompt.append("- Sub-branches (10-15 nodes): Pha trộn 40% FORMULA + 40% EXERCISE + 20% CONCEPT\n");
        prompt.append("- Mỗi khái niệm quan trọng cần có công thức (formula) và bài tập (exercise) đi kèm\n\n");
        
        prompt.append("{\n");
        prompt.append("  \"centralTopic\": \"").append(request.getTopic()).append("\",\n");
        prompt.append("  \"branches\": [\n");
        prompt.append("    {\n");
        prompt.append("      \"title\": \"Khái niệm tổng quan\",\n");
        prompt.append("      \"description\": \"Mô tả chi tiết 200-500 từ\",\n");
        prompt.append("      \"nodeType\": \"CONCEPT\",\n");
        prompt.append("      \"concept\": {\n");
        prompt.append("        \"name\": \"Tên khái niệm\",\n");
        prompt.append("        \"definition\": \"Định nghĩa chính xác của khái niệm\",\n");
        prompt.append("        \"explanation\": \"Giải thích chi tiết với emoji 📐 và ký hiệu toán học x², √\",\n");
        prompt.append("        \"examples\": [\"Ví dụ 1 cụ thể với số\", \"Ví dụ 2 thực tế\"]\n");
        prompt.append("      },\n");
        prompt.append("      \"subBranches\": [\n");
        prompt.append("        {\n");
        prompt.append("          \"title\": \"Công thức tính toán\",\n");
        prompt.append("          \"content\": \"Nội dung giải thích công thức 200-500 từ\",\n");
        prompt.append("          \"nodeType\": \"FORMULA\",\n");
        prompt.append("          \"formulas\": [\n");
        prompt.append("            {\n");
        prompt.append("              \"name\": \"Tên công thức\",\n");
        prompt.append("              \"formulaText\": \"Công thức dạng text: a² + b² = c²\",\n");
        prompt.append("              \"formulaLatex\": \"a^2 + b^2 = c^2\",\n");
        prompt.append("              \"variables\": \"a: cạnh góc vuông thứ nhất, b: cạnh góc vuông thứ hai, c: cạnh huyền\",\n");
        prompt.append("              \"usageExample\": \"Ví dụ: Tam giác vuông có a=3, b=4 thì c=√(3²+4²)=5\"\n");
        prompt.append("            }\n");
        prompt.append("          ]\n");
        prompt.append("        },\n");
        prompt.append("        {\n");
        prompt.append("          \"title\": \"Bài tập thực hành\",\n");
        prompt.append("          \"content\": \"Hướng dẫn làm bài tập 200-500 từ\",\n");
        prompt.append("          \"nodeType\": \"EXERCISE\",\n");
        prompt.append("          \"exercises\": [\n");
        prompt.append("            {\n");
        prompt.append("              \"question\": \"Câu hỏi bài tập cụ thể\",\n");
        prompt.append("              \"answer\": \"Đáp án ngắn gọn\",\n");
        prompt.append("              \"solution\": \"Lời giải chi tiết từng bước\",\n");
        prompt.append("              \"difficulty\": \"easy|medium|hard\",\n");
        prompt.append("              \"cognitiveLevel\": \"remember|understand|apply|analyze\",\n");
        prompt.append("              \"hints\": \"Gợi ý: Áp dụng công thức...\"\n");
        prompt.append("            }\n");
        prompt.append("          ]\n");
        prompt.append("        }\n");
        prompt.append("      ]\n");
        prompt.append("    }\n");
        prompt.append("  ]\n");
        prompt.append("}\n\n");
        
        // Professional rules
        prompt.append("QUY TẮC CHUYÊN MÔN:\n");
        prompt.append("1. PHẠM VI KIẾN THỨC: Tự do mở rộng kiến thức từ nhiều nguồn, không bị giới hạn\n");
        prompt.append("2. ĐỘ SÂU: Đào sâu chi tiết với nhiều góc nhìn, ví dụ thực tế\n");
        prompt.append("3. SỐ LƯỢNG NODES: Tối thiểu 15-20 nodes đảm bảo độ chi tiết\n");
        prompt.append("4. NỘI DUNG: Mỗi node 200-500 từ với emoji (🔢 📐 ✏️ 🎯), công thức (x², √, ∫, Σ), ví dụ cụ thể\n");
        prompt.append("5. BÀI TẬP: Mỗi khái niệm quan trọng cần có bài tập với đáp án chi tiết\n");
        prompt.append("6. CÔNG THỨC: Bao gồm cả giải thích và ví dụ áp dụng\n\n");
        
        prompt.append("ĐỊNH DẠNG ĐẦU RA:\n");
        prompt.append("CHỈ TRẢ VỀ JSON THUẦN TÚY, KHÔNG CÓ TEXT GIẢI THÍCH THÊM, KHÔNG CÓ MARKDOWN CODE BLOCK.\n");
        
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
                        .maxOutputTokens(8000)  // Increased for detailed content
                        .temperature(0.7)
                        .topP(0.95)
                        .topK(40.0)
                        .build())
                .build();

        WebClient webClient = webClientBuilder
                .baseUrl(geminiBaseUrl)
                .defaultHeader(HttpHeaders.CONTENT_TYPE, MediaType.APPLICATION_JSON_VALUE)
                .build();

        log.debug("Calling Gemini API with model: {}", model);
        
        GeminiResponse response = webClient
                .post()
                .uri("/models/{model}:generateContent?key={apiKey}", model, geminiApiKey)
                .bodyValue(request)
                .retrieve()
                .bodyToMono(GeminiResponse.class)
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
        mindmap.setIsPublic(false);
        mindmap.setIsAiGenerated(true);
        mindmap.setAiProvider("gemini");
        mindmap.setAiModel(request.getAiModel() != null ? request.getAiModel() : geminiPrimaryModel);
        mindmap.setCreatedAt(LocalDateTime.now());
        mindmap.setUpdatedAt(LocalDateTime.now());
        return mindmap;
    }

    /**
     * Parse nodes from AI JSON response
     */
    private List<MindmapNode> parseNodesFromAiResponse(Mindmap mindmap, String aiJsonResponse) {
        List<MindmapNode> nodes = new ArrayList<>();

        try {
            log.info("Parsing AI response JSON for mindmap: {}", mindmap.getId());
            
            JsonNode root = objectMapper.readTree(aiJsonResponse);

            // Create central topic node (level 0)
            MindmapNode centralNode = new MindmapNode();
            centralNode.setMindmapId(mindmap.getId());
            centralNode.setTitle(root.path("centralTopic").asText(mindmap.getTitle()));
            centralNode.setContent("🎯 Chủ đề trung tâm: " + mindmap.getTitle());
            centralNode.setNodeType(MindmapNode.NodeType.CONCEPT);
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
        try {
            // Try to parse the JSON response
            root = objectMapper.readTree(aiJsonResponse);
        } catch (JsonEOFException e) {
            log.error("JSON response is truncated or malformed at line {}, column {}: {}", 
                e.getLocation() != null ? e.getLocation().getLineNr() : "?",
                e.getLocation() != null ? e.getLocation().getColumnNr() : "?",
                e.getMessage());
            log.warn("Skipping entity parsing due to malformed JSON response");
            return;
        } catch (Exception e) {
            log.error("Failed to parse JSON response: {}", e.getMessage());
            log.warn("Skipping entity parsing due to JSON parse error");
            return;
        }

        try {
            JsonNode branches = root.path("branches");

            if (!branches.isArray()) {
                log.warn("No branches array found for parsing related entities");
                return;
            }

            int nodeIndex = 1; // Skip root node at index 0

            for (JsonNode branch : branches) {
                // Parse branch-level entities BASED ON NODE TYPE
                if (nodeIndex < nodes.size()) {
                    MindmapNode branchNode = nodes.get(nodeIndex);
                    parseEntitiesForNodeType(branch, branchNode);
                    nodeIndex++;
                }

                // Parse sub-branch-level entities BASED ON NODE TYPE
                JsonNode subBranches = branch.path("subBranches");
                if (subBranches.isArray()) {
                    for (JsonNode subBranch : subBranches) {
                        if (nodeIndex < nodes.size()) {
                            MindmapNode subNode = nodes.get(nodeIndex);
                            parseEntitiesForNodeType(subBranch, subNode);
                            nodeIndex++;
                        }
                    }
                }
            }

            log.info("Parsed and saved related entities for {} nodes", nodeIndex);

        } catch (Exception e) {
            log.error("Failed to parse related entities: {}", e.getMessage(), e);
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
                log.warn("Node '{}' (type={}) is missing or has incomplete 'concept' object in AI response",
                    node.getTitle(), node.getNodeType());
                log.warn("Skipping concept creation - AI response must include complete concept data for CONCEPT nodes");
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
                    log.warn("Node '{}' has type=FORMULA but is missing 'formulas' array in AI response",
                        node.getTitle());
                    log.warn("Skipping formula creation - AI response must include formulas array for FORMULA nodes");
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
                    log.warn("Node '{}' has type=EXERCISE but is missing 'exercises' array in AI response",
                        node.getTitle());
                    log.warn("Skipping exercise creation - AI response must include exercises array for EXERCISE nodes");
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
     * Parse exercises from AI JSON response for exercise generation
     */
    private List<Exercise> parseExercisesFromAiResponse(String aiJsonResponse, Long nodeId, Long userId) {
        List<Exercise> exercises = new ArrayList<>();

        try {
            log.info("Parsing exercises from AI response");
            JsonNode root = objectMapper.readTree(aiJsonResponse);

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
