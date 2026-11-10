CREATE TABLE blog_about_page (
    id BIGINT PRIMARY KEY,
    content_md TEXT NOT NULL,
    content_html TEXT NOT NULL,
    content_text TEXT NOT NULL,
    updated_by BIGINT NOT NULL,
    created_at TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    updated_at TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    CONSTRAINT chk_blog_about_page_singleton CHECK (id = 1)
);
