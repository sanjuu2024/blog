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

### 2.4 权限级别

| 权限级别 | 说明 |
| --- | --- |
| `PUBLIC` | 游客可访问，无需登录 |
| `LOGIN` | 登录用户可访问，管理员也可访问 |
| `ADMIN` | 仅管理员可访问 |

### 2.5 通用响应结构

所有接口统一返回如下结构：

```json
{
  "code": "0",
  "message": "success",
  "data": {},
  "traceId": "f9a7b6d8c1e24f0b",
  "timestamp": "2026-04-22T22:10:00+08:00"
}
```

#### 响应外层字段说明

| 字段名称 | 字段类型 | 字段解释 | 业务例子 |
| --- | --- | --- | --- |
| `code` | `String` | 业务状态码，`0` 表示成功 | `0` |
| `message` | `String` | 业务描述信息 | `success` |
| `data` | `Object/Array/null` | 实际返回数据 | `{}`、`[]`、`null` |
| `traceId` | `String` | 请求链路追踪 ID，便于排查问题 | `f9a7b6d8c1e24f0b` |
| `timestamp` | `String` | 响应时间 | `2026-04-22T22:10:00+08:00` |

### 2.6 常见业务状态码

| 状态码 | 含义 |
| --- | --- |
| `0` | 成功 |
| `A001` | 请求参数不合法 |
| `A002` | 未登录或 Access Token 无效 |
| `A003` | 无权限访问 |
| `A004` | Refresh Token 无效或已过期 |
| `A005` | 用户已被禁用 |
| `U001` | 用户名已存在 |
| `U002` | 邮箱已存在 |
| `U003` | 账号或密码错误 |
| `U004` | 原密码错误 |
| `B001` | 文章不存在 |
| `B002` | 分类不存在 |
| `B003` | 标签不存在 |
| `B004` | 分类或其子分类下存在文章，不能删除 |

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

## 3. 接口总览

| 模块 | 路由 | 路径 | 权限 | 说明 |
| --- | --- | --- | --- | --- |
| 认证 | `POST` | `/api/v1/auth/register` | `PUBLIC` | 用户注册 |
| 认证 | `POST` | `/api/v1/auth/login` | `PUBLIC` | 用户登录 |
| 认证 | `POST` | `/api/v1/auth/refresh` | `PUBLIC` | 刷新登录态 |
| 认证 | `POST` | `/api/v1/auth/logout` | `LOGIN` | 用户退出登录 |
| 前台文章 | `GET` | `/api/v1/articles` | `PUBLIC` | 获取已发布文章分页列表 |
| 前台文章 | `GET` | `/api/v1/articles/{articleId}` | `PUBLIC` | 获取文章详情 |
| 前台分类 | `GET` | `/api/v1/categories` | `PUBLIC` | 获取启用分类列表 |
| 个人中心 | `GET` | `/api/v1/users/me` | `LOGIN` | 获取当前登录用户信息 |
| 个人中心 | `PUT` | `/api/v1/users/me/profile` | `LOGIN` | 更新个人资料 |
| 个人中心 | `PUT` | `/api/v1/users/me/password` | `LOGIN` | 修改密码 |
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
| `username` | `String` | 是 | 用户名，长度建议 4-20，需唯一 | `alice_dev` |
| `email` | `String` | 是 | 邮箱，需唯一 | `alice@example.com` |
| `password` | `String` | 是 | 登录密码，建议 8-20 位 | `Passw0rd!` |

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

#### data 字段说明

| 字段名称 | 字段类型 | 字段解释 | 业务例子 |
| --- | --- | --- | --- |
| `userId` | `Long` | 新创建用户 ID | `10002` |
| `username` | `String` | 用户名 | `alice_dev` |
| `nickname` | `String` | 用户昵称，初始可为空 | `` |
| `email` | `String` | 邮箱 | `alice@example.com` |
| `role` | `String` | 用户角色 | `USER` |
| `status` | `String` | 用户状态 | `ACTIVE` |
| `createdAt` | `String` | 注册时间 | `2026-04-22T22:20:00+08:00` |

