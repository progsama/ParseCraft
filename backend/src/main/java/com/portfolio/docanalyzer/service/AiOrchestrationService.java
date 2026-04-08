package com.portfolio.docanalyzer.service;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.portfolio.docanalyzer.client.AiClient;
import com.portfolio.docanalyzer.dto.AiStructuredResponse;
import com.portfolio.docanalyzer.exception.AiClientException;
import com.portfolio.docanalyzer.model.SummaryStyle;
import com.portfolio.docanalyzer.util.AiJsonSanitizer;
import com.portfolio.docanalyzer.util.PromptFactory;
import java.util.Locale;
import org.springframework.stereotype.Service;

@Service
public class AiOrchestrationService {

    private final AiClient aiClient;
    private final PromptFactory promptFactory;
    private final AiJsonSanitizer aiJsonSanitizer;
    private final ObjectMapper objectMapper;

    public AiOrchestrationService(
            AiClient aiClient,
            PromptFactory promptFactory,
            AiJsonSanitizer aiJsonSanitizer,
            ObjectMapper objectMapper
    ) {
        this.aiClient = aiClient;
        this.promptFactory = promptFactory;
        this.aiJsonSanitizer = aiJsonSanitizer;
        this.objectMapper = objectMapper;
    }

    public AiStructuredResponse analyze(String documentText, SummaryStyle style) {
        String prompt = style == SummaryStyle.BARD
                ? promptFactory.buildBardSummaryEmphasisPrompt(documentText)
                : promptFactory.buildPrompt(documentText, style);
        String rawResponse = aiClient.analyze(prompt);
        AiStructuredResponse parsed;
        try {
            parsed = parseWithRepairRetries(documentText, style, rawResponse);
        } catch (AiClientException ex) {
            parsed = recoverFromLineFormatFallback(documentText, style);
        }

        if (!passesQualityGate(parsed, style)) {
            String reason = "Summary was generic or style mismatch.";
            String repairPrompt = promptFactory.buildRepairPrompt(
                    documentText, style, serializeResponseForPrompt(parsed), reason);
            String repairedRaw = aiClient.analyze(repairPrompt);
            try {
                parsed = parseWithRepairRetries(documentText, style, repairedRaw);
            } catch (AiClientException ex) {
                parsed = recoverFromLineFormatFallback(documentText, style);
            }
        }

        if (style == SummaryStyle.BARD && shouldEnrichBardSummary(parsed)) {
            parsed = enrichBardSummary(documentText, parsed);
        }

        return ensureCompleteResponse(parsed, documentText, style);
    }

    /**
     * Second LLM pass focused on bard/herald summary when the first pass is empty or still reads like plain prose.
     */
    private AiStructuredResponse enrichBardSummary(String documentText, AiStructuredResponse parsed) {
        try {
            // Different wording than the primary bard prompt; avoids repeating the same failing request.
            String prompt = promptFactory.buildJsonFallbackPrompt(documentText, SummaryStyle.BARD);
            String raw = aiClient.analyze(prompt);
            AiStructuredResponse second = parseStructured(raw);
            String mergedSummary = normalize(second.summary());
            if (mergedSummary.length() < 40) {
                return parsed;
            }
            String tone = normalize(second.tone());
            String explanation = normalize(second.toneExplanation());
            return new AiStructuredResponse(
                    tone.isBlank() ? parsed.tone() : tone,
                    explanation.isBlank() ? parsed.toneExplanation() : explanation,
                    mergedSummary
            );
        } catch (AiClientException ignored) {
            return parsed;
        }
    }

    private boolean shouldEnrichBardSummary(AiStructuredResponse parsed) {
        String s = normalize(parsed.summary());
        if (s.isBlank()) {
            return true;
        }
        if (s.length() < 80) {
            return true;
        }
        long lines = s.lines().filter(line -> !line.isBlank()).count();
        if (lines < 3) {
            return true;
        }
        String low = s.toLowerCase(Locale.ROOT);
        boolean hasHeraldOpening = low.contains("hear ye")
                || low.contains("hear,")
                || low.contains("attend")
                || low.contains("hark")
                || low.contains("good people")
                || low.contains("o ye");
        return !hasHeraldOpening;
    }

    private AiStructuredResponse parseWithRepairRetries(String documentText, SummaryStyle style, String initialRawResponse) {
        String currentRaw = initialRawResponse;
        for (int attempt = 1; attempt <= 3; attempt++) {
            try {
                return parseStructured(currentRaw);
            } catch (AiClientException ex) {
                if (attempt == 3) {
                    throw ex;
                }
                String reason = "Output was not valid JSON on attempt " + attempt + ".";
                String repairPrompt = promptFactory.buildRepairPrompt(documentText, style, currentRaw, reason);
                currentRaw = aiClient.analyze(repairPrompt);
            }
        }
        throw new AiClientException("AI response could not be parsed as structured JSON after retries.");
    }

    private String serializeResponseForPrompt(AiStructuredResponse response) {
        try {
            return objectMapper.writeValueAsString(response);
        } catch (JsonProcessingException ex) {
            return "{\"tone\":\"\",\"tone_explanation\":\"\",\"summary\":\"\"}";
        }
    }

