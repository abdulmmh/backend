package com.codeassistant.controller;

import com.codeassistant.dto.ChatRequest;
import com.codeassistant.dto.ChatResponse;
import com.codeassistant.service.CodeAssistantService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@Slf4j
@RestController
@RequestMapping("/api/assistant")
@RequiredArgsConstructor
public class CodeAssistantController {

    private final CodeAssistantService codeAssistantService;

    /**
     * POST /api/assistant/chat
     *
     * Accepts the full conversation history and returns the next assistant reply.
     * The frontend maintains conversation state and sends the full history each time.
     */
    @PostMapping("/chat")
    public ResponseEntity<ChatResponse> chat(@Valid @RequestBody ChatRequest request) {
        log.debug("Chat request received: {} messages, language={}",
                request.getMessages().size(), request.getLanguage());

        ChatResponse response = codeAssistantService.chat(request);
        return ResponseEntity.ok(response);
    }

    /**
     * GET /api/assistant/health
     *
     * Simple liveness check used by frontend to verify backend is reachable.
     */
    @GetMapping("/health")
    public ResponseEntity<String> health() {
        return ResponseEntity.ok("Code Assistant API is running");
    }
}