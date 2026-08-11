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
- PostgreSQL 使用根目录 `Dockerfile` 构建包含 `zhparser` 的镜像，并将 `backend/scripts/db/bootstrap/init.sql` 挂载到 `/docker-entrypoint-initdb.d/00_init.sql`。
- `/docker-entrypoint-initdb.d` 脚本只会在数据卷首次初始化时执行；业务库和测试库仍由 Flyway migration 分别创建 `zhparser` 扩展、文本搜索配置及全文索引。
- Redis 如果用于 Refresh Token 或缓存，也必须配置持久化或明确可丢失策略。
- 生产环境数据库密码、JWT 密钥、Redis 密码、对象存储密钥不能直接写在 `docker-compose.yaml` 中。
- 建议使用 `.env` 或服务器侧环境变量注入配置。

建议保留的关键配置项：

```text
BLOG_DB_URL
BLOG_DB_USERNAME
BLOG_DB_PASSWORD
BLOG_JWT_SECRET
BLOG_TEST_DB_URL
BLOG_TEST_DB_USERNAME
BLOG_TEST_DB_PASSWORD
BLOG_TEST_JWT_SECRET
POSTGRES_DB
POSTGRES_USER
POSTGRES_PASSWORD
REDIS_PASSWORD
JWT_SECRET
BLOG_OBJECT_STORAGE_PROVIDER
BLOG_OBJECT_STORAGE_ENDPOINT
BLOG_OBJECT_STORAGE_BUCKET
BLOG_OBJECT_STORAGE_ACCESS_KEY_ID
BLOG_OBJECT_STORAGE_ACCESS_KEY_SECRET
BLOG_OBJECT_STORAGE_PUBLIC_BASE_URL
```

Spring Boot 配置按运行场景区分：

- IDEA 本地启动后端时使用 `dev` profile，通过 IDEA Run Configuration 注入 `BLOG_DB_*` 和 `BLOG_JWT_SECRET`
- 自动化集成测试使用 `test` profile，通过 JUnit 或 Maven Run Configuration 注入 `BLOG_TEST_DB_*` 和 `BLOG_TEST_JWT_SECRET`
- 生产容器使用 `prod` profile，通过 Compose 将服务器环境变量或根目录 `.env` 中的生产值注入 backend 容器
- `application-test.yaml` 只放在 `src/test/resources`，不会打包进生产应用
- `dev`、`test` 使用本地 HTTP 时 Refresh Token Cookie 的 `Secure` 为 `false`；`prod` 必须为 `true`
- 测试数据库必须独立于开发数据库，避免测试运行 Flyway 或写入测试数据时修改开发数据

环境文件的职责需要区分：

- `.env.example` 是提交到仓库的变量清单，保留 `BLOG_TEST_*`，方便新环境知道自动化测试需要哪些变量。
- `.env` 是本机或服务器的真实值，不会被提交。它是否包含 `BLOG_TEST_*` 取决于是否用它来运行集成测试；生产 Compose 不需要这些测试变量。
- Spring Boot 和 Maven 不会自动读取项目根目录的 `.env`。IDEA Run Configuration、Maven/CI 环境或 Compose 必须显式注入变量。
- `application-test.yaml` 仍然需要保留。它把测试 profile 的数据库和 JWT 配置切换到 `BLOG_TEST_*`，从配置层面阻止集成测试误连开发库；`@ActiveProfiles("test")` 负责选择这个文件。

仓库根目录只提交 `.env.example` 作为变量清单，不提交真实 `.env`。Compose 仅在部署 backend 容器时需要设置 `SPRING_PROFILES_ACTIVE=prod`；如果本地只用 Compose 启动 PostgreSQL、后端仍由 IDEA 启动，则后端继续使用 `dev` profile。

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

P1 图片上传使用阿里云 OSS，约定如下：

- 使用公共读 Bucket 和独立的 HTTPS 图片域名，匿名用户只能读取对象，不能写入或列举 Bucket。
- 使用 RAM 子账号并限制到指定 Bucket，禁止使用阿里云主账号 AccessKey。
- 浏览器把图片上传给后端，后端完成权限、类型、大小校验后中转到 OSS；前端不持有 OSS 凭证。
- 对象 key 由后端按业务场景、年月和 UUID 生成，不使用原始文件名，也不覆盖已有对象。
- P1 不自动删除被替换或失去引用的对象；后续确有统一资源管理需求时再增加资源表和异步清理任务。

本地和生产环境需要注入：

```text
BLOG_OBJECT_STORAGE_PROVIDER=aliyun
BLOG_OBJECT_STORAGE_ENDPOINT=https://oss-<region>.aliyuncs.com
BLOG_OBJECT_STORAGE_BUCKET=<bucket-name>
BLOG_OBJECT_STORAGE_ACCESS_KEY_ID=<ram-access-key-id>
BLOG_OBJECT_STORAGE_ACCESS_KEY_SECRET=<ram-access-key-secret>
BLOG_OBJECT_STORAGE_PUBLIC_BASE_URL=https://img.example.com
```

`BLOG_OBJECT_STORAGE_ENDPOINT` 必须包含 `https://` 协议；公开域名生产环境必须使用 HTTPS，避免前端页面因混合内容阻止图片加载。

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
