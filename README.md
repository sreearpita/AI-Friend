# AI-Friend

AI-Friend is evolving from a prototype chat app into a tenant-aware wellness assistant platform. The backend core includes versioned chat APIs, API-key auth, persistence, audit events, safety routing, model abstraction, signed host-tool callbacks, and a React demo client.

The current implementation still uses Ollama locally for model calls. Flowelle-style host tools are supported through signed callback contracts, while curated RAG remains future work.

## Tech Stack

### Backend

- Java 17
- Spring Boot 3.2.3
- Spring AI 0.8.1
- Spring Web, Validation, Data JPA, Actuator
- Flyway database migrations
- H2 for local/test fallback
- PostgreSQL driver for deployed environments
- Ollama local model provider
- Signed host-tool callbacks for host-owned facts

### Frontend

- React 19
- Material UI 7
- Create React App test/build tooling
- Browser `fetch` for API calls
- Demo rendering of citations and host-tool call statuses under AI messages

## Current API

### `POST /v1/chat/messages`

Requires tenant authentication with the `X-AIF-Tenant-Key` header.

Request:

```json
{
  "externalUserId": "flowelle-user-1",
  "sessionId": null,
  "message": "What can I eat before my period?",
  "locale": "en-US",
  "scopes": ["wellness"]
}
```

Response:

```json
{
  "sessionId": "uuid",
  "answer": "Assistant answer",
  "safetyStatus": "OK",
  "citations": [],
  "toolCalls": [],
  "createdAt": "2026-06-07T00:00:00Z"
}
```

### Compatibility Endpoint

`POST /chat` still accepts a plain text body for the old demo flow. It uses the seeded demo tenant and delegates to the new orchestration path.

## Local Defaults

The app seeds a local demo tenant by default.

- API key header: `X-AIF-Tenant-Key`
- Demo key: `dev-aif-demo-key`
- Backend URL: `http://localhost:8080`
- Frontend URL: `http://localhost:3000`
- Ollama URL: `http://localhost:11434`
- Ollama model: `mistral`
- Local database: in-memory H2

## Configuration

Backend configuration is environment-backed through `src/main/resources/application.properties`.

Common overrides:

```bash
export SERVER_PORT=8080
export AIF_OLLAMA_BASE_URL=http://localhost:11434
export AIF_OLLAMA_MODEL=mistral
export AIF_ALLOWED_ORIGINS=http://localhost:3000
export AIF_DEMO_API_KEY=dev-aif-demo-key
export AIF_DATASOURCE_URL=jdbc:postgresql://localhost:5432/aifriend
export AIF_DATASOURCE_DRIVER=org.postgresql.Driver
export AIF_DATASOURCE_USERNAME=aifriend
export AIF_DATASOURCE_PASSWORD=change-me
export AIF_JPA_DDL_AUTO=update
export AIF_SEED_DEMO_TOOLS=false
export AIF_DEMO_TOOL_CALLBACK_URL=http://localhost:8090/aif/tools
export AIF_DEMO_TOOL_SIGNING_SECRET=dev-aif-tool-secret
export AIF_DEMO_TOOL_SIGNING_KEY_ID=dev-v1
export AIF_TOOL_REQUEST_TIMEOUT_MS=2000
export AIF_FLYWAY_ENABLED=true
export AIF_FLYWAY_BASELINE_ON_MIGRATE=true
export AIF_RETRIEVAL_ENABLED=true
export AIF_RETRIEVAL_MAX_CITATIONS=3
export AIF_RETRIEVAL_MIN_QUERY_LENGTH=4
```

Frontend configuration uses Create React App environment variables:

```bash
REACT_APP_AIF_API_BASE_URL=http://localhost:8080
REACT_APP_AIF_TENANT_API_KEY=dev-aif-demo-key
REACT_APP_AIF_EXTERNAL_USER_ID=demo-user
```

Only use the demo tenant key for local development.

## Host Tool Callbacks

AI-Friend does not store raw Flowelle health data. When a user asks for host-owned facts, such as next period date, cycle length, food, diet, or exercise suggestions, AI-Friend can call tenant-configured host tools and inject only approved returned summaries/facts into the model prompt for that turn.

Current deterministic tool intents:

- `cycle-summary`: triggered by next-period and cycle-length prompts.
- `user-preferences`: triggered by food, diet, exercise, preference, and lifestyle prompts.

Flowelle contract assumptions are based on the current `sreearpita/Flowelle` repository:

- `auth-service` runs at `http://localhost:8081` and exposes `POST /aif/tools/user-preferences`.
- `cycles-service` runs at `http://localhost:8082` and exposes `POST /api/aif/tools/cycle-summary`.
- Existing Flowelle endpoints include `GET /api/cycles/predictions?userId={id}`, `GET /api/cycles/current?userId={id}`, and `GET /auth/me`.

Manual local callback wiring example:

```bash
export AIF_SEED_DEMO_TOOLS=true
export AIF_DEMO_TOOL_CALLBACK_URL=http://localhost:8090/aif/tools
# Configure tenant tool rows to point at:
#   http://localhost:8082/api/aif/tools/cycle-summary
#   http://localhost:8081/aif/tools/user-preferences
```

