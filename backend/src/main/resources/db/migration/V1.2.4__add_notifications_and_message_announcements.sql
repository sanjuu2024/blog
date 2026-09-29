ALTER TABLE blog_message_board
    ADD COLUMN IF NOT EXISTS is_announcement BOOLEAN NOT NULL DEFAULT FALSE,
    ADD COLUMN IF NOT EXISTS is_pinned BOOLEAN NOT NULL DEFAULT FALSE;

CREATE INDEX IF NOT EXISTS idx_blog_message_board_public_sort
    ON blog_message_board (parent_id, status, is_announcement DESC, is_pinned DESC, created_at DESC, id DESC);

CREATE TABLE IF NOT EXISTS blog_notification (
    id BIGSERIAL PRIMARY KEY,
    type VARCHAR(30) NOT NULL
        CHECK (type IN ('COMMENT_REPLY', 'MESSAGE_REPLY', 'ADMIN_MESSAGE')),
    target_scope VARCHAR(30) NOT NULL
        CHECK (target_scope IN ('SELECTED_USERS', 'ALL_USERS')),
    title VARCHAR(100) NOT NULL,
    content TEXT NOT NULL CHECK (char_length(content) BETWEEN 1 AND 2000),
    status VARCHAR(20) NOT NULL DEFAULT 'PUBLISHED'
        CHECK (status IN ('DRAFT', 'PUBLISHED', 'OFFLINE')),
    source_type VARCHAR(30),
    source_id BIGINT,
    created_by BIGINT,
    published_at TIMESTAMPTZ,
    offline_at TIMESTAMPTZ,
    created_at TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    updated_at TIMESTAMPTZ NOT NULL DEFAULT NOW()
);

CREATE INDEX IF NOT EXISTS idx_blog_notification_created
    ON blog_notification (created_at DESC, id DESC);

CREATE INDEX IF NOT EXISTS idx_blog_notification_status_created
    ON blog_notification (status, created_at DESC, id DESC);

CREATE TABLE IF NOT EXISTS blog_notification_recipient (
    notification_id BIGINT NOT NULL,
    user_id BIGINT NOT NULL,
    read_at TIMESTAMPTZ,
    created_at TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    PRIMARY KEY (notification_id, user_id)
);

CREATE INDEX IF NOT EXISTS idx_blog_notification_recipient_user_created
    ON blog_notification_recipient (user_id, created_at DESC, notification_id DESC);

CREATE INDEX IF NOT EXISTS idx_blog_notification_recipient_unread
    ON blog_notification_recipient (user_id, created_at DESC)
    WHERE read_at IS NULL;
