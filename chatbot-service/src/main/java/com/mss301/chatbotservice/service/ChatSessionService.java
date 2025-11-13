package com.mss301.chatbotservice.service;

import com.mss301.chatbotservice.dtos.request.ChatSessionRequest;
import com.mss301.chatbotservice.dtos.request.ChatbotRequest;
import com.mss301.chatbotservice.dtos.response.ChatResponse;
import com.mss301.chatbotservice.dtos.response.ChatSessionResponse;

import java.util.List;

public interface ChatSessionService {
   Long createSession(ChatSessionRequest chatSessionRequest);
   ChatResponse sendMessage(ChatbotRequest request, Long sessionId);
   List<ChatResponse> getSessionMessages(Long sessionId);
   List<ChatSessionResponse> getByUserId(Long userId);
   void deleteSession(Long sessionId);
}