package com.mss301.chatbotservice.service;

import com.mss301.chatbotservice.chatbot.model.AiResponse;
import com.mss301.chatbotservice.chatbot.model.Chatbot;
import com.mss301.chatbotservice.chatbot.model.Message;
import com.mss301.chatbotservice.enums.ChatRole;
import com.mss301.chatbotservice.model.ChatMessage;
import com.mss301.chatbotservice.model.ChatSession;
import com.mss301.chatbotservice.model.ExpertProfile;
import com.mss301.chatbotservice.model.dtos.request.ChatSessionRequest;
import com.mss301.chatbotservice.model.dtos.request.ChatbotRequest;
import com.mss301.chatbotservice.model.dtos.response.ChatResponse;
import com.mss301.chatbotservice.model.dtos.response.ChatSessionReponse;
import com.mss301.chatbotservice.repository.ChatMessageRepository;
import com.mss301.chatbotservice.repository.ChatSessionRepository;
import com.mss301.chatbotservice.repository.ExpertProfileRepository;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.*;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.client.RestTemplate;

import java.time.LocalDateTime;
import java.util.List;
import java.util.stream.Collectors;

@Slf4j
@Service
public class ChatSessionServiceImp implements ChatSessionService {

    @Value("${chat.api}")
    private String API;

    @Value("${chat.apiKey}")
    private String TOKEN;

    @Autowired
    private ChatSessionRepository chatSessionRepository;

    @Autowired
    private ChatMessageRepository chatMessageRepository;

    @Autowired
    private ExpertProfileRepository expertProfileRepository;

//    @Autowired
//    private RagService ragService;

    @Autowired
    private RestTemplate restTemplate;

    @Override
    @Transactional
    public Long createSession(ChatSessionRequest chatSessionRequest) {
        ExpertProfile expertProfile = expertProfileRepository.findById(chatSessionRequest.getExpertProfileId())
                .orElseThrow(() -> new RuntimeException("Expert profile not found"));

        ChatSession session = new ChatSession();
        session.setUserId(chatSessionRequest.getUserId());
        session.setExpertProfileId(expertProfile);
        session.setTitle("New Chat Session");
        session.setStatus(true);

        ChatSession savedSession = chatSessionRepository.save(session);
        return savedSession.getId();
    }

    @Override
    @Transactional
    public ChatResponse sendMessage(ChatbotRequest chatbotRequest, Long sessionId) {
        ChatSession session = chatSessionRepository.findById(sessionId)
                .orElseThrow(() -> new RuntimeException("Session not found"));

        // Lưu tin nhắn của user
        ChatMessage userMessage = ChatMessage.builder()
                .role(ChatRole.USER)
                .content(chatbotRequest.getMessages())
                .createdAt(LocalDateTime.now())
                .tokensUsed(0L)
                .build();

        ChatMessage savedUserMessage = chatMessageRepository.save(userMessage);

        session.getChatMessages().add(savedUserMessage);
        chatSessionRepository.save(session);

        // Tạo response từ AI (giả lập)
        Chatbot sendMessage = new Chatbot();
        sendMessage.setModel(session.getExpertProfileId().getPromptConfig());
        Message message = Message.builder()
                .role("user")
                .content(generatePrompt(chatbotRequest.getMessages()))
                .build();
        sendMessage.addMessage(message);

        try {
            HttpHeaders headers = new HttpHeaders();
            headers.setContentType(MediaType.APPLICATION_JSON);
            headers.setBearerAuth(TOKEN);

            HttpEntity<Chatbot> request = new HttpEntity<>(sendMessage, headers);

            ResponseEntity<AiResponse> response =
                    restTemplate.exchange(API, HttpMethod.POST, request, AiResponse.class);

            System.out.println(response);

            if (response.getStatusCode() == HttpStatus.OK && response.getBody() != null) {
                AiResponse aiResponse = response.getBody();
                System.out.println(aiResponse);

                ChatMessage aiMessage = ChatMessage.builder()
                        .tokensUsed(aiResponse.getUsage().getTotal_tokens())
                        .createdAt(LocalDateTime.now())
                        .content(aiResponse.getChoices().getFirst().getMessage().getContent())
                        .role(ChatRole.ASSISTANT)
                        .build();

                ChatMessage savedAiMessage = chatMessageRepository.save(aiMessage);
                session.getChatMessages().add(savedAiMessage);
                chatSessionRepository.save(session);

                log.info("AI response saved with ID: {}", savedAiMessage.getId());

                return convertChatMessageToResponse(savedAiMessage);
            } else {
                ChatMessage requestError = ChatMessage.builder()
                        .tokensUsed(0L)
                        .createdAt(LocalDateTime.now())
                        .content("The AI failed to return a response. Please try a different model.")
                        .role(ChatRole.ASSISTANT)
                        .build();
                ChatMessage savedErrorMessage = chatMessageRepository.save(requestError);
                session.getChatMessages().add(savedErrorMessage);
                chatSessionRepository.save(session);

                return convertChatMessageToResponse(savedErrorMessage);
            }

        } catch (Exception e) {
            log.error("Error calling Service: {}", e.getMessage(), e);

            // Tạo error response
            ChatMessage errorMessage = ChatMessage.builder()
                    .tokensUsed(0L)
                    .createdAt(LocalDateTime.now())
                    .content("Sorry, I'm unable to process your request at the moment.")
                    .role(ChatRole.ASSISTANT)
                    .build();
            ChatMessage savedErrorMessage = chatMessageRepository.save(errorMessage);
            session.getChatMessages().add(savedErrorMessage);
            chatSessionRepository.save(session);

            return convertChatMessageToResponse(savedErrorMessage);
        }
    }


