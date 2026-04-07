package com.portfolio.docanalyzer.service;

import com.portfolio.docanalyzer.config.UploadProperties;
import com.portfolio.docanalyzer.dto.AiStructuredResponse;
import com.portfolio.docanalyzer.dto.AnalysisResponse;
import com.portfolio.docanalyzer.exception.InvalidRequestException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.mock.web.MockMultipartFile;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class DocumentAnalysisServiceTest {

    @Mock
    private DocumentParsingService documentParsingService;

    @Mock
    private TextPreparationService textPreparationService;

    @Mock
    private AiOrchestrationService aiOrchestrationService;

    private DocumentAnalysisService documentAnalysisService;

    @BeforeEach
    void setUp() {
        UploadProperties uploadProperties = new UploadProperties(List.of("pdf", "docx", "txt"), 15000);
        documentAnalysisService = new DocumentAnalysisService(
                uploadProperties,
                documentParsingService,
                textPreparationService,
                aiOrchestrationService
        );
    }

    @Test
    void shouldReturnAnalysisResponseForValidInput() {
        MockMultipartFile file = new MockMultipartFile(
                "file",
                "notes.txt",
                "text/plain",
                "Original content".getBytes()
        );

        when(documentParsingService.extractText(any(), eq("txt"))).thenReturn("Extracted text");
        when(textPreparationService.prepareForAi("Extracted text")).thenReturn("Prepared text");
        when(aiOrchestrationService.analyze(eq("Prepared text"), eq(com.portfolio.docanalyzer.model.SummaryStyle.FORMAL)))
                .thenReturn(new AiStructuredResponse("Professional", "Clear and direct", "Short summary"));

        AnalysisResponse response = documentAnalysisService.analyze(file, "formal");

        assertEquals("Professional", response.tone());
        assertEquals("Clear and direct", response.toneExplanation());
        assertEquals("Short summary", response.summary());
        assertEquals("formal", response.summaryStyle());
    }

    @Test
    void shouldRejectUnsupportedFileType() {
        MockMultipartFile file = new MockMultipartFile(
                "file",
                "notes.md",
                "text/markdown",
                "content".getBytes()
        );

        assertThrows(InvalidRequestException.class, () -> documentAnalysisService.analyze(file, "formal"));
    }

    @Test
    void shouldRejectEmptyFile() {
        MockMultipartFile file = new MockMultipartFile(
                "file",
                "empty.txt",
                "text/plain",
                new byte[0]
        );

        assertThrows(InvalidRequestException.class, () -> documentAnalysisService.analyze(file, "formal"));
    }
}
