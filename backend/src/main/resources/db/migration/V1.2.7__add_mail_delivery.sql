CREATE TABLE blog_mail_delivery (
    id BIGSERIAL PRIMARY KEY,
    mail_type VARCHAR(30) NOT NULL
        CHECK (mail_type IN ('COMMENT_REPLY', 'MESSAGE_REPLY')),
    source_id BIGINT NOT NULL,
    reply_id BIGINT NOT NULL,
    recipient_masked VARCHAR(255) NOT NULL,
    status VARCHAR(20) NOT NULL DEFAULT 'PENDING'
        CHECK (status IN ('PENDING', 'SENT', 'FAILED')),
    attempt_count INTEGER NOT NULL DEFAULT 0 CHECK (attempt_count >= 0),
    last_error_type VARCHAR(100),
    last_error_message VARCHAR(255),
    sent_at TIMESTAMPTZ,
    last_attempt_at TIMESTAMPTZ,
    created_at TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    updated_at TIMESTAMPTZ NOT NULL DEFAULT NOW()
);

CREATE INDEX idx_blog_mail_delivery_status_created
    ON blog_mail_delivery (status, created_at DESC, id DESC);
