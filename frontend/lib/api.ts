import type { AnalysisResponse, ApiErrorResponse, SummaryStyle } from "@/types/analysis";

const API_BASE_URL =
  process.env.NEXT_PUBLIC_API_BASE_URL ?? "http://localhost:8080";

export async function analyzeDocument(
  options: { file?: File | null; text?: string },
  style: SummaryStyle,
): Promise<AnalysisResponse> {
  const formData = new FormData();
  formData.append("style", style);

  const trimmed = options.text?.trim() ?? "";
  if (trimmed.length > 0) {
    formData.append("text", trimmed);
  } else if (options.file) {
    formData.append("file", options.file);
  }

  const response = await fetch(`${API_BASE_URL}/api/v1/documents/analyze`, {
    method: "POST",
    body: formData,
  });

  if (!response.ok) {
    let errorMessage = `Request failed with status ${response.status}`;
    try {
      const parsed = (await response.json()) as ApiErrorResponse;
      if (parsed.message) {
        errorMessage = parsed.message;
      }
    } catch {
      // Fall back to generic message for non-JSON responses.
    }
    throw new Error(errorMessage);
  }

  return (await response.json()) as AnalysisResponse;
}
