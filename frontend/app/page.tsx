import { AnalysisForm } from "@/components/AnalysisForm";

export default function Home() {
  return (
    <div className="min-h-screen bg-slate-100">
      <main className="mx-auto max-w-5xl px-6 py-12">
        <header className="mb-8">
          <h1 className="mt-2 text-3xl font-bold tracking-tight text-slate-900">
            ParseCraft
          </h1>
          <p className="mt-3 max-w-3xl text-slate-600">
            Paste text or upload a PDF, DOCX, or TXT file to detect document tone
            and get a summary in your chosen style: formal authority voice,
            everyday / Gen Z casual, or a bard-herald proclamation.
          </p>
        </header>
        <AnalysisForm />
      </main>
    </div>
  );
}
