-- Enable pgvector extension (required: install pgvector in PostgreSQL first)
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
