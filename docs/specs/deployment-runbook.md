# Deployment Runbook

## 1. Purpose

This runbook describes how to prepare, deploy, validate, and operate the private recruiter portfolio in staging and production.

It documents the intended runtime model and explicitly highlights current repository gaps that must be resolved before production deployment.

## 2. Runtime Topology

Target runtime components:

- frontend static app
- backend Spring Boot API
- Redis
- Qdrant
- OpenAI API
- Brevo transactional email API

Expected logical flow:

1. recruiter requests access
2. backend validates request and sends invitation email via Brevo
3. recruiter consumes magic link
4. backend creates authenticated recruiter session
5. frontend loads private portfolio
6. chat requests go to backend and use OpenAI + Qdrant retrieval

## 3. Environment Profiles

### Development

Purpose:

- local implementation and manual testing

Expected characteristics:

- `SPRING_PROFILES_ACTIVE=dev`
- local Redis and Qdrant
- recruiter-access enabled for local testing
- email provider in `LOG` mode or test-safe mode
- captcha bypass allowed by config

### Staging

Purpose:

- production-like validation

Expected characteristics:

- `SPRING_PROFILES_ACTIVE=prod` or dedicated staging profile
- real Redis and Qdrant
- real OpenAI
- real Brevo to controlled recipients
- real secure cookie behavior
- restricted CORS and secrets

### Production

Purpose:

- real recruiter usage

Expected characteristics:

- hardened secrets
- secure cookies
- explicit CORS
- production email and captcha enabled
- documented rollback path

## 4. Required Infrastructure

### Backend

- Java runtime compatible with current build
- reachable Redis
- reachable Qdrant gRPC endpoint
- outbound network access to OpenAI and Brevo

### Frontend

- static hosting or containerized frontend deployment
- base URL consistent with recruiter-access links

### External Services

- OpenAI account and API key
- Brevo account, API key, template ID, verified sender
- optional captcha provider setup for production

## 5. Deployment Preparation Checklist

Before first deployment:

1. verify all secrets are present
2. verify `FRONTEND_BASE_URL` is correct
3. verify `FRONTEND_ORIGIN` matches the hosted frontend
4. verify Brevo sender identity and template id
5. verify Redis and Qdrant connectivity from target environment
6. verify document folder strategy for runtime ingestion
7. verify production cookie settings
8. verify OpenAPI/Swagger exposure policy

## 6. Current Repository Gaps

The current deployment assets are not fully aligned with the live application configuration.

Known gaps:

- `docker/docker-compose.yml` still references `ANTHROPIC_API_KEY`, while the backend now uses OpenAI only
- Kubernetes manifests still reference removed or irrelevant settings such as database, RabbitMQ, and Anthropic
- deployment manifests do not yet declare the full set of secrets required by recruiter-access, Brevo, and OpenAI

Consequence:

- Docker/Kubernetes assets must be reviewed and corrected before calling deployment production-ready

## 7. Recommended Deployment Order

### Step A: Infrastructure

- provision Redis
- provision Qdrant
- provision backend runtime
- provision frontend hosting

### Step B: Secrets

- inject OpenAI secret
- inject recruiter access secret
- inject Brevo credentials
- inject optional captcha credentials
- inject admin API key if retained

### Step C: Backend rollout

- deploy backend
- validate health endpoints
- validate startup logs
- confirm config validation passes

### Step D: Frontend rollout

- deploy frontend
- verify origin and CORS behavior
- verify access screen loads

### Step E: Functional smoke validation

- request invitation
- receive email
- consume link
- load private page
- open chat
- ask one grounded question
- logout

## 8. Smoke Test Procedure

Minimum smoke checks after each deployment:

1. deployment `Ready` and service endpoints populated
2. `GET /api/v1/recruiter-access/session`
3. `GET /`
4. invitation request flow
5. invitation consumption flow
6. `POST /api/v1/sessions`
7. one chat streaming request
8. one document listing request if enabled

Operational note:

- actuator/management checks stay internal on port `9090`
- public ingress validation stays on the application port `8080`

Expected result:

- all endpoints behave consistently
- no broken cookie/session behavior
- no provider configuration errors

## 9. Rollback Guidance

Rollback triggers:

- startup failure
- invalid invitation flow
- broken chat behavior
- broken CORS/cookie behavior
- provider misconfiguration

Rollback path should be:

1. revert backend to previous image
2. revert frontend to previous static release if needed
3. keep data stores intact unless schema/runtime mismatch requires intervention
4. preserve logs for analysis

## 10. Operational Notes

The intended operating model is lightweight.
The maintainer should be able to:

- deploy with documented env vars
- inspect health and logs
- validate core journeys manually
- debug failures without guessing hidden dependencies
