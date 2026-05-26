# 个人技术博客系统数据库设计

## 1. 文档信息

- 项目名称：个人技术博客系统
- 文档版本：v1.0
- 文档日期：2026-04-22
- 适用数据库：PostgreSQL
- 对应 PRD：[PRD.md](./PRD.md)

## 2. 设计说明

### 2.1 命名规范

- 所有业务表统一使用 `blog_` 前缀，避免与 PostgreSQL 关键字冲突
- 主键统一使用 `BIGSERIAL`
- 时间字段统一使用 `TIMESTAMPTZ`
- 后端、数据库与容器环境中的业务时间默认按 UTC 存储；接口返回 ISO 8601 时间，前端展示时再按用户所在时区或站点展示时区格式化
- `created_at` 和 `updated_at` 在数据库层保留 `DEFAULT NOW()` 作为兜底；正常业务写入时由应用层通过 MyBatis Plus 自动填充维护
- `updated_at` 默认给出初始值，后续更新时由应用层自动刷新
- 单行业务更新应优先传入实体对象，使用 MyBatis Plus 的 `insert`、`updateById` 或 `update(entity, wrapper)`，由公共填充器统一维护 `created_at` 和 `updated_at`
- 避免在普通业务更新中使用 wrapper-only update；该写法没有可填充实体，不作为 `updated_at` 自动维护的默认路径
- 批量更新、XML SQL 或 `@Update` 自定义 SQL 必须显式维护 `updated_at`；涉及撤销会话等业务时间时，同时显式维护 `revoked_at` 等对应字段

### 2.2 版本范围

| 分类 | 表名 | 说明 |
| --- | --- | --- |
| P0 必建 | `blog_user` | 用户表 |
| P0 必建 | `blog_auth_session` | 认证会话表，服务于双 Token 与登录审计 |
| P0 必建 | `blog_category` | 分类表，支持一级/二级分类 |
| P0 必建 | `blog_tag` | 标签表 |
| P0 必建 | `blog_article` | 文章表 |
| P0 必建 | `blog_article_tag` | 文章标签关联表 |
| 预留表 | `blog_comment` | 评论表，P1 使用 |
| 预留表 | `blog_article_like` | 点赞表，P2 使用 |
| 预留表 | `blog_article_favorite` | 收藏表，P2 使用 |
| 预留表 | `blog_message_board` | 留言表，P1 使用 |
| 预留表 | `blog_project` | 项目作品表，P1 使用 |
| 预留表 | `blog_friend_link` | 友链表，P2 使用 |

### 2.3 设计原则

- P0 阶段 `Refresh Token` 以数据库会话表为准，刷新、退出、修改密码、禁用用户等安全事件通过会话表做失效控制；修改用户角色不直接撤销 Refresh Token
- P1 阶段引入 Redis 保存运行态会话与 `tokenVersion`，用于降低鉴权查询成本，并支持修改密码、禁用用户、修改角色后的旧 Access Token 立即失效
- P1 阶段后台管理操作审计日志建议使用独立审计表，记录操作者、目标资源、操作类型、操作结果和操作时间
- `Refresh Token` 使用轮转机制；每次刷新成功后，旧会话记录标记为 `REVOKED`，新 Refresh Token 对应新的 `ACTIVE` 会话记录
- P0 阶段不因普通刷新失败自动撤销用户全部活跃 Refresh Token；修改密码、用户禁用、管理员强制下线等明确安全事件可按业务规则撤销全部会话
- 短暂宽限期用于处理网络波动下的幂等重试；P1 接入 Redis 后，可由 Redis 记录旧 `token_jti` 到新令牌结果的短 TTL 映射，数据库继续保留审计状态
- 评论、点赞、收藏等互动能力虽然不在 P0 落地，但数据库结构先预留
- 统计字段如 `comment_count`、`like_count` 放在主表冗余，行为明细拆分到独立表
- 分类采用树形结构建模，当前业务约束为两级分类
- 数据库不创建业务表之间的物理外键，统一使用逻辑外键；关联完整性、删除校验和级联清理由应用层负责

### 2.4 逻辑外键约定

本项目业务表之间不使用 PostgreSQL `FOREIGN KEY` 物理约束，所有 `xxx_id` 字段均按逻辑外键处理。这样便于开发期重置表结构、后续软删除、数据归档和潜在拆分；代价是 service 层必须显式校验关联数据。

