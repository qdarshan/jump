CREATE TABLE IF NOT EXISTS jump_link
(
    code       TEXT PRIMARY KEY,
    url        TEXT NOT NULL,
    created_by TEXT NOT NULL,
    created_at TEXT NOT NULL,
    expires_at TEXT NOT NULL
);

CREATE INDEX IF NOT EXISTS idx_jump_link_expires_at ON jump_link (expires_at);