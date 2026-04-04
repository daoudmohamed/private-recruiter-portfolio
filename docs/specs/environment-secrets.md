# Environment Variables and Secrets

## 1. Purpose

This document lists the runtime configuration expected by the application and separates mandatory settings from optional ones.

## 2. Mandatory for Production

### Core runtime

- `SPRING_PROFILES_ACTIVE=prod`
- `OPENAI_API_KEY`
- `REDIS_HOST`
- `REDIS_PORT`
- `QDRANT_HOST`
- `QDRANT_GRPC_PORT`

### Recruiter access

- `FRONTEND_BASE_URL`
- `FRONTEND_ORIGIN`
- `RECRUITER_ACCESS_TOKEN_SECRET`
- `RECRUITER_ACCESS_FROM_EMAIL`
- `RECRUITER_ACCESS_FROM_NAME` or default accepted

### Brevo

- `BREVO_API_KEY`
- `BREVO_TEMPLATE_ID`
- `RECRUITER_ACCESS_REPLY_TO` if distinct from sender

### Captcha for production

- `RECRUITER_ACCESS_CAPTCHA_SITE_KEY`
- `RECRUITER_ACCESS_CAPTCHA_RECAPTCHA_SECRET_KEY`

## 3. Recommended for Production

- `ADMIN_API_KEY`
- `QDRANT_API_KEY` if Qdrant is protected
- `REDIS_PASSWORD` if Redis is protected
- `RECRUITER_ACCESS_EMAIL_SUBJECT`

## 4. Optional / Local Defaults

- `DOCUMENTS_FOLDER`
- `API_KEY` for compatibility fallback
- `RECRUITER_ACCESS_CAPTCHA_PROVIDER`
- `RECRUITER_ACCESS_CAPTCHA_VERIFY_ENABLED`
- `RECRUITER_ACCESS_CAPTCHA_RECAPTCHA_VERIFY_URL`

## 5. Derived from Current Config

The backend currently reads at least the following families:

- OpenAI
- Redis
- Qdrant
- recruiter access
- captcha
- Brevo
- CORS/frontend origin

## 6. Current Consistency Note

Deployment assets were aligned with the current runtime assumptions.

What still remains to verify is not naming consistency, but real environment validation:

- secrets actually provisioned in staging/production
- provider connectivity
- expected frontend URLs and origins
- cookie and invite behavior behind the real deployment topology

## 7. Secret Handling Rules

- do not commit secrets
- prefer external secret managers or deployment secrets
- avoid fallback defaults in production for sensitive values
- rotate recruiter access and provider secrets if exposure is suspected

## 8. Suggested Grouping

### Backend app secrets

- `OPENAI_API_KEY`
- `ADMIN_API_KEY`
- `RECRUITER_ACCESS_TOKEN_SECRET`

### Email secrets

- `BREVO_API_KEY`

### Infra secrets

- `REDIS_PASSWORD`
- `QDRANT_API_KEY`

### Captcha secrets

- `RECRUITER_ACCESS_CAPTCHA_RECAPTCHA_SECRET_KEY`
