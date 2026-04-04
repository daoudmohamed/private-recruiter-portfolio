# Technical Specification

## 1. Technical Goal

Build a stable, deployable system that supports a private recruiter portfolio with AI-assisted profile exploration.

The system must optimize for:

- maintainability
- predictable behavior
- operational simplicity
- low-to-moderate operating cost
- safe handling of recruiter access and profile data

## 2. System Context

The system consists of:

- a Spring Boot + Kotlin backend
- a React + Vite frontend
- Redis for session and access state
- Qdrant for vector storage
- OpenAI for chat and embeddings
- Brevo for transactional email

## 3. Target Architecture

### Frontend

Responsibilities:

- access-gated portfolio UI
- recruiter-first content rendering
- session-aware chat UI
- request-invitation / consume-link flows
- defensive SSE parsing

Constraints:

- no business logic duplication from backend
- API access centralized in `frontend/src/utils/api.ts`
- limited browser persistence

### Backend API

Responsibilities:

- access control
- recruiter invitation lifecycle
- session lifecycle
- chat orchestration
- document ingestion and retrieval
- error shaping and logging

Constraints:

- thin controllers
- business flow in services
- typed configuration
- single source of truth for state

### Data/Infra Components

- Redis: recruiter sessions, invitation state, chat memory, ingestion metadata, rate limiting
- Qdrant: indexed document chunks with metadata
- OpenAI: embeddings + chat generation
- Brevo: invitation email delivery

## 4. Main Components

### Recruiter Access Domain

Key components:

- `RecruiterAccessController`
- `RecruiterAccessFilter`
- recruiter invitation/session services in `RecruiterAccessSupport.kt`

Responsibilities:

- request invitation
- create/send invitation
- consume invitation link
- manage authenticated recruiter session
- expose current session state

### Chat Domain

Key components:

- `ChatController`
- `ChatService`
- `RedisChatMemory`
- `SimpleRetriever`
- prompt provider

Responsibilities:

- validate chat request
- retrieve relevant profile context
- produce short grounded answers
- support streaming

### Session Domain

Key components:

- `SessionController`
- `SessionService`

Responsibilities:

- create and close chat sessions
- track activity
- expose history

### RAG Ingestion Domain

Key components:

- `DocumentIngestionController`
- `DocumentIngestionService`
- `FolderScannerService`

Responsibilities:

- ingest files
- ensure source-level idempotency
- expose list/delete by source
- scan configured document directory

## 5. Target Data Contracts

### Recruiter Invitation

Must track:

- invitation ID
- recipient email
- token hash
- status
- created at
- expires at
- consumed at

### Recruiter Session

Must track:

- session ID
- browser/session token reference
- created at
- expires at

### Chat Session

Must track:

- session ID
- created at
- updated at
- message count
- active/closed state

### Vector Metadata

Each chunk should preserve:

- source
- content type
- chunk index
- ingestion timestamp
- optional source fingerprint/version

## 6. API Surface Targets

### Public Access Endpoints

- request invitation
- consume invitation
- get current recruiter session
- logout recruiter session

### Protected Product Endpoints

- chat
- chat sync
- session lifecycle
- portfolio document operations

### Admin/Technical Endpoints

- document ingestion
- manual scans
- operational/admin access endpoints if kept

## 7. Configuration Strategy

Configuration must remain typed and validated.

Groups should remain explicit:

- `knowledgebase.security.*`
- `knowledgebase.recruiter-access.*`
- `knowledgebase.chat.*`
- `knowledgebase.rag.*`
- `knowledgebase.documents.*`
- `redis.*`
- `qdrant.*`

Production startup must fail fast on missing:

- OpenAI API key
- Brevo API key and template id
- recruiter access token secret
- required admin secrets
- required infrastructure hosts

## 8. Deployment Model

### Local Development

- frontend via Vite
- backend via Spring Boot
- Redis/Qdrant via Docker Compose
- permissive local defaults where explicitly allowed

### Staging

- production-like configuration
- real email delivery to controlled recipients
- real OpenAI and Qdrant integration
- restricted secrets

### Production

- containerized deployment
- externally managed secrets
- secure cookies
- production CORS
- monitored Redis and Qdrant

Existing repo assets already suggest:

- Dockerfile
- Docker Compose
- Kubernetes base + overlays

The target is to make those deployment assets authoritative and tested.

## 9. Technical Stability Requirements

The product should not be considered stable until:

- backend tests cover critical flows
- frontend build is reproducible
- configuration validation is strict
- invitation and session flows are verified end to end
- Qdrant and Redis bootstrapping are predictable
- logs are actionable and non-sensitive

## 10. Technical Acceptance Criteria

The technical baseline is acceptable when:

- a new environment can be configured from documented secrets and endpoints
- app startup fails clearly on invalid configuration
- recruiter access works end to end
- chat works with OpenAI and grounded retrieval
- ingestion is idempotent
- core test suite passes
- deployment artifacts reflect the actual runtime
