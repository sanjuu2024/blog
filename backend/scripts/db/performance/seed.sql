\set ON_ERROR_STOP on

DO $$
BEGIN
    IF current_database() NOT LIKE '%performance%' THEN
        RAISE EXCEPTION 'Performance seed must run against a database whose name contains "performance"; current database: %', current_database();
    END IF;
    IF NOT EXISTS (SELECT 1 FROM flyway_schema_history WHERE version = '1.1.7' AND success = TRUE) THEN
        RAISE EXCEPTION 'Run Flyway through version 1.1.7 before seeding performance data';
    END IF;
    IF NOT EXISTS (SELECT 1 FROM blog_user WHERE LOWER(username) = 'admin' AND role = 'ADMIN' AND status = 'ACTIVE') THEN
        RAISE EXCEPTION 'An active admin account is required before seeding performance data';
    END IF;
END
$$;

BEGIN;

-- 每次先清理旧性能数据，使数据量和关联关系可重复；保留独立 cleanup.sql 供只清理时使用。
DELETE FROM blog_auth_session WHERE user_id IN (SELECT id FROM blog_user WHERE username LIKE 'perf_user_%');
DELETE FROM blog_admin_audit_log WHERE operator_username LIKE 'perf_user_%';
DELETE FROM blog_message_board WHERE content LIKE '[PERF]%';
DELETE FROM blog_comment
WHERE article_id IN (SELECT id FROM blog_article WHERE slug LIKE 'perf-article-%')
   OR user_id IN (SELECT id FROM blog_user WHERE username LIKE 'perf_user_%');
DELETE FROM blog_article_like
WHERE article_id IN (SELECT id FROM blog_article WHERE slug LIKE 'perf-article-%')
   OR user_id IN (SELECT id FROM blog_user WHERE username LIKE 'perf_user_%');
DELETE FROM blog_article_favorite
WHERE article_id IN (SELECT id FROM blog_article WHERE slug LIKE 'perf-article-%')
   OR user_id IN (SELECT id FROM blog_user WHERE username LIKE 'perf_user_%');
DELETE FROM blog_article_tag WHERE article_id IN (SELECT id FROM blog_article WHERE slug LIKE 'perf-article-%');
DELETE FROM blog_article_tag WHERE tag_id IN (SELECT id FROM blog_tag WHERE name LIKE 'PERF-%');
DELETE FROM blog_article WHERE slug LIKE 'perf-article-%';
DELETE FROM blog_tag WHERE name LIKE 'PERF-%';
DELETE FROM blog_category
WHERE name = 'PERF综合'
  AND parent_id IN (SELECT id FROM blog_category WHERE name = 'PERF性能测试' AND parent_id IS NULL);
DELETE FROM blog_category WHERE name = 'PERF性能测试' AND parent_id IS NULL;
DELETE FROM blog_user WHERE username LIKE 'perf_user_%';

INSERT INTO blog_category (name, parent_id, level, description, sort_no, status, created_at, updated_at)
VALUES ('PERF性能测试', NULL, 1, '仅用于性能测试的一级分类', 900, 'ENABLED', NOW(), NOW());

INSERT INTO blog_category (name, parent_id, level, description, sort_no, status, created_at, updated_at)
SELECT 'PERF综合', id, 2, '仅用于性能测试的二级分类', 10, 'ENABLED', NOW(), NOW()
FROM blog_category WHERE name = 'PERF性能测试' AND parent_id IS NULL;

INSERT INTO blog_tag (name, description, status, created_at, updated_at)
VALUES
    ('PERF-Java', '仅用于性能测试的 Java 标签', 'ENABLED', NOW(), NOW()),
    ('PERF-Spring', '仅用于性能测试的 Spring 标签', 'ENABLED', NOW(), NOW()),
    ('PERF-Search', '仅用于性能测试的搜索标签', 'ENABLED', NOW(), NOW());

