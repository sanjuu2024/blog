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
| P1 使用 | `blog_comment` | 评论表，支持审核、无限层级回复和逻辑删除 |
| 预留表 | `blog_article_like` | 点赞表，P2 使用 |
| 历史保留 | `blog_article_favorite` | `V1.0.0` 已创建但当前产品暂不排期，应用不读写 |
| P1 使用 | `blog_message_board` | 留言表，支持游客留言、审核、管理员回复和通知退订 |
| P1 使用 | `blog_admin_audit_log` | 后台管理操作追加式审计日志 |
| 预留表 | `blog_project` | 项目作品表，待有实际作品后再评估使用 |
| 预留表 | `blog_friend_link` | 友链表，P2 使用 |

### 2.3 设计原则

- P0 阶段 `Refresh Token` 以数据库会话表为准，刷新、退出、修改密码、禁用用户等安全事件通过会话表做失效控制；修改用户角色不直接撤销 Refresh Token
- P1 已使用 Redis 缓存用户 `tokenVersion`，用于降低 Access Token 鉴权时的数据库查询成本；缓存未命中或 Redis 不可用时回源数据库，缓存有效期与 Access Token 有效期一致
- Refresh Token 会话仍以 `blog_auth_session` 为权威来源，Redis 运行态会话与刷新宽限期不在本次 `tokenVersion` 实现范围内
- P1 阶段后台管理操作审计日志建议使用独立审计表，记录操作者、目标资源、操作类型、操作结果和操作时间
- `blog_admin_audit_log` 只允许应用追加和管理员查询，不提供修改或删除接口；成功日志与业务操作同事务提交，失败日志在业务回滚后独立提交
- `Refresh Token` 使用轮转机制；每次刷新成功后，旧会话记录标记为 `REVOKED`，新 Refresh Token 对应新的 `ACTIVE` 会话记录
- P0 阶段不因普通刷新失败自动撤销用户全部活跃 Refresh Token；修改密码、用户禁用、管理员强制下线等明确安全事件可按业务规则撤销全部会话
- 短暂宽限期用于处理网络波动下的幂等重试；P1 接入 Redis 后，可由 Redis 记录旧 `token_jti` 到新令牌结果的短 TTL 映射，数据库继续保留审计状态
- 评论、点赞等互动能力虽然不在 P0 落地，但数据库结构先预留；P1 启用评论表并通过新 migration 扩展审核、根评论和逻辑删除字段；历史版本遗留的收藏结构单独说明
- 统计字段如 `comment_count`、`like_count` 放在主表冗余，行为明细拆分到独立表
- `blog_article.comment_count` 统计文章下全部 `APPROVED` 评论，包括顶层评论和回复
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
| `blog_comment` | `root_id` | `blog_comment.id` | 顶层评论为空；所有层级回复必须指向所属顶层评论 |
| `blog_comment` | `reviewed_by` | `blog_user.id` | 审核、拒绝或隐藏评论时记录管理员用户 ID |
| `blog_comment` | `deleted_by` | `blog_user.id` | 用户或管理员删除评论时记录操作者用户 ID |
| `blog_article_like` | `article_id` | `blog_article.id` | 点赞前必须确认文章存在且可见 |
| `blog_article_like` | `user_id` | `blog_user.id` | 点赞前必须确认用户存在且未被禁用 |
| `blog_message_board` | `user_id` | `blog_user.id` | 登录用户留言时记录用户 ID；游客留言时允许为空，历史游客留言不自动关联后注册用户 |
| `blog_message_board` | `parent_id` | `blog_message_board.id` | 管理员回复时必须确认父留言是已通过的顶层留言 |

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
    token_version BIGINT NOT NULL DEFAULT 0
        CHECK (token_version >= 0),
    avatar_url VARCHAR(500) NOT NULL DEFAULT '',
    bio VARCHAR(100) NOT NULL DEFAULT '',
    email_verified BOOLEAN NOT NULL DEFAULT FALSE,
    email_verified_at TIMESTAMPTZ,
    last_login_at TIMESTAMPTZ,
    deleted_at TIMESTAMPTZ,
    created_at TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    updated_at TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    CONSTRAINT chk_blog_user_username_length
        CHECK (char_length(username) BETWEEN 2 AND 20),
    CONSTRAINT chk_blog_user_username_format
        CHECK (username ~ '^[A-Za-z0-9_-]{2,20}$'),
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

