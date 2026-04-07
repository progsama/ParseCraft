package com.portfolio.docanalyzer.model;

import com.portfolio.docanalyzer.exception.InvalidRequestException;
import java.util.Arrays;
import java.util.Locale;

public enum SummaryStyle {
    FORMAL("formal"),
    EVERYDAY("everyday"),
    BARD("bard");

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
        String normalized = rawStyle.trim().toLowerCase(Locale.ROOT)
                .replace("-", "")
                .replace("/", "")
                .replace(" ", "");

        return switch (normalized) {
            case "formal" -> FORMAL;
            case "everyday",
                    "genz",
                    "casual",
                    "genzcasual",
                    "informal" -> EVERYDAY;
            case "bard",
                    "herald",
                    "bardherald" -> BARD;
            default -> Arrays.stream(values())
                    .filter(s -> s.apiValue.equals(normalized))
                    .findFirst()
                    .orElseThrow(() -> new InvalidRequestException(
                            "Unsupported summary style. Use: formal, everyday, or bard."));
        };
    }
}
