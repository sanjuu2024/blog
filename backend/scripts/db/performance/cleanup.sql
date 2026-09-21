\set ON_ERROR_STOP on

DO $$
BEGIN
    IF current_database() NOT LIKE '%performance%' THEN
        RAISE EXCEPTION 'Performance cleanup must run against a database whose name contains "performance"; current database: %', current_database();
    END IF;
END
$$;

BEGIN;

DELETE FROM blog_auth_session
WHERE user_id IN (SELECT id FROM blog_user WHERE username LIKE 'perf_user_%');

DELETE FROM blog_admin_audit_log
WHERE operator_username LIKE 'perf_user_%';

DELETE FROM blog_message_board
WHERE content LIKE '[PERF]%';

DELETE FROM blog_comment
WHERE article_id IN (SELECT id FROM blog_article WHERE slug LIKE 'perf-article-%')
   OR user_id IN (SELECT id FROM blog_user WHERE username LIKE 'perf_user_%');

DELETE FROM blog_article_like
WHERE article_id IN (SELECT id FROM blog_article WHERE slug LIKE 'perf-article-%')
   OR user_id IN (SELECT id FROM blog_user WHERE username LIKE 'perf_user_%');

DELETE FROM blog_article_favorite
WHERE article_id IN (SELECT id FROM blog_article WHERE slug LIKE 'perf-article-%')
   OR user_id IN (SELECT id FROM blog_user WHERE username LIKE 'perf_user_%');

DELETE FROM blog_article_tag
WHERE article_id IN (SELECT id FROM blog_article WHERE slug LIKE 'perf-article-%');

DELETE FROM blog_article_tag
WHERE tag_id IN (SELECT id FROM blog_tag WHERE name LIKE 'PERF-%');

DELETE FROM blog_article
WHERE slug LIKE 'perf-article-%';

DELETE FROM blog_tag
WHERE name LIKE 'PERF-%';

DELETE FROM blog_category
WHERE name = 'PERF综合'
  AND parent_id IN (
      SELECT id FROM blog_category
      WHERE name = 'PERF性能测试' AND parent_id IS NULL
  );

DELETE FROM blog_category
WHERE name = 'PERF性能测试' AND parent_id IS NULL;

DELETE FROM blog_user
WHERE username LIKE 'perf_user_%';

COMMIT;

SELECT
    (SELECT COUNT(*) FROM blog_user WHERE username LIKE 'perf_user_%') AS remaining_perf_users,
    (SELECT COUNT(*) FROM blog_article WHERE slug LIKE 'perf-article-%') AS remaining_perf_articles,
    (SELECT COUNT(*) FROM blog_comment WHERE content LIKE '[PERF]%') AS remaining_perf_comments,
    (SELECT COUNT(*) FROM blog_message_board WHERE content LIKE '[PERF]%') AS remaining_perf_messages;