- `username` 长度为 2-20 个字符，只允许英文字母、数字、下划线和短横线，不限制首字符类型；保留注册时输入的原始大小写，用于展示和公开资料；唯一性和登录查询按 `LOWER(username)` 做大小写不敏感处理
- `email` 唯一性和登录查询按 `LOWER(email)` 做大小写不敏感处理，产品层统一将 `A@example.com` 和 `a@example.com` 视为同一个邮箱账号
- 例如允许用户注册展示名 `Sanjuu`，但不允许另一个人再注册 `sanjuu`；登录时输入 `sanjuu`、`SANJUU`、`Sanjuu` 都能找到同一个账号

### 字段表

| 字段名称 | 字段类型 | 字段解释 | 业务例子 |
| --- | --- | --- | --- |
| `id` | `BIGSERIAL` | 用户主键 ID，可用于公开作者信息、登录态、后台管理和逻辑外键关联 | `10001` |
| `username` | `VARCHAR(20)` | 登录用户名，长度 2-20 个字符，只允许英文字母、数字、下划线和短横线；要求唯一，注册后不可修改 | `ccsanjuu` |
| `nickname` | `VARCHAR(20)` | 展示昵称，长度 1-20 个字符，注册时默认使用用户名初始化 | `sanjuu` |
| `email` | `VARCHAR(255)` | 用户邮箱，要求唯一，支持邮箱登录 | `ccsanjuu@example.com` |
| `password_hash` | `VARCHAR(255)` | 密码哈希值，禁止明文存储 | `$2a$10$abc...` |
| `role` | `VARCHAR(20)` | 用户角色，区分管理员和普通用户 | `ADMIN`、`USER` |
| `status` | `VARCHAR(20)` | 用户状态，控制是否允许登录 | `ACTIVE`、`DISABLED` |
| `token_version` | `BIGINT` | Access Token 版本号；修改密码、禁用用户或修改角色时原子递增，使旧 Access Token 立即失效 | `0`、`1` |
| `avatar_url` | `VARCHAR(500)` | 用户头像地址，P0 可为空；P1 保存 OSS 自定义域名的公开 URL | `https://img.example.com/avatars/10002/avatar.jpg` |
| `bio` | `VARCHAR(100)` | 用户个人简介，换行和空行均计入长度 | `专注后端和前端工程化` |
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
- 修改密码或禁用用户：将该用户全部 `ACTIVE` Refresh Token 会话更新为 `REVOKED`，同时原子递增 `blog_user.token_version`，使已签发 Access Token 立即失效。
- 修改用户角色：不直接撤销 Refresh Token，但原子递增 `blog_user.token_version`，使旧 Access Token 立即失效；使用现有 Refresh Token 刷新或重新登录后取得携带新角色的新 Access Token。
- Access Token 携带签发时的 `tokenVersion`；鉴权时优先与 Redis 缓存比较，缓存未命中或 Redis 不可用时回源 `blog_user.token_version`。数据库是持久化权威来源，Redis 只保存可重建缓存。
- 登录、刷新登录态、修改密码、禁用用户和修改角色时锁定同一条 `blog_user` 记录；登录取得锁后使用最新密码和账号状态继续校验，刷新请求取得锁后必须重新校验旧 Refresh Token，避免并发账号安全操作重新产生有效会话。
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
    search_vector TSVECTOR GENERATED ALWAYS AS (
        SETWEIGHT(TO_TSVECTOR('public.zhparser_cfg'::regconfig, title), 'A') ||
        SETWEIGHT(TO_TSVECTOR('public.zhparser_cfg'::regconfig, summary), 'B') ||
        SETWEIGHT(TO_TSVECTOR('public.zhparser_cfg'::regconfig, content_text), 'C')
    ) STORED,
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

