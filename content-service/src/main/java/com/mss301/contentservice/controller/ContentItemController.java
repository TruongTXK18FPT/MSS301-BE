package com.mss301.contentservice.controller;

import java.util.List;

import jakarta.validation.Valid;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

import com.mss301.contentservice.dto.request.ContentItemRequest;
import com.mss301.contentservice.dto.response.ContentItemResponse;
import com.mss301.contentservice.service.ContentItemService;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;

@RestController
@RequestMapping("/api/v1/contents")
@RequiredArgsConstructor
@Tag(name = "Content Management")
public class ContentItemController {

    private final ContentItemService service;

    @PostMapping
    @Operation(summary = "Create content")
    public ResponseEntity<ContentItemResponse> create(
            @Valid @RequestBody ContentItemRequest request, Authentication authentication) {
        Long userId = Long.parseLong(authentication.getName());
        return ResponseEntity.status(HttpStatus.CREATED).body(service.create(request, userId));
    }

    @PutMapping("/{id}")
    @Operation(summary = "Update content")
    public ResponseEntity<ContentItemResponse> update(
            @PathVariable Long id, @Valid @RequestBody ContentItemRequest request, Authentication authentication) {
        Long userId = Long.parseLong(authentication.getName());
        return ResponseEntity.ok(service.update(id, request, userId));
    }

    @DeleteMapping("/{id}")
    @Operation(summary = "Delete content")
    public ResponseEntity<Void> delete(@PathVariable Long id, Authentication authentication) {
        Long userId = Long.parseLong(authentication.getName());
        service.delete(id, userId);
        return ResponseEntity.noContent().build();
    }

    @GetMapping("/{id}")
    @Operation(summary = "Get content by id")
    public ResponseEntity<ContentItemResponse> getById(@PathVariable Long id, Authentication authentication) {
        Long userId = Long.parseLong(authentication.getName());
        return ResponseEntity.ok(service.getById(id, userId));
    }

    @GetMapping("/me")
    @Operation(summary = "My contents")
    public ResponseEntity<List<ContentItemResponse>> my(Authentication authentication) {
        Long userId = Long.parseLong(authentication.getName());
        return ResponseEntity.ok(service.getMyContents(userId));
    }

    @GetMapping("/public")
    @Operation(summary = "Public contents")
    public ResponseEntity<List<ContentItemResponse>> publics() {
        return ResponseEntity.ok(service.getPublicContents());
    }
}
