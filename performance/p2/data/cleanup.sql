\set ON_ERROR_STOP on

DO $$
BEGIN
    IF current_database() <> 'blog_p2_performance' THEN
        RAISE EXCEPTION 'P2 cleanup only accepts blog_p2_performance; actual: %', current_database();
    END IF;
    IF NOT EXISTS (SELECT 1 FROM flyway_schema_history WHERE version = '1.2.13' AND success) THEN
        RAISE EXCEPTION 'Apply P2 migrations through 1.2.13 first';
    END IF;
END
$$;

BEGIN;
CREATE TEMP TABLE p2_cleanup_users ON COMMIT DROP AS
SELECT id FROM blog_user WHERE starts_with(username, 'p2_perf_user_');
CREATE TEMP TABLE p2_cleanup_articles ON COMMIT DROP AS
SELECT id FROM blog_article WHERE starts_with(slug, 'p2-perf-article-');
CREATE TEMP TABLE p2_cleanup_comments ON COMMIT DROP AS
SELECT id FROM blog_comment WHERE article_id IN (SELECT id FROM p2_cleanup_articles)
    OR user_id IN (SELECT id FROM p2_cleanup_users);
CREATE TEMP TABLE p2_cleanup_messages ON COMMIT DROP AS
WITH RECURSIVE targets AS (
    SELECT id FROM blog_message_board WHERE starts_with(content, '[P2-PERF]')
        OR user_id IN (SELECT id FROM p2_cleanup_users)
    UNION
    SELECT reply.id FROM blog_message_board reply JOIN targets ON reply.parent_id = targets.id
)
SELECT id FROM targets;
CREATE TEMP TABLE p2_cleanup_notifications ON COMMIT DROP AS
SELECT id FROM blog_notification WHERE starts_with(title, '[P2-PERF]')
    OR created_by IN (SELECT id FROM p2_cleanup_users)
    OR (source_type = 'COMMENT' AND source_id IN (SELECT id FROM p2_cleanup_comments))
    OR (source_type = 'MESSAGE' AND source_id IN (SELECT id FROM p2_cleanup_messages));

DELETE FROM blog_notification_recipient WHERE notification_id IN (SELECT id FROM p2_cleanup_notifications)
    OR user_id IN (SELECT id FROM p2_cleanup_users);
DELETE FROM blog_notification WHERE id IN (SELECT id FROM p2_cleanup_notifications);
DELETE FROM blog_mail_delivery WHERE recipient_user_id IN (SELECT id FROM p2_cleanup_users)
    OR (mail_type = 'COMMENT_REPLY' AND (source_id IN (SELECT id FROM p2_cleanup_comments) OR reply_id IN (SELECT id FROM p2_cleanup_comments)))
    OR (mail_type = 'MESSAGE_REPLY' AND (source_id IN (SELECT id FROM p2_cleanup_messages) OR reply_id IN (SELECT id FROM p2_cleanup_messages)));
DELETE FROM blog_comment_like WHERE comment_id IN (SELECT id FROM p2_cleanup_comments)
    OR user_id IN (SELECT id FROM p2_cleanup_users);
DELETE FROM blog_article_daily_stat WHERE article_id IN (SELECT id FROM p2_cleanup_articles);
DELETE FROM blog_article_like WHERE article_id IN (SELECT id FROM p2_cleanup_articles)
    OR user_id IN (SELECT id FROM p2_cleanup_users);
DELETE FROM blog_article_favorite WHERE article_id IN (SELECT id FROM p2_cleanup_articles)
    OR user_id IN (SELECT id FROM p2_cleanup_users);
DELETE FROM blog_article_tag WHERE article_id IN (SELECT id FROM p2_cleanup_articles)
    OR tag_id IN (SELECT id FROM blog_tag WHERE starts_with(name, 'P2-PERF-'));
DELETE FROM blog_auth_session WHERE user_id IN (SELECT id FROM p2_cleanup_users);
DELETE FROM blog_security_event WHERE user_id IN (SELECT id FROM p2_cleanup_users)
    OR actor_id IN (SELECT id FROM p2_cleanup_users) OR starts_with(description, '[P2-PERF]');
DELETE FROM blog_admin_audit_log WHERE operator_id IN (SELECT id FROM p2_cleanup_users)
    OR (resource_type = 'NOTIFICATION' AND resource_id IN (SELECT id::text FROM p2_cleanup_notifications));
DELETE FROM blog_message_board WHERE id IN (SELECT id FROM p2_cleanup_messages);
DELETE FROM blog_comment WHERE id IN (SELECT id FROM p2_cleanup_comments);
DELETE FROM blog_article WHERE id IN (SELECT id FROM p2_cleanup_articles);
DELETE FROM blog_tag WHERE starts_with(name, 'P2-PERF-');
DELETE FROM blog_category WHERE parent_id IN (SELECT id FROM blog_category WHERE name = 'P2性能测试' AND parent_id IS NULL);
DELETE FROM blog_category WHERE name = 'P2性能测试' AND parent_id IS NULL;
DELETE FROM blog_user WHERE id IN (SELECT id FROM p2_cleanup_users);
COMMIT;

SELECT 'P2 cleanup complete; bootstrap administrator and Flyway history preserved' AS result;
