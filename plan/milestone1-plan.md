# Milestone 1 Implementation Plan

## Summary

Implement the backend platform core first. Keep the existing React app as a demo client, but replace the prototype backend chat flow with a versioned, tenant-aware, auditable chat API that can later connect to Flowelle tools, RAG, and production LLM providers.

## Key Changes

- Add `POST /v1/chat/messages`.
  - Request: `externalUserId`, optional `sessionId`, `message`, optional `locale`, optional `scopes`.
  - Response: `sessionId`, `answer`, `safetyStatus`, `citations`, `toolCalls`, `createdAt`.
- Keep `/chat` temporarily as a compatibility wrapper that calls the new service path.
- Introduce backend layers:
  - `ChatController` for HTTP only.
  - `ChatOrchestratorService` for prompt assembly, safety checks, retrieval/tool placeholders, model calls, and persistence.
  - `ModelClient` interface with current Ollama implementation.
  - `SafetyService` with wellness-only guardrails and red-flag routing.
  - `AuditService` for structured events without logging full user content.
- Add persistence with PostgreSQL-ready entities:
  - tenant, API key hash, chat session, chat message, audit event.
  - Use H2 for tests/dev fallback initially unless Postgres config is provided.
- Add API-key auth for `/v1/**`.
  - Header: `X-AIF-Tenant-Key`.
  - Reject missing/invalid keys with structured `401`.
  - Store only hashed keys.
- Add structured errors:
  - validation errors return `400`.
  - auth errors return `401`.
  - model/tool failures return safe fallback responses when possible.
- Update config:
  - Move Ollama base URL/model, allowed demo CORS origin, API-key seed, and frontend backend URL into environment-backed properties.
  - Stop logging full chat messages.
- Add focused backend tests:
  - successful chat creates/reuses session.
  - invalid API key rejected.
  - blank/oversized message rejected.
  - red-flag medical prompt returns safety-routed response.
  - Ollama failure returns safe fallback.
- Minimal frontend update:
  - Point demo client to `/v1/chat/messages`.
  - Read API base URL from env.
  - Use a demo tenant key only for local development.

## Test Plan

- Run `./mvnw test`.
- Add controller/service tests with mocked model client.
- Add frontend smoke test after installing dependencies: initial render, send message, error state.
- Do not require real Ollama for automated tests.
- Manual local check:
  - start backend,
  - start frontend,
  - send a normal wellness message,
  - send a red-flag symptom message,
  - verify no raw message body appears in backend logs.

## Assumptions

- First implementation should prioritize backend platform viability over full Flowelle integration.
- Flowelle tools and pgvector RAG are placeholders in Milestone 1, with interfaces prepared but no real host callback or embedding pipeline yet.
- Existing Spring Boot + React stack remains for now.
- `/chat` remains temporarily to avoid breaking the current demo during migration.
