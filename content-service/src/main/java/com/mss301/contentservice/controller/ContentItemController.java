package com.mss301.contentservice.controller;

import java.util.List;

import jakarta.validation.Valid;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

import com.mss301.contentservice.dto.ApiResponse;
import com.mss301.contentservice.dto.request.ContentItemRequest;
import com.mss301.contentservice.dto.response.ContentItemResponse;
import com.mss301.contentservice.service.ContentItemService;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;

@RestController
@RequestMapping("/contents")
@RequiredArgsConstructor
@Tag(name = "Content Management")
public class ContentItemController {

    private final ContentItemService service;

    @PostMapping
    @Operation(summary = "Create content")
    public ResponseEntity<ApiResponse<ContentItemResponse>> create(
            @Valid @RequestBody ContentItemRequest request, 
            @RequestParam(required = false) Long classroomId,
            Authentication authentication) {
        Long userId = Long.parseLong(authentication.getName());
        ContentItemResponse response = service.create(request, userId, classroomId);
        return ResponseEntity.status(HttpStatus.CREATED).body(ApiResponse.success(response));
    }

    @PutMapping("/{id}")
    @Operation(summary = "Update content")
    public ResponseEntity<ApiResponse<ContentItemResponse>> update(
            @PathVariable Long id, @Valid @RequestBody ContentItemRequest request, Authentication authentication) {
        Long userId = Long.parseLong(authentication.getName());
        ContentItemResponse response = service.update(id, request, userId);
        return ResponseEntity.ok(ApiResponse.success(response));
    }

    @DeleteMapping("/{id}")
    @Operation(summary = "Delete content")
    public ResponseEntity<ApiResponse<Void>> delete(@PathVariable Long id, Authentication authentication) {
        Long userId = Long.parseLong(authentication.getName());
        service.delete(id, userId);
        return ResponseEntity.ok(ApiResponse.success("Xóa thành công", null));
    }

    @GetMapping("/{id}")
    @Operation(summary = "Get content by id")
    public ResponseEntity<ApiResponse<ContentItemResponse>> getById(@PathVariable Long id, Authentication authentication) {
        Long userId = Long.parseLong(authentication.getName());
        ContentItemResponse response = service.getById(id, userId);
        return ResponseEntity.ok(ApiResponse.success(response));
    }

    @GetMapping("/me")
    @Operation(summary = "My contents")
    public ResponseEntity<ApiResponse<List<ContentItemResponse>>> my(
            @RequestParam(required = false) Long classroomId,
            @RequestParam(required = false) String type,
            Authentication authentication) {
        Long userId = Long.parseLong(authentication.getName());
        List<ContentItemResponse> responses = service.getMyContents(userId, classroomId, type);
        return ResponseEntity.ok(ApiResponse.success(responses));
    }

    @GetMapping("/public")
    @Operation(summary = "Public contents")
    public ResponseEntity<ApiResponse<List<ContentItemResponse>>> publics(
            @RequestParam(required = false) String type,
            @RequestParam(required = false) String subject,
            @RequestParam(required = false) String grade) {
        List<ContentItemResponse> responses = service.getPublicContents(type, subject, grade);
        return ResponseEntity.ok(ApiResponse.success(responses));
    }

    @GetMapping("/search")
    @Operation(summary = "Search public contents by subject/grade/keyword")
    public ResponseEntity<ApiResponse<List<ContentItemResponse>>> search(
            @RequestParam(required = false) String subject,
            @RequestParam(required = false) String grade,
            @RequestParam(required = false) String keyword,
            @RequestParam(required = false) String type) {
        List<ContentItemResponse> responses = service.searchPublic(subject, grade, keyword, type);
        return ResponseEntity.ok(ApiResponse.success(responses));
    }

    @GetMapping("/classroom/{classroomId}")
    @Operation(summary = "Get contents by classroom")
    public ResponseEntity<ApiResponse<List<ContentItemResponse>>> getByClassroom(
            @PathVariable Long classroomId,
            @RequestParam(required = false) String type,
            Authentication authentication) {
        Long userId = Long.parseLong(authentication.getName());
        List<ContentItemResponse> responses = service.getByClassroom(classroomId, type, userId);
        return ResponseEntity.ok(ApiResponse.success(responses));
    }
}
