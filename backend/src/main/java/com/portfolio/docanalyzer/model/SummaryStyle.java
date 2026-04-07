package com.portfolio.docanalyzer.model;

import com.portfolio.docanalyzer.exception.InvalidRequestException;
import java.util.Arrays;

public enum SummaryStyle {
    FORMAL("formal"),
    INFORMAL("informal"),
    CASUAL("casual"),
    GEN_Z("genz");

    private final String apiValue;

    SummaryStyle(String apiValue) {
        this.apiValue = apiValue;
    }

    public String apiValue() {
        return apiValue;
    }

    public static SummaryStyle fromInput(String rawStyle) {
        if (rawStyle == null || rawStyle.isBlank()) {
            throw new InvalidRequestException("Summary style is required.");
        }
        String normalized = rawStyle.trim().toLowerCase().replace("-", "").replace(" ", "");
        return Arrays.stream(values())
                .filter(style -> style.apiValue.equals(normalized))
                .findFirst()
                .orElseThrow(() -> new InvalidRequestException(
                        "Unsupported summary style. Use: formal, informal, casual, or genz."));
    }
}
