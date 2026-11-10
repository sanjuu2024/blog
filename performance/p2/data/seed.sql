\set ON_ERROR_STOP on

-- Independent P2 fixtures; no schema change and no P1 includes.
\ir cleanup.sql

DO $$
BEGIN
    IF NOT EXISTS (SELECT 1 FROM blog_user WHERE role = 'ADMIN' AND status = 'ACTIVE' AND deleted_at IS NULL) THEN
        RAISE EXCEPTION 'Bootstrap an active test administrator before seeding';
    END IF;
END
$$;

BEGIN;
CREATE TEMP TABLE p2_admin ON COMMIT DROP AS
SELECT id FROM blog_user WHERE role = 'ADMIN' AND status = 'ACTIVE' AND deleted_at IS NULL ORDER BY id LIMIT 1;
INSERT INTO blog_category (name, level, description, sort_no, status)
VALUES ('P2性能测试', 1, '独立 P2 验收分类', 900, 'ENABLED');
INSERT INTO blog_category (name, parent_id, level, description, sort_no, status)
SELECT children.name, parent.id, 2, '独立 P2 二级分类', children.sort_no, 'ENABLED'
FROM (VALUES ('P2综合', 10), ('P2实践', 20)) children(name, sort_no)
CROSS JOIN blog_category parent WHERE parent.name = 'P2性能测试' AND parent.parent_id IS NULL;
INSERT INTO blog_tag (name, description, status)
VALUES ('P2-PERF-Java', 'P2 测试', 'ENABLED'), ('P2-PERF-Spring', 'P2 测试', 'ENABLED'), ('P2-PERF-Search', 'P2 测试', 'ENABLED');

-- Fixed password 123456 is intentionally public and restricted to isolated fixtures.
INSERT INTO blog_user (username, nickname, email, password_hash, role, status, token_version, avatar_url, bio, email_verified)
SELECT 'p2_perf_user_' || LPAD(n::text, 3, '0'), 'P2性能用户' || n,
    'p2_perf_user_' || LPAD(n::text, 3, '0') || '@example.invalid',
    '$2a$10$2ZdiSFvpHOXLvfTZShhGbe65aEFhlaHI2OQwbane4ioJ3pXvghvNS',
    'USER', 'ACTIVE', 0, '', '独立 P2 性能验收用户', FALSE FROM generate_series(1, 100) numbers(n);

INSERT INTO blog_article (title, slug, summary, content_md, content_html, content_text, cover_url,
    status, category_id, author_id, is_top, allow_comment, view_count, comment_count, like_count, published_at)
SELECT '[P2-PERF] Spring Boot 性能文章 ' || n, 'p2-perf-article-' || LPAD(n::text, 3, '0'),
    '性能测试摘要，含 Spring Boot、Redis、PostgreSQL 和中文搜索关键词。',
    '# P2 性能文章 ' || n || E'\n\n' || repeat('性能测试正文，Spring Boot、Redis、PostgreSQL。' || E'\n\n', 30),
    '<h1>P2 性能文章 ' || n || '</h1>' || repeat('<p>性能测试正文，Spring Boot、Redis、PostgreSQL。</p>', 30),
    repeat('性能测试正文，Spring Boot、Redis、PostgreSQL。', 30), '', 'PUBLISHED', category.id, admin_user.id,
    n <= 4, TRUE, n * 10, 0, 0,
    CASE WHEN n = 1 THEN TIMESTAMPTZ '2026-01-01 00:30:00+08'
         ELSE make_timestamptz(2023 + (n % 4), 7, 1 + (n % 27), 12, 0, 0, 'Asia/Shanghai') END
FROM generate_series(1, 100) numbers(n)
JOIN blog_category category ON category.name = CASE WHEN n % 2 = 0 THEN 'P2实践' ELSE 'P2综合' END
    AND category.parent_id IN (SELECT id FROM blog_category WHERE name = 'P2性能测试' AND parent_id IS NULL)
CROSS JOIN p2_admin admin_user;
INSERT INTO blog_article_tag (article_id, tag_id)
SELECT article.id, tag.id FROM blog_article article CROSS JOIN blog_tag tag
WHERE starts_with(article.slug, 'p2-perf-article-') AND starts_with(tag.name, 'P2-PERF-');

