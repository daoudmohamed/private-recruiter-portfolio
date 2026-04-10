# Admin Documents Interface

This document defines the private admin-only interface used to manage indexed documents in the RAG knowledge base.

## Goal

Provide a browser interface for operators to:

- list indexed document sources
- upload a new document for ingestion
- trigger a filesystem rescan
- delete indexed documents by source

The interface is intentionally limited to capabilities already supported by the backend API.

## Route and exposure

- Frontend route: `/admin/documents`
- The route is intentionally not linked from the public navigation.
- The route serves the normal SPA shell, but all document-management data remains protected by backend admin authentication.

## Authentication model

V1 uses the existing admin API key:

- request header: `X-API-Key`
- backend secret: `ADMIN_API_KEY`

Browser behavior:

- the admin key is entered manually in the admin screen
- it is stored only in memory for the current tab lifecycle
- it is never persisted to `localStorage` or `sessionStorage`

## Backend contract

Protected admin endpoints:

- `GET /api/v1/documents`
- `POST /api/v1/documents/upload`
- `POST /api/v1/documents/scan`
- `DELETE /api/v1/documents/{source}`

Expected responses:

- list: `sources[]` and `count`
- upload: success/message/chunks summary
- scan: ingestion summary
- delete: source/deleted/message

## Non-goals for V1

This interface does not yet provide:

- document preview
- document editing or renaming
- admin session login or cookie auth
- role-based browser identities
