package com.portfolio.docanalyzer.util;

import com.portfolio.docanalyzer.model.SummaryStyle;
import org.springframework.stereotype.Component;

@Component
public class PromptFactory {

    public String buildPrompt(String documentText, SummaryStyle style) {
        String styleGuidance = styleGuidance(style);
        return """
                You are a document analysis assistant.
                Analyze the input document and respond strictly as JSON with keys:
                - tone
                - tone_explanation
                - summary

                Rules:
                - Keep tone concise (2-4 words) and based on the source document only.
                - Keep tone_explanation to 1-2 sentences explaining what the source document is trying to convey and why that tone fits.
                - tone and tone_explanation must stay source-accurate across all summary styles.
                - Summary must be a faithful summary of the source document, not generic advice.
                - Summary must include: main purpose, key action if any, deadline or consequence if present.
                - Summary must include at least 2 concrete details from the document when available.
                - Preserve facts, names, and intent. Do not invent details.
                - Do not mention "JSON", "schema", "prompt", or formatting instructions in the summary.
                - Follow the requested summary style exactly: %s.
                - Style guidance: %s
                - Do not include markdown or code fences.
                - Ensure valid JSON object only.
                - "summary" must be plain text only (for bard style, use line breaks between short lines if needed).

                Document:
                %s
                """.formatted(style.apiValue(), styleGuidance, documentText);
    }

    public String buildRepairPrompt(String documentText, SummaryStyle style, String previousJson, String reason) {
        return """
                Your previous answer was not acceptable.
                Reason: %s

                Return ONLY valid JSON with keys:
                - tone
                - tone_explanation
                - summary

                Requirements:
                - Keep the factual meaning anchored to the source document.
                - tone and tone_explanation must reflect the source document tone (not the requested style).
                - only summary should match %s style.
                - summary must include concrete document details, not generic phrasing.
                %s
                - No markdown, no code fences, no extra keys.

                Previous answer:
                %s

                Source document:
                %s
                """.formatted(reason, style.apiValue(), styleRepairHints(style), previousJson, documentText);
    }

    public String buildStyleRepairPrompt(String documentText, SummaryStyle style, String previousJson) {
        return """
                Rewrite ONLY the summary style while preserving facts exactly.
                Return ONLY valid JSON with keys: tone, tone_explanation, summary.
                Requested style: %s

                Keep tone and tone_explanation aligned to the original document tone.

                %s

                Previous output:
                %s

                Source document:
                %s
                """.formatted(style.apiValue(), styleRepairBlock(style), previousJson, documentText);
    }

    private String styleRepairHints(SummaryStyle style) {
        return switch (style) {
            case FORMAL -> "- For formal: documentation/office/authority language; no slang.";
            case EVERYDAY -> "- For everyday: plain modern speech; light contractions OK; optional light Gen Z markers (fr, ngl, lowkey) — do not overdo.";
            case BARD -> "- For bard: short herald-style lines, rhythmic, like a town crier announcing news; still accurate.";
        };
    }

    private String styleRepairBlock(SummaryStyle style) {
        return switch (style) {
            case FORMAL -> """
                    Summary style — formal:
                    Language of documentation, offices, and people in authority: precise, structured, professional.
                    """;
            case EVERYDAY -> """
                    Summary style — everyday / Gen Z–casual:
                    How regular people talk day to day; may include light Gen Z phrasing; stay clear and readable.
                    """;
            case BARD -> """
                    Summary style — bard / herald:
                    Deliver the summary as a short spoken announcement: stately, rhythmic lines (like a herald or bard proclaiming news).
                    You may use "Hear ye" or "Attend:" sparingly; keep facts correct; 4-8 short lines is enough.
                    """;
        };
    }

    private String styleGuidance(SummaryStyle style) {
        return switch (style) {
            case FORMAL -> """
                    Formal: language used in documentation, offices, and by people of authority. Precise, neutral, professional.
                    No slang. Full sentences. Suitable for policy letters and official notices.
                    """;
            case EVERYDAY -> """
                    Gen Z / casual everyday: natural speech regular people use, including light Gen Z markers if they fit.
                    Relaxed, conversational, easy to read; avoid stiff legal tone in the summary only.
                    """;
            case BARD -> """
                    Bard / herald: rewrite the summary as a short rhythmic proclamation (verse-like spacing OK in plain text).
                    Stately, memorable, medieval-herald flavor ("Attend, good people…", short lines), but every fact must stay true.
                    """;
        };
    }
}
