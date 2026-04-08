package com.portfolio.docanalyzer.controller;

import com.portfolio.docanalyzer.client.AiClient;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.http.MediaType;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.test.web.servlet.MockMvc;

import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.multipart;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
class DocumentAnalysisControllerIntegrationTest {

    @Autowired
    private MockMvc mockMvc;

    @MockBean
    private AiClient aiClient;

    @Test
    void shouldReturnHealthOk() throws Exception {
        mockMvc.perform(get("/api/v1/health"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("ok"));
    }

    @Test
    void shouldAnalyzeDocumentSuccessfully() throws Exception {
        when(aiClient.analyze(anyString())).thenReturn("""
                {
                  "tone": "Confident and clear",
                  "tone_explanation": "The writing is direct and persuasive.",
                  "summary": "This is the rewritten summary."
                }
                """);

        MockMultipartFile file = new MockMultipartFile(
                "file",
                "sample.txt",
                MediaType.TEXT_PLAIN_VALUE,
                "The original document content goes here.".getBytes()
        );

        mockMvc.perform(multipart("/api/v1/documents/analyze")
                        .file(file)
                        .param("style", "formal"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.tone").value("Confident and clear"))
                .andExpect(jsonPath("$.toneExplanation").value("The writing is direct and persuasive."))
                .andExpect(jsonPath("$.summary").value("This is the rewritten summary."))
                .andExpect(jsonPath("$.summaryStyle").value("formal"));
    }

    @Test
    void shouldAnalyzePastedTextWithoutFile() throws Exception {
        when(aiClient.analyze(anyString())).thenReturn("""
                {
                  "tone": "Formal",
                  "tone_explanation": "Official notice tone.",
                  "summary": "Summary from paste."
                }
                """);

        mockMvc.perform(multipart("/api/v1/documents/analyze")
                        .param("text", "Suspension letter body text here.")
                        .param("style", "bard"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.summaryStyle").value("bard"));
    }

    @Test
    void shouldReturnBadRequestForUnsupportedFileType() throws Exception {
        MockMultipartFile file = new MockMultipartFile(
                "file",
                "sample.md",
                "text/markdown",
                "Invalid type content".getBytes()
        );

        mockMvc.perform(multipart("/api/v1/documents/analyze")
                        .file(file)
                        .param("style", "formal"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.message").value("Unsupported file type. Allowed: pdf, docx, txt."));
    }
}
