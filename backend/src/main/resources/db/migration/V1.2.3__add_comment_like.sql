ALTER TABLE blog_comment
    ADD COLUMN IF NOT EXISTS like_count INTEGER NOT NULL DEFAULT 0;

ALTER TABLE blog_comment
    DROP CONSTRAINT IF EXISTS chk_blog_comment_like_count;

ALTER TABLE blog_comment
    ADD CONSTRAINT chk_blog_comment_like_count
        CHECK (like_count >= 0);

CREATE TABLE IF NOT EXISTS blog_comment_like (
    id BIGSERIAL PRIMARY KEY,
    comment_id BIGINT NOT NULL,
    user_id BIGINT NOT NULL,
    created_at TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    CONSTRAINT uq_blog_comment_like UNIQUE (comment_id, user_id)
);

CREATE INDEX IF NOT EXISTS idx_blog_comment_like_user_created
    ON blog_comment_like (user_id, created_at DESC);
