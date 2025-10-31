package com.mss301.contentservice.controller;

import java.util.List;

import jakarta.validation.Valid;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

import com.mss301.contentservice.dto.ApiResponse;
import com.mss301.contentservice.dto.request.QuizQuestionRequest;
import com.mss301.contentservice.dto.response.QuizQuestionResponse;
import com.mss301.contentservice.service.QuizQuestionService;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;

@RestController
@RequestMapping("/contents/{contentId}/quiz/questions")
@RequiredArgsConstructor
@Tag(name = "Quiz Question Management")
public class QuizQuestionController {

    private final QuizQuestionService quizQuestionService;

    @GetMapping
    @Operation(summary = "Get all questions for a quiz")
    public ResponseEntity<ApiResponse<List<QuizQuestionResponse>>> getQuestions(
            @PathVariable Long contentId,
            Authentication authentication) {
        Long userId = Long.parseLong(authentication.getName());
        List<QuizQuestionResponse> questions = quizQuestionService.getQuestionsByQuizId(contentId, userId);
        return ResponseEntity.ok(ApiResponse.success(questions));
    }

    @GetMapping("/{questionId}")
    @Operation(summary = "Get a specific question")
    public ResponseEntity<ApiResponse<QuizQuestionResponse>> getQuestion(
            @PathVariable Long contentId,
            @PathVariable Long questionId,
            Authentication authentication) {
        Long userId = Long.parseLong(authentication.getName());
        QuizQuestionResponse question = quizQuestionService.getQuestionById(questionId, userId);
        return ResponseEntity.ok(ApiResponse.success(question));
    }

    @PostMapping
    @Operation(summary = "Add a new question to quiz")
    public ResponseEntity<ApiResponse<QuizQuestionResponse>> addQuestion(
            @PathVariable Long contentId,
            @Valid @RequestBody QuizQuestionRequest request,
            Authentication authentication) {
        Long userId = Long.parseLong(authentication.getName());
        QuizQuestionResponse question = quizQuestionService.addQuestion(contentId, request, userId);
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(ApiResponse.success("Question added successfully", question));
    }

    @PutMapping("/{questionId}")
    @Operation(summary = "Update a question")
    public ResponseEntity<ApiResponse<QuizQuestionResponse>> updateQuestion(
            @PathVariable Long contentId,
            @PathVariable Long questionId,
            @Valid @RequestBody QuizQuestionRequest request,
            Authentication authentication) {
        Long userId = Long.parseLong(authentication.getName());
        QuizQuestionResponse question = quizQuestionService.updateQuestion(questionId, request, userId);
        return ResponseEntity.ok(ApiResponse.success("Question updated successfully", question));
    }

    @DeleteMapping("/{questionId}")
    @Operation(summary = "Delete a question")
    public ResponseEntity<ApiResponse<Void>> deleteQuestion(
            @PathVariable Long contentId,
            @PathVariable Long questionId,
            Authentication authentication) {
        Long userId = Long.parseLong(authentication.getName());
        quizQuestionService.deleteQuestion(questionId, userId);
        return ResponseEntity.ok(ApiResponse.success("Question deleted successfully", null));
    }

    @PostMapping("/batch")
    @Operation(summary = "Add multiple questions at once")
    public ResponseEntity<ApiResponse<List<QuizQuestionResponse>>> addQuestions(
            @PathVariable Long contentId,
            @Valid @RequestBody List<QuizQuestionRequest> requests,
            Authentication authentication) {
        Long userId = Long.parseLong(authentication.getName());
        List<QuizQuestionResponse> questions = quizQuestionService.addQuestions(contentId, requests, userId);
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(ApiResponse.success("Questions added successfully", questions));
    }
}
