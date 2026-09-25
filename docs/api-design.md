# 个人技术博客系统接口文档

## 1. 文档信息

- 项目名称：个人技术博客系统
- 文档版本：v1.0
- 文档日期：2026-04-22
- 接口版本：`/api/v1/**`
- 对应文档：[PRD.md](./PRD.md)、[database-design.md](./database-design.md)

## 2. 通用约定

### 2.1 基础路径

- 基础前缀：`/api/v1`
- 数据格式：`application/json`
- 字符集：`UTF-8`
- 时间格式：ISO 8601
  例：`2026-04-22T22:10:00+08:00`

### 2.2 接口版本管理

- 当前版本统一使用 `v1`
- 后续若出现兼容性变更，新增为 `v2`
- 原则上不同版本接口并行一段时间，避免前端一次性重构

### 2.3 鉴权方式

- Access Token：用于访问受保护接口
- Refresh Token：用于刷新登录态和退出登录
- Access Token 通过请求头传递：

```http
Authorization: Bearer <access_token>
```

说明：

- Access Token 有效期较短，P0 默认 15 分钟。
- Refresh Token 不返回给前端 JavaScript 读取，登录和刷新成功后由后端通过 `Set-Cookie` 写入 `HttpOnly` Cookie，前端刷新登录态和退出登录时由浏览器自动携带该 Cookie。
- Refresh Token Cookie 名称为 `refresh_token`，路径为 `/api/v1/auth`，只随 `/api/v1/auth/**` 请求发送；Cookie 使用 `HttpOnly`、`SameSite=Lax`，生产 HTTPS 环境应启用 `Secure`。
- Access Token 携带签发时的用户角色、状态快照和内部 `tokenVersion` claim；`tokenVersion` 不作为接口响应字段单独暴露。
- 每次解析 Access Token 后，后端优先读取 Redis 中的当前版本，缓存未命中或 Redis 不可用时回源数据库；JWT 版本与当前版本不一致时返回 `101001`。缓存有效期与 Access Token 有效期一致。
- 用户修改密码或被禁用时，后端撤销其全部活跃 Refresh Token 并递增 `tokenVersion`；修改用户角色时不撤销 Refresh Token，但仍递增 `tokenVersion`，使旧 Access Token 立即失效。
- Redis 中的 `tokenVersion` 是可重建缓存，数据库 `blog_user.token_version` 是持久化权威来源；Redis 重启或缓存丢失不会重置版本。

### 2.4 权限级别

| 权限级别 | 说明 |
| --- | --- |
| `PUBLIC` | 游客可访问，无需登录 |
| `LOGIN` | 登录用户可访问，管理员也可访问 |
| `ADMIN` | 仅管理员可访问 |

公开用户资料、文章作者资料卡等前台公开场景可以返回用户 ID，并可使用 `userId` 作为路径标识。`username` 主要作为展示字段和登录标识，注册后不可修改。

### 2.5 通用响应结构

所有接口统一返回如下结构：

```json
{
  "code": 0,
  "message": "成功",
  "data": {}
}
```

#### 响应外层字段说明

| 字段名称 | 字段类型 | 字段解释 | 业务例子 |
| --- | --- | --- | --- |
| `code` | `Integer` | 业务状态码，`0` 表示成功 | `0` |
| `message` | `String` | 业务描述信息 | `成功` |
| `data` | `Object/Array/null` | 实际返回数据 | `{}`、`[]`、`null` |

#### HTTP 状态码约定

接口响应同时使用 HTTP 状态码和响应体中的业务状态码：

- HTTP 状态码用于表达请求在协议层面的粗粒度结果，便于浏览器、Axios、网关和日志系统识别。
- 业务状态码用于表达项目内部的具体业务结果，前端应优先根据 `code` 判断精确错误原因。
- 业务错误不统一返回 HTTP `200`，而是返回与错误类型匹配的 HTTP 状态码。

| HTTP 状态码 | 含义 | 前端处理建议 |
| --- | --- | --- |
| `200` | 请求成功 | 正常读取 `data` |
| `400` | 请求参数不合法 | 展示字段校验、请求体格式或参数类型错误提示 |
| `401` | 未登录、Token 无效/过期或登录凭证错误 | 根据业务状态码决定刷新 Token、跳转登录页或提示账号密码错误 |
| `403` | 已登录但无权限，或账号被禁用 | 提示无权限或账号禁用 |
| `404` | 请求资源不存在 | 展示空状态、资源不存在或 404 提示 |
| `409` | 业务冲突 | 展示用户名/邮箱已存在、资源被占用等冲突提示 |
| `500` | 系统内部异常 | 统一提示系统异常，避免暴露服务端内部细节 |

### 2.6 常见业务状态码

以下为当前已定义的业务状态码：

| 业务状态码 | HTTP 状态码 | 消息 | 含义 |
| --- | --- | --- | --- |
| `0` | `200` | `成功` | 请求成功 |
| `199001` | `400` | `请求参数不合法` | 请求参数校验失败、请求体格式错误或字段类型不匹配 |
| `199404` | `404` | `请求资源不存在` | 请求的资源不存在 |
| `299001` | `500` | `系统内部异常` | 服务端出现未分类异常 |
| `101001` | `401` | `未登录或 Access Token 无效` | 访问受保护接口时未登录、Access Token 缺失、格式错误或签名无效 |
| `101002` | `401` | `Access Token 已过期` | Access Token 已过期，前端可尝试使用 Refresh Token 刷新登录态 |
| `101003` | `403` | `无权限访问` | 当前身份无权访问目标资源 |
| `101004` | `401` | `Refresh Token 无效或已过期` | Refresh Token 缺失、格式错误、签名无效、已过期、已撤销或找不到对应会话 |
| `102001` | `404` | `用户不存在` | 指定用户不存在 |
| `102002` | `409` | `用户名已存在` | 注册或修改资料时用户名冲突 |
| `102003` | `401` | `账号或密码错误` | 登录凭证校验失败 |
| `102004` | `409` | `邮箱已存在` | 注册时邮箱冲突 |
| `102005` | `403` | `用户已被禁用` | 用户状态为禁用，不允许登录或刷新登录态 |
| `102006` | `400` | `原密码错误` | 修改密码时原密码校验失败 |
| `102007` | `403` | `当前用户不允许修改自己的角色` | 管理员修改当前登录用户自身角色时被拒绝 |
| `102008` | `403` | `当前用户不允许修改自己的状态` | 管理员修改当前登录用户自身状态时被拒绝 |
| `103001` | `404` | `文章不存在` | 指定文章不存在 |
| `103002` | `404` | `文章分类不存在` | 创建、更新、筛选文章或前台展示文章时指定分类不存在 |
| `103003` | `400` | `文章只能绑定二级分类` | 创建或更新文章时绑定了一级分类 |
| `103004` | `409` | `文章分类已禁用` | 创建、更新、筛选文章或前台展示文章时关联分类已禁用 |
| `103005` | `404` | `文章标签不存在` | 创建或更新文章时指定标签不存在 |
| `103006` | `409` | `文章标签已禁用` | 创建或更新文章时指定标签已禁用 |
| `103007` | `409` | `文章状态流转不合法` | 创建、更新或修改文章状态时不符合状态流转规则 |
| `103008` | `404` | `文章当前不可见` | 前台访问未发布或已下线文章时，文章对前台不可见 |
| `103009` | `404` | `文章作者不存在` | 前台展示文章详情时，文章关联作者不存在 |
| `104001` | `409` | `分类名称已存在` | 创建或更新分类时，同级分类下名称冲突 |
| `104002` | `409` | `该分类下存在子分类，请先删除或迁移子分类` | 删除一级分类时，该分类下仍存在二级分类 |
| `104003` | `409` | `该分类下存在文章，请先迁移文章或删除文章` | 删除二级分类时，该分类下仍有关联文章 |
| `104004` | `404` | `该分类不存在` | 指定分类不存在 |
| `104005` | `400` | `二级分类必须指定父分类` | 创建或更新二级分类时未传父分类 |
| `104006` | `400` | `一级分类不能指定父分类` | 创建或更新一级分类时传入了父分类 |
| `104007` | `400` | `父分类不存在或父分类不是一级分类` | 创建或更新二级分类时父分类不合法 |
| `104008` | `409` | `不允许更新分类的级别` | 更新分类时尝试修改一级/二级分类层级 |
| `106001` | `404` | `评论不存在` | 指定评论不存在、已删除或当前用户不可见 |
| `106002` | `409` | `文章已关闭评论` | 文章 `allow_comment = false`，不允许新增评论或回复 |
| `106003` | `409` | `回复目标不可用` | 父评论不属于当前文章、未审核通过或已不允许回复 |
| `106004` | `429` | `评论过于频繁，请稍后再试` | 同一用户对同一文章 10 秒内重复发表评论或回复 |
| `106005` | `403` | `无权操作该评论` | 普通用户删除不属于自己的评论 |
| `106006` | `409` | `评论状态流转不合法` | 后台审核动作与评论当前状态不匹配 |
| `106007` | `400` | `评论处理原因不能为空` | 后台拒绝、隐藏或删除评论时未填写处理原因 |
| `107001` | `400` | `请选择需要上传的图片` | Multipart 图片为空或未携带文件 |
| `107002` | `415` | `仅支持 JPG、JPEG、PNG、WebP 和 GIF 图片` | 图片真实文件类型不在允许范围内 |
| `107003` | `413` | `图片大小超过限制` | 头像超过 2 MB，或其他图片超过 10 MB |
| `107004` | `502` | `图片上传失败，请稍后重试` | 后端调用对象存储上传失败 |
| `107005` | `429` | `头像上传过于频繁，请稍后再试` | 同一用户每分钟超过 1 次，或每 24 小时超过 10 次头像上传 |
| `108001` | `404` | `留言不存在` | 指定留言不存在、已删除或当前用户不可见 |
| `108003` | `409` | `留言回复目标不可用` | 回复目标不是已通过的顶层留言 |
| `108004` | `429` | `留言过于频繁，请稍后再试` | 同一游客 IP 或登录用户超过留言频率限制 |
| `108005` | `403` | `无权操作该留言` | 登录用户删除不属于自己的顶层留言 |
| `108006` | `409` | `留言状态流转不合法` | 后台处理动作与留言当前状态不匹配 |
| `108007` | `400` | `留言处理原因不能为空` | 拒绝、隐藏或删除留言时未填写原因 |
| `108008` | `400` | `留言昵称不合法` | 游客昵称为空、过长或包含控制字符 |
| `108009` | `400` | `勾选回复通知时必须填写邮箱` | 游客勾选通知但未提供邮箱 |
| `108010` | `400` | `留言退订链接无效或已失效` | 退订令牌不存在或不对应顶层留言 |
| `108011` | `409` | `批量通过的留言必须是待审核顶层留言` | 批量 ID 中包含不存在、非顶层或非待审核留言 |
| `108012` | `400` | `一次最多通过 100 条留言` | 批量通过列表超过 100 条 |
| `108013` | `500` | `留言通知邮件配置不完整` | 启用邮件通知时 SMTP 或发件配置不完整 |

### 2.7 分页结构

列表接口统一返回如下分页结构：

```json
{
  "records": [],
  "pageNum": 1,
  "pageSize": 10,
  "total": 35,
  "totalPages": 4,
  "hasNext": true
}
```

#### 分页字段说明

| 字段名称 | 字段类型 | 字段解释 | 业务例子 |
| --- | --- | --- | --- |
| `records` | `Array` | 当前页数据列表 | `[{...}, {...}]` |
| `pageNum` | `Integer` | 当前页码，从 1 开始 | `1` |
| `pageSize` | `Integer` | 每页条数 | `10` |
| `total` | `Long` | 总记录数 | `35` |
| `totalPages` | `Integer` | 总页数 | `4` |
| `hasNext` | `Boolean` | 是否存在下一页 | `true` |

### 2.8 枚举说明

| 枚举字段 | 可选值 | 说明 |
| --- | --- | --- |
| `role` | `ADMIN`、`USER` | 用户角色 |
| `userStatus` | `ACTIVE`、`DISABLED` | 用户状态 |
| `articleStatus` | `DRAFT`、`PUBLISHED`、`OFFLINE` | 文章状态 |
| `categoryStatus` | `ENABLED`、`DISABLED` | 分类状态 |
| `categoryLevel` | `1`、`2` | 分类层级，`1` 为一级分类，`2` 为二级分类 |
| `tagStatus` | `ENABLED`、`DISABLED` | 标签状态 |
| `commentStatus` | `PENDING`、`APPROVED`、`REJECTED`、`HIDDEN`、`DELETED` | 评论状态 |
| `commentModerationAction` | `APPROVE`、`REJECT`、`HIDE`、`DELETE` | 后台评论处理动作 |
| `messageStatus` | `PENDING`、`APPROVED`、`REJECTED`、`HIDDEN`、`DELETED` | 留言状态 |
| `messageType` | `TOP_LEVEL`、`REPLY` | 留言层级 |
| `messageModerationAction` | `APPROVE`、`REJECT`、`HIDE`、`DELETE` | 后台留言处理动作 |
| `imageUploadScene` | `ARTICLE_COVER`、`ARTICLE_CONTENT`、`PROJECT_COVER` | 后台图片使用场景 |

## 3. 接口总览

