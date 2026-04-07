import { AnalysisForm } from "@/components/AnalysisForm";

export default function Home() {
  return (
    <div className="min-h-screen bg-slate-100">
      <main className="mx-auto max-w-5xl px-6 py-12">
        <header className="mb-8">
          <p className="text-sm font-medium uppercase tracking-wide text-slate-500">
            Portfolio Project
          </p>
          <h1 className="mt-2 text-3xl font-bold tracking-tight text-slate-900">
            AI Document Analyzer
          </h1>
          <p className="mt-3 max-w-3xl text-slate-600">
            Upload a PDF, DOCX, or TXT file to detect its tone and generate a
            rewritten summary in your selected style.
          </p>
        </header>
        <AnalysisForm />
      </main>
    </div>
  );
}
