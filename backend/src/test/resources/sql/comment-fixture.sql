-- Test fixture only. It is intentionally not a Flyway migration.
-- Future comment integration tests load it with @Sql before each test method.

TRUNCATE TABLE blog_comment RESTART IDENTITY;

UPDATE blog_article
SET comment_count = 0;

INSERT INTO blog_user (
    username,
    nickname,
    email,
    password_hash,
    role,
    status,
    avatar_url,
    bio,
    created_at,
    updated_at
)
VALUES
    (
        'cmt_alice',
        'Alice',
        'cmt_alice@example.com',
        '$2a$10$2ZdiSFvpHOXLvfTZShhGbe65aEFhlaHI2OQwbane4ioJ3pXvghvNS',
        'USER',
        'ACTIVE',
        '',
        '评论测试用户 Alice',
        NOW(),
        NOW()
    ),
    (
        'cmt_bob',
        'Bob',
        'cmt_bob@example.com',
        '$2a$10$2ZdiSFvpHOXLvfTZShhGbe65aEFhlaHI2OQwbane4ioJ3pXvghvNS',
        'USER',
        'ACTIVE',
        '',
        '评论测试用户 Bob',
        NOW(),
        NOW()
    ),
    (
        'cmt_carol',
        'Carol',
        'cmt_carol@example.com',
        '$2a$10$2ZdiSFvpHOXLvfTZShhGbe65aEFhlaHI2OQwbane4ioJ3pXvghvNS',
        'USER',
        'ACTIVE',
        '',
        '评论测试用户 Carol',
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
    SELECT id FROM blog_user WHERE LOWER(username) = LOWER('cmt_alice')
),
bob AS (
    SELECT id FROM blog_user WHERE LOWER(username) = LOWER('cmt_bob')
),
carol AS (
    SELECT id FROM blog_user WHERE LOWER(username) = LOWER('cmt_carol')
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
        '文章对 Access Token 与 Refresh Token 的说明很清楚，尤其是刷新失败后的处理策略。',
        'APPROVED',
        TIMESTAMPTZ '2026-07-22 09:00:00+08',
        TIMESTAMPTZ '2026-07-22 09:00:00+08'
    FROM target_article, alice
    RETURNING id, article_id
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
        '令牌轮转的部分很有启发，我也准备在自己的项目里补上会话失效控制。',
        'APPROVED',
        TIMESTAMPTZ '2026-07-22 09:10:00+08',
        TIMESTAMPTZ '2026-07-22 09:10:00+08'
    FROM target_article, bob
    RETURNING id, article_id
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
        TIMESTAMPTZ '2026-07-22 09:20:00+08',
        TIMESTAMPTZ '2026-07-22 09:20:00+08'
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
        TIMESTAMPTZ '2026-07-22 10:00:00+08',
        '内容与文章讨论无关',
        TIMESTAMPTZ '2026-07-22 09:30:00+08',
        TIMESTAMPTZ '2026-07-22 10:00:00+08'
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
        TIMESTAMPTZ '2026-07-22 10:10:00+08',
        '包含不友善表达',
        TIMESTAMPTZ '2026-07-22 09:40:00+08',
        TIMESTAMPTZ '2026-07-22 10:10:00+08'
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
        TIMESTAMPTZ '2026-07-22 11:00:00+08',
        TIMESTAMPTZ '2026-07-22 09:50:00+08',
        TIMESTAMPTZ '2026-07-22 11:00:00+08'
    FROM target_article, carol
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
        '我也很关注这一点，刷新接口的失败分支处理得是否统一会直接影响前端体验。',
        'APPROVED',
        TIMESTAMPTZ '2026-07-22 09:01:00+08',
        TIMESTAMPTZ '2026-07-22 09:01:00+08'
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
        '同意，尤其要避免刷新失败后前端不断重试造成重复请求。',
        'APPROVED',
        TIMESTAMPTZ '2026-07-22 09:02:00+08',
        TIMESTAMPTZ '2026-07-22 09:02:00+08'
    FROM approved_reply, carol
    RETURNING id
),
pending_reply AS (
    INSERT INTO blog_comment (
        article_id, user_id, parent_id, root_id, content, status, created_at, updated_at
    )
    SELECT
        approved_root.article_id,
        alice.id,
        approved_root.id,
        approved_root.id,
        '补充一个疑问：旧 Refresh Token 被撤销后，网络重试应该如何处理？',
        'PENDING',
        TIMESTAMPTZ '2026-07-22 09:03:00+08',
        TIMESTAMPTZ '2026-07-22 09:03:00+08'
    FROM approved_root, alice
    RETURNING id
),
rejected_reply AS (
    INSERT INTO blog_comment (
        article_id, user_id, parent_id, root_id, content, status,
        reviewed_by, reviewed_at, moderation_reason, created_at, updated_at
    )
    SELECT
        approved_root.article_id,
        carol.id,
        approved_root.id,
        approved_root.id,
        '重复发送相同内容。',
        'REJECTED',
        admin_user.id,
        TIMESTAMPTZ '2026-07-22 10:20:00+08',
        '重复发布无实际讨论价值的内容',
        TIMESTAMPTZ '2026-07-22 09:04:00+08',
        TIMESTAMPTZ '2026-07-22 10:20:00+08'
    FROM approved_root, carol, admin_user
    RETURNING id
),
hidden_reply AS (
    INSERT INTO blog_comment (
        article_id, user_id, parent_id, root_id, content, status,
        reviewed_by, reviewed_at, moderation_reason, created_at, updated_at
    )
    SELECT
        approved_root.article_id,
        bob.id,
        approved_root.id,
        approved_root.id,
        '请不要再继续讨论这个问题。',
        'HIDDEN',
        admin_user.id,
        TIMESTAMPTZ '2026-07-22 10:30:00+08',
        '语气不当',
        TIMESTAMPTZ '2026-07-22 09:05:00+08',
        TIMESTAMPTZ '2026-07-22 10:30:00+08'
    FROM approved_root, bob, admin_user
    RETURNING id
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
        TIMESTAMPTZ '2026-07-22 11:00:00+08',
        TIMESTAMPTZ '2026-07-22 09:51:00+08',
        TIMESTAMPTZ '2026-07-22 11:00:00+08'
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