| 模块 | 路由 | 路径 | 权限 | 说明 |
| --- | --- | --- | --- | --- |
| 认证 | `POST` | `/api/v1/auth/register` | `PUBLIC` | 用户注册 |
| 认证 | `POST` | `/api/v1/auth/login` | `PUBLIC` | 用户登录 |
| 认证 | `POST` | `/api/v1/auth/refresh` | `PUBLIC` | 刷新登录态 |
| 认证 | `POST` | `/api/v1/auth/logout` | `PUBLIC` | 用户退出登录 |
| 前台文章 | `GET` | `/api/v1/articles` | `PUBLIC` | 获取已发布文章分页列表 |
| 前台文章 | `GET` | `/api/v1/articles/{articleId}` | `PUBLIC` | 获取文章详情 |
| 前台评论 | `GET` | `/api/v1/articles/{articleId}/comments` | `PUBLIC` | 获取顶层评论分页列表，可选登录态用于返回本人评论 |
| 前台评论 | `POST` | `/api/v1/articles/{articleId}/comments` | `LOGIN` | 发表评论或回复 |
| 前台评论 | `GET` | `/api/v1/comments/{commentId}/replies` | `PUBLIC` | 按需获取顶层评论下的平铺回复 |
| 前台评论 | `DELETE` | `/api/v1/comments/{commentId}` | `LOGIN` | 删除自己的评论及其后代 |
| 前台分类 | `GET` | `/api/v1/categories` | `PUBLIC` | 获取启用分类列表 |
| 前台标签 | `GET` | `/api/v1/tags` | `PUBLIC` | 获取启用标签列表 |
| 前台用户 | `GET` | `/api/v1/users/{userId}/public-profile` | `PUBLIC` | 获取用户公开资料卡 |
| 个人中心 | `GET` | `/api/v1/users/me` | `LOGIN` | 获取当前登录用户信息 |
| 个人中心 | `PUT` | `/api/v1/users/me/profile` | `LOGIN` | 更新个人资料 |
| 个人中心 | `PUT` | `/api/v1/users/me/password` | `LOGIN` | 修改密码 |
| 个人中心 | `PUT` | `/api/v1/users/me/avatar` | `LOGIN` | 上传并更新当前用户头像 |
| 后台用户 | `GET` | `/api/v1/admin/users` | `ADMIN` | 获取用户分页列表 |
| 后台用户 | `PATCH` | `/api/v1/admin/users/{userId}/status` | `ADMIN` | 修改用户状态 |
| 后台用户 | `PATCH` | `/api/v1/admin/users/{userId}/role` | `ADMIN` | 修改用户角色 |
| 后台文章 | `GET` | `/api/v1/admin/articles` | `ADMIN` | 获取后台文章分页列表 |
| 后台文章 | `GET` | `/api/v1/admin/articles/{articleId}` | `ADMIN` | 获取后台文章详情 |
| 后台文章 | `POST` | `/api/v1/admin/articles` | `ADMIN` | 创建文章 |
| 后台文章 | `PUT` | `/api/v1/admin/articles/{articleId}` | `ADMIN` | 更新文章 |
| 后台文章 | `PATCH` | `/api/v1/admin/articles/{articleId}/status` | `ADMIN` | 修改文章状态 |
| 后台文章 | `DELETE` | `/api/v1/admin/articles/{articleId}` | `ADMIN` | 删除文章 |
| 后台分类 | `GET` | `/api/v1/admin/categories` | `ADMIN` | 获取分类列表 |
| 后台分类 | `POST` | `/api/v1/admin/categories` | `ADMIN` | 创建分类 |
| 后台分类 | `PUT` | `/api/v1/admin/categories/{categoryId}` | `ADMIN` | 更新分类 |
| 后台分类 | `DELETE` | `/api/v1/admin/categories/{categoryId}` | `ADMIN` | 删除分类 |
| 后台标签 | `GET` | `/api/v1/admin/tags` | `ADMIN` | 获取标签列表 |
| 后台标签 | `POST` | `/api/v1/admin/tags` | `ADMIN` | 创建标签 |
| 后台标签 | `PUT` | `/api/v1/admin/tags/{tagId}` | `ADMIN` | 更新标签 |
| 后台标签 | `DELETE` | `/api/v1/admin/tags/{tagId}` | `ADMIN` | 删除标签 |
| 后台审计日志 | `GET` | `/api/v1/admin/audit-logs` | `ADMIN` | 获取后台操作审计日志分页列表 |
| 后台评论 | `GET` | `/api/v1/admin/comments` | `ADMIN` | 获取评论审核分页列表 |
| 后台评论 | `PATCH` | `/api/v1/admin/comments/{commentId}/moderation` | `ADMIN` | 审核、隐藏或删除评论 |
| 前台留言 | `GET` | `/api/v1/messages` | `PUBLIC` | 获取留言及管理员回复分页列表，可选登录态 |
| 前台留言 | `POST` | `/api/v1/messages` | `PUBLIC` | 游客或登录用户发表顶层留言 |
| 前台留言 | `DELETE` | `/api/v1/messages/{messageId}` | `LOGIN` | 登录用户删除自己的顶层留言 |
| 前台留言 | `POST` | `/api/v1/messages/notifications/unsubscribe` | `PUBLIC` | 幂等关闭单条留言后续回复通知 |
| 后台留言 | `GET` | `/api/v1/admin/messages` | `ADMIN` | 获取留言审核分页列表 |
| 后台留言 | `PATCH` | `/api/v1/admin/messages/{messageId}/moderation` | `ADMIN` | 审核、隐藏或删除留言 |
| 后台留言 | `POST` | `/api/v1/admin/messages/{messageId}/replies` | `ADMIN` | 对已通过顶层留言回复 |
| 后台留言 | `PATCH` | `/api/v1/admin/messages/batch-approval` | `ADMIN` | 批量通过待审核顶层留言，最多 100 条 |
| 后台文件 | `POST` | `/api/v1/admin/files/images` | `ADMIN` | 上传文章封面、正文图片或项目封面 |

## 4. 认证模块

## 4.1 用户注册

- 路由：`POST`
- 路径：`/api/v1/auth/register`
- 权限：`PUBLIC`

### 请求参数

#### Header 参数

| 字段名称 | 字段类型 | 必填 | 字段解释 | 业务例子 |
| --- | --- | --- | --- | --- |
| `Content-Type` | `String` | 是 | 请求体类型 | `application/json` |

#### Body 参数

| 字段名称 | 字段类型 | 必填 | 字段解释 | 业务例子 |
| --- | --- | --- | --- | --- |
| `username` | `String` | 是 | 用户名，2-20 位，只允许英文字母、数字、下划线和短横线，不限制首字符类型，需唯一 | `alice_dev` |
| `email` | `String` | 是 | 邮箱，仅支持常见邮箱格式，不能包含空格或中文字符，需唯一 | `alice@example.com` |
| `password` | `String` | 是 | 登录密码，6-32 位；允许英文字母、数字、下划线、短横线和常用 ASCII 特殊字符，不允许中文或空白字符 | `Passw0rd!` |

说明：注册时无需传入 `nickname`，服务端默认使用 `username` 初始化昵称，后续用户可在个人中心修改昵称。
用户名注册后不可修改，前端不应提供用户名修改入口。
用户名和邮箱的唯一性均按大小写不敏感处理，但 `username` 会保留注册时输入的原始大小写用于展示。例如允许用户注册展示名 `Sanjuu`，但不允许另一个人再注册 `sanjuu`；邮箱同理，`A@example.com` 和 `a@example.com` 会识别为同一个邮箱账号，不能重复注册。

### 请求样例

```http
POST /api/v1/auth/register
Content-Type: application/json

{
  "username": "alice_dev",
  "email": "alice@example.com",
  "password": "Passw0rd!"
}
```

### 响应参数

注册成功时，后端仅返回统一成功响应，`data` 为 `null`。前端收到成功结果后提示“注册成功，请登录”并跳转登录页。

### 响应样例

```json
{
  "code": 0,
  "message": "成功",
  "data": null
}
```

## 4.2 用户登录

- 路由：`POST`
- 路径：`/api/v1/auth/login`
- 权限：`PUBLIC`

### 请求参数

#### Body 参数

| 字段名称 | 字段类型 | 必填 | 字段解释 | 业务例子 |
| --- | --- | --- | --- | --- |
| `account` | `String` | 是 | 登录账号，必须是 2-20 位合法用户名或合法邮箱；包含 `@` 时按邮箱登录，否则按用户名登录 | `alice_dev`、`alice@example.com` |
| `password` | `String` | 是 | 登录密码，6-32 位；允许英文字母、数字、下划线、短横线和常用 ASCII 特殊字符，不允许中文或空白字符 | `Passw0rd!` |

说明：用户名和邮箱登录均按大小写不敏感处理。也就是允许用户注册展示名 `Sanjuu`，但不允许另一个人再注册 `sanjuu`；登录时输入 `sanjuu`、`SANJUU`、`Sanjuu` 都能找到同一个账号。邮箱同理，`A@example.com` 和 `a@example.com` 会识别为同一个邮箱账号。

### 请求样例

```http
POST /api/v1/auth/login
Content-Type: application/json

{
  "account": "alice@example.com",
  "password": "Passw0rd!"
}
```

### 响应参数

#### data 字段说明

| 字段名称 | 字段类型 | 字段解释 | 业务例子 |
| --- | --- | --- | --- |
| `accessToken` | `String` | 用于访问受保护接口的令牌 | `eyJhbGciOiJIUzI1NiJ9...` |
| `accessTokenExpiresAt` | `String` | Access Token 过期时间 | `2026-04-22T22:35:00+08:00` |
| `refreshTokenExpiresAt` | `String` | Refresh Token Cookie 过期时间 | `2026-04-29T22:20:00+08:00` |
| `tokenType` | `String` | Token 类型 | `Bearer` |
| `user.id` | `Long` | 当前登录用户 ID | `10002` |
| `user.username` | `String` | 用户名 | `alice_dev` |
| `user.nickname` | `String` | 昵称 | `Alice` |
| `user.email` | `String` | 邮箱 | `alice@example.com` |
| `user.role` | `String` | 用户角色 | `USER` |
| `user.status` | `String` | 用户状态 | `ACTIVE` |
| `user.avatarUrl` | `String` | 用户头像地址 | `` |
| `user.bio` | `String` | 用户简介 | `热爱前后端开发` |

### 响应样例

```json
{
  "code": 0,
  "message": "成功",
  "data": {
    "accessToken": "eyJhbGciOiJIUzI1NiJ9.access.token",
    "accessTokenExpiresAt": "2026-04-22T22:35:00+08:00",
    "refreshTokenExpiresAt": "2026-04-29T22:20:00+08:00",
    "tokenType": "Bearer",
    "user": {
      "id": 10002,
      "username": "alice_dev",
      "nickname": "Alice",
      "email": "alice@example.com",
      "role": "USER",
      "status": "ACTIVE",
      "avatarUrl": "",
      "bio": "热爱前后端开发"
    }
  }
}
```

登录成功时，后端同时通过响应头写入 Refresh Token Cookie：

```http
Set-Cookie: refresh_token=<refresh_token>; Max-Age=604800; Path=/api/v1/auth; HttpOnly; SameSite=Lax
```

## 4.3 刷新登录态

- 路由：`POST`
- 路径：`/api/v1/auth/refresh`
- 权限：`PUBLIC`

刷新登录态采用 Refresh Token 轮转机制。Refresh Token 由浏览器通过 `refresh_token` HttpOnly Cookie 自动携带，不放在请求体中。每次刷新成功后，后端都会返回新的 Access Token，通过 `Set-Cookie` 写入新的 Refresh Token Cookie，并撤销本次请求携带的旧 Refresh Token。正常刷新只轮转当前会话，不影响同一用户在其他设备上的活跃 Refresh Token。

P0 阶段，Refresh Token 缺失、格式错误、签名无效、已过期、已撤销或找不到对应会话时，统一返回 `101004`。前端收到后清理本地登录态并引导重新登录；后端不因普通刷新失败自动撤销该用户全部活跃 Refresh Token。

登录、刷新登录态、修改密码、禁用用户和修改角色通过同一条用户记录的行锁串行执行。登录取得锁后使用最新密码和账号状态继续校验；刷新请求取得锁后会重新校验旧 Refresh Token，避免这些请求在等待锁期间账号或会话已经发生变化，却仍然签发新会话。

P1 阶段可结合 Redis 短 TTL 宽限期、`token_jti` 状态和 `tokenVersion` 做更精细的幂等重试与重放检测；只有在明确识别为高风险重放或管理员强制下线等安全事件时，才撤销该用户全部活跃 Refresh Token。

### 请求参数

#### Cookie 参数

| 字段名称 | 字段类型 | 必填 | 字段解释 | 业务例子 |
| --- | --- | --- | --- | --- |
| `refresh_token` | `String` | 是 | 浏览器自动携带的 Refresh Token Cookie，前端 JavaScript 不读取该值 | `eyJhbGciOiJIUzI1NiJ9.refresh.token` |

### 请求样例

```http
POST /api/v1/auth/refresh
Cookie: refresh_token=eyJhbGciOiJIUzI1NiJ9.refresh.token
```

### 响应参数

#### data 字段说明

| 字段名称 | 字段类型 | 字段解释 | 业务例子 |
| --- | --- | --- | --- |
| `accessToken` | `String` | 新的 Access Token | `eyJhbGciOiJIUzI1NiJ9.new.access` |
| `accessTokenExpiresAt` | `String` | 新 Access Token 过期时间 | `2026-04-22T23:05:00+08:00` |
| `refreshTokenExpiresAt` | `String` | 新 Refresh Token Cookie 过期时间 | `2026-04-29T22:50:00+08:00` |
| `tokenType` | `String` | Token 类型 | `Bearer` |

### 响应样例

```json
{
  "code": 0,
  "message": "成功",
  "data": {
    "accessToken": "eyJhbGciOiJIUzI1NiJ9.new.access",
    "accessTokenExpiresAt": "2026-04-22T23:05:00+08:00",
    "refreshTokenExpiresAt": "2026-04-29T22:50:00+08:00",
    "tokenType": "Bearer"
  }
}
```

刷新成功时，后端同时通过响应头轮转 Refresh Token Cookie：

```http
Set-Cookie: refresh_token=<new_refresh_token>; Max-Age=604800; Path=/api/v1/auth; HttpOnly; SameSite=Lax
```

