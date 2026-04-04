# MVP Baseline

## 1. MVP Objective

The MVP exists to prove one clear value:

Enable a recruiter to access a private interactive CV, understand Mohamed Daoud's profile quickly, ask a few factual questions, and contact him easily.

This MVP is not a commercial SaaS launch.
It is a stable, deployable private recruiter portfolio.

## 2. In-Scope MVP

### A. Private Recruiter Access

The MVP includes:

- email-based invitation request
- magic-link invitation delivery
- temporary recruiter browser session
- clear handling of invalid, expired, and already-used links

### B. Recruiter-First Private Homepage

The MVP includes:

- clear positioning hero
- recruiter summary
- target roles
- "what I can take on quickly"
- recruiter-friendly proof blocks
- visible contact calls to action

### C. Structured Profile Content

The MVP includes:

- skills / stack section
- recruiter-scannable experience section
- recruiter brief source of truth
- wording aligned with backend/lead profile positioning

### D. AI Chat

The MVP includes:

- short factual recruiter-oriented answers
- grounded responses based on indexed content
- suggested recruiter questions
- support for context-aware short follow-up prompts

The chat remains a supporting layer, not the main product surface.

### E. Deployability Baseline

The MVP includes:

- documented runtime configuration
- aligned Docker assets
- aligned Kubernetes manifests
- deployment runbook
- release checklist
- environment/secrets matrix

## 3. Out of Scope for MVP

The MVP explicitly excludes:

- admin backoffice UI
- recruiter CRM
- advanced analytics or dashboards
- recruiter-specific personalization flows
- public portfolio mode
- advanced ranking or recommendation systems
- AI experimentation beyond concise factual profile Q&A
- non-essential frontend redesign work

## 4. MVP Deliverables

The MVP deliverables are:

1. stable private-access flow
2. recruiter-first frontend experience
3. usable grounded chat
4. curated recruiter brief source
5. aligned deployment assets
6. deployment and release documentation

## 5. MVP Definition of Done

The MVP is considered complete when:

1. a recruiter can request access and enter the site
2. the homepage is useful without using chat
3. the chat answers simple targeted questions correctly and concisely
4. the recruiter can contact Mohamed easily
5. deployment prerequisites are documented and realistic
6. the runtime path is stable enough to validate in staging

## 6. MVP Acceptance Criteria

### Functional

- access request works
- invitation email can be delivered
- invitation link can be consumed
- private session behaves correctly
- homepage communicates the profile quickly
- chat is grounded and concise
- contact path is obvious

### Technical

- backend configuration is typed and validated
- critical backend tests pass
- frontend build passes
- Docker/Kubernetes assets reflect the real runtime
- secrets and environment variables are documented

### Product

- the experience feels like a premium private CV
- the experience does not feel like a generic chatbot or SaaS
- the recruiter can decide quickly whether to continue the conversation

## 7. Immediate Post-MVP Priorities

Once the MVP is accepted, the next priorities become:

- staging validation
- release-readiness audit
- content quality improvements for RAG
- operational smoke automation
