# SRE Operations Guide

## 1. Purpose

This guide defines the intended operating model for the private recruiter portfolio in a small self-hosted production setup, with special attention to:

- single-owner operation
- `k3s` on Raspberry Pi
- GitHub Actions based delivery
- lightweight but disciplined SRE practices

## 2. Operating Model

### Assumed platform

- one `k3s` cluster
- likely one ARM64 node
- Traefik ingress
- GitHub Actions building and publishing multi-arch images
- GHCR as image registry

### Assumed production posture

- low traffic
- human-paced deploys
- no strict HA guarantee
- fast manual recovery preferred over complex automation

## 3. Recommended Runtime Topology

Minimum production components:

- application deployment: 1 replica to start
- Redis: persistent storage enabled
- Qdrant: persistent storage enabled
- Traefik ingress
- TLS certificate management

Recommended approach on Raspberry:

- start with 1 application pod
- avoid HPA initially
- increase replicas only after real capacity observation

## 4. Capacity and Resource Guidance

### Application

Start conservatively:

- request: `256Mi` to `512Mi`
- limit: `1Gi` to `1500Mi`
- CPU request: `100m` to `250m`
- CPU limit: `500m` to `1000m`

Only increase after observing:

- RSS memory
- GC pressure
- response latency
- chat concurrency

Current `production-k3s` target on 8 GB Raspberry:

- app request: `384Mi`
- app limit: `1024Mi`
- JVM bounded with `JAVA_TOOL_OPTIONS="-XX:MaxRAMPercentage=60 -XX:InitialRAMPercentage=25 -XX:+UseContainerSupport"`

Operational intent:

- keep the app heap under control instead of relying only on the cgroup limit
- leave headroom for native memory, Netty, and filesystem cache

### Redis

Guidance:

- persistent volume required
- set memory cap explicitly
- keep append-only mode only if the hardware and storage can sustain it

Current `production-k3s` target on 8 GB Raspberry:

- request: `128Mi`
- limit: `256Mi`
- Redis `maxmemory`: `192mb`
- policy: `allkeys-lru`

This is intentionally conservative because Redis in this product holds ephemeral/session-oriented data, not the primary durable business source of truth.

### Qdrant

Guidance:

- persistent volume required
- SSD strongly preferred over SD card
- watch memory use as embeddings and documents grow

Current `production-k3s` target on 8 GB Raspberry:

- request: `256Mi`
- limit: `1Gi`

Keep this budget for now unless real data volume shows pressure.

## 5. Storage Guidance

For Raspberry production:

- do not rely on SD card alone for long-lived state
- prefer SSD-backed storage for:
  - Redis persistence
  - Qdrant storage
  - cluster state if possible

Minimum policy:

- persistent volumes for Redis and Qdrant
- documented backup location
- periodic backup test

## 6. Ingress and TLS

### Ingress

Use Traefik ingress with:

- one public host
- one service
- HTTPS only for public use

### TLS

Choose one model:

1. cert-manager with Let's Encrypt
2. manually provisioned TLS secret with documented renewal process

Preferred model:

- cert-manager if the cluster is internet reachable

## 7. Secrets Management

Cluster secrets required at minimum:

- `openai-api-key`
- `recruiter-access-token-secret`
- `admin-api-key`
- `brevo-api-key`
- `recruiter-access-captcha-recaptcha-secret-key` if captcha is enabled
- optional `redis-password`
- optional `qdrant-api-key`

Rules:

- never commit live secrets
- never keep production secrets in local config files
- rotate any secret that was ever published in Git history

Recommended evolution:

- current acceptable state: Kubernetes Secret created out of band
- next maturity step: SOPS, Sealed Secrets, or External Secrets

## 8. Deployment Procedure

### Step 1 — Build and publish image

Triggered by GitHub Actions:

- run CI
- build multi-arch image
- push to GHCR

### Step 2 — Validate manifests

Before deploy:

- render `k8s/overlays/production-k3s`
- confirm ingress domain
- confirm image tag
- confirm cluster secrets exist

### Step 3 — Deploy

Run GitHub Actions deploy workflow:

- first with `dry_run=true`
- then with `dry_run=false`

### Step 4 — Post-deploy checks

