package com.codeassistant.dto;

import com.fasterxml.jackson.annotation.JsonProperty;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

// ─── Inbound DTOs (Frontend → Backend) ────────────────────────────────────────

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ChatRequest {

    @NotNull(message = "Messages list must not be null")
    private List<ConversationMessage> messages;

    /** Optional: programming language context for the system prompt */
    private String language;

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class ConversationMessage {
        @NotBlank(message = "Role must not be blank")
        private String role; // "user" | "assistant"

        @NotBlank(message = "Content must not be blank")
        private String content;
    }
}