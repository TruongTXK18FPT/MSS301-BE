package com.mss301.chatbotservice.service;

import com.mss301.chatbotservice.model.dtos.request.ChatSessionRequest;
import com.mss301.chatbotservice.model.dtos.request.ChatbotRequest;
import com.mss301.chatbotservice.model.dtos.response.ChatResponse;
import com.mss301.chatbotservice.model.dtos.response.ChatSessionReponse;

import java.util.List;

public interface ChatSessionService {
   Long createSession(ChatSessionRequest chatSessionRequest);
   ChatResponse sendMessage(ChatbotRequest request, Long sessionId);
   List<ChatResponse> getSessionMessages(Long sessionId);
   List<ChatSessionReponse>getByUserId(Long userId);
   void deleteSession(Long sessionId);
}