### 响应样例

```json
{
  "code": "0",
  "message": "success",
  "data": {
    "userId": 10002,
    "username": "alice_dev",
    "nickname": "",
    "email": "alice@example.com",
    "role": "USER",
    "status": "ACTIVE",
    "createdAt": "2026-04-22T22:20:00+08:00"
  },
  "traceId": "3c5d8f2a0b6344b1",
  "timestamp": "2026-04-22T22:20:00+08:00"
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
| `account` | `String` | 是 | 登录账号，支持用户名或邮箱 | `alice_dev`、`alice@example.com` |
| `password` | `String` | 是 | 登录密码 | `Passw0rd!` |

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
| `accessTokenExpiresAt` | `String` | Access Token 过期时间 | `2026-04-22T23:20:00+08:00` |
| `refreshToken` | `String` | 用于刷新登录态的令牌 | `eyJhbGciOiJIUzI1NiJ9...` |
| `refreshTokenExpiresAt` | `String` | Refresh Token 过期时间 | `2026-04-29T22:20:00+08:00` |
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
  "code": "0",
  "message": "success",
  "data": {
    "accessToken": "eyJhbGciOiJIUzI1NiJ9.access.token",
    "accessTokenExpiresAt": "2026-04-22T23:20:00+08:00",
    "refreshToken": "eyJhbGciOiJIUzI1NiJ9.refresh.token",
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
  },
  "traceId": "b3c8900f2d554b4f",
  "timestamp": "2026-04-22T22:20:10+08:00"
}
```

## 4.3 刷新登录态

- 路由：`POST`
- 路径：`/api/v1/auth/refresh`
- 权限：`PUBLIC`

### 请求参数

#### Body 参数

| 字段名称 | 字段类型 | 必填 | 字段解释 | 业务例子 |
| --- | --- | --- | --- | --- |
| `refreshToken` | `String` | 是 | Refresh Token | `eyJhbGciOiJIUzI1NiJ9.refresh.token` |

### 请求样例

```http
POST /api/v1/auth/refresh
Content-Type: application/json

{
  "refreshToken": "eyJhbGciOiJIUzI1NiJ9.refresh.token"
}
```

### 响应参数

#### data 字段说明

| 字段名称 | 字段类型 | 字段解释 | 业务例子 |
| --- | --- | --- | --- |
| `accessToken` | `String` | 新的 Access Token | `eyJhbGciOiJIUzI1NiJ9.new.access` |
| `accessTokenExpiresAt` | `String` | 新 Access Token 过期时间 | `2026-04-22T23:50:00+08:00` |
| `refreshToken` | `String` | 新的 Refresh Token，若启用轮换则返回新值 | `eyJhbGciOiJIUzI1NiJ9.new.refresh` |
| `refreshTokenExpiresAt` | `String` | 新 Refresh Token 过期时间 | `2026-04-29T22:50:00+08:00` |
| `tokenType` | `String` | Token 类型 | `Bearer` |

### 响应样例

```json
{
  "code": "0",
  "message": "success",
  "data": {
    "accessToken": "eyJhbGciOiJIUzI1NiJ9.new.access",
    "accessTokenExpiresAt": "2026-04-22T23:50:00+08:00",
    "refreshToken": "eyJhbGciOiJIUzI1NiJ9.new.refresh",
    "refreshTokenExpiresAt": "2026-04-29T22:50:00+08:00",
    "tokenType": "Bearer"
  },
  "traceId": "f30b1ed17c9947a2",
  "timestamp": "2026-04-22T22:50:00+08:00"
}
```

## 4.4 用户退出登录

- 路由：`POST`
- 路径：`/api/v1/auth/logout`
- 权限：`LOGIN`

