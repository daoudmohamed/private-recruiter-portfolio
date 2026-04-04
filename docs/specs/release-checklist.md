# Release Checklist

## 1. Scope

This checklist applies to staging and production candidate releases.

## 2. Product Readiness

- recruiter access flow is coherent
- homepage content is recruiter-first and current
- contact paths are valid
- AI wording remains factual and concise
- source documents used by RAG are reviewed

## 3. Configuration Readiness

- `OPENAI_API_KEY` configured
- `FRONTEND_BASE_URL` configured
- `FRONTEND_ORIGIN` configured
- `RECRUITER_ACCESS_TOKEN_SECRET` configured
- `ADMIN_API_KEY` configured if admin paths remain enabled
- `BREVO_API_KEY` configured
- `BREVO_TEMPLATE_ID` configured with numeric template id
- `RECRUITER_ACCESS_FROM_EMAIL` configured
- `RECRUITER_ACCESS_FROM_NAME` configured
- optional captcha settings configured for prod

## 4. Security Readiness

- recruiter-access enabled outside dev/test
- secure cookies enabled in production
- public paths reviewed
- admin paths reviewed
- logs do not leak raw tokens or full emails
- CORS origins restricted to expected frontend origins

## 5. Infrastructure Readiness

- Redis reachable
- Qdrant reachable
- outbound connectivity to OpenAI works
- outbound connectivity to Brevo works
- health endpoints reachable

## 6. Backend Validation

- `./gradlew test` passes
- startup succeeds with target profile
- no invalid configuration error at boot
- recruiter-access endpoints behave as expected
- chat works against target provider config

## 7. Frontend Validation

- frontend build succeeds
- access screen works
- private page renders correctly
- chat works visually
- contact links are valid
- mobile/desktop spot check done

## 8. Functional Smoke Checks

- request invitation
- receive invitation email
- consume invitation link
- verify recruiter session is established
- verify logout works
- verify already-used and expired link behavior
- verify one successful chat question

## 9. Deployment Asset Review

- Docker image matches current runtime assumptions
- Kubernetes manifests match current env vars and providers
- obsolete env vars removed
- obsolete dependencies removed from runtime manifests

## 10. Release Decision

A release is acceptable only if:

- critical flows work end to end
- deployment assets are aligned with current application behavior
- no blocker remains on access, chat, or provider configuration