## 4.4 用户退出登录

- 路由：`POST`
- 路径：`/api/v1/auth/logout`
- 权限：`PUBLIC`

说明：

- 该接口用于退出当前登录会话，不要求携带有效 Access Token
- 浏览器自动携带 `refresh_token` Cookie，后端据此定位并撤销当前会话对应的 Refresh Token
- 该接口按幂等语义处理：如果 Refresh Token 缺失、已过期、已撤销或找不到对应会话，后端也返回成功，前端只需清理本地登录态即可
- 退出成功后后端会通过 `Set-Cookie` 清除 `refresh_token` Cookie

### 请求参数

#### Cookie 参数

| 字段名称 | 字段类型 | 必填 | 字段解释 | 业务例子 |
| --- | --- | --- | --- | --- |
| `refresh_token` | `String` | 否 | 当前会话对应的 Refresh Token Cookie；缺失时仍按幂等退出处理 | `eyJhbGciOiJIUzI1NiJ9.refresh.token` |

### 请求样例

```http
POST /api/v1/auth/logout
Cookie: refresh_token=eyJhbGciOiJIUzI1NiJ9.refresh.token
```

### 响应参数

无业务数据返回，`data` 固定为 `null`。

退出成功时，后端同时通过响应头清除 Refresh Token Cookie：

```http
Set-Cookie: refresh_token=; Max-Age=0; Path=/api/v1/auth; HttpOnly; SameSite=Lax
```

### 响应样例

```json
{
  "code": 0,
  "message": "成功",
  "data": null
}
```

## 5. 前台公共接口

## 5.1 获取已发布文章分页列表

- 路由：`GET`
- 路径：`/api/v1/articles`
- 权限：`PUBLIC`

### 请求参数

#### Query 参数

| 字段名称 | 字段类型 | 必填 | 字段解释 | 业务例子 |
| --- | --- | --- | --- | --- |
| `pageNum` | `Integer` | 否 | 页码，默认 `1` | `1` |
| `pageSize` | `Integer` | 否 | 每页条数，默认 `10`，最大 `20` | `10` |
| `keyword` | `String` | 否 | 搜索关键词，最长 100 个字符；同时匹配标题、摘要和纯文本正文 | `Spring Boot` |
| `categoryId` | `Long` | 否 | 分类 ID，用于分类筛选。支持一级分类或二级分类；传一级分类时返回其下所有二级分类文章 | `20001`、`21001` |
| `tagIds` | `Array<Long>` | 否 | 标签 ID 列表，用于标签筛选；传多个时表示文章必须同时包含这些标签 | `[30001, 30002]` |
| `isTop` | `Boolean` | 否 | 是否置顶；`true` 只返回置顶文章，`false` 只返回非置顶文章，不传则不限制 | `true` |
| `sort` | `String` | 否 | 排序模式；`DEFAULT` 为相关度（有关键词时）或置顶优先，`LATEST` 为仅按首次发布时间倒序，默认 `DEFAULT` | `LATEST` |

### 搜索规则

- `keyword` 去除首尾空白后为空时不添加搜索条件。
- 非空关键词通过 PostgreSQL `zhparser` 解析，并使用 `plainto_tsquery` 查询 `title`、`summary` 和 `content_text` 生成的全文检索向量；多个解析后的检索词之间为 AND 关系。若单字符未产生有效词元或关键词为纯数字，则对三个字段执行字面量包含匹配兜底；因此纯数字关键词支持按子串命中更长数字文本。
- 搜索条件可以和 `categoryId`、`tagIds`、`isTop`、`sort` 组合使用，分页结构和排序规则保持不变。
- 仅返回 `PUBLISHED` 文章，草稿和已下线文章即使匹配关键词也不会出现在结果中。
- 搜索时使用 `ts_headline` 返回可选的标题高亮和摘要/正文高亮片段；不新增独立搜索接口。
- `highlightedTitle` 和 `searchSnippet` 中只有后端生成的 `<mark class="article-search-highlight">` 标签，原始文章文本在返回前会进行 HTML 转义。

### 排序规则

- `sort=DEFAULT` 或不传 `sort`：有关键词时按全文检索相关度 DESC，再按 `isTop DESC, publishedAt DESC, id DESC` 排序；无关键词时按 `isTop DESC, publishedAt DESC, id DESC` 排序。
- `sort=LATEST`：不考虑置顶和全文检索相关度，按 `publishedAt DESC, id DESC` 排序。
- `isTop` 只负责筛选，可以和任一排序模式组合使用。
- 首页置顶文章使用 `isTop=true`，首页最新文章使用 `sort=LATEST` 且不传 `isTop`，因此同一篇文章允许同时出现在两个区域。

### 请求样例

```http
GET /api/v1/articles?pageNum=1&pageSize=10&keyword=Spring%20Boot&categoryId=20001&tagIds=30001&tagIds=30002
```

首页置顶文章与最新文章请求样例：

```http
GET /api/v1/articles?pageNum=1&pageSize=4&isTop=true
GET /api/v1/articles?pageNum=1&pageSize=4&sort=LATEST
```

### 响应参数

#### data 字段说明

`data` 为分页结构，`records` 中单条记录字段如下：

说明：

- `tags` 仅返回当前启用且仍存在的关联标签。
- 若文章存在历史关联的禁用标签或已被删除的标签，前台列表响应中不返回该标签。
- 通过 Query 参数 `tagIds` 进行筛选时，传入的标签仍必须存在且启用；若标签不存在或已禁用，后端返回对应业务错误。
- 通过 Query 参数 `categoryId` 进行筛选时，传入分类必须存在且启用；若分类不存在或已禁用，后端返回对应业务错误。
- 前台文章列表仅展示所属二级分类及其父分类均存在且启用的文章。

| 字段名称 | 字段类型 | 字段解释 | 业务例子 |
| --- | --- | --- | --- |
| `id` | `Long` | 文章 ID | `40001` |
| `title` | `String` | 文章标题 | `Spring Boot 双 Token 登录实践` |
| `summary` | `String` | 文章摘要 | `本文记录双 Token 的实现思路与接口设计` |
| `highlightedTitle` | `String` | 可选。搜索且标题命中时返回的安全高亮 HTML；其他场景不返回该字段 | `<mark class="article-search-highlight">Spring Boot</mark> 双 Token 登录实践` |
| `searchSnippet` | `String` | 可选。摘要命中时返回高亮摘要；仅正文命中时返回命中位置附近的高亮正文片段；仅标题命中或未搜索时不返回 | `本文记录 <mark class="article-search-highlight">双 Token</mark> 的实现思路` |
| `coverUrl` | `String` | 封面地址 | `https://cdn.example.com/cover/token.png` |
| `isTop` | `Boolean` | 是否置顶 | `true` |
| `publishedAt` | `String` | 发布时间 | `2026-04-22T23:00:00+08:00` |
| `viewCount` | `Integer` | 浏览量预留值；P2 启用真实浏览统计 | `128` |
| `category.id` | `Long` | 文章绑定的二级分类 ID | `21001` |
| `category.name` | `String` | 二级分类名称 | `Java` |
| `category.level` | `Integer` | 分类层级，固定为 `2` | `2` |
| `category.parent.id` | `Long` | 所属一级分类 ID | `20001` |
| `category.parent.name` | `String` | 所属一级分类名称 | `技术` |
| `tags` | `Array<Object>` | 标签列表 | `[{"id":30001,"name":"JWT"}]` |

### 响应样例

```json
{
  "code": 0,
  "message": "成功",
  "data": {
    "records": [
      {
        "id": 40001,
        "title": "Spring Boot 双 Token 登录实践",
        "summary": "本文记录双 Token 的实现思路与接口设计",
        "highlightedTitle": "<mark class=\"article-search-highlight\">Spring Boot</mark> 双 Token 登录实践",
        "searchSnippet": "本文记录 <mark class=\"article-search-highlight\">双 Token</mark> 的实现思路与接口设计",
        "coverUrl": "https://cdn.example.com/cover/token.png",
        "isTop": true,
        "publishedAt": "2026-04-22T23:00:00+08:00",
        "viewCount": 128,
        "category": {
          "id": 21001,
          "name": "Java",
          "level": 2,
          "parent": {
            "id": 20001,
            "name": "技术"
          }
        },
        "tags": [
          {
            "id": 30001,
            "name": "JWT"
          }
        ]
      }
    ],
    "pageNum": 1,
    "pageSize": 10,
    "total": 1,
    "totalPages": 1,
    "hasNext": false
  }
}
```

## 5.2 获取文章详情

- 路由：`GET`
- 路径：`/api/v1/articles/{articleId}`
- 权限：`PUBLIC`

说明：P0 阶段文章详情接口和前台文章详情页均以 `articleId` 作为稳定定位标识。`slug` 仅作为 P2 URL 可读化与 SEO 优化预留，后续可扩展为类似 `/articles/{articleId}-{slug}` 的前台展示 URL。

文章详情返回的 `tags` 仅包含当前启用且仍存在的关联标签；历史关联的禁用标签或已被删除的标签不返回，不影响文章详情本身展示。

文章详情要求文章所属二级分类及其父分类均存在且启用；若分类不存在，返回 `ARTICLE_CATEGORY_NOT_FOUND`；若分类已禁用，返回 `ARTICLE_CATEGORY_DISABLED`，前台不展示该文章详情。

文章详情要求文章关联作者存在；若作者不存在，返回 `ARTICLE_AUTHOR_NOT_FOUND`，前台不展示该文章详情。

### 请求参数

#### Path 参数

| 字段名称 | 字段类型 | 必填 | 字段解释 | 业务例子 |
| --- | --- | --- | --- | --- |
| `articleId` | `Long` | 是 | 文章 ID | `40001` |

### 请求样例

```http
GET /api/v1/articles/40001
```

### 响应参数

#### data 字段说明

| 字段名称 | 字段类型 | 字段解释 | 业务例子 |
| --- | --- | --- | --- |
| `id` | `Long` | 文章 ID | `40001` |
| `title` | `String` | 文章标题 | `Spring Boot 双 Token 登录实践` |
| `summary` | `String` | 文章摘要 | `本文记录双 Token 的实现思路与接口设计` |
| `contentHtml` | `String` | HTML 正文，由 Markdown 转译得到，供前端渲染 | `<h1>一、背景</h1><p>...</p>` |
| `coverUrl` | `String` | 封面地址 | `https://cdn.example.com/cover/token.png` |
| `isTop` | `Boolean` | 是否置顶 | `true` |
| `allowComment` | `Boolean` | 是否允许新增评论和回复；关闭后已有 APPROVED 评论仍可展示 | `true` |
| `viewCount` | `Integer` | 浏览量预留值；P2 启用真实浏览统计 | `128` |
| `commentCount` | `Integer` | 评论数 | `0` |
| `likeCount` | `Integer` | 点赞数 | `0` |
| `publishedAt` | `String` | 发布时间 | `2026-04-22T23:00:00+08:00` |
| `updatedAt` | `String` | 更新时间 | `2026-04-22T23:10:00+08:00` |
| `category.id` | `Long` | 文章绑定的二级分类 ID | `21001` |
| `category.name` | `String` | 二级分类名称 | `Java` |
| `category.level` | `Integer` | 分类层级，固定为 `2` | `2` |
| `category.parent.id` | `Long` | 所属一级分类 ID | `20001` |
| `category.parent.name` | `String` | 所属一级分类名称 | `技术` |
| `tags` | `Array<Object>` | 标签列表 | `[{"id":30001,"name":"JWT"}]` |
| `author.id` | `Long` | 作者用户 ID | `10001` |
| `author.username` | `String` | 作者用户名 | `ccsanjuu` |
| `author.nickname` | `String` | 作者昵称 | `sanjuu` |
| `author.avatarUrl` | `String` | 作者头像地址 | `https://cdn.example.com/avatar/1.png` |
| `author.bio` | `String` | 作者个人简介 | `专注后端和前端工程化` |

### 响应样例

```json
{
  "code": 0,
  "message": "成功",
  "data": {
    "id": 40001,
    "title": "Spring Boot 双 Token 登录实践",
    "summary": "本文记录双 Token 的实现思路与接口设计",
    "contentHtml": "<h1>一、背景</h1><p>这里是文章正文</p>",
    "coverUrl": "https://cdn.example.com/cover/token.png",
    "isTop": true,
    "allowComment": true,
    "viewCount": 128,
    "commentCount": 0,
    "likeCount": 0,
    "publishedAt": "2026-04-22T23:00:00+08:00",
    "updatedAt": "2026-04-22T23:10:00+08:00",
    "category": {
      "id": 21001,
      "name": "Java",
      "level": 2,
      "parent": {
        "id": 20001,
        "name": "技术"
      }
    },
    "tags": [
      {
        "id": 30001,
        "name": "JWT"
      }
    ],
    "author": {
      "id": 10001,
      "username": "ccsanjuu",
      "nickname": "sanjuu",
      "avatarUrl": "https://cdn.example.com/avatar/1.png",
      "bio": "专注后端和前端工程化"
    }
  }
}
```

## 5.3 获取启用分类列表

- 路由：`GET`
- 路径：`/api/v1/categories`
- 权限：`PUBLIC`

说明：

- 仅返回启用状态的分类。
- 一级分类按 `sortNo ASC, id ASC` 排序。
- 每个一级分类下的二级分类也按 `sortNo ASC, id ASC` 排序。

### 请求参数

无

### 请求样例

```http
GET /api/v1/categories
```

### 响应参数

#### data 字段说明

`data` 返回一级分类树，一级分类下包含二级分类列表；一级分类的 `parentId` 为空，二级分类的 `children` 为空数组。

