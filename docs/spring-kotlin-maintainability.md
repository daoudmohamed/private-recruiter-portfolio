# Spring Kotlin Maintainability Guide

## Goal

This guide defines the target engineering standard for the backend.
It is intentionally opinionated toward long-term maintainability over short-term convenience.

## 1. Package and Responsibility Design

### Preferred structure

- `api.controller`: REST endpoints only
- `api.filter` and `api.handler`: cross-cutting HTTP concerns
- `application.service`: use-case orchestration
- `application.dto`: transport DTOs
- `domain.model`: business concepts
- `ai.*`: AI and RAG infrastructure
- `config`: Spring bootstrapping and external systems

### Rules

- domain classes must not depend on Spring
- application services may depend on domain and infrastructure abstractions
- infrastructure code may depend on Spring and external SDKs
- controllers should not know Redis, Qdrant, or prompt details

## 2. Kotlin Coding Practices

### Favor immutability

Prefer `val` over `var`.
Use data classes for immutable transport and domain state when appropriate.

### Keep functions small

A function should usually do one thing:

- validate
- transform
- orchestrate
- persist
- map

If one method does several of these, split it.

### Be explicit with names

Prefer names like:

- `scanFolder`
- `deleteBySource`
- `updateActivity`

Avoid vague names like:

- `process`
- `handle`
- `execute`

unless the surrounding type makes the responsibility obvious.

### Null handling

Handle nullable values at the boundary.
Do not spread `String?` or `Map<String, Any>?` through the codebase unless semantically justified.

## 3. Spring Practices

### Prefer typed properties

Bad:

```kotlin
@Value("\${knowledgebase.rag.top-k:5}")
private val topK: Int = 5
```

Better for grouped config:

```kotlin
@ConfigurationProperties("knowledgebase.rag")
data class RagProperties(
    val topK: Int = 5,
    val similarityThreshold: Double = 0.65
)
```

Use `@Value` only for isolated one-off values.

### Keep beans focused

Configuration classes should wire beans, not contain business logic.
If bean initialization becomes operationally complex, move the logic to a dedicated service.

### Avoid framework leakage

Do not let framework classes dominate business signatures.

Prefer:

```kotlin
suspend fun ingestFile(filename: String, content: Resource): IngestResult
```

over signatures polluted by HTTP layer concerns.

## 4. WebFlux and Coroutines

### Recommended style

Use coroutines as the primary programming model in business code.

- `suspend fun` for request handlers and services
- `Flow<T>` for streaming where needed

### Blocking guidance

Blocking calls are acceptable only when isolated and explicit.

Good:

```kotlin
withContext(Dispatchers.IO) {
    vectorStore.add(chunks)
}
```

Bad:

```kotlin
filePart.content().block()
```

### Rule of thumb

If a call can stall a request thread, isolate it.
If it appears on the hot path and cannot be isolated cleanly, reconsider the integration approach.

## 5. API and DTO Practices

### DTOs

Use DTOs to model API contracts.
Do not expose internal domain classes directly if the contract may evolve differently.

### Validation

Validate at the edge:

- blank messages
- invalid file types
- missing session IDs
- malformed request payloads

Prefer validation close to input, with domain checks in services.

### Error model

Keep a consistent error response shape.
The current `GlobalExceptionHandler` is the right place to centralize this behavior.

## 6. Persistence and External Systems

### Redis

Use Redis for:

- session state
- chat memory
- ingestion metadata

Do not scatter key naming across the codebase without constants.
Every Redis key family should have:

- a stable prefix
- a clear owner component
- a documented lifecycle

### Qdrant

Each vector point must preserve enough metadata to support operations later:

- `source`
- `type`
- ingestion timestamp

If future delete/list/filter operations are expected, the metadata model must be planned before ingesting at scale.

## 7. RAG Ingestion Design

### Mandatory properties

Ingestion must be:

- idempotent
- traceable
- reversible by source

### Expected workflow

1. detect new or modified file
2. remove old vectors for this source if needed
3. split and ingest new chunks
4. record source metadata and fingerprint
5. expose status via API

### Anti-patterns

- re-embedding unchanged files
- duplicate chunks for the same source version
- exposing delete/list endpoints without real backend support

## 8. Testing Strategy

### Unit tests

Use for:

- mapping logic
- prompt context formatting
- session state transitions
- file validation rules

### Integration tests

Use for:

- controller + service behavior
- Redis-backed session handling
- ingestion lifecycle
- security filters

### Suggested first test suite

- `ChatServiceTest`
- `SessionControllerIntegrationTest`
- `DocumentIngestionServiceTest`
- `DocumentIngestionControllerIntegrationTest`
- `ApiKeyFilterTest`

## 9. Documentation Discipline

Documentation must evolve with the code.

Update docs when:

- an endpoint changes behavior
- configuration keys change
- a provider or runtime assumption changes
- a convention becomes mandatory

Delete stale documentation quickly.
Outdated docs are worse than missing docs.

## 10. Build Warning Triage

Warnings must be triaged, not ignored wholesale.

### Fix immediately

Fix warnings that come from repository code when they indicate:

- deprecated Spring or Kotlin APIs in business or configuration code
- ambiguous annotation targets in Kotlin constructor injection
- dead beans or unused infrastructure wiring
- runtime blocking on request paths

### Isolate and document

When a warning cannot be removed cleanly because it comes from a framework seam, prefer:

- one small adapter class
- one local suppression at file or method level
- a short comment explaining why the warning remains

Avoid broad global suppression flags in Gradle just to get a quiet build.

### Currently accepted stack warnings

At the time of this guide, the repository still emits some warnings during tests that are considered external to application code:

- JDK 25 native-access warnings from Gradle or Netty
- `sun.misc.Unsafe` warnings from protobuf, gRPC, or Netty artifacts
- class data sharing warnings from the test JVM

These should be tracked as dependency/runtime noise, not "fixed" in application code.

### Escalate when they change

Re-evaluate accepted warnings when:

- the project upgrades Spring Boot, Netty, gRPC, protobuf, or Java
- a warning moves from dependency output into repository code
- a previously compile-time warning becomes a runtime startup risk

## 11. Refactoring Priorities for This Repository

Recommended next steps:

1. introduce typed configuration properties for `knowledgebase.*`
2. add backend tests for sessions, ingestion, and API key behavior
3. remove remaining blocking patterns on the request path
4. clarify the single source of truth for chat memory
5. separate AI provider wiring from environment-specific model choices
