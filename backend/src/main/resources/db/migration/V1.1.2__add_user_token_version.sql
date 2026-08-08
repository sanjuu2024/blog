ALTER TABLE blog_user
    ADD COLUMN IF NOT EXISTS token_version BIGINT NOT NULL DEFAULT 0;

ALTER TABLE blog_user
    ADD CONSTRAINT chk_blog_user_token_version_nonnegative
        CHECK (token_version >= 0);
