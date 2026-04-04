# Helm Production Guide

## Purpose

Helm is the recommended parameterization layer for real production deployment.

Kustomize remains available as a reference baseline, but Helm is the preferred way to set:

- image repository and tag
- ingress host and TLS secret
- runtime resources
- external Redis and Qdrant endpoints
- recruiter-access email and captcha settings
- existing Kubernetes secret name

## Chart location

- `helm/private-recruiter-portfolio`

## Files

- `Chart.yaml`
- `values.yaml`
- `values.production-k3s.yaml`
- `templates/*`

## Current operating model

The chart reflects the validated runtime model:

- one full-stack application image
- application traffic on `8080`
- management traffic on `9090`
- readiness/liveness on the management port
- Prometheus annotations on the pod
- ingress exposing only the application service
- secrets read from an existing Kubernetes secret

## Mapping from Kustomize

Reference source:

- `k8s/overlays/production-k3s`

Helm should be considered the variable-driven equivalent of that overlay.

## Validation

Recommended commands:

```bash
helm lint ./helm/private-recruiter-portfolio
helm template private-recruiter-portfolio ./helm/private-recruiter-portfolio \
  -f ./helm/private-recruiter-portfolio/values.production-k3s.yaml
```

## Notes

- Redis and Qdrant are not chart-managed in this iteration.
- The chart assumes `mutuelle-secrets` exists unless overridden.
- Real hostnames and TLS secret names must be set in `values.production-k3s.yaml` before production use.
