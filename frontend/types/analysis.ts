/** API values; aliases accepted server-side (e.g. casual, genz -> everyday). */
export type SummaryStyle = "formal" | "everyday" | "bard";

export interface AnalysisResponse {
  tone: string;
  toneExplanation: string;
  summary: string;
  summaryStyle: string;
}

export interface ApiErrorResponse {
  timestamp?: string;
  status?: number;
  error?: string;
  message?: string;
  path?: string;
}