INSERT INTO blog_user (username, nickname, email, password_hash, role, status, token_version, avatar_url, bio, email_verified, created_at, updated_at)
SELECT
    'perf_user_' || LPAD(number::TEXT, 3, '0'),
    '性能用户' || number,
    'perf_user_' || LPAD(number::TEXT, 3, '0') || '@example.test',
    '$2a$10$2ZdiSFvpHOXLvfTZShhGbe65aEFhlaHI2OQwbane4ioJ3pXvghvNS',
    'USER', 'ACTIVE', 0, '', '仅用于 P1 性能测试的虚构用户', FALSE,
    NOW() - number * INTERVAL '1 hour', NOW() - number * INTERVAL '1 hour'
FROM generate_series(1, 100) AS numbers(number);

WITH admin_user AS (
    SELECT id FROM blog_user WHERE LOWER(username) = 'admin' LIMIT 1
), perf_category AS (
    SELECT child.id
    FROM blog_category child
    JOIN blog_category parent ON parent.id = child.parent_id
    WHERE parent.name = 'PERF性能测试' AND parent.parent_id IS NULL AND child.name = 'PERF综合'
    LIMIT 1
)
INSERT INTO blog_article (title, slug, summary, content_md, content_html, content_text, cover_url, status, category_id, author_id, is_top, allow_comment, view_count, comment_count, like_count, favorite_count, published_at, created_at, updated_at)
SELECT
    '[PERF] Spring Boot 性能基线文章 ' || number,
    'perf-article-' || LPAD(number::TEXT, 3, '0'),
    '用于中文全文搜索和文章列表压测的摘要，包含 Spring Boot、Redis、PostgreSQL 与性能基线关键词。',
    '# 性能基线文章 ' || number || E'\n\n这是一篇用于压测的文章。\n\n## 技术栈\n\nSpring Boot、Redis、PostgreSQL、Vue。',
    '<h1>性能基线文章 ' || number || '</h1><p>这是一篇用于压测的文章。</p><h2>技术栈</h2><p>Spring Boot、Redis、PostgreSQL、Vue。</p>',
    '性能基线文章 ' || number || '。这是一篇用于中文全文搜索的压测文章，包含 Spring Boot、Redis、PostgreSQL、Vue、性能基线和中文搜索关键词。',
    '', 'PUBLISHED', perf_category.id, admin_user.id, number <= 5, TRUE, number * 10, 0, 0, 0,
    NOW() - number * INTERVAL '1 hour', NOW() - number * INTERVAL '1 hour', NOW() - number * INTERVAL '1 hour'
FROM generate_series(1, 100) AS numbers(number)
CROSS JOIN admin_user
CROSS JOIN perf_category;

INSERT INTO blog_article_tag (article_id, tag_id, created_at)
SELECT article.id, tag.id, NOW()
FROM blog_article article
CROSS JOIN blog_tag tag
WHERE article.slug LIKE 'perf-article-%'
  AND tag.name IN ('PERF-Java', 'PERF-Spring', 'PERF-Search');

WITH admin_user AS (
    SELECT id FROM blog_user WHERE LOWER(username) = 'admin' LIMIT 1
), perf_articles AS (
    SELECT id, ROW_NUMBER() OVER (ORDER BY id) AS row_number
    FROM blog_article WHERE slug LIKE 'perf-article-%'
), perf_users AS (
    SELECT id, ROW_NUMBER() OVER (ORDER BY id) AS row_number
    FROM blog_user WHERE username LIKE 'perf_user_%'
), inserted_roots AS (
    INSERT INTO blog_comment (article_id, user_id, parent_id, root_id, content, status, reviewed_by, reviewed_at, created_at, updated_at)
    SELECT
        article.id, user_account.id, NULL, NULL,
        '[PERF] 性能测试顶层评论 ' || number || '，用于验证评论分页、作者映射和回复计数。',
        'APPROVED', admin_user.id, NOW() - number * INTERVAL '1 minute',
        NOW() - number * INTERVAL '1 minute', NOW() - number * INTERVAL '1 minute'
    FROM generate_series(1, 1000) AS numbers(number)
    JOIN perf_articles article ON article.row_number = ((number - 1) % 100) + 1
    JOIN perf_users user_account ON user_account.row_number = ((number - 1) % 100) + 1
    CROSS JOIN admin_user
    RETURNING id, article_id
), numbered_roots AS (
    SELECT id, article_id, ROW_NUMBER() OVER (ORDER BY id) AS row_number FROM inserted_roots
)
INSERT INTO blog_comment (article_id, user_id, parent_id, root_id, content, status, reviewed_by, reviewed_at, created_at, updated_at)
SELECT
    root_comment.article_id, user_account.id, root_comment.id, root_comment.id,
    '[PERF] 性能测试回复 ' || root_comment.row_number || '，用于验证回复游标分页和回复计数。',
    'APPROVED', admin_user.id,
    NOW() - root_comment.row_number * INTERVAL '1 minute' + INTERVAL '30 seconds',
    NOW() - root_comment.row_number * INTERVAL '1 minute' + INTERVAL '30 seconds',
    NOW() - root_comment.row_number * INTERVAL '1 minute' + INTERVAL '30 seconds'
