package com.portfolio.docanalyzer.client;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.portfolio.docanalyzer.config.AiProperties;
import com.portfolio.docanalyzer.exception.AiClientException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.http.HttpStatusCode;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClientException;
import org.springframework.web.client.RestClientResponseException;
import org.springframework.web.client.RestClient;

@Component
@ConditionalOnProperty(name = "app.ai.provider", havingValue = "gemini")
public class GeminiClient implements AiClient {

    private final RestClient restClient;
    private final AiProperties aiProperties;
    private final ObjectMapper objectMapper;

    public GeminiClient(AiProperties aiProperties) {
        this.aiProperties = aiProperties;
        this.objectMapper = new ObjectMapper();
        String baseUrl = Objects.requireNonNull(aiProperties.baseUrl(), "app.ai.base-url must be set for Gemini");
        if (baseUrl.isBlank()) {
            throw new IllegalStateException("app.ai.base-url must be non-blank for Gemini");
        }
        this.restClient = RestClient.builder()
                .baseUrl(baseUrl)
                .defaultHeader(HttpHeaders.CONTENT_TYPE, MediaType.APPLICATION_JSON_VALUE)
                .build();
    }

    @Override
    public String analyze(String prompt) {
        if (aiProperties.apiKey() == null || aiProperties.apiKey().isBlank()) {
            throw new AiClientException("AI_API_KEY is not configured.");
        }

        Map<String, Object> payload = Map.of(
                "contents", List.of(Map.of(
                        "parts", List.of(Map.of("text", prompt))
                )),
                "generationConfig", Map.of(
                        "temperature", aiProperties.temperature(),
                        "maxOutputTokens", aiProperties.maxTokens(),
                        "responseMimeType", "application/json",
                        "responseSchema", Map.of(
                                "type", "OBJECT",
                                "required", List.of("tone", "tone_explanation", "summary"),
                                "properties", Map.of(
                                        "tone", Map.of("type", "STRING"),
                                        "tone_explanation", Map.of("type", "STRING"),
                                        "summary", Map.of("type", "STRING")
                                )
                        )
                )
        );

        JsonNode root = executeWithRetry(payload);

        try {
            JsonNode firstCandidate = root.path("candidates").get(0);
            if (firstCandidate == null || firstCandidate.isMissingNode()) {
                String blockReason = root.path("promptFeedback").path("blockReason").asText();
                if (!blockReason.isBlank()) {
                    throw new AiClientException("Gemini blocked the response: " + blockReason);
                }
                throw new AiClientException("Gemini returned no candidates for this request.");
            }

            StringBuilder textBuilder = new StringBuilder();
            JsonNode parts = firstCandidate.path("content").path("parts");
            for (JsonNode part : parts) {
                String piece = part.path("text").asText();
                if (piece != null && !piece.isBlank()) {
                    textBuilder.append(piece);
                }
            }

            String result = textBuilder.toString().trim();
            if (result.isBlank()) {
                throw new AiClientException("Gemini returned an empty candidate payload.");
            }
            return result;
        } catch (AiClientException ex) {
            throw ex;
        } catch (RuntimeException ex) {
            String compactRoot;
            try {
                compactRoot = objectMapper.writeValueAsString(root);
            } catch (JsonProcessingException jpe) {
                compactRoot = "<unavailable>";
            }
            throw new AiClientException("Invalid response received from Gemini provider. Payload: " + compactRoot, ex);
        }
    }

    private JsonNode executeWithRetry(Map<String, Object> payload) {
        int attempts = 3;
        long[] backoffMs = new long[] {400L, 900L, 1600L};
        List<String> models = resolveModelOrder();
        String lastError = null;

        for (String model : models) {
            for (int i = 0; i < attempts; i++) {
                try {
                    return restClient.post()
                            .uri(uriBuilder -> uriBuilder
                                    .path("/v1beta/models/{model}:generateContent")
                                    .queryParam("key", aiProperties.apiKey())
                                    .build(model))
                            .body(Objects.requireNonNull(payload))
                            .retrieve()
                            .body(JsonNode.class);
                } catch (RestClientResponseException ex) {
                    String providerMessage = ex.getResponseBodyAsString();
                    lastError = "model=%s status=%s body=%s".formatted(model, ex.getStatusCode(), providerMessage);
                    if (!isRetryable(ex.getStatusCode()) || i == attempts - 1) {
                        // Try next fallback model when possible.
                        break;
                    }
                    sleepQuietly(backoffMs[i]);
                } catch (RestClientException ex) {
                    lastError = "model=%s error=%s".formatted(model, ex.getMessage());
                    if (i == attempts - 1) {
                        break;
                    }
                    sleepQuietly(backoffMs[i]);
                }
            }
        }

        throw new AiClientException("Gemini API request failed after retries/fallbacks. " + lastError);
    }

    private boolean isRetryable(HttpStatusCode status) {
        int code = status.value();
        return code == 429 || code == 500 || code == 503;
    }

    private void sleepQuietly(long millis) {
        try {
            Thread.sleep(millis);
        } catch (InterruptedException ie) {
            Thread.currentThread().interrupt();
        }
    }

    private List<String> resolveModelOrder() {
        List<String> models = new ArrayList<>();
        models.add(aiProperties.model());
        if (aiProperties.fallbackModels() != null) {
            aiProperties.fallbackModels().stream()
                    .filter(model -> model != null && !model.isBlank())
                    .map(String::trim)
                    .forEach(models::add);
        }

        if (models.size() == 1) {
            // Safe defaults for stability under transient model overload/quota limits.
            models.addAll(Arrays.asList("gemini-2.0-flash", "gemini-flash-lite-latest"));
        }
        return Collections.unmodifiableList(models);
    }
}