| 表 | 字段 | 逻辑关联 | 应用层约束 |
| --- | --- | --- | --- |
| `blog_auth_session` | `user_id` | `blog_user.id` | 创建会话前必须确认用户存在且状态允许登录；用户禁用时刷新 Token 必须失败 |
| `blog_category` | `parent_id` | `blog_category.id` | 二级分类必须挂载到存在的一级分类；一级分类 `parent_id` 必须为空 |
| `blog_article` | `category_id` | `blog_category.id` | 创建或发布文章时必须确认分类存在、启用且为二级分类 |
| `blog_article` | `author_id` | `blog_user.id` | 创建文章时必须确认作者存在且具备管理员权限 |
| `blog_article_tag` | `article_id` | `blog_article.id` | 写入关联前必须确认文章存在；删除文章时由应用层清理关联记录 |
| `blog_article_tag` | `tag_id` | `blog_tag.id` | 写入关联前必须确认标签存在且启用；删除标签时由应用层清理关联记录 |
| `blog_comment` | `article_id` | `blog_article.id` | 评论前必须确认文章存在且允许评论 |
| `blog_comment` | `user_id` | `blog_user.id` | 评论前必须确认用户存在且未被禁用 |
| `blog_comment` | `parent_id` | `blog_comment.id` | 回复评论时必须确认父评论存在且属于同一文章 |
| `blog_article_like` | `article_id` | `blog_article.id` | 点赞前必须确认文章存在且可见 |
| `blog_article_like` | `user_id` | `blog_user.id` | 点赞前必须确认用户存在且未被禁用 |
| `blog_article_favorite` | `article_id` | `blog_article.id` | 收藏前必须确认文章存在且可见 |
| `blog_article_favorite` | `user_id` | `blog_user.id` | 收藏前必须确认用户存在且未被禁用 |
| `blog_message_board` | `user_id` | `blog_user.id` | 登录用户留言时记录用户 ID；游客留言时允许为空 |
| `blog_message_board` | `parent_id` | `blog_message_board.id` | 回复留言时必须确认父留言存在 |

删除或下线数据时，不能依赖数据库级联删除。当前建议默认避免物理删除核心数据；确需删除时，由 service 在同一事务中按业务规则清理子表或拒绝删除，例如删除分类前校验关联文章、删除文章时清理文章标签关联。

## 3. P0 必建表

## 3.1 表名：`blog_user`

### SQL（PostgreSQL）

```sql
CREATE TABLE IF NOT EXISTS blog_user (
    id BIGSERIAL PRIMARY KEY,
    username VARCHAR(20) NOT NULL,
    nickname VARCHAR(20) NOT NULL,
    email VARCHAR(255) NOT NULL,
    password_hash VARCHAR(255) NOT NULL,
    role VARCHAR(20) NOT NULL DEFAULT 'USER'
        CHECK (role IN ('ADMIN', 'USER')),
    status VARCHAR(20) NOT NULL DEFAULT 'ACTIVE'
        CHECK (status IN ('ACTIVE', 'DISABLED')),
    avatar_url VARCHAR(500) NOT NULL DEFAULT '',
    bio VARCHAR(500) NOT NULL DEFAULT '',
    email_verified BOOLEAN NOT NULL DEFAULT FALSE,
    email_verified_at TIMESTAMPTZ,
    last_login_at TIMESTAMPTZ,
    deleted_at TIMESTAMPTZ,
    created_at TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    updated_at TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    CONSTRAINT chk_blog_user_username_length
        CHECK (char_length(username) BETWEEN 4 AND 20),
    CONSTRAINT chk_blog_user_nickname_length
        CHECK (char_length(nickname) BETWEEN 1 AND 20)
);

CREATE UNIQUE INDEX IF NOT EXISTS uq_blog_user_username_lower
    ON blog_user (LOWER(username));

CREATE UNIQUE INDEX IF NOT EXISTS uq_blog_user_email_lower
    ON blog_user (LOWER(email));

CREATE INDEX IF NOT EXISTS idx_blog_user_role_status
    ON blog_user (role, status);
```

说明：

- `username` 保留注册时输入的原始大小写，用于展示和公开资料；唯一性和登录查询按 `LOWER(username)` 做大小写不敏感处理
- `email` 唯一性和登录查询按 `LOWER(email)` 做大小写不敏感处理，产品层统一将 `A@example.com` 和 `a@example.com` 视为同一个邮箱账号
- 例如允许用户注册展示名 `Sanjuu`，但不允许另一个人再注册 `sanjuu`；登录时输入 `sanjuu`、`SANJUU`、`Sanjuu` 都能找到同一个账号

### 字段表

