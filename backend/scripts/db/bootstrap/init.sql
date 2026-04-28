-- This script is executed only on first container initialization by
-- /docker-entrypoint-initdb.d in the official PostgreSQL image.
--
-- It should contain instance/database bootstrap logic (extensions, text search config),
-- while business tables/constraints remain managed by Flyway migrations.

CREATE EXTENSION IF NOT EXISTS zhparser;

DO $$
BEGIN
    IF NOT EXISTS (
        SELECT 1
        FROM pg_catalog.pg_ts_config
        WHERE cfgname = 'zhparser_cfg'
    ) THEN
        CREATE TEXT SEARCH CONFIGURATION zhparser_cfg (PARSER = zhparser);
    END IF;
END
$$;

ALTER TEXT SEARCH CONFIGURATION zhparser_cfg
    DROP MAPPING IF EXISTS FOR n, v, a, i, e, l;

ALTER TEXT SEARCH CONFIGURATION zhparser_cfg
    ADD MAPPING FOR n, v, a, i, e, l WITH simple;

DO $$
BEGIN
    EXECUTE format(
        'ALTER DATABASE %I SET default_text_search_config = %L',
        current_database(),
        'public.zhparser_cfg'
    );
END
$$;