WITH articles AS (
    SELECT id, ROW_NUMBER() OVER (ORDER BY id) AS row_number FROM blog_article WHERE starts_with(slug, 'p2-perf-article-')
), users AS (
    SELECT id, ROW_NUMBER() OVER (ORDER BY id) AS row_number FROM blog_user WHERE starts_with(username, 'p2_perf_user_')
)
INSERT INTO blog_comment (article_id, user_id, content, status, reviewed_by, reviewed_at)
SELECT article.id, user_account.id, '[P2-PERF] 顶层评论 ' || n, 'APPROVED', admin_user.id, NOW()
FROM generate_series(1, 1000) numbers(n)
JOIN articles article ON article.row_number = (n - 1) % 100 + 1
JOIN users user_account ON user_account.row_number = (n - 1) % 100 + 1 CROSS JOIN p2_admin admin_user;
INSERT INTO blog_comment (article_id, user_id, parent_id, root_id, content, status, reviewed_by, reviewed_at)
SELECT root.article_id, admin_user.id, root.id, root.id, '[P2-PERF] 评论回复 ' || root.id,
    'APPROVED', admin_user.id, NOW() FROM blog_comment root CROSS JOIN p2_admin admin_user
WHERE root.parent_id IS NULL AND root.article_id IN (SELECT id FROM blog_article WHERE starts_with(slug, 'p2-perf-article-'));
UPDATE blog_article article SET comment_count = (SELECT COUNT(*) FROM blog_comment item WHERE item.article_id = article.id AND item.status = 'APPROVED')
WHERE starts_with(article.slug, 'p2-perf-article-');

INSERT INTO blog_message_board (nickname, email, content, status, notify_on_reply, reviewed_by, reviewed_at)
SELECT 'P2游客' || n, '', '[P2-PERF] 顶层留言 ' || n, 'APPROVED', FALSE, admin_user.id, NOW()
FROM generate_series(1, 400) numbers(n) CROSS JOIN p2_admin admin_user;
INSERT INTO blog_message_board (user_id, parent_id, nickname, email, content, status, notify_on_reply)
SELECT admin_user.id, root.id, 'P2管理员', '', '[P2-PERF] 留言回复 ' || root.id, 'APPROVED', FALSE
FROM (SELECT id FROM blog_message_board WHERE parent_id IS NULL AND starts_with(content, '[P2-PERF]') ORDER BY id LIMIT 100) root
CROSS JOIN p2_admin admin_user;

INSERT INTO blog_article_daily_stat (article_id, stat_date, view_count)
SELECT id, (published_at AT TIME ZONE 'Asia/Shanghai')::date, view_count FROM blog_article WHERE starts_with(slug, 'p2-perf-article-');
INSERT INTO blog_article_like (article_id, user_id)
SELECT article.id, user_account.id FROM blog_article article
CROSS JOIN (SELECT id FROM blog_user WHERE starts_with(username, 'p2_perf_user_') ORDER BY id LIMIT 10) user_account
WHERE starts_with(article.slug, 'p2-perf-article-');
UPDATE blog_article article SET like_count = (SELECT COUNT(*) FROM blog_article_like item WHERE item.article_id = article.id)
WHERE starts_with(article.slug, 'p2-perf-article-');
INSERT INTO blog_comment_like (comment_id, user_id)
SELECT comment.id, user_account.id FROM blog_comment comment
CROSS JOIN (SELECT id FROM blog_user WHERE starts_with(username, 'p2_perf_user_') ORDER BY id LIMIT 3) user_account
WHERE comment.article_id IN (SELECT id FROM blog_article WHERE starts_with(slug, 'p2-perf-article-'));
UPDATE blog_comment comment SET like_count = (SELECT COUNT(*) FROM blog_comment_like item WHERE item.comment_id = comment.id)
WHERE comment.article_id IN (SELECT id FROM blog_article WHERE starts_with(slug, 'p2-perf-article-'));

WITH inserted AS (
    INSERT INTO blog_notification (type, target_scope, title, content, status, created_by, published_at)
    SELECT 'ADMIN_MESSAGE', 'ALL_USERS', '[P2-PERF] 管理员消息 ' || n, '[P2-PERF] 多页通知和全部已读测试',
        'PUBLISHED', admin_user.id, NOW() FROM generate_series(1, 200) numbers(n) CROSS JOIN p2_admin admin_user RETURNING id
)
INSERT INTO blog_notification_recipient (notification_id, user_id)
SELECT inserted.id, user_account.id FROM inserted CROSS JOIN blog_user user_account WHERE starts_with(user_account.username, 'p2_perf_user_');
WITH inserted AS (
    INSERT INTO blog_notification (type, target_scope, title, content, status, source_type, source_id, published_at)
    SELECT 'COMMENT_REPLY', 'SELECTED_USERS', '[P2-PERF] 评论回复', reply.content, 'PUBLISHED', 'COMMENT', reply.id, NOW()
    FROM blog_comment reply WHERE reply.parent_id IS NOT NULL
        AND reply.article_id IN (SELECT id FROM blog_article WHERE starts_with(slug, 'p2-perf-article-')) RETURNING id, source_id
)
INSERT INTO blog_notification_recipient (notification_id, user_id)
SELECT inserted.id, original.user_id FROM inserted JOIN blog_comment reply ON reply.id = inserted.source_id
JOIN blog_comment original ON original.id = reply.parent_id;

