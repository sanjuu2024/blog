ALTER TABLE blog_article_like
    DROP CONSTRAINT IF EXISTS uq_blog_article_like;

ALTER TABLE blog_article_like
    ALTER COLUMN user_id DROP NOT NULL;

ALTER TABLE blog_article_like
    ADD COLUMN IF NOT EXISTS visitor_token_hash VARCHAR(71);

ALTER TABLE blog_article_like
    ADD CONSTRAINT chk_blog_article_like_actor
        CHECK ((user_id IS NOT NULL) <> (visitor_token_hash IS NOT NULL));

CREATE UNIQUE INDEX IF NOT EXISTS uq_blog_article_like_user
    ON blog_article_like (article_id, user_id)
    WHERE user_id IS NOT NULL;

CREATE UNIQUE INDEX IF NOT EXISTS uq_blog_article_like_visitor
    ON blog_article_like (article_id, visitor_token_hash)
    WHERE visitor_token_hash IS NOT NULL;
