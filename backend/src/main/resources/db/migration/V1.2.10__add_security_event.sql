CREATE TABLE blog_security_event (
    id BIGSERIAL PRIMARY KEY,
    event_type VARCHAR(50) NOT NULL,
    outcome VARCHAR(10) NOT NULL CHECK (outcome IN ('SUCCESS', 'FAILURE')),
    user_id BIGINT,
    actor_id BIGINT,
    account_mask VARCHAR(255),
    ip_hash VARCHAR(64),
    user_agent_hash VARCHAR(64),
    description VARCHAR(255) NOT NULL,
    created_at TIMESTAMPTZ NOT NULL
);

CREATE INDEX idx_blog_security_event_created
    ON blog_security_event (created_at DESC, id DESC);

CREATE INDEX idx_blog_security_event_type_outcome_created
    ON blog_security_event (event_type, outcome, created_at DESC);

CREATE INDEX idx_blog_security_event_user_created
    ON blog_security_event (user_id, created_at DESC);
