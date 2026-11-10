SET client_encoding = 'UTF8';
SET standard_conforming_strings = on;

CREATE TABLE IF NOT EXISTS blog_user (
    id BIGSERIAL PRIMARY KEY,
    username VARCHAR(20) NOT NULL,
    nickname VARCHAR(20) NOT NULL,
    email VARCHAR(255) NOT NULL,
    password_hash VARCHAR(255) NOT NULL,
    role VARCHAR(20) NOT NULL DEFAULT 'USER'
        CHECK (role IN ('ADMIN', 'USER')),
    status VARCHAR(20) NOT NULL DEFAULT 'ACTIVE'
        CHECK (status IN ('ACTIVE', 'DISABLED')),
    avatar_url VARCHAR(500) NOT NULL DEFAULT '',
    bio VARCHAR(500) NOT NULL DEFAULT '',
    email_verified BOOLEAN NOT NULL DEFAULT FALSE,
    email_verified_at TIMESTAMPTZ,
    last_login_at TIMESTAMPTZ,
    deleted_at TIMESTAMPTZ,
    created_at TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    updated_at TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    CONSTRAINT chk_blog_user_username_length
        CHECK (char_length(username) BETWEEN 4 AND 20),
    CONSTRAINT chk_blog_user_nickname_length
        CHECK (char_length(nickname) BETWEEN 1 AND 20)
);

CREATE UNIQUE INDEX IF NOT EXISTS uq_blog_user_username_lower
    ON blog_user (LOWER(username));

CREATE UNIQUE INDEX IF NOT EXISTS uq_blog_user_email_lower
    ON blog_user (LOWER(email));

CREATE INDEX IF NOT EXISTS idx_blog_user_role_status
    ON blog_user (role, status);

CREATE TABLE IF NOT EXISTS blog_auth_session (
    id BIGSERIAL PRIMARY KEY,
    user_id BIGINT NOT NULL,
    token_jti VARCHAR(64) NOT NULL,
    token_hash VARCHAR(255) NOT NULL,
    token_type VARCHAR(20) NOT NULL DEFAULT 'REFRESH'
        CHECK (token_type IN ('REFRESH')),
    status VARCHAR(20) NOT NULL DEFAULT 'ACTIVE'
        CHECK (status IN ('ACTIVE', 'REVOKED', 'EXPIRED')),
    expires_at TIMESTAMPTZ NOT NULL,
    revoked_at TIMESTAMPTZ,
    ip INET,
    user_agent VARCHAR(500),
    device_info VARCHAR(255),
    created_at TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    updated_at TIMESTAMPTZ NOT NULL DEFAULT NOW()
);

CREATE UNIQUE INDEX IF NOT EXISTS uq_blog_auth_session_token_jti
    ON blog_auth_session (token_jti);

CREATE INDEX IF NOT EXISTS idx_blog_auth_session_user_status
    ON blog_auth_session (user_id, status);

CREATE INDEX IF NOT EXISTS idx_blog_auth_session_expires_at
    ON blog_auth_session (expires_at);

CREATE TABLE IF NOT EXISTS blog_category (
    id BIGSERIAL PRIMARY KEY,
    name VARCHAR(50) NOT NULL,
    parent_id BIGINT,
    level SMALLINT NOT NULL
        CHECK (level IN (1, 2)),
    description VARCHAR(255) NOT NULL DEFAULT '',
    sort_no INTEGER NOT NULL DEFAULT 0,
    status VARCHAR(20) NOT NULL DEFAULT 'ENABLED'
        CHECK (status IN ('ENABLED', 'DISABLED')),
    seo_title VARCHAR(100) NOT NULL DEFAULT '',
    seo_description VARCHAR(255) NOT NULL DEFAULT '',
    created_at TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    updated_at TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    CONSTRAINT chk_blog_category_level_parent
        CHECK (
            (level = 1 AND parent_id IS NULL) OR
            (level = 2 AND parent_id IS NOT NULL)
        )
);

CREATE UNIQUE INDEX IF NOT EXISTS uq_blog_category_level1_name_lower
    ON blog_category (LOWER(name))
    WHERE parent_id IS NULL;

CREATE UNIQUE INDEX IF NOT EXISTS uq_blog_category_level2_parent_name_lower
    ON blog_category (parent_id, LOWER(name))
    WHERE parent_id IS NOT NULL;

