package com.portfolio.docanalyzer.client;

import java.util.List;
import java.util.Map;

import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClient;
import org.springframework.web.client.RestClientException;

import com.fasterxml.jackson.databind.JsonNode;
import com.portfolio.docanalyzer.config.AiProperties;
import com.portfolio.docanalyzer.exception.AiClientException;

@Component
@ConditionalOnProperty(name = "app.ai.provider", havingValue = "openai", matchIfMissing = true)
public class OpenAiClient implements AiClient {

    private final RestClient restClient;
    private final AiProperties aiProperties;

    public OpenAiClient(AiProperties aiProperties) {
        this.aiProperties = aiProperties;
        this.restClient = RestClient.builder()
                .baseUrl(aiProperties.baseUrl())
                .defaultHeader(HttpHeaders.CONTENT_TYPE, MediaType.APPLICATION_JSON_VALUE)
                .build();
    }

    @Override
    public String analyze(String prompt) {
        if (aiProperties.apiKey() == null || aiProperties.apiKey().isBlank()) {
            throw new AiClientException("AI_API_KEY is not configured.");
        }

        Map<String, Object> payload = Map.of(
                "model", aiProperties.model(),
                "temperature", aiProperties.temperature(),
                "max_tokens", aiProperties.maxTokens(),
                "messages", List.of(
                        Map.of("role", "system", "content", "You are a strict JSON generator."),
                        Map.of("role", "user", "content", prompt)
                )
        );

        JsonNode root;
        try {
            root = restClient.post()
                    .uri("/v1/chat/completions")
                    .header(HttpHeaders.AUTHORIZATION, "Bearer " + aiProperties.apiKey())
                    .body(payload)
                    .retrieve()
                    .body(JsonNode.class);
        } catch (RestClientException ex) {
            throw new AiClientException("OpenAI API request failed. Check API key, model, and quota.", ex);
        }

        try {
            return root.path("choices").get(0).path("message").path("content").asText();
        } catch (Exception ex) {
            throw new AiClientException("Invalid response received from AI provider.", ex);
        }
    }
}
