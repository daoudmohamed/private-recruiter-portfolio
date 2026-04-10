# Product Specification Index

This folder contains the active reference documentation for the product, runtime, and deployment model.

Keep only documents here that are still useful for implementation, deployment, or operations.

- `functional-spec.md`: product scope, actors, user journeys, functional rules
- `technical-spec.md`: target architecture, components, contracts, technical decisions
- `non-functional-spec.md`: security, reliability, observability, performance, compliance
- `github-actions-cicd.md`: CI/CD contract, GitHub Actions workflows, required environment secrets
- `sre-operations-guide.md`: recommended operating model for self-hosted production and incident handling
- `stateful-services-contract.md`: operational contract for Redis and Qdrant in production
- `helm-production-guide.md`: Helm chart usage and mapping from the production-k3s baseline
- `deployment-runbook.md`: manual deployment and recovery procedure
- `release-checklist.md`: release and verification checklist
- `environment-secrets.md`: runtime and deployment secret matrix

Older audit, trajectory, or one-off review notes have been removed from this folder to keep the set focused.

The current reference set is:

- `docs/specs/*` for product and delivery
- `docs/recruiter-access-privacy.md` for privacy policy of recruiter access
- `docs/spring-kotlin-maintainability.md` for maintainability standards