| 字段名称 | 字段类型 | 字段解释 | 业务例子 |
| --- | --- | --- | --- |
| `id` | `Long` | 分类 ID | `20001` |
| `parentId` | `Long` | 父分类 ID，一级分类为空，二级分类为所属一级分类 ID | `NULL`、`20001` |
| `name` | `String` | 分类名称 | `技术` |
| `level` | `Integer` | 分类层级，一级为 `1`，二级为 `2` | `1`、`2` |
| `description` | `String` | 分类描述 | `技术内容一级分类` |
| `sortNo` | `Integer` | 排序值，值越小越靠前 | `10` |
| `articleCount` | `Long` | 前台可见已发布文章数量。一级分类为其下二级分类汇总，二级分类为自身文章数 | `18` |
| `children` | `Array<Object>` | 子分类列表。一级分类下返回二级分类，二级分类返回空数组 | `[{"id":21001,"name":"Java","children":[]}]` |

### 响应样例

```json
{
  "code": 0,
  "message": "成功",
  "data": [
    {
      "id": 20001,
      "parentId": null,
      "name": "技术",
      "level": 1,
      "description": "技术内容一级分类",
      "sortNo": 10,
      "articleCount": 18,
      "children": [
        {
          "id": 21001,
          "parentId": 20001,
          "name": "Java",
          "level": 2,
          "description": "Java 相关文章",
          "sortNo": 11,
          "articleCount": 6,
          "children": []
        },
        {
          "id": 21002,
          "parentId": 20001,
          "name": "算法",
          "level": 2,
          "description": "算法相关内容",
          "sortNo": 12,
          "articleCount": 4,
          "children": []
        }
      ]
    }
  ]
}
```

## 5.4 获取启用标签列表

- 路由：`GET`
- 路径：`/api/v1/tags`
- 权限：`PUBLIC`

说明：

- 该接口用于前台文章列表筛选面板、标签云等公开展示场景。
- 仅返回启用状态的标签。
- 按 `articleCount DESC, name ASC, id ASC` 排序，其中 `articleCount` 为该标签下前台可见已发布文章数量。
- 前台文章列表通过 `tagIds` 进行标签筛选；后端仍需校验传入标签是否存在且启用，避免用户手动构造 URL 访问禁用标签。

### 请求参数

无

### 请求样例

```http
GET /api/v1/tags
```

### 响应参数

#### data 字段说明

`data` 返回启用标签列表。

| 字段名称 | 字段类型 | 字段解释 | 业务例子 |
| --- | --- | --- | --- |
| `id` | `Long` | 标签 ID | `30001` |
| `name` | `String` | 标签名称 | `JWT` |
| `articleCount` | `Long` | 该标签下前台可见已发布文章数量 | `8` |

### 响应样例

```json
{
  "code": 0,
  "message": "成功",
  "data": [
    {
      "id": 30001,
      "name": "JWT",
      "articleCount": 8
    },
    {
      "id": 30002,
      "name": "Spring Boot",
      "articleCount": 6
    }
  ]
}
```

## 5.5 获取用户公开资料卡

- 路由：`GET`
- 路径：`/api/v1/users/{userId}/public-profile`
- 权限：`PUBLIC`

说明：公开用户资料卡用于文章作者悬浮卡、作者基础信息展示等前台场景。公开场景使用 `userId` 作为路径标识，`username` 作为展示字段。

### 请求参数

#### Path 参数

| 字段名称 | 字段类型 | 必填 | 字段解释 | 业务例子 |
| --- | --- | --- | --- | --- |
| `userId` | `Long` | 是 | 用户 ID | `10001` |

### 请求样例

```http
GET /api/v1/users/10001/public-profile
```

### 响应参数

#### data 字段说明

| 字段名称 | 字段类型 | 字段解释 | 业务例子 |
| --- | --- | --- | --- |
| `id` | `Long` | 用户 ID | `10001` |
| `username` | `String` | 用户名 | `ccsanjuu` |
| `nickname` | `String` | 昵称 | `sanjuu` |
| `avatarUrl` | `String` | 头像地址 | `https://cdn.example.com/avatar/1.png` |
| `bio` | `String` | 个人简介 | `专注后端和前端工程化` |

### 响应样例

```json
{
  "code": 0,
  "message": "成功",
  "data": {
    "id": 10001,
    "username": "ccsanjuu",
    "nickname": "sanjuu",
    "avatarUrl": "https://cdn.example.com/avatar/1.png",
    "bio": "专注后端和前端工程化"
  }
}
```

## 6. 个人中心接口

## 6.1 获取当前登录用户信息

- 路由：`GET`
- 路径：`/api/v1/users/me`
- 权限：`LOGIN`

### 请求参数

#### Header 参数

| 字段名称 | 字段类型 | 必填 | 字段解释 | 业务例子 |
| --- | --- | --- | --- | --- |
| `Authorization` | `String` | 是 | Access Token | `Bearer eyJhbGciOiJIUzI1NiJ9.access` |

### 请求样例

```http
GET /api/v1/users/me
Authorization: Bearer eyJhbGciOiJIUzI1NiJ9.access
```

### 响应参数

#### data 字段说明

| 字段名称 | 字段类型 | 字段解释 | 业务例子 |
| --- | --- | --- | --- |
| `id` | `Long` | 用户 ID | `10002` |
| `username` | `String` | 用户名 | `alice_dev` |
| `nickname` | `String` | 昵称 | `Alice` |
| `email` | `String` | 邮箱 | `alice@example.com` |
| `role` | `String` | 用户角色 | `USER` |
| `status` | `String` | 用户状态 | `ACTIVE` |
| `avatarUrl` | `String` | 头像地址 | `` |
| `bio` | `String` | 个人简介 | `热爱前后端开发` |
| `lastLoginAt` | `String/null` | 最近登录时间；从未登录过时为空 | `2026-04-22T22:20:10+08:00` |
| `createdAt` | `String` | 注册时间 | `2026-04-22T22:20:00+08:00` |

### 响应样例

```json
{
  "code": 0,
  "message": "成功",
  "data": {
    "id": 10002,
    "username": "alice_dev",
    "nickname": "Alice",
    "email": "alice@example.com",
    "role": "USER",
    "status": "ACTIVE",
    "avatarUrl": "",
    "bio": "热爱前后端开发",
    "lastLoginAt": "2026-04-22T22:20:10+08:00",
    "createdAt": "2026-04-22T22:20:00+08:00"
  }
}
```

## 6.2 更新个人资料

- 路由：`PUT`
- 路径：`/api/v1/users/me/profile`
- 权限：`LOGIN`

### 请求参数

#### Header 参数

| 字段名称 | 字段类型 | 必填 | 字段解释 | 业务例子 |
| --- | --- | --- | --- | --- |
| `Authorization` | `String` | 是 | Access Token | `Bearer eyJhbGciOiJIUzI1NiJ9.access` |

#### Body 参数

| 字段名称 | 字段类型 | 必填 | 字段解释 | 业务例子 |
| --- | --- | --- | --- | --- |
| `nickname` | `String` | 否 | 用户昵称，1-20 位，不能全为空白字符 | `Alice` |
| `bio` | `String` | 否 | 个人简介，最长 100 个字符，换行和空行均计入字符数 | `专注 Java 与前端工程化` |

### 请求样例

```http
PUT /api/v1/users/me/profile
Authorization: Bearer eyJhbGciOiJIUzI1NiJ9.access
Content-Type: application/json

{
  "nickname": "Alice",
  "bio": "专注 Java 与前端工程化"
}
```

### 响应参数

#### data 字段说明

| 字段名称 | 字段类型 | 字段解释 | 业务例子 |
| --- | --- | --- | --- |
| `id` | `Long` | 用户 ID | `10002` |
| `username` | `String` | 用户名 | `alice_dev` |
| `nickname` | `String` | 更新后的昵称 | `Alice` |
| `email` | `String` | 用户邮箱 | `alice@example.com` |
| `avatarUrl` | `String` | 用户头像地址 | `` |
| `bio` | `String` | 更新后的简介 | `专注 Java 与前端工程化` |
| `updatedAt` | `String` | 更新时间 | `2026-04-22T23:20:00+08:00` |

### 响应样例

```json
{
  "code": 0,
  "message": "成功",
  "data": {
    "id": 10002,
    "username": "alice_dev",
    "nickname": "Alice",
    "email": "alice@example.com",
    "avatarUrl": "",
    "bio": "专注 Java 与前端工程化",
    "updatedAt": "2026-04-22T23:20:00+08:00"
  }
}
```

## 6.3 修改密码

- 路由：`PUT`
- 路径：`/api/v1/users/me/password`
- 权限：`LOGIN`

### 请求参数

#### Header 参数

| 字段名称 | 字段类型 | 必填 | 字段解释 | 业务例子 |
| --- | --- | --- | --- | --- |
| `Authorization` | `String` | 是 | Access Token | `Bearer eyJhbGciOiJIUzI1NiJ9.access` |

#### Body 参数

| 字段名称 | 字段类型 | 必填 | 字段解释 | 业务例子 |
| --- | --- | --- | --- | --- |
| `oldPassword` | `String` | 是 | 原密码，6-32 位；允许英文字母、数字、下划线、短横线和常用 ASCII 特殊字符，不允许中文或空白字符 | `Passw0rd!` |
| `newPassword` | `String` | 是 | 新密码，6-32 位；允许英文字母、数字、下划线、短横线和常用 ASCII 特殊字符，不允许中文或空白字符 | `NewPassw0rd!` |

### 请求样例

```http
PUT /api/v1/users/me/password
Authorization: Bearer eyJhbGciOiJIUzI1NiJ9.access
Content-Type: application/json

{
  "oldPassword": "Passw0rd!",
  "newPassword": "NewPassw0rd!"
}
```

### 响应参数

无业务数据返回，`data` 固定为 `null`。

### 响应样例

```json
{
  "code": 0,
  "message": "成功",
  "data": null
}
```

## 7. 后台用户管理接口

## 7.1 获取用户分页列表

- 路由：`GET`
- 路径：`/api/v1/admin/users`
- 权限：`ADMIN`

### 请求参数

#### Header 参数

| 字段名称 | 字段类型 | 必填 | 字段解释 | 业务例子 |
| --- | --- | --- | --- | --- |
| `Authorization` | `String` | 是 | 管理员 Access Token | `Bearer eyJhbGciOiJIUzI1NiJ9.admin` |

#### Query 参数

| 字段名称 | 字段类型 | 必填 | 字段解释 | 业务例子 |
| --- | --- | --- | --- | --- |
| `pageNum` | `Integer` | 否 | 页码，默认 `1` | `1` |
| `pageSize` | `Integer` | 否 | 每页条数，默认 `10` | `10` |
| `username` | `String` | 否 | 用户名模糊搜索，最长 20 个字符，不区分大小写，`%`、`_` 等字符按字面量匹配 | `alice` |
| `email` | `String` | 否 | 邮箱模糊搜索，最长 255 个字符，不区分大小写，`%`、`_` 等字符按字面量匹配 | `example.com` |
| `role` | `String` | 否 | 角色筛选 | `USER` |
| `status` | `String` | 否 | 状态筛选 | `ACTIVE` |

`username`、`email`、`role` 和 `status` 可以组合使用，同时传入多个条件时按 AND 关系筛选。用户名和邮箱均按不区分大小写的字面量包含关系进行模糊搜索。

排序规则：默认按 `createdAt` 倒序、`id` 倒序返回，不提供自定义排序参数。

### 请求样例

```http
GET /api/v1/admin/users?pageNum=1&pageSize=10&username=alice&email=example.com&status=ACTIVE
Authorization: Bearer eyJhbGciOiJIUzI1NiJ9.admin
```

### 响应参数

#### data 字段说明

`data` 为分页结构，`records` 中单条记录字段如下：

| 字段名称 | 字段类型 | 字段解释 | 业务例子 |
| --- | --- | --- | --- |
| `id` | `Long` | 用户 ID | `10002` |
| `username` | `String` | 用户名 | `alice_dev` |
| `nickname` | `String` | 昵称 | `Alice` |
| `email` | `String` | 邮箱 | `alice@example.com` |
| `role` | `String` | 角色 | `USER` |
| `status` | `String` | 状态 | `ACTIVE` |
| `lastLoginAt` | `String/null` | 最近登录时间；从未登录过时为空 | `2026-04-22T22:20:10+08:00` |
| `createdAt` | `String` | 注册时间 | `2026-04-22T22:20:00+08:00` |

### 响应样例

```json
{
  "code": 0,
  "message": "成功",
  "data": {
    "records": [
      {
        "id": 10002,
        "username": "alice_dev",
        "nickname": "Alice",
        "email": "alice@example.com",
        "role": "USER",
        "status": "ACTIVE",
        "lastLoginAt": "2026-04-22T22:20:10+08:00",
        "createdAt": "2026-04-22T22:20:00+08:00"
      }
    ],
    "pageNum": 1,
    "pageSize": 10,
    "total": 1,
    "totalPages": 1,
    "hasNext": false
  }
}
```

## 7.2 修改用户状态

- 路由：`PATCH`
- 路径：`/api/v1/admin/users/{userId}/status`
- 权限：`ADMIN`

### 请求参数

#### Path 参数

| 字段名称 | 字段类型 | 必填 | 字段解释 | 业务例子 |
| --- | --- | --- | --- | --- |
| `userId` | `Long` | 是 | 用户 ID | `10002` |

#### Body 参数

| 字段名称 | 字段类型 | 必填 | 字段解释 | 业务例子 |
| --- | --- | --- | --- | --- |
| `status` | `String` | 是 | 目标状态，仅支持 `ACTIVE`、`DISABLED` | `DISABLED` |

### 请求样例

```http
PATCH /api/v1/admin/users/10002/status
Authorization: Bearer eyJhbGciOiJIUzI1NiJ9.admin
Content-Type: application/json

{
  "status": "DISABLED"
}
```

