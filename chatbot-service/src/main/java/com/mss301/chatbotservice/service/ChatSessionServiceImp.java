package com.mss301.chatbotservice.service;

import com.mss301.chatbotservice.dtos.request.ChatSessionRequest;
import com.mss301.chatbotservice.dtos.request.ChatbotRequest;
import com.mss301.chatbotservice.dtos.response.ChatResponse;
import com.mss301.chatbotservice.dtos.response.ChatSessionResponse;
import com.mss301.chatbotservice.enums.ChatRole;
import com.mss301.chatbotservice.exception.ChatbotServiceException;
import com.mss301.chatbotservice.model.ChatMessage;
import com.mss301.chatbotservice.model.ChatSession;
import com.mss301.chatbotservice.model.ExpertProfile;
import com.mss301.chatbotservice.repository.ChatMessageRepository;
import com.mss301.chatbotservice.repository.ChatSessionRepository;
import com.mss301.chatbotservice.repository.ExpertProfileRepository;
import com.mss301.chatbotservice.dtos.request.RagRequest;
import com.mss301.chatbotservice.dtos.request.TtsRequest;
import com.mss301.chatbotservice.dtos.response.RagResponse;
import com.mss301.chatbotservice.dtos.response.TtsResponse;
import com.mss301.chatbotservice.enums.LLMProvider;
import com.mss301.chatbotservice.service.llm.GeminiService;
import com.mss301.chatbotservice.service.llm.MistralService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

@Slf4j
@Service
@RequiredArgsConstructor
public class ChatSessionServiceImp implements ChatSessionService {

    private static final String SESSION_NOT_FOUND_MESSAGE = "Session not found";

    private final ChatSessionRepository chatSessionRepository;
    private final ChatMessageRepository chatMessageRepository;
    private final ExpertProfileRepository expertProfileRepository;
    private final GeminiService geminiService;
    private final MistralService mistralService;
    private final RagService ragService;
    private final TtsService ttsService;

    @Override
    @Transactional
    public Long createSession(ChatSessionRequest chatSessionRequest) {
        ExpertProfile expertProfile = null;
        
        // Log all available expert profiles for debugging
        List<ExpertProfile> allProfiles = expertProfileRepository.findAll();
        log.info("Available expert profiles in database: {}", 
                allProfiles.stream()
                        .map(p -> String.format("ID=%d, Code=%s, Name=%s", p.getId(), p.getCode(), p.getName()))
                        .collect(java.util.stream.Collectors.joining(", ")));
        
        // Try to find by code first (more reliable)
        if (chatSessionRequest.getExpertProfileCode() != null && !chatSessionRequest.getExpertProfileCode().isBlank()) {
            expertProfile = expertProfileRepository.findByCode(chatSessionRequest.getExpertProfileCode())
                    .orElse(null);
            if (expertProfile == null) {
                log.warn("Expert profile not found by code: {}. Available codes: {}", 
                        chatSessionRequest.getExpertProfileCode(),
                        allProfiles.stream().map(ExpertProfile::getCode).collect(java.util.stream.Collectors.joining(", ")));
            }
        }
        
        // Fallback to ID if code not found or not provided
        if (expertProfile == null && chatSessionRequest.getExpertProfileId() != null) {
            expertProfile = expertProfileRepository.findById(chatSessionRequest.getExpertProfileId())
                    .orElse(null);
            if (expertProfile == null) {
                log.warn("Expert profile not found by ID: {}. Available IDs: {}", 
                        chatSessionRequest.getExpertProfileId(),
                        allProfiles.stream().map(p -> p.getId().toString()).collect(java.util.stream.Collectors.joining(", ")));
            }
        }
        
        if (expertProfile == null) {
            log.error("Expert profile not found. Request: expertProfileId={}, expertProfileCode={}", 
                    chatSessionRequest.getExpertProfileId(), chatSessionRequest.getExpertProfileCode());
            log.error("Available expert profiles: {}", 
                    allProfiles.stream()
                            .map(p -> String.format("ID=%d, Code=%s", p.getId(), p.getCode()))
                            .collect(java.util.stream.Collectors.joining(", ")));
            throw new ChatbotServiceException(
                    String.format("Expert profile not found. ID: %s, Code: %s. Available profiles: %s", 
                            chatSessionRequest.getExpertProfileId(), 
                            chatSessionRequest.getExpertProfileCode(),
                            allProfiles.stream()
                                    .map(p -> String.format("ID=%d, Code=%s", p.getId(), p.getCode()))
                                    .collect(java.util.stream.Collectors.joining(", "))));
        }
        
        if (!Boolean.TRUE.equals(expertProfile.getActive())) {
            log.warn("Attempted to create session with inactive expert profile: {}", expertProfile.getCode());
            throw new ChatbotServiceException("Expert profile is not active");
        }

        ChatSession session = new ChatSession();
        session.setUserId(chatSessionRequest.getUserId());
        session.setExpertProfileId(expertProfile);
        session.setTitle("New Chat Session");
        session.setStatus(true);

        ChatSession savedSession = chatSessionRepository.save(session);
        log.info("Created new chat session with ID: {} for user: {} with expert profile: {} (ID: {})", 
                savedSession.getId(), chatSessionRequest.getUserId(), expertProfile.getCode(), expertProfile.getId());
        return savedSession.getId();
    }

