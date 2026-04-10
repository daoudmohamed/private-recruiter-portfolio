# AGENTS

## Purpose

This repository contains a Spring Boot + Kotlin backend for a RAG assistant and a React/Vite frontend.
Any agent working in this codebase must optimize for maintainability first, then delivery speed.

The backend is the priority system. Changes must preserve:

- clear package boundaries
- explicit configuration
- predictable error handling
- testable business logic
- operational simplicity

## Architecture Rules

### Backend layers

Keep these responsibilities stable:

- `api/*`: HTTP concerns only
- `application/*`: orchestration and use cases
- `domain/*`: domain models and pure business rules
- `ai/*`: LLM, retrieval, memory, ingestion, prompt infrastructure
- `config/*`: Spring wiring and external system configuration

Do not move infrastructure logic into controllers.
Do not put HTTP request/response concerns into domain models.

### Controllers

Controllers must stay thin.
They should:

- validate and translate input
- call a single application/service entry point
- map outputs to DTOs
- avoid business branching

Avoid:

- `.block()` in controllers
- file parsing logic in controllers
- retry logic in controllers
- direct Redis or Qdrant access in controllers

### Services

Application services orchestrate workflows.
Infrastructure services encapsulate external systems.

Prefer one clear responsibility per service:

- `ChatService`: chat orchestration
- `SessionService`: session lifecycle
- `DocumentIngestionService`: document ingestion mechanics
- `FolderScannerService`: scanning workflow

If a service starts mixing orchestration, persistence, validation, and formatting, split it.

## Spring + Kotlin Conventions

### Configuration

Prefer typed configuration over scattered `@Value`.

Recommended direction:

- use `@ConfigurationProperties` for grouped settings
- keep environment-specific overrides in `application-*.yml`
- fail fast when a required secret or endpoint is missing in non-dev environments

Avoid duplicated property namespaces.
Do not introduce new config keys without updating all profiles and docs.

### Dependency injection

Use constructor injection only.
Do not use field injection.

If constructor annotations are needed in Kotlin, prefer explicit targets such as `@param:Value`.

### Nullability

Lean on Kotlin null-safety.

- avoid nullable types unless absence is valid
- never propagate platform nullability blindly
- convert external nullable values at the boundary

### Error handling

Use explicit exceptions for invalid requests and infrastructure failures.
Centralize HTTP error shaping in `GlobalExceptionHandler`.

Prefer:

- `ResponseStatusException` for request-level problems
- domain-specific exceptions for business errors
- clear logs with context but without leaking secrets

### Logging

Use structured, actionable logs.

Good logs answer:

- what operation failed
- which resource/session/source was involved
- whether the failure is retryable

Do not log API keys, tokens, prompt contents with sensitive data, or full document payloads.

## WebFlux and Coroutines Rules

The project currently uses WebFlux with Kotlin coroutines.
That means blocking calls on the request path must be treated as defects.

Prefer:

- `suspend` functions for request handling
- coroutine-friendly adapters
- isolating unavoidable blocking I/O behind `Dispatchers.IO`

Avoid:

- `runBlocking` in normal runtime flow
- `.block()` in controllers/services
- mixing reactive and coroutine styles in the same method unless necessary

If a dependency is fundamentally blocking, isolate it in a dedicated infrastructure service and document why.

## RAG and AI Rules

### Retrieval

Retrieval must be deterministic and inspectable.

- keep source metadata on each chunk
- make ingestion idempotent
- avoid duplicate embeddings for the same unchanged source
- keep retrieval thresholds and top-k configurable

### Prompts

Treat prompt templates as versioned application assets.

- keep prompts in `src/main/resources/prompts`
- inject retrieved context explicitly
- do not hardcode large prompt strings in Kotlin classes

### Model wiring

Model provider choice must be consistent with configuration.
Do not wire one provider in code and another in YAML.

### Memory

Conversation memory must have a single source of truth.
Do not store the same history twice through two different mechanisms unless there is a documented reason.

## API Design Rules

- keep DTOs separate from domain models
- validate request payloads at the edge
- use stable JSON response shapes
- avoid exposing endpoints marked "not implemented"
- prefer returning useful status summaries for background-like operations such as document scans

When an endpoint is partial, either complete it or hide it.

## Testing Policy

Every non-trivial backend change should move the project toward test coverage.

Minimum expectation for new backend behavior:

- unit tests for pure logic
- integration tests for controller-to-service flow when behavior changes
- focused tests for ingestion idempotency, session lifecycle, and security decisions

Priority test areas for this repository:

- chat request validation
- session existence and closure behavior
- document ingestion and re-ingestion
- delete-by-source behavior
- API key enforcement by profile

`test NO-SOURCE` is not an acceptable steady state.

## Maintainability Guidelines

Before adding code, prefer:

1. extending an existing coherent abstraction
2. extracting a focused collaborator
3. introducing a new component only if the responsibility is truly new

When changing code:

- remove dead code and stale comments
- keep docs aligned with the implementation
- avoid placeholder endpoints or misleading names
- prefer explicit names over short clever ones

## Build Warning Policy

Treat warnings in three categories:

- local code warnings: fix them in the same change whenever reasonable
- framework integration warnings: isolate, document, and avoid spreading them
- dependency/runtime warnings: document them once and do not churn code trying to suppress them blindly

Current accepted external warnings for this repository:

- JDK 25 restricted/native access warnings emitted by Gradle and Netty
- `sun.misc.Unsafe` warnings emitted by protobuf, gRPC, or Netty dependencies
- JVM class data sharing warnings during tests

These are currently considered stack-level noise, not application defects.
Do not add risky workarounds in application code just to silence them.

If a warning starts coming from repository code instead of dependencies:

- fix it if the change is low risk
- otherwise localize the suppression and document why

## Frontend Rules

The frontend is a client of the backend API.
Keep it simple and resilient.

- use a feature-first structure by default:
  - `frontend/src/app/*` for bootstrapping, global shell, and app-wide hooks
  - `frontend/src/features/*` for business capabilities and feature orchestration
  - `frontend/src/shared/*` for reusable UI primitives and stable cross-feature utilities
- parse streaming responses defensively
- keep API access centralized in `frontend/src/utils/api.ts`
- keep top-level app components thin; do not let `App.tsx` become a dumping ground for orchestration
- put feature orchestration in dedicated hooks or feature components, not in presentation-only sections
- keep reusable sections and UI shell elements separate from recruiter-access and chat workflows
- keep browser persistence limited and intentional
- do not expose broken links or placeholder UX

Prefer:

- small presentational components with explicit props
- one clear orchestration hook per feature when state and side effects grow
- extracting shared UI only after a second real reuse appears

Avoid:

- flat `components/` and `utils/` growth without ownership boundaries
- mixing recruiter access, chat streaming, branding, and portfolio content in one component
- hiding backend calls inside arbitrary UI leaf components

## Expected Output for Future Agents

When making substantial changes:

- update code
- update tests if behavior changed
- update docs if conventions or runtime behavior changed
- mention any remaining risk explicitly

For deeper backend conventions, see:

- `docs/spring-kotlin-maintainability.md`
