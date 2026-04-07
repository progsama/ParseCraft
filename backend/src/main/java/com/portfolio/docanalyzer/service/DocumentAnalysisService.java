package com.portfolio.docanalyzer.service;

import com.portfolio.docanalyzer.config.UploadProperties;
import com.portfolio.docanalyzer.dto.AiStructuredResponse;
import com.portfolio.docanalyzer.dto.AnalysisResponse;
import com.portfolio.docanalyzer.exception.InvalidRequestException;
import com.portfolio.docanalyzer.model.SummaryStyle;
import java.util.Locale;
import java.util.Objects;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

@Service
public class DocumentAnalysisService {

    private final UploadProperties uploadProperties;
    private final DocumentParsingService documentParsingService;
    private final TextPreparationService textPreparationService;
    private final AiOrchestrationService aiOrchestrationService;

    public DocumentAnalysisService(
            UploadProperties uploadProperties,
            DocumentParsingService documentParsingService,
            TextPreparationService textPreparationService,
            AiOrchestrationService aiOrchestrationService
    ) {
        this.uploadProperties = uploadProperties;
        this.documentParsingService = documentParsingService;
        this.textPreparationService = textPreparationService;
        this.aiOrchestrationService = aiOrchestrationService;
    }

    public AnalysisResponse analyze(MultipartFile file, String styleInput) {
        validateFile(file);
        SummaryStyle style = SummaryStyle.fromInput(styleInput);
        String extension = getExtension(Objects.requireNonNull(file.getOriginalFilename()));
        validateFileType(extension);

        String extractedText = documentParsingService.extractText(file, extension);
        if (extractedText.isBlank()) {
            throw new InvalidRequestException("The uploaded file does not contain readable text.");
        }

        String aiReadyText = textPreparationService.prepareForAi(extractedText);
        AiStructuredResponse aiResponse = aiOrchestrationService.analyze(aiReadyText, style);

        return new AnalysisResponse(
                aiResponse.tone(),
                aiResponse.toneExplanation(),
                aiResponse.summary(),
                style.apiValue()
        );
    }

    private void validateFile(MultipartFile file) {
        if (file == null || file.isEmpty()) {
            throw new InvalidRequestException("A non-empty file is required.");
        }
    }

    private void validateFileType(String extension) {
        boolean allowed = uploadProperties.allowedExtensions().stream()
                .map(s -> s.toLowerCase(Locale.ROOT).trim())
                .anyMatch(allowedExt -> allowedExt.equals(extension));
        if (!allowed) {
            throw new InvalidRequestException("Unsupported file type. Allowed: pdf, docx, txt.");
        }
    }

    private String getExtension(String filename) {
        int idx = filename.lastIndexOf('.');
        if (idx == -1 || idx == filename.length() - 1) {
            throw new InvalidRequestException("File must include an extension.");
        }
        return filename.substring(idx + 1).toLowerCase(Locale.ROOT);
    }
}
