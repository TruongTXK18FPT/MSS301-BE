package com.mss301.contentservice.service.impl;

import java.io.IOException;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.function.Supplier;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.springframework.stereotype.Service;

import com.google.gson.Gson;
import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import com.mss301.contentservice.config.GeminiConfig;
import com.mss301.contentservice.dto.request.GenerateQuizRequest;
import com.mss301.contentservice.dto.request.QuizRequestPayload;
import com.mss301.contentservice.service.AiQuizService;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@Slf4j
@Service
@RequiredArgsConstructor
public class AiQuizServiceImpl implements AiQuizService {

    private final GeminiConfig geminiConfig;
    private final Gson gson = new Gson();
    private final HttpClient httpClient = HttpClient.newHttpClient();

    private static final int MAX_RETRY_ATTEMPTS = 3;
    private static final long INITIAL_BACKOFF_MILLIS = 1000L;
    private static final String FIELD_PARTS = "parts";
    private static final String MULTIPLE_CHOICE = "MULTIPLE_CHOICE";

    private static final String SYSTEM_PROMPT =
            """
            Bạn là một chuyên gia giáo dục toán học với nhiều năm kinh nghiệm tạo đề thi và bài tập.

            NHIỆM VỤ: Tạo câu hỏi quiz toán học chất lượng cao, không trùng lặp.

            QUY TẮC BẮT BUỘC:

            1. TÍNH DUY NHẤT (ANTI-DUPLICATION):
               - Mỗi câu hỏi phải có cách tiếp cận khác biệt về mặt toán học
               - Không được tạo các câu hỏi chỉ thay đổi số nhưng cùng dạng bài
               - Đa dạng hóa: phương trình, bất phương trình, hệ phương trình, hàm số, hình học, v.v.
               - Nếu cùng chủ đề, phải khác độ khó và phương pháp giải

            2. ĐỊNH DẠNG TOÁN HỌC (LaTeX):
               - Sử dụng LaTeX cho tất cả công thức: $x^2 + 5x + 6$ cho inline
               - Sử dụng $$....$$ cho công thức block (xuống dòng)
               - Phân số: $\\\\frac{a}{b}$
               - Căn bậc hai: $\\\\sqrt{x}$
               - Mũ: $x^{2n+1}$
               - Tổng: $\\\\sum_{i=1}^{n} a_i$
               - Tích phân: $\\\\int_a^b f(x)dx$
               - Ma trận: $\\\\begin{pmatrix} a & b \\\\\\\\ c & d \\\\end{pmatrix}$
               - Ký tự Hy Lạp: $\\\\alpha, \\\\beta, \\\\gamma, \\\\pi, \\\\theta$

            3. CẤU TRÚC CÂU HỎI:
               - Đề bài rõ ràng, đầy đủ
               - 4 đáp án cho trắc nghiệm (A, B, C, D)
               - Chỉ 1 đáp án đúng duy nhất
               - Các đáp án sai phải hợp lý (là lỗi phổ biến học sinh thường mắc)
               - Điểm số hợp lý (5-20 điểm tùy độ khó)

            4. ĐỘ KHÓ THEO LỚP:
               Lớp 6-7: Số học cơ bản, phân số, tỉ lệ
               Lớp 8-9: Phương trình bậc 1, 2, hệ phương trình, hàm số
               Lớp 10: Lượng giác, vectơ, hình học không gian
               Lớp 11: Đạo hàm, giới hạn, tổ hợp xác suất
               Lớp 12: Tích phân, số phức, hình học không gian nâng cao

            5. CÁC DẠNG BÀI ĐA DẠNG:
               - Giải phương trình/bất phương trình
               - Tính giá trị biểu thức
               - Chứng minh
               - Tìm điều kiện
               - Bài toán thực tế (có ứng dụng)
               - Hình học (tính độ dài, diện tích, thể tích)
               - Tổ hợp, xác suất
               - Hàm số (khảo sát, đồ thị)

            6. GIẢI THÍCH ĐÁP ÁN (EXPLANATION):
               - Mỗi câu hỏi PHẢI có phần giải thích chi tiết
               - Giải thích gồm: phương pháp giải, các bước thực hiện, kết quả
               - Sử dụng LaTeX cho các công thức trong giải thích
               - Giải thích phải ngắn gọn nhưng đầy đủ (3-5 dòng)
               - Giúp học sinh hiểu TẠI SAO đáp án đó đúng

            OUTPUT FORMAT (JSON):
            Trả về mảng JSON với cấu trúc:
            [
              {
                "text": "...",
                "type": "MULTIPLE_CHOICE",
                "points": 10,
                "explanation": "Giải thích chi tiết cách giải bài này...",
                "options": [
                  {"text": "...", "correct": false},
                  {"text": "...", "correct": true},
                  {"text": "...", "correct": false},
                  {"text": "...", "correct": false}
                ]
              }
            ]

            LƯU Ý QUAN TRỌNG:
            - Đảm bảo LaTeX syntax chính xác (double backslash)
            - Kiểm tra kỹ đáp án trước khi trả về
            - Không tạo câu hỏi có nhiều đáp án đúng
            - PHẢI có explanation cho mỗi câu hỏi
            - Chỉ trả về JSON array, không có text nào khác
            """;

