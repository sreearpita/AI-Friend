# AI-Friend Platform Core Plan

## Summary

Build AI-Friend from a prototype chat app into a server-to-server, embeddable AI assistant platform. V1 will use Spring Boot, PostgreSQL + pgvector, provider-based LLM adapters, scoped host-app tools, curated retrieval, tenant API keys, audit logs, and a wellness-only safety boundary. Flowelle becomes the first reference integration, but raw health data stays inside Flowelle.

## Key Changes

- Replace the raw `/chat` prototype with versioned APIs.
- Keep raw menstrual/cycle/symptom data host-owned; AI-Friend calls Flowelle tools for scoped facts.
- Add platform data models for tenants, API keys, user consent scopes, sessions, messages, derived preference summaries, content chunks, embeddings, and audit events.
- Add a tool registry so tenants can configure allowed tools and signed server callback endpoints.
- Add RAG over curated wellness content using pgvector, with citations for health-related answers.
- Add wellness-only safety orchestration with red-flag routing.
- Add model abstraction so Ollama can remain local/dev while production providers fit behind the same interface.
- Add privacy controls: hashed API keys, no full health-message logging, audit events, rate limits, quotas, encryption-at-rest assumption, and deletion/export hooks.
- Treat the current React app as a demo/reference client, not the trust boundary.

## Flowelle Reference Behavior

- "When is my next period date?" calls Flowelle's cycle-summary tool and explains the returned prediction with uncertainty.
- "What's my cycle length?" uses Flowelle's computed cycle stats.
- "Suggest food/diet/exercise" combines Flowelle context, user preferences, and curated wellness retrieval.
- "How to manage hormone imbalance?" gives general education, lifestyle guidance, and professional-care routing without diagnosis or treatment claims.

## Test Plan

- Backend unit tests for chat orchestration, tool authorization, RAG retrieval, safety routing, and model fallback.
- Integration tests with Testcontainers for PostgreSQL + pgvector migrations and retrieval.
- Contract tests for Flowelle tool callback schemas and signed requests.
- Security tests for invalid API keys, disallowed scopes, rate limits, and log redaction.
- Frontend tests for sending messages, displaying citations/tool states, loading/error states, and env-based API config.
- Acceptance scenarios covering next-period answer, cycle-length answer, wellness advice, unavailable host tool, unavailable model, and red-flag medical question.

## Assumptions

- V1 uses platform core first, not a Flowelle-only implementation.
- V1 is wellness-only, not clinical decision support.
- Raw health data remains in Flowelle; AI-Friend stores conversations, consent/audit records, content embeddings, and derived preference summaries.
- V1 uses tenant API keys for server-to-server auth.
- V1 targets pragmatic production: single-region managed services, observability, retries, rate limits, and graceful degradation.
- Safety/privacy design should align with HHS health-app guidance, FTC Health Breach Notification expectations, FDA CDS boundaries, OWASP LLM risks, and NIST AI RMF guidance before production launch.
