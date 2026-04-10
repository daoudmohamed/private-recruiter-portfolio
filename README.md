# Private Recruiter Portfolio

Private recruiter-facing portfolio with an AI-assisted chat, backed by Spring Boot/Kotlin and a React/Vite frontend.

The product is designed to:

- gate access to recruiter-only content
- deliver a concise, interactive portfolio experience
- answer profile questions through a retrieval-augmented chat
- stay maintainable and operationally simple

## Stack

### Backend

- Java 25
- Kotlin
- Spring Boot 4
- Spring WebFlux + coroutines
- Spring AI
- Redis
- Qdrant

### Frontend

- React 19
- Vite
- TypeScript
- Tailwind CSS

### Infra

- Docker / Docker Compose
- Helm
- Kubernetes / K3s
- GitHub Actions
- GHCR
- Tailscale

## Repository Layout

```text
src/main/kotlin/com/knowledgebase/
  api/            HTTP controllers, filters, DTO mapping
  application/    use cases and orchestration
  domain/         domain models and pure business rules
  ai/             chat, retrieval, ingestion, prompt infrastructure
  config/         Spring configuration and external wiring

frontend/src/
  app/            app shell, bootstrapping, app-wide hooks
  features/       business capabilities
  shared/         stable UI primitives and reusable utilities
  utils/          centralized API client and low-level helpers

helm/private-recruiter-portfolio/
  production Helm chart

k8s/services/production-k3s/
  Redis and Qdrant manifests for production K3s

docs/
  technical, deployment, security, and operational documentation
```

## Main Functional Areas

### Recruiter Access

- request invitation
- consume temporary access link
- maintain recruiter session
- optional reCAPTCHA v3 protection

### Recruiter Portfolio

- recruiter-focused landing page
- structured summary of experience and positioning
- contact entry points

### AI Chat

- session-aware chat
- SSE streaming responses
- retrieval over local profile documents

### Document Ingestion

- scans local documents
- chunks and embeds content
- stores vectors in Qdrant
- keeps ingestion idempotent by source

## Local Development

### Prerequisites

- Java 25
- Node.js 20+
- npm
- Docker / Docker Compose

### 1. Start infrastructure

Use the repo Docker assets to run the backend dependencies locally.

If you want the published image locally:

```bash
APP_IMAGE='ghcr.io/daoudmohamed/private-recruiter-portfolio:main' \
docker compose -f docker/docker-compose.ghcr.yml up -d
```

For regular local development, use the compose stack and local backend/frontend processes.

### 2. Run the backend

```bash
./gradlew bootRun
```

The backend uses typed configuration from:

- `src/main/resources/application.yml`
- `src/main/resources/application-prod.yml`

### 3. Run the frontend

```bash
cd frontend
npm install
npm run dev
```

## Testing

### Backend

```bash
./gradlew test
```

### Frontend

```bash
cd frontend
npm test
```

Coverage:

```bash
cd frontend
npm run test:coverage
```

### Frontend build

```bash
cd frontend
npm run build
```

## Configuration

Important runtime integrations include:

- `OPENAI_API_KEY`
- `RECRUITER_ACCESS_TOKEN_SECRET`
- `ADMIN_API_KEY`
- `BREVO_API_KEY`
- `RECRUITER_ACCESS_CAPTCHA_SITE_KEY`
- `RECRUITER_ACCESS_CAPTCHA_RECAPTCHA_SECRET_KEY`
- Redis host / password
- Qdrant host / API key

Production configuration is intentionally strict and should fail fast when required values are missing.

See:

- [Environment Secrets](./docs/specs/environment-secrets.md)
- [Technical Specification](./docs/specs/technical-spec.md)

## Deployment

Production deploy is driven by GitHub Actions plus Helm.

Current production model:

- image published to GHCR
- manual `Deploy` workflow
- Tailscale connection from GitHub Actions to the K3s cluster
- Helm deploy to `mutuelle-production`
- Redis and Qdrant provisioned from the repo

Key docs:

- [GitHub Actions CI/CD](./docs/specs/github-actions-cicd.md)
- [Deployment Runbook](./docs/specs/deployment-runbook.md)
- [Helm Production Guide](./docs/specs/helm-production-guide.md)
- [SRE Operations Guide](./docs/specs/sre-operations-guide.md)

## Maintainability Rules

This repository is opinionated about code structure.

Highlights:

- controllers stay thin
- typed Spring configuration over scattered `@Value`
- backend business flow lives in services, not controllers
- frontend follows a feature-first structure
- API access stays centralized in `frontend/src/utils/api.ts`

Contributor and agent guidance:

- [AGENTS.md](./AGENTS.md)
- [Spring/Kotlin Maintainability Guide](./docs/spring-kotlin-maintainability.md)

## Notes

- JDK 25 / Netty restricted-access warnings are currently accepted stack noise
- the frontend build may warn about a chunk slightly above 500 kB; this is currently non-blocking