    @Override
    public List<QuizRequestPayload.QuizQuestionRequest> generateQuizQuestions(
            GenerateQuizRequest request, List<String> existingQuestions) {

        String userPrompt = buildUserPrompt(request, existingQuestions);

        List<QuizRequestPayload.QuizQuestionRequest> questions;
        boolean usedFallback = false;

        try {
            String responseText = callGeminiApi(SYSTEM_PROMPT, userPrompt);
            questions = parseQuestions(responseText);
        } catch (GeminiApiException e) {
            if (e.isRetryable()) {
                log.warn(
                        "Gemini API tạm thời không khả dụng: {}. Sử dụng bộ câu hỏi dự phòng.",
                        e.getMessage());
                questions = buildFallbackQuestions(request, existingQuestions);
                usedFallback = true;
            } else {
                log.error("Gemini API gặp lỗi không thể khôi phục.", e);
                throw new QuizGenerationException("Lỗi tạo câu hỏi: " + e.getMessage(), e);
            }
        } catch (RuntimeException e) {
            log.error("Lỗi khi xử lý phản hồi từ Gemini", e);
            throw new QuizGenerationException("Lỗi tạo câu hỏi: " + e.getMessage(), e);
        }

        // Validate and filter
        List<QuizRequestPayload.QuizQuestionRequest> validQuestions = new ArrayList<>();
        for (QuizRequestPayload.QuizQuestionRequest question : questions) {
            if (validateQuestion(question, existingQuestions)) {
                validQuestions.add(question);
            } else {
                log.debug("Question rejected: {}", question.getText());
            }
        }

        // If all questions rejected due to high similarity, return them anyway with warning
        if (validQuestions.isEmpty() && !questions.isEmpty()) {
            log.warn(
                    "All questions rejected by validation. Returning {} questions with relaxed filter.",
                    questions.size());
            // Return first N questions regardless of duplicate check
            int numToReturn = Math.min(request.getNumQuestions(), questions.size());
            return questions.subList(0, numToReturn);
        }

        if (validQuestions.isEmpty()) {
            throw new QuizGenerationException("Không tạo được câu hỏi hợp lệ nào. Vui lòng thử lại.");
        }

        if (usedFallback) {
            log.info(
                    "Đã trả về {} câu hỏi dự phòng do Gemini API không khả dụng.",
                    validQuestions.size());
        }

        return validQuestions;
    }

