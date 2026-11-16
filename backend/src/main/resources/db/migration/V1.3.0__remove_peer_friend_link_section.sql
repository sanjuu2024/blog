-- 保留历史友链及其审计时间，仅将同行者归入友人帐。
UPDATE blog_friend_link
SET section = 'FRIENDS'
WHERE section = 'PEERS';

ALTER TABLE blog_friend_link
    DROP CONSTRAINT blog_friend_link_section_check;

ALTER TABLE blog_friend_link
    ADD CONSTRAINT blog_friend_link_section_check
        CHECK (section IN ('FRIENDS', 'EXPERTS'));
