package com.mss301.ragservice.controller;

import com.mss301.ragservice.dto.request.TtsRequest;
import com.mss301.ragservice.dto.response.TtsResponse;
import com.mss301.ragservice.service.TtsService;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@Slf4j
@RestController
@RequestMapping("/api/v1/tts")
@RequiredArgsConstructor
@Tag(name = "Tts Service", description = "Text-to-Speech Generation API endpoints")
public class TtsController {

    private final TtsService ttsService;

    @PostMapping("/speak")
    public ResponseEntity<TtsResponse> speak(@RequestBody TtsRequest request) {
        return ResponseEntity.ok(ttsService.speak(request));
    }
}