    private String buildUserPrompt(GenerateQuizRequest request, List<String> existingQuestions) {
        StringBuilder prompt = new StringBuilder();
        prompt.append(String.format(
                "Tạo %d câu hỏi trắc nghiệm về chủ đề \"%s\" cho học sinh lớp %s.%n%n",
                request.getNumQuestions(), request.getTopic(), request.getGrade()));

        if (!existingQuestions.isEmpty()) {
            prompt.append("CÁC CÂU ĐÃ TỒN TẠI (KHÔNG ĐƯỢC TRÙNG LẶP):")
                    .append(System.lineSeparator());
            for (int i = 0; i < existingQuestions.size(); i++) {
                String q = existingQuestions.get(i);
                prompt.append(String.format(
                        "%d. %s%n", i + 1, q.substring(0, Math.min(100, q.length()))));
            }
            prompt.append(System.lineSeparator());
        }

        prompt.append("YÊU CẦU:").append(System.lineSeparator());
        prompt.append(String.format(
                "- Tạo %d câu hỏi HOÀN TOÀN MỚI, không trùng với các câu trên%n",
                request.getNumQuestions()));
        prompt.append("- Đa dạng về dạng toán và phương pháp giải").append(System.lineSeparator());
        prompt.append("- Sử dụng LaTeX cho công thức toán học").append(System.lineSeparator());
        prompt.append("- Mỗi câu có 4 đáp án, chỉ 1 đáp án đúng").append(System.lineSeparator());
        prompt.append(String.format("- Độ khó phù hợp với lớp %s%n", request.getGrade()));
        prompt.append("- Trả về JSON array như format đã chỉ định, KHÔNG CÓ TEXT NÀO KHÁC")
                .append(System.lineSeparator());

        return prompt.toString();
    }

    private String callGeminiApi(String systemPrompt, String userPrompt) {
        String url = String.format(
                "https://generativelanguage.googleapis.com/v1beta/models/%s:generateContent?key=%s",
                geminiConfig.getModel(), geminiConfig.getApiKey());

        JsonObject requestBody = new JsonObject();

        // System instructions
        JsonObject systemInstruction = new JsonObject();
        JsonObject systemPart = new JsonObject();
        systemPart.addProperty("text", systemPrompt);
        JsonArray systemParts = new JsonArray();
        systemParts.add(systemPart);
        systemInstruction.add(FIELD_PARTS, systemParts);
        requestBody.add("systemInstruction", systemInstruction);

        // Contents (user message)
        JsonArray contents = new JsonArray();
        JsonObject content = new JsonObject();
        content.addProperty("role", "user");
        JsonArray parts = new JsonArray();
        JsonObject part = new JsonObject();
        part.addProperty("text", userPrompt);
        parts.add(part);
        content.add(FIELD_PARTS, parts);
        contents.add(content);
        requestBody.add("contents", contents);

        // Generation config
        JsonObject generationConfig = new JsonObject();
        generationConfig.addProperty("temperature", geminiConfig.getTemperature());
        generationConfig.addProperty("maxOutputTokens", geminiConfig.getMaxOutputTokens());
        requestBody.add("generationConfig", generationConfig);

        HttpRequest httpRequest = HttpRequest.newBuilder()
                .uri(URI.create(url))
                .header("Content-Type", "application/json")
                .POST(HttpRequest.BodyPublishers.ofString(gson.toJson(requestBody)))
                .build();

        GeminiApiException lastError = null;

        for (int attempt = 1; attempt <= MAX_RETRY_ATTEMPTS; attempt++) {
            try {
                HttpResponse<String> response =
                        httpClient.send(httpRequest, HttpResponse.BodyHandlers.ofString());

                if (response.statusCode() == 200) {
                    return extractResponseText(response.body());
                }

                String errorMessage = extractErrorMessage(response.statusCode(), response.body());
                boolean retryable = isRetryableStatus(response.statusCode());

                if (!retryable) {
                    log.error("Gemini API trả về lỗi không thể retry: {}", errorMessage);
                    throw new GeminiApiException(errorMessage, false);
                }

                lastError = new GeminiApiException(errorMessage, true);
                log.warn(
                        "Gemini API tạm thời lỗi (attempt {}/{}): {}",
                        attempt,
                        MAX_RETRY_ATTEMPTS,
                        errorMessage);
            } catch (IOException e) {
                lastError = new GeminiApiException(
                        "Lỗi I/O khi gọi Gemini API: " + e.getMessage(), true, e);
                log.warn(
                        "Không thể kết nối tới Gemini API (attempt {}/{}): {}",
                        attempt,
                        MAX_RETRY_ATTEMPTS,
                        e.getMessage());
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
                throw new GeminiApiException("Luồng bị gián đoạn khi gọi Gemini API", true, e);
            }

            if (attempt < MAX_RETRY_ATTEMPTS) {
                sleepBeforeRetry(attempt);
            }
        }

        throw lastError != null
                ? lastError
                : new GeminiApiException("Gemini API lỗi không xác định", true);
    }