CREATE INDEX IF NOT EXISTS idx_blog_article_search_vector
    ON blog_article
    USING GIN (search_vector);
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
| `search_vector` | `TSVECTOR` | 由标题、摘要和纯文本正文生成的全文检索向量，标题、摘要、正文权重依次为 A、B、C | `'spring':2A '安全':18C` |
| `cover_url` | `VARCHAR(500)` | 文章封面地址，可手工录入外部 URL，或保存 P1 图片上传接口返回的 OSS URL | `https://img.example.com/articles/covers/2026/08/cover.png` |
| `status` | `VARCHAR(20)` | 文章状态 | `DRAFT`、`PUBLISHED`、`OFFLINE` |
| `category_id` | `BIGINT` | 文章所属二级分类 ID，应用层需校验不能绑定一级分类 | `21001` |
| `author_id` | `BIGINT` | 文章作者 ID，当前通常是管理员 | `10001` |
| `is_top` | `BOOLEAN` | 是否置顶 | `true` |
| `allow_comment` | `BOOLEAN` | 是否允许评论，先预留控制字段 | `true` |
| `view_count` | `INTEGER` | 浏览量冗余字段，P2 确定有效浏览与去重规则后启用真实统计 | `128` |
| `comment_count` | `INTEGER` | 评论数冗余字段，P1 启用 | `6` |
| `like_count` | `INTEGER` | 点赞数冗余字段，P2 启用 | `35` |
| `favorite_count` | `INTEGER` | 历史预留收藏数冗余字段，当前 API 不返回且应用不读写 | `12` |
| `published_at` | `TIMESTAMPTZ` | 发布时间，草稿阶段可为空 | `2026-04-22 23:00:00+08` |
| `created_at` | `TIMESTAMPTZ` | 创建时间 | `2026-04-22 21:30:00+08` |
| `updated_at` | `TIMESTAMPTZ` | 更新时间 | `2026-04-22 22:45:00+08` |

说明：

- 例如一级分类 `技术` 下可挂二级分类 `Java`、`算法`、`前端三剑客`
- 文章的 `category_id` 应指向 `Java`、`算法` 这类二级分类，而不是 `技术` 这类一级分类
- 保存或更新文章时，应用层应以 `content_md` 为源生成 `content_html` 与 `content_text`
- P1 文章搜索使用 PostgreSQL `zhparser` 解析 `title`、`summary` 和 `content_text`，并通过生成列 `search_vector` 与 GIN 索引完成全文检索
- `search_vector` 为标题、摘要、正文分别设置 A、B、C 权重；有关键词且使用默认排序时通过 `ts_rank` 计算相关度，权重影响标题、摘要和正文的匹配分数
- 搜索查询使用 `plainto_tsquery('public.zhparser_cfg', keyword)`，多个解析后的检索词之间为 AND 关系；单字符未产生词元时由应用查询对三个字段执行字面量包含匹配兜底
- `zhparser` 扩展和 `public.zhparser_cfg` 均属于数据库级对象；每个由 Flyway 管理的数据库都必须执行对应 migration，不能只依赖容器首次初始化脚本
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

## 4. P1 已启用与后续预留表

## 4.1 P1 已启用表：`blog_comment`

### SQL（PostgreSQL）

```sql
CREATE TABLE IF NOT EXISTS blog_comment (
    id BIGSERIAL PRIMARY KEY,
    article_id BIGINT NOT NULL,
    user_id BIGINT NOT NULL,
    parent_id BIGINT,
    root_id BIGINT,
    content TEXT NOT NULL,
    status VARCHAR(20) NOT NULL DEFAULT 'PENDING'
        CHECK (status IN ('PENDING', 'APPROVED', 'REJECTED', 'HIDDEN', 'DELETED')),
    reviewed_by BIGINT,
    reviewed_at TIMESTAMPTZ,
    moderation_reason VARCHAR(255),
    deleted_by BIGINT,
    deleted_at TIMESTAMPTZ,
    created_at TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    updated_at TIMESTAMPTZ NOT NULL DEFAULT NOW()
);

CREATE INDEX IF NOT EXISTS idx_blog_comment_article_status
    ON blog_comment (article_id, status);

CREATE INDEX IF NOT EXISTS idx_blog_comment_user_id
    ON blog_comment (user_id);

CREATE INDEX IF NOT EXISTS idx_blog_comment_parent_id
    ON blog_comment (parent_id);

CREATE INDEX IF NOT EXISTS idx_blog_comment_root_status_created_at
    ON blog_comment (root_id, status, created_at, id);

CREATE INDEX IF NOT EXISTS idx_blog_comment_top_level_paging
    ON blog_comment (article_id, status, created_at DESC, id DESC)
    WHERE parent_id IS NULL;

CREATE INDEX IF NOT EXISTS idx_blog_comment_user_article_created_at
    ON blog_comment (user_id, article_id, created_at DESC);
```

