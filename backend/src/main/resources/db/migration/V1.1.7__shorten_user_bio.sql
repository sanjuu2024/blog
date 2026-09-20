-- 缩短字段前先阻止可能造成用户简介丢失的隐式截断。
DO $$
BEGIN
    IF EXISTS (
        SELECT 1
        FROM blog_user
        WHERE char_length(bio) > 100
    ) THEN
        RAISE EXCEPTION 'Cannot shorten blog_user.bio: existing values exceed 100 characters';
    END IF;
END
$$;

ALTER TABLE blog_user
    ALTER COLUMN bio TYPE VARCHAR(100);