    private void sleepBeforeRetry(int attempt) {
        long backoffMillis = INITIAL_BACKOFF_MILLIS << (attempt - 1);
        try {
            Thread.sleep(backoffMillis);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            throw new GeminiApiException(
                    "Luồng bị gián đoạn khi chờ retry Gemini API", true, e);
        }
    }

    private String extractResponseText(String responseBody) {
        JsonObject responseJson = JsonParser.parseString(responseBody).getAsJsonObject();
        JsonArray candidates = responseJson.getAsJsonArray("candidates");
        if (candidates == null || candidates.isEmpty()) {
            throw new GeminiApiException("Không nhận được phản hồi từ Gemini API", true);
        }

        JsonObject candidate = candidates.get(0).getAsJsonObject();
        JsonObject contentObj = candidate.getAsJsonObject("content");
        if (contentObj == null) {
            throw new GeminiApiException("Phản hồi từ Gemini API thiếu nội dung", true);
        }

        JsonArray responseParts = contentObj.getAsJsonArray(FIELD_PARTS);
        if (responseParts == null || responseParts.isEmpty()) {
            throw new GeminiApiException("Phản hồi từ Gemini API thiếu phần văn bản", true);
        }

        JsonObject partObject = responseParts.get(0).getAsJsonObject();
        if (!partObject.has("text")) {
            throw new GeminiApiException("Phản hồi từ Gemini API thiếu trường text", true);
        }

        return partObject.get("text").getAsString();
    }

    private boolean isRetryableStatus(int statusCode) {
        return statusCode == 429 || statusCode == 500 || statusCode == 502 || statusCode == 503 || statusCode == 504;
    }

    private String extractErrorMessage(int statusCode, String responseBody) {
        try {
            JsonObject responseJson = JsonParser.parseString(responseBody).getAsJsonObject();
            if (responseJson.has("error")) {
                JsonObject error = responseJson.getAsJsonObject("error");
                StringBuilder builder = new StringBuilder();
                builder.append("Gemini API error ").append(statusCode);

                if (error.has("status")) {
                    builder.append(" (").append(error.get("status").getAsString()).append(")");
                }

                if (error.has("message")) {
                    builder.append(": ").append(error.get("message").getAsString());
                }

                return builder.toString();
            }
        } catch (Exception ex) {
            log.debug("Không phân tích được thông báo lỗi từ Gemini: {}", ex.getMessage());
        }

        return responseBody != null && !responseBody.isBlank()
                ? String.format("Gemini API error %d: %s", statusCode, responseBody)
                : String.format("Gemini API error: %d", statusCode);
    }

