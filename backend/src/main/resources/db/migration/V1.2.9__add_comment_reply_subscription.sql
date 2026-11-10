ALTER TABLE blog_comment
    ADD COLUMN IF NOT EXISTS notify_on_reply BOOLEAN NOT NULL DEFAULT FALSE,
    ADD COLUMN IF NOT EXISTS unsubscribe_token VARCHAR(128);

CREATE UNIQUE INDEX IF NOT EXISTS uq_blog_comment_unsubscribe_token
    ON blog_comment (unsubscribe_token)
    WHERE unsubscribe_token IS NOT NULL;