| 字段名称 | 字段类型 | 字段解释 | 业务例子 |
| --- | --- | --- | --- |
| `id` | `BIGSERIAL` | 用户主键 ID，可用于公开作者信息、登录态、后台管理和逻辑外键关联 | `10001` |
| `username` | `VARCHAR(20)` | 登录用户名，要求唯一，支持用户名登录；注册后不可修改，作为展示字段和登录标识 | `ccsanjuu` |
| `nickname` | `VARCHAR(20)` | 展示昵称，长度 1-20 个字符，注册时默认使用用户名初始化 | `sanjuu` |
| `email` | `VARCHAR(255)` | 用户邮箱，要求唯一，支持邮箱登录 | `ccsanjuu@example.com` |
| `password_hash` | `VARCHAR(255)` | 密码哈希值，禁止明文存储 | `$2a$10$abc...` |
| `role` | `VARCHAR(20)` | 用户角色，区分管理员和普通用户 | `ADMIN`、`USER` |
| `status` | `VARCHAR(20)` | 用户状态，控制是否允许登录 | `ACTIVE`、`DISABLED` |
| `avatar_url` | `VARCHAR(500)` | 用户头像地址，P0 可为空，前端显示默认头像 | `https://cdn.example.com/avatar/1.png` |
| `bio` | `VARCHAR(500)` | 用户个人简介 | `专注后端和前端工程化` |
| `email_verified` | `BOOLEAN` | 邮箱是否完成验证，P0 默认未启用，但字段先预留 | `false` |
| `email_verified_at` | `TIMESTAMPTZ` | 邮箱验证完成时间 | `2026-05-01 10:00:00+08` |
| `last_login_at` | `TIMESTAMPTZ` | 最近一次登录时间 | `2026-04-22 22:10:00+08` |
| `deleted_at` | `TIMESTAMPTZ` | 软删除时间，当前版本可不使用 | `NULL` |
| `created_at` | `TIMESTAMPTZ` | 记录创建时间 | `2026-04-22 21:00:00+08` |
| `updated_at` | `TIMESTAMPTZ` | 记录更新时间 | `2026-04-22 22:10:00+08` |

公开用户资料、文章作者资料卡等前台公开场景可以返回 `blog_user.id`，并可使用 `userId` 作为路径标识；`username` 主要作为展示字段和登录标识。

## 3.2 表名：`blog_auth_session`

### SQL（PostgreSQL）

```sql
CREATE TABLE IF NOT EXISTS blog_auth_session (
    id BIGSERIAL PRIMARY KEY,
    user_id BIGINT NOT NULL,
    token_jti VARCHAR(64) NOT NULL,
    token_hash VARCHAR(255) NOT NULL,
    token_type VARCHAR(20) NOT NULL DEFAULT 'REFRESH'
        CHECK (token_type IN ('REFRESH')),
    status VARCHAR(20) NOT NULL DEFAULT 'ACTIVE'
        CHECK (status IN ('ACTIVE', 'REVOKED', 'EXPIRED')),
    expires_at TIMESTAMPTZ NOT NULL,
    revoked_at TIMESTAMPTZ,
    ip INET,
    user_agent VARCHAR(500),
    device_info VARCHAR(255),
    created_at TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    updated_at TIMESTAMPTZ NOT NULL DEFAULT NOW()
);

CREATE UNIQUE INDEX IF NOT EXISTS uq_blog_auth_session_token_jti
    ON blog_auth_session (token_jti);

CREATE INDEX IF NOT EXISTS idx_blog_auth_session_user_status
    ON blog_auth_session (user_id, status);

CREATE INDEX IF NOT EXISTS idx_blog_auth_session_expires_at
    ON blog_auth_session (expires_at);
```

### 字段表

| 字段名称 | 字段类型 | 字段解释 | 业务例子 |
| --- | --- | --- | --- |
| `id` | `BIGSERIAL` | 会话主键 ID | `50001` |
| `user_id` | `BIGINT` | 关联的用户 ID | `10001` |
| `token_jti` | `VARCHAR(64)` | Token 唯一标识，用于查 Redis、做失效控制 | `7b2f4c5d9a...` |
| `token_hash` | `VARCHAR(255)` | Refresh Token 摘要值，避免直接落库原文 | `sha256:ab12...` |
| `token_type` | `VARCHAR(20)` | Token 类型，当前只保留 Refresh Token | `REFRESH` |
| `status` | `VARCHAR(20)` | 会话状态 | `ACTIVE`、`REVOKED`、`EXPIRED` |
| `expires_at` | `TIMESTAMPTZ` | Refresh Token 失效时间 | `2026-04-29 22:10:00+08` |
| `revoked_at` | `TIMESTAMPTZ` | 主动撤销时间 | `2026-04-23 09:00:00+08` |
| `ip` | `INET` | 登录来源 IP，用于审计 | `192.168.1.100` |
| `user_agent` | `VARCHAR(500)` | 浏览器或客户端标识 | `Mozilla/5.0 Chrome/135` |
| `device_info` | `VARCHAR(255)` | 设备描述信息 | `Windows 11 / Edge` |
| `created_at` | `TIMESTAMPTZ` | 会话创建时间 | `2026-04-22 22:10:00+08` |
| `updated_at` | `TIMESTAMPTZ` | 会话更新时间 | `2026-04-22 22:20:00+08` |

### Refresh Token 轮转状态流转