说明：管理员不能修改当前登录用户自身状态。状态修改为 `DISABLED` 时，后端会撤销该用户全部活跃 Refresh Token，并递增 tokenVersion 使旧 Access Token 立即失效；修改为 `ACTIVE` 时不撤销 Refresh Token，也不再次递增 tokenVersion，因为禁用时签发的旧 Token 已经失效。

### 响应参数

#### data 字段说明

| 字段名称 | 字段类型 | 字段解释 | 业务例子 |
| --- | --- | --- | --- |
| `id` | `Long` | 用户 ID | `10002` |
| `status` | `String` | 更新后的状态 | `DISABLED` |

### 响应样例

```json
{
  "code": 0,
  "message": "成功",
  "data": {
    "id": 10002,
    "status": "DISABLED"
  }
}
```

## 7.3 修改用户角色

- 路由：`PATCH`
- 路径：`/api/v1/admin/users/{userId}/role`
- 权限：`ADMIN`

### 请求参数

#### Path 参数

| 字段名称 | 字段类型 | 必填 | 字段解释 | 业务例子 |
| --- | --- | --- | --- | --- |
| `userId` | `Long` | 是 | 用户 ID | `10002` |

#### Body 参数

| 字段名称 | 字段类型 | 必填 | 字段解释 | 业务例子 |
| --- | --- | --- | --- | --- |
| `role` | `String` | 是 | 目标角色，仅支持 `ADMIN`、`USER` | `ADMIN` |

### 请求样例

```http
PATCH /api/v1/admin/users/10002/role
Authorization: Bearer eyJhbGciOiJIUzI1NiJ9.admin
Content-Type: application/json

{
  "role": "ADMIN"
}
```

说明：管理员不能修改当前登录用户自身角色。角色发生变化后不直接撤销 Refresh Token，但会递增 tokenVersion 使旧 Access Token 立即失效；新的角色在使用现有 Refresh Token 刷新登录态或重新登录后生效。

### 响应参数

#### data 字段说明

| 字段名称 | 字段类型 | 字段解释 | 业务例子 |
| --- | --- | --- | --- |
| `id` | `Long` | 用户 ID | `10002` |
| `role` | `String` | 更新后的角色 | `ADMIN` |

### 响应样例

```json
{
  "code": 0,
  "message": "成功",
  "data": {
    "id": 10002,
    "role": "ADMIN"
  }
}
```

## 8. 后台文章管理接口

## 8.1 获取后台文章分页列表

- 路由：`GET`
- 路径：`/api/v1/admin/articles`
- 权限：`ADMIN`

### 请求参数

#### Query 参数

| 字段名称 | 字段类型 | 必填 | 字段解释 | 业务例子 |
| --- | --- | --- | --- | --- |
| `pageNum` | `Integer` | 否 | 页码，默认 `1` | `1` |
| `pageSize` | `Integer` | 否 | 每页条数，默认 `10` | `10` |
| `title` | `String` | 否 | 按标题模糊搜索 | `Token` |
| `categoryId` | `Long` | 否 | 分类 ID。支持一级分类或二级分类筛选；文章实际绑定的仍是二级分类 | `20001`、`21001` |
| `status` | `String` | 否 | 文章状态筛选 | `DRAFT` |
| `isTop` | `Boolean` | 否 | 是否置顶筛选 | `true` |

### 请求样例

```http
GET /api/v1/admin/articles?pageNum=1&pageSize=10&status=DRAFT
Authorization: Bearer eyJhbGciOiJIUzI1NiJ9.admin
```

### 响应参数

#### data 字段说明

`data` 为分页结构，`records` 中单条记录字段如下：

| 字段名称 | 字段类型 | 字段解释 | 业务例子 |
| --- | --- | --- | --- |
| `id` | `Long` | 文章 ID | `40001` |
| `title` | `String` | 文章标题 | `Spring Boot 双 Token 登录实践` |
| `summary` | `String` | 摘要 | `本文记录双 Token 的实现思路与接口设计` |
| `status` | `String` | 文章状态 | `DRAFT` |
| `isTop` | `Boolean` | 是否置顶 | `true` |
| `coverUrl` | `String` | 封面地址 | `https://cdn.example.com/cover/token.png` |
| `publishedAt` | `String` | 发布时间 | `2026-04-22T23:00:00+08:00` |
| `updatedAt` | `String` | 更新时间 | `2026-04-22T23:10:00+08:00` |
| `category.id` | `Long` | 文章绑定的二级分类 ID | `21001` |
| `category.name` | `String` | 二级分类名称 | `Java` |
| `category.level` | `Integer` | 分类层级，固定为 `2` | `2` |
| `category.parent.id` | `Long` | 所属一级分类 ID | `20001` |
| `category.parent.name` | `String` | 所属一级分类名称 | `技术` |

### 响应样例

```json
{
  "code": 0,
  "message": "成功",
  "data": {
    "records": [
      {
        "id": 40001,
        "title": "Spring Boot 双 Token 登录实践",
        "summary": "本文记录双 Token 的实现思路与接口设计",
        "status": "DRAFT",
        "isTop": true,
        "coverUrl": "https://cdn.example.com/cover/token.png",
        "publishedAt": null,
        "updatedAt": "2026-04-22T23:10:00+08:00",
        "category": {
          "id": 21001,
          "name": "Java",
          "level": 2,
          "parent": {
            "id": 20001,
            "name": "技术"
          }
        }
      }
    ],
    "pageNum": 1,
    "pageSize": 10,
    "total": 1,
    "totalPages": 1,
    "hasNext": false
  }
}
```

## 8.2 获取后台文章详情

- 路由：`GET`
- 路径：`/api/v1/admin/articles/{articleId}`
- 权限：`ADMIN`

### 请求参数

#### Path 参数

| 字段名称 | 字段类型 | 必填 | 字段解释 | 业务例子 |
| --- | --- | --- | --- | --- |
| `articleId` | `Long` | 是 | 文章 ID | `40001` |

### 请求样例

```http
GET /api/v1/admin/articles/40001
Authorization: Bearer eyJhbGciOiJIUzI1NiJ9.admin
```

### 响应参数

#### data 字段说明

| 字段名称 | 字段类型 | 字段解释 | 业务例子 |
| --- | --- | --- | --- |
| `id` | `Long` | 文章 ID | `40001` |
| `title` | `String` | 文章标题 | `Spring Boot 双 Token 登录实践` |
| `summary` | `String` | 摘要 | `本文记录双 Token 的实现思路与接口设计` |
| `contentMd` | `String` | Markdown 正文，供后台编辑器回显 | `# 一、背景\n...` |
| `contentHtml` | `String` | HTML 正文，由 Markdown 转译得到 | `<h1>一、背景</h1><p>...</p>` |
| `contentText` | `String` | 纯文本正文，由 Markdown 提取得到，供全文搜索使用 | `一、背景 ...` |
| `coverUrl` | `String` | 封面地址 | `https://cdn.example.com/cover/token.png` |
| `status` | `String` | 文章状态 | `DRAFT` |
| `isTop` | `Boolean` | 是否置顶 | `true` |
| `categoryId` | `Long` | 二级分类 ID，文章只能绑定二级分类 | `21001` |
| `tagIds` | `Array<Long>` | 标签 ID 列表，便于回显编辑器 | `[30001, 30002]` |
| `allowComment` | `Boolean` | 是否允许评论 | `true` |
| `publishedAt` | `String` | 发布时间 | `null` |
| `createdAt` | `String` | 创建时间 | `2026-04-22T22:50:00+08:00` |
| `updatedAt` | `String` | 更新时间 | `2026-04-22T23:10:00+08:00` |

### 响应样例

```json
{
  "code": 0,
  "message": "成功",
  "data": {
    "id": 40001,
    "title": "Spring Boot 双 Token 登录实践",
    "summary": "本文记录双 Token 的实现思路与接口设计",
    "contentMd": "# 一、背景\n\n这里是正文",
    "contentHtml": "<h1>一、背景</h1><p>这里是正文</p>",
    "contentText": "一、背景 这里是正文",
    "coverUrl": "https://cdn.example.com/cover/token.png",
    "status": "DRAFT",
    "isTop": true,
    "categoryId": 21001,
    "tagIds": [30001, 30002],
    "allowComment": true,
    "publishedAt": null,
    "createdAt": "2026-04-22T22:50:00+08:00",
    "updatedAt": "2026-04-22T23:10:00+08:00"
  }
}
```

## 8.3 创建文章

- 路由：`POST`
- 路径：`/api/v1/admin/articles`
- 权限：`ADMIN`

### 请求参数

#### Body 参数

| 字段名称 | 字段类型 | 必填 | 字段解释 | 业务例子 |
| --- | --- | --- | --- | --- |
| `title` | `String` | 是 | 文章标题 | `Spring Boot 双 Token 登录实践` |
| `summary` | `String` | 否 | 文章摘要 | `本文记录双 Token 的实现思路与接口设计` |
| `contentMd` | `String` | 是 | Markdown 正文。`contentHtml` 与 `contentText` 由后端自动生成，无需前端传入 | `# 一、背景\n...` |
| `categoryId` | `Long` | 是 | 二级分类 ID，文章只能绑定二级分类 | `21001` |
| `tagIds` | `Array<Long>` | 否 | 标签 ID 列表 | `[30001, 30002]` |
| `coverUrl` | `String` | 否 | 封面地址 | `https://cdn.example.com/cover/token.png` |
| `isTop` | `Boolean` | 否 | 是否置顶，默认 `false` | `true` |
| `status` | `String` | 是 | 创建时状态，仅允许 `DRAFT` 或 `PUBLISHED`，不允许直接创建为 `OFFLINE` | `DRAFT` |
| `allowComment` | `Boolean` | 否 | 是否允许新增评论和回复；关闭后已有 APPROVED 评论仍可展示 | `true` |

### 请求样例

```http
POST /api/v1/admin/articles
Authorization: Bearer eyJhbGciOiJIUzI1NiJ9.admin
Content-Type: application/json

{
  "title": "Spring Boot 双 Token 登录实践",
  "summary": "本文记录双 Token 的实现思路与接口设计",
  "contentMd": "# 一、背景\n\n这里是正文",
  "categoryId": 21001,
  "tagIds": [30001, 30002],
  "coverUrl": "https://cdn.example.com/cover/token.png",
  "isTop": true,
  "status": "DRAFT",
  "allowComment": true
}
```

### 响应参数

#### data 字段说明

| 字段名称 | 字段类型 | 字段解释 | 业务例子 |
| --- | --- | --- | --- |
| `id` | `Long` | 新建文章 ID | `40001` |
| `status` | `String` | 当前文章状态 | `DRAFT` |
| `publishedAt` | `String` | 首次发布时间，草稿可为空 | `null` |
| `createdAt` | `String` | 创建时间 | `2026-04-22T23:45:00+08:00` |

### 响应样例

```json
{
  "code": 0,
  "message": "成功",
  "data": {
    "id": 40001,
    "status": "DRAFT",
    "publishedAt": null,
    "createdAt": "2026-04-22T23:45:00+08:00"
  }
}
```

## 8.4 更新文章

- 路由：`PUT`
- 路径：`/api/v1/admin/articles/{articleId}`
- 权限：`ADMIN`

### 请求参数

#### Path 参数

| 字段名称 | 字段类型 | 必填 | 字段解释 | 业务例子 |
| --- | --- | --- | --- | --- |
| `articleId` | `Long` | 是 | 文章 ID | `40001` |

#### Body 参数

| 字段名称 | 字段类型 | 必填 | 字段解释 | 业务例子 |
| --- | --- | --- | --- | --- |
| `title` | `String` | 是 | 文章标题 | `Spring Boot 双 Token 登录实践` |
| `summary` | `String` | 否 | 文章摘要 | `本文记录双 Token 的实现思路与接口设计` |
| `contentMd` | `String` | 是 | Markdown 正文。`contentHtml` 与 `contentText` 由后端自动生成，无需前端传入 | `# 一、背景\n...` |
| `categoryId` | `Long` | 是 | 二级分类 ID，文章只能绑定二级分类 | `21001` |
| `tagIds` | `Array<Long>` | 否 | 标签 ID 列表 | `[30001, 30002]` |
| `coverUrl` | `String` | 否 | 封面地址 | `https://cdn.example.com/cover/token.png` |
| `isTop` | `Boolean` | 否 | 是否置顶 | `true` |
| `status` | `String` | 是 | 编辑后的状态；从未发布过的文章允许 `DRAFT` 或 `PUBLISHED`，已发布过的文章仅允许 `PUBLISHED` 或 `OFFLINE` | `PUBLISHED` |
| `allowComment` | `Boolean` | 否 | 是否允许评论 | `true` |

### 请求样例

```http
PUT /api/v1/admin/articles/40001
Authorization: Bearer eyJhbGciOiJIUzI1NiJ9.admin
Content-Type: application/json

{
  "title": "Spring Boot 双 Token 登录实践",
  "summary": "本文记录双 Token 的实现思路与接口设计",
  "contentMd": "# 一、背景\n\n这里是更新后的正文",
  "categoryId": 21001,
  "tagIds": [30001, 30002],
  "coverUrl": "https://cdn.example.com/cover/token.png",
  "isTop": true,
  "status": "PUBLISHED",
  "allowComment": true
}
```

### 响应参数

#### data 字段说明

| 字段名称 | 字段类型 | 字段解释 | 业务例子 |
| --- | --- | --- | --- |
| `id` | `Long` | 文章 ID | `40001` |
| `status` | `String` | 更新后的状态 | `PUBLISHED` |
| `publishedAt` | `String` | 首次发布时间，仅在文章第一次变为 `PUBLISHED` 时写入 | `2026-04-22T23:50:00+08:00` |
| `updatedAt` | `String` | 更新时间 | `2026-04-22T23:50:00+08:00` |

### 响应样例