    private AiStructuredResponse recoverFromLineFormatFallback(String documentText, SummaryStyle style) {
        String jsonPrompt = promptFactory.buildJsonFallbackPrompt(documentText, style);
        try {
            String rawJson = aiClient.analyze(jsonPrompt);
            AiStructuredResponse parsed = parseStructured(rawJson);
            String s = normalize(parsed.summary());
            if (s.length() >= 20) {
                return parsed;
            }
        } catch (AiClientException ignored) {
            // Try plain-text template fallback below.
        }

        String fallbackPrompt = """
                Return exactly this plain-text template and nothing else:
                TONE: <value>
                EXPLANATION: <value>
                SUMMARY: <value>

                Rules:
                - Keep EXPLANATION to 1 sentence (about the source document tone and message).
                - Keep SUMMARY faithful to the document in %s style (for bard: short rhythmic lines).
                - Do not use JSON, markdown, or code fences.
                Base everything on this document:
                %s
                """.formatted(style.apiValue(), documentText);

        String raw = aiClient.analyze(fallbackPrompt);
        String tone = extractSectionValue(raw, "TONE:", new String[] {"EXPLANATION:", "SUMMARY:"}, "Neutral");
        String explanation = extractSectionValue(raw, "EXPLANATION:", new String[] {"SUMMARY:"}, "Tone explanation unavailable.");
        String summary = extractSectionValue(raw, "SUMMARY:", new String[] {}, "");
        return new AiStructuredResponse(tone, explanation, summary);
    }

    private String extractSectionValue(String raw, String prefix, String[] nextPrefixes, String defaultValue) {
        if (raw == null || raw.isBlank()) {
            return defaultValue;
        }

        String normalized = raw.replace("\r\n", "\n").replace("\r", "\n");
        String upper = normalized.toUpperCase(Locale.ROOT);
        String upperPrefix = prefix.toUpperCase(Locale.ROOT);

        int start = upper.indexOf(upperPrefix);
        if (start < 0) {
            return defaultValue;
        }

        int valueStart = start + upperPrefix.length();
        int end = normalized.length();
        for (String nextPrefix : nextPrefixes) {
            int next = upper.indexOf(nextPrefix.toUpperCase(Locale.ROOT), valueStart);
            if (next >= 0 && next < end) {
                end = next;
            }
        }

        String value = normalized.substring(valueStart, end).trim();
        if (value.isBlank()) {
            return defaultValue;
        }
        return value;
    }

    private AiStructuredResponse ensureCompleteResponse(
            AiStructuredResponse response,
            String documentText,
            SummaryStyle style
    ) {
        String tone = normalize(response.tone());
        String explanation = normalize(response.toneExplanation());
        String summary = normalize(response.summary());

        if (isMissing(tone)) {
            tone = inferTone(documentText);
        }
        if (isMissing(explanation)) {
            explanation = inferToneExplanation(tone);
        }
        if (isMissing(summary)) {
            summary = inferSummary(documentText, style);
        }

        return new AiStructuredResponse(tone, explanation, summary);
    }

    private boolean isMissing(String value) {
        if (value.isBlank()) {
            return true;
        }
        String lower = value.toLowerCase(Locale.ROOT).trim();
        // Do not treat "neutral" as missing — it is a valid short tone label.
        return lower.equals("unavailable")
                || lower.equals("n/a")
                || lower.equals("summary unavailable.")
                || lower.equals("tone explanation unavailable.");
    }

    private String normalize(String value) {
        return value == null ? "" : value.trim();
    }

    private String inferTone(String documentText) {
        String text = documentText.toLowerCase(Locale.ROOT);
        if (text.contains("suspend") || text.contains("discipline") || text.contains("violation")) {
            return "Serious and formal";
        }
        return "Neutral and informative";
    }

    private String inferToneExplanation(String tone) {
        if (tone.toLowerCase(Locale.ROOT).contains("serious")) {
            return "The document uses direct and formal language to communicate consequences and policy-based decisions.";
        }
        return "The document presents information in a clear, explanatory tone focused on facts.";
    }

    private String inferSummary(String documentText, SummaryStyle style) {
        String compact = documentText.replaceAll("\\s+", " ").trim();
        if (compact.isBlank()) {
            return "The document could not be summarized from the extracted text.";
        }
        String base = compact.length() > 420 ? compact.substring(0, 420) + "..." : compact;
        return switch (style) {
            case FORMAL -> "This document states: " + base;
            case EVERYDAY, BARD -> "The model did not return a valid styled summary. "
                    + "Try again, shorten the text, or check your AI provider quota. "
                    + "Excerpt for reference: " + base;
        };
    }

    private AiStructuredResponse parseStructured(String rawResponse) {
        String jsonPayload = aiJsonSanitizer.sanitizeToJson(rawResponse);
        try {
            return objectMapper.readValue(jsonPayload, AiStructuredResponse.class);
        } catch (JsonProcessingException ex) {
            throw new AiClientException("AI response could not be parsed as structured JSON.", ex);
        }
    }

    private boolean passesQualityGate(AiStructuredResponse response, SummaryStyle style) {
        String summary = response.summary() == null ? "" : response.summary().trim();
        String explanation = response.toneExplanation() == null ? "" : response.toneExplanation().trim();
        String combined = (summary + " " + explanation).toLowerCase(Locale.ROOT);

        int minLen = switch (style) {
            case FORMAL -> 80;
            case EVERYDAY, BARD -> 50;
        };
        if (summary.length() < minLen) {
            return false;
        }

        if (containsGenericPhrases(combined)) {
            return false;
        }

        return switch (style) {
            case FORMAL -> !containsSlang(combined);
            case EVERYDAY -> true;
            case BARD -> true;
        };
    }

    private boolean containsGenericPhrases(String text) {
        return text.contains("summary of the provided text")
                || text.contains("this is a summary")
                || text.contains("the document outlines")
                || text.contains("the text provides information")
                || text.contains("without going into detail");
    }

    private boolean containsSlang(String text) {
        return text.contains("fr")
                || text.contains("no cap")
                || text.contains("lowkey")
                || text.contains("vibe");
    }
}
