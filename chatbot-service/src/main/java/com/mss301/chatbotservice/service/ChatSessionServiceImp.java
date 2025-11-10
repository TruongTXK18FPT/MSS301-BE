package com.mss301.chatbotservice.service;

import com.mss301.chatbotservice.enums.ChatRole;
import com.mss301.chatbotservice.enums.LLMProvider;
import com.mss301.chatbotservice.enums.ResponseMode;
import com.mss301.chatbotservice.model.ChatMessage;
import com.mss301.chatbotservice.model.ChatSession;
import com.mss301.chatbotservice.model.ExpertProfile;
import com.mss301.chatbotservice.model.dtos.request.ChatRequest;
import com.mss301.chatbotservice.model.dtos.request.ChatSessionRequest;
import com.mss301.chatbotservice.model.dtos.request.RagRequest;
import com.mss301.chatbotservice.model.dtos.response.ChatResponse;
import com.mss301.chatbotservice.model.dtos.response.ChatSessionReponse;
import com.mss301.chatbotservice.model.dtos.response.RagResponse;
import com.mss301.chatbotservice.repository.ChatMessageRepository;
import com.mss301.chatbotservice.repository.ChatSessionRepository;
import com.mss301.chatbotservice.repository.ExpertProfileRepository;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;
import java.util.stream.Collectors;

@Slf4j
@Service
public class ChatSessionServiceImp implements ChatSessionService {

    @Autowired
    private ChatSessionRepository chatSessionRepository;

    @Autowired
    private ChatMessageRepository chatMessageRepository;

    @Autowired
    private ExpertProfileRepository expertProfileRepository;

    @Autowired
    private RagService ragService;

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
    public ChatResponse sendMessage(ChatRequest request, Long sessionId) {
        ChatSession session = chatSessionRepository.findById(sessionId)
                .orElseThrow(() -> new RuntimeException("Session not found"));

        // Lưu tin nhắn của user
        ChatMessage userMessage = ChatMessage.builder()
                .role(ChatRole.USER)
                .content(request.getContent())
                .createdAt(LocalDateTime.now())
                .tokensUsed(0L)
                .build();

        ChatMessage savedUserMessage = chatMessageRepository.save(userMessage);

        session.getChatMessages().add(savedUserMessage);
        chatSessionRepository.save(session);

        // Tạo response từ AI (giả lập)
        RagRequest ragRequest = RagRequest.builder()
                .queryText(request.getContent())
                .llmProvider(request.getProvider() != null ? request.getProvider() : LLMProvider.MISTRAL)
                .build();

        try {
            // 3. Gọi RAG Service để lấy AI response
            log.info("Calling RAG Service with query: {}", userMessage.getContent());
            RagResponse ragResponse = ragService.processQuery(ragRequest);

            // 4. Tạo và lưu tin nhắn AI response
            ChatMessage aiMessage = ChatMessage.builder()
                    .tokensUsed(Long.valueOf(ragResponse.getChunksUsed()))
                    .createdAt(LocalDateTime.now())
                    .content(ragResponse.getQueryText())
                    .role(ChatRole.ASSISTANT)
                    .build();

            ChatMessage savedAiMessage = chatMessageRepository.save(aiMessage);
            session.getChatMessages().add(savedAiMessage);
            chatSessionRepository.save(session);

            log.info("AI response saved with ID: {}", savedAiMessage.getId());

            return convertToResponse(savedAiMessage);

        } catch (Exception e) {
            log.error("Error calling RAG Service: {}", e.getMessage(), e);

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

            return convertToResponse(savedErrorMessage);
        }
    }

    @Override
    public List<ChatResponse> getSessionMessages(Long sessionId) {
        ChatSession session = chatSessionRepository.findById(sessionId)
                .orElseThrow(() -> new RuntimeException("Session not found"));

        List<ChatMessage> messages = session.getChatMessages();
        return messages.stream()
                .map(this::convertToResponse)
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

    private ChatResponse convertToResponse(ChatMessage message) {
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