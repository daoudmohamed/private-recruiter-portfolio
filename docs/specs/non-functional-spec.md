# Non-Functional Specification

## 1. Objective

Define the quality bar required to call the product deployable and stable.

## 2. Reliability

The system must:

- start cleanly with valid configuration
- fail fast with invalid configuration
- remain functional when optional UX layers fail
- degrade gracefully on chat/provider errors

Targets:

- no silent startup misconfiguration
- no broken access flow due to missing secrets
- no duplicate ingestion behavior for unchanged sources

## 3. Security

The system must:

- protect the portfolio behind recruiter access outside dev/test
- use time-limited invitation links
- use server-validated recruiter sessions
- use `HttpOnly` cookies
- use `Secure` cookies in production
- avoid logging secrets, full tokens, and full email addresses

The system should:

- keep CORS explicit by environment
- rate-limit invitation requests
- rate-limit abuse-prone public endpoints
- support clear secret rotation procedures

## 4. Privacy and Compliance

The system must:

- collect recruiter email only for private-access management
- document email usage and retention
- document service providers involved in email and anti-bot flow
- avoid reusing collected recruiter emails for marketing

The system should:

- keep invitation/session data for the shortest useful duration
- keep security logs limited and masked
- preserve a written processing notice in docs

## 5. Performance

Target expectations are modest but explicit.

### Recruiter access pages

- page should render fast on standard desktop/mobile connections
- primary content should remain usable without excessive animation cost

### Chat

- chat request validation should be immediate
- streaming should begin promptly when the provider responds
- vague-question short-circuiting should reduce unnecessary model calls

### Retrieval

- retrieval should remain bounded by configurable `topK`
- ingestion should avoid reprocessing unchanged sources

## 6. Observability

The system must expose enough operational information to diagnose:

- failed invitation requests
- failed invitation deliveries
- invalid/expired invitation consumption
- recruiter session issues
- chat provider failures
- retrieval/ingestion problems

Minimum expectation:

- actionable logs
- request correlation where possible
- environment-aware logging levels

Preferred next step:

- basic metrics for invitation requests, chat volume, provider failures, ingestion outcomes

## 7. Maintainability

The product must continue to follow repository rules:

- typed configuration
- clear package boundaries
- thin controllers
- isolated infrastructure logic
- aligned docs and code

Every non-trivial new behavior should move the repo toward:

- better test coverage
- less duplicated configuration
- more explicit contracts

## 8. Testability

Required test baseline before calling the product stable:

- configuration binding tests
- security and access tests
- chat behavior tests
- ingestion tests
- HTTP tests for critical flows

Preferred additions for release readiness:

- a small end-to-end test script or smoke suite
- staging validation checklist

## 9. Operability

The system should be operable by a single maintainer.

This implies:

- straightforward environment configuration
- simple startup model
- documented secrets
- documented deployment path
- manageable third-party dependencies

## 10. Release Readiness Criteria

The product is non-functionally acceptable when:

- configuration is documented and validated
- invitation flow is safe and reliable
- chat and retrieval fail gracefully
- logs are sufficient for debugging
- core tests pass consistently
- deployment steps are documented and repeatable