    /**
     * Sanitizes JSON text by fixing invalid escape sequences.
     * This prevents MalformedJsonException when AI returns improperly escaped strings.
     */
    private String sanitizeJsonEscapeSequences(String jsonText) {
        if (jsonText == null) {
            return null;
        }
        
        StringBuilder result = new StringBuilder();
        boolean inString = false;
        boolean escaped = false;
        
        for (int i = 0; i < jsonText.length(); i++) {
            char c = jsonText.charAt(i);
            
            // Track if we're inside a string literal
            if (c == '"' && !escaped) {
                inString = !inString;
                result.append(c);
                continue;
            }
            
            // Handle escape sequences only inside strings
            if (inString && c == '\\' && !escaped) {
                // Check if next character forms a valid escape sequence
                if (i + 1 < jsonText.length()) {
                    char next = jsonText.charAt(i + 1);
                    // Valid JSON escape sequences: " \ / b f n r t u
                    if (next == '"' || next == '\\' || next == '/' || 
                        next == 'b' || next == 'f' || next == 'n' || 
                        next == 'r' || next == 't' || next == 'u') {
                        // Valid escape - keep it
                        result.append(c);
                        escaped = true;
                        continue;
                    } else {
                        // Invalid escape - escape the backslash itself
                        result.append("\\\\");
                        continue;
                    }
                } else {
                    // Backslash at end of string - escape it
                    result.append("\\\\");
                    continue;
                }
            }
            
            escaped = false;
            result.append(c);
        }
        
        return result.toString();
    }

    private List<QuizRequestPayload.QuizQuestionRequest> parseQuestions(String responseText) {
        // Try to extract JSON from markdown code blocks
        Pattern jsonPattern = Pattern.compile("```(?:json)?\\s*\\n?(\\[[\\s\\S]*?\\])\\s*\\n?```");
        Matcher matcher = jsonPattern.matcher(responseText);
        String jsonText = responseText;
        if (matcher.find()) {
            jsonText = matcher.group(1);
        }

        // Remove any trailing text after JSON
        int lastBracket = jsonText.lastIndexOf(']');
        if (lastBracket != -1) {
            jsonText = jsonText.substring(0, lastBracket + 1);
        }

        // Sanitize JSON by fixing invalid escape sequences
        String originalJson = jsonText;
        jsonText = sanitizeJsonEscapeSequences(jsonText);
        
        if (!originalJson.equals(jsonText)) {
            log.debug("JSON was sanitized to fix escape sequences");
        }

        try {
            JsonArray jsonArray = JsonParser.parseString(jsonText).getAsJsonArray();
            List<QuizRequestPayload.QuizQuestionRequest> questions = new ArrayList<>();

            for (JsonElement element : jsonArray) {
                JsonObject obj = element.getAsJsonObject();

                List<QuizRequestPayload.QuizOptionRequest> options = new ArrayList<>();
                JsonArray optionsArray = obj.getAsJsonArray("options");
                for (JsonElement optElement : optionsArray) {
                    JsonObject optObj = optElement.getAsJsonObject();
                    options.add(QuizRequestPayload.QuizOptionRequest.builder()
                            .text(optObj.get("text").getAsString())
                            .correct(optObj.get("correct").getAsBoolean())
                            .build());
                }

                questions.add(QuizRequestPayload.QuizQuestionRequest.builder()
                        .text(obj.get("text").getAsString())
                        .type(obj.get("type").getAsString())
                        .points(obj.has("points") ? obj.get("points").getAsInt() : 10)
                        .explanation(obj.has("explanation") ? obj.get("explanation").getAsString() : null)
                        .options(options)
                        .build());
            }

            return questions;
        } catch (Exception e) {
            log.error("Failed to parse AI response: {}", jsonText, e);
            throw new QuizGenerationException("AI response không phải JSON hợp lệ: " + e.getMessage(), e);
        }
    }

