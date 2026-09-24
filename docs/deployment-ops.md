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
- PostgreSQL 使用 `docker/postgres/Dockerfile` 构建包含 `zhparser` 二进制扩展的镜像，并将 `backend/scripts/db/bootstrap/init.sql` 挂载到 `/docker-entrypoint-initdb.d/00_init.sql`。
- `init.sql` 只保留启动说明，不创建数据库对象；业务库和测试库统一由 Flyway migration 分别创建 `zhparser` 扩展、文本搜索配置及全文索引，避免备份恢复时与预创建的 `zhparser_cfg` 冲突。
- Redis 如果用于 Refresh Token 或缓存，也必须配置持久化或明确可丢失策略。
- 生产环境数据库密码、JWT 密钥、Redis 密码、对象存储密钥不能直接写在 `docker-compose.yaml` 中。
- 建议使用 `.env` 或服务器侧环境变量注入配置。
- 通过 Compose `environment:` 注入的值通常可被有 Docker 管理权限的用户通过
  `docker inspect <container>` 查看；写入 `command:` 的值还可能出现在容器命令行元数据中。
- 本地 WSL 演练可使用仓库外、权限受限的 `.env.prodtest`；正式生产应优先使用 Docker
  Secrets 或云厂商密钥管理服务。应用目前读取 `BLOG_*` 环境变量，切换到文件型 Secret
  前需要增加 `/run/secrets/*` 到配置的适配层，不能只修改 Compose 文件。

生产式 Compose 的 Docker 日志使用有限大小的轮转，避免容器日志无限增长：

```yaml
logging:
  driver: json-file
  options:
    max-size: 10m
    max-file: "5"
```

当前 `compose.prod.yaml` 对 PostgreSQL、Redis、Nginx 使用 `10m × 5`，backend 使用
`20m × 5`。轮转只限制 Docker 日志文件，不替代应用错误日志、Nginx 访问日志或数据库备份；
发布后应通过 `docker compose logs` 和宿主机磁盘监控确认配置生效。

镜像和源码依赖应在正式发布前固定到可验证的不可变版本。基础镜像使用
`image:tag@sha256:<digest>`，zhparser 源码使用固定 Git commit，而不是默认拉取分支最新提交。
digest 和 commit 必须从实际构建环境取得并记录在发布记录中，不能手工猜测：

```bash
docker buildx imagetools inspect postgres:16.13-bookworm
docker buildx imagetools inspect redis:7.4-alpine
docker buildx imagetools inspect nginx:1.29-alpine
docker buildx imagetools inspect maven:3.9.11-eclipse-temurin-21
docker buildx imagetools inspect eclipse-temurin:21-jre
docker buildx imagetools inspect node:22-alpine
git ls-remote https://github.com/amutu/zhparser.git HEAD
```

拿到结果后，把 Dockerfile 中的 `FROM` 和 zhparser checkout 改为对应 digest/commit，并在
发布记录中保留镜像名称、digest、zhparser commit、构建时间和源码 commit SHA。

当前生产式部署演练已锁定以下版本：

| 构建依赖 | 锁定值 |
| --- | --- |
| PostgreSQL 基础镜像 | `postgres:16.13-bookworm@sha256:472efd9a66f2b2f1a5aeb18b28de74332e6ef88c2b93a1a5d812fb6db67a5f60` |
| Redis 运行时镜像 | `redis:7.4-alpine@sha256:858f009f9709ce576febc734aa78b8f6d624b82571f9ddb6bda4377c833b3499` |
| Maven 构建镜像 | `maven:3.9.11-eclipse-temurin-21@sha256:6fdc855a6ed81d288ca7ca37ac6ff5e9308b612485c0801d70b25a858c83d237` |
| Java 运行时镜像 | `eclipse-temurin:21-jre@sha256:49e21e16e3c86eb7816a44a67549910ed090fbeb40c29c525d58bf5e02e91b0f` |
| Node 构建镜像 | `node:22-alpine@sha256:b6f26b36c8ff49624cfdac716b8ea1138d606df02586a77d364bb5536a634f85` |
| Nginx 运行时镜像 | `nginx:1.29-alpine@sha256:5616878291a2eed594aee8db4dade5878cf7edcb475e59193904b198d9b830de` |
| zhparser 源码 commit | `2e995c4df672563992b4d7a147b8fa2d0d4cda6c` |