### 请求参数

#### Header 参数

| 字段名称 | 字段类型 | 必填 | 字段解释 | 业务例子 |
| --- | --- | --- | --- | --- |
| `Authorization` | `String` | 是 | Access Token | `Bearer eyJhbGciOiJIUzI1NiJ9.access` |

#### Body 参数

| 字段名称 | 字段类型 | 必填 | 字段解释 | 业务例子 |
| --- | --- | --- | --- | --- |
| `refreshToken` | `String` | 是 | 当前会话对应的 Refresh Token | `eyJhbGciOiJIUzI1NiJ9.refresh.token` |

### 请求样例

```http
POST /api/v1/auth/logout
Authorization: Bearer eyJhbGciOiJIUzI1NiJ9.access
Content-Type: application/json

{
  "refreshToken": "eyJhbGciOiJIUzI1NiJ9.refresh.token"
}
```

### 响应参数

#### data 字段说明

| 字段名称 | 字段类型 | 字段解释 | 业务例子 |
| --- | --- | --- | --- |
| `success` | `Boolean` | 是否退出成功 | `true` |

### 响应样例

```json
{
  "code": "0",
  "message": "success",
  "data": {
    "success": true
  },
  "traceId": "cb16b523eb4f46d2",
  "timestamp": "2026-04-22T22:55:00+08:00"
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
| `categoryId` | `Long` | 否 | 分类 ID，用于分类筛选。支持一级分类或二级分类；传一级分类时返回其下所有二级分类文章 | `20001`、`21001` |
| `tagId` | `Long` | 否 | 标签 ID，用于标签筛选 | `30001` |

### 请求样例

```http
GET /api/v1/articles?pageNum=1&pageSize=10&categoryId=20001
```

### 响应参数

#### data 字段说明

`data` 为分页结构，`records` 中单条记录字段如下：

| 字段名称 | 字段类型 | 字段解释 | 业务例子 |
| --- | --- | --- | --- |
| `id` | `Long` | 文章 ID | `40001` |
| `title` | `String` | 文章标题 | `Spring Boot 双 Token 登录实践` |
| `summary` | `String` | 文章摘要 | `本文记录双 Token 的实现思路与接口设计` |
| `coverUrl` | `String` | 封面地址 | `https://cdn.example.com/cover/token.png` |
| `isTop` | `Boolean` | 是否置顶 | `true` |
| `publishedAt` | `String` | 发布时间 | `2026-04-22T23:00:00+08:00` |
| `viewCount` | `Integer` | 浏览量 | `128` |
| `category.id` | `Long` | 文章绑定的二级分类 ID | `21001` |
| `category.name` | `String` | 二级分类名称 | `Java` |
| `category.level` | `Integer` | 分类层级，固定为 `2` | `2` |
| `category.parent.id` | `Long` | 所属一级分类 ID | `20001` |
| `category.parent.name` | `String` | 所属一级分类名称 | `技术` |
| `tags` | `Array<Object>` | 标签列表 | `[{"id":30001,"name":"JWT"}]` |

### 响应样例

```json
{
  "code": "0",
  "message": "success",
  "data": {
    "records": [
      {
        "id": 40001,
        "title": "Spring Boot 双 Token 登录实践",
        "summary": "本文记录双 Token 的实现思路与接口设计",
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
  },
  "traceId": "7a4ac2ca74df44ce",
  "timestamp": "2026-04-22T23:05:00+08:00"
}
```

## 5.2 获取文章详情