### 字段表

| 字段名称 | 字段类型 | 字段解释 | 业务例子 |
| --- | --- | --- | --- |
| `id` | `BIGSERIAL` | 评论主键 ID | `60001` |
| `article_id` | `BIGINT` | 所属文章 ID | `40001` |
| `user_id` | `BIGINT` | 评论用户 ID | `10002` |
| `parent_id` | `BIGINT` | 父评论 ID，一级评论为空，回复评论时有值 | `60000` |
| `root_id` | `BIGINT` | 所属顶层评论 ID；顶层评论为空，所有后代回复均指向同一顶层评论 | `60001` |
| `content` | `TEXT` | 评论内容 | `这篇文章对双 Token 的解释很清楚` |
| `status` | `VARCHAR(20)` | 评论状态 | `PENDING`、`APPROVED`、`REJECTED`、`HIDDEN`、`DELETED` |
| `reviewed_by` | `BIGINT` | 最近一次审核、拒绝或隐藏操作的管理员用户 ID | `10001` |
| `reviewed_at` | `TIMESTAMPTZ` | 最近一次审核、拒绝或隐藏时间 | `2026-05-01 10:30:00+08` |
| `moderation_reason` | `VARCHAR(255)` | 管理员拒绝、隐藏或删除评论时填写的处理原因 | `包含人身攻击内容` |
| `deleted_by` | `BIGINT` | 删除评论的用户或管理员 ID | `10002` |
| `deleted_at` | `TIMESTAMPTZ` | 逻辑删除时间 | `2026-05-01 10:40:00+08` |
| `created_at` | `TIMESTAMPTZ` | 创建时间 | `2026-05-01 10:20:00+08` |
| `updated_at` | `TIMESTAMPTZ` | 更新时间 | `2026-05-01 10:25:00+08` |

### 状态、层级与计数规则

- 顶层评论的 `parent_id` 和 `root_id` 均为空
- 回复的 `parent_id` 指向直接父评论，`root_id` 指向所属顶层评论；无限层级回复在前端统一平铺到第二层
- 数据库默认将新评论设为 `PENDING`；应用层创建普通用户评论时保持该状态，创建管理员评论时显式写为 `APPROVED`
- 仅 `APPROVED` 可向其他用户和游客公开；管理员直接创建的 `APPROVED` 评论不填写 `reviewed_by`、`reviewed_at`
- `REJECTED` 表示审核未通过，`HIDDEN` 表示曾公开后被管理员隐藏，`DELETED` 表示已逻辑删除
- 用户或管理员删除评论时，在同一事务中将目标评论及全部后代标记为 `DELETED`，并写入 `deleted_by`、`deleted_at`
- `blog_article.comment_count` 只统计全部 `APPROVED` 评论，包括顶层评论和回复
- 状态流转、子树删除和 `comment_count` 增减必须在同一事务中完成，避免冗余计数失真
- 创建回复、审核和删除属于同一评论树的并发写操作，必须在事务中锁定所属顶层评论，并在取得锁后重新校验目标评论状态
- 统一的评论树锁用于避免删除期间新增回复或审核状态变化造成孤立数据和 `comment_count` 失真
- 评论审核、拒绝、隐藏或删除的结构化操作历史由 P1 后台操作审计日志记录；评论表字段仅保存当前状态和最近一次处理信息

### P2 评论直接回复通知预留

- P2 为单条评论增加自愿订阅直接回复邮件的能力，订阅者固定为该评论作者，收件地址使用用户账号邮箱
- 计划通过新的 Flyway migration 增加 `notify_on_reply` 和随机不透明 `unsubscribe_token`，不修改已经执行的 P1 migration
- `unsubscribe_token` 仅能关闭对应评论未来的直接回复通知，并建立非空值唯一索引
- 具体字段与索引只在 P2 接口模型确定后落地；当前 P1 数据库结构和 OpenAPI 不提前变更

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

## 4.3 历史保留表：`blog_article_favorite`

