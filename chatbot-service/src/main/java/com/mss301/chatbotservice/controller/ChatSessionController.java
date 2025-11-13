package com.mss301.chatbotservice.controller;

import com.mss301.chatbotservice.model.dtos.request.ChatSessionRequest;
import com.mss301.chatbotservice.model.dtos.request.ChatbotRequest;
import com.mss301.chatbotservice.model.dtos.response.ApiResponse;
import com.mss301.chatbotservice.model.dtos.response.ChatResponse;
import com.mss301.chatbotservice.service.ChatSessionService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/chatbot")
@CrossOrigin
public class ChatSessionController {

    @Autowired
    private ChatSessionService chatSessionService;

    @PostMapping("/sessions")
    public ResponseEntity<ApiResponse<Long>> createSession(
            @RequestHeader("Authorization") String token,
            @RequestBody ChatSessionRequest chatSessionRequest) {
        Long sessionId = chatSessionService.createSession(chatSessionRequest);
        return ResponseEntity.ok(ApiResponse.<Long>builder()
                .code(200)
                .message("Session created successfully")
                .result(sessionId)
                .build());
    }

    @PostMapping("/sessions/{sessionId}")
    public ResponseEntity<ApiResponse<ChatResponse>> sendMessage(
            @RequestHeader("Authorization") String token,
            @PathVariable("sessionId") Long sessionId, @RequestBody ChatbotRequest request) {
        ChatResponse response = chatSessionService.sendMessage(request, sessionId);
        return ResponseEntity.ok(ApiResponse.<ChatResponse>builder()
                .code(200)
                .message("Message sent successfully")
                .result(response)
                .build());
    }

    @GetMapping("/sessions/{sessionId}")
    public ResponseEntity<ApiResponse<List<ChatResponse>>> getSessionMessages(
            @RequestHeader("Authorization") String token,
            @PathVariable Long sessionId) {
        List<ChatResponse> messages = chatSessionService.getSessionMessages(sessionId);
        return ResponseEntity.ok(ApiResponse.<List<ChatResponse>>builder()
                .code(200)
                .message("Messages retrieved successfully")
                .result(messages)
                .build());
    }

    @DeleteMapping("/sessions/{sessionId}")
    public ResponseEntity<ApiResponse<Void>> deleteSession(
            @RequestHeader("Authorization") String token,
            @PathVariable Long sessionId) {
        chatSessionService.deleteSession(sessionId);
        return ResponseEntity.ok(ApiResponse.<Void>builder()
                .code(200)
                .message("Session deleted successfully")
                .build());
    }
}