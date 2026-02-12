-- Spring AI Chat Memory table
CREATE TABLE IF NOT EXISTS SPRING_AI_CHAT_MEMORY (
    conversation_id VARCHAR(256) NOT NULL,
    content TEXT NOT NULL,
    type VARCHAR(10) NOT NULL,
    "timestamp" TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP
);

CREATE INDEX IF NOT EXISTS idx_spring_ai_chat_memory_conversation
    ON SPRING_AI_CHAT_MEMORY (conversation_id);

-- PGVector extension (required for vector store)
CREATE EXTENSION IF NOT EXISTS vector;
