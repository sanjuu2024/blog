CREATE TABLE blog_article_daily_stat (
    article_id BIGINT NOT NULL,
    stat_date DATE NOT NULL,
    view_count BIGINT NOT NULL DEFAULT 0 CHECK (view_count >= 0),
    PRIMARY KEY (article_id, stat_date)
);

CREATE INDEX idx_blog_article_daily_stat_date
    ON blog_article_daily_stat (stat_date DESC);
