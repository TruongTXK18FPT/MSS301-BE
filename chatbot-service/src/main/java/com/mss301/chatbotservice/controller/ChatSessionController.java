package com.mss301.chatbotservice.controller;

import com.mss301.chatbotservice.dtos.request.ChatSessionRequest;
import com.mss301.chatbotservice.dtos.request.ChatbotRequest;
import com.mss301.chatbotservice.dtos.response.ApiResponse;
import com.mss301.chatbotservice.dtos.response.ChatResponse;
import com.mss301.chatbotservice.dtos.response.ChatSessionResponse;
import com.mss301.chatbotservice.service.ChatSessionService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/chatbot")
@RequiredArgsConstructor
public class ChatSessionController {

    private final ChatSessionService chatSessionService;

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

    @GetMapping("/sessions")
    public ResponseEntity<ApiResponse<List<ChatSessionResponse>>> getSessionsByUserId(
            @RequestHeader("Authorization") String token,
            @RequestParam("userId") Long userId) {
        List<ChatSessionResponse> sessions = chatSessionService.getByUserId(userId);
        return ResponseEntity.ok(ApiResponse.<List<ChatSessionResponse>>builder()
                .code(200)
                .message("Sessions retrieved successfully")
                .result(sessions)
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