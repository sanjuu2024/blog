\set ON_ERROR_STOP on

-- P2 read-only snapshots: run before/after each write scenario using metadata.json IDs.
BEGIN READ ONLY;
DO $$
BEGIN
    IF current_database() <> 'blog_p2_performance' THEN
        RAISE EXCEPTION 'P2 snapshots only accept blog_p2_performance';
    END IF;
END
$$;
SELECT current_database() AS database, NOW() AS snapshot_time;

SELECT article.id, article.slug, article.view_count, article.like_count,
    (SELECT COUNT(*) FROM blog_article_like item WHERE item.article_id = article.id) AS like_details,
    (SELECT COALESCE(SUM(stat.view_count), 0) FROM blog_article_daily_stat stat WHERE stat.article_id = article.id) AS daily_view_sum
FROM blog_article article WHERE article.slug LIKE 'p2-perf-article-%' ORDER BY article.id;

SELECT notification.id, notification.title, notification.status, COUNT(recipient.user_id) AS recipients,
    COUNT(recipient.read_at) AS read_recipients
FROM blog_notification notification LEFT JOIN blog_notification_recipient recipient ON recipient.notification_id = notification.id
WHERE notification.title LIKE '[P2-PERF]%'
GROUP BY notification.id ORDER BY notification.id DESC LIMIT 20;

SELECT notification_id, user_id, COUNT(*) AS duplicate_count
FROM blog_notification_recipient GROUP BY notification_id, user_id HAVING COUNT(*) > 1;

-- ALL_USERS includes disabled users, excludes deleted users and the publishing administrator.
SELECT notification.id, notification.created_by,
    (SELECT COUNT(*) FROM blog_user WHERE deleted_at IS NULL AND id <> notification.created_by) AS expected_recipients,
    (SELECT COUNT(*) FROM blog_notification_recipient WHERE notification_id = notification.id) AS actual_recipients
FROM blog_notification notification
WHERE notification.type = 'ADMIN_MESSAGE' AND notification.target_scope = 'ALL_USERS'
    AND notification.status = 'PUBLISHED' AND starts_with(notification.title, '[P2-PERF]')
ORDER BY notification.id DESC LIMIT 20;

WITH expected AS (
    SELECT notification.id AS notification_id, recipient.id AS user_id
    FROM blog_notification notification JOIN blog_user recipient
        ON recipient.deleted_at IS NULL AND recipient.id <> notification.created_by
    WHERE notification.type = 'ADMIN_MESSAGE' AND notification.target_scope = 'ALL_USERS'
        AND notification.status = 'PUBLISHED' AND starts_with(notification.title, '[P2-PERF]')
), actual AS (
    SELECT recipient.notification_id, recipient.user_id
    FROM blog_notification_recipient recipient JOIN blog_notification notification ON notification.id = recipient.notification_id
    WHERE notification.type = 'ADMIN_MESSAGE' AND notification.target_scope = 'ALL_USERS'
        AND notification.status = 'PUBLISHED' AND starts_with(notification.title, '[P2-PERF]')
)
(SELECT 'MISSING' AS difference, * FROM expected EXCEPT SELECT 'MISSING', * FROM actual)
UNION ALL
(SELECT 'UNEXPECTED' AS difference, * FROM actual EXCEPT SELECT 'UNEXPECTED', * FROM expected)
ORDER BY notification_id, user_id;

SELECT recipient.user_id, COUNT(*) FILTER (WHERE recipient.read_at IS NULL) AS unread
FROM blog_notification_recipient recipient JOIN blog_notification notification ON notification.id = recipient.notification_id
WHERE notification.title LIKE '[P2-PERF]%' GROUP BY recipient.user_id ORDER BY recipient.user_id;

SELECT delivery.id, delivery.status, delivery.attempt_count, delivery.last_attempt_at
FROM blog_mail_delivery delivery
WHERE delivery.source_id IN (SELECT id FROM blog_comment WHERE content LIKE '[P2-PERF]%')
   OR delivery.source_id IN (SELECT id FROM blog_message_board WHERE content LIKE '[P2-PERF]%')
ORDER BY delivery.id DESC LIMIT 30;

SELECT COUNT(*) AS perf_comments FROM blog_comment WHERE content LIKE '[P2-PERF]%';
SELECT COUNT(*) AS perf_messages FROM blog_message_board WHERE content LIKE '[P2-PERF]%';
SELECT stat.article_id, stat.stat_date, stat.view_count FROM blog_article_daily_stat stat
WHERE stat.article_id IN (SELECT id FROM blog_article WHERE starts_with(slug, 'p2-perf-article-'))
ORDER BY stat.article_id, stat.stat_date;
SELECT item.article_id, item.user_id, item.visitor_token_hash FROM blog_article_like item
WHERE item.article_id IN (SELECT id FROM blog_article WHERE starts_with(slug, 'p2-perf-article-'))
ORDER BY item.article_id, item.user_id NULLS LAST, item.visitor_token_hash;
COMMIT;