    private List<QuizRequestPayload.QuizQuestionRequest> buildFallbackQuestions(
            GenerateQuizRequest request, List<String> existingQuestions) {
        String topicValue = request.getTopic();
        if (topicValue == null || topicValue.isBlank()) {
            topicValue = "toán học";
        }
        final String topic = topicValue;

        int requested = request.getNumQuestions() != null ? Math.max(1, request.getNumQuestions()) : 1;
        List<Supplier<QuizRequestPayload.QuizQuestionRequest>> generators = List.of(
                () -> createEquationFallbackQuestion(topic),
                () -> createExpressionFallbackQuestion(topic),
                () -> createGeometryFallbackQuestion(topic));

        Set<String> existingSet = new HashSet<>(existingQuestions);
        List<QuizRequestPayload.QuizQuestionRequest> fallback = new ArrayList<>();
        int index = 0;
        int guard = 0;
        int limit = requested + generators.size() * 2;

        while (fallback.size() < requested && guard < limit) {
            QuizRequestPayload.QuizQuestionRequest candidate =
                    generators.get(index % generators.size()).get();
            index++;
            guard++;

            if (existingSet.contains(candidate.getText())) {
                continue;
            }

            fallback.add(candidate);
        }

        while (fallback.size() < requested) {
            fallback.add(generators.get(index % generators.size()).get());
            index++;
        }

        return fallback;
    }

    private QuizRequestPayload.QuizQuestionRequest createEquationFallbackQuestion(String topic) {
        return QuizRequestPayload.QuizQuestionRequest.builder()
                .text(String.format(
                        "Giải phương trình $2x + 5 = 15$ thuộc chủ đề \"%s\". Giá trị của $x$ là bao nhiêu?",
                        topic))
                .type(MULTIPLE_CHOICE)
                .points(10)
                .explanation("Giải phương trình: $2x + 5 = 15 \\Rightarrow 2x = 10 \\Rightarrow x = 5$.")
                .options(List.of(
                        QuizRequestPayload.QuizOptionRequest.builder().text("$x = 3$").correct(false).build(),
                        QuizRequestPayload.QuizOptionRequest.builder().text("$x = 4$").correct(false).build(),
                        QuizRequestPayload.QuizOptionRequest.builder().text("$x = 5$").correct(true).build(),
                        QuizRequestPayload.QuizOptionRequest.builder().text("$x = 6$").correct(false).build()))
                .build();
    }

    private QuizRequestPayload.QuizQuestionRequest createExpressionFallbackQuestion(String topic) {
        return QuizRequestPayload.QuizQuestionRequest.builder()
                .text(String.format(
                        "Trong chủ đề \"%s\", hãy tính giá trị của biểu thức $3^2 + 4^2$.", topic))
                .type(MULTIPLE_CHOICE)
                .points(10)
                .explanation(
                        "Tính: $3^2 + 4^2 = 9 + 16 = 25$. Đây là câu hỏi dự phòng khi Gemini quá tải.")
                .options(List.of(
                        QuizRequestPayload.QuizOptionRequest.builder().text("$21$").correct(false).build(),
                        QuizRequestPayload.QuizOptionRequest.builder().text("$24$").correct(false).build(),
                        QuizRequestPayload.QuizOptionRequest.builder().text("$25$").correct(true).build(),
                        QuizRequestPayload.QuizOptionRequest.builder().text("$26$").correct(false).build()))
                .build();
    }

    private QuizRequestPayload.QuizQuestionRequest createGeometryFallbackQuestion(String topic) {
        return QuizRequestPayload.QuizQuestionRequest.builder()
                .text(String.format(
                        "Một tam giác vuông trong chủ đề \"%s\" có hai cạnh góc vuông lần lượt là $6$ và $8$. "
                                + "Độ dài cạnh huyền bằng bao nhiêu?",
                        topic))
                .type(MULTIPLE_CHOICE)
                .points(12)
                .explanation(
                        "Áp dụng định lý Pitago: $c = \\sqrt{6^2 + 8^2} = \\sqrt{36 + 64} = 10$. Câu hỏi dự phòng khi Gemini quá tải.")
                .options(List.of(
                        QuizRequestPayload.QuizOptionRequest.builder().text("$8$").correct(false).build(),
                        QuizRequestPayload.QuizOptionRequest.builder().text("$9$").correct(false).build(),
                        QuizRequestPayload.QuizOptionRequest.builder().text("$10$").correct(true).build(),
                        QuizRequestPayload.QuizOptionRequest.builder().text("$12$").correct(false).build()))
                .build();
    }