- 路由：`GET`
- 路径：`/api/v1/articles/{articleId}`
- 权限：`PUBLIC`

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
| `allowComment` | `Boolean` | 是否允许评论，当前为预留字段 | `true` |
| `viewCount` | `Integer` | 浏览量 | `128` |
| `commentCount` | `Integer` | 评论数 | `0` |
| `likeCount` | `Integer` | 点赞数 | `0` |
| `favoriteCount` | `Integer` | 收藏数 | `0` |
| `publishedAt` | `String` | 发布时间 | `2026-04-22T23:00:00+08:00` |
| `updatedAt` | `String` | 更新时间 | `2026-04-22T23:10:00+08:00` |
| `category.id` | `Long` | 文章绑定的二级分类 ID | `21001` |
| `category.name` | `String` | 二级分类名称 | `Java` |
| `category.level` | `Integer` | 分类层级，固定为 `2` | `2` |
| `category.parent.id` | `Long` | 所属一级分类 ID | `20001` |
| `category.parent.name` | `String` | 所属一级分类名称 | `技术` |
| `tags` | `Array<Object>` | 标签列表 | `[{"id":30001,"name":"JWT"}]` |
| `author.id` | `Long` | 作者 ID | `10001` |
| `author.username` | `String` | 作者用户名 | `ccsanjuu` |
| `author.nickname` | `String` | 作者昵称 | `sanjuu` |

### 响应样例

```json
{
  "code": "0",
  "message": "success",
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
    "favoriteCount": 0,
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
      "nickname": "sanjuu"
    }
  },
  "traceId": "1d65ea705b9b45d5",
  "timestamp": "2026-04-22T23:10:00+08:00"
}
```

## 5.3 获取启用分类列表

- 路由：`GET`
- 路径：`/api/v1/categories`
- 权限：`PUBLIC`

### 请求参数

无

### 请求样例

```http
GET /api/v1/categories
```

### 响应参数

#### data 字段说明

`data` 返回一级分类树，一级分类下包含二级分类列表。

| 字段名称 | 字段类型 | 字段解释 | 业务例子 |
| --- | --- | --- | --- |
| `id` | `Long` | 一级分类 ID | `20001` |
| `name` | `String` | 一级分类名称 | `技术` |
| `level` | `Integer` | 分类层级，一级固定为 `1` | `1` |
| `description` | `String` | 分类描述 | `技术内容一级分类` |
| `sortNo` | `Integer` | 排序值 | `10` |
| `articleCount` | `Integer` | 一级分类下已发布文章总数 | `18` |
| `children` | `Array<Object>` | 二级分类列表 | `[{"id":21001,"name":"Java"}]` |
| `children[].id` | `Long` | 二级分类 ID | `21001` |
| `children[].parentId` | `Long` | 所属一级分类 ID | `20001` |
| `children[].name` | `String` | 二级分类名称 | `Java` |
| `children[].level` | `Integer` | 分类层级，二级固定为 `2` | `2` |
| `children[].description` | `String` | 二级分类描述 | `Java 相关文章` |
| `children[].sortNo` | `Integer` | 排序值 | `11` |
| `children[].articleCount` | `Integer` | 该二级分类下已发布文章数量 | `6` |

### 响应样例

```json
{
  "code": "0",
  "message": "success",
  "data": [
    {
      "id": 20001,
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
          "articleCount": 6
        },
        {
          "id": 21002,
          "parentId": 20001,
          "name": "算法",
          "level": 2,
          "description": "算法相关内容",
          "sortNo": 12,
          "articleCount": 4
        }
      ]
    }
  ],
  "traceId": "f28d47e62fd64f3e",
  "timestamp": "2026-04-22T23:12:00+08:00"
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
| `lastLoginAt` | `String` | 最近登录时间 | `2026-04-22T22:20:10+08:00` |
| `createdAt` | `String` | 注册时间 | `2026-04-22T22:20:00+08:00` |

### 响应样例

```json
{
  "code": "0",
  "message": "success",
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
  },
  "traceId": "16d68b769fdc4c44",
  "timestamp": "2026-04-22T23:15:00+08:00"
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
| `username` | `String` | 否 | 用户名，若传入则需校验唯一 | `alice_dev_new` |
| `nickname` | `String` | 否 | 用户昵称 | `Alice` |
| `bio` | `String` | 否 | 个人简介 | `专注 Java 与前端工程化` |