该表由已执行的 `V1.0.0` migration 创建，当前收藏功能暂不排期，应用不提供收藏接口，
也不读写该表。保留以下结构仅用于说明现有数据库状态；未来如启用收藏，应通过新的
Flyway migration 和接口设计恢复业务约束，不修改历史 migration。

### 历史 SQL（PostgreSQL）

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
        CHECK (status IN ('PENDING', 'APPROVED', 'REJECTED', 'HIDDEN', 'DELETED')),
    notify_on_reply BOOLEAN NOT NULL DEFAULT FALSE,
    unsubscribe_token VARCHAR(128),
    moderation_reason VARCHAR(255),
    reviewed_by BIGINT,
    reviewed_at TIMESTAMPTZ,
    deleted_by BIGINT,
    deleted_at TIMESTAMPTZ,
    created_at TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    updated_at TIMESTAMPTZ NOT NULL DEFAULT NOW()
);

CREATE INDEX IF NOT EXISTS idx_blog_message_board_status
    ON blog_message_board (status);

CREATE INDEX IF NOT EXISTS idx_blog_message_board_user_id
    ON blog_message_board (user_id);

CREATE INDEX IF NOT EXISTS idx_blog_message_board_parent_status_created
    ON blog_message_board (parent_id, status, created_at, id);

CREATE UNIQUE INDEX IF NOT EXISTS uq_blog_message_board_unsubscribe_token
    ON blog_message_board (unsubscribe_token)
    WHERE unsubscribe_token IS NOT NULL;
```

### 字段表

| 字段名称 | 字段类型 | 字段解释 | 业务例子 |
| --- | --- | --- | --- |
| `id` | `BIGSERIAL` | 留言主键 ID | `90001` |
| `user_id` | `BIGINT` | 登录用户留言时关联的用户 ID，游客留言可为空 | `10002` |
| `parent_id` | `BIGINT` | 回复留言时指向父留言 ID | `90000` |
| `nickname` | `VARCHAR(50)` | 留言昵称；登录用户保存当前昵称快照，游客必填 | `路人甲` |
| `email` | `VARCHAR(255)` | 私有联系邮箱，游客可选；管理员可查看，前台不返回 | `guest@example.com` |
| `content` | `TEXT` | 留言内容 | `博客很清爽，期待评论功能上线` |
| `status` | `VARCHAR(20)` | 留言状态 | `PENDING`、`APPROVED`、`REJECTED`、`HIDDEN`、`DELETED` |
| `notify_on_reply` | `BOOLEAN` | 是否接收该顶层留言的后续回复通知 | `TRUE` |
| `unsubscribe_token` | `VARCHAR(128)` | 随机退订令牌，仅用于该顶层留言通知退订 | `随机不透明字符串` |
| `moderation_reason` | `VARCHAR(255)` | 拒绝、隐藏或删除原因 | `内容不适合公开` |
| `reviewed_by` | `BIGINT` | 最近一次审核操作的管理员 ID | `10001` |
| `reviewed_at` | `TIMESTAMPTZ` | 最近一次审核时间 | `2026-05-10 14:05:00+08` |
| `deleted_by` | `BIGINT` | 逻辑删除操作者 ID | `10001` |
| `deleted_at` | `TIMESTAMPTZ` | 逻辑删除时间 | `2026-05-10 14:06:00+08` |
| `created_at` | `TIMESTAMPTZ` | 创建时间 | `2026-05-10 14:00:00+08` |
| `updated_at` | `TIMESTAMPTZ` | 更新时间 | `2026-05-10 14:10:00+08` |

P1 留言邮件使用纯文本；P2 升级为 `multipart/alternative` 只改变邮件内容格式，不需要修改现有留言订阅字段。

## 4.5 P1 使用表：`blog_admin_audit_log`

### SQL（PostgreSQL）

```sql
CREATE TABLE IF NOT EXISTS blog_admin_audit_log (
    id BIGSERIAL PRIMARY KEY,
    operator_id BIGINT NOT NULL,
    operator_username VARCHAR(20) NOT NULL,
    resource_type VARCHAR(30) NOT NULL,
    resource_id TEXT,
    action VARCHAR(30) NOT NULL,
    action_detail VARCHAR(255),
    result VARCHAR(20) NOT NULL CHECK (result IN ('SUCCESS', 'FAILURE')),
    failure_code INTEGER,
    failure_message VARCHAR(255),
    request_method VARCHAR(10) NOT NULL,
    request_path VARCHAR(500) NOT NULL,
    created_at TIMESTAMPTZ NOT NULL DEFAULT NOW()
);

