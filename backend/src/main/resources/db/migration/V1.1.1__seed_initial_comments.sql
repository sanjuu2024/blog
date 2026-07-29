-- Seed demonstration accounts and comments for a fresh database.
-- The demo account password hashes intentionally do not correspond to a published password.

INSERT INTO blog_user (
    username,
    nickname,
    email,
    password_hash,
    role,
    status,
    avatar_url,
    bio,
    email_verified,
    created_at,
    updated_at
)
VALUES
    (
        'demo_alice',
        'Alice',
        'demo_alice@example.com',
        '$2a$10$6gRy9E3P7OeJ1icmflX2o.A0aSpUpTnY8LA5wNoyo3QeiEZ7weXve',
        'USER',
        'ACTIVE',
        '',
        '用于展示评论功能的演示用户。',
        FALSE,
        NOW(),
        NOW()
    ),
    (
        'demo_bob',
        'Bob',
        'demo_bob@example.com',
        '$2a$10$6gRy9E3P7OeJ1icmflX2o.A0aSpUpTnY8LA5wNoyo3QeiEZ7weXve',
        'USER',
        'ACTIVE',
        '',
        '用于展示评论功能的演示用户。',
        FALSE,
        NOW(),
        NOW()
    ),
    (
        'demo_carol',
        'Carol',
        'demo_carol@example.com',
        '$2a$10$6gRy9E3P7OeJ1icmflX2o.A0aSpUpTnY8LA5wNoyo3QeiEZ7weXve',
        'USER',
        'ACTIVE',
        '',
        '用于展示评论功能的演示用户。',
        FALSE,
        NOW(),
        NOW()
    )
ON CONFLICT DO NOTHING;

