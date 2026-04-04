# Product Specification Index

This folder defines the target specification baseline for turning the project into a stable, deployable product.

Documents are split by responsibility:

- `mvp-baseline.md`: agreed MVP scope, deliverables, out-of-scope, acceptance criteria
- `functional-spec.md`: product scope, actors, user journeys, functional rules
- `technical-spec.md`: target architecture, components, contracts, technical decisions
- `non-functional-spec.md`: security, reliability, observability, performance, compliance
- `delivery-trajectory.md`: phased path from current state to a stable production-ready release
- `audit-preparation.md`: code and runtime review checklist against the MVP baseline

These documents are intended to become the reference for future implementation work.

Legacy prompt or one-off execution documents should not be added back at the root `docs/` level unless they remain active reference material.
The current reference set is:

- `docs/specs/*` for product and delivery
- `docs/recruiter-access-privacy.md` for privacy policy of recruiter access
- `docs/spring-kotlin-maintainability.md` for maintainability standards
