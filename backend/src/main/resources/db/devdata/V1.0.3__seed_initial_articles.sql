-- Seed initial article management data for local bootstrap and first deployment.
-- This migration also ensures a few second-level categories exist because
-- articles must be bound to second-level categories.

WITH required_categories(parent_name, name, description, sort_no, status) AS (
    VALUES
        ('技术', '算法', '算法训练、竞赛题解和思维整理', 10, 'ENABLED'),
        ('技术', 'Java', 'Java 与 Spring Boot 后端开发相关文章', 20, 'ENABLED'),
        ('技术', '前端三剑客', 'HTML、CSS、JavaScript 和 Vue 相关文章', 30, 'ENABLED'),
        ('杂记', '随笔', '日常记录、复盘和阶段性总结', 10, 'ENABLED')
)
INSERT INTO blog_category (
    name,
    parent_id,
    level,
    description,
    sort_no,
    status,
    created_at,
    updated_at
)
SELECT
    required_categories.name,
    parent_category.id,
    2,
    required_categories.description,
    required_categories.sort_no,
    required_categories.status,
    NOW(),
    NOW()
FROM required_categories
JOIN blog_category parent_category
    ON parent_category.parent_id IS NULL
   AND LOWER(parent_category.name) = LOWER(required_categories.parent_name)
WHERE NOT EXISTS (
    SELECT 1
    FROM blog_category existing_category
    WHERE existing_category.parent_id = parent_category.id
      AND LOWER(existing_category.name) = LOWER(required_categories.name)
);

WITH seed_articles(
    slug,
    title,
    summary,
    content_md,
    content_html,
    content_text,
    status,
    parent_category_name,
    category_name,
    is_top,
    allow_comment,
    view_count,
    comment_count,
    like_count,
    favorite_count,
    published_at,
    created_at,
    updated_at
) AS (
    VALUES
        (
            'codeforces-round-notes',
            'Codeforces 训练复盘：从读题到补题',
            '记录一次 Codeforces 训练后的复盘方法，包括读题、赛中取舍和赛后补题节奏。',
            E'# Codeforces 训练复盘：从读题到补题\n\n这篇文章整理一次 Codeforces 训练后的复盘流程。\n\n## 读题\n\n先确认约束范围，再判断可能的数据结构和复杂度。\n\n## 补题\n\n补题时保留错误思路，方便以后复盘。',
            '<h1>Codeforces 训练复盘：从读题到补题</h1><p>这篇文章整理一次 Codeforces 训练后的复盘流程。</p><h2>读题</h2><p>先确认约束范围，再判断可能的数据结构和复杂度。</p><h2>补题</h2><p>补题时保留错误思路，方便以后复盘。</p>',
            'Codeforces 训练复盘：从读题到补题 这篇文章整理一次 Codeforces 训练后的复盘流程。读题 先确认约束范围，再判断可能的数据结构和复杂度。补题 补题时保留错误思路，方便以后复盘。',
            'PUBLISHED',
            '技术',
            '算法',
            TRUE,
            TRUE,
            128,
            0,
            12,
            4,
            TIMESTAMPTZ '2026-05-01 20:00:00+08',
            TIMESTAMPTZ '2026-05-01 19:30:00+08',
            TIMESTAMPTZ '2026-05-01 20:00:00+08'
        ),
        (
            'atcoder-dp-practice',
            'AtCoder DP 练习笔记',
            '梳理 AtCoder DP 题单中的常见状态设计方式，作为动态规划训练的阶段性记录。',
            E'# AtCoder DP 练习笔记\n\n动态规划的难点通常不是转移，而是状态定义。\n\n## 状态设计\n\n先写出最小子问题，再补充必要维度。\n\n## 调试方法\n\n用小样例手推状态表，比直接盯代码更可靠。',
            '<h1>AtCoder DP 练习笔记</h1><p>动态规划的难点通常不是转移，而是状态定义。</p><h2>状态设计</h2><p>先写出最小子问题，再补充必要维度。</p><h2>调试方法</h2><p>用小样例手推状态表，比直接盯代码更可靠。</p>',
            'AtCoder DP 练习笔记 动态规划的难点通常不是转移，而是状态定义。状态设计 先写出最小子问题，再补充必要维度。调试方法 用小样例手推状态表，比直接盯代码更可靠。',
            'PUBLISHED',
            '技术',
            '算法',
            FALSE,
            TRUE,
            96,
            0,
            8,
            3,
            TIMESTAMPTZ '2026-05-03 21:00:00+08',
            TIMESTAMPTZ '2026-05-03 20:20:00+08',
            TIMESTAMPTZ '2026-05-03 21:00:00+08'
        ),
        (
            'spring-boot-dual-token-login',
            'Spring Boot 双 Token 登录实践',
            '记录 access token 与 refresh token 的职责划分、刷新流程和后端校验边界。',
            E'# Spring Boot 双 Token 登录实践\n\n双 Token 方案可以兼顾访问体验和安全边界。\n\n## Access Token\n\nAccess Token 适合短有效期，用于访问业务接口。\n\n## Refresh Token\n\nRefresh Token 适合长有效期，用于换取新的 Access Token。',
            '<h1>Spring Boot 双 Token 登录实践</h1><p>双 Token 方案可以兼顾访问体验和安全边界。</p><h2>Access Token</h2><p>Access Token 适合短有效期，用于访问业务接口。</p><h2>Refresh Token</h2><p>Refresh Token 适合长有效期，用于换取新的 Access Token。</p>',
            'Spring Boot 双 Token 登录实践 双 Token 方案可以兼顾访问体验和安全边界。Access Token Access Token 适合短有效期，用于访问业务接口。Refresh Token Refresh Token 适合长有效期，用于换取新的 Access Token。',
            'PUBLISHED',
            '技术',
            'Java',
            FALSE,
            TRUE,
            76,
            0,
            6,
            2,
            TIMESTAMPTZ '2026-05-05 18:00:00+08',
            TIMESTAMPTZ '2026-05-05 17:10:00+08',
            TIMESTAMPTZ '2026-05-05 18:00:00+08'
        ),
        (
            'vue-admin-category-page-draft',
            'Vue 3 后台分类管理页草稿',
            '整理后台分类管理页的查询、表格、抽屉表单和状态切换交互。',
            E'# Vue 3 后台分类管理页草稿\n\n后台页面优先保证清晰、稳定和可维护。\n\n## 查询区\n\n查询条件需要和接口参数边界保持一致。\n\n## 表单\n\n新增和编辑可以复用同一个抽屉表单。',
            '<h1>Vue 3 后台分类管理页草稿</h1><p>后台页面优先保证清晰、稳定和可维护。</p><h2>查询区</h2><p>查询条件需要和接口参数边界保持一致。</p><h2>表单</h2><p>新增和编辑可以复用同一个抽屉表单。</p>',
            'Vue 3 后台分类管理页草稿 后台页面优先保证清晰、稳定和可维护。查询区 查询条件需要和接口参数边界保持一致。表单 新增和编辑可以复用同一个抽屉表单。',
            'DRAFT',
            '技术',
            '前端三剑客',
            FALSE,
            FALSE,
            0,
            0,
            0,
            0,
            NULL,
            TIMESTAMPTZ '2026-05-08 15:00:00+08',
            TIMESTAMPTZ '2026-05-08 15:00:00+08'
        ),
        (
            'old-solution-archive-offline',
            '旧题解归档计划',
            '这是一篇用于测试下线状态的旧题解整理计划，前台不应展示。',
            E'# 旧题解归档计划\n\n这篇文章用于记录旧题解整理计划。\n\n## 归档范围\n\n先整理重复度较高的基础题，再整理专题训练题。',
            '<h1>旧题解归档计划</h1><p>这篇文章用于记录旧题解整理计划。</p><h2>归档范围</h2><p>先整理重复度较高的基础题，再整理专题训练题。</p>',
            '旧题解归档计划 这篇文章用于记录旧题解整理计划。归档范围 先整理重复度较高的基础题，再整理专题训练题。',
            'OFFLINE',
            '杂记',
            '随笔',
            FALSE,
            TRUE,
            43,
            0,
            2,
            1,
            TIMESTAMPTZ '2026-04-20 10:00:00+08',
            TIMESTAMPTZ '2026-04-20 09:30:00+08',
            TIMESTAMPTZ '2026-05-09 10:00:00+08'
        )
),
admin_author AS (
    SELECT id
    FROM blog_user
    WHERE LOWER(username) = LOWER('admin')
    LIMIT 1
)
INSERT INTO blog_article (
    title,
    slug,
    summary,
    content_md,
    content_html,
    content_text,
    cover_url,
    status,
    category_id,
    author_id,
    is_top,
    allow_comment,
    view_count,
    comment_count,
    like_count,
    favorite_count,
    published_at,
    created_at,
    updated_at
)
SELECT
    seed_articles.title,
    seed_articles.slug,
    seed_articles.summary,
    seed_articles.content_md,
    seed_articles.content_html,
    seed_articles.content_text,
    '',
    seed_articles.status,
    child_category.id,
    admin_author.id,
    seed_articles.is_top,
    seed_articles.allow_comment,
    seed_articles.view_count,
    seed_articles.comment_count,
    seed_articles.like_count,
    seed_articles.favorite_count,
    seed_articles.published_at,
    seed_articles.created_at,
    seed_articles.updated_at
