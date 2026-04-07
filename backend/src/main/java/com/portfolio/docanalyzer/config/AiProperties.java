package com.portfolio.docanalyzer.config;

import java.math.BigDecimal;
import java.util.List;
import org.springframework.boot.context.properties.ConfigurationProperties;

@ConfigurationProperties(prefix = "app.ai")
public record AiProperties(
        String provider,
        String baseUrl,
        String apiKey,
        String model,
        List<String> fallbackModels,
        BigDecimal temperature,
        int maxTokens
) {
}