### 请求样例

```http
PUT /api/v1/users/me/profile
Authorization: Bearer eyJhbGciOiJIUzI1NiJ9.access
Content-Type: application/json

{
  "username": "alice_dev_new",
  "nickname": "Alice",
  "bio": "专注 Java 与前端工程化"
}
```

### 响应参数

#### data 字段说明

| 字段名称 | 字段类型 | 字段解释 | 业务例子 |
| --- | --- | --- | --- |
| `id` | `Long` | 用户 ID | `10002` |
| `username` | `String` | 更新后的用户名 | `alice_dev_new` |
| `nickname` | `String` | 更新后的昵称 | `Alice` |
| `email` | `String` | 用户邮箱 | `alice@example.com` |
| `avatarUrl` | `String` | 用户头像地址 | `` |
| `bio` | `String` | 更新后的简介 | `专注 Java 与前端工程化` |
| `updatedAt` | `String` | 更新时间 | `2026-04-22T23:20:00+08:00` |

### 响应样例

```json
{
  "code": "0",
  "message": "success",
  "data": {
    "id": 10002,
    "username": "alice_dev_new",
    "nickname": "Alice",
    "email": "alice@example.com",
    "avatarUrl": "",
    "bio": "专注 Java 与前端工程化",
    "updatedAt": "2026-04-22T23:20:00+08:00"
  },
  "traceId": "727541ef1e1c4f77",
  "timestamp": "2026-04-22T23:20:00+08:00"
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
| `oldPassword` | `String` | 是 | 原密码 | `Passw0rd!` |
| `newPassword` | `String` | 是 | 新密码 | `NewPassw0rd!` |

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

#### data 字段说明

| 字段名称 | 字段类型 | 字段解释 | 业务例子 |
| --- | --- | --- | --- |
| `success` | `Boolean` | 是否修改成功 | `true` |

### 响应样例

```json
{
  "code": "0",
  "message": "success",
  "data": {
    "success": true
  },
  "traceId": "b167021c836f4712",
  "timestamp": "2026-04-22T23:25:00+08:00"
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
| `keyword` | `String` | 否 | 用户名或邮箱模糊搜索 | `alice` |
| `role` | `String` | 否 | 角色筛选 | `USER` |
| `status` | `String` | 否 | 状态筛选 | `ACTIVE` |

### 请求样例

```http
GET /api/v1/admin/users?pageNum=1&pageSize=10&keyword=alice&status=ACTIVE
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
| `lastLoginAt` | `String` | 最近登录时间 | `2026-04-22T22:20:10+08:00` |
| `createdAt` | `String` | 注册时间 | `2026-04-22T22:20:00+08:00` |

### 响应样例

```json
{
  "code": "0",
  "message": "success",
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
  },
  "traceId": "ccd57c77325a482e",
  "timestamp": "2026-04-22T23:30:00+08:00"
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

### 响应参数

#### data 字段说明

| 字段名称 | 字段类型 | 字段解释 | 业务例子 |
| --- | --- | --- | --- |
| `id` | `Long` | 用户 ID | `10002` |
| `status` | `String` | 更新后的状态 | `DISABLED` |
| `updatedAt` | `String` | 更新时间 | `2026-04-22T23:35:00+08:00` |

### 响应样例

```json
{
  "code": "0",
  "message": "success",
  "data": {
    "id": 10002,
    "status": "DISABLED",
    "updatedAt": "2026-04-22T23:35:00+08:00"
  },
  "traceId": "9f9b6261c0ad40c1",
  "timestamp": "2026-04-22T23:35:00+08:00"
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

### 响应参数

#### data 字段说明

| 字段名称 | 字段类型 | 字段解释 | 业务例子 |
| --- | --- | --- | --- |
| `id` | `Long` | 用户 ID | `10002` |
| `role` | `String` | 更新后的角色 | `ADMIN` |
| `updatedAt` | `String` | 更新时间 | `2026-04-22T23:36:00+08:00` |

### 响应样例

```json
{
  "code": "0",
  "message": "success",
  "data": {
    "id": 10002,
    "role": "ADMIN",
    "updatedAt": "2026-04-22T23:36:00+08:00"
  },
  "traceId": "c4d95157647e4f5d",
  "timestamp": "2026-04-22T23:36:00+08:00"
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
  "code": "0",
  "message": "success",
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
  },
  "traceId": "71d8f4d1f8c14914",
  "timestamp": "2026-04-22T23:40:00+08:00"
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
  "code": "0",
  "message": "success",
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
  },
  "traceId": "2435069b7f8d44f8",
  "timestamp": "2026-04-22T23:42:00+08:00"
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
| `status` | `String` | 是 | 创建时状态，建议 `DRAFT` 或 `PUBLISHED` | `DRAFT` |
| `allowComment` | `Boolean` | 否 | 是否允许评论，预留字段 | `true` |

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
| `publishedAt` | `String` | 发布时间，草稿可为空 | `null` |
| `createdAt` | `String` | 创建时间 | `2026-04-22T23:45:00+08:00` |