    private boolean validateQuestion(
            QuizRequestPayload.QuizQuestionRequest question, List<String> existingQuestions) {
        // Validate math formulas
        if (!validateMathFormulas(question.getText())) {
            log.warn("Invalid math formulas in question: {}", question.getText());
        }

        // Validate options
        if (question.getOptions() == null || question.getOptions().size() != 4) {
            log.warn("Question must have exactly 4 options");
            return false;
        }

        long correctCount = question.getOptions().stream()
                .filter(opt -> Boolean.TRUE.equals(opt.getCorrect()))
                .count();
        if (correctCount != 1) {
            log.warn("Question must have exactly 1 correct answer, found: {}", correctCount);
            // Try to fix
            boolean foundCorrect = false;
            for (QuizRequestPayload.QuizOptionRequest opt : question.getOptions()) {
                if (Boolean.TRUE.equals(opt.getCorrect()) && !foundCorrect) {
                    foundCorrect = true;
                } else if (Boolean.TRUE.equals(opt.getCorrect())) {
                    opt.setCorrect(false);
                }
            }
            if (!foundCorrect) {
                question.getOptions().get(0).setCorrect(true);
            }
        }

        // Check duplication
        if (isDuplicate(question.getText(), existingQuestions)) {
            log.warn("Question is duplicate: {}", question.getText());
            return false;
        }

        return true;
    }

    private boolean validateMathFormulas(String text) {
        // Check for balanced $ signs
        long dollarCount = text.chars().filter(ch -> ch == '$').count();
        if (dollarCount % 2 != 0) {
            return false;
        }

        // Check for balanced braces
        int braceCount = 0;
        for (char c : text.toCharArray()) {
            if (c == '{') braceCount++;
            if (c == '}') braceCount--;
            if (braceCount < 0) return false;
        }

        return braceCount == 0;
    }

    private boolean isDuplicate(String newQuestion, List<String> existingQuestions) {
        Set<String> newKeywords = extractKeywords(newQuestion);

        for (String existingQ : existingQuestions) {
            Set<String> existingKeywords = extractKeywords(existingQ);

            // Calculate Jaccard similarity
            Set<String> intersection = new HashSet<>(newKeywords);
            intersection.retainAll(existingKeywords);

            Set<String> union = new HashSet<>(newKeywords);
            union.addAll(existingKeywords);

            double similarity = union.isEmpty() ? 0 : (double) intersection.size() / union.size();

            // Increase threshold to reduce false positives (was 0.7, now 0.85)
            // Allow similar questions if they test different concepts
            if (similarity > 0.85) {
                log.debug("Question marked as duplicate (similarity: {})", similarity);
                return true;
            }
        }

        return false;
    }

    private Set<String> extractKeywords(String text) {
        // Remove LaTeX markup
        String cleaned = text.replaceAll("\\$\\$?[^$]+\\$\\$?", "MATH")
                .toLowerCase()
                .replaceAll("[^\\w\\s]", " ");

        Set<String> keywords = new HashSet<>();
        for (String word : cleaned.split("\\s+")) {
            if (word.length() > 3) {
                keywords.add(word);
            }
        }
        return keywords;
    }

    private static class QuizGenerationException extends RuntimeException {
        QuizGenerationException(String message) {
            super(message);
        }

        QuizGenerationException(String message, Throwable cause) {
            super(message, cause);
        }
    }

    private static class GeminiApiException extends RuntimeException {
        private final boolean retryable;

        GeminiApiException(String message, boolean retryable) {
            super(message);
            this.retryable = retryable;
        }

        GeminiApiException(String message, boolean retryable, Throwable cause) {
            super(message, cause);
            this.retryable = retryable;
        }

        boolean isRetryable() {
            return retryable;
        }
    }
}