Verify:

- Deployment rollout complete
- `GET /actuator/health`
- recruiter invitation request works
- recruiter invitation consumption works
- chat works on at least one known grounded question
- logout works

## 9. Monitoring Minimum Baseline

At minimum, monitor:

- application up/down
- restart count
- pod OOMKilled events
- CPU and memory usage
- ingress error rate
- OpenAI failures
- Brevo failures

If Prometheus is added later, start with:
- request count
- request latency
- error rate
- JVM memory
- Redis health
- chat invocation duration

Current target implementation:

- Prometheus endpoint exposed on management port `9090`
- application traffic remains on `8080`
- ingress stays on the application port only

## 10. Logging Guidance

Logs should support:

- startup diagnosis
- provider failure diagnosis
- invitation flow validation
- chat request failure validation

Rules:

- do not log raw recruiter tokens
- do not log full recruiter emails when avoidable
- do not log secrets or kubeconfig material

Recommended retention:

- enough to investigate recent incidents
- not indefinite on a small node

## 11. Backup and Recovery

### Must-have

- backup strategy for Qdrant
- backup strategy for Redis if session loss matters
- recovery notes for re-creating Kubernetes secrets

### Restore expectations

Acceptable for this project:

- application can be redeployed quickly
- recruiter sessions may be lost in a hard Redis recovery event
- vector store should be restorable or rebuildable from source documents

Preferred resilience model:

- documents remain the source of truth
- Qdrant can be recreated if needed

See also:

- `stateful-services-contract.md`

## 12. Incident Response Playbook

### App not reachable

Check:

- ingress
- service endpoints
- pod status
- liveness/readiness probe failures

### Invitation flow broken

Check:

- recruiter-access config
- Brevo API key
- Brevo template id
- callback/base URL
- captcha config

### Chat broken

Check:

- OpenAI API key
- Qdrant reachability
- document ingestion state
- vector collection existence

### High memory or restarts

Check:

- pod memory usage
- JVM container tuning
- Qdrant/Redis contention
- overly aggressive replica count

## 13. Rollback Procedure

Rollback should be simple:

1. identify previous working image tag
2. redeploy previous image
3. verify rollout
4. run smoke checks
5. inspect logs before retrying a forward deploy

Avoid:

- deleting stateful stores unless corruption is confirmed
- changing multiple infra variables during rollback

## 14. Recommended Near-Term Improvements

### P0

- create a dedicated Raspberry-sized production overlay
- make ingress hostnames and TLS real
- document Redis/Qdrant provisioning and persistence

### P1

- add Prometheus metrics endpoint
- add container vulnerability scanning in CI
- add formal smoke-test evidence after deploy

### P2

- add automated certificate management
- add structured rollback commands to the runbook
- add light alerting

## 15. Final Guidance

Run this platform as a **small, carefully operated production**.

That means:

- optimize for clarity over complexity
- prefer one deployable unit
- prefer explicit runbooks over hidden automation
- accept that single-node hardware requires conservative sizing

The system is suitable for real use if operated with discipline, but it should not be mistaken for a highly available or fully automated platform.

## 16. Operational Maturity Targets

### Level 1 — Deployable

Conditions:

- image builds and pushes
- manifests render
- deploy works
- manual smoke test passes

### Level 2 — Operable

Conditions:

- real HTTPS ingress
- secrets managed only in cluster/runtime systems
- rollback procedure documented and tested
- Redis and Qdrant persistence/backup defined

### Level 3 — Observable

Conditions:

- Prometheus metrics exposed
- minimum dashboard or metric review routine exists
- provider failures are visible without manual deep log inspection

### Level 4 — Stable

Conditions:

- capacity is sized from real measurements
- repeated deploys succeed predictably
- incidents can be triaged from runbook + telemetry
- no major manual tribal knowledge remains

## 17. Recommended Order Of Execution

1. Make the production overlay realistic for Raspberry hardware.
2. Make ingress and TLS real.
3. Make Redis and Qdrant operationally explicit.
4. Add runtime smoke evidence after every release.
5. Add minimal observability.

Do not invert this order.
There is little value in advanced dashboards if the deployment shape and stateful dependencies are still informal.