### 响应样例

```json
{
  "code": "0",
  "message": "success",
  "data": {
    "id": 40001,
    "status": "DRAFT",
    "publishedAt": null,
    "createdAt": "2026-04-22T23:45:00+08:00"
  },
  "traceId": "a7322db35d784703",
  "timestamp": "2026-04-22T23:45:00+08:00"
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
| `status` | `String` | 是 | 编辑后的状态 | `PUBLISHED` |
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
| `publishedAt` | `String` | 发布时间 | `2026-04-22T23:50:00+08:00` |
| `updatedAt` | `String` | 更新时间 | `2026-04-22T23:50:00+08:00` |

### 响应样例

```json
{
  "code": "0",
  "message": "success",
  "data": {
    "id": 40001,
    "status": "PUBLISHED",
    "publishedAt": "2026-04-22T23:50:00+08:00",
    "updatedAt": "2026-04-22T23:50:00+08:00"
  },
  "traceId": "37b6f479d04a4951",
  "timestamp": "2026-04-22T23:50:00+08:00"
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
| `status` | `String` | 是 | 目标状态，建议用于 `PUBLISHED` 或 `OFFLINE` | `OFFLINE` |

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
  "code": "0",
  "message": "success",
  "data": {
    "id": 40001,
    "status": "OFFLINE",
    "updatedAt": "2026-04-22T23:55:00+08:00"
  },
  "traceId": "701fd994505645cc",
  "timestamp": "2026-04-22T23:55:00+08:00"
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

#### data 字段说明

| 字段名称 | 字段类型 | 字段解释 | 业务例子 |
| --- | --- | --- | --- |
| `success` | `Boolean` | 是否删除成功 | `true` |

### 响应样例

```json
{
  "code": "0",
  "message": "success",
  "data": {
    "success": true
  },
  "traceId": "9e723234f738461b",
  "timestamp": "2026-04-22T23:58:00+08:00"
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

### 请求样例

```http
GET /api/v1/admin/categories?status=ENABLED
Authorization: Bearer eyJhbGciOiJIUzI1NiJ9.admin
```

### 响应参数

#### data 字段说明

默认返回分类树，一级分类下包含二级分类列表。

| 字段名称 | 字段类型 | 字段解释 | 业务例子 |
| --- | --- | --- | --- |
| `id` | `Long` | 分类 ID | `20001` |
| `parentId` | `Long` | 父分类 ID，一级分类为空 | `NULL`、`20001` |
| `level` | `Integer` | 分类层级 | `1`、`2` |
| `name` | `String` | 分类名称 | `技术`、`Java` |
| `description` | `String` | 分类描述 | `技术内容一级分类` |
| `sortNo` | `Integer` | 排序值 | `10` |
| `status` | `String` | 分类状态 | `ENABLED` |
| `articleCount` | `Integer` | 关联文章数。一级分类为其下全部二级分类汇总，二级分类为自身文章数 | `18` |
| `createdAt` | `String` | 创建时间 | `2026-04-22T21:10:00+08:00` |
| `children` | `Array<Object>` | 二级分类列表，仅一级分类节点返回 | `[{"id":21001,"name":"Java"}]` |

### 响应样例

```json
{
  "code": "0",
  "message": "success",
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
          "createdAt": "2026-04-22T21:20:00+08:00"
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
          "createdAt": "2026-04-22T21:21:00+08:00"
        }
      ]
    }
  ],
  "traceId": "145a6f41eb434612",
  "timestamp": "2026-04-23T00:00:00+08:00"
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
| `sortNo` | `Integer` | 否 | 排序值，默认 `0` | `10` |
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
  "code": "0",
  "message": "success",
  "data": {
    "id": 21001,
    "parentId": 20001,
    "level": 2,
    "name": "Java",
    "status": "ENABLED",
    "createdAt": "2026-04-23T00:05:00+08:00"
  },
  "traceId": "8d2eb9cb1ef249bb",
  "timestamp": "2026-04-23T00:05:00+08:00"
}
```

## 9.3 更新分类

- 路由：`PUT`
- 路径：`/api/v1/admin/categories/{categoryId}`
- 权限：`ADMIN`

### 请求参数

#### Path 参数

| 字段名称 | 字段类型 | 必填 | 字段解释 | 业务例子 |
| --- | --- | --- | --- | --- |
| `categoryId` | `Long` | 是 | 分类 ID | `21001` |

#### Body 参数

| 字段名称 | 字段类型 | 必填 | 字段解释 | 业务例子 |
| --- | --- | --- | --- | --- |
| `parentId` | `Long` | 否 | 父分类 ID。一级分类为空，二级分类必须指向一级分类 | `NULL`、`20001` |
| `level` | `Integer` | 是 | 分类层级 | `2` |
| `name` | `String` | 是 | 分类名称 | `Java` |
| `description` | `String` | 否 | 分类描述 | `Java 相关文章` |
| `sortNo` | `Integer` | 否 | 排序值 | `5` |
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
  "code": "0",
  "message": "success",
  "data": {
    "id": 21001,
    "parentId": 20001,
    "level": 2,
    "name": "Java",
    "status": "ENABLED",
    "updatedAt": "2026-04-23T00:06:00+08:00"
  },
  "traceId": "d18c5d9b33464136",
  "timestamp": "2026-04-23T00:06:00+08:00"
}
```

