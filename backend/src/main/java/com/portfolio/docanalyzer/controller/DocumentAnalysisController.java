package com.portfolio.docanalyzer.controller;

import com.portfolio.docanalyzer.dto.AnalysisResponse;
import com.portfolio.docanalyzer.service.DocumentAnalysisService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.constraints.NotBlank;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;

@Validated
@RestController
@RequestMapping("/api/v1/documents")
@Tag(name = "Document Analysis", description = "Upload documents for tone + style-aware summary analysis")
public class DocumentAnalysisController {

    private final DocumentAnalysisService documentAnalysisService;

    public DocumentAnalysisController(DocumentAnalysisService documentAnalysisService) {
        this.documentAnalysisService = documentAnalysisService;
    }

    @Operation(
            summary = "Analyze document tone and summary",
            description = "Uploads a PDF, DOCX, or TXT and returns detected tone, explanation, and rewritten summary.",
            responses = {
                    @ApiResponse(responseCode = "200", description = "Analysis complete"),
                    @ApiResponse(responseCode = "400", description = "Invalid request", content = @Content(schema = @Schema(hidden = true))),
                    @ApiResponse(responseCode = "422", description = "Document parsing failed", content = @Content(schema = @Schema(hidden = true))),
                    @ApiResponse(responseCode = "502", description = "AI provider failure", content = @Content(schema = @Schema(hidden = true)))
            }
    )
    @PostMapping(
            path = "/analyze",
            consumes = MediaType.MULTIPART_FORM_DATA_VALUE,
            produces = MediaType.APPLICATION_JSON_VALUE
    )
    public ResponseEntity<AnalysisResponse> analyzeDocument(
            @RequestParam("file") MultipartFile file,
            @RequestParam("style") @NotBlank(message = "Summary style is required.") String style
    ) {
        AnalysisResponse response = documentAnalysisService.analyze(file, style);
        return ResponseEntity.ok(response);
    }
}
