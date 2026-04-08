# ParseCraft

AI-powered document analysis tool that extracts tone and generates style-aware summaries from pasted text or uploaded documents.

## Live Demo

**Try ParseCraft:** [https://d1qzyiqerket0h.cloudfront.net](https://d1qzyiqerket0h.cloudfront.net)

- Frontend (CloudFront): [https://d1qzyiqerket0h.cloudfront.net](https://d1qzyiqerket0h.cloudfront.net)
- Backend (AWS App Runner): [https://jjttw5tiex.us-east-1.awsapprunner.com](https://jjttw5tiex.us-east-1.awsapprunner.com)

---

## Product Overview

ParseCraft is a full-stack application for fast document understanding. Users can paste text or upload a PDF/DOCX/TXT file, choose a summary style, and receive:

- detected document tone,
- a short tone explanation,
- a rewritten summary in the selected style.

The backend orchestrates LLM responses with structured JSON parsing, fallback handling, and style-specific prompt guidance.
It is designed as a stateless API service, which keeps each request self-contained and simplifies cloud deployment and scaling.

---

## Engineering Decisions (Practical)

- **Stateless backend design:** no database in v1, so each request is independent and easier to run in containers/cloud services.
- **Provider-agnostic AI integration:** backend supports configurable AI providers and fallback models via environment variables.
- **Structured response handling:** AI output is sanitized/parsing-validated before returning results to clients.
- **Input safety and UX constraints:** file extension/size checks, text-length limits, and clear error paths for bad input.
- **Production-ready API basics:** health endpoint, OpenAPI docs, CORS configuration, and tested service/integration flows.

---

## Features

- Paste text or upload a file for analysis.
- Pasted text takes priority when both text and file are provided.
- Supported summary styles:
  - `formal`
  - `everyday` (Gen Z / casual)
  - `bard` (herald-style prose)
- Tone detection + explanation.
- Style-aware summary generation.
- Health endpoints:
  - Backend: `GET /api/v1/health`
  - Frontend: `GET /health`
- Swagger/OpenAPI docs for backend API.

### Summarization Workflow

1. Frontend sends multipart request with `style` plus either `text` or `file`.
2. Backend extracts/prepares document content (text upload or parsed file content).
3. AI orchestration generates structured output (`tone`, `tone_explanation`, `summary`) with retry/fallback behavior.
4. API returns normalized analysis response to frontend for display.

---

## Tech Stack

### Frontend

- Next.js 16 (App Router)
- React 19 + TypeScript
- Tailwind CSS 4

### Backend

- Java 17
- Spring Boot 3.3
- Spring Validation
- springdoc-openapi (Swagger UI)
- Apache PDFBox + Apache POI for document parsing

### AI Integration

- Configurable provider: Gemini or OpenAI-compatible APIs
- JSON response sanitization and parsing
- Retry/fallback model orchestration

### Runtime & Hosting

- Frontend: AWS S3 + CloudFront
- Backend: AWS App Runner
- Local containers: Docker Compose

### Deployed Cloud Architecture

- **Frontend delivery:** static web app served globally via CloudFront.
- **Backend hosting:** Spring Boot API deployed as a containerized service on AWS App Runner.
- **Decoupled services:** frontend and backend are independently deployable and connected via configurable API base URL.

---

## Project Structure

```text
AI Project/
├─ backend/
│  ├─ src/main/java/com/portfolio/docanalyzer/
│  │  ├─ client/        # AI provider clients (Gemini/OpenAI)
│  │  ├─ controller/    # API controllers
│  │  ├─ service/       # parsing + orchestration + analysis logic
│  │  ├─ model/         # domain models/enums
│  │  └─ util/          # prompt + JSON sanitization helpers
│  ├─ src/main/resources/application.yml
│  ├─ .env.example
│  ├─ Dockerfile
│  └─ pom.xml
├─ frontend/
│  ├─ app/              # Next.js app routes and pages
│  ├─ components/       # UI components
│  ├─ lib/              # API client helpers
│  ├─ types/            # shared TS types
│  ├─ Dockerfile
│  └─ package.json
├─ docs/
├─ docker-compose.yml
└─ README.md
```

---

## API Summary

### Analyze Document

- **Endpoint:** `POST /api/v1/documents/analyze`
- **Content type:** `multipart/form-data`
- **Fields:**
  - `style` (required): `formal`, `everyday`, `bard`
  - `text` (optional): pasted plain text
  - `file` (optional): `.pdf`, `.docx`, `.txt`
- If both `text` and `file` are sent, `text` is used.

### Health

- **Endpoint:** `GET /api/v1/health`
- **Response:** `{ "status": "ok" }`

### Swagger

- Local Swagger UI: `http://localhost:8080/swagger-ui.html`
- Local OpenAPI JSON: `http://localhost:8080/api-docs`

---

## Supported File Types and Limits

- Supported file extensions: `.pdf`, `.docx`, `.txt`
- Frontend file size validation: **10MB**
- Backend multipart limits (default):
  - `APP_MAX_FILE_SIZE=10MB`
  - `APP_MAX_REQUEST_SIZE=10MB`
- Frontend pasted-text limit: **50,000 characters**
- Backend AI text-prep cap (default): `APP_MAX_CHARS_FOR_AI=15000`

---

## Environment Variables

### Backend (`backend/.env`)

| Variable | Required | Description | Default/Example |
| --- | --- | --- | --- |
| `PORT` | No | Backend port | `8080` |
| `APP_CORS_ALLOWED_ORIGINS` | Yes (for browser access) | Allowed CORS origins | `http://localhost:3000,http://localhost:3001` |
| `APP_MAX_FILE_SIZE` | No | Max uploaded file size | `10MB` |
| `APP_MAX_REQUEST_SIZE` | No | Max multipart request size | `10MB` |
| `APP_MAX_CHARS_FOR_AI` | No | Max characters sent to AI after prep | `15000` |
| `AI_PROVIDER` | Yes | AI provider (`gemini` or `openai`) | `gemini` |
| `AI_BASE_URL` | Yes | Provider base URL | `https://generativelanguage.googleapis.com` |
| `AI_API_KEY` | Yes | Provider API key | _(set your secret)_ |
| `AI_MODEL` | Yes | Primary model name | `gemini-2.5-flash` |
| `AI_FALLBACK_MODELS` | No | Comma-separated fallback models | `gemini-2.0-flash,gemini-flash-lite-latest` |
| `AI_TEMPERATURE` | No | Generation temperature | `0.2` |
| `AI_MAX_TOKENS` | No | Max output tokens | `2048` |

### Frontend (`frontend/.env.local`)

| Variable | Required | Description | Default/Example |
| --- | --- | --- | --- |
| `NEXT_PUBLIC_API_BASE_URL` | Yes (recommended) | Backend base URL used by frontend | `http://localhost:8080` |

---

## Local Development

For detailed setup, see [`docs/SETUP.md`](docs/SETUP.md).

### Quick Start

1. **Backend**
   - `cd backend`
   - copy `.env.example` to `.env`
   - set `AI_API_KEY` and provider/model values
   - run: `mvn clean test` then `mvn spring-boot:run`

2. **Frontend**
   - `cd frontend`
   - `npm ci`
   - set `NEXT_PUBLIC_API_BASE_URL=http://localhost:8080` in `.env.local` (optional but recommended)
   - run: `npm run dev`

3. Open `http://localhost:3000`

---

## Docker

1. Ensure `backend/.env` exists and includes a valid `AI_API_KEY`.
2. From repo root:

```bash
docker compose up --build
```

1. Access:

   - Frontend: `http://localhost:3001`
   - Backend: `http://localhost:8080`
   - Backend health: `http://localhost:8080/api/v1/health`
   - Frontend health: `http://localhost:3001/health`

---

## How to Use

For a fuller walkthrough, see [`docs/USAGE.md`](docs/USAGE.md).

1. Open the web app: [https://d1qzyiqerket0h.cloudfront.net](https://d1qzyiqerket0h.cloudfront.net)
2. Paste document text **or** upload a supported file (`.pdf`, `.docx`, `.txt`).
3. Choose a summary style:
   - Formal
   - Gen Z / Casual
   - Bard / Herald
4. Click **Analyze**.
5. Review:
   - detected tone,
   - tone explanation,
   - generated summary.

---

## Deployment

For deployment details, see [`docs/DEPLOYMENT.md`](docs/DEPLOYMENT.md).

- Frontend is deployed on **AWS S3 + CloudFront**:
  - [https://d1qzyiqerket0h.cloudfront.net](https://d1qzyiqerket0h.cloudfront.net)
- Backend is deployed on **AWS App Runner**:
  - [https://jjttw5tiex.us-east-1.awsapprunner.com](https://jjttw5tiex.us-east-1.awsapprunner.com)

---

## Screenshots

Screenshots can be added in `docs/screenshots/`.

Placeholder entries:

- Main analyze form
- Analysis result panel
- Swagger/API docs page

---

## Resume-Friendly Highlights

- Built and deployed a cloud-hosted full-stack application (**Next.js + Spring Boot**) with separate frontend/backend infrastructure on AWS.
- Implemented robust document input handling for `.pdf`, `.docx`, and `.txt`, plus a paste-first workflow with frontend and backend validation rules.
- Developed a stateless summarization pipeline with structured AI response parsing, retry/fallback behavior, and style-specific prompt control.
- Added API reliability and operability features including health endpoints, OpenAPI documentation, multipart validation, and CORS configuration.
- Containerized both services with Docker and documented local/dev deployment paths for reproducible onboarding.

### Internship-Relevant Takeaways

- Demonstrates ability to ship across the full stack: UI, API, cloud deployment, and developer documentation.
- Shows practical engineering judgment under realistic constraints (v1 scope, stateless design, clear interfaces, tested behavior).
- Reflects production habits valued in internships: environment-based config, health checks, input validation, and maintainable docs.

---

## Future Improvements

- Add authentication and per-user analysis history.
- Add persistence layer for saved documents and results.
- Add async job processing for large files and longer summaries.
- Add richer observability (structured logs, metrics, tracing).
- Expand supported file types and batch processing.
- Add model evaluation metrics and prompt/version tracking.

---

## Contributing

Contributions are welcome.

1. Fork the repository.
2. Create a feature branch.
3. Make focused changes with tests where applicable.
4. Open a pull request with a clear summary and test notes.

Please avoid committing secrets (`.env`, API keys, credentials).

---

## License

This project is licensed under the MIT License. See [LICENSE](LICENSE).