    @Override
    public List<ChatResponse> getSessionMessages(Long sessionId) {
        ChatSession session = chatSessionRepository.findById(sessionId)
                .orElseThrow(() -> new RuntimeException("Session not found"));

        List<ChatMessage> messages = session.getChatMessages();
        return messages.stream()
                .map(this::convertChatMessageToResponse)
                .collect(Collectors.toList());
    }

    @Override
    public List<ChatSessionReponse> getByUserId(Long userId) {
        return chatSessionRepository.findByUserId(userId)
                .stream()
                .map(session -> convertToSessionResponse(session))
                .collect(Collectors.toList());
    }

    @Override
    @Transactional
    public void deleteSession(Long sessionId) {
        ChatSession session = chatSessionRepository.findById(sessionId)
                .orElseThrow(() -> new RuntimeException("Session not found"));
        session.setStatus(false);
        chatSessionRepository.save(session);
    }

    private String generatePrompt(String userMessage) {
        return """
                BẠN LÀ TRỢ LÝ TOÁN HỌC CHUYÊN NGHIỆP
                
                VAI TRÒ:
                - Bạn là giáo viên toán học có kinh nghiệm, chuyên giảng dạy và giải đáp thắc mắc về toán học
                - Bạn chỉ trả lời các câu hỏi liên quan đến toán học (đại số, hình học, giải tích, xác suất thống kê, v.v.)
                
                QUY TẮC XỬ LÝ:
                
                1. CÂU HỎI ĐÚNG CHỦ ĐỀ TOÁN HỌC:
                   - Giải thích rõ ràng, chi tiết từng bước
                   - Sử dụng ví dụ minh họa khi cần thiết
                   - Trình bày công thức và lời giải một cách logic
                   - Kiểm tra lại đáp án trước khi đưa ra
                
                2. CÂU HỎI NGOÀI CHỦ ĐỀ:
                   - Trả lời: "Xin lỗi, tôi chỉ có thể hỗ trợ các câu hỏi liên quan đến toán học. Câu hỏi của bạn không thuộc lĩnh vực chuyên môn của tôi. Vui lòng đặt câu hỏi về toán học để tôi có thể giúp bạn."
                
                3. CÂU HỎI KHÓ/KHÔNG RÕ RÀNG:
                   - Nếu không hiểu câu hỏi: "Tôi chưa hiểu rõ câu hỏi của bạn. Bạn có thể diễn đạt lại hoặc cung cấp thêm thông tin không?"
                   - Nếu vượt quá khả năng: "Câu hỏi này khá phức tạp và nằm ngoài phạm vi kiến thức tôi có thể đảm bảo độ chính xác. Tôi khuyên bạn nên tham khảo thêm từ giáo viên hoặc tài liệu chuyên sâu."
                
                4. NGÔN TỪ KHÔNG PHÙ HỢP:
                   - Từ chối trả lời các câu hỏi có ngôn từ thô tục, xúc phạm, phân biệt đối xử
                   - Trả lời: "Tôi không thể phản hồi tin nhắn này do vi phạm quy tắc giao tiếp văn minh. Vui lòng đặt câu hỏi một cách lịch sự và tôn trọng."
                
                5. CÂU HỎI YÊU CẦU LÀM BÀI:
                   - Không làm thay bài tập/bài kiểm tra
                   - Hướng dẫn cách giải và gợi ý tư duy thay vì đưa đáp án trực tiếp
                
                NGUYÊN TẮC QUAN TRỌNG:
                - KHÔNG bịa đặt hoặc đoán mò đáp án
                - KHÔNG trả lời câu hỏi ngoài chủ đề toán học
                - KHÔNG sử dụng hoặc phản hồi ngôn từ không phù hợp
                - Luôn thừa nhận giới hạn kiến thức khi không chắc chắn
                - Trả lời bằng tiếng Việt một cách rõ ràng, dễ hiểu
                
                CÂU HỎI CỦA HỌC SINH:
                """ + userMessage;
    }

    private ChatResponse convertChatMessageToResponse(ChatMessage message) {
        return ChatResponse.builder()
                .messageId(message.getId())
                .role(message.getRole())
                .content(message.getContent())
                .tokensUsed(message.getTokensUsed())
                .createdAt(message.getCreatedAt())
                .build();
    }

    private ChatSessionReponse convertToSessionResponse(ChatSession session) {
        return ChatSessionReponse.builder()
                .id(session.getId())
                .userId(session.getUserId())
                .expertProfileId(session.getExpertProfileId())
                .title(session.getTitle())
                .status(session.getStatus())
                .createTime(session.getCreateTime())
                .updateTime(session.getUpdateTime())
                .build();
    }
}