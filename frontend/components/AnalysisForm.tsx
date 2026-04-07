"use client";

import { useMemo, useState } from "react";
import { analyzeDocument } from "@/lib/api";
import type { AnalysisResponse, SummaryStyle } from "@/types/analysis";
import { AnalysisResult } from "./AnalysisResult";

const STYLES: Array<{ value: SummaryStyle; label: string; hint?: string }> = [
  {
    value: "formal",
    label: "Formal",
    hint: "Documentation, offices, people in authority",
  },
  {
    value: "everyday",
    label: "Gen Z / Casual",
    hint: "Everyday speech or light Gen Z tone",
  },
  {
    value: "bard",
    label: "Bard / Herald",
    hint: "Rhythmic, proclamation-style summary",
  },
];

const ALLOWED_EXTENSIONS = [".pdf", ".docx", ".txt"];
const MAX_FILE_SIZE_BYTES = 10 * 1024 * 1024;
const MAX_PASTE_CHARS = 50_000;

export function AnalysisForm() {
  const [pastedText, setPastedText] = useState("");
  const [file, setFile] = useState<File | null>(null);
  const [style, setStyle] = useState<SummaryStyle>("formal");
  const [result, setResult] = useState<AnalysisResponse | null>(null);
  const [isLoading, setIsLoading] = useState(false);
  const [error, setError] = useState<string | null>(null);

  const fileHint = useMemo(
    () => "Optional file: PDF, DOCX, TXT (max 10MB). Pasted text takes priority if both are provided.",
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

  function handlePasteChange(event: React.ChangeEvent<HTMLTextAreaElement>) {
    setPastedText(event.target.value);
    setResult(null);
    setError(null);
  }

  async function handleSubmit(event: React.FormEvent<HTMLFormElement>) {
    event.preventDefault();
    setError(null);
    setResult(null);

    const trimmedPaste = pastedText.trim();
    if (trimmedPaste.length > MAX_PASTE_CHARS) {
      setError(`Pasted text is too long. Maximum ${MAX_PASTE_CHARS.toLocaleString()} characters.`);
      return;
    }

    if (!trimmedPaste && !file) {
      setError("Paste text above or choose a file to analyze.");
      return;
    }

    setIsLoading(true);
    try {
      const response = await analyzeDocument(
        { text: trimmedPaste || undefined, file: trimmedPaste ? null : file },
        style,
      );
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

  const canSubmit =
    (pastedText.trim().length > 0 || file !== null) && !isLoading;

  return (
    <div className="grid gap-6 lg:grid-cols-2">
      <section className="rounded-2xl border border-slate-200 bg-white p-6 shadow-sm">
        <h2 className="text-xl font-semibold text-slate-900">Analyze text or file</h2>
        <p className="mt-1 text-sm text-slate-600">{fileHint}</p>

        <form className="mt-5 space-y-5" onSubmit={handleSubmit}>
          <div>
            <label
              className="mb-2 block text-sm font-medium text-slate-700"
              htmlFor="pasted-text"
            >
              Paste document text
            </label>
            <textarea
              id="pasted-text"
              rows={6}
              value={pastedText}
              onChange={handlePasteChange}
              placeholder="Paste the document content here (optional if you upload a file instead)…"
              className="block w-full resize-y rounded-lg border border-slate-300 bg-white p-3 text-sm text-slate-900 placeholder:text-slate-400 focus:border-slate-500 focus:outline-none focus:ring-1 focus:ring-slate-500"
            />
          </div>

          <div>
            <label
              className="mb-2 block text-sm font-medium text-slate-700"
              htmlFor="file-upload"
            >
              Or upload a file
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
              Summary style
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
                  {option.hint ? ` — ${option.hint}` : ""}
                </option>
              ))}
            </select>
          </div>

          <button
            type="submit"
            disabled={!canSubmit}
            className="inline-flex w-full items-center justify-center rounded-lg bg-slate-900 px-4 py-2.5 text-sm font-medium text-white transition hover:bg-slate-700 disabled:cursor-not-allowed disabled:bg-slate-400"
          >
            {isLoading ? "Analyzing..." : "Analyze"}
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
            Paste text or upload a file, choose a style, then run analysis.
          </p>
        </section>
      )}
    </div>
  );
}
