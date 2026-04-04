# SRE Infrastructure Audit

## 1. Scope

This audit reviews the infrastructure and operations posture actually defined in the repository:

- Docker image and local Compose assets
- Kubernetes manifests and overlays
- GitHub Actions CI/CD workflows
- runtime configuration and production profile assumptions
- deployability on a small self-hosted `k3s` cluster on Raspberry Pi

The goal is not to assess product UX, but to determine whether the system is operationally credible, supportable, and safe enough for a lightweight production deployment.

## 2. Executive Summary

Current infrastructure maturity is **usable for a lightweight single-owner production**, but **not yet hardened as a resilient SRE-grade platform**.

What is solid:

- CI, image build, and deployment workflow structure exist
- Docker image is now aligned with Java 25 and ARM64 target builds
- Kubernetes manifests are coherent enough to render and deploy
- security posture is better than average for a small app: non-root, read-only root filesystem, probes, explicit secrets, private recruiter access

What is still weak:

- observability is minimal
- rollback and runtime verification are still mostly manual
- the manifests still imply a level of HA that does not match a likely single-node Raspberry `k3s`
- stateful dependencies are external assumptions, not managed or validated by this repo
- TLS/certificate lifecycle and ingress hardening are only partially defined

## 3. Architecture Assessment

### 3.1 Runtime topology

The current intended production topology is:

- one full-stack application container
- Redis
- Qdrant
- Traefik ingress
- OpenAI external API
- Brevo external API

This is a pragmatic topology for a private recruiter portfolio. It is simple enough to operate on a small cluster and avoids unnecessary split-brain between frontend and backend.

### 3.2 Container strategy

The current Dockerfile builds:

- React frontend
- Spring Boot backend
- a single runtime image serving both

This is the right tradeoff for this project:

- one image
- one deployment unit
- one ingress/service path
- no cross-origin production complexity

Operationally, this reduces failure domains and release drift.

### 3.3 Kubernetes deployment model

The Kubernetes base manifests define:

- Deployment
- Service
- HPA
- PDB
- Ingress
- ConfigMap
- ServiceAccount

This is structurally sound, but the shape still reflects a more generic cloud deployment than a likely Raspberry single-node cluster.

## 4. Findings

### 4.1 High — Declared availability is stronger than real availability

The base deployment starts at 2 replicas and production scales to 3 replicas, with HPA and PDB enabled.

Relevant files:

- `k8s/base/deployment.yaml`
- `k8s/base/hpa.yaml`
- `k8s/base/pdb.yaml`
- `k8s/overlays/production/kustomization.yaml`

Problem:

- on a Raspberry `k3s`, the likely real topology is a single node
- anti-affinity does not create real fault tolerance on one node
- HPA on a constrained node can create scheduling pressure, evictions, or noisy-neighbor issues
- a PDB with `minAvailable: 2` in production is misleading if there is only one node or limited capacity

Impact:

- repo suggests high availability that the platform may not deliver
- maintenance or node failure still causes full downtime
- operators may assume safer rollouts than the hardware actually supports

Recommendation:

- define a specific `k3s-single-node` production overlay
- use `replicas: 1` or `2` only if hardware sizing is proven
- reduce or disable HPA unless metrics server and spare capacity are confirmed
- keep the PDB only if it reflects an actual multi-node maintenance posture

### 4.2 High — Stateful dependencies are external assumptions, not an operational contract

The application depends on Redis and Qdrant, but the production Kubernetes manifests in this repo do not provision them.

Relevant files:

- `k8s/base/configmap.yaml`
- `k8s/overlays/production/kustomization.yaml`
- `src/main/resources/application.yml`

Problem:

- the app expects `redis-prod` and `qdrant-prod`
- there is no repo-managed deployment, chart reference, storage contract, or backup contract for those services
- there is no health verification in CI/CD that confirms these dependencies exist in the target cluster

Impact:

- deployment success does not mean runtime success
- production readiness depends on undocumented external infrastructure state
- recovery and backup posture for recruiter session state and vector data are undefined

Recommendation:

- either manage Redis and Qdrant explicitly in infra docs or add their deployment references
- define backup, restore, and persistence strategy
- document service ownership and expected endpoints in the runbook
- see `stateful-services-contract.md` as the minimum contract to close this finding

### 4.3 High — Observability is below minimum SRE operating comfort

The application exposes only `health`, `info`, and `metrics`.

Relevant files:

- `src/main/resources/application.yml`
- `src/main/resources/application-prod.yml`
- `.github/workflows/deploy.yml`

Problem:

- there is no Prometheus scrape endpoint
- no log shipping contract
- no alerting model
- no SLO/SLI definition
- no dashboard baseline

Impact:

- production failures will be diagnosed primarily through ad hoc log inspection
- latency regressions, provider failures, and recruiter-access anomalies may go unnoticed
- capacity planning on Raspberry hardware is blind

Recommendation:

- add Micrometer Prometheus registry and `/actuator/prometheus`
- define minimum metrics:
  - HTTP latency/error rate
  - OpenAI request failures
  - Brevo request failures
  - recruiter invitation requests
  - recruiter session creation/logout
  - chat request duration
- document log access and retention expectations

Closure note:

- a stronger closure is to expose metrics on a management-only port, not through the public ingress path

### 4.4 Medium — Ingress exists, but TLS lifecycle is still manual and placeholder-driven

The repo now includes a Traefik ingress.

Relevant files:

- `k8s/base/ingress.yaml`
- `k8s/overlays/production/kustomization.yaml`
- `k8s/overlays/staging/kustomization.yaml`

Problem:

- base ingress still uses placeholder hostnames
- production and staging overlays still use example domains
- TLS is modeled via pre-existing secrets, but no cert-manager flow is defined

Impact:

- deployability is plausible, but not production-complete
- certificate rotation is operationally manual unless managed elsewhere

Recommendation:

- replace example domains with real domains
- either:
  - use cert-manager with a ClusterIssuer, or
  - document manual certificate provisioning and renewal

### 4.5 Medium — CI/CD is good for build/release mechanics, but weak on runtime safety

The GitHub Actions pipeline covers:

- tests
- lint/build
- secret scanning
- image build
- render/apply deployment

Relevant files:

- `.github/workflows/ci.yml`
- `.github/workflows/container-image.yml`
- `.github/workflows/deploy.yml`

Problem:

- deploy workflow validates manifests client-side, not cluster server-side until apply
- no post-deploy smoke automation
- no rollback automation
- no image vulnerability scan in CI

Impact:

- runtime misconfiguration can survive until manual testing
- production deploy confidence depends on manual discipline

Recommendation:

- add a post-deploy smoke job or a separately documented manual gate with recorded evidence
- add Trivy or Grype image scanning
- add rollback instructions using previous image tag

### 4.6 Medium — Production profile is safer, but logging and secrets posture still need discipline

The production profile disables Swagger and reduces logs, which is good.

Relevant files:

- `src/main/resources/application-prod.yml`
- `k8s/base/secret.example.yaml`

Problem:

- security still depends on correct external secret management
- there is no sealed secret or external secret operator pattern
- Git history already required cleanup for leaked local secrets

Impact:

- the operational model is safe only if secrets discipline is maintained outside the repo

Recommendation:

- treat the cluster as the source of truth for secrets
- use SOPS, Sealed Secrets, or External Secrets if the setup grows
- rotate previously exposed keys if not already done

### 4.7 Medium — Single-node `k3s` capacity risk is understated

Production resource requests and limits are aggressive for Raspberry hardware.

Relevant files:

- `k8s/base/deployment.yaml`
- `k8s/overlays/production/kustomization.yaml`

Problem:

