-- Seed the initial administrator account for local bootstrap and first deployment.
-- Temporary credentials:
--   username: admin
--   email: 2269102080@qq.com
--   password: 123456
-- Please change the password immediately after the first successful login.

INSERT INTO blog_user (
    username,
    nickname,
    email,
    password_hash,
    role,
    status,
    avatar_url,
    bio,
    email_verified,
    created_at,
    updated_at
)
SELECT
    'admin',
    '站点管理员',
    '2269102080@qq.com',
    '$2a$10$2ZdiSFvpHOXLvfTZShhGbe65aEFhlaHI2OQwbane4ioJ3pXvghvNS',
    'ADMIN',
    'ACTIVE',
    '',
    '系统初始化管理员账号，首次登录后记得修改默认密码。',
    FALSE,
    NOW(),
    NOW()
WHERE NOT EXISTS (
    SELECT 1
    FROM blog_user
    WHERE LOWER(username) = LOWER('admin')
       OR LOWER(email) = LOWER('2269102080@qq.com')
);