CREATE INDEX IF NOT EXISTS idx_blog_category_parent_status_sort
    ON blog_category (parent_id, status, sort_no);

CREATE INDEX IF NOT EXISTS idx_blog_category_level_status_sort
    ON blog_category (level, status, sort_no);

CREATE TABLE IF NOT EXISTS blog_tag (
    id BIGSERIAL PRIMARY KEY,
    name VARCHAR(50) NOT NULL,
    description VARCHAR(255) NOT NULL DEFAULT '',
    status VARCHAR(20) NOT NULL DEFAULT 'ENABLED'
        CHECK (status IN ('ENABLED', 'DISABLED')),
    created_at TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    updated_at TIMESTAMPTZ NOT NULL DEFAULT NOW()
);

CREATE UNIQUE INDEX IF NOT EXISTS uq_blog_tag_name_lower
    ON blog_tag (LOWER(name));

CREATE INDEX IF NOT EXISTS idx_blog_tag_status
    ON blog_tag (status);

CREATE TABLE IF NOT EXISTS blog_article (
    id BIGSERIAL PRIMARY KEY,
    title VARCHAR(200) NOT NULL,
    slug VARCHAR(200),
    summary VARCHAR(500) NOT NULL DEFAULT '',
    content_md TEXT NOT NULL,
    content_html TEXT NOT NULL,
    content_text TEXT NOT NULL,
    cover_url VARCHAR(500) NOT NULL DEFAULT '',
    status VARCHAR(20) NOT NULL DEFAULT 'DRAFT'
        CHECK (status IN ('DRAFT', 'PUBLISHED', 'OFFLINE')),
    category_id BIGINT NOT NULL,
    author_id BIGINT NOT NULL,
    is_top BOOLEAN NOT NULL DEFAULT FALSE,
    allow_comment BOOLEAN NOT NULL DEFAULT TRUE,
    view_count INTEGER NOT NULL DEFAULT 0 CHECK (view_count >= 0),
    comment_count INTEGER NOT NULL DEFAULT 0 CHECK (comment_count >= 0),
    like_count INTEGER NOT NULL DEFAULT 0 CHECK (like_count >= 0),
    favorite_count INTEGER NOT NULL DEFAULT 0 CHECK (favorite_count >= 0),
    published_at TIMESTAMPTZ,
    created_at TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    updated_at TIMESTAMPTZ NOT NULL DEFAULT NOW()
);

CREATE UNIQUE INDEX IF NOT EXISTS uq_blog_article_slug
    ON blog_article (slug)
    WHERE slug IS NOT NULL;

CREATE INDEX IF NOT EXISTS idx_blog_article_status_published_at
    ON blog_article (status, published_at DESC);

CREATE INDEX IF NOT EXISTS idx_blog_article_category_id
    ON blog_article (category_id);

CREATE INDEX IF NOT EXISTS idx_blog_article_author_id
    ON blog_article (author_id);

CREATE INDEX IF NOT EXISTS idx_blog_article_top_publish
    ON blog_article (is_top, published_at DESC);

CREATE TABLE IF NOT EXISTS blog_article_tag (
    article_id BIGINT NOT NULL,
    tag_id BIGINT NOT NULL,
    created_at TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    PRIMARY KEY (article_id, tag_id)
);

CREATE INDEX IF NOT EXISTS idx_blog_article_tag_tag_id
    ON blog_article_tag (tag_id);

CREATE TABLE IF NOT EXISTS blog_comment (
    id BIGSERIAL PRIMARY KEY,
    article_id BIGINT NOT NULL,
    user_id BIGINT NOT NULL,
    parent_id BIGINT,
    content TEXT NOT NULL,
    status VARCHAR(20) NOT NULL DEFAULT 'PENDING'
        CHECK (status IN ('PENDING', 'APPROVED', 'REJECTED')),
    created_at TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    updated_at TIMESTAMPTZ NOT NULL DEFAULT NOW()
);

CREATE INDEX IF NOT EXISTS idx_blog_comment_article_status
    ON blog_comment (article_id, status);

CREATE INDEX IF NOT EXISTS idx_blog_comment_user_id
    ON blog_comment (user_id);

CREATE INDEX IF NOT EXISTS idx_blog_comment_parent_id
    ON blog_comment (parent_id);