- 登录成功：创建一条 `ACTIVE` 的 Refresh Token 会话记录。
- 刷新成功：在同一事务中将旧会话记录更新为 `REVOKED`，设置 `revoked_at` 和 `updated_at`，并为新 Refresh Token 创建新的 `ACTIVE` 会话记录。
- 刷新失败：Refresh Token 缺失、格式错误、签名无效、已过期、已撤销或找不到对应会话时，统一返回 `Refresh Token 无效或已过期`；P0 阶段不因普通刷新失败自动撤销该用户全部 `ACTIVE` 会话记录。
- 宽限期重试：P1 接入 Redis 后，若旧 Refresh Token 已 `REVOKED` 且仍处于 Redis 宽限期内，后端可返回第一次刷新时生成的新令牌结果，不再创建新的会话记录。
- 重放检测：P1 可结合 Redis 短 TTL 宽限期、`token_jti` 状态和 `tokenVersion` 识别高风险重放；确认高风险后再撤销该用户全部 `ACTIVE` 会话记录。
- 主动退出登录：将当前 Refresh Token 对应会话记录更新为 `REVOKED`。
- 修改密码或禁用用户：将该用户全部 `ACTIVE` Refresh Token 会话更新为 `REVOKED`；P0 阶段已签发 Access Token 依赖短有效期自然过期。
- 修改用户角色：P0 阶段不直接撤销 Refresh Token，新的角色在刷新登录态或重新登录后生效；P1 阶段通过 Redis `tokenVersion` 让旧 Access Token 立即失效。
- 定期维护：可将 `expires_at` 早于当前时间且仍为 `ACTIVE` 的记录更新为 `EXPIRED`。

## 3.3 表名：`blog_category`

### SQL（PostgreSQL）

```sql
CREATE TABLE IF NOT EXISTS blog_category (
    id BIGSERIAL PRIMARY KEY,
    name VARCHAR(50) NOT NULL,
    parent_id BIGINT,
    level SMALLINT NOT NULL
        CHECK (level IN (1, 2)),
    description VARCHAR(255) NOT NULL DEFAULT '',
    sort_no INTEGER NOT NULL DEFAULT 0,
    status VARCHAR(20) NOT NULL DEFAULT 'ENABLED'
        CHECK (status IN ('ENABLED', 'DISABLED')),
    seo_title VARCHAR(100) NOT NULL DEFAULT '',
    seo_description VARCHAR(255) NOT NULL DEFAULT '',
    created_at TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    updated_at TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    CONSTRAINT chk_blog_category_level_parent
        CHECK (
            (level = 1 AND parent_id IS NULL) OR
            (level = 2 AND parent_id IS NOT NULL)
        )
);

CREATE UNIQUE INDEX IF NOT EXISTS uq_blog_category_level1_name_lower
    ON blog_category (LOWER(name))
    WHERE parent_id IS NULL;

CREATE UNIQUE INDEX IF NOT EXISTS uq_blog_category_level2_parent_name_lower
    ON blog_category (parent_id, LOWER(name))
    WHERE parent_id IS NOT NULL;

CREATE INDEX IF NOT EXISTS idx_blog_category_parent_status_sort
    ON blog_category (parent_id, status, sort_no);

CREATE INDEX IF NOT EXISTS idx_blog_category_level_status_sort
    ON blog_category (level, status, sort_no);
```

### 字段表

| 字段名称 | 字段类型 | 字段解释 | 业务例子 |
| --- | --- | --- | --- |
| `id` | `BIGSERIAL` | 分类主键 ID | `20001` |
| `name` | `VARCHAR(50)` | 分类名称。一级分类在全局唯一；二级分类在同一父分类下唯一 | `技术`、`Java` |
| `parent_id` | `BIGINT` | 父分类 ID。一级分类为空，二级分类指向所属一级分类 | 一级分类 `NULL`；二级分类 `20001` |
| `level` | `SMALLINT` | 分类层级。`1` 表示一级分类，`2` 表示二级分类 | `1`、`2` |
| `description` | `VARCHAR(255)` | 分类说明 | `技术类主分类`、`Java 相关文章` |
| `sort_no` | `INTEGER` | 分类排序值，值越小越靠前 | `10` |
| `status` | `VARCHAR(20)` | 分类状态 | `ENABLED` |
| `seo_title` | `VARCHAR(100)` | SEO 标题预留字段 | `技术文章分类` |
| `seo_description` | `VARCHAR(255)` | SEO 描述预留字段 | `收录 Java、算法、前端等技术内容` |
| `created_at` | `TIMESTAMPTZ` | 创建时间 | `2026-04-22 21:10:00+08` |
| `updated_at` | `TIMESTAMPTZ` | 更新时间 | `2026-04-22 21:20:00+08` |

说明：

- 一级分类初始默认值建议为：`技术`、`学业`、`消遣`、`杂记`
- 二级分类示例：`算法`、`Java`、`前端三剑客`
- 文章实际绑定的应为二级分类

### 一级分类初始化数据建议

```sql
INSERT INTO blog_category (name, parent_id, level, description, sort_no, status)
VALUES
    ('技术', NULL, 1, '技术内容一级分类', 10, 'ENABLED'),
    ('学业', NULL, 1, '学业内容一级分类', 20, 'ENABLED'),
    ('消遣', NULL, 1, '消遣内容一级分类', 30, 'ENABLED'),
    ('杂记', NULL, 1, '杂记内容一级分类', 40, 'ENABLED');
```