```json
{
  "code": 0,
  "message": "成功",
  "data": {
    "id": 40001,
    "status": "PUBLISHED",
    "publishedAt": "2026-04-22T23:50:00+08:00",
    "updatedAt": "2026-04-22T23:50:00+08:00"
  }
}
```

## 8.5 修改文章状态

- 路由：`PATCH`
- 路径：`/api/v1/admin/articles/{articleId}/status`
- 权限：`ADMIN`

### 请求参数

#### Path 参数

| 字段名称 | 字段类型 | 必填 | 字段解释 | 业务例子 |
| --- | --- | --- | --- | --- |
| `articleId` | `Long` | 是 | 文章 ID | `40001` |

#### Body 参数

| 字段名称 | 字段类型 | 必填 | 字段解释 | 业务例子 |
| --- | --- | --- | --- | --- |
| `status` | `String` | 是 | 目标状态；从未发布过的文章仅允许发布为 `PUBLISHED`，已发布过的文章仅允许在 `PUBLISHED` 与 `OFFLINE` 间流转 | `OFFLINE` |

### 请求样例

```http
PATCH /api/v1/admin/articles/40001/status
Authorization: Bearer eyJhbGciOiJIUzI1NiJ9.admin
Content-Type: application/json

{
  "status": "OFFLINE"
}
```

### 响应参数

#### data 字段说明

| 字段名称 | 字段类型 | 字段解释 | 业务例子 |
| --- | --- | --- | --- |
| `id` | `Long` | 文章 ID | `40001` |
| `status` | `String` | 更新后的状态 | `OFFLINE` |
| `updatedAt` | `String` | 更新时间 | `2026-04-22T23:55:00+08:00` |

### 响应样例

```json
{
  "code": 0,
  "message": "成功",
  "data": {
    "id": 40001,
    "status": "OFFLINE",
    "updatedAt": "2026-04-22T23:55:00+08:00"
  }
}
```

## 8.6 删除文章

- 路由：`DELETE`
- 路径：`/api/v1/admin/articles/{articleId}`
- 权限：`ADMIN`

### 请求参数

#### Path 参数

| 字段名称 | 字段类型 | 必填 | 字段解释 | 业务例子 |
| --- | --- | --- | --- | --- |
| `articleId` | `Long` | 是 | 文章 ID | `40001` |

### 请求样例

```http
DELETE /api/v1/admin/articles/40001
Authorization: Bearer eyJhbGciOiJIUzI1NiJ9.admin
```

### 响应参数

无业务数据返回，`data` 固定为 `null`。

### 响应样例

```json
{
  "code": 0,
  "message": "成功",
  "data": null
}
```

## 9. 后台分类管理接口

## 9.1 获取分类列表

- 路由：`GET`
- 路径：`/api/v1/admin/categories`
- 权限：`ADMIN`

### 请求参数

#### Query 参数

| 字段名称 | 字段类型 | 必填 | 字段解释 | 业务例子 |
| --- | --- | --- | --- | --- |
| `keyword` | `String` | 否 | 分类名称模糊搜索 | `Java` |
| `status` | `String` | 否 | 分类状态筛选 | `ENABLED` |
| `level` | `Integer` | 否 | 分类层级筛选，`1` 为一级分类，`2` 为二级分类 | `2` |
| `parentId` | `Long` | 否 | 父分类 ID，查询某个一级分类下的二级分类时使用 | `20001` |

### 返回规则

- 不传 `keyword`、`status`、`level` 和 `parentId` 时，返回完整分类树，一级分类下包含二级分类。
- 传入 `keyword`、`status`、`level` 或 `parentId` 中任一条件时，返回符合条件的平铺列表，每个节点的 `children` 为空数组；这样状态筛选可以返回父级状态不同的二级分类。
- `parentId` 与 `level=1` 不能同时使用；若同时传入，后端返回参数错误。
- `parentId` 与 `level=2` 可以同时使用，但 `level=2` 只是冗余限定。

### 请求样例

```http
GET /api/v1/admin/categories?status=ENABLED
Authorization: Bearer eyJhbGciOiJIUzI1NiJ9.admin
```

### 响应参数

#### data 字段说明

默认返回分类树，一级分类下包含二级分类列表，二级分类的 `children` 为空数组。

| 字段名称 | 字段类型 | 字段解释 | 业务例子 |
| --- | --- | --- | --- |
| `id` | `Long` | 分类 ID | `20001` |
| `parentId` | `Long` | 父分类 ID，一级分类为空 | `NULL`、`20001` |
| `level` | `Integer` | 分类层级 | `1`、`2` |
| `name` | `String` | 分类名称 | `技术`、`Java` |
| `description` | `String` | 分类描述 | `技术内容一级分类` |
| `sortNo` | `Integer` | 排序值，值越小越靠前 | `10` |
| `status` | `String` | 分类状态 | `ENABLED` |
| `articleCount` | `Long` | 关联文章数。一级分类为其下全部二级分类汇总，二级分类为自身文章数 | `18` |
| `createdAt` | `String` | 创建时间 | `2026-04-22T21:10:00+08:00` |
| `children` | `Array<Object>` | 子分类列表。一级分类下返回二级分类，二级分类返回空数组 | `[{"id":21001,"name":"Java","children":[]}]` |

### 响应样例

```json
{
  "code": 0,
  "message": "成功",
  "data": [
    {
      "id": 20001,
      "parentId": null,
      "level": 1,
      "name": "技术",
      "description": "技术内容一级分类",
      "sortNo": 10,
      "status": "ENABLED",
      "articleCount": 18,
      "createdAt": "2026-04-22T21:10:00+08:00",
      "children": [
        {
          "id": 21001,
          "parentId": 20001,
          "level": 2,
          "name": "Java",
          "description": "Java 相关文章",
          "sortNo": 11,
          "status": "ENABLED",
          "articleCount": 6,
          "createdAt": "2026-04-22T21:20:00+08:00",
          "children": []
        },
        {
          "id": 21002,
          "parentId": 20001,
          "level": 2,
          "name": "算法",
          "description": "算法相关内容",
          "sortNo": 12,
          "status": "ENABLED",
          "articleCount": 4,
          "createdAt": "2026-04-22T21:21:00+08:00",
          "children": []
        }
      ]
    }
  ]
}
```

## 9.2 创建分类

- 路由：`POST`
- 路径：`/api/v1/admin/categories`
- 权限：`ADMIN`

### 请求参数

#### Body 参数

| 字段名称 | 字段类型 | 必填 | 字段解释 | 业务例子 |
| --- | --- | --- | --- | --- |
| `parentId` | `Long` | 否 | 父分类 ID。创建一级分类时为空，创建二级分类时必填一级分类 ID | `NULL`、`20001` |
| `level` | `Integer` | 是 | 分类层级，`1` 为一级分类，`2` 为二级分类 | `2` |
| `name` | `String` | 是 | 分类名称。一级分类全局唯一；二级分类在同一父分类下唯一 | `Java` |
| `description` | `String` | 否 | 分类描述 | `Java 相关文章` |
| `sortNo` | `Integer` | 否 | 排序值，值越小越靠前，默认 `0` | `10` |
| `status` | `String` | 否 | 分类状态，默认 `ENABLED` | `ENABLED` |

### 请求样例

```http
POST /api/v1/admin/categories
Authorization: Bearer eyJhbGciOiJIUzI1NiJ9.admin
Content-Type: application/json

{
  "parentId": 20001,
  "level": 2,
  "name": "Java",
  "description": "Java 相关文章",
  "sortNo": 11,
  "status": "ENABLED"
}
```

### 响应参数

#### data 字段说明

| 字段名称 | 字段类型 | 字段解释 | 业务例子 |
| --- | --- | --- | --- |
| `id` | `Long` | 分类 ID | `21001` |
| `parentId` | `Long` | 父分类 ID | `20001` |
| `level` | `Integer` | 分类层级 | `2` |
| `name` | `String` | 分类名称 | `Java` |
| `status` | `String` | 分类状态 | `ENABLED` |
| `createdAt` | `String` | 创建时间 | `2026-04-23T00:05:00+08:00` |

### 响应样例

```json
{
  "code": 0,
  "message": "成功",
  "data": {
    "id": 21001,
    "parentId": 20001,
    "level": 2,
    "name": "Java",
    "status": "ENABLED",
    "createdAt": "2026-04-23T00:05:00+08:00"
  }
}
```

## 9.3 更新分类

- 路由：`PUT`
- 路径：`/api/v1/admin/categories/{categoryId}`
- 权限：`ADMIN`

说明：

- 更新分类时不允许修改分类层级，`level` 必须与当前分类原层级保持一致。
- 若需调整分类层级，应先新建目标层级分类，再迁移相关内容后删除原分类。

### 请求参数

#### Path 参数

| 字段名称 | 字段类型 | 必填 | 字段解释 | 业务例子 |
| --- | --- | --- | --- | --- |
| `categoryId` | `Long` | 是 | 分类 ID | `21001` |

#### Body 参数

| 字段名称 | 字段类型 | 必填 | 字段解释 | 业务例子 |
| --- | --- | --- | --- | --- |
| `parentId` | `Long` | 否 | 父分类 ID。一级分类为空，二级分类必须指向一级分类 | `NULL`、`20001` |
| `level` | `Integer` | 是 | 分类层级，更新时必须与当前分类原层级保持一致 | `2` |
| `name` | `String` | 是 | 分类名称 | `Java` |
| `description` | `String` | 否 | 分类描述 | `Java 相关文章` |
| `sortNo` | `Integer` | 否 | 排序值，值越小越靠前 | `5` |
| `status` | `String` | 否 | 分类状态 | `ENABLED` |

### 请求样例

```http
PUT /api/v1/admin/categories/21001
Authorization: Bearer eyJhbGciOiJIUzI1NiJ9.admin
Content-Type: application/json

{
  "parentId": 20001,
  "level": 2,
  "name": "Java",
  "description": "Java 核心相关文章",
  "sortNo": 11,
  "status": "ENABLED"
}
```

### 响应参数

#### data 字段说明

| 字段名称 | 字段类型 | 字段解释 | 业务例子 |
| --- | --- | --- | --- |
| `id` | `Long` | 分类 ID | `21001` |
| `parentId` | `Long` | 父分类 ID | `20001` |
| `level` | `Integer` | 分类层级 | `2` |
| `name` | `String` | 分类名称 | `Java` |
| `status` | `String` | 分类状态 | `ENABLED` |
| `updatedAt` | `String` | 更新时间 | `2026-04-23T00:06:00+08:00` |

### 响应样例

```json
{
  "code": 0,
  "message": "成功",
  "data": {
    "id": 21001,
    "parentId": 20001,
    "level": 2,
    "name": "Java",
    "status": "ENABLED",
    "updatedAt": "2026-04-23T00:06:00+08:00"
  }
}
```

## 9.4 删除分类

- 路由：`DELETE`
- 路径：`/api/v1/admin/categories/{categoryId}`
- 权限：`ADMIN`

说明：

-   删除一级分类前，必须先删除其下所有二级分类。
-   删除二级分类前，必须确认该分类下没有关联文章。

### 请求参数

#### Path 参数

| 字段名称 | 字段类型 | 必填 | 字段解释 | 业务例子 |
| --- | --- | --- | --- | --- |
| `categoryId` | `Long` | 是 | 分类 ID | `21001` |

### 请求样例

```http
DELETE /api/v1/admin/categories/21001
Authorization: Bearer eyJhbGciOiJIUzI1NiJ9.admin
```

### 响应参数

无业务数据返回，`data` 固定为 `null`。

### 响应样例

```json
{
  "code": 0,
  "message": "成功",
  "data": null
}
```

## 10. 后台标签管理接口

## 10.1 获取标签列表

- 路由：`GET`
- 路径：`/api/v1/admin/tags`
- 权限：`ADMIN`

### 请求参数

#### Query 参数

| 字段名称 | 字段类型 | 必填 | 字段解释 | 业务例子 |
| --- | --- | --- | --- | --- |
| `keyword` | `String` | 否 | 标签名称模糊搜索 | `JWT` |
| `status` | `String` | 否 | 标签状态筛选 | `ENABLED` |

### 请求样例

```http
GET /api/v1/admin/tags?status=ENABLED
Authorization: Bearer eyJhbGciOiJIUzI1NiJ9.admin
```

### 响应参数

#### data 字段说明

| 字段名称 | 字段类型 | 字段解释 | 业务例子 |
| --- | --- | --- | --- |
| `id` | `Long` | 标签 ID | `30001` |
| `name` | `String` | 标签名称 | `JWT` |
| `description` | `String` | 标签描述 | `和认证授权相关的文章标签` |
| `status` | `String` | 标签状态 | `ENABLED` |
| `articleCount` | `Long` | 关联文章数 | `5` |
| `createdAt` | `String` | 创建时间 | `2026-04-22T21:15:00+08:00` |

### 响应样例

```json
{
  "code": 0,
  "message": "成功",
  "data": [
    {
      "id": 30001,
      "name": "JWT",
      "description": "和认证授权相关的文章标签",
      "status": "ENABLED",
      "articleCount": 5,
      "createdAt": "2026-04-22T21:15:00+08:00"
    }
  ]
}
```

## 10.2 创建标签

- 路由：`POST`
- 路径：`/api/v1/admin/tags`
- 权限：`ADMIN`

### 请求参数

#### Body 参数

| 字段名称 | 字段类型 | 必填 | 字段解释 | 业务例子 |
| --- | --- | --- | --- | --- |
| `name` | `String` | 是 | 标签名称，需唯一 | `JWT` |
| `description` | `String` | 否 | 标签描述 | `和认证授权相关的文章标签` |
| `status` | `String` | 否 | 标签状态，默认 `ENABLED` | `ENABLED` |

### 请求样例

