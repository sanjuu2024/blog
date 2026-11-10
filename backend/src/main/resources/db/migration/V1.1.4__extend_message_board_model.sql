-- P1 留言板：审核状态、通知退订和逻辑删除字段。
ALTER TABLE blog_message_board
    DROP CONSTRAINT IF EXISTS blog_message_board_status_check;

ALTER TABLE blog_message_board
    ADD CONSTRAINT blog_message_board_status_check
        CHECK (status IN ('PENDING', 'APPROVED', 'REJECTED', 'HIDDEN', 'DELETED'));

ALTER TABLE blog_message_board
    ADD COLUMN IF NOT EXISTS notify_on_reply BOOLEAN NOT NULL DEFAULT FALSE,
    ADD COLUMN IF NOT EXISTS unsubscribe_token VARCHAR(128),
    ADD COLUMN IF NOT EXISTS moderation_reason VARCHAR(255),
    ADD COLUMN IF NOT EXISTS reviewed_by BIGINT,
    ADD COLUMN IF NOT EXISTS reviewed_at TIMESTAMPTZ,
    ADD COLUMN IF NOT EXISTS deleted_by BIGINT,
    ADD COLUMN IF NOT EXISTS deleted_at TIMESTAMPTZ;

CREATE INDEX IF NOT EXISTS idx_blog_message_board_parent_status_created
    ON blog_message_board (parent_id, status, created_at, id);

CREATE UNIQUE INDEX IF NOT EXISTS uq_blog_message_board_unsubscribe_token
    ON blog_message_board (unsubscribe_token)
    WHERE unsubscribe_token IS NOT NULL;
