-- 收紧用户名格式，并允许长度为 2-3 个字符的用户名。
ALTER TABLE blog_user
    DROP CONSTRAINT chk_blog_user_username_length;

ALTER TABLE blog_user
    ADD CONSTRAINT chk_blog_user_username_length
        CHECK (char_length(username) BETWEEN 2 AND 20),
    ADD CONSTRAINT chk_blog_user_username_format
        CHECK (username ~ '^[A-Za-z0-9_-]{2,20}$');