WITH
target_article AS (
    SELECT id
    FROM blog_article
    WHERE slug = 'spring-boot-dual-token-login'
),
admin_user AS (
    SELECT id
    FROM blog_user
    WHERE LOWER(username) = LOWER('admin')
    LIMIT 1
),
alice AS (
    SELECT id FROM blog_user WHERE LOWER(username) = LOWER('demo_alice')
),
bob AS (
    SELECT id FROM blog_user WHERE LOWER(username) = LOWER('demo_bob')
),
carol AS (
    SELECT id FROM blog_user WHERE LOWER(username) = LOWER('demo_carol')
),
approved_root AS (
    INSERT INTO blog_comment (
        article_id, user_id, parent_id, root_id, content, status, created_at, updated_at
    )
    SELECT
        target_article.id,
        alice.id,
        NULL,
        NULL,
        '文章把 Access Token 与 Refresh Token 的职责划分得很清楚，刷新失败后的处理边界也很有参考价值。',
        'APPROVED',
        TIMESTAMPTZ '2026-05-06 10:00:00+08',
        TIMESTAMPTZ '2026-05-06 10:00:00+08'
    FROM target_article, alice
    RETURNING id, article_id
),
approved_reply AS (
    INSERT INTO blog_comment (
        article_id, user_id, parent_id, root_id, content, status, created_at, updated_at
    )
    SELECT
        approved_root.article_id,
        bob.id,
        approved_root.id,
        approved_root.id,
        '我也很关注这一点，刷新接口的失败分支是否统一会直接影响前端体验。',
        'APPROVED',
        TIMESTAMPTZ '2026-05-06 10:08:00+08',
        TIMESTAMPTZ '2026-05-06 10:08:00+08'
    FROM approved_root, bob
    RETURNING id, article_id, root_id
),
nested_approved_reply AS (
    INSERT INTO blog_comment (
        article_id, user_id, parent_id, root_id, content, status, created_at, updated_at
    )
    SELECT
        approved_reply.article_id,
        carol.id,
        approved_reply.id,
        approved_reply.root_id,
        '同意，尤其要避免刷新失败后前端不断重试，导致同一个请求被重复发送。',
        'APPROVED',
        TIMESTAMPTZ '2026-05-06 10:15:00+08',
        TIMESTAMPTZ '2026-05-06 10:15:00+08'
    FROM approved_reply, carol
    RETURNING id
),
second_approved_root AS (
    INSERT INTO blog_comment (
        article_id, user_id, parent_id, root_id, content, status, created_at, updated_at
    )
    SELECT
        target_article.id,
        bob.id,
        NULL,
        NULL,
        '我在自己的项目里也遇到过登录态续期的问题，这篇文章提供了很清晰的排查思路。',
        'APPROVED',
        TIMESTAMPTZ '2026-05-07 14:30:00+08',
        TIMESTAMPTZ '2026-05-07 14:30:00+08'
    FROM target_article, bob
    RETURNING id, article_id
),
admin_approved_reply AS (
    INSERT INTO blog_comment (
        article_id, user_id, parent_id, root_id, content, status, created_at, updated_at
    )
    SELECT
        second_approved_root.article_id,
        admin_user.id,
        second_approved_root.id,
        second_approved_root.id,
        '感谢反馈。后续会继续完善会话管理相关的实现和测试用例。',
        'APPROVED',
        TIMESTAMPTZ '2026-05-07 15:00:00+08',
        TIMESTAMPTZ '2026-05-07 15:00:00+08'
    FROM second_approved_root, admin_user
    RETURNING id
),
pending_root AS (
    INSERT INTO blog_comment (
        article_id, user_id, parent_id, root_id, content, status, created_at, updated_at
    )
    SELECT
        target_article.id,
        carol.id,
        NULL,
        NULL,
        '想请教一下，多端同时登录时是否需要为每个设备单独维护 Refresh Token？',
        'PENDING',
        TIMESTAMPTZ '2026-05-08 09:20:00+08',
        TIMESTAMPTZ '2026-05-08 09:20:00+08'
    FROM target_article, carol
    RETURNING id
),
rejected_root AS (
    INSERT INTO blog_comment (
        article_id, user_id, parent_id, root_id, content, status,
        reviewed_by, reviewed_at, moderation_reason, created_at, updated_at
    )
    SELECT
        target_article.id,
        alice.id,
        NULL,
        NULL,
        '欢迎互关，已私信联系方式。',
        'REJECTED',
        admin_user.id,
        TIMESTAMPTZ '2026-05-08 10:00:00+08',
        '内容与文章讨论无关',
        TIMESTAMPTZ '2026-05-08 09:30:00+08',
        TIMESTAMPTZ '2026-05-08 10:00:00+08'
    FROM target_article, alice, admin_user
    RETURNING id
),
hidden_root AS (
    INSERT INTO blog_comment (
        article_id, user_id, parent_id, root_id, content, status,
        reviewed_by, reviewed_at, moderation_reason, created_at, updated_at
    )
    SELECT
        target_article.id,
        bob.id,
        NULL,
        NULL,
        '这篇文章没有任何价值。',
        'HIDDEN',
        admin_user.id,
        TIMESTAMPTZ '2026-05-08 10:10:00+08',
        '包含不友善表达',
        TIMESTAMPTZ '2026-05-08 09:40:00+08',
        TIMESTAMPTZ '2026-05-08 10:10:00+08'
    FROM target_article, bob, admin_user
    RETURNING id
),
deleted_root AS (
    INSERT INTO blog_comment (
        article_id, user_id, parent_id, root_id, content, status,
        deleted_by, deleted_at, created_at, updated_at
    )
    SELECT
        target_article.id,
        carol.id,
        NULL,
        NULL,
        '这个问题我后来已经自己解决了，因此删除原来的留言。',
        'DELETED',
        carol.id,
        TIMESTAMPTZ '2026-05-08 11:00:00+08',
        TIMESTAMPTZ '2026-05-08 09:50:00+08',
        TIMESTAMPTZ '2026-05-08 11:00:00+08'
    FROM target_article, carol
    RETURNING id, article_id
),
deleted_reply AS (
    INSERT INTO blog_comment (
        article_id, user_id, parent_id, root_id, content, status,
        deleted_by, deleted_at, created_at, updated_at
    )
    SELECT
        deleted_root.article_id,
        alice.id,
        deleted_root.id,
        deleted_root.id,
        '我原本也遇到了类似问题，后来通过重新登录解决了。',
        'DELETED',
        carol.id,
        TIMESTAMPTZ '2026-05-08 11:00:00+08',
        TIMESTAMPTZ '2026-05-08 09:51:00+08',
        TIMESTAMPTZ '2026-05-08 11:00:00+08'
    FROM deleted_root, alice, carol
    RETURNING id
)
UPDATE blog_article article
SET comment_count = (
    SELECT COUNT(*)
    FROM blog_comment comment
    WHERE comment.article_id = article.id
      AND comment.status = 'APPROVED'
)
WHERE article.id IN (SELECT id FROM target_article);