- production requests move to `1Gi` memory and `500m` CPU per pod
- limits go to `4Gi` and `2000m`
- HPA max is 20 pods

Impact:

- on Raspberry hardware this is unrealistic unless the node is unusually large
- scheduling and OOM risk are high
- the declared autoscaling posture is not aligned with probable edge hardware reality

Recommendation:

- right-size resources against the actual node
- start with one pod and measured usage
- tune Java heap and Spring AI usage empirically

### 4.8 Low — Compose remains useful for local smoke, not an SRE-grade parity environment

The Docker Compose stack is fine for local validation.

Relevant files:

- `docker/docker-compose.yml`
- `docker/docker-compose.dev.yml`

Problem:

- it is not a true production analog
- local mounted documents and direct host ports are intentionally dev-oriented

Impact:

- acceptable for developer workflows
- should not be mistaken for production parity

Recommendation:

- keep Compose for local smoke only
- treat Kubernetes as the production contract

## 5. What Is Production-Ready Enough

For a lightweight private portfolio, the following parts are close to acceptable:

- single-image application model
- private recruiter access controls
- non-root runtime and read-only root filesystem
- CI build/test/publish flow
- manual deployment workflow

## 6. What Prevents a Stronger SRE Rating

The main blockers to a stronger SRE posture are:

1. no explicit operational contract for Redis and Qdrant
2. minimal observability
3. unrealistic production scaling defaults for a Raspberry `k3s`
4. manual TLS and post-deploy validation posture
5. no automated rollback/smoke enforcement

## 7. Target Rating

Current rating for a lightweight self-hosted production:

- **Build and delivery maturity**: B
- **Runtime hardening**: B-
- **Observability**: D+
- **Operational resilience**: C-
- **Single-owner deployability**: B
- **Overall SRE readiness**: **C+**

This is good enough for a carefully operated personal production, but not yet for a “set and forget” service posture.

## 8. Priority Actions

### P0

- create a real `production-k3s` overlay sized for the Raspberry node
- replace placeholder ingress domains and TLS secrets with real values
- document and validate Redis/Qdrant ownership, storage, and backup

### P1

- add Prometheus metrics export
- add image vulnerability scanning in CI
- add post-deploy smoke validation or mandatory release gate

### P2

- formalize rollback commands
- improve alerting and log access
- introduce secret management tooling if the platform grows

## 9. Final SRE Recommendation

This infrastructure is **viable for a small, owner-operated production deployment** if the operator accepts:

- a single-node failure domain
- mostly manual incident response
- limited observability

It should be treated as:

- a pragmatic production MVP
- not a highly available platform
- not a fully hardened SRE environment yet

The right next step is not more architecture. It is to **tighten operational realism**:

- size the deployment to the actual Raspberry cluster
- make ingress/TLS real
- make stateful dependencies explicit
- add just enough monitoring to detect breakage before users do

## 10. Remediation Matrix

### Finding 4.1 — Availability posture too optimistic

- Priority: P0
- Effort: Medium
- Suggested owner: Platform / repo owner
- Target outcome: manifests reflect the real Raspberry `k3s` production topology

Actions:

1. create a dedicated production overlay for single-node `k3s`
2. reduce default production replicas to `1`
3. disable HPA initially, or set it behind an explicit capacity check
4. adjust or remove PDB if there is no real multi-node maintenance need

Evidence of closure:

- a dedicated `k8s/overlays/production-k3s` overlay exists for the actual cluster shape
- `kubectl kustomize` renders realistic replica counts and scaling settings
- deployment succeeds without unschedulable pods

### Finding 4.2 — Redis and Qdrant are not an explicit infra contract

- Priority: P0
- Effort: Medium
- Suggested owner: Platform / repo owner
- Target outcome: Redis and Qdrant are operationally explicit, not tribal knowledge

Actions:

1. document where Redis and Qdrant are provisioned
2. document persistence classes / storage paths
3. define backup and restore procedure
4. add a runtime smoke check that confirms both dependencies are reachable

