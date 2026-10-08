-- 历史摘要无法还原，保留事件并让新增来源字段为空。
ALTER TABLE blog_security_event
    ADD COLUMN account VARCHAR(255),
    ADD COLUMN ip VARCHAR(64),
    ADD COLUMN user_agent TEXT,
    ADD COLUMN request_method VARCHAR(10),
    ADD COLUMN request_path TEXT,
    DROP COLUMN account_mask,
    DROP COLUMN ip_hash,
    DROP COLUMN user_agent_hash;

-- 自身操作只保留目标用户，操作者字段用于管理员操作其他用户。
UPDATE blog_security_event SET actor_id = NULL WHERE actor_id = user_id;
