export type SummaryStyle = "formal" | "informal" | "casual" | "genz";

export interface AnalysisResponse {
  tone: string;
  toneExplanation: string;
  summary: string;
  summaryStyle: SummaryStyle;
}

export interface ApiErrorResponse {
  timestamp?: string;
  status?: number;
  error?: string;
  message?: string;
  path?: string;
}
