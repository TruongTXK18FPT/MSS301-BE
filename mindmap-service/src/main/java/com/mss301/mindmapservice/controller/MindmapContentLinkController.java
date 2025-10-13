package com.mss301.mindmapservice.controller;

import java.util.List;

import jakarta.validation.Valid;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

import com.mss301.mindmapservice.dto.ApiResponse;
import com.mss301.mindmapservice.dto.request.MindmapContentLinkRequest;
import com.mss301.mindmapservice.dto.response.MindmapContentLinkResponse;
import com.mss301.mindmapservice.service.MindmapContentLinkService;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;

@RestController
@RequestMapping("/api/v1/mindmap/{mindmapId}/contents")
@RequiredArgsConstructor
@Tag(name = "Mindmap Content Links")
public class MindmapContentLinkController {

    private final MindmapContentLinkService service;

    @PostMapping
    @Operation(summary = "Attach content to mindmap")
    public ResponseEntity<ApiResponse<MindmapContentLinkResponse>> attach(
            @PathVariable Long mindmapId,
            @Valid @RequestBody MindmapContentLinkRequest request,
            Authentication authentication) {
        Long userId = Long.parseLong(authentication.getName());
        var res = service.attach(mindmapId, request, userId);
        return ResponseEntity.status(HttpStatus.CREATED).body(ApiResponse.success("Attached", res));
    }

    @DeleteMapping("/{linkId}")
    @Operation(summary = "Detach content from mindmap")
    public ResponseEntity<ApiResponse<Void>> detach(
            @PathVariable Long mindmapId, @PathVariable("linkId") Long linkId, Authentication authentication) {
        Long userId = Long.parseLong(authentication.getName());
        service.detach(mindmapId, linkId, userId);
        return ResponseEntity.ok(ApiResponse.success("Detached", null));
    }

    @GetMapping
    @Operation(summary = "List mindmap contents")
    public ResponseEntity<ApiResponse<List<MindmapContentLinkResponse>>> list(
            @PathVariable Long mindmapId, Authentication authentication) {
        Long userId = Long.parseLong(authentication.getName());
        var list = service.list(mindmapId, userId);
        return ResponseEntity.ok(ApiResponse.success(list));
    }
}
