# Real AI-Friend / Flowelle E2E

This suite starts the real AI-Friend, Flowelle auth-service, Flowelle cycles-service, PostgreSQL, and Ollama services with Docker Compose. It is separate from Maven tests that use `MockMvc`, Mockito, or stubbed callback HTTP.

Prerequisites:

- Docker Engine/Desktop with Compose v2
- Node.js 18 or newer
- The sibling Flowelle checkout at `../Flowelle`, or `FLOWELLE_DIR` pointing to it
- Enough disk and memory for the configured Ollama model

Run from the AI-Friend repository:

```bash
node e2e/run-e2e.js
```

The runner fails closed if Docker, any application health endpoint, PostgreSQL, or Ollama is unavailable. It generates disposable keys, configures AI-Friend through its real admin API, registers a real Flowelle user, sends a real proxy chat request, and verifies signed callbacks, consent behavior, and replay rejection.

The suite uses host ports `18080` for AI-Friend, `18081` for Flowelle auth-service, and `18082` for Flowelle cycles-service. Runtime files and redacted Compose logs are written under `e2e/.runtime/` and are ignored by Git.
