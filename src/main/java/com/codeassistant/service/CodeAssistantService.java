package com.codeassistant.service;

import com.codeassistant.dto.GeminiDtos.*;
import com.codeassistant.dto.ChatRequest;
import com.codeassistant.dto.ChatResponse;
import com.codeassistant.exception.AnthropicApiException;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.web.reactive.function.client.WebClient;
import org.springframework.web.reactive.function.client.WebClientResponseException;

import java.util.List;
import java.util.stream.Collectors;

@Slf4j
@Service
@RequiredArgsConstructor
public class CodeAssistantService {

    private final WebClient geminiWebClient;

    @Value("${gemini.api.key}")
    private String apiKey;

    @Value("${gemini.api.model}")
    private String model;

    @Value("${gemini.api.max-tokens}")
    private int maxTokens;

    private static final String SYSTEM_PROMPT = """
            You are an expert code assistant. You write clean, efficient, and well-documented code.

            Your capabilities:
            - Write production-ready code in any language
            - Debug and fix issues with clear explanations
            - Refactor for readability, performance, and best practices
            - Explain complex concepts with concrete examples
            - Review code and suggest improvements
            - Generate unit tests and documentation

            Response guidelines:
            - Always wrap code in triple backticks with the language (e.g. ```java)
            - Keep explanations concise but complete
            - Point out edge cases or security concerns when relevant
            - Use idiomatic patterns for the target language/framework
            """;

    /**
     * Sends the conversation to Gemini and returns the assistant reply.
     * Gemini uses "model" for the assistant role, so we map accordingly.
     */
    public ChatResponse chat(ChatRequest request) {
        List<Content> contents = buildContents(request);
        String systemPrompt = buildSystemPrompt(request.getLanguage());

        GeminiRequest geminiRequest = GeminiRequest.builder()
                .systemInstruction(SystemInstruction.builder()
                        .parts(List.of(Part.builder().text(systemPrompt).build()))
                        .build())
                .contents(contents)
                .generationConfig(GenerationConfig.builder()
                        .maxOutputTokens(maxTokens)
                        .temperature(0.7)
                        .build())
                .build();

        // Gemini API key is passed as a query param, not a header
        String uri = "/v1beta/models/" + model + ":generateContent?key=" + apiKey;

        log.debug("Sending request to Gemini: model={}, turns={}", model, contents.size());

        try {
            GeminiResponse response = geminiWebClient.post()
                    .uri(uri)
                    .bodyValue(geminiRequest)
                    .retrieve()
                    .bodyToMono(GeminiResponse.class)
                    .block();

            return mapToResponse(response);

        } catch (WebClientResponseException ex) {
            log.error("Gemini API returned {}: {}", ex.getStatusCode(), ex.getResponseBodyAsString());
            HttpStatus status = HttpStatus.resolve(ex.getStatusCode().value());
            if (status == null) status = HttpStatus.INTERNAL_SERVER_ERROR;

            String userMessage = switch (ex.getStatusCode().value()) {
                case 400 -> "Bad request sent to Gemini. Please try rephrasing.";
                case 403 -> "Invalid API key. Please check your Gemini configuration.";
                case 429 -> "Rate limit exceeded. Please wait a moment and try again.";
                case 500 -> "Gemini service error. Please try again shortly.";
                default  -> "AI service error: " + ex.getMessage();
            };

            throw new AnthropicApiException(userMessage, status, ex);

        } catch (Exception ex) {
            log.error("Unexpected error calling Gemini API", ex);
            throw new AnthropicApiException(
                    "Failed to reach AI service. Please try again.",
                    HttpStatus.SERVICE_UNAVAILABLE,
                    ex
            );
        }
    }

    // ─── Private helpers ──────────────────────────────────────────────────────

    private List<Content> buildContents(ChatRequest request) {
        return request.getMessages().stream()
                .map(m -> Content.builder()
                        // Gemini uses "model" for assistant turns, not "assistant"
                        .role("assistant".equals(m.getRole()) ? "model" : "user")
                        .parts(List.of(Part.builder().text(m.getContent()).build()))
                        .build())
                .collect(Collectors.toList());
    }

    private String buildSystemPrompt(String language) {
        if (language != null && !language.isBlank()) {
            return SYSTEM_PROMPT + "\nThe user is working primarily with: " + language
                    + ". Prefer this language for examples unless told otherwise.";
        }
        return SYSTEM_PROMPT;
    }

    private ChatResponse mapToResponse(GeminiResponse response) {
        if (response == null
                || response.getCandidates() == null
                || response.getCandidates().isEmpty()) {
            throw new AnthropicApiException("Empty response from Gemini", HttpStatus.INTERNAL_SERVER_ERROR);
        }

        Candidate candidate = response.getCandidates().get(0);
        String text = candidate.getContent().getParts().stream()
                .map(Part::getText)
                .collect(Collectors.joining("\n"));

        ChatResponse.Usage usage = null;
        if (response.getUsageMetadata() != null) {
            usage = ChatResponse.Usage.builder()
                    .inputTokens(response.getUsageMetadata().getPromptTokenCount())
                    .outputTokens(response.getUsageMetadata().getCandidatesTokenCount())
                    .build();
        }

        return ChatResponse.builder()
                .content(text)
                .role("assistant")
                .usage(usage)
                .build();
    }
}