## 3.4 表名：`blog_tag`

### SQL（PostgreSQL）

```sql
CREATE TABLE IF NOT EXISTS blog_tag (
    id BIGSERIAL PRIMARY KEY,
    name VARCHAR(50) NOT NULL,
    description VARCHAR(255) NOT NULL DEFAULT '',
    status VARCHAR(20) NOT NULL DEFAULT 'ENABLED'
        CHECK (status IN ('ENABLED', 'DISABLED')),
    created_at TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    updated_at TIMESTAMPTZ NOT NULL DEFAULT NOW()
);

CREATE UNIQUE INDEX IF NOT EXISTS uq_blog_tag_name_lower
    ON blog_tag (LOWER(name));

CREATE INDEX IF NOT EXISTS idx_blog_tag_status
    ON blog_tag (status);
```

### 字段表

| 字段名称 | 字段类型 | 字段解释 | 业务例子 |
| --- | --- | --- | --- |
| `id` | `BIGSERIAL` | 标签主键 ID | `30001` |
| `name` | `VARCHAR(50)` | 标签名称，要求唯一 | `JWT` |
| `description` | `VARCHAR(255)` | 标签说明，用于后台管理和后续搜索提示 | `和认证授权相关的文章标签` |
| `status` | `VARCHAR(20)` | 标签状态 | `ENABLED` |
| `created_at` | `TIMESTAMPTZ` | 创建时间 | `2026-04-22 21:15:00+08` |
| `updated_at` | `TIMESTAMPTZ` | 更新时间 | `2026-04-22 21:16:00+08` |

## 3.5 表名：`blog_article`

### SQL（PostgreSQL）

```sql
CREATE TABLE IF NOT EXISTS blog_article (
    id BIGSERIAL PRIMARY KEY,
    title VARCHAR(200) NOT NULL,
    slug VARCHAR(200),
    summary VARCHAR(500) NOT NULL DEFAULT '',
    content_md TEXT NOT NULL,
    content_html TEXT NOT NULL,
    content_text TEXT NOT NULL,
    cover_url VARCHAR(500) NOT NULL DEFAULT '',
    status VARCHAR(20) NOT NULL DEFAULT 'DRAFT'
        CHECK (status IN ('DRAFT', 'PUBLISHED', 'OFFLINE')),
    category_id BIGINT NOT NULL,
    author_id BIGINT NOT NULL,
    is_top BOOLEAN NOT NULL DEFAULT FALSE,
    allow_comment BOOLEAN NOT NULL DEFAULT TRUE,
    view_count INTEGER NOT NULL DEFAULT 0 CHECK (view_count >= 0),
    comment_count INTEGER NOT NULL DEFAULT 0 CHECK (comment_count >= 0),
    like_count INTEGER NOT NULL DEFAULT 0 CHECK (like_count >= 0),
    favorite_count INTEGER NOT NULL DEFAULT 0 CHECK (favorite_count >= 0),
    published_at TIMESTAMPTZ,
    created_at TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    updated_at TIMESTAMPTZ NOT NULL DEFAULT NOW()
);

CREATE UNIQUE INDEX IF NOT EXISTS uq_blog_article_slug
    ON blog_article (slug)
    WHERE slug IS NOT NULL;

CREATE INDEX IF NOT EXISTS idx_blog_article_status_published_at
    ON blog_article (status, published_at DESC);

CREATE INDEX IF NOT EXISTS idx_blog_article_category_id
    ON blog_article (category_id);

CREATE INDEX IF NOT EXISTS idx_blog_article_author_id
    ON blog_article (author_id);

CREATE INDEX IF NOT EXISTS idx_blog_article_top_publish
    ON blog_article (is_top, published_at DESC);
```

### 字段表

