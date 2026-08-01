CREATE TABLE IF NOT EXISTS agent_conversation_history (
    id        INTEGER PRIMARY KEY AUTOINCREMENT,
    agent     TEXT NOT NULL,
    ts        TEXT NOT NULL,
    utterance TEXT NOT NULL,
    response  TEXT
);

CREATE INDEX IF NOT EXISTS idx_agent_history_agent_ts ON agent_conversation_history(agent, ts);

CREATE TABLE IF NOT EXISTS long_term_memory (
    id          INTEGER PRIMARY KEY AUTOINCREMENT,
    fact        TEXT    NOT NULL,
    category    TEXT    NOT NULL,
    saved_at    TEXT    NOT NULL
);

CREATE INDEX IF NOT EXISTS idx_ltm_category ON long_term_memory(category);
CREATE INDEX IF NOT EXISTS idx_ltm_saved_at ON long_term_memory(saved_at);
