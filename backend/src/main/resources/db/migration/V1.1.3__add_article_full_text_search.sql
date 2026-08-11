-- Enable zhparser in every database managed by Flyway. The extension binary is
-- provided by the custom PostgreSQL image, while extensions and configurations
-- themselves are database-scoped.
CREATE EXTENSION IF NOT EXISTS zhparser;

DO $$
BEGIN
    IF NOT EXISTS (
        SELECT 1
        FROM pg_catalog.pg_ts_config
        WHERE cfgname = 'zhparser_cfg'
          AND cfgnamespace = 'public'::regnamespace
    ) THEN
        CREATE TEXT SEARCH CONFIGURATION public.zhparser_cfg (PARSER = zhparser);
    END IF;
END
$$;

ALTER TEXT SEARCH CONFIGURATION public.zhparser_cfg
    DROP MAPPING IF EXISTS FOR n, v, a, i, e, l;

ALTER TEXT SEARCH CONFIGURATION public.zhparser_cfg
    ADD MAPPING FOR n, v, a, i, e, l WITH simple;

ALTER TABLE blog_article
    ADD COLUMN search_vector TSVECTOR
        GENERATED ALWAYS AS (
            SETWEIGHT(TO_TSVECTOR('public.zhparser_cfg'::regconfig, title), 'A') ||
            SETWEIGHT(TO_TSVECTOR('public.zhparser_cfg'::regconfig, summary), 'B') ||
            SETWEIGHT(TO_TSVECTOR('public.zhparser_cfg'::regconfig, content_text), 'C')
        ) STORED;

CREATE INDEX idx_blog_article_search_vector
    ON blog_article
    USING GIN (search_vector);
