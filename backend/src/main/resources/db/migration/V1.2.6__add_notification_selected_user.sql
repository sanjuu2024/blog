ALTER TABLE blog_notification
    ADD COLUMN IF NOT EXISTS selected_user_id BIGINT;
