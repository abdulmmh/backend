package com.codeassistant.dto;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

/**
 * Internal DTOs that map exactly to the Google Gemini /v1beta API contract.
 *
 * Gemini uses "contents" (not "messages"), roles are "user" / "model"
 * (not "user" / "assistant"), and the system prompt is a separate field.
 */
public class GeminiDtos {

    // ─── Request ──────────────────────────────────────────────────────────────

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class GeminiRequest {
        @JsonProperty("system_instruction")
        private SystemInstruction systemInstruction;
        private List<Content> contents;
        @JsonProperty("generationConfig")
        private GenerationConfig generationConfig;
    }

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class SystemInstruction {
        private List<Part> parts;
    }

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class Content {
        private String role; // "user" or "model"
        private List<Part> parts;
    }

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class Part {
        private String text;
    }

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class GenerationConfig {
        @JsonProperty("maxOutputTokens")
        private int maxOutputTokens;
        private double temperature;
    }

    // ─── Response ─────────────────────────────────────────────────────────────

    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    @JsonIgnoreProperties(ignoreUnknown = true)
    public static class GeminiResponse {
        private List<Candidate> candidates;
        @JsonProperty("usageMetadata")
        private UsageMetadata usageMetadata;
    }

    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    @JsonIgnoreProperties(ignoreUnknown = true)
    public static class Candidate {
        private Content content;
        @JsonProperty("finishReason")
        private String finishReason;
    }

    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    @JsonIgnoreProperties(ignoreUnknown = true)
    public static class UsageMetadata {
        @JsonProperty("promptTokenCount")
        private int promptTokenCount;
        @JsonProperty("candidatesTokenCount")
        private int candidatesTokenCount;
        @JsonProperty("totalTokenCount")
        private int totalTokenCount;
    }
}