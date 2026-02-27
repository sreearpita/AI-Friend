# AI Friend Chat Application

A modern chat application that simulates conversations with an AI friend using Spring Boot and React. The AI is powered by Ollama and **remembers you across conversations** using persistent memory stored in PostgreSQL.

## Features

- 🤖 Personalized AI responses with context awareness
- 🧠 **Persistent cross-conversation memory** – the AI remembers facts about you
- 💬 Real-time chat interface
- 🎨 Modern, responsive UI design
- ⚡ Fast and efficient message handling
- 🔄 Smooth animations and transitions
- 📱 Mobile-friendly design

## Tech Stack

### Backend
- Java 17
- Spring Boot 3.2.3
- Spring Data JPA + Hibernate
- PostgreSQL with pgvector extension
- Flyway database migrations
- Ollama AI Integration (via REST API)

### Frontend
- React
- Material-UI (MUI)
- Axios
- Modern CSS with gradients and animations

## Prerequisites

Before running the application, make sure you have the following installed:
- Java 17 or higher
- Node.js and npm
- Ollama (with mistral model installed)
- Maven
- Docker & Docker Compose (for PostgreSQL)

## Setup and Installation

### 1. Start PostgreSQL with pgvector

Use Docker Compose to start a PostgreSQL instance with the pgvector extension pre-installed:

```bash
docker-compose up -d
```

This starts PostgreSQL on `localhost:5432` with:
- Database: `aifriend`
- Username: `aifriend`
- Password: `aifriend`

The `pgvector/pgvector:pg16` image includes the vector extension. Flyway will automatically run migrations on startup to create all required tables.

> **Production note:** Override credentials using environment variables `DB_URL`, `DB_USER`, and `DB_PASSWORD`.

### 2. Backend Setup

1. Clone the repository:
```bash
git clone <repository-url>
cd <project-directory>
```

2. Make sure Ollama is running with the mistral model:
```bash
ollama pull mistral
ollama serve
```

3. Build and run the Spring Boot application:
```bash
mvn spring-boot:run
```

The backend will start on `http://localhost:8080`.

### 3. Frontend Setup

1. Navigate to the frontend directory:
```bash
cd chat-frontend
```

2. Install dependencies:
```bash
npm install
```

3. Start the development server:
```bash
npm start
```

The frontend will be available at `http://localhost:3000`.

## Project Structure

```
├── src/
│   ├── main/
│   │   ├── java/
│   │   │   └── com/example/demo/
│   │   │       ├── controller/        # REST controllers
│   │   │       │   ├── ChatController.java          # Legacy /chat endpoint
│   │   │       │   ├── ChatApiController.java       # POST /api/chat
│   │   │       │   └── ConversationController.java  # POST /api/conversations
│   │   │       ├── model/             # JPA entities
│   │   │       ├── repository/        # Spring Data repositories
│   │   │       ├── service/           # Business logic
│   │   │       │   ├── OllamaService.java       # Ollama HTTP client
│   │   │       │   ├── ChatApiService.java      # Chat orchestration + memory
│   │   │       │   ├── MemoryService.java       # Memory extraction & retrieval
│   │   │       │   ├── ConversationService.java # Conversation management
│   │   │       │   └── UserService.java         # User management
│   │   │       ├── dto/               # Request/response DTOs
│   │   │       └── SpringbootAIApplication.java
│   │   └── resources/
│   │       ├── application.properties # Application configuration
│   │       └── db/migration/          # Flyway SQL migrations
│   └── test/                          # Test files
├── chat-frontend/                     # React frontend
│   ├── src/
│   │   ├── App.js                     # Main React component
│   │   └── ...
│   └── package.json
├── docker-compose.yml                 # PostgreSQL + pgvector
└── README.md
```

## API Endpoints

### New persistent-memory API

| Method | Path | Header | Body | Description |
|--------|------|--------|------|-------------|
| `POST` | `/api/conversations` | `X-User-Id: <uuid>` | – | Create a new conversation |
| `POST` | `/api/chat` | `X-User-Id: <uuid>` | `{conversationId, message}` | Send a message |

### Legacy endpoint (stateless, no memory)

| Method | Path | Body | Description |
|--------|------|------|-------------|
| `POST` | `/chat` | `<text>` | Single-turn chat (no persistence) |

## Configuration

Key properties in `application.properties` (all overridable via environment variables):

| Property | Env var | Default | Description |
|----------|---------|---------|-------------|
| `app.ollama.base-url` | `OLLAMA_BASE_URL` | `http://localhost:11434` | Ollama server URL |
| `app.ollama.model` | `OLLAMA_MODEL` | `mistral` | LLM model name |
| `spring.datasource.url` | `DB_URL` | `jdbc:postgresql://localhost:5432/aifriend` | Database URL |
| `spring.datasource.username` | `DB_USER` | `aifriend` | Database user |
| `spring.datasource.password` | `DB_PASSWORD` | `aifriend` | Database password |
| `app.cors.allowed-origins` | `CORS_ALLOWED_ORIGINS` | `http://localhost:3000` | CORS allowed origins |
| `app.memory.top-k` | – | `10` | Number of memories injected per prompt |
| `app.chat.last-n-messages` | – | `20` | Conversation history window |

## How Memory Works

1. **Retrieval**: Before each response, the top-K most recently confirmed memories are fetched and injected into the system prompt.
2. **Extraction**: After the AI responds, a second LLM call extracts new facts from the user's message **asynchronously** (no added latency).
3. **Deduplication**: Facts are matched case-insensitively; existing facts update their confidence and `last_confirmed_at` timestamp.
4. **Persistence**: All data (users, conversations, messages, memories) is stored in PostgreSQL and survives restarts.

> **pgvector note:** The `vector` extension is enabled in the migration for future semantic similarity retrieval. Current MVP uses recency-based retrieval (`ORDER BY updated_at DESC`).
