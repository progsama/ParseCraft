package com.portfolio.docanalyzer.util;

import com.portfolio.docanalyzer.exception.AiClientException;
import org.springframework.stereotype.Component;

@Component
public class AiJsonSanitizer {

    public String sanitizeToJson(String rawResponse) {
        if (rawResponse == null || rawResponse.isBlank()) {
            throw new AiClientException("AI returned an empty response.");
        }

        String trimmed = rawResponse.trim();

        // Remove common markdown code fence wrappers.
        if (trimmed.startsWith("```")) {
            trimmed = trimmed.replaceFirst("^```(?:json)?\\s*", "");
            trimmed = trimmed.replaceFirst("\\s*```$", "");
            trimmed = trimmed.trim();
        }

        int start = trimmed.indexOf('{');
        int end = trimmed.lastIndexOf('}');
        if (start >= 0 && end > start) {
            return trimmed.substring(start, end + 1);
        }

        throw new AiClientException("AI response did not contain a valid JSON object.");
    }
}
