import type { AnalysisResponse } from "@/types/analysis";

interface AnalysisResultProps {
  result: AnalysisResponse;
}

export function AnalysisResult({ result }: AnalysisResultProps) {
  return (
    <section className="rounded-2xl border border-slate-200 bg-white p-6 shadow-sm">
      <h2 className="text-xl font-semibold text-slate-900">Analysis Result</h2>
      <div className="mt-5 space-y-4">
        <div>
          <p className="text-sm font-medium uppercase tracking-wide text-slate-500">
            Detected Tone
          </p>
          <p className="mt-1 text-base font-semibold text-slate-900">
            {result.tone}
          </p>
        </div>

        <div>
          <p className="text-sm font-medium uppercase tracking-wide text-slate-500">
            Tone Explanation
          </p>
          <p className="mt-1 text-slate-700">{result.toneExplanation}</p>
        </div>

        <div>
          <p className="text-sm font-medium uppercase tracking-wide text-slate-500">
            Summary ({result.summaryStyle})
          </p>
          <p className="mt-1 whitespace-pre-line text-slate-700">
            {result.summary}
          </p>
        </div>
      </div>
    </section>
  );
}
