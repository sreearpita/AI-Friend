# AI-Friend

AI-Friend is evolving from a prototype chat app into a tenant-aware wellness assistant platform. Milestone 1 introduces the backend platform core: versioned chat APIs, API-key auth, persistence, audit events, safety routing, model abstraction, and a React demo client.

The current implementation still uses Ollama locally for model calls, with placeholders for future Flowelle tools and curated RAG.

## Tech Stack

### Backend

- Java 17
- Spring Boot 3.2.3
- Spring AI 0.8.1
- Spring Web, Validation, Data JPA, Actuator
- H2 for local/test fallback
- PostgreSQL driver for deployed environments
- Ollama local model provider

### Frontend

- React 19
- Material UI 7
- Create React App test/build tooling
- Browser `fetch` for API calls

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
```

Frontend configuration uses Create React App environment variables:

```bash
REACT_APP_AIF_API_BASE_URL=http://localhost:8080
REACT_APP_AIF_TENANT_API_KEY=dev-aif-demo-key
REACT_APP_AIF_EXTERNAL_USER_ID=demo-user
```

Only use the demo tenant key for local development.

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

Automated backend tests mock the model client and do not require a live Ollama instance.

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
│   └── service/            # Chat orchestration, model, safety, audit placeholders
├── src/test/java/          # Backend integration tests
└── chat-frontend/          # React demo client
```

## Milestone 1 Status

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
- Structured API errors
- React demo client updated to the v1 API
- Backend and frontend smoke tests

Still placeholder or future work:

- Flowelle host-tool callbacks
- Curated RAG and pgvector embeddings
- Production migrations instead of Hibernate `ddl-auto`
- Rate limits, quotas, and deeper tenant administration
- Streaming responses
- Production deployment packaging

## License

This project is licensed under the MIT License. See `LICENSE` for details.