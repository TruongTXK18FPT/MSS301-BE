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

            OUTPUT FORMAT (JSON):
            Trả về mảng JSON với cấu trúc:
            [
              {
                "text": "...",
                "type": "MULTIPLE_CHOICE",
                "points": 10,
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
            - Chỉ trả về JSON array, không có text nào khác
            """;

    @Override
    public List<QuizRequestPayload.QuizQuestionRequest> generateQuizQuestions(
            GenerateQuizRequest request, List<String> existingQuestions) {

        String userPrompt = buildUserPrompt(request, existingQuestions);

        try {
            String responseText = callGeminiApi(SYSTEM_PROMPT, userPrompt);
            List<QuizRequestPayload.QuizQuestionRequest> questions = parseQuestions(responseText);

            // Validate and filter
            List<QuizRequestPayload.QuizQuestionRequest> validQuestions = new ArrayList<>();
            for (QuizRequestPayload.QuizQuestionRequest question : questions) {
                if (validateQuestion(question, existingQuestions)) {
                    validQuestions.add(question);
                }
            }

            if (validQuestions.isEmpty()) {
                throw new RuntimeException("Không tạo được câu hỏi hợp lệ nào. Vui lòng thử lại.");
            }

            return validQuestions;
        } catch (Exception e) {
            log.error("Error generating quiz questions", e);
            throw new RuntimeException("Lỗi tạo câu hỏi: " + e.getMessage());
        }
    }

    private String buildUserPrompt(GenerateQuizRequest request, List<String> existingQuestions) {
        StringBuilder prompt = new StringBuilder();
        prompt.append(String.format(
                "Tạo %d câu hỏi trắc nghiệm về chủ đề \"%s\" cho học sinh lớp %s.\n\n",
                request.getNumQuestions(), request.getTopic(), request.getGrade()));

        if (!existingQuestions.isEmpty()) {
            prompt.append("CÁC CÂU ĐÃ TỒN TẠI (KHÔNG ĐƯỢC TRÙNG LẶP):\n");
            for (int i = 0; i < existingQuestions.size(); i++) {
                String q = existingQuestions.get(i);
                prompt.append(String.format("%d. %s\n", i + 1, q.substring(0, Math.min(100, q.length()))));
            }
            prompt.append("\n");
        }

        prompt.append("YÊU CẦU:\n");
        prompt.append(String.format("- Tạo %d câu hỏi HOÀN TOÀN MỚI, không trùng với các câu trên\n",
                request.getNumQuestions()));
        prompt.append("- Đa dạng về dạng toán và phương pháp giải\n");
        prompt.append("- Sử dụng LaTeX cho công thức toán học\n");
        prompt.append("- Mỗi câu có 4 đáp án, chỉ 1 đáp án đúng\n");
        prompt.append(String.format("- Độ khó phù hợp với lớp %s\n", request.getGrade()));
        prompt.append("- Trả về JSON array như format đã chỉ định, KHÔNG CÓ TEXT NÀO KHÁC\n");

        return prompt.toString();
    }

    private String callGeminiApi(String systemPrompt, String userPrompt)
            throws IOException, InterruptedException {
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
        systemInstruction.add("parts", systemParts);
        requestBody.add("systemInstruction", systemInstruction);

        // Contents (user message)
        JsonArray contents = new JsonArray();
        JsonObject content = new JsonObject();
        content.addProperty("role", "user");
        JsonArray parts = new JsonArray();
        JsonObject part = new JsonObject();
        part.addProperty("text", userPrompt);
        parts.add(part);
        content.add("parts", parts);
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

        HttpResponse<String> response = httpClient.send(httpRequest, HttpResponse.BodyHandlers.ofString());

        if (response.statusCode() != 200) {
            log.error("Gemini API error: {} - {}", response.statusCode(), response.body());
            throw new RuntimeException("Gemini API error: " + response.statusCode());
        }

        JsonObject responseJson = JsonParser.parseString(response.body()).getAsJsonObject();
        JsonArray candidates = responseJson.getAsJsonArray("candidates");
        if (candidates == null || candidates.isEmpty()) {
            throw new RuntimeException("No response from Gemini API");
        }

        JsonObject candidate = candidates.get(0).getAsJsonObject();
        JsonObject contentObj = candidate.getAsJsonObject("content");
        JsonArray responseParts = contentObj.getAsJsonArray("parts");
        String text = responseParts.get(0).getAsJsonObject().get("text").getAsString();

        return text;
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
                        .options(options)
                        .build());
            }

            return questions;
        } catch (Exception e) {
            log.error("Failed to parse AI response: {}", jsonText, e);
            throw new RuntimeException("AI response không phải JSON hợp lệ: " + e.getMessage());
        }
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

        long correctCount =
                question.getOptions().stream().filter(QuizRequestPayload.QuizOptionRequest::getCorrect).count();
        if (correctCount != 1) {
            log.warn("Question must have exactly 1 correct answer, found: {}", correctCount);
            // Try to fix
            boolean foundCorrect = false;
            for (QuizRequestPayload.QuizOptionRequest opt : question.getOptions()) {
                if (opt.getCorrect() && !foundCorrect) {
                    foundCorrect = true;
                } else if (opt.getCorrect()) {
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

            if (similarity > 0.7) {
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
}
