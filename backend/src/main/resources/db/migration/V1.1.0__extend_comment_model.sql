ALTER TABLE blog_comment
    ADD COLUMN root_id BIGINT,
    ADD COLUMN reviewed_by BIGINT,
    ADD COLUMN reviewed_at TIMESTAMPTZ,
    ADD COLUMN moderation_reason VARCHAR(255),
    ADD COLUMN deleted_by BIGINT,
    ADD COLUMN deleted_at TIMESTAMPTZ;

ALTER TABLE blog_comment
    DROP CONSTRAINT IF EXISTS blog_comment_status_check;

ALTER TABLE blog_comment
    ADD CONSTRAINT blog_comment_status_check
        CHECK (status IN (
            'PENDING',
            'APPROVED',
            'REJECTED',
            'HIDDEN',
            'DELETED'
        ));

-- 兼容 migration 执行前可能已经存在的回复数据。
WITH RECURSIVE comment_tree AS (
    SELECT
        id AS comment_id,
        id AS root_comment_id
    FROM blog_comment
    WHERE parent_id IS NULL

    UNION ALL

    SELECT
        child.id,
        tree.root_comment_id
    FROM blog_comment child
    JOIN comment_tree tree
        ON child.parent_id = tree.comment_id
)
UPDATE blog_comment comment
SET root_id = tree.root_comment_id
FROM comment_tree tree
WHERE comment.id = tree.comment_id
  AND comment.parent_id IS NOT NULL;

CREATE INDEX idx_blog_comment_root_status_created_at
    ON blog_comment (root_id, status, created_at, id);

CREATE INDEX idx_blog_comment_top_level_paging
    ON blog_comment (article_id, status, created_at DESC, id DESC)
    WHERE parent_id IS NULL;

CREATE INDEX idx_blog_comment_user_article_created_at
    ON blog_comment (user_id, article_id, created_at DESC);