| 字段名称 | 字段类型 | 字段解释 | 业务例子 |
| --- | --- | --- | --- |
| `id` | `BIGSERIAL` | 文章主键 ID | `40001` |
| `title` | `VARCHAR(200)` | 文章标题 | `Spring Boot 双 Token 登录实践` |
| `slug` | `VARCHAR(200)` | 文章 SEO 路径标识，P0 可为空；P2 再用于 URL 可读化优化 | `spring-boot-dual-token` |
| `summary` | `VARCHAR(500)` | 文章摘要，列表页用于简介展示 | `本文记录双 Token 的实现思路与接口设计` |
| `content_md` | `TEXT` | Markdown 正文内容，作为后台编辑源 | `# 一、背景\n...` |
| `content_html` | `TEXT` | 由 Markdown 转译得到的 HTML 正文，供前台渲染使用 | `<h1>一、背景</h1><p>...</p>` |
| `content_text` | `TEXT` | 从 Markdown 提取的纯文本正文，供全文搜索使用 | `一、背景 ...` |
| `cover_url` | `VARCHAR(500)` | 文章封面地址，P0 可先手工录入 URL 或留空 | `https://cdn.example.com/cover/token.png` |
| `status` | `VARCHAR(20)` | 文章状态 | `DRAFT`、`PUBLISHED`、`OFFLINE` |
| `category_id` | `BIGINT` | 文章所属二级分类 ID，应用层需校验不能绑定一级分类 | `21001` |
| `author_id` | `BIGINT` | 文章作者 ID，当前通常是管理员 | `10001` |
| `is_top` | `BOOLEAN` | 是否置顶 | `true` |
| `allow_comment` | `BOOLEAN` | 是否允许评论，先预留控制字段 | `true` |
| `view_count` | `INTEGER` | 浏览量冗余字段 | `128` |
| `comment_count` | `INTEGER` | 评论数冗余字段，P1 启用 | `6` |
| `like_count` | `INTEGER` | 点赞数冗余字段，P2 启用 | `35` |
| `favorite_count` | `INTEGER` | 收藏数冗余字段，P2 启用 | `12` |
| `published_at` | `TIMESTAMPTZ` | 发布时间，草稿阶段可为空 | `2026-04-22 23:00:00+08` |
| `created_at` | `TIMESTAMPTZ` | 创建时间 | `2026-04-22 21:30:00+08` |
| `updated_at` | `TIMESTAMPTZ` | 更新时间 | `2026-04-22 22:45:00+08` |

说明：

- 例如一级分类 `技术` 下可挂二级分类 `Java`、`算法`、`前端三剑客`
- 文章的 `category_id` 应指向 `Java`、`算法` 这类二级分类，而不是 `技术` 这类一级分类
- 保存或更新文章时，应用层应以 `content_md` 为源生成 `content_html` 与 `content_text`
- P0 阶段文章详情 URL 以 `id` 作为稳定定位标识；`slug` 不作为必填字段，也不要求管理员手动维护
- P2 阶段可在前台 URL 中追加 `slug` 提升可读性，例如 `/articles/40001-spring-boot-dual-token-login`，实际定位仍优先以 `id` 为准

## 3.6 表名：`blog_article_tag`

### SQL（PostgreSQL）

```sql
CREATE TABLE IF NOT EXISTS blog_article_tag (
    article_id BIGINT NOT NULL,
    tag_id BIGINT NOT NULL,
    created_at TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    PRIMARY KEY (article_id, tag_id)
);

CREATE INDEX IF NOT EXISTS idx_blog_article_tag_tag_id
    ON blog_article_tag (tag_id);
```

### 字段表

| 字段名称 | 字段类型 | 字段解释 | 业务例子 |
| --- | --- | --- | --- |
| `article_id` | `BIGINT` | 关联文章 ID | `40001` |
| `tag_id` | `BIGINT` | 关联标签 ID | `30001` |
| `created_at` | `TIMESTAMPTZ` | 关联关系创建时间 | `2026-04-22 22:00:00+08` |

## 4. 预留表

## 4.1 表名：`blog_comment`

### SQL（PostgreSQL）

```sql
CREATE TABLE IF NOT EXISTS blog_comment (
    id BIGSERIAL PRIMARY KEY,
    article_id BIGINT NOT NULL,
    user_id BIGINT NOT NULL,
    parent_id BIGINT,
    content TEXT NOT NULL,
    status VARCHAR(20) NOT NULL DEFAULT 'PENDING'
        CHECK (status IN ('PENDING', 'APPROVED', 'REJECTED')),
    created_at TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    updated_at TIMESTAMPTZ NOT NULL DEFAULT NOW()
);

CREATE INDEX IF NOT EXISTS idx_blog_comment_article_status
    ON blog_comment (article_id, status);

CREATE INDEX IF NOT EXISTS idx_blog_comment_user_id
    ON blog_comment (user_id);

CREATE INDEX IF NOT EXISTS idx_blog_comment_parent_id
    ON blog_comment (parent_id);
```

### 字段表

| 字段名称 | 字段类型 | 字段解释 | 业务例子 |
| --- | --- | --- | --- |
| `id` | `BIGSERIAL` | 评论主键 ID | `60001` |
| `article_id` | `BIGINT` | 所属文章 ID | `40001` |
| `user_id` | `BIGINT` | 评论用户 ID | `10002` |
| `parent_id` | `BIGINT` | 父评论 ID，一级评论为空，回复评论时有值 | `60000` |
| `content` | `TEXT` | 评论内容 | `这篇文章对双 Token 的解释很清楚` |
| `status` | `VARCHAR(20)` | 评论状态，可用于审核流 | `PENDING`、`APPROVED` |
| `created_at` | `TIMESTAMPTZ` | 创建时间 | `2026-05-01 10:20:00+08` |
| `updated_at` | `TIMESTAMPTZ` | 更新时间 | `2026-05-01 10:25:00+08` |

## 4.2 表名：`blog_article_like`

### SQL（PostgreSQL）

