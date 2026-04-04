# Stateful Services Contract

## 1. Purpose

This document makes Redis and Qdrant explicit operational dependencies for production.

The application is not production-viable unless both services are:

- provisioned
- reachable from the application namespace
- backed by persistent storage
- recoverable after a node or storage failure

## 2. Required Services

### Redis

Required role:

- recruiter session state
- rate limiting state
- chat/session-adjacent ephemeral data

Expected service name in `production-k3s`:

- `redis-prod`

Required contract:

- TCP reachable on port `6379`
- persistent storage enabled if session persistence matters
- password enabled if the cluster trust boundary is not sufficient

Operational note:

- loss of Redis is not a total product data-loss event
- recruiter sessions and rate-limit counters may be lost

### Qdrant

Required role:

- vector store for recruiter-profile RAG

Expected service name in `production-k3s`:

- `qdrant-prod`

Required contract:

- gRPC reachable on port `6334`
- storage persisted outside ephemeral container filesystem
- collection rebuild possible from source documents

Operational note:

- Qdrant is recoverable if source documents are intact
- rebuild time must be acceptable for this small product

## 3. Persistence Expectations

### Redis

Minimum expectation:

- durable volume or documented acceptance of session loss

Recommended:

- persistent volume
- append-only or snapshot mode chosen intentionally
- explicit memory policy

### Qdrant

Minimum expectation:

- persistent volume on SSD-backed storage

Recommended:

- avoid SD-card-only persistence for long-lived production use
- document volume path and storage class

## 4. Backup Policy

### Redis

Minimum acceptable position:

- session loss is acceptable
- no business-critical durable source of truth is stored only in Redis

If session continuity matters:

- take periodic Redis snapshots
- document restore steps

### Qdrant

Minimum acceptable position:

- source documents remain the true source of truth
- Qdrant can be rebuilt from document ingestion

Recommended:

- periodic snapshot/backup of Qdrant storage
- documented rebuild procedure

## 5. Restore Procedure Expectations

### Redis restore

Acceptable result:

- service returns
- application starts
- users may need to reauthenticate

### Qdrant restore

Acceptable result:

- service returns
- collection either:
  - restored from backup
  - or rebuilt from source documents

## 6. Deployment-Time Checks

Before production deploy, verify:

1. `redis-prod` service exists in `mutuelle-production`
2. `qdrant-prod` service exists in `mutuelle-production`
3. `mutuelle-secrets` exists in `mutuelle-production`
4. persistent volumes are attached or documented

## 7. Smoke Checks

After deployment, verify:

1. app rollout completes
2. health endpoint responds
3. recruiter-access session endpoint responds
4. one grounded chat request succeeds

If chat fails while health succeeds, treat Qdrant and/or OpenAI reachability as first suspects.

## 8. Ownership

For this project, the repo owner is also the infra owner.

That means Redis and Qdrant must not remain “someone else’s problem”.
They are part of the production contract even if their manifests live outside this repository.