CREATE INDEX IF NOT EXISTS idx_blog_admin_audit_log_created_at
    ON blog_admin_audit_log (created_at DESC, id DESC);

CREATE INDEX IF NOT EXISTS idx_blog_admin_audit_log_operator_created
    ON blog_admin_audit_log (operator_id, created_at DESC);

CREATE INDEX IF NOT EXISTS idx_blog_admin_audit_log_resource_created
    ON blog_admin_audit_log (resource_type, created_at DESC);

CREATE INDEX IF NOT EXISTS idx_blog_admin_audit_log_action_result_created
    ON blog_admin_audit_log (action, result, created_at DESC);
```

### 字段表

| 字段名称 | 字段类型 | 字段解释 |
| --- | --- | --- |
| `id` | `BIGSERIAL` | 审计日志主键和稳定排序标识 |
| `operator_id` | `BIGINT` | 操作管理员用户 ID |
| `operator_username` | `VARCHAR(20)` | 操作时的用户名快照 |
| `resource_type` | `VARCHAR(30)` | 目标资源类型，如 `ARTICLE`、`COMMENT`、`MESSAGE` |
| `resource_id` | `TEXT` | 目标 ID、批量 ID 列表或 OSS 公开 URL；无结果时为空 |
| `action` | `VARCHAR(30)` | 操作类型，如 `CREATE`、`MODERATE`、`UPLOAD` |
| `action_detail` | `VARCHAR(255)` | 状态、审核动作或上传场景等非敏感明细 |
| `result` | `VARCHAR(20)` | `SUCCESS` 或 `FAILURE` |
| `failure_code` | `INTEGER` | 失败时的业务码，成功时为空 |
| `failure_message` | `VARCHAR(255)` | 失败时的安全错误说明，成功时为空 |
| `request_method` | `VARCHAR(10)` | HTTP 方法 |
| `request_path` | `VARCHAR(500)` | 不包含 query 参数的请求路径 |
| `created_at` | `TIMESTAMPTZ` | 审计记录创建时间 |

审计日志只允许追加和查询，不保存请求体、查询参数、密码、Token、邮箱、评论或留言正文、文件原名等敏感信息。成功日志和业务操作在同一事务中提交；失败日志在业务事务回滚后通过独立事务保存。

## 4.6 表名：`blog_project`

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

## 4.7 表名：`blog_friend_link`

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
9. `blog_article_favorite`（历史保留，当前不启用）
10. `blog_message_board`
11. `blog_admin_audit_log`
12. `blog_project`
13. `blog_friend_link`

## 6. 落地建议

- P0 最少先落地 `blog_user`、`blog_auth_session`、`blog_category`、`blog_tag`、`blog_article`、`blog_article_tag`
- 历史版本已创建的预留表不会因功能暂不排期而回滚，后续结构变更必须新增 migration
- `blog_auth_session` 可以作为 Redis 的补充审计表，不要求所有鉴权逻辑都依赖数据库
- 文章封面与头像上传虽然在 P1 实现，但建议 P0 先把 URL 字段建好
- P1 图片二进制保存在阿里云 OSS，业务表和 Markdown 仅保存公开 URL；当前不新增通用文件资源表
- P1 不维护图片引用关系，也不自动删除被替换或失去引用的 OSS 对象；后续确有统一资源管理需求时再设计 `blog_asset` 表
- 留言表的状态扩展、通知字段和索引通过 `V1.1.4` migration 落地，不修改已执行的初始 migration
- 留言通知令牌使用随机不透明值；数据库泄露时不会暴露用户密码或登录 Token，令牌仅能关闭对应顶层留言的后续通知
- 用户名长度和格式约束通过 `V1.1.6` migration 更新；升级前若存在不符合新规则的用户名，迁移会失败并要求先处理存量数据
- 用户简介字段通过 `V1.1.7` migration 缩短为 `VARCHAR(100)`；升级前若存在超过 100 个字符的简介，迁移会失败并要求先处理存量数据