```sql
CREATE TABLE IF NOT EXISTS blog_article_like (
    id BIGSERIAL PRIMARY KEY,
    article_id BIGINT NOT NULL,
    user_id BIGINT NOT NULL,
    created_at TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    CONSTRAINT uq_blog_article_like UNIQUE (article_id, user_id)
);

CREATE INDEX IF NOT EXISTS idx_blog_article_like_user_id
    ON blog_article_like (user_id);
```

### 字段表

| 字段名称 | 字段类型 | 字段解释 | 业务例子 |
| --- | --- | --- | --- |
| `id` | `BIGSERIAL` | 点赞记录主键 ID | `70001` |
| `article_id` | `BIGINT` | 被点赞文章 ID | `40001` |
| `user_id` | `BIGINT` | 点赞用户 ID | `10002` |
| `created_at` | `TIMESTAMPTZ` | 点赞时间 | `2026-05-03 09:30:00+08` |

## 4.3 表名：`blog_article_favorite`

### SQL（PostgreSQL）

```sql
CREATE TABLE IF NOT EXISTS blog_article_favorite (
    id BIGSERIAL PRIMARY KEY,
    article_id BIGINT NOT NULL,
    user_id BIGINT NOT NULL,
    created_at TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    CONSTRAINT uq_blog_article_favorite UNIQUE (article_id, user_id)
);

CREATE INDEX IF NOT EXISTS idx_blog_article_favorite_user_id
    ON blog_article_favorite (user_id);
```

### 字段表

| 字段名称 | 字段类型 | 字段解释 | 业务例子 |
| --- | --- | --- | --- |
| `id` | `BIGSERIAL` | 收藏记录主键 ID | `80001` |
| `article_id` | `BIGINT` | 被收藏文章 ID | `40001` |
| `user_id` | `BIGINT` | 收藏用户 ID | `10002` |
| `created_at` | `TIMESTAMPTZ` | 收藏时间 | `2026-05-03 09:40:00+08` |

## 4.4 表名：`blog_message_board`

### SQL（PostgreSQL）

```sql
CREATE TABLE IF NOT EXISTS blog_message_board (
    id BIGSERIAL PRIMARY KEY,
    user_id BIGINT,
    parent_id BIGINT,
    nickname VARCHAR(50) NOT NULL DEFAULT '',
    email VARCHAR(255) NOT NULL DEFAULT '',
    content TEXT NOT NULL,
    status VARCHAR(20) NOT NULL DEFAULT 'PENDING'
        CHECK (status IN ('PENDING', 'APPROVED', 'REJECTED')),
    created_at TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    updated_at TIMESTAMPTZ NOT NULL DEFAULT NOW()
);

CREATE INDEX IF NOT EXISTS idx_blog_message_board_status
    ON blog_message_board (status);

CREATE INDEX IF NOT EXISTS idx_blog_message_board_user_id
    ON blog_message_board (user_id);
```

### 字段表

| 字段名称 | 字段类型 | 字段解释 | 业务例子 |
| --- | --- | --- | --- |
| `id` | `BIGSERIAL` | 留言主键 ID | `90001` |
| `user_id` | `BIGINT` | 登录用户留言时关联的用户 ID，游客留言可为空 | `10002` |
| `parent_id` | `BIGINT` | 回复留言时指向父留言 ID | `90000` |
| `nickname` | `VARCHAR(50)` | 留言昵称，游客场景使用 | `路人甲` |
| `email` | `VARCHAR(255)` | 留言邮箱，游客场景使用 | `guest@example.com` |
| `content` | `TEXT` | 留言内容 | `博客很清爽，期待评论功能上线` |
| `status` | `VARCHAR(20)` | 留言状态，可支持审核 | `PENDING`、`APPROVED` |
| `created_at` | `TIMESTAMPTZ` | 创建时间 | `2026-05-10 14:00:00+08` |
| `updated_at` | `TIMESTAMPTZ` | 更新时间 | `2026-05-10 14:10:00+08` |

## 4.5 表名：`blog_project`

### SQL（PostgreSQL）

```sql
CREATE TABLE IF NOT EXISTS blog_project (
    id BIGSERIAL PRIMARY KEY,
    title VARCHAR(200) NOT NULL,
    slug VARCHAR(200),
    summary VARCHAR(500) NOT NULL DEFAULT '',
    content_md TEXT NOT NULL DEFAULT '',
    cover_url VARCHAR(500) NOT NULL DEFAULT '',
    project_url VARCHAR(500) NOT NULL DEFAULT '',
    source_url VARCHAR(500) NOT NULL DEFAULT '',
    tech_stack VARCHAR(255) NOT NULL DEFAULT '',
    sort_no INTEGER NOT NULL DEFAULT 0,
    status VARCHAR(20) NOT NULL DEFAULT 'DRAFT'
        CHECK (status IN ('DRAFT', 'PUBLISHED', 'OFFLINE')),
    is_top BOOLEAN NOT NULL DEFAULT FALSE,
    published_at TIMESTAMPTZ,
    created_at TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    updated_at TIMESTAMPTZ NOT NULL DEFAULT NOW()
);

CREATE UNIQUE INDEX IF NOT EXISTS uq_blog_project_slug
    ON blog_project (slug)
    WHERE slug IS NOT NULL;

CREATE INDEX IF NOT EXISTS idx_blog_project_status_sort
    ON blog_project (status, sort_no, published_at DESC);
```

