package com.mss301.mindmapservice.controller;

import java.util.List;

import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import com.mss301.mindmapservice.dto.ApiResponse;
import com.mss301.mindmapservice.service.AiService;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@RestController
@RequestMapping("/api/v1/ai")
@RequiredArgsConstructor
@Slf4j
@Tag(name = "AI Services", description = "APIs for AI-related operations")
public class AiController {

    private final AiService aiService;

    @GetMapping("/models/{provider}")
    @Operation(
            summary = "Get available AI models",
            description = "Get list of available models for a specific AI provider")
    public ResponseEntity<ApiResponse<List<String>>> getAvailableModels(@PathVariable String provider) {

        log.info("Getting available models for provider: {}", provider);

        List<String> models = aiService.getAvailableModels(provider);
        return ResponseEntity.ok(ApiResponse.success(models));
    }

    @GetMapping("/health/{provider}")
    @Operation(summary = "Check AI service health", description = "Check if an AI service is healthy and available")
    public ResponseEntity<ApiResponse<Boolean>> checkServiceHealth(@PathVariable String provider) {

        log.info("Checking health for AI provider: {}", provider);

        boolean isHealthy = aiService.isServiceHealthy(provider);
        return ResponseEntity.ok(ApiResponse.success(isHealthy));
    }

    @GetMapping("/health")
    @Operation(summary = "Check all AI services health", description = "Check health status of all AI services")
    public ResponseEntity<ApiResponse<Object>> checkAllServicesHealth() {

        log.info("Checking health for all AI services");

        boolean mistralHealthy = aiService.isServiceHealthy("mistral");
        boolean geminiHealthy = aiService.isServiceHealthy("gemini");

        HealthStatus healthStatus = new HealthStatus(mistralHealthy, geminiHealthy);

        return ResponseEntity.ok(ApiResponse.success(healthStatus));
    }

    private static class HealthStatus {
        public final boolean mistral;
        public final boolean gemini;
        public final boolean overall;

        public HealthStatus(boolean mistral, boolean gemini) {
            this.mistral = mistral;
            this.gemini = gemini;
            this.overall = mistral || gemini;
        }
    }
}
