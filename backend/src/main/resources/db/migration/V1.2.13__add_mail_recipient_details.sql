ALTER TABLE blog_mail_delivery
    ADD COLUMN recipient_email VARCHAR(255),
    ADD COLUMN recipient_user_id BIGINT,
    ALTER COLUMN recipient_masked SET DEFAULT '';

-- 只回填可信业务关联，不用当前邮箱伪造历史投递地址。
UPDATE blog_mail_delivery delivery
SET recipient_user_id = comment.user_id
FROM blog_comment comment
WHERE delivery.mail_type = 'COMMENT_REPLY' AND comment.id = delivery.source_id;

UPDATE blog_mail_delivery delivery
SET recipient_user_id = message.user_id
FROM blog_message_board message
WHERE delivery.mail_type = 'MESSAGE_REPLY' AND message.id = delivery.source_id;
