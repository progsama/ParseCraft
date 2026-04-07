package com.portfolio.docanalyzer.dto;

import com.fasterxml.jackson.annotation.JsonProperty;

public record AiStructuredResponse(
        String tone,
        @JsonProperty("tone_explanation")
        String toneExplanation,
        String summary
) {
}
