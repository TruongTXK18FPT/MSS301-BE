package com.mss301.contentservice.controller;

import jakarta.validation.Valid;

import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.mss301.contentservice.dto.request.QuizRequestPayload;
import com.mss301.contentservice.dto.response.QuizResponsePayload;
import com.mss301.contentservice.service.QuizService;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;

@RestController
@RequestMapping("/contents/{id}/quiz")
@RequiredArgsConstructor
@Tag(name = "Quiz Management")
public class QuizController {

    private final QuizService quizService;

    @GetMapping
    @Operation(summary = "Get quiz definition for a content item")
    public ResponseEntity<QuizResponsePayload> get(
            @PathVariable("id") Long contentItemId, Authentication authentication) {
        Long userId = Long.parseLong(authentication.getName());
        return ResponseEntity.ok(quizService.getQuiz(contentItemId, userId));
    }

    @PutMapping
    @Operation(summary = "Create or replace quiz definition for a content item")
    public ResponseEntity<QuizResponsePayload> put(
            @PathVariable("id") Long contentItemId,
            @Valid @RequestBody QuizRequestPayload.QuizRequest request,
            Authentication authentication) {
        Long userId = Long.parseLong(authentication.getName());
        return ResponseEntity.ok(quizService.putQuiz(contentItemId, request, userId));
    }
}
