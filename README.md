# AI Document Analyzer

Portfolio-quality full-stack project built to showcase **Java, Spring Boot, REST APIs, file processing, Docker, and AWS-ready deployment design**.

Users upload a document (`PDF`, `DOCX`, or `TXT`) and receive:

- detected tone
- short tone explanation
- summary rewritten in one style: `formal`, `informal`, `casual`, `genz`

---

## Tech Stack

- **Backend:** Java 17, Spring Boot, Spring Validation, OpenAPI/Swagger
- **Document Parsing:** Apache PDFBox, Apache POI (DOCX)
- **AI Integration:** OpenAI-compatible REST client
- **Frontend:** Next.js (App Router), TypeScript, Tailwind CSS
- **Containerization:** Docker, Docker Compose
- **Architecture style:** Stateless, no database for v1

---

## Monorepo Structure

```text
.
├── backend
│   ├── src/main/java/com/portfolio/docanalyzer
│   │   ├── client
│   │   ├── config
│   │   ├── controller
│   │   ├── dto
│   │   ├── exception
│   │   ├── model
│   │   ├── service
│   │   └── util
│   ├── src/main/resources/application.yml
│   └── pom.xml
├── frontend
│   ├── app
│   ├── components
│   ├── lib
│   └── types
└── docker-compose.yml
```

---

## Architecture

### Request Flow

1. Frontend uploads file and selected style to backend REST endpoint.
2. Backend validates request and file type/size.
3. Document parsing service extracts text.
4. Text preparation service trims text with a chunking extension point.
5. AI orchestration service builds prompt and calls AI provider.
6. API returns structured JSON response to frontend.

### Backend Design Choices

- **Layered architecture:** clear `controller -> service -> client` separation.
- **DTO-first API contract:** stable and interview-demo friendly.
- **Enum-based style strategy:** centralized style handling.
- **Global exception handler:** consistent error payloads.
- **Stateless processing:** files are handled per request only, no persistence.
- **Extensibility:** chunking point prepared for large-document v2.

---

## API

### Analyze Document

- **Endpoint:** `POST /api/v1/documents/analyze`
- **Content-Type:** `multipart/form-data`
- **Fields:**
  - `file`: uploaded file (`pdf`, `docx`, `txt`)
  - `style`: one of `formal | informal | casual | genz`

### Success Response

```json
{
  "tone": "Professional and assertive",
  "toneExplanation": "The document uses direct language and structured argumentation.",
  "summary": "This document outlines ...",
  "summaryStyle": "formal"
}
```

### Error Response (example)

```json
{
  "timestamp": "2026-04-06T18:23:21.821Z",
  "status": 400,
  "error": "Bad Request",
  "message": "Unsupported file type. Allowed: pdf, docx, txt.",
  "path": "/api/v1/documents/analyze"
}
```

---

## Local Development

## 1) Backend

```bash
cd backend
cp .env.example .env
# set AI_API_KEY in your environment or .env setup
mvn spring-boot:run
```

- Backend runs on `http://localhost:8080`
- Swagger UI: `http://localhost:8080/swagger-ui.html`

## 2) Frontend

```bash
cd frontend
cp .env.example .env.local
npm install
npm run dev
```

- Frontend runs on `http://localhost:3000`

---

## Testing

### Backend tests

```bash
cd backend
mvn clean test
```

Includes:

- unit tests for summary style normalization/validation
- unit tests for document analysis service validation/flow
- integration test for upload endpoint with mocked AI client

---

## Docker

### Run with Docker Compose

```bash
cp backend/.env.example backend/.env
# edit backend/.env and set AI_API_KEY
docker compose up --build
```

Services:

- Frontend: `http://localhost:3000`
- Backend: `http://localhost:8080`

---

## Environment Variables

### Backend (`backend/.env`)

- `AI_API_KEY` (required)
- `AI_BASE_URL` (default: `https://api.openai.com`)
- `AI_MODEL` (default: `gpt-4o-mini`)
- `AI_TEMPERATURE` (default: `0.2`)
- `AI_MAX_TOKENS` (default: `700`)
- `APP_CORS_ALLOWED_ORIGINS` (default: `http://localhost:3000`)
- `APP_MAX_FILE_SIZE` (default: `10MB`)
- `APP_MAX_REQUEST_SIZE` (default: `10MB`)
- `APP_MAX_CHARS_FOR_AI` (default: `15000`)

### Frontend (`frontend/.env.local`)

- `NEXT_PUBLIC_API_BASE_URL` (default: `http://localhost:8080`)

---

## Portfolio Notes

### Why this project helps screening

- Demonstrates practical Spring Boot REST API design.
- Shows file upload handling + PDF/DOCX/TXT extraction.
- Integrates external AI API with structured JSON contract.
- Uses production-like validation and exception strategy.
- Includes containerization and clear AWS migration path.

### Suggested screenshot section

Add screenshots in a `screenshots/` folder and reference here:

- upload form
- loading state
- successful analysis result
- error state for invalid file type

---

## AWS Deployment Path (Next Phase)

### Option A (recommended for portfolio)

- **Frontend:** Vercel or S3 + CloudFront
- **Backend:** AWS ECS Fargate
- **Container registry:** Amazon ECR
- **Secrets:** AWS Secrets Manager / SSM Parameter Store
- **Observability:** CloudWatch logs + alarms

### Option B (full AWS)

- **Frontend:** S3 + CloudFront
- **Backend:** Elastic Beanstalk (Docker) or ECS Fargate
- **Networking:** ALB + HTTPS (ACM)

### Minimal production hardening checklist

- Add rate limiting
- Add request tracing / correlation IDs
- Add retry + timeout policy for AI client
- Add integration tests with mocked AI provider
- Add CI pipeline for lint/build/test/docker build

---

## Next Enhancements

- True chunking and map-reduce summarization for long documents
- Multi-provider AI client abstraction
- Optional auth + per-user history (with DB) in v2
- Background job processing for very large files
