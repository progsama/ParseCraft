# AI Document Analyzer

Full-stack portfolio project: a **Next.js** web app and **Spring Boot** API that extract text from documents (or accept pasted text), infer **tone**, and generate a **style-aware summary** using OpenAI or Google Gemini.

---

## Table of contents

- [Features](#features)
- [Architecture](#architecture)
- [Tech stack](#tech-stack)
- [API](#api-documentation)
- [Local setup](#local-setup)
- [Docker](#docker)
- [Interactive API docs (Swagger UI)](#interactive-api-docs-swagger-ui)
- [Future: AWS deployment](#future-aws-deployment)
- [Screenshots](#screenshots)
- [Resume-ready bullets](#resume-ready-bullets)
- [Interview talking points](#interview-talking-points)

---

## Features

- **Paste or upload** — Analyze plain text pasted in the UI, or upload **PDF**, **DOCX**, or **TXT**. If both are provided, **pasted text takes priority** (file parsing is skipped).
- **Tone analysis** — Returns a high-level **tone** label and a short **tone explanation** grounded in the content.
- **Three summary styles** — Configurable rewrite of the material:
  - **Formal** — Documentation, office, authority-register language.
  - **Everyday / Gen Z–casual** — Natural, contemporary casual phrasing (aliases like `casual`, `genz` accepted server-side).
  - **Bard / Herald** — Short, rhythmic “proclamation” style while staying factually tied to the source (aliases like `herald` accepted server-side).
- **Pluggable AI** — Switch between **OpenAI** and **Gemini** via environment variables; optional **fallback models** for resilience.
- **Quality gates** — Server-side checks and structured JSON parsing with repair paths when the model output is incomplete.
- **CORS-aware** — Origins configurable for local dev and Docker (e.g. port **3001** for the containerized frontend).
- **Tests** — JUnit/Mockito unit tests and Spring MVC integration tests for the analyze endpoint.

---

## Architecture

```text
┌─────────────────┐     HTTPS (browser)      ┌──────────────────────┐
│  Next.js (React)│ ────────────────────────►  │  Spring Boot REST API │
│  static UI      │   multipart/form-data      │  /api/v1/documents/*  │
└────────┬────────┘                            └──────────┬───────────┘
         │                                               │
         │  NEXT_PUBLIC_API_BASE_URL                     │ PDFBox / POI
         │  (e.g. http://localhost:8080)                 │ (extract text)
         │                                               ▼
         │                                    ┌──────────────────────┐
         │                                    │  AiOrchestration      │
         │                                    │  (prompts, JSON parse, │
         │                                    │   quality gates)      │
         │                                    └──────────┬───────────┘
         │                                               │
         │                                               ▼
         │                                    ┌──────────────────────┐
         └──────────────────────────────────  │  OpenAI or Gemini     │
              (no DB in v1)                   │  (HTTP, stateless)    │
                                              └──────────────────────┘
```

**Design in one paragraph:** The browser talks only to the API. The API is **stateless**: each request carries the document text (from paste or parsed file), style choice, and credentials flow through environment variables—no session store and **no database in v1**. Parsing happens in-process (PDFBox, POI). The orchestration layer builds provider-specific prompts, requests **structured JSON** from the LLM, sanitizes and validates the response, and applies fallbacks when fields are missing or fail quality checks.

---

## Tech stack

| Layer | Choice |
|--------|--------|
| Frontend | Next.js 16, React 19, TypeScript, Tailwind CSS |
| Backend | Java 17, Spring Boot 3.3, Spring Validation |
| Docs | springdoc-openapi (Swagger UI) |
| Documents | Apache PDFBox (PDF), Apache POI (DOCX), plain text |
| AI | OpenAI-compatible or Gemini REST via configurable base URL |

---

## API documentation

### `POST /api/v1/documents/analyze`

**Content-Type:** `multipart/form-data`

| Field | Required | Description |
|--------|----------|-------------|
| `style` | **Yes** | Summary style. Canonical values: `formal`, `everyday`, `bard`. Aliases (e.g. `casual`, `genz` → everyday; `herald` → bard) are normalized server-side. |
| `text` | No* | Plain text to analyze. If present and non-blank after trim, it is used and **any uploaded file is ignored**. |
| `file` | No* | One file: `.pdf`, `.docx`, or `.txt` (per server config). Used when `text` is empty. |

\*Exactly one of `text` or `file` must effectively supply content: either non-empty `text`, or a non-empty file.

**Success — `200 OK`** — JSON body:

```json
{
  "tone": "string",
  "toneExplanation": "string",
  "summary": "string",
  "summaryStyle": "formal | everyday | bard"
}
```

**Error responses** (typical):

| Status | Meaning |
|--------|---------|
| `400` | Missing style, invalid style, or neither usable text nor file |
| `422` | Document could not be parsed |
| `502` | AI provider error or invalid/unusable model response |

**Example (curl)** — pasted text only:

```bash
curl -s -X POST "http://localhost:8080/api/v1/documents/analyze" \
  -F "style=everyday" \
  -F "text=Your document text here."
```

**Example** — file upload:

```bash
curl -s -X POST "http://localhost:8080/api/v1/documents/analyze" \
  -F "style=formal" \
  -F "file=@./sample.pdf"
```

---

## Local setup

### Prerequisites

- **Java 17** and **Maven 3.9+** (backend)
- **Node.js 20+** and npm (frontend)
- An **API key** for your chosen provider (`AI_PROVIDER` = `openai` or `gemini`)

### Backend

```bash
cd backend
cp .env.example .env
# Edit .env: set AI_API_KEY, AI_PROVIDER, AI_MODEL, APP_CORS_ALLOWED_ORIGINS (include http://localhost:3000 for default Next dev port)
mvn clean test
mvn spring-boot:run
```

API base URL defaults to **http://localhost:8080**.

### Frontend

```bash
cd frontend
npm ci
# Optional: create .env.local with NEXT_PUBLIC_API_BASE_URL=http://localhost:8080
npm run dev
```

Open **http://localhost:3000** (default Next.js dev port).

---

## Docker

From the **repository root** (where `docker-compose.yml` lives):

1. Ensure **`backend/.env`** exists (copy from `backend/.env.example`) with at least **`AI_API_KEY`** and **`APP_CORS_ALLOWED_ORIGINS`** including **`http://localhost:3001`** (mapped host port for the frontend container).

2. Build and run:

```bash
docker compose up --build
```

| Service | URL |
|---------|-----|
| Frontend | http://localhost:3001 |
| Backend | http://localhost:8080 |

The compose file sets `NEXT_PUBLIC_API_BASE_URL=http://localhost:8080` so the **browser** calls the API on the host (correct for local Docker). If you use `127.0.0.1` or another origin, add it to **`APP_CORS_ALLOWED_ORIGINS`** and restart the backend.

---

## Interactive API docs (Swagger UI)

With the backend running:

- **Swagger UI:** http://localhost:8080/swagger-ui.html  
- **OpenAPI JSON:** http://localhost:8080/api-docs  

---

## Future: AWS deployment

This section is a **roadmap** (not implemented in-repo yet). It shows how you would credibly evolve the same architecture on AWS.

| Concern | Typical AWS approach |
|---------|----------------------|
| **Frontend** | Build Next.js static/standalone output → **S3** + **CloudFront**, or **Amplify Hosting** for git-based deploys. |
| **Backend** | Container image → **Elastic Container Service (ECS)** on Fargate, or **AWS App Runner** for minimal ops. |
| **Load & TLS** | **Application Load Balancer** + ACM certificate in front of ECS; CloudFront in front of the SPA. |
| **Secrets** | Store `AI_API_KEY` in **Secrets Manager** or **SSM Parameter Store**; inject as env vars in the task definition. |
| **Config** | Non-secret settings (model names, CORS origins, token limits) via **SSM** or task env; **no keys in images**. |
| **Observability** | **CloudWatch** logs/metrics/alarms; optional **X-Ray** for trace IDs on the API. |
| **Future persistence** | When you add users or history, introduce **RDS** (PostgreSQL) or **DynamoDB** behind the same stateless API (JWT or session in Redis if needed). |

**CORS in production:** Set `APP_CORS_ALLOWED_ORIGINS` to your real CloudFront/Amplify HTTPS origin only.

---

## Screenshots

Add your own captures under **`docs/screenshots/`** and replace the placeholders below (or embed directly in your portfolio site).

| # | Suggested content | Suggested filename |
|---|-------------------|-------------------|
| 1 | Landing page with paste area + upload + style dropdown | `docs/screenshots/01-form.png` |
| 2 | Results: tone + explanation + summary | `docs/screenshots/02-result.png` |
| 3 | Swagger UI showing `POST /analyze` | `docs/screenshots/03-swagger.png` |

**Markdown placeholders** (uncomment when files exist):

```markdown
<!-- ![Form](docs/screenshots/01-form.png) -->
<!-- ![Result](docs/screenshots/02-result.png) -->
```

See **`docs/screenshots/README.md`** for a short checklist.

---

## Resume-ready bullets

Use or adapt these (quantify where you can: latency, test count, model names you actually use).

- Designed and implemented a **full-stack AI document analysis** tool: **Spring Boot 3** REST API and **Next.js** SPA with **multipart** upload and **paste-to-analyze** flow; **pasted text prioritized** over file when both are present.
- Integrated **LLM providers** (OpenAI / Gemini) via **configuration-driven** HTTP clients, with **structured JSON** outputs, **sanitization**, and **quality gates** before returning tone and style-aware summaries.
- Parsed **PDF** and **DOCX** server-side (**PDFBox**, **Apache POI**) with validation for file types and size limits; configurable **character caps** for model input.
- Exposed **OpenAPI** documentation via **springdoc** and **Swagger UI**; wrote **JUnit** unit and **Spring MVC** integration tests for critical paths.
- Containerized **frontend and backend** with **Docker Compose** for repeatable local and demo environments; documented **CORS** and environment-based secrets for production readiness.

---

## Interview talking points

**Why Spring Boot?**  
It matches common enterprise stacks, gives a fast path to a **production-style REST API** (validation, exception handling, multipart, dependency injection), and pairs cleanly with **OpenAPI** and tests. For a portfolio piece aimed at backend or full-stack roles, it signals familiarity with tools recruiters already keyword-search for.

**Why no database in v1?**  
The product scope is **stateless analysis**: upload or paste, analyze, return JSON. There are no users, sessions, or saved reports yet. Skipping a database keeps **deployment, security surface, and complexity** lower while the core value—reliable parsing + LLM orchestration—is proven. A database becomes justified when you add **accounts, audit history, or saved analyses**.

**Why stateless?**  
Each request is **self-contained** (document + style + config). That simplifies **horizontal scaling** later (any instance can serve any request), avoids sticky sessions, and aligns with **12-factor** style config (secrets via env). When you add persistence, you can keep the API stateless and push session/user state to **JWT + DB** or a cache without rewriting the core analysis flow.

---

## License

Private portfolio / demonstration — adjust as needed for your situation.