Evidence of closure:

- runbook includes ownership, endpoints, storage, and restore notes
- post-deploy smoke confirms Redis and Qdrant reachability
- backup location and recovery procedure are written down

### Finding 4.3 — Observability is too weak

- Priority: P1
- Effort: Medium
- Suggested owner: Application + platform owner
- Target outcome: enough telemetry exists to detect and debug failures quickly

Actions:

1. add Prometheus registry support
2. expose `/actuator/prometheus`
3. define a minimum dashboard or metric checklist
4. add counters/timers for:
   - recruiter invitation flow
   - chat requests
   - provider failures

Evidence of closure:

- Prometheus endpoint is exposed in production
- at least one dashboard or metric review checklist exists
- failures in OpenAI/Brevo/chat flow can be observed without raw log grepping only

### Finding 4.4 — Ingress/TLS still not production-real

- Priority: P0
- Effort: Low to Medium
- Suggested owner: Platform / repo owner
- Target outcome: public entrypoint is real, routable, and renewable

Actions:

1. replace placeholder hosts with the real domain
2. choose TLS operating model:
   - cert-manager
   - or manual TLS with documented renewal
3. validate end-to-end HTTPS access from outside the cluster

Evidence of closure:

- real hostnames are present in the production overlay
- certificate issuance/renewal path is documented and tested
- browser access works over HTTPS without manual patching

### Finding 4.5 — CI/CD lacks runtime safety gates

- Priority: P1
- Effort: Medium
- Suggested owner: Platform / repo owner
- Target outcome: deploy confidence is based on evidence, not only on manifest apply success

Actions:

1. add image vulnerability scanning in CI
2. add post-deploy smoke validation or a hard manual release gate
3. document rollback-by-image-tag procedure

Evidence of closure:

- CI fails on significant image security issues
- deploy workflow or runbook includes a formal smoke step
- rollback instructions are executable in one pass

### Finding 4.6 — Secrets posture is still fragile

- Priority: P1
- Effort: Medium
- Suggested owner: Platform / repo owner
- Target outcome: production secrets are handled consistently and recoverably

Actions:

1. confirm all previously exposed keys have been rotated
2. keep cluster secrets as the runtime source of truth
3. plan a next-step secret-management model if infra grows

Evidence of closure:

- key rotation is confirmed
- production secrets are no longer sourced from local files
- secret provisioning procedure is documented end to end

### Finding 4.7 — Production sizing is not aligned with Raspberry constraints

- Priority: P0
- Effort: Low
- Suggested owner: Platform / repo owner
- Target outcome: the declared resource model matches the actual node

Actions:

1. measure node memory and CPU budget
2. lower production requests/limits to conservative values
3. deploy one pod first and observe
4. tune Java/container memory from measured behavior

Evidence of closure:

- no unschedulable pods
- no OOMKilled restarts during smoke and normal use
- resource requests reflect observed usage, not generic defaults

## 11. 30-Day Hardening Plan

### Week 1

- create a real Raspberry-sized production overlay
- set real ingress hostname and TLS strategy
- right-size resources for one-node production

### Week 2

- document Redis and Qdrant provisioning, persistence, and backup
- validate full external access over HTTPS
- validate smoke test against the live domain

### Week 3

- add vulnerability scanning to CI
- document rollback procedure
- define and record post-deploy smoke evidence

### Week 4

- add Prometheus endpoint and minimum metrics
- review logs and alerts
- rerun the SRE audit after the first stable deploy

## 12. Exit Criteria For A Stronger Audit

The next SRE audit should be materially better only if all of the following are true:

- manifests match the actual Raspberry production topology
- public HTTPS access is real and repeatable
- Redis and Qdrant are explicitly operated, backed up, and recoverable
- deploys include a formal runtime validation step
- minimum observability exists beyond raw logs
