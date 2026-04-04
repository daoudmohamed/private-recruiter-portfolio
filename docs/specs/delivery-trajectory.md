# Delivery Trajectory

## 1. Goal State

The target state is a private recruiter portfolio that is:

- functionally coherent
- technically stable
- deployable with documented infrastructure
- maintainable by one main owner

## 2. Current Direction

The project already has strong foundations:

- typed configuration
- recruiter access flow
- AI/RAG core
- frontend recruiter-first positioning
- test foundation on critical backend flows

The remaining work is less about inventing features and more about making the system release-grade.

## 3. Phased Trajectory

### Phase 1: Specification Baseline

Goal:

- freeze the target product scope and technical direction

Deliverables:

- functional spec
- technical spec
- non-functional spec
- delivery trajectory

Exit criteria:

- future work can be mapped to an explicit target

### Phase 2: Product Hardening

Goal:

- close feature gaps in the intended MVP

Priority topics:

- finalize recruiter access end-to-end behavior
- finalize Brevo production integration
- improve source documents for RAG quality
- add a concise recruiter brief source of truth
- verify frontend content consistency

Exit criteria:

- product experience is coherent end to end

### Phase 3: Operational Hardening

Goal:

- make the system deployable and predictable

Priority topics:

- review Docker/Kubernetes assets against runtime reality
- document environment variables and secrets
- add staging-oriented configuration guidance
- add smoke-check steps
- verify cookie, CORS, and proxy/TLS behavior

Exit criteria:

- staging deployment is documented and runnable

### Phase 4: Stability Hardening

Goal:

- make the system reliable enough for sustained real usage

Priority topics:

- fill remaining backend test gaps
- add release checklist
- add minimal metrics/log review guidance
- validate error and fallback behavior

Exit criteria:

- system can be considered stable for real recruiter use

## 4. Recommended Workstreams

### Workstream A: Product Content

- curate CV/source documents
- keep static content and AI context aligned
- maintain recruiter-first wording

### Workstream B: Access and Trust

- validate invitation lifecycle
- document privacy behavior
- ensure production-safe email and session behavior

### Workstream C: AI Quality and Cost

- keep prompts concise
- keep retrieval inspectable
- reduce unnecessary calls
- improve answer usefulness rather than length

### Workstream D: Deployment and Ops

- environment documentation
- deployment verification
- smoke checks
- operational runbook

## 5. Suggested Milestones

### Milestone M1: Scoped MVP

Definition:

- specs written
- frontend recruiter-first baseline done
- recruiter access functionally coherent

### Milestone M2: Deployable Staging

Definition:

- documented secrets
- Docker/K8s path aligned
- staging runbook available
- invitation/chat core validated in staging

### Milestone M3: Stable Release Candidate

Definition:

- core tests pass
- fallback behavior validated
- release checklist complete
- production configuration hardened

### Milestone M4: Stable Production Portfolio

Definition:

- real deployment running
- monitored critical flows
- content, access, and AI quality all coherent

## 6. Immediate Next Steps

The most useful next implementation steps are:

1. add an explicit environment/deployment runbook
2. add a release checklist for staging and production
3. review all required secrets and provider settings
4. validate Brevo/OpenAI/Qdrant/Redis runtime assumptions
5. define a recruiter-brief source document for the RAG

## 7. Definition of Finished Product

The product can be considered finished enough for stable deployment when:

- the scope is frozen and documented
- the recruiter journey works end to end
- the AI is useful, factual, and cost-controlled
- the infrastructure path is documented and repeatable
- the system can be operated and debugged without guesswork
