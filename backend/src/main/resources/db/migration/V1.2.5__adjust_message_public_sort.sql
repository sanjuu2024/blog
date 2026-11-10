DROP INDEX IF EXISTS idx_blog_message_board_public_sort;

CREATE INDEX idx_blog_message_board_public_sort
    ON blog_message_board (parent_id, status, is_pinned DESC, created_at DESC, id DESC);
