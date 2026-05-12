# 个人技术博客系统部署与运维约定

## 1. 文档信息

- 项目名称：个人技术博客系统
- 文档版本：v1.0
- 文档日期：2026-04-29
- 适用范围：生产环境部署、备份、对象存储、域名与基础运维
- 相关文档：[PRD.md](./PRD.md)、[database-design.md](./database-design.md)

## 2. 总体原则

- 生产环境默认使用 `Docker Compose` 编排服务。
- 数据库、缓存等有状态服务必须使用持久化卷或外部托管服务，不能依赖容器可写层保存数据。
- 数据库必须建立定期备份机制，备份文件应存放在服务器本机以外的位置。
- 头像、文章封面、后续上传文件等二进制资源应使用对象存储，不直接依赖应用服务器本地磁盘。
- 域名解析 TTL 不要设得太长，迁移时好切换。
- 生产密钥、数据库密码、对象存储密钥等敏感配置必须通过环境变量或密钥管理方式注入，不提交到代码仓库。

## 3. 推荐部署拓扑

P0 阶段推荐的单机部署形态：

```text
Docker Compose
  ├─ nginx: HTTPS, static files, reverse proxy
  ├─ backend: Spring Boot API
  ├─ postgres: PostgreSQL
  └─ redis: Redis

External services
  ├─ object storage: avatars, article covers, uploaded files
  └─ backup storage: database backup archives
```

说明：

- `Nginx` 可以写入 `Docker Compose`，统一管理 HTTPS、静态资源和后端反向代理。
- 前端构建产物由 `Nginx` 托管。
- 后端只暴露给反向代理，不建议直接暴露应用端口到公网。
- PostgreSQL 和 Redis 与应用部署在同一台服务器即可满足当前个人博客使用场景。

## 4. Docker Compose 约定

- Compose 文件应明确容器名称、端口、网络、持久化卷、环境变量和重启策略。
- `Nginx` 可以作为 Compose 服务之一，也可以由宿主机直接安装；本项目优先推荐纳入 Compose 统一管理。
- PostgreSQL 数据目录必须挂载到 volume。
- Redis 如果用于 Refresh Token 或缓存，也必须配置持久化或明确可丢失策略。
- 生产环境数据库密码、JWT 密钥、Redis 密码、对象存储密钥不能直接写在 `docker-compose.yaml` 中。
- 建议使用 `.env` 或服务器侧环境变量注入配置。

建议保留的关键配置项：

```text
POSTGRES_DB
POSTGRES_USER
POSTGRES_PASSWORD
REDIS_PASSWORD
JWT_SECRET
OBJECT_STORAGE_ENDPOINT
OBJECT_STORAGE_BUCKET
OBJECT_STORAGE_ACCESS_KEY
OBJECT_STORAGE_SECRET_KEY
```

## 5. 数据库备份约定

生产环境必须对 PostgreSQL 做定期备份。

建议策略：

- 每日至少备份一次。
- 备份文件命名包含环境、数据库名和时间戳。
- 备份文件应上传到对象存储或其他远端存储。
- 本机可保留最近若干份备份，但不能只保留本机备份。
- 至少在重要版本发布前手动备份一次。
- 定期做恢复演练，确认备份文件可用。

推荐备份命名示例：

```text
blog_db_prod_20260429_020000.dump
```

推荐恢复演练频率：

- P0 阶段：每次重要发布前至少手动验证一次。
- 稳定运行后：至少每季度验证一次。

## 6. 文件与对象存储约定

P0 不提供本地图片上传服务，但生产设计默认面向对象存储。

适合放入对象存储的内容：

- 用户头像
- 文章封面
- 文章内图片
- 项目封面
- 后续上传附件
- 数据库备份归档

数据库中只保存 URL 或对象 key，不保存二进制文件本体。

建议后续上传能力落地时遵守：

- 后端负责鉴权、文件类型校验和对象 key 生成。
- 前端不直接保存对象存储密钥。
- 公共可读资源可使用 CDN 加速。
- 私有资源通过签名 URL 或后端代理访问。

## 7. 域名与 HTTPS

- 生产环境必须启用 HTTPS。
- `Nginx` 负责证书终止和反向代理。
- 前端静态资源与后端 API 按同域部署设计；生产环境由 `Nginx` 托管前端产物，并将 `/api/**` 反向代理到后端服务。
- Refresh Token 由后端通过 `Set-Cookie` 写入 `refresh_token` HttpOnly Cookie，`Nginx` 不应丢弃或改写该响应头。
- `refresh_token` Cookie 路径固定为 `/api/v1/auth`，只随认证相关接口发送；生产 HTTPS 环境必须带 `Secure`，并保持 `HttpOnly`、`SameSite=Lax`。
- 若后端需要根据请求协议决定是否添加 `Secure`，反向代理应正确传递 `X-Forwarded-Proto` 等代理头。
- 当前设计不依赖跨域 CORS；若未来改成前后端跨站点部署，需要重新评估 `SameSite=None; Secure`、携带凭证的 CORS 配置和 CSRF 防护策略。
- 域名解析 TTL 不要设得太长，推荐先使用 `300s` 到 `600s`，迁移稳定后可按需调高。
- 迁移服务器、切换负载均衡或更换 CDN 前，提前降低 TTL。

Refresh Token Cookie 的 `Secure` 属性通过后端配置区分环境：

```yaml
blog:
  auth:
    refresh-cookie:
      secure: true
```

- 开发环境如果使用本地 HTTP，可配置为 `false`，避免浏览器因 `Secure` 拒绝发送 Cookie。
- 生产环境启用 HTTPS 后必须配置为 `true`，确保 `refresh_token` 只随 HTTPS 请求发送。

## 8. 日志与监控

P0 阶段至少需要关注：

- 服务器 CPU、内存、磁盘占用
- Docker 容器运行状态
- PostgreSQL 连接数和磁盘占用
- Redis 内存占用
- Nginx 访问日志和错误日志
- 后端应用错误日志

建议告警阈值：

- 磁盘使用率超过 `80%`
- 内存使用率长期超过 `80%`
- CPU 使用率长期超过 `70%`
- 数据库备份任务失败
- 后端容器反复重启

## 9. 发布前检查清单

- 已完成数据库备份。
- Flyway migration 可正常执行。
- Docker Compose 配置未写死生产敏感信息。
- 对象存储 bucket、权限和访问域名已配置。
- Nginx HTTPS 证书有效。
- 域名解析 TTL 适合本次发布或迁移。
- 后端健康检查、前端静态资源、公开文章页面和登录接口可访问。
- 发布后检查应用日志、Nginx 日志和容器状态。