这些值只代表当前演练所验证的构建输入。升级任一镜像、zhparser 或源码依赖时，应重新构建、
运行测试并更新本表；不要只修改 tag 而保留旧 digest。当前 SCWS 源码包仍由版本号 URL
下载，若未来需要更强的完全可复现构建，还应额外记录该 tarball 的 SHA-256。

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
BLOG_REDIS_HOST
BLOG_REDIS_PORT
BLOG_REDIS_PASSWORD
BLOG_REDIS_DATABASE
BLOG_TEST_REDIS_DATABASE
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

## 7. 回复通知邮件配置

P1 留言回复通知和 P2 评论直接回复通知复用同一套 SMTP 配置。生产环境启用邮件通知时需要注入：

```text
BLOG_MAIL_ENABLED=true
BLOG_MAIL_FROM=<发件邮箱>
BLOG_MAIL_FRONTEND_BASE_URL=https://blog.example.com
BLOG_SMTP_HOST=<SMTP 服务地址>
BLOG_SMTP_PORT=587
BLOG_SMTP_USERNAME=<SMTP 用户名>
BLOG_SMTP_PASSWORD=<SMTP 授权码或密码>
BLOG_SMTP_AUTH=true
BLOG_SMTP_STARTTLS=true
BLOG_SMTP_CONNECTION_TIMEOUT=5000
BLOG_SMTP_READ_TIMEOUT=10000
BLOG_SMTP_WRITE_TIMEOUT=10000
```

- `BLOG_MAIL_FROM` 应使用 SMTP 服务允许的发件地址，通常与 SMTP 账号一致。
- 生产环境的 `BLOG_MAIL_FRONTEND_BASE_URL` 必须使用站点 HTTPS 地址，用于生成留言页和退订链接。
- 密码或授权码只通过生产环境变量或密钥管理服务提供，不写入 Git、镜像或日志。
- 生产环境默认启用邮件通知；配置不完整时后端拒绝启动，避免回复已创建但通知长期静默失败。
- 邮件功能已启用但运行时无法取得邮件发送器时，后端记录包含留言 ID、回复 ID 和失败原因的结构化错误日志，不记录收件邮箱或留言正文，也不回滚已经提交的管理员回复。
- `BLOG_SMTP_CONNECTION_TIMEOUT`、`BLOG_SMTP_READ_TIMEOUT`、`BLOG_SMTP_WRITE_TIMEOUT` 的单位均为毫秒，默认分别为 5000、10000、10000，避免网络异常时邮件线程长期阻塞而无法进入重试。
- 开发环境默认关闭邮件；本地需要实际发信时，通过环境变量显式设置 `BLOG_MAIL_ENABLED=true` 并提供完整 SMTP 配置。
- 生产环境启用邮件时，`BLOG_MAIL_FRONTEND_BASE_URL` 必须显式配置为完整 HTTPS 地址；缺失、使用 HTTP 或地址格式不合法时后端拒绝启动。
- P1 留言通知以纯文本发送；P2 将留言与评论通知统一为 `multipart/alternative`，同时携带 `text/plain` 与 `text/html`。
- HTML 邮件使用内联样式和兼容邮件客户端的简单布局，并为不支持 HTML 的客户端保留语义一致的纯文本版本。
- 模板必须对昵称、文章标题、留言、评论和回复内容执行 HTML 转义；不得包含 JavaScript、表单或依赖外部 CSS。

## 8. 域名与 HTTPS

