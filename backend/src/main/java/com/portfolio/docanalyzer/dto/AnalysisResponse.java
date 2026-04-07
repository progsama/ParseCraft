package com.portfolio.docanalyzer.dto;

public record AnalysisResponse(
        String tone,
        String toneExplanation,
        String summary,
        String summaryStyle
) {
}