## 9.4 删除分类

- 路由：`DELETE`
- 路径：`/api/v1/admin/categories/{categoryId}`
- 权限：`ADMIN`

说明：

- 删除一级分类时，需要保证该一级分类本身及其下所有二级分类都没有关联文章
- 删除二级分类时，需要保证该二级分类下没有关联文章

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

#### data 字段说明

| 字段名称 | 字段类型 | 字段解释 | 业务例子 |
| --- | --- | --- | --- |
| `success` | `Boolean` | 是否删除成功 | `true` |

### 响应样例

```json
{
  "code": "0",
  "message": "success",
  "data": {
    "success": true
  },
  "traceId": "e793d93438b9498a",
  "timestamp": "2026-04-23T00:08:00+08:00"
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
| `articleCount` | `Integer` | 关联文章数 | `5` |
| `createdAt` | `String` | 创建时间 | `2026-04-22T21:15:00+08:00` |

### 响应样例

```json
{
  "code": "0",
  "message": "success",
  "data": [
    {
      "id": 30001,
      "name": "JWT",
      "description": "和认证授权相关的文章标签",
      "status": "ENABLED",
      "articleCount": 5,
      "createdAt": "2026-04-22T21:15:00+08:00"
    }
  ],
  "traceId": "841bd3a5e09d4ad8",
  "timestamp": "2026-04-23T00:10:00+08:00"
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
  "code": "0",
  "message": "success",
  "data": {
    "id": 30001,
    "name": "JWT",
    "status": "ENABLED",
    "createdAt": "2026-04-23T00:12:00+08:00"
  },
  "traceId": "af9c44268fdc4fbe",
  "timestamp": "2026-04-23T00:12:00+08:00"
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
  "code": "0",
  "message": "success",
  "data": {
    "id": 30001,
    "name": "JWT",
    "status": "ENABLED",
    "updatedAt": "2026-04-23T00:13:00+08:00"
  },
  "traceId": "e12e3e0de63c4387",
  "timestamp": "2026-04-23T00:13:00+08:00"
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

#### data 字段说明

| 字段名称 | 字段类型 | 字段解释 | 业务例子 |
| --- | --- | --- | --- |
| `success` | `Boolean` | 是否删除成功 | `true` |

### 响应样例

```json
{
  "code": "0",
  "message": "success",
  "data": {
    "success": true
  },
  "traceId": "7cf3bfdab7ef4d53",
  "timestamp": "2026-04-23T00:14:00+08:00"
}
```

## 11. 业务规则补充

### 11.1 用户相关

- 被禁用用户不可登录
- 被禁用用户已有 Access Token 可自然过期
- 被禁用用户在刷新 Token 时必须失败，返回 `A005`
- 用户修改用户名时必须重新校验唯一性

### 11.2 文章相关

- 前台文章列表和详情仅返回 `PUBLISHED` 状态文章
- 草稿和下线文章仅后台可见
- 文章创建或更新为 `PUBLISHED` 时，后端应自动写入 `publishedAt`
- 文章必须绑定二级分类，不能直接绑定一级分类
- 文章保存或更新时，后端应以 `contentMd` 为源自动生成并持久化 `contentHtml`
- 文章保存或更新时，后端应以 `contentMd` 为源自动提取并持久化 `contentText`
- 前台文章详情接口优先返回 `contentHtml` 用于页面渲染
- 文章从 `PUBLISHED` 修改为 `OFFLINE` 后，前台立即不可见

### 11.3 分类和标签相关

- 删除一级分类前需要校验其自身及其下所有二级分类是否存在关联文章
- 删除二级分类前需要校验该二级分类是否存在关联文章
- 删除标签时可由后端同步清理 `article_tag` 关联关系
- 前台分类接口默认返回一级分类树，二级分类挂载在 `children` 字段
- 前台主导航不展示标签，标签主要用于筛选和后续搜索

## 12. 后续版本预留接口

以下接口不在 P0 范围内，仅做路由预留说明：

| 模块 | 路由 | 路径 | 版本规划 |
| --- | --- | --- | --- |
| 评论 | `GET` | `/api/v1/articles/{articleId}/comments` | P1 |
| 评论 | `POST` | `/api/v1/articles/{articleId}/comments` | P1 |
| 点赞 | `POST` | `/api/v1/articles/{articleId}/like` | P2 |
| 取消点赞 | `DELETE` | `/api/v1/articles/{articleId}/like` | P2 |
| 收藏 | `POST` | `/api/v1/articles/{articleId}/favorite` | P2 |
| 取消收藏 | `DELETE` | `/api/v1/articles/{articleId}/favorite` | P2 |
| 留言 | `GET` | `/api/v1/messages` | P1 |
| 留言 | `POST` | `/api/v1/messages` | P1 |
| 项目 | `GET` | `/api/v1/projects` | P1 |
| 项目详情 | `GET` | `/api/v1/projects/{projectId}` | P1 |