```http
POST /api/v1/admin/tags
Authorization: Bearer eyJhbGciOiJIUzI1NiJ9.admin
Content-Type: application/json

{
  "name": "JWT",
  "description": "和认证授权相关的文章标签",
  "status": "ENABLED"
}
```

### 响应参数

#### data 字段说明

| 字段名称 | 字段类型 | 字段解释 | 业务例子 |
| --- | --- | --- | --- |
| `id` | `Long` | 标签 ID | `30001` |
| `name` | `String` | 标签名称 | `JWT` |
| `status` | `String` | 标签状态 | `ENABLED` |
| `createdAt` | `String` | 创建时间 | `2026-04-23T00:12:00+08:00` |

### 响应样例

```json
{
  "code": 0,
  "message": "成功",
  "data": {
    "id": 30001,
    "name": "JWT",
    "status": "ENABLED",
    "createdAt": "2026-04-23T00:12:00+08:00"
  }
}
```

## 10.3 更新标签

- 路由：`PUT`
- 路径：`/api/v1/admin/tags/{tagId}`
- 权限：`ADMIN`

### 请求参数

#### Path 参数

| 字段名称 | 字段类型 | 必填 | 字段解释 | 业务例子 |
| --- | --- | --- | --- | --- |
| `tagId` | `Long` | 是 | 标签 ID | `30001` |

#### Body 参数

| 字段名称 | 字段类型 | 必填 | 字段解释 | 业务例子 |
| --- | --- | --- | --- | --- |
| `name` | `String` | 是 | 标签名称 | `JWT` |
| `description` | `String` | 否 | 标签描述 | `认证授权相关文章` |
| `status` | `String` | 否 | 标签状态 | `ENABLED` |

### 请求样例

```http
PUT /api/v1/admin/tags/30001
Authorization: Bearer eyJhbGciOiJIUzI1NiJ9.admin
Content-Type: application/json

{
  "name": "JWT",
  "description": "认证授权相关文章",
  "status": "ENABLED"
}
```

### 响应参数

#### data 字段说明

| 字段名称 | 字段类型 | 字段解释 | 业务例子 |
| --- | --- | --- | --- |
| `id` | `Long` | 标签 ID | `30001` |
| `name` | `String` | 标签名称 | `JWT` |
| `status` | `String` | 标签状态 | `ENABLED` |
| `updatedAt` | `String` | 更新时间 | `2026-04-23T00:13:00+08:00` |

### 响应样例

```json
{
  "code": 0,
  "message": "成功",
  "data": {
    "id": 30001,
    "name": "JWT",
    "status": "ENABLED",
    "updatedAt": "2026-04-23T00:13:00+08:00"
  }
}
```

## 10.4 删除标签

- 路由：`DELETE`
- 路径：`/api/v1/admin/tags/{tagId}`
- 权限：`ADMIN`

### 请求参数

#### Path 参数

| 字段名称 | 字段类型 | 必填 | 字段解释 | 业务例子 |
| --- | --- | --- | --- | --- |
| `tagId` | `Long` | 是 | 标签 ID | `30001` |

### 请求样例

```http
DELETE /api/v1/admin/tags/30001
Authorization: Bearer eyJhbGciOiJIUzI1NiJ9.admin
```

### 响应参数

无业务数据返回，`data` 固定为 `null`。

### 响应样例

```json
{
  "code": 0,
  "message": "成功",
  "data": null
}
```

## 11. 评论接口

## 11.1 获取顶层评论分页列表

- 路由：`GET`
- 路径：`/api/v1/articles/{articleId}/comments`
- 权限：`PUBLIC`，可选携带 Access Token

该接口只返回顶层评论、`APPROVED` 回复数量和当前请求者是否存在可见回复，不直接携带回复记录。游客只看到 `APPROVED` 评论；登录用户还可以看到自己发表的 `PENDING`、`REJECTED` 评论及处理原因。顶层评论按 `created_at DESC, id DESC` 排序。

不携带 Access Token 时按游客身份处理；请求一旦携带 Token，Token 无效或过期必须返回 HTTP `401`，不能静默降级为游客，否则会隐藏当前用户自己的非公开评论。

### 请求参数

| 参数位置 | 字段名称 | 字段类型 | 必填 | 字段解释 |
| --- | --- | --- | --- | --- |
| Path | `articleId` | `Long` | 是 | 已发布文章 ID |
| Query | `pageNum` | `Integer` | 否 | 页码，默认 `1` |
| Query | `pageSize` | `Integer` | 否 | 每页条数，默认 `10`，最大 `20` |

### 响应参数

`data` 使用通用分页结构。顶层评论返回 `id`、`articleId`、`content`、`status`、`moderationReason`、`author`、`replyCount`、`hasVisibleReplies`、`isMine` 和 `createdAt`。`replyCount` 只统计该顶层评论下状态为 `APPROVED` 的全部层级回复；登录用户仍可以看到自己处于 `PENDING`、`REJECTED` 状态的回复，但这些回复不计入数量。`hasVisibleReplies` 表示当前请求者是否至少可以看到一条回复，用于在仅有本人未通过回复时保留展开入口。

## 11.2 获取顶层评论下的回复

- 路由：`GET`
- 路径：`/api/v1/comments/{commentId}/replies`
- 权限：`PUBLIC`，可选携带 Access Token

`commentId` 必须指向顶层评论。接口返回该顶层评论下所有层级的可见回复，并平铺为一个列表；`parentId` 保留直接父评论关系，`replyToUser` 用于显示直接被回复的用户。回复按 `created_at ASC, id ASC` 排序。

不携带 Access Token 时按游客身份处理；请求一旦携带 Token，Token 无效或过期必须返回 HTTP `401`，不能静默降级为游客，否则会隐藏当前用户自己的非公开回复。

首次展开默认请求 `limit=5`；之后把响应中的非空 `nextCursor` 原样传回并使用 `limit=10` 继续加载。`cursor` 是服务端生成的不透明字符串，前端不得解析或自行构造。

### 请求参数

| 参数位置 | 字段名称 | 字段类型 | 必填 | 字段解释 |
| --- | --- | --- | --- | --- |
| Path | `commentId` | `Long` | 是 | 顶层评论 ID |
| Query | `limit` | `Integer` | 否 | 本次获取条数，首次默认 `5`，最大 `20` |
| Query | `cursor` | `String` | 否 | 上一次响应返回的游标；首次请求不传 |

### 响应参数

响应包含 `records`、`nextCursor` 和 `hasNext`。单条回复包含顶层评论的公共字段，并额外返回 `parentId`、`rootId` 和 `replyToUser`。

## 11.3 发表评论或回复

- 路由：`POST`
- 路径：`/api/v1/articles/{articleId}/comments`
- 权限：`LOGIN`

`parentId` 为空时创建顶层评论；传入时创建回复。回复目标必须属于当前文章且状态为 `APPROVED`。普通用户新评论和回复创建为 `PENDING`，审核前只对作者本人和管理员可见；管理员新评论和回复直接创建为 `APPROVED`，不产生审核人和审核时间。

### Body 参数

| 字段名称 | 字段类型 | 必填 | 字段解释 |
| --- | --- | --- | --- |
| `content` | `String` | 是 | 纯文本内容，去除首尾空白后长度 `1-1000` |
| `parentId` | `Long` | 否 | 直接父评论 ID；不传表示顶层评论 |

### 响应规则

- 顶层评论的 `parentId`、`rootId` 均为空；回复的 `rootId` 指向所属顶层评论。
- 普通用户返回创建后的 `PENDING` 评论，供作者立即看到审核状态；管理员返回直接通过的 `APPROVED` 评论。
- 同一用户对同一文章 10 秒内只能成功创建一条评论或回复，超限返回 HTTP `429` 和业务码 `106004`。
- `allowComment=false` 时返回 HTTP `409` 和业务码 `106002`，已有评论仍可读取和删除。

## 11.4 删除自己的评论

- 路由：`DELETE`
- 路径：`/api/v1/comments/{commentId}`
- 权限：`LOGIN`

用户只能删除自己的评论。删除采用逻辑删除，并在同一事务中把目标评论及其全部后代标记为 `DELETED`；所有被删除且原为 `APPROVED` 的记录均从文章 `comment_count` 中扣除。成功时返回 `deletedApprovedCount`，表示本次实际扣减的已通过评论数量。

## 11.5 获取后台评论分页列表

- 路由：`GET`
- 路径：`/api/v1/admin/comments`
- 权限：`ADMIN`

支持 `pageNum`、`pageSize`、`articleId`、`userId`、`status`、`type`、`createdAtFrom` 和 `createdAtTo` 查询参数；`type` 可取 `TOP_LEVEL`、`REPLY`。列表按 `created_at DESC, id DESC` 排序，记录返回文章、作者、层级、审核与删除信息。

### 请求参数

| 参数位置 | 字段名称 | 字段类型 | 必填 | 字段解释 |
| --- | --- | --- | --- | --- |
| Query | `pageNum` | `Integer` | 否 | 页码，默认 `1` |
| Query | `pageSize` | `Integer` | 否 | 每页条数，默认 `10` |
| Query | `articleId` | `Long` | 否 | 文章 ID |
| Query | `userId` | `Long` | 否 | 评论用户 ID |
| Query | `status` | `String` | 否 | 评论状态 |
| Query | `type` | `String` | 否 | 评论层级类型：`TOP_LEVEL`、`REPLY` |
| Query | `createdAtFrom` | `String` | 否 | 创建时间范围开始，ISO 8601 时间 |
| Query | `createdAtTo` | `String` | 否 | 创建时间范围结束，ISO 8601 时间 |

## 11.6 审核、隐藏或删除评论

- 路由：`PATCH`
- 路径：`/api/v1/admin/comments/{commentId}/moderation`
- 权限：`ADMIN`

### Body 参数

| 字段名称 | 字段类型 | 必填 | 字段解释 |
| --- | --- | --- | --- |
| `action` | `String` | 是 | `APPROVE`、`REJECT`、`HIDE` 或 `DELETE` |
| `reason` | `String` | 条件必填 | 最长 255 个字符；拒绝、隐藏、删除时必填，通过时不传 |

### 状态流转

| 当前状态 | 允许动作 | 目标状态 |
| --- | --- | --- |
| `PENDING` | `APPROVE`、`REJECT`、`DELETE` | `APPROVED`、`REJECTED`、`DELETED` |
| `REJECTED` | `APPROVE`、`DELETE` | `APPROVED`、`DELETED` |
| `APPROVED` | `HIDE`、`DELETE` | `HIDDEN`、`DELETED` |
| `HIDDEN` | `APPROVE`、`DELETE` | `APPROVED`、`DELETED` |
| `DELETED` | 无 | 不允许继续处理 |

通过、拒绝、隐藏成功后写入 `reviewedBy`、`reviewedAt`；拒绝、隐藏、删除保存 `moderationReason`，通过时清空该字段。管理员删除写入 `deletedBy`、`deletedAt`，不清空历史 `reviewedBy`、`reviewedAt`。状态变化、子树逻辑删除、`comment_count` 更新和后台操作审计日志必须保持事务一致性。管理员不能修改评论正文。

## 11.7 P2 评论直接回复邮件通知预留

- P1 评论接口和 OpenAPI 保持不变；P2 实现时再为创建评论请求增加可选的 `notifyOnReply` 字段，并补充退订接口
- `notifyOnReply=true` 表示当前登录用户订阅这条新评论未来的直接回复，收件地址使用账号邮箱
- 只有其他用户创建的直接回复变为 `APPROVED` 后才触发通知；普通用户回复处于 `PENDING` 时不发送，自己回复自己不发送
- 退订操作以单条评论为范围且保持幂等，不影响其他评论订阅
- P2 评论通知和留言通知均发送 `multipart/alternative`：`text/plain` 与 `text/html` 内容语义一致，客户端自行选择可渲染版本
- HTML 正文中的昵称、文章标题、评论及回复必须转义；链接使用绝对 HTTPS 地址，不依赖 JavaScript、外部 CSS 或表单

## 12. 留言接口

### 12.1 获取留言分页列表

- 路由：`GET /api/v1/messages`
- 权限：`PUBLIC`，可选携带 Access Token
- 只分页返回顶层留言；每条顶层留言携带其管理员回复。游客只能看到 `APPROVED`，登录用户还可看到自己 `PENDING`、`REJECTED` 的顶层留言。
- 顶层留言按 `created_at DESC, id DESC` 排序，回复按 `created_at ASC, id ASC` 排序；不返回邮箱。
- 不携带 Access Token 时按游客身份处理；请求一旦携带 Token，Token 无效或过期必须返回 HTTP `401`，不能静默降级为游客，否则会隐藏当前用户自己的非公开留言。

### 12.2 发表留言

- 路由：`POST /api/v1/messages`
- 权限：`PUBLIC`，可选携带 Access Token
- 游客必须填写 1-20 个字符昵称；邮箱可选，勾选 `notifyOnReply` 时必填且格式合法。登录用户忽略昵称和邮箱输入，使用当前账号资料快照；勾选 `notifyOnReply` 时使用账号邮箱接收通知。
- 内容为 1-1000 个字符的纯文本；普通用户和游客为 `PENDING`，管理员为 `APPROVED`。
- 游客按 IP 每 30 秒 1 条、每小时最多 10 条，登录用户按用户 ID执行相同 Redis 原子限流。
- 不携带 Access Token 时按游客请求校验；请求一旦携带 Token，Token 无效或过期必须返回 HTTP `401`，由前端刷新登录态后重试，不能降级为游客后再校验游客昵称。

### 12.3 删除自己的留言

- 路由：`DELETE /api/v1/messages/{messageId}`
- 权限：`LOGIN`
- 只能删除自己的顶层留言，采用逻辑删除；管理员回复一并标记为 `DELETED`。操作幂等性不对外承诺，目标不存在或已删除返回资源不存在。

### 12.4 退订留言回复通知