FROM seed_articles
JOIN blog_category parent_category
    ON parent_category.parent_id IS NULL
   AND LOWER(parent_category.name) = LOWER(seed_articles.parent_category_name)
JOIN blog_category child_category
    ON child_category.parent_id = parent_category.id
   AND LOWER(child_category.name) = LOWER(seed_articles.category_name)
CROSS JOIN admin_author
WHERE NOT EXISTS (
    SELECT 1
    FROM blog_article existing_article
    WHERE existing_article.slug = seed_articles.slug
);

WITH seed_article_tags(article_slug, tag_name) AS (
    VALUES
        ('codeforces-round-notes', 'Codeforces'),
        ('codeforces-round-notes', 'XCPC'),
        ('codeforces-round-notes', '题解'),
        ('atcoder-dp-practice', 'AtCoder'),
        ('atcoder-dp-practice', 'XCPC'),
        ('atcoder-dp-practice', '题解'),
        ('spring-boot-dual-token-login', '题解'),
        ('vue-admin-category-page-draft', '题解'),
        ('old-solution-archive-offline', '洛谷'),
        ('old-solution-archive-offline', '题解')
)
INSERT INTO blog_article_tag (
    article_id,
    tag_id,
    created_at
)
SELECT
    blog_article.id,
    blog_tag.id,
    NOW()
FROM seed_article_tags
JOIN blog_article
    ON blog_article.slug = seed_article_tags.article_slug
JOIN blog_tag
    ON LOWER(blog_tag.name) = LOWER(seed_article_tags.tag_name)
WHERE NOT EXISTS (
    SELECT 1
    FROM blog_article_tag existing_article_tag
    WHERE existing_article_tag.article_id = blog_article.id
      AND existing_article_tag.tag_id = blog_tag.id
);