CREATE TABLE IF NOT EXISTS blog_article_like (
    id BIGSERIAL PRIMARY KEY,
    article_id BIGINT NOT NULL,
    user_id BIGINT NOT NULL,
    created_at TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    CONSTRAINT uq_blog_article_like UNIQUE (article_id, user_id)
);

CREATE INDEX IF NOT EXISTS idx_blog_article_like_user_id
    ON blog_article_like (user_id);

CREATE TABLE IF NOT EXISTS blog_article_favorite (
    id BIGSERIAL PRIMARY KEY,
    article_id BIGINT NOT NULL,
    user_id BIGINT NOT NULL,
    created_at TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    CONSTRAINT uq_blog_article_favorite UNIQUE (article_id, user_id)
);

CREATE INDEX IF NOT EXISTS idx_blog_article_favorite_user_id
    ON blog_article_favorite (user_id);

CREATE TABLE IF NOT EXISTS blog_message_board (
    id BIGSERIAL PRIMARY KEY,
    user_id BIGINT,
    parent_id BIGINT,
    nickname VARCHAR(50) NOT NULL DEFAULT '',
    email VARCHAR(255) NOT NULL DEFAULT '',
    content TEXT NOT NULL,
    status VARCHAR(20) NOT NULL DEFAULT 'PENDING'
        CHECK (status IN ('PENDING', 'APPROVED', 'REJECTED')),
    created_at TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    updated_at TIMESTAMPTZ NOT NULL DEFAULT NOW()
);

CREATE INDEX IF NOT EXISTS idx_blog_message_board_status
    ON blog_message_board (status);

CREATE INDEX IF NOT EXISTS idx_blog_message_board_user_id
    ON blog_message_board (user_id);

CREATE TABLE IF NOT EXISTS blog_project (
    id BIGSERIAL PRIMARY KEY,
    title VARCHAR(200) NOT NULL,
    slug VARCHAR(200),
    summary VARCHAR(500) NOT NULL DEFAULT '',
    content_md TEXT NOT NULL DEFAULT '',
    cover_url VARCHAR(500) NOT NULL DEFAULT '',
    project_url VARCHAR(500) NOT NULL DEFAULT '',
    source_url VARCHAR(500) NOT NULL DEFAULT '',
    tech_stack VARCHAR(255) NOT NULL DEFAULT '',
    sort_no INTEGER NOT NULL DEFAULT 0,
    status VARCHAR(20) NOT NULL DEFAULT 'DRAFT'
        CHECK (status IN ('DRAFT', 'PUBLISHED', 'OFFLINE')),
    is_top BOOLEAN NOT NULL DEFAULT FALSE,
    published_at TIMESTAMPTZ,
    created_at TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    updated_at TIMESTAMPTZ NOT NULL DEFAULT NOW()
);

CREATE UNIQUE INDEX IF NOT EXISTS uq_blog_project_slug
    ON blog_project (slug)
    WHERE slug IS NOT NULL;

CREATE INDEX IF NOT EXISTS idx_blog_project_status_sort
    ON blog_project (status, sort_no, published_at DESC);

CREATE TABLE IF NOT EXISTS blog_friend_link (
    id BIGSERIAL PRIMARY KEY,
    name VARCHAR(100) NOT NULL,
    url VARCHAR(500) NOT NULL,
    logo_url VARCHAR(500) NOT NULL DEFAULT '',
    description VARCHAR(255) NOT NULL DEFAULT '',
    sort_no INTEGER NOT NULL DEFAULT 0,
    status VARCHAR(20) NOT NULL DEFAULT 'ENABLED'
        CHECK (status IN ('ENABLED', 'DISABLED')),
    created_at TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    updated_at TIMESTAMPTZ NOT NULL DEFAULT NOW()
);

CREATE UNIQUE INDEX IF NOT EXISTS uq_blog_friend_link_url
    ON blog_friend_link (url);

CREATE INDEX IF NOT EXISTS idx_blog_friend_link_status_sort
    ON blog_friend_link (status, sort_no);

INSERT INTO blog_category (name, parent_id, level, description, sort_no, status)
VALUES
    ('技术', NULL, 1, '技术内容一级分类', 10, 'ENABLED'),
    ('学业', NULL, 1, '学业内容一级分类', 20, 'ENABLED'),
    ('消遣', NULL, 1, '消遣内容一级分类', 30, 'ENABLED'),
    ('杂记', NULL, 1, '杂记内容一级分类', 40, 'ENABLED')
ON CONFLICT DO NOTHING;
