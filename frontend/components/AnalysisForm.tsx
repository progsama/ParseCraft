"use client";

import { useMemo, useState } from "react";
import { analyzeDocument } from "@/lib/api";
import type { AnalysisResponse, SummaryStyle } from "@/types/analysis";
import { AnalysisResult } from "./AnalysisResult";

const STYLES: Array<{ value: SummaryStyle; label: string }> = [
  { value: "formal", label: "Formal" },
  { value: "informal", label: "Informal" },
  { value: "casual", label: "Casual" },
  { value: "genz", label: "Gen Z" },
];

const ALLOWED_EXTENSIONS = [".pdf", ".docx", ".txt"];
const MAX_FILE_SIZE_BYTES = 10 * 1024 * 1024;

export function AnalysisForm() {
  const [file, setFile] = useState<File | null>(null);
  const [style, setStyle] = useState<SummaryStyle>("formal");
  const [result, setResult] = useState<AnalysisResponse | null>(null);
  const [isLoading, setIsLoading] = useState(false);
  const [error, setError] = useState<string | null>(null);

  const fileHint = useMemo(
    () => "Supported: PDF, DOCX, TXT (max 10MB)",
    [],
  );

  function validateSelectedFile(selected: File): string | null {
    const lowerName = selected.name.toLowerCase();
    const hasAllowedExtension = ALLOWED_EXTENSIONS.some((ext) =>
      lowerName.endsWith(ext),
    );
    if (!hasAllowedExtension) {
      return "Unsupported file type. Please upload PDF, DOCX, or TXT.";
    }

    if (selected.size > MAX_FILE_SIZE_BYTES) {
      return "File is too large. Maximum allowed size is 10MB.";
    }
    return null;
  }

  function handleFileChange(event: React.ChangeEvent<HTMLInputElement>) {
    const selected = event.target.files?.[0];
    setResult(null);
    setError(null);

    if (!selected) {
      setFile(null);
      return;
    }

    const validationError = validateSelectedFile(selected);
    if (validationError) {
      setFile(null);
      setError(validationError);
      return;
    }

    setFile(selected);
  }

  async function handleSubmit(event: React.FormEvent<HTMLFormElement>) {
    event.preventDefault();
    setError(null);
    setResult(null);

    if (!file) {
      setError("Please select a file first.");
      return;
    }

    setIsLoading(true);
    try {
      const response = await analyzeDocument(file, style);
      setResult(response);
    } catch (submitError) {
      setError(
        submitError instanceof Error
          ? submitError.message
          : "Something went wrong while analyzing the document.",
      );
    } finally {
      setIsLoading(false);
    }
  }

  return (
    <div className="grid gap-6 lg:grid-cols-2">
      <section className="rounded-2xl border border-slate-200 bg-white p-6 shadow-sm">
        <h2 className="text-xl font-semibold text-slate-900">Upload Document</h2>
        <p className="mt-1 text-sm text-slate-600">{fileHint}</p>

        <form className="mt-5 space-y-5" onSubmit={handleSubmit}>
          <div>
            <label
              className="mb-2 block text-sm font-medium text-slate-700"
              htmlFor="file-upload"
            >
              Document
            </label>
            <input
              id="file-upload"
              type="file"
              accept=".pdf,.docx,.txt"
              onChange={handleFileChange}
              className="block w-full rounded-lg border border-slate-300 bg-slate-50 p-2.5 text-sm text-slate-900 file:mr-3 file:rounded-md file:border-0 file:bg-slate-900 file:px-3 file:py-1.5 file:text-sm file:font-medium file:text-white hover:file:bg-slate-700"
            />
          </div>

          <div>
            <label
              className="mb-2 block text-sm font-medium text-slate-700"
              htmlFor="style"
            >
              Summary Style
            </label>
            <select
              id="style"
              value={style}
              onChange={(event) => setStyle(event.target.value as SummaryStyle)}
              className="block w-full rounded-lg border border-slate-300 bg-white p-2.5 text-sm text-slate-900"
            >
              {STYLES.map((option) => (
                <option key={option.value} value={option.value}>
                  {option.label}
                </option>
              ))}
            </select>
          </div>

          <button
            type="submit"
            disabled={isLoading || !file}
            className="inline-flex w-full items-center justify-center rounded-lg bg-slate-900 px-4 py-2.5 text-sm font-medium text-white transition hover:bg-slate-700 disabled:cursor-not-allowed disabled:bg-slate-400"
          >
            {isLoading ? "Analyzing..." : "Analyze Document"}
          </button>
        </form>

        {error ? (
          <p className="mt-4 rounded-lg border border-red-200 bg-red-50 p-3 text-sm text-red-700">
            {error}
          </p>
        ) : null}
      </section>

      {result ? (
        <AnalysisResult result={result} />
      ) : (
        <section className="rounded-2xl border border-dashed border-slate-300 bg-slate-50 p-6">
          <h2 className="text-xl font-semibold text-slate-900">Results</h2>
          <p className="mt-2 text-slate-600">
            Submit a document to view tone analysis and rewritten summary.
          </p>
        </section>
      )}
    </div>
  );
}
