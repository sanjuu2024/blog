-- Seed initial competitive programming tags for local bootstrap and first deployment.

WITH initial_tags(name, description, status) AS (
    VALUES
        ('Codeforces', 'Codeforces 题目、比赛和题解相关内容', 'ENABLED'),
        ('AtCoder', 'AtCoder 题目、比赛和题解相关内容', 'ENABLED'),
        ('牛客', '牛客竞赛、练习和题解相关内容', 'ENABLED'),
        ('洛谷', '洛谷题目、训练和题解相关内容', 'ENABLED'),
        ('XCPC', 'XCPC 训练、比赛和算法竞赛相关内容', 'ENABLED'),
        ('题解', '题目解析和解题思路相关内容', 'ENABLED')
)
INSERT INTO blog_tag (
    name,
    description,
    status,
    created_at,
    updated_at
)
SELECT
    initial_tags.name,
    initial_tags.description,
    initial_tags.status,
    NOW(),
    NOW()
FROM initial_tags
WHERE NOT EXISTS (
    SELECT 1
    FROM blog_tag
    WHERE LOWER(blog_tag.name) = LOWER(initial_tags.name)
);
