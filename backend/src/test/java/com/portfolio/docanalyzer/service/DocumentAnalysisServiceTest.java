package com.portfolio.docanalyzer.service;

import com.portfolio.docanalyzer.config.UploadProperties;
import com.portfolio.docanalyzer.dto.AiStructuredResponse;
import com.portfolio.docanalyzer.dto.AnalysisResponse;
import com.portfolio.docanalyzer.exception.InvalidRequestException;
import com.portfolio.docanalyzer.model.SummaryStyle;
import java.util.List;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.mock.web.MockMultipartFile;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class DocumentAnalysisServiceTest {

    @Mock
    private DocumentParsingService documentParsingService;

    @Mock
    private TextPreparationService textPreparationService;

    @Mock
    private AiOrchestrationService aiOrchestrationService;

    private DocumentAnalysisService newDocumentAnalysisService() {
        UploadProperties uploadProperties = new UploadProperties(List.of("pdf", "docx", "txt"), 15000);
        return new DocumentAnalysisService(
                uploadProperties,
                documentParsingService,
                textPreparationService,
                aiOrchestrationService
        );
    }

    @Test
    void shouldReturnAnalysisResponseForValidFile() {
        DocumentAnalysisService documentAnalysisService = newDocumentAnalysisService();
        MockMultipartFile file = new MockMultipartFile(
                "file",
                "notes.txt",
                "text/plain",
                "Original content".getBytes()
        );

        when(documentParsingService.extractText(any(), eq("txt"))).thenReturn("Extracted text");
        when(textPreparationService.prepareForAi("Extracted text")).thenReturn("Prepared text");
        when(aiOrchestrationService.analyze(eq("Prepared text"), eq(SummaryStyle.FORMAL)))
                .thenReturn(new AiStructuredResponse("Professional", "Clear and direct", "Short summary"));

        AnalysisResponse response = documentAnalysisService.analyze(file, null, "formal");

        assertEquals("Professional", response.tone());
        assertEquals("Clear and direct", response.toneExplanation());
        assertEquals("Short summary", response.summary());
        assertEquals("formal", response.summaryStyle());
    }

    @Test
    void shouldPreferPastedTextOverFile() {
        DocumentAnalysisService documentAnalysisService = newDocumentAnalysisService();
        MockMultipartFile file = new MockMultipartFile(
                "file",
                "notes.txt",
                "text/plain",
                "ignored".getBytes()
        );

        when(textPreparationService.prepareForAi("Pasted body")).thenReturn("Prepared text");
        when(aiOrchestrationService.analyze(eq("Prepared text"), eq(SummaryStyle.EVERYDAY)))
                .thenReturn(new AiStructuredResponse("Casual", "Expl", "Sum"));

        AnalysisResponse response = documentAnalysisService.analyze(file, "Pasted body", "everyday");

        assertEquals("everyday", response.summaryStyle());
        verify(documentParsingService, never()).extractText(any(), any());
    }

    @Test
    void shouldRejectUnsupportedFileType() {
        DocumentAnalysisService documentAnalysisService = newDocumentAnalysisService();
        MockMultipartFile file = new MockMultipartFile(
                "file",
                "notes.md",
                "text/markdown",
                "content".getBytes()
        );

        InvalidRequestException ex =
                assertThrows(InvalidRequestException.class, () -> documentAnalysisService.analyze(file, null, "formal"));
        assertFalse(ex.getMessage().isBlank());
    }

    @Test
    void shouldRejectEmptyFileWhenNoText() {
        DocumentAnalysisService documentAnalysisService = newDocumentAnalysisService();
        MockMultipartFile file = new MockMultipartFile(
                "file",
                "empty.txt",
                "text/plain",
                new byte[0]
        );

        InvalidRequestException ex =
                assertThrows(InvalidRequestException.class, () -> documentAnalysisService.analyze(file, null, "formal"));
        assertFalse(ex.getMessage().isBlank());
    }
}