- 路由：`POST /api/v1/messages/notifications/unsubscribe`
- 权限：`PUBLIC`
- Body：`{ "token": "..." }`。令牌为随机不透明值，仅对应一条顶层留言。令牌无效返回 `MESSAGE_UNSUBSCRIBE_TOKEN_INVALID`；重复提交已退订令牌仍返回成功。
- 前端退订页面的 GET 只展示确认信息，POST 后才修改通知状态；只影响未来回复。

### 12.5 获取后台留言分页列表

- 路由：`GET /api/v1/admin/messages`
- 权限：`ADMIN`
- 支持 `pageNum`、`pageSize`、`messageId`、`userId`、`guestNickname`、`guestEmail`、`content`、`status`、`type`、`createdAtFrom`、`createdAtTo`。后台响应可返回私有邮箱、通知开关、审核与删除信息。

### 12.6 审核、隐藏或删除留言

- 路由：`PATCH /api/v1/admin/messages/{messageId}/moderation`
- 权限：`ADMIN`
- Body 与评论审核相同，动作是 `APPROVE`、`REJECT`、`HIDE`、`DELETE`；拒绝、隐藏、删除必须提供原因。删除顶层留言时其管理员回复一起逻辑删除，删除单条回复不影响顶层留言。
- 状态流转与评论一致：`PENDING` 可通过、拒绝或删除，`APPROVED` 可隐藏或删除，`REJECTED`、`HIDDEN` 可重新通过或删除，`DELETED` 为终态。

### 12.7 管理员回复留言

- 路由：`POST /api/v1/admin/messages/{messageId}/replies`
- 权限：`ADMIN`
- `messageId` 必须是已通过的顶层留言。管理员回复直接为 `APPROVED`，管理员可对同一留言发表多条回复。事务提交后按该留言的通知开关异步发送邮件；P1 使用纯文本，P2 升级为同时携带 `text/plain` 与 `text/html` 的 `multipart/alternative`。

### 12.8 批量通过留言

- 路由：`PATCH /api/v1/admin/messages/batch-approval`
- 权限：`ADMIN`
- Body：`{ "messageIds": [90001, 90002] }`，不能为空且最多 100 个。所有 ID 必须是 `PENDING` 顶层留言，否则整批失败；成功后统一写入审核管理员和审核时间。

## 12.9 后台操作审计日志

### 12.9.1 获取审计日志分页列表

- 路由：`GET`
- 路径：`/api/v1/admin/audit-logs`
- 权限：`ADMIN`

支持 `pageNum`、`pageSize`、`operatorId`、`resourceType`、`resourceId`、`action`、`result`、`createdAtFrom` 和 `createdAtTo` 查询参数。列表按 `created_at DESC, id DESC` 排序。

响应记录包含操作者用户名快照、目标资源、操作明细、执行结果、失败业务码、失败说明、请求方法、请求路径和操作时间。审计日志只允许追加和查询，不提供修改或删除接口。

审计记录不包含请求体、query 参数、密码、Token、邮箱、评论或留言正文、文件原名等敏感数据。后台写操作成功时，业务数据和 SUCCESS 日志在同一事务中提交；业务失败在原事务回滚后以独立事务记录 FAILURE。

#### Query 参数

| 参数位置 | 字段名称 | 字段类型 | 必填 | 字段解释 |
| --- | --- | --- | --- | --- |
| Query | `pageNum` | `Integer` | 否 | 页码，默认 `1` |
| Query | `pageSize` | `Integer` | 否 | 每页条数，默认 `10` |
| Query | `operatorId` | `Long` | 否 | 操作者用户 ID |
| Query | `resourceType` | `String` | 否 | `USER`、`ARTICLE`、`CATEGORY`、`TAG`、`COMMENT`、`MESSAGE` 或 `FILE` |
| Query | `resourceId` | `String` | 否 | 目标资源 ID、批量 ID 列表或对象 URL，支持模糊匹配 |
| Query | `action` | `String` | 否 | 操作类型 |
| Query | `result` | `String` | 否 | `SUCCESS` 或 `FAILURE` |
| Query | `createdAtFrom` | `String` | 否 | 创建时间范围开始，ISO 8601 时间 |
| Query | `createdAtTo` | `String` | 否 | 创建时间范围结束，ISO 8601 时间 |

## 13. 图片上传接口

图片上传使用 `multipart/form-data`，由后端校验并中转上传到阿里云 OSS 公共读 Bucket。前端不得接触对象存储 AccessKey。支持 JPEG（`.jpg`、`.jpeg`）、PNG、WebP 和 GIF，不支持 SVG；后端以文件真实内容识别结果为准，不信任客户端文件扩展名或 Content-Type。

上传对象使用不可变 UUID key，并通过配置的自定义公开域名返回完整 URL。P1 不压缩、不转换格式、不生成缩略图，也不自动删除被替换或失去引用的 OSS 对象。

### 13.1 上传并更新当前用户头像

- 路由：`PUT`
- 路径：`/api/v1/users/me/avatar`
- 权限：`LOGIN`
- Content-Type：`multipart/form-data`

请求字段：

| 位置 | 字段 | 类型 | 必填 | 说明 |
| --- | --- | --- | --- | --- |
| Form Data | `file` | `binary` | 是 | 头像图片，最大 2 MB |

上传成功后，后端更新 `blog_user.avatar_url` 并返回当前头像信息：

```json
{
  "code": 0,
  "message": "成功",
  "data": {
    "id": 10002,
    "avatarUrl": "https://img.example.com/avatars/10002/2026/08/uuid.jpg",
    "updatedAt": "2026-08-09T10:00:00+08:00"
  }
}
```

同一用户每分钟最多成功上传 1 次头像，并且每 24 小时最多成功上传 10 次。限流使用 Redis 原子执行；上传或数据库事务失败时释放本次预占额度。

### 13.2 上传后台图片

- 路由：`POST`
- 路径：`/api/v1/admin/files/images`
- 权限：`ADMIN`
- Content-Type：`multipart/form-data`

请求字段：

| 位置 | 字段 | 类型 | 必填 | 说明 |
| --- | --- | --- | --- | --- |
| Form Data | `file` | `binary` | 是 | 图片文件，最大 10 MB |
| Form Data | `scene` | `String` | 是 | `ARTICLE_COVER` 或 `ARTICLE_CONTENT`；`PROJECT_COVER` 为未来项目模块预留 |

缺少图片时返回 `107001`；`scene` 缺失或枚举值不合法时返回 `199001`。

响应：

```json
{
  "code": 0,
  "message": "成功",
  "data": {
    "url": "https://img.example.com/articles/content/2026/08/uuid.png",
    "originalName": "architecture.png",
    "contentType": "image/png",
    "size": 245760
  }
}
```

文章封面上传后由前端把 URL 回填至原有封面输入框；文章正文图片上传后由 `md-editor-v3` 插入 Markdown 图片语法。该接口仅创建 OSS 对象，不直接修改文章或项目数据。

## 14. 业务规则补充

### 14.1 用户相关

- 被禁用用户不可登录
- 被禁用用户已有 Access Token 通过 tokenVersion 校验立即失效
- 被禁用用户在刷新 Token 时必须失败，返回 `102005`
- 管理员不能修改当前登录用户自身的状态和角色
- 用户修改密码或被禁用后，后端撤销该用户全部活跃 Refresh Token
- 用户角色变更后不直接撤销 Refresh Token，但会递增 tokenVersion；新的角色在刷新登录态或重新登录后生效
- 修改密码、禁用用户、修改角色后，旧 Access Token 通过 Redis + tokenVersion 校验立即失效
- 用户名注册后不支持在个人中心修改
- 用户名不允许包含 `@`，登录接口可用 `account` 是否包含 `@` 区分邮箱登录和用户名登录
- Refresh Token 每次刷新成功后都必须轮转，本次请求携带的旧 Refresh Token 标记为 `REVOKED`
- Refresh Token 缺失、格式错误、签名无效、已过期、已撤销或找不到对应会话时，统一返回 `101004`
- P0 阶段不因普通刷新失败自动撤销该用户全部活跃 Refresh Token；明确安全事件，如修改密码、用户禁用、管理员强制下线，可按对应业务规则撤销全部会话
- P1 阶段可结合 Redis 短 TTL 宽限期、`token_jti` 状态和 `tokenVersion` 增强幂等重试与重放检测

### 14.2 文章相关

- 前台文章列表和详情仅返回 `PUBLISHED` 状态文章
- 草稿和下线文章仅后台可见
- `DRAFT` 表示从未发布过的草稿；`OFFLINE` 表示曾经发布过但当前下线
- 创建文章时仅允许 `DRAFT` 或 `PUBLISHED`，不允许直接创建为 `OFFLINE`
- 从未发布过的文章可保持 `DRAFT` 或发布为 `PUBLISHED`，不允许转为 `OFFLINE`
- 已发布过的文章仅允许在 `PUBLISHED` 与 `OFFLINE` 之间流转，不允许回退为 `DRAFT`
- `publishedAt` 表示首次发布时间，仅在文章第一次变为 `PUBLISHED` 时写入；下线、重新发布或更新已发布文章时不刷新
- 文章必须绑定二级分类，不能直接绑定一级分类
- 文章保存或更新时，后端应以 `contentMd` 为源自动生成并持久化 `contentHtml`
- 文章保存或更新时，后端应以 `contentMd` 为源自动提取并持久化 `contentText`
- 前台文章详情接口优先返回 `contentHtml` 用于页面渲染
- 前台文章列表和详情响应中的 `tags` 仅展示当前启用且仍存在的关联标签；历史关联的禁用标签或已删除标签不返回
- 前台文章搜索复用文章列表接口，通过 PostgreSQL `zhparser` 对标题、摘要和纯文本正文执行全文检索，并返回可选的安全高亮片段
- 前台文章列表通过 `tagIds` 筛选时，传入标签必须存在且启用；不存在返回 `ARTICLE_TAG_NOT_FOUND`，禁用返回 `ARTICLE_TAG_DISABLED`
- 前台文章列表通过 `categoryId` 筛选时，传入分类必须存在且启用；不存在返回 `ARTICLE_CATEGORY_NOT_FOUND`，禁用返回 `ARTICLE_CATEGORY_DISABLED`
- 前台文章列表和详情要求文章所属二级分类及其父分类均存在且启用；分类不存在或禁用时，文章对前台不可见
- P0 文章详情以 `articleId` 定位；P2 再考虑将 `slug` 追加到前台 URL 中提升可读性与 SEO 表达
- 文章从 `PUBLISHED` 修改为 `OFFLINE` 后，前台立即不可见

### 14.3 分类和标签相关

- 删除一级分类前需要校验其自身及其下所有二级分类是否存在关联文章
- 删除二级分类前需要校验该二级分类是否存在关联文章
- 删除标签时可由后端同步清理 `article_tag` 关联关系
- 前台分类接口默认返回一级分类树，二级分类挂载在 `children` 字段
- 前台主导航不展示标签，标签主要用于筛选和后续搜索

### 14.4 评论相关

- `comment_count` 统计文章下全部 `APPROVED` 评论，包括顶层评论和回复
- 关闭 `allowComment` 只阻止新评论和回复，不影响已有评论展示和作者删除
- 前台只显示两层，数据层仍保留无限层级的 `parentId` 和 `rootId`
- 顶层评论分页不预载回复；回复由用户点击“共 x 条回复，点击查看”后按需请求
- 评论内容只按纯文本展示，不解析 HTML 或 Markdown

### 14.5 图片上传相关

- 图片二进制只保存在 OSS，数据库和 Markdown 只保存完整公开 URL
- P1 不新增文件资源表，不维护图片引用关系，不提供删除 OSS 对象的业务接口
- 用户头像通过个人中心接口上传；文章封面和正文图片仅管理员可上传，项目封面场景暂为未来模块预留
- 后台文章封面仍允许手工输入外部 URL，上传 OSS 后自动回填同一字段
- OSS Bucket 为公共读，但写入权限仅授予后端使用的 RAM 子账号；AccessKey、Secret 和 Bucket 配置不得返回前端或写入日志

### 14.6 日志与审计

- P0 阶段保留认证成功、认证失败、改密、禁用用户等关键安全事件的应用日志，避免记录密码、Token 等敏感值
- 登录失败时，未匹配到用户的账号输入必须脱敏；匹配到用户后的密码错误或账号禁用事件记录用户 ID 和规范用户名，不记录输入邮箱
- 留言回复通知在邮件功能已启用但邮件发送器不可用时，记录不包含邮箱和正文的结构化失败日志；通知失败不回滚已提交的管理员回复
- P1 阶段补充后台管理操作审计日志，记录操作者用户 ID、用户名快照、目标资源类型与标识、操作类型、操作结果、失败业务码、请求方法、请求路径和操作时间
- 审计日志只允许追加和查询；成功日志与业务操作同事务提交，失败日志在业务事务回滚后以独立事务提交
- 审计日志不记录请求体、查询参数、密码、Token、邮箱、评论或留言正文、文件原名等敏感值

## 15. 后续版本预留接口

以下接口不在 P0 范围内，仅做路由预留说明：

| 模块 | 路由 | 路径 | 版本规划 |
| --- | --- | --- | --- |
| 点赞 | `POST` | `/api/v1/articles/{articleId}/like` | P2 |
| 取消点赞 | `DELETE` | `/api/v1/articles/{articleId}/like` | P2 |
| 留言 | `GET` | `/api/v1/messages` | P1 |
| 留言 | `POST` | `/api/v1/messages` | P1 |
| 评论批量通过 | `PATCH` | `/api/v1/admin/comments/batch-approval` | 待实际审核量评估 |
| 项目 | `GET` | `/api/v1/projects` | 待有实际项目作品后评估 |
| 项目详情 | `GET` | `/api/v1/projects/{projectId}` | 待有实际项目作品后评估 |
