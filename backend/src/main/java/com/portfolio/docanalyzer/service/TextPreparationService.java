package com.portfolio.docanalyzer.service;

import com.portfolio.docanalyzer.config.UploadProperties;
import org.springframework.stereotype.Service;

@Service
public class TextPreparationService {

    private final UploadProperties uploadProperties;

    public TextPreparationService(UploadProperties uploadProperties) {
        this.uploadProperties = uploadProperties;
    }

    public String prepareForAi(String text) {
        String normalized = text == null ? "" : text.trim();
        if (normalized.length() <= uploadProperties.maxCharsForAi()) {
            return normalized;
        }

        // Extension point: replace with chunking + map/reduce summarization in v2.
        return normalized.substring(0, uploadProperties.maxCharsForAi());
    }
}