FROM numbered_roots root_comment
JOIN perf_users user_account ON user_account.row_number = (root_comment.row_number % 100) + 1
CROSS JOIN admin_user;

UPDATE blog_article article
SET comment_count = (SELECT COUNT(*) FROM blog_comment comment WHERE comment.article_id = article.id AND comment.status = 'APPROVED')
WHERE article.slug LIKE 'perf-article-%';

WITH admin_user AS (
    SELECT id FROM blog_user WHERE LOWER(username) = 'admin' LIMIT 1
), inserted_roots AS (
    INSERT INTO blog_message_board (user_id, parent_id, nickname, email, content, status, notify_on_reply, unsubscribe_token, reviewed_by, reviewed_at, created_at, updated_at)
    SELECT NULL, NULL, '性能游客' || number, '', '[PERF] 性能测试顶层留言 ' || number || '，用于验证留言分页和管理员回复组装。',
           'APPROVED', FALSE, NULL, admin_user.id, NOW() - number * INTERVAL '2 minutes',
           NOW() - number * INTERVAL '2 minutes', NOW() - number * INTERVAL '2 minutes'
    FROM generate_series(1, 400) AS numbers(number)
    CROSS JOIN admin_user
    RETURNING id
), numbered_roots AS (
    SELECT id, ROW_NUMBER() OVER (ORDER BY id) AS row_number FROM inserted_roots
)
INSERT INTO blog_message_board (user_id, parent_id, nickname, email, content, status, notify_on_reply, unsubscribe_token, created_at, updated_at)
SELECT admin_user.id, root_message.id, '', '', '[PERF] 管理员性能测试回复 ' || root_message.row_number, 'APPROVED', FALSE, NULL,
       NOW() - root_message.row_number * INTERVAL '2 minutes' + INTERVAL '1 minute', NOW() - root_message.row_number * INTERVAL '2 minutes' + INTERVAL '1 minute'
FROM numbered_roots root_message
CROSS JOIN admin_user
WHERE root_message.row_number <= 100;

COMMIT;

ANALYZE blog_user;
ANALYZE blog_category;
ANALYZE blog_tag;
ANALYZE blog_article;
ANALYZE blog_article_tag;
ANALYZE blog_comment;
ANALYZE blog_message_board;

SELECT
    (SELECT COUNT(*) FROM blog_user WHERE username LIKE 'perf_user_%') AS perf_users,
    (SELECT COUNT(*) FROM blog_article WHERE slug LIKE 'perf-article-%') AS perf_articles,
    (SELECT COUNT(*) FROM blog_comment WHERE content LIKE '[PERF]%') AS perf_comments,
    (SELECT COUNT(*) FROM blog_message_board WHERE content LIKE '[PERF]%') AS perf_messages,
    (SELECT COUNT(*) FROM blog_article WHERE slug LIKE 'perf-article-%' AND status = 'PUBLISHED') AS published_perf_articles,
    (SELECT COUNT(*) FROM blog_comment WHERE content LIKE '[PERF]%' AND status = 'APPROVED') AS approved_perf_comments,
    (SELECT COUNT(*) FROM blog_message_board WHERE content LIKE '[PERF]%' AND status = 'APPROVED') AS approved_perf_messages;

SELECT id AS published_article_id FROM blog_article WHERE slug = 'perf-article-001';
SELECT id AS root_comment_id FROM blog_comment WHERE parent_id IS NULL AND content LIKE '[PERF]%' ORDER BY id LIMIT 1;
SELECT username, '123456' AS password FROM blog_user WHERE username = 'perf_user_001';