- 生产环境必须启用 HTTPS。
- `Nginx` 负责证书终止和反向代理。
- 前端静态资源与后端 API 按同域部署设计；生产环境由 `Nginx` 托管前端产物，并将 `/api/**` 反向代理到后端服务。
- Refresh Token 由后端通过 `Set-Cookie` 写入 `refresh_token` HttpOnly Cookie，`Nginx` 不应丢弃或改写该响应头。
- `refresh_token` Cookie 路径固定为 `/api/v1/auth`，只随认证相关接口发送；生产 HTTPS 环境必须带 `Secure`，并保持 `HttpOnly`、`SameSite=Lax`。
- 若后端需要根据请求协议决定是否添加 `Secure`，反向代理应正确传递 `X-Forwarded-Proto` 等代理头。
- `Nginx` 应对 `POST /api/v1/messages` 等公开写接口配置独立的 `limit_req` 限流规则，在请求进入后端前拦截明显的高频访问；应用层仍需使用 Redis 完成业务身份维度的第二层限流。
- `Nginx` 必须覆盖并重新生成 `X-Real-IP`、`X-Forwarded-For` 等客户端地址请求头，后端只信任由受控反向代理写入的代理头，不能直接信任公网请求自行携带的同名头。
- 当前设计不依赖跨域 CORS；若未来改成前后端跨站点部署，需要重新评估 `SameSite=None; Secure`、携带凭证的 CORS 配置和 CSRF 防护策略。
- 域名解析 TTL 不要设得太长，推荐先使用 `300s` 到 `600s`，迁移稳定后可按需调高。
- 迁移服务器、切换负载均衡或更换 CDN 前，提前降低 TTL。

生产环境使用标准的宿主机端口 `80 -> 80`、`443 -> 443` 时，HTTP 重定向应为
`https://$host$request_uri`。本机 WSL 演练若使用 Compose 的 `8088:80`、`8443:443`，
该重定向会默认丢失 `8443`，浏览器会跳到 `https://localhost` 的 443 端口。应使用单独的
prodtest Nginx 配置，将重定向写成 `https://$host:8443$request_uri`，并只在演练 Compose
中挂载它；正式生产继续使用不带端口的配置。另一种简单方案是确保宿主机可绑定 80/443，
这样演练也使用标准端口，不需要修改重定向逻辑。

Refresh Token Cookie 的 `Secure` 属性通过后端配置区分环境：

```yaml
blog:
  auth:
    refresh-cookie:
      secure: true
```

- 开发环境如果使用本地 HTTP，可配置为 `false`，避免浏览器因 `Secure` 拒绝发送 Cookie。
- 生产环境启用 HTTPS 后必须配置为 `true`，确保 `refresh_token` 只随 HTTPS 请求发送。

生产式 Compose 使用 Spring Boot Actuator 的 `/actuator/health/readiness` 作为 backend 健康检查；该端点只返回健康状态，不展示数据库、Redis 或其他内部细节。Nginx 使用独立的 `/nginx-health` 端点检查自身是否已正常提供服务，并等待 backend 通过 readiness 后再启动流量代理。

生产环境管理员初始化不应依赖 devdata migration，也不应在镜像或仓库中保存固定的
`admin/123456`。推荐提供一次性 bootstrap 命令或独立 Compose profile：从受保护的环境变量或
Secret 文件读取管理员用户名、邮箱和密码，使用后端现有密码哈希逻辑创建管理员；若管理员已
存在则失败并停止，缺少必要变量也必须失败，密码不得写入日志。bootstrap 成功后删除临时
Secret，普通 `prod` 启动不再自动创建或重置管理员。该流程需要先在后端实现一次性命令，
不能直接把一条永久 SQL 或默认密码当作生产初始化方案。

## 9. 日志与监控

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

## 10. 发布前检查清单

- 已完成数据库备份。
- Flyway migration 可正常执行。
- Docker Compose 配置未写死生产敏感信息。
- 对象存储 bucket、权限和访问域名已配置。
- 若启用回复通知，SMTP 凭证、超时、发件地址和前端 HTTPS 地址已配置；P1 验证留言纯文本邮件，P2 升级后再分别验证 `text/plain` 与 `text/html`。
- Nginx HTTPS 证书有效。
- 公开写接口已配置 Nginx 请求限流，并确认后端取得的客户端地址来自受控反向代理。
- 域名解析 TTL 适合本次发布或迁移。
- 后端健康检查、前端静态资源、公开文章页面和登录接口可访问。
- 发布后检查应用日志、Nginx 日志和容器状态。
