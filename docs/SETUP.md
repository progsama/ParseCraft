# ParseCraft Setup Guide

This document explains how to run ParseCraft locally with either direct local processes or Docker.

## Prerequisites

- Java 17+
- Maven 3.9+
- Node.js 20+
- npm
- Docker Desktop (optional, for containerized setup)
- AI provider key (Gemini or OpenAI-compatible)

## 1) Clone and enter the project

```bash
git clone <your-repo-url>
cd "AI Project"
```

## 2) Backend setup

```bash
cd backend
cp .env.example .env
```

Edit `backend/.env` and set:
- `AI_API_KEY` (required)
- `AI_PROVIDER`, `AI_MODEL`, `AI_BASE_URL` as needed
- `APP_CORS_ALLOWED_ORIGINS` to include your frontend origin

Run backend:

```bash
mvn clean test
mvn spring-boot:run
```

Backend endpoints:
- API base: `http://localhost:8080`
- Health: `http://localhost:8080/api/v1/health`
- Swagger UI: `http://localhost:8080/swagger-ui.html`

## 3) Frontend setup

```bash
cd ../frontend
npm ci
```

Optional but recommended: create `frontend/.env.local`:

```env
NEXT_PUBLIC_API_BASE_URL=http://localhost:8080
```

Run frontend:

```bash
npm run dev
```

Frontend URLs:
- App: `http://localhost:3000`
- Health: `http://localhost:3000/health`

## 4) Docker setup (alternative)

From repo root:

```bash
docker compose up --build
```

Docker URLs:
- Frontend: `http://localhost:3001`
- Backend: `http://localhost:8080`

Stop containers:

```bash
docker compose down
```

## Troubleshooting

- **CORS errors:** ensure `APP_CORS_ALLOWED_ORIGINS` includes your frontend URL (`http://localhost:3000` or `http://localhost:3001`).
- **AI errors (401/403/429/502):** verify API key, quota, provider URL, and model names.
- **Java class version/test issues:** run `mvn clean test` (not only `mvn test`).
- **Frontend cannot reach backend:** confirm `NEXT_PUBLIC_API_BASE_URL` points to `http://localhost:8080`.