    @Override
    @Transactional
    public ChatResponse sendMessage(ChatbotRequest chatbotRequest, Long sessionId) {
        ChatSession session = chatSessionRepository.findById(sessionId)
                .orElseThrow(() -> new ChatbotServiceException(SESSION_NOT_FOUND_MESSAGE));

        ExpertProfile expertProfile = session.getExpertProfileId();
        if (expertProfile == null) {
            throw new ChatbotServiceException("Expert profile not found in session");
        }

        // Lưu tin nhắn của user
        String userInput = chatbotRequest.getMessages() != null ? chatbotRequest.getMessages().trim() : "";
        if (userInput.isEmpty()) {
            throw new ChatbotServiceException("User message must not be empty");
        }

        ChatMessage userMessage = ChatMessage.builder()
                .role(ChatRole.USER)
                .content(userInput)
                .createdAt(LocalDateTime.now())
                .tokensUsed(0L)
                .build();

        ChatMessage savedUserMessage = chatMessageRepository.save(userMessage);
        if (session.getChatMessages() == null) {
            session.setChatMessages(new java.util.ArrayList<>());
        }
        session.getChatMessages().add(savedUserMessage);
        chatSessionRepository.save(session);

        try {
            String gradeLevel = (chatbotRequest.getGradeLevel() != null && !chatbotRequest.getGradeLevel().isBlank())
                    ? chatbotRequest.getGradeLevel().trim()
                    : "Chưa xác định";

            String aiResponse;
            long tokensUsed;
            List<ChatResponse.Source> sources = new ArrayList<>();
            String audioUrl = null;

            // Kiểm tra nếu expert profile sử dụng RAG
            if (Boolean.TRUE.equals(expertProfile.getUseRag())) {
                log.info("Using RAG service for expert profile: {}", expertProfile.getName());
                
                // Chuyển đổi LLMProvider từ chatbot-service sang rag-service
                com.mss301.ragservice.enums.LLMProvider ragLlmProvider = convertToRagLLMProvider(expertProfile.getLlmProvider());
                
                // Xác định ResponseMode
                com.mss301.ragservice.enums.ResponseMode responseMode = Boolean.TRUE.equals(chatbotRequest.getUseVoiceChat()) 
                    ? com.mss301.ragservice.enums.ResponseMode.VOICECHAT 
                    : com.mss301.ragservice.enums.ResponseMode.CHAT;
                
                // Tạo RAG request
                // Ưu tiên fileStoreName nếu có (cho Google File Search)
                // Nếu không có fileStoreName, sử dụng documentId (cho RAG truyền thống)
                String fileStoreName = chatbotRequest.getFileStoreName();
                boolean hasFileStore = (fileStoreName != null && !fileStoreName.isBlank());
                
                // Log chi tiết để debug
                log.info("=== RAG Request Debug ===");
                log.info("ChatbotRequest - fileStoreName: '{}', documentId: '{}'", 
                    fileStoreName, chatbotRequest.getDocumentId());
                log.info("hasFileStore: {}", hasFileStore);
                
                // Luôn giữ documentId để fallback nếu fileStoreName không tồn tại
                String documentId = chatbotRequest.getDocumentId() != null ? chatbotRequest.getDocumentId() : "";
                
                // Nếu có fileStoreName, không cần useDocuments vì file-search tự xử lý
                boolean useDocuments = hasFileStore ? false : true;
                
                log.info("Creating RAG Request - hasFileStore: {}, fileStoreName: '{}', documentId: '{}', useDocuments: {}", 
                    hasFileStore, fileStoreName, documentId, useDocuments);
                
                RagRequest ragRequest = RagRequest.builder()
                        .documentId(documentId)
                        .chapterId(chatbotRequest.getChapterId() != null ? chatbotRequest.getChapterId() : "")
                        .lessonId(chatbotRequest.getLessonId() != null ? chatbotRequest.getLessonId() : "")
                        .fileStoreName(fileStoreName)
                        .queryText(userInput)
                        .mode(responseMode)
                        .llmProvider(ragLlmProvider)
                        .useSemantic(true)
                        .useDocuments(useDocuments) // Chỉ dùng retrieval service nếu không có fileStoreName
                        .topK(7)
                        .build();
                
                log.info("RAG Request created - fileStoreName: {}, documentId: {}, chapterId: {}, lessonId: {}, useDocuments: {}", 
                    ragRequest.getFileStoreName(), ragRequest.getDocumentId(), 
                    ragRequest.getChapterId(), ragRequest.getLessonId(), ragRequest.getUseDocuments());
                
                // Gọi RAG service
                RagResponse ragResponse = ragService.processQuery(ragRequest);
                
                // Parse response từ RAG
                if (ragResponse.getContent() instanceof Map) {
                    @SuppressWarnings("unchecked")
                    Map<String, Object> contentMap = (Map<String, Object>) ragResponse.getContent();
                    aiResponse = (String) contentMap.get("answer");
                    
                    // Extract sources
                    @SuppressWarnings("unchecked")
                    List<Map<String, Object>> sourcesList = (List<Map<String, Object>>) contentMap.get("sources");
                    if (sourcesList != null) {
                        for (Map<String, Object> sourceMap : sourcesList) {
                            ChatResponse.Source source = ChatResponse.Source.builder()
                                    .content((String) sourceMap.get("content"))
                                    .score(((Number) sourceMap.getOrDefault("score", 0.0)).doubleValue())
                                    .documentId((String) sourceMap.get("documentId"))
                                    .chapterId((String) sourceMap.get("chapterId"))
                                    .lessonId((String) sourceMap.get("lessonId"))
                                    .chapterTitle((String) sourceMap.get("chapterTitle"))
                                    .lessonTitle((String) sourceMap.get("lessonTitle"))
                                    .pageNumber(sourceMap.get("pageNumber") != null ? ((Number) sourceMap.get("pageNumber")).longValue() : null)
                                    .build();
                            sources.add(source);
                        }
                    }
                } else {
                    aiResponse = ragResponse.getContent() != null ? ragResponse.getContent().toString() : "Không thể xử lý phản hồi từ RAG service";
                }
                
                tokensUsed = Math.max(1, aiResponse.length() / 4);
                log.info("RAG response received. Answer length: {}, Sources: {}", aiResponse.length(), sources.size());
            } else {
                // Sử dụng LLM trực tiếp như cũ
                String prompt = generatePrompt(expertProfile.getPromptConfig(), userInput, gradeLevel);
                log.info("Using expert profile: {} with LLM provider: {}", expertProfile.getName(), expertProfile.getLlmProvider());

                switch (expertProfile.getLlmProvider()) {
                    case GEMINI -> aiResponse = geminiService.generateResponse(prompt);
                    case MISTRAL -> aiResponse = mistralService.generateResponse(prompt);
                    default -> throw new ChatbotServiceException("Unsupported LLM provider: " + expertProfile.getLlmProvider());
                }

                tokensUsed = Math.max(1, aiResponse.length() / 4);
            }

            // Nếu sử dụng voice chat, gọi TTS service
            if (Boolean.TRUE.equals(chatbotRequest.getUseVoiceChat())) {
                try {
                    com.mss301.ragservice.enums.ResponseMode ttsMode = Boolean.TRUE.equals(expertProfile.getUseRag()) 
                        ? com.mss301.ragservice.enums.ResponseMode.VOICECHAT 
                        : com.mss301.ragservice.enums.ResponseMode.CHAT;
                    
                    TtsRequest ttsRequest = new TtsRequest(aiResponse, ttsMode);
                    TtsResponse ttsResponse = ttsService.speak(ttsRequest);
                    
                    if (ttsResponse.isSuccess() && ttsResponse.getAudioUrl() != null) {
                        audioUrl = ttsResponse.getAudioUrl();
                        log.info("TTS audio generated successfully. URL: {}", audioUrl);
                    } else {
                        log.warn("TTS generation failed: {}", ttsResponse.getMessage());
                    }
                } catch (Exception e) {
                    log.error("Error generating TTS audio: {}", e.getMessage(), e);
                    // Không throw exception, chỉ log lỗi để chat vẫn hoạt động
                }
            }

            // Lưu tin nhắn AI
                ChatMessage aiMessage = ChatMessage.builder()
                    .tokensUsed(tokensUsed)
                        .createdAt(LocalDateTime.now())
                    .content(aiResponse)
                        .role(ChatRole.ASSISTANT)
                        .build();

                ChatMessage savedAiMessage = chatMessageRepository.save(aiMessage);
                session.getChatMessages().add(savedAiMessage);
                chatSessionRepository.save(session);

            log.info("AI response saved with ID: {}, tokens used: {}", savedAiMessage.getId(), tokensUsed);

            // Tạo response với audioUrl và sources
            ChatResponse response = convertChatMessageToResponse(savedAiMessage);
            response.setAudioUrl(audioUrl);
            response.setSources(sources.isEmpty() ? null : sources);
            
            return response;

        } catch (Exception e) {
            log.error("Error calling LLM service: {}", e.getMessage(), e);

            // Tạo error response
            ChatMessage errorMessage = ChatMessage.builder()
                    .tokensUsed(0L)
                    .createdAt(LocalDateTime.now())
                    .content("Xin lỗi, tôi không thể xử lý yêu cầu của bạn lúc này. Vui lòng thử lại sau.")
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
                .orElseThrow(() -> new ChatbotServiceException(SESSION_NOT_FOUND_MESSAGE));

        List<ChatMessage> messages = session.getChatMessages();
        if (messages == null) {
            return List.of();
        }
        return messages.stream()
                .map(this::convertChatMessageToResponse)
                .toList();
    }

    @Override
    public List<ChatSessionResponse> getByUserId(Long userId) {
        return chatSessionRepository.findByUserId(userId)
                .stream()
                .map(this::convertToSessionResponse)
                .toList();
    }

    @Override
    @Transactional
    public void deleteSession(Long sessionId) {
        ChatSession session = chatSessionRepository.findById(sessionId)
                .orElseThrow(() -> new ChatbotServiceException(SESSION_NOT_FOUND_MESSAGE));
        session.setStatus(false);
        chatSessionRepository.save(session);
        log.info("Deleted session with ID: {}", sessionId);
    }

    private String generatePrompt(String promptTemplate, String userMessage, String gradeLevel) {
        // Thay thế placeholder {gradeLevel} trong prompt template
        String prompt = promptTemplate.replace("{gradeLevel}", gradeLevel);
        // Thêm câu hỏi của học sinh vào cuối
        return prompt + userMessage;
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

    private ChatSessionResponse convertToSessionResponse(ChatSession session) {
        return ChatSessionResponse.builder()
                .id(session.getId())
                .userId(session.getUserId())
                .expertProfileId(session.getExpertProfileId() != null ? session.getExpertProfileId().getId() : null)
                .expertProfileName(session.getExpertProfileId() != null ? session.getExpertProfileId().getName() : null)
                .title(session.getTitle())
                .status(session.getStatus())
                .createTime(session.getCreateTime())
                .updateTime(session.getUpdateTime())
                .build();
    }
    
    private com.mss301.ragservice.enums.LLMProvider convertToRagLLMProvider(LLMProvider chatbotProvider) {
        return switch (chatbotProvider) {
            case GEMINI -> com.mss301.ragservice.enums.LLMProvider.GEMINI;
            case MISTRAL -> com.mss301.ragservice.enums.LLMProvider.MISTRAL;
            default -> com.mss301.ragservice.enums.LLMProvider.GEMINI; // Default fallback
        };
    }
}