Shared callback contract fixtures live in:

- AI-Friend: `src/test/resources/contracts/flowelle/`
- Flowelle cycles-service: `backend/cycles-service/src/test/resources/contracts/aif/`
- Flowelle auth-service: `backend/auth-service/src/test/resources/contracts/aif/`

Typed Flowelle tool contracts in AI-Friend:

- `FlowelleCycleSummaryRequest` / `FlowelleCycleSummaryResponse`
- `FlowelleUserPreferencesRequest` / `FlowelleUserPreferencesResponse`

Host tool callbacks are signed with HMAC-SHA256 over:

```text
timestamp + "." + jsonRequestBody
```

Callback requests include:

- `X-AIF-Tenant`
- `X-AIF-Timestamp`
- `X-AIF-Signature`
- `X-AIF-Request-Id`
- `X-AIF-Key-Id`

Tool calls are tenant-scoped and request-scope checked. `cycle-summary` requires a configured scope such as `cycle:read`; `user-preferences` requires a configured scope such as `preferences:read`. If a tool is missing, disabled, denied by scope, times out, or fails, the chat flow continues with a safe `SKIPPED` or `FAILED` tool call summary and general model context.

`TenantToolConfig.signingSecret` is currently suitable for local/dev use only. Production should source callback secrets from a secret manager or encrypted column and use `signingKeyId` for key rotation.

## Curated Retrieval

AI-Friend uses Flyway-managed content tables for reviewed wellness citations:

- `content_sources`: reviewed source metadata such as title, publisher, URL, locale, and status.
- `content_chunks`: reviewed chunks with topic, text, and keywords.

The current retrieval implementation is keyword-based, not vector-based. It only considers active, approved content and returns at most `AIF_RETRIEVAL_MAX_CITATIONS` citations. Relevant citation context is also added to the model prompt so wellness answers can be grounded in curated material.

Seeded local content covers:

- menstrual cramps self-care basics,
- PMS nutrition and hydration,
- gentle exercise during PMS/period,
- red-flag symptom routing.

This is a foundation for future pgvector retrieval. Production content should replace the placeholder source URLs and go through a review process.

## Running Locally

Start Ollama:

```bash
ollama pull mistral
ollama serve
```

Start the backend:

```bash
./mvnw spring-boot:run
```

Start the frontend:

```bash
cd chat-frontend
npm install
npm start
```

Open `http://localhost:3000`.

## Tests

Backend:

```bash
./mvnw test
```

Frontend:

```bash
cd chat-frontend
CI=true npm test -- --watchAll=false
```

Flowelle service tests:

```bash
cd ../Flowelle/backend/cycles-service && ./mvnw test
cd ../auth-service && mvn test
```

Automated backend tests mock the model client and do not require a live Ollama instance. Flowelle callback integration smoke tests use `MockRestServiceServer` and do not require live Flowelle services.

PostgreSQL migration coverage uses Testcontainers in `PostgresMigrationIntegrationTest`. Docker must be available for that test to run; if Docker is unavailable, the test is skipped rather than weakening the rest of the suite.

The React demo renders citations and host-tool statuses returned by `/v1/chat/messages` under each AI message for local inspection.

## Project Structure

```text
.
├── plan/
│   ├── master-plan.md
│   └── milestone1-plan.md
├── src/main/java/com/example/demo/
│   ├── config/             # Environment-backed app and web config
│   ├── controller/         # HTTP API
│   ├── dto/                # Request/response contracts
│   ├── exception/          # Structured API errors
│   ├── model/              # JPA entities and domain records
│   ├── repository/         # Spring Data repositories
│   ├── security/           # Tenant API-key auth and demo seeding
│   └── service/            # Chat orchestration, model, safety, audit, host tools
├── src/test/java/          # Backend integration tests
└── chat-frontend/          # React demo client
```

## Platform Status

Implemented:

- Versioned `/v1/chat/messages` endpoint
- Legacy `/chat` wrapper
- Tenant API-key auth for `/v1/**`
- Hashed API key storage
- Demo tenant seeding
- JPA entities for tenants, API keys, sessions, messages, and audit events
- H2 local/test fallback with PostgreSQL-ready configuration
- Chat orchestration service with session reuse and bounded history
- Safety red-flag routing and model fallback response
- Tenant-configured signed host-tool callbacks
- Typed Flowelle cycle summary and user preference contracts
- Flyway-managed platform schema
- Curated wellness content tables and keyword citations
- Scope checks and safe tool skip/failure behavior
- Structured API errors
- React demo client updated to the v1 API
- Backend and frontend smoke tests
- Testcontainers PostgreSQL migration verification
- Shared AI-Friend / Flowelle callback contract fixtures and signing-vector tests
- Stubbed Flowelle HTTP integration smoke coverage
- Frontend citation and tool-call visibility in the demo UI

Still placeholder or future work:

- Flowelle-side implementation of AI-Friend callback endpoints
- pgvector embeddings and semantic retrieval
- Rate limits, quotas, and deeper tenant administration
- Streaming responses
- Production deployment packaging

## License

This project is licensed under the MIT License. See `LICENSE` for details.