### 字段表

| 字段名称 | 字段类型 | 字段解释 | 业务例子 |
| --- | --- | --- | --- |
| `id` | `BIGSERIAL` | 项目主键 ID | `100001` |
| `title` | `VARCHAR(200)` | 项目名称 | `个人博客系统` |
| `slug` | `VARCHAR(200)` | 项目 SEO 路径标识 | `personal-blog-system` |
| `summary` | `VARCHAR(500)` | 项目简介 | `基于 Vue 和 Spring Boot 的个人博客练手项目` |
| `content_md` | `TEXT` | 项目详细介绍 Markdown | `## 项目背景\n...` |
| `cover_url` | `VARCHAR(500)` | 项目封面地址 | `https://cdn.example.com/project/blog-cover.png` |
| `project_url` | `VARCHAR(500)` | 项目演示地址 | `https://blog.example.com` |
| `source_url` | `VARCHAR(500)` | 源码仓库地址 | `https://github.com/xxx/blog` |
| `tech_stack` | `VARCHAR(255)` | 技术栈概述 | `Vue3, Spring Boot, PostgreSQL, Redis` |
| `sort_no` | `INTEGER` | 排序值 | `1` |
| `status` | `VARCHAR(20)` | 项目状态 | `DRAFT`、`PUBLISHED`、`OFFLINE` |
| `is_top` | `BOOLEAN` | 是否置顶 | `true` |
| `published_at` | `TIMESTAMPTZ` | 发布时间 | `2026-05-15 12:00:00+08` |
| `created_at` | `TIMESTAMPTZ` | 创建时间 | `2026-05-10 10:00:00+08` |
| `updated_at` | `TIMESTAMPTZ` | 更新时间 | `2026-05-12 18:00:00+08` |

## 4.6 表名：`blog_friend_link`

### SQL（PostgreSQL）

```sql
CREATE TABLE IF NOT EXISTS blog_friend_link (
    id BIGSERIAL PRIMARY KEY,
    name VARCHAR(100) NOT NULL,
    url VARCHAR(500) NOT NULL,
    logo_url VARCHAR(500) NOT NULL DEFAULT '',
    description VARCHAR(255) NOT NULL DEFAULT '',
    sort_no INTEGER NOT NULL DEFAULT 0,
    status VARCHAR(20) NOT NULL DEFAULT 'ENABLED'
        CHECK (status IN ('ENABLED', 'DISABLED')),
    created_at TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    updated_at TIMESTAMPTZ NOT NULL DEFAULT NOW()
);

CREATE UNIQUE INDEX IF NOT EXISTS uq_blog_friend_link_url
    ON blog_friend_link (url);

CREATE INDEX IF NOT EXISTS idx_blog_friend_link_status_sort
    ON blog_friend_link (status, sort_no);
```

### 字段表

| 字段名称 | 字段类型 | 字段解释 | 业务例子 |
| --- | --- | --- | --- |
| `id` | `BIGSERIAL` | 友链主键 ID | `110001` |
| `name` | `VARCHAR(100)` | 友链名称 | `某某技术博客` |
| `url` | `VARCHAR(500)` | 友链地址 | `https://tech.example.com` |
| `logo_url` | `VARCHAR(500)` | 友链 Logo 地址 | `https://tech.example.com/logo.png` |
| `description` | `VARCHAR(255)` | 友链说明 | `专注 Java 与中间件` |
| `sort_no` | `INTEGER` | 排序值 | `5` |
| `status` | `VARCHAR(20)` | 友链状态 | `ENABLED` |
| `created_at` | `TIMESTAMPTZ` | 创建时间 | `2026-06-01 09:00:00+08` |
| `updated_at` | `TIMESTAMPTZ` | 更新时间 | `2026-06-01 09:10:00+08` |

## 5. 建表顺序建议

建议按以下顺序执行建表 SQL：

1. `blog_user`
2. `blog_auth_session`
3. `blog_category`
4. `blog_tag`
5. `blog_article`
6. `blog_article_tag`
7. `blog_comment`
8. `blog_article_like`
9. `blog_article_favorite`
10. `blog_message_board`
11. `blog_project`
12. `blog_friend_link`

## 6. 落地建议

- P0 最少先落地 `blog_user`、`blog_auth_session`、`blog_category`、`blog_tag`、`blog_article`、`blog_article_tag`
- 如果你想避免后续频繁改表，建议本次把预留表一起建好
- `blog_auth_session` 可以作为 Redis 的补充审计表，不要求所有鉴权逻辑都依赖数据库
- 文章封面与头像上传虽然在 P1 实现，但建议 P0 先把 URL 字段建好
