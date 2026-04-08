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
                - For non-formal styles: the summary must be fully REWRITTEN in that voice. Do not copy-paste formal sentences,
                  letterhead phrasing, or long quotations from the source. Paraphrase every idea in the requested style.
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

    /**
     * Short, JSON-only recovery prompt when the primary structured response could not be parsed.
     * Truncates very long inputs to reduce token errors.
     */
    public String buildJsonFallbackPrompt(String documentText, SummaryStyle style) {
        String doc = documentText.length() > 14_000 ? documentText.substring(0, 14_000) + "\n[document truncated for length]" : documentText;
        String styleGuidance = styleGuidance(style);
        return """
                Output a single JSON object only. No markdown code fences. No text before or after the JSON.
                Required keys (exactly, use double quotes): "tone", "tone_explanation", "summary".
                tone and tone_explanation describe the source document (formal/casual register of the original), not the summary voice.
                The summary field must follow style "%s" only.

                Style rules for the summary field:
                %s

                Rules:
                - Paraphrase; do not copy-paste long spans from the document into summary.
                - Include concrete facts (names, dates, outcomes) when present.

                Document:
                %s
                """.formatted(style.apiValue(), styleGuidance, doc);
    }

    /**
     * Second pass for bard only: model sometimes returns an empty or prose-like summary on long documents.
     * Uses a shorter excerpt so the herald-style rewrite fits in the output budget.
     */
    public String buildBardSummaryEmphasisPrompt(String documentText) {
        String doc = documentText.length() > 10_000 ? documentText.substring(0, 10_000) + "\n[...truncated...]" : documentText;
        return """
                You output JSON only. Keys: "tone", "tone_explanation", "summary" (use tone_explanation with underscore).
                tone: 2-4 words describing the DOCUMENT's register (e.g. Serious and formal).
                tone_explanation: 1-2 sentences about how the source document sounds (stay accurate to the source).
                summary: THIS FIELD IS THE ONLY PLACE FOR BARD STYLE. It must be 6-14 short lines separated by newlines.
                Start with a herald opening ("Hear ye", "Attend, good people", or "Hark"). Each line is rhythmic, like a town crier.
                Paraphrase facts only — do not copy letterhead, salutations, or long formal sentences from the document.
                Include names, dates, and outcomes when present.

                Document:
                %s
                """.formatted(doc);
    }

    private String styleRepairHints(SummaryStyle style) {
        return switch (style) {
            case FORMAL -> "- For formal: documentation/office/authority language; no slang.";
            case EVERYDAY -> "- For everyday: plain modern speech; light contractions OK; optional light Gen Z markers (fr, ngl, lowkey) — do not overdo.";
            case BARD -> "- For bard: short herald-style lines, rhythmic, like a town crier announcing news; still accurate.";
        };
    }

    private String styleGuidance(SummaryStyle style) {
        return switch (style) {
            case FORMAL -> """
                    Formal: language used in documentation, offices, and by people of authority. Precise, neutral, professional.
                    No slang. Full sentences. Suitable for policy letters and official notices.
                    """;
            case EVERYDAY -> """
                    Gen Z / casual everyday: write the whole summary like you are explaining to a friend.
                    Use contractions (it's, don't, they're), plain words, and short sentences. You may use light Gen Z phrasing
                    (fr, ngl, lowkey) sparingly if it fits — do not force slang. Never keep the stiff legal or office voice
                    of the source in the summary field.
                    """;
            case BARD -> """
                    Bard / herald: the entire summary must be a short spoken proclamation in 4–12 short lines (like a town crier).
                    Open with a herald flourish ("Hear ye", "Attend, good people", "Hark") then deliver facts in rhythmic lines.
                    Do not paste the original letter's sentences or paragraphs. Paraphrase only; every line should sound like
                    verse or a proclamation, not a copy of the document.
                    """;
        };
    }
}
