# ParseCraft Deployment Notes

This project is deployed as a split frontend/backend architecture.

## Current Production Deployment

### Frontend
- Hosting: **AWS S3 + CloudFront**
- URL: https://d1qzyiqerket0h.cloudfront.net

### Backend
- Hosting: **AWS App Runner**
- URL: https://jjttw5tiex.us-east-1.awsapprunner.com

## Health Endpoints

- Frontend: `GET /health`
- Backend: `GET /api/v1/health`

## Deployment Configuration Considerations

### Frontend
- Must be configured with backend base URL using:
  - `NEXT_PUBLIC_API_BASE_URL`
- Build output should include route handlers (e.g., `/health`).

### Backend
- Must be configured with:
  - AI provider settings (`AI_PROVIDER`, `AI_BASE_URL`, `AI_MODEL`, etc.)
  - `AI_API_KEY`
  - CORS origins (`APP_CORS_ALLOWED_ORIGINS`) including production CloudFront domain

## Environment Variables Used in Deployment

### Frontend
- `NEXT_PUBLIC_API_BASE_URL`

### Backend
- `PORT`
- `APP_CORS_ALLOWED_ORIGINS`
- `APP_MAX_FILE_SIZE`
- `APP_MAX_REQUEST_SIZE`
- `APP_MAX_CHARS_FOR_AI`
- `AI_PROVIDER`
- `AI_BASE_URL`
- `AI_API_KEY`
- `AI_MODEL`
- `AI_FALLBACK_MODELS`
- `AI_TEMPERATURE`
- `AI_MAX_TOKENS`

## Docker Deployment (Local/Preview)

Use Docker Compose for local parity:

```bash
docker compose up --build
```

- Frontend: `http://localhost:3001`
- Backend: `http://localhost:8080`

## Suggested Next Improvements

- Add CI/CD pipeline (lint, tests, build, image scan, deploy).
- Add runtime metrics and alerting for backend health and latency.
- Add structured deployment versioning and changelog automation.
