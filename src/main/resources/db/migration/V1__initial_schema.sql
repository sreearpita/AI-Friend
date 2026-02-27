-- Enable pgvector extension (requires pgvector/pgvector:pg16 image or manual install)
CREATE EXTENSION IF NOT EXISTS vector;

CREATE TABLE app_user (
    id         UUID        PRIMARY KEY,
    created_at TIMESTAMP   NOT NULL DEFAULT CURRENT_TIMESTAMP
);

CREATE TABLE conversation (
    id         UUID        PRIMARY KEY,
    user_id    UUID        NOT NULL REFERENCES app_user(id),
    title      VARCHAR(255),
    created_at TIMESTAMP   NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP   NOT NULL DEFAULT CURRENT_TIMESTAMP
);

CREATE TABLE message (
    id              UUID        PRIMARY KEY,
    conversation_id UUID        NOT NULL REFERENCES conversation(id),
    role            VARCHAR(50) NOT NULL,
    content         TEXT        NOT NULL,
    created_at      TIMESTAMP   NOT NULL DEFAULT CURRENT_TIMESTAMP
);

CREATE TABLE memory (
    id                UUID        PRIMARY KEY,
    user_id           UUID        NOT NULL REFERENCES app_user(id),
    category          VARCHAR(100),
    fact              TEXT        NOT NULL,
    confidence        REAL,
    created_at        TIMESTAMP   NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at        TIMESTAMP   NOT NULL DEFAULT CURRENT_TIMESTAMP,
    last_confirmed_at TIMESTAMP
);

-- Indexes for common query patterns
CREATE INDEX idx_conversation_user_id ON conversation(user_id);
CREATE INDEX idx_message_conversation_created ON message(conversation_id, created_at DESC);
CREATE INDEX idx_memory_user_updated ON memory(user_id, updated_at DESC);
