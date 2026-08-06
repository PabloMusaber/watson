CREATE TABLE IF NOT EXISTS conversation_message (
    id          INTEGER PRIMARY KEY AUTOINCREMENT,
    message_id  TEXT NOT NULL,
    session_id  TEXT NOT NULL,
    role        TEXT NOT NULL,
    agent       TEXT NOT NULL,
    channel_id  TEXT NOT NULL,
    ts          TEXT NOT NULL,
    text        TEXT NOT NULL
);

CREATE INDEX IF NOT EXISTS idx_conv_msg_session_agent_ts ON conversation_message(session_id, agent, ts);
CREATE INDEX IF NOT EXISTS idx_conv_msg_channel_ts ON conversation_message(channel_id, ts);
CREATE INDEX IF NOT EXISTS idx_conv_msg_message_id ON conversation_message(message_id);

CREATE TABLE IF NOT EXISTS long_term_memory (
    id          INTEGER PRIMARY KEY AUTOINCREMENT,
    fact        TEXT    NOT NULL,
    category    TEXT    NOT NULL,
    saved_at    TEXT    NOT NULL
);

CREATE INDEX IF NOT EXISTS idx_ltm_category ON long_term_memory(category);
CREATE INDEX IF NOT EXISTS idx_ltm_saved_at ON long_term_memory(saved_at);
