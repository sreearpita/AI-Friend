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
.
├── src/                                # Backend source files
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
| `POST` | `/api/chat` | `X-User-Id: <uuid>` | `{conversationId, message}` | Send a message; returns `{reply}` |

### Legacy endpoint (still available)
| Method | Path | Body | Description |
|--------|------|------|-------------|
| `POST` | `/chat` | plain text | Stateless chat (no memory) |

## Database Schema

The Flyway migration (`V1__initial_schema.sql`) creates:

- **`app_user`** – anonymous users identified by a UUID stored in the browser
- **`conversation`** – chat sessions belonging to a user
- **`message`** – individual messages (user + assistant) in a conversation
- **`memory`** – facts extracted from conversations (persisted across sessions)

The migration also enables the **pgvector** extension for future embedding-based retrieval.

## Configuration

### Backend (`application.properties`)

```properties
# Ollama
spring.ai.ollama.base-url=http://localhost:11434
spring.ai.ollama.model=mistral

# Database
spring.datasource.url=jdbc:postgresql://localhost:5432/aifriend
spring.datasource.username=aifriend
spring.datasource.password=aifriend

# Persona
app.persona=You are my best friend...

# Memory settings
app.memory.top-k=10
app.chat.last-n-messages=20
```

### Frontend

The frontend reads `REACT_APP_API_BASE` (defaults to `http://localhost:8080`) for API calls. Set this environment variable if your backend runs elsewhere.

## How Memory Works

1. **User identity**: On first load, the React app generates a UUID and stores it in `localStorage` as `ai_friend_user_id`. This UUID is sent as the `X-User-Id` header on every request.

2. **Conversation**: On first load (or when starting fresh), the frontend creates a conversation via `POST /api/conversations`. The `conversationId` is stored in `localStorage`.

3. **Memory extraction**: After each user message, the backend calls Ollama with a special extraction prompt to pull out personal facts (name, preferences, goals, etc.) and stores them in the `memory` table.

4. **Memory injection**: On every chat request, the top-K most recent memories are injected into the system prompt so the AI "remembers" the user.

5. **Persistence**: Both conversation history and memories survive backend restarts because everything is stored in PostgreSQL.

## Contributing

Contributions are welcome! Please feel free to submit a Pull Request.

## License

This project is licensed under the MIT License – see the LICENSE file for details.
 