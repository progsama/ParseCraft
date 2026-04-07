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
                - Keep summary to 3-5 sentences.
                - Summary must be a faithful summary of the source document, not generic advice.
                - Summary must include:
                  1) what happened / main purpose,
                  2) what action is required (if any),
                  3) deadline or consequence (if present).
                - Summary must include at least 2 concrete details from the document when available (for example: event, violation, date, requested action, consequence).
                - Preserve facts, names, and intent. Do not invent details.
                - Do not mention "JSON", "schema", "prompt", or formatting instructions in the summary.
                - Follow the requested style exactly: %s.
                - Style guidance: %s
                - Style intensity:
                  - formal: fully professional, policy-like wording.
                  - informal: friendly and conversational.
                  - casual: noticeably everyday and plainspoken, avoid institutional phrasing.
                  - genz: clearly Gen Z voice with 1-3 light markers (e.g., "fr", "lowkey", "no cap", "major"), but still readable.
                - Do not include markdown or code fences.
                - Ensure valid JSON object only.
                - "summary" must be plain text only.

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
                - For casual: use plainly spoken language and shorter sentences.
                - For genz: include 1-3 light Gen Z markers, but avoid overdoing slang.
                - No markdown, no code fences, no extra keys.

                Previous answer:
                %s

                Source document:
                %s
                """.formatted(reason, style.apiValue(), previousJson, documentText);
    }

    public String buildStyleRepairPrompt(String documentText, SummaryStyle style, String previousJson) {
        return """
                Rewrite ONLY the summary style while preserving facts exactly.
                Return ONLY valid JSON with keys: tone, tone_explanation, summary.
                Requested style: %s

                Keep tone and tone_explanation aligned to the original document tone.
                Do not make tone or tone_explanation sound Gen Z/casual unless the source itself has that tone.

                Specific style constraints:
                - casual: plainspoken everyday wording, short direct sentences, avoid legal/corporate diction.
                - genz: modern conversational voice with 1-3 light Gen Z markers (for example: fr, lowkey, no cap, major), still clear.

                Previous output:
                %s

                Source document:
                %s
                """.formatted(style.apiValue(), previousJson, documentText);
    }

    private String styleGuidance(SummaryStyle style) {
        return switch (style) {
            case FORMAL -> "Professional, precise, and polished. No slang. Full sentences. Corporate/legal-safe tone.";
            case INFORMAL -> "Friendly and conversational with clear wording. Light contractions are fine.";
            case CASUAL -> "Clearly everyday language, relaxed plainspoken tone, shorter direct sentences, avoid bureaucratic words.";
            case GEN_Z -> "Noticeably Gen Z voice, modern and energetic, include a few light slang markers while keeping clarity.";
        };
    }
}
