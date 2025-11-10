package com.mss301.chatbotservice.controller;

import com.mss301.chatbotservice.model.dtos.request.ChatRequest;
import com.mss301.chatbotservice.model.dtos.request.ChatSessionRequest;
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
    public ResponseEntity<ApiResponse<Long>> createSession(@RequestBody ChatSessionRequest chatSessionRequest) {
        Long sessionId = chatSessionService.createSession(chatSessionRequest);
        return ResponseEntity.ok(ApiResponse.<Long>builder()
                .code(200)
                .message("Session created successfully")
                .result(sessionId)
                .build());
    }

    @PostMapping("/send/{sessionId}")
    public ResponseEntity<ApiResponse<ChatResponse>> sendMessage(@PathVariable("sessionId") Long sessionId, @RequestBody ChatRequest request) {
        ChatResponse response = chatSessionService.sendMessage(request, sessionId);
        return ResponseEntity.ok(ApiResponse.<ChatResponse>builder()
                .code(200)
                .message("Message sent successfully")
                .result(response)
                .build());
    }

    @GetMapping("/sessions/{sessionId}/messages")
    public ResponseEntity<ApiResponse<List<ChatResponse>>> getSessionMessages(@PathVariable Long sessionId) {
        List<ChatResponse> messages = chatSessionService.getSessionMessages(sessionId);
        return ResponseEntity.ok(ApiResponse.<List<ChatResponse>>builder()
                .code(200)
                .message("Messages retrieved successfully")
                .result(messages)
                .build());
    }

    @DeleteMapping("/sessions/{sessionId}")
    public ResponseEntity<ApiResponse<Void>> deleteSession(@PathVariable Long sessionId) {
        chatSessionService.deleteSession(sessionId);
        return ResponseEntity.ok(ApiResponse.<Void>builder()
                .code(200)
                .message("Session deleted successfully")
                .build());
    }
}