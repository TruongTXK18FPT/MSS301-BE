package com.mss301.chatbotservice.controller;

import com.mss301.chatbotservice.model.dtos.request.ChatRequest;
import com.mss301.chatbotservice.model.dtos.response.ApiResponse;
import com.mss301.chatbotservice.model.dtos.response.ChatResponse;
import com.mss301.chatbotservice.service.ChatSessionService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/v1/chat")
@CrossOrigin
public class ChatSessionController {

    @Autowired
    private ChatSessionService chatSessionService;

    @PostMapping("/sessions")
    public ResponseEntity<ApiResponse<Long>> createSession(@RequestParam Long userId, @RequestParam Long expertProfileId) {
        Long sessionId = chatSessionService.createSession(userId, expertProfileId);
        return ResponseEntity.ok(ApiResponse.<Long>builder()
                .code(200)
                .message("Session created successfully")
                .result(sessionId)
                .build());
    }

    @PostMapping("/send")
    public ResponseEntity<ApiResponse<ChatResponse>> sendMessage(@RequestBody ChatRequest request) {
        ChatResponse response = chatSessionService.sendMessage(request);
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