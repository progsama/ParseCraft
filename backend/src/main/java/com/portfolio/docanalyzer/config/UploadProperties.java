package com.portfolio.docanalyzer.config;

import java.util.List;
import org.springframework.boot.context.properties.ConfigurationProperties;

@ConfigurationProperties(prefix = "app.upload")
public record UploadProperties(List<String> allowedExtensions, int maxCharsForAi) {
}