-- Read fixtures do not schedule mail and must never be used as retry fixtures.
INSERT INTO blog_mail_delivery (mail_type, source_id, reply_id, recipient_email, recipient_user_id, status, attempt_count,
    last_error_type, last_error_message, last_attempt_at, sent_at)
SELECT 'COMMENT_REPLY', original.id, reply.id, recipient.email, recipient.id,
    CASE WHEN reply.id % 2 = 0 THEN 'FAILED' ELSE 'SENT' END, 1,
    CASE WHEN reply.id % 2 = 0 THEN 'P2_PERF_FIXTURE' ELSE NULL END,
    CASE WHEN reply.id % 2 = 0 THEN '[P2-PERF] Synthetic read fixture' ELSE NULL END,
    NOW(), CASE WHEN reply.id % 2 <> 0 THEN NOW() ELSE NULL END
FROM blog_comment reply JOIN blog_comment original ON original.id = reply.parent_id
JOIN blog_user recipient ON recipient.id = original.user_id
WHERE reply.article_id IN (SELECT id FROM blog_article WHERE starts_with(slug, 'p2-perf-article-')) ORDER BY reply.id LIMIT 300;
INSERT INTO blog_security_event (event_type, outcome, user_id, account, ip, user_agent, request_method, request_path, description, created_at)
SELECT 'LOGIN', CASE WHEN n % 2 = 0 THEN 'FAILURE' ELSE 'SUCCESS' END, user_account.id, user_account.username,
    '127.0.0.1', 'P2 performance fixture', 'POST', '/api/v1/auth/login', '[P2-PERF] Synthetic security event', NOW() - n * INTERVAL '1 minute'
FROM generate_series(1, 2000) numbers(n)
JOIN (SELECT id, username, ROW_NUMBER() OVER (ORDER BY id) AS row_number FROM blog_user WHERE starts_with(username, 'p2_perf_user_')) user_account
    ON user_account.row_number = (n - 1) % 100 + 1;
COMMIT;

ANALYZE blog_user;
ANALYZE blog_category;
ANALYZE blog_tag;
ANALYZE blog_article;
ANALYZE blog_article_tag;
ANALYZE blog_comment;
ANALYZE blog_message_board;
ANALYZE blog_article_like;
ANALYZE blog_comment_like;
ANALYZE blog_article_daily_stat;
ANALYZE blog_notification;
ANALYZE blog_notification_recipient;
ANALYZE blog_mail_delivery;
ANALYZE blog_security_event;

SELECT 'articleId' AS property, id::text AS value FROM blog_article WHERE slug = 'p2-perf-article-001'
UNION ALL SELECT 'bootstrapArticleId', id::text FROM blog_article WHERE slug = 'p2-perf-article-002'
UNION ALL SELECT 'viewArticleId', id::text FROM blog_article WHERE slug = 'p2-perf-article-003'
UNION ALL SELECT 'commentId', MIN(id)::text FROM blog_comment WHERE starts_with(content, '[P2-PERF]')
UNION ALL SELECT 'categoryId', id::text FROM blog_category WHERE name = 'P2综合'
UNION ALL SELECT 'tagId', id::text FROM blog_tag WHERE name = 'P2-PERF-Java';
SELECT 'articles' AS kind, COUNT(*) FROM blog_article WHERE starts_with(slug, 'p2-perf-article-')
UNION ALL SELECT 'comments', COUNT(*) FROM blog_comment WHERE starts_with(content, '[P2-PERF]')
UNION ALL SELECT 'messages', COUNT(*) FROM blog_message_board WHERE starts_with(content, '[P2-PERF]')
UNION ALL SELECT 'users', COUNT(*) FROM blog_user WHERE starts_with(username, 'p2_perf_user_')
UNION ALL SELECT 'notifications', COUNT(*) FROM blog_notification WHERE starts_with(title, '[P2-PERF]')
UNION ALL SELECT 'notification_recipients', COUNT(*) FROM blog_notification_recipient
    WHERE notification_id IN (SELECT id FROM blog_notification WHERE starts_with(title, '[P2-PERF]'))
UNION ALL SELECT 'mail_deliveries', COUNT(*) FROM blog_mail_delivery
    WHERE reply_id IN (SELECT id FROM blog_comment WHERE starts_with(content, '[P2-PERF]')) AND mail_type = 'COMMENT_REPLY'
UNION ALL SELECT 'article_likes', COUNT(*) FROM blog_article_like
    WHERE article_id IN (SELECT id FROM blog_article WHERE starts_with(slug, 'p2-perf-article-'))
UNION ALL SELECT 'comment_likes', COUNT(*) FROM blog_comment_like
    WHERE comment_id IN (SELECT id FROM blog_comment WHERE starts_with(content, '[P2-PERF]'))
UNION ALL SELECT 'security_events', COUNT(*) FROM blog_security_event WHERE starts_with(description, '[P2-PERF]');
