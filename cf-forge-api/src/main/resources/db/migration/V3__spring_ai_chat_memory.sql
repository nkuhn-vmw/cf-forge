CREATE TABLE IF NOT EXISTS ai_chat_memory (
    conversation_id VARCHAR(36) NOT NULL,
    content TEXT NOT NULL,
    type VARCHAR(10) NOT NULL,
    "timestamp" TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP
);

CREATE INDEX idx_chat_memory_conversation ON ai_chat_memory(conversation_id);
