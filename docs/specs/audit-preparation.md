# Audit Preparation

## 1. Purpose

This document defines how to audit the codebase against the agreed MVP baseline.

The goal is not a generic code review.
The goal is to answer:

- does the current code implement the MVP scope?
- what is still missing for deployable stability?
- what defects or gaps block a release candidate?

## 2. Audit Axes

### A. Functional Coverage

Questions:

- does the recruiter access flow fully match the MVP?
- is the homepage aligned with the recruiter-first scope?
- is the chat behavior aligned with the MVP contract?
- are contact paths always reachable?

Primary files to inspect:

- frontend `App.tsx`
- `RecruiterAccessController.kt`
- `RecruiterAccessSupport.kt`
- `ChatController.kt`
- `ChatService.kt`

### B. Technical Coverage

Questions:

- are configuration and runtime assumptions explicit?
- are Docker and Kubernetes assets aligned?
- are provider integrations consistent with the spec?
- are obsolete references removed?

Primary files to inspect:

- `application*.yml`
- `docker/*`
- `k8s/*`
- `AiConfig.kt`
- config properties classes

### C. Quality and Stability

Questions:

- are critical paths covered by tests?
- do error paths degrade safely?
- are logs safe and actionable?
- are access/session/invitation flows safe?

Primary files to inspect:

- `src/test/kotlin/...`
- security filters
- exception handler
- recruiter access tests
- chat tests

### D. Product Readiness

Questions:

- is the recruiter brief source usable as a grounded input?
- are the visible frontend texts aligned with the MVP?
- is the deployment documentation sufficient to run staging?

Primary files to inspect:

- `docs/specs/*`
- frontend sections
- `documents/*`

## 3. Audit Output Format

The final audit should be structured as:

1. MVP coverage status
2. release blockers
3. medium risks
4. non-blocking improvements
5. release recommendation

## 4. Release Blocker Criteria

A finding is a release blocker if it breaks one of:

- recruiter access end to end
- grounded chat path
- contact conversion path
- deployability baseline
- configuration/runtime consistency

## 5. Suggested Audit Sequence

1. verify MVP baseline document
2. inspect functional flows
3. inspect runtime/deployment assets
4. inspect test coverage on critical flows
5. inspect docs and operational readiness
6. issue final release-readiness assessment
