-- V1.0.0 only created the empty placeholder table; no migration or devdata seeds friend links.
DROP TABLE IF EXISTS blog_friend_link;

CREATE TABLE blog_friend_link (
    id BIGSERIAL PRIMARY KEY,
    name VARCHAR(100) NOT NULL,
    url VARCHAR(500) NOT NULL,
    logo_url VARCHAR(500) NOT NULL DEFAULT '',
    description VARCHAR(255) NOT NULL DEFAULT '',
    section VARCHAR(20) NOT NULL
        CHECK (section IN ('FRIENDS', 'EXPERTS', 'PEERS')),
    sort_no INTEGER NOT NULL DEFAULT 100
        CHECK (sort_no >= 0),
    status VARCHAR(20) NOT NULL DEFAULT 'ENABLED'
        CHECK (status IN ('ENABLED', 'DISABLED')),
    created_at TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    updated_at TIMESTAMPTZ NOT NULL DEFAULT NOW()
);

CREATE UNIQUE INDEX uq_blog_friend_link_url
    ON blog_friend_link (url);

CREATE INDEX idx_blog_friend_link_status_section_sort
    ON blog_friend_link (status, section, sort_no, id);

CREATE INDEX idx_blog_friend_link_created_at
    ON blog_friend_link (created_at DESC, id DESC);
