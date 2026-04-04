# private-recruiter-portfolio Helm Chart

This chart packages the current `production-k3s` runtime model for the recruiter portfolio.

It intentionally manages only the application layer:

- Deployment
- Service
- Ingress
- ConfigMap
- ServiceAccount

It does **not** deploy Redis or Qdrant.
Those services remain external dependencies and must already exist in the target cluster.

## Commands

Render locally:

```bash
helm template private-recruiter-portfolio ./helm/private-recruiter-portfolio \
  -f ./helm/private-recruiter-portfolio/values.production-k3s.yaml
```

Install or upgrade:

```bash
helm upgrade --install private-recruiter-portfolio ./helm/private-recruiter-portfolio \
  --namespace mutuelle-production \
  --create-namespace \
  -f ./helm/private-recruiter-portfolio/values.production-k3s.yaml
```

## Required external secret

By default, the chart expects an existing Kubernetes secret named `mutuelle-secrets` containing:

- `openai-api-key`
- `recruiter-access-token-secret`
- `admin-api-key`
- `brevo-api-key`
- `recruiter-access-captcha-recaptcha-secret-key`
- optional `redis-password`
- optional `qdrant-api-key`
