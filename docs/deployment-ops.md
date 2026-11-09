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
- 生产密钥、数据库密码、对象存储密钥等敏感配置保存在仓库外的 `.env.prod`，通过 Compose 显式注入容器环境变量，不提交到代码仓库。

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
- 正式 Compose 默认项目名为 `blog-prod`；本次计划发布版本为 `v1.0.0`，定制 PostgreSQL、后端和前端 Nginx 镜像分别为 `blog-postgres:v1.0.0`、`blog-backend:v1.0.0`、`blog-nginx:v1.0.0`，通过 `BLOG_POSTGRES_IMAGE`、`BLOG_BACKEND_IMAGE`、`BLOG_NGINX_IMAGE` 覆盖。bootstrap 与后端使用同一个 `BLOG_BACKEND_IMAGE`。版本正式发布后不覆盖对应标签，后续发布使用新版本或 Commit 标签；Redis、入口 Nginx 和各 Dockerfile 基础镜像继续使用已锁定的 digest。镜像标签不等于 Git 发布标签，也不能代替部署前的构建或拉取；只执行 up 可能继续使用本机同名旧镜像。
- 已使用旧默认项目名 `blog-prodtest` 创建容器的部署，更新时显式保留 `-p blog-prodtest`；不要仅因改名而创建新项目并误以为数据丢失。`prodtest_pg_data`、`prodtest_redis_data` 是历史卷逻辑名，本次保持不变，不移动或删除数据。
- `Nginx` 可以作为 Compose 服务之一，也可以由宿主机直接安装；本项目优先推荐纳入 Compose 统一管理。
- PostgreSQL 数据目录必须挂载到 volume。
- PostgreSQL 使用 `docker/postgres/Dockerfile` 构建包含 `zhparser` 二进制扩展的镜像，并将 `backend/scripts/db/bootstrap/init.sql` 挂载到 `/docker-entrypoint-initdb.d/00_init.sql`。
- `init.sql` 只保留启动说明，不创建数据库对象；业务库和测试库统一由 Flyway migration 分别创建 `zhparser` 扩展、文本搜索配置及全文索引，避免备份恢复时与预创建的 `zhparser_cfg` 冲突。
- Redis 如果用于 Refresh Token 或缓存，也必须配置持久化或明确可丢失策略。
- 生产环境数据库密码、JWT 密钥、Redis 密码、对象存储密钥不能直接写在 `docker-compose.yaml` 中。
- 正式部署以根目录 `.env.prod.example` 为唯一 Compose 输入模板，复制为服务器仓库外的 `.env.prod`；根目录 `.env.example` 用于本地开发与通用变量参考，`performance/p2/acceptance.env.example` 用于隔离验收，两者不代替正式模板。`POSTGRES_ADMIN_USERNAME/PASSWORD` 仅用于数据库管理及一次性 `db-prepare`，`POSTGRES_APP_USERNAME/PASSWORD` 用于受限业务连接；PostgreSQL 容器内部仍通过官方的 `POSTGRES_USER/PASSWORD` 接收管理凭据。首次部署、版本更新及恢复流程见 [数据库账号与部署流程](./database-accounts.md)。
- 隐私政策由后端镜像中的 `content/privacy-policy.md` 提供；修改政策必须重新构建并部署 backend，
  应用启动时会校验资源非空并以规范化内容 SHA-256 作为版本。
- 通过 Compose `environment:` 注入的值通常可被有 Docker 管理权限的用户通过
  `docker inspect <container>` 查看；写入 `command:` 的值还可能出现在容器命令行元数据中。
- 正式 `.env.prod` 的父目录权限设为 `700`，文件权限设为 `600`，归部署用户所有；通过 `docker compose --env-file /绝对路径/.env.prod` 使用，不依赖 Spring Boot 自动读取 `.env`。只分享 `config -q` 的校验结果，不公开展开后的 Compose 配置或完整容器 inspect；有 Docker 管理权限的用户仍能读取容器环境变量。首次管理员密码继续使用现有一次性临时文件，初始化后删除，不长期存入 `.env.prod`。

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
POSTGRES_APP_USERNAME
POSTGRES_APP_PASSWORD
POSTGRES_ADMIN_USERNAME
POSTGRES_ADMIN_PASSWORD
BLOG_JWT_SECRET
BLOG_TEST_DB_URL
POSTGRES_TEST_APP_USERNAME
POSTGRES_TEST_APP_PASSWORD
POSTGRES_TEST_ADMIN_USERNAME
POSTGRES_TEST_ADMIN_PASSWORD
BLOG_TEST_JWT_SECRET
BLOG_REDIS_HOST
BLOG_REDIS_PORT
BLOG_REDIS_PASSWORD
BLOG_REDIS_DATABASE
BLOG_EDGE_NETWORK
BLOG_WEB_ALIAS
BLOG_INGRESS_IP
BLOG_TEST_REDIS_DATABASE
POSTGRES_DB
REDIS_PASSWORD
JWT_SECRET
BLOG_OBJECT_STORAGE_PROVIDER
BLOG_OBJECT_STORAGE_ENDPOINT
BLOG_OBJECT_STORAGE_BUCKET
BLOG_OBJECT_STORAGE_ACCESS_KEY_ID
BLOG_OBJECT_STORAGE_ACCESS_KEY_SECRET
BLOG_OBJECT_STORAGE_PUBLIC_BASE_URL
BLOG_TURNSTILE_ENABLED
BLOG_TURNSTILE_SECRET_KEY
BLOG_TURNSTILE_EXPECTED_HOSTNAME
BLOG_TURNSTILE_EXPECTED_ACTION
VITE_TURNSTILE_SITE_KEY
```

Spring Boot 配置按运行场景区分：

- IDEA 本地启动后端时使用 `dev` profile，通过 IDEA Run Configuration 注入 `BLOG_DB_URL`、`POSTGRES_ADMIN_*`、`POSTGRES_APP_*` 和 `BLOG_JWT_SECRET`；启动时 Flyway 用管理连接迁移并自动授权，业务连接只用应用账号
- 自动化集成测试使用 `test` profile，通过 JUnit 或 Maven Run Configuration 注入 `BLOG_TEST_DB_URL`、`POSTGRES_TEST_ADMIN_*`、`POSTGRES_TEST_APP_*` 和 `BLOG_TEST_JWT_SECRET`，同样自动迁移并授权
- 生产容器使用 `prod` profile，通过 Compose 将服务器环境变量或根目录 `.env` 中的生产值注入 backend 容器
- `application-test.yaml` 只放在 `src/test/resources`，不会打包进生产应用
- `dev`、`test` 使用本地 HTTP 时 Refresh Token Cookie 的 `Secure` 为 `false`；`prod` 必须为 `true`
- 测试数据库必须独立于开发数据库，避免测试运行 Flyway 或写入测试数据时修改开发数据

环境文件的职责需要区分：

- `.env.example` 是提交到仓库的变量清单，保留 `BLOG_TEST_*`，方便新环境知道自动化测试需要哪些变量。
- `.env` 是本机或服务器的真实值，不会被提交。它是否包含 `BLOG_TEST_*` 取决于是否用它来运行集成测试；生产 Compose 不需要这些测试变量。
- Spring Boot 和 Maven 不会自动读取项目根目录的 `.env`。IDEA Run Configuration、Maven/CI 环境或 Compose 必须显式注入变量。
- `application-test.yaml` 仍然需要保留。它把测试 profile 的数据库和 JWT 配置切换到 `BLOG_TEST_*`，从配置层面阻止集成测试误连开发库；`@ActiveProfiles("test")` 负责选择这个文件。

仓库根目录提交 `.env.example` 作为本地开发变量参考、`.env.prod.example` 作为正式 Compose 输入模板，不提交真实 `.env` 或 `.env.prod`。Compose 仅在部署 backend 容器时需要设置 `SPRING_PROFILES_ACTIVE=prod`；如果本地只用 Compose 启动 PostgreSQL、后端仍由 IDEA 启动，则后端继续使用 `dev` profile。

## 5. 数据库备份约定

生产环境必须对 PostgreSQL 做定期备份。

备份和恢复使用管理账号；普通 backend 只持有受限账号。`pg_dump` 不包含实例角色，`--no-owner --no-privileges` 恢复后也不保留原授权，搬到新服务器时先运行账号模式，再恢复备份，最后由普通 `up` 自动迁移与授权。完整可复制命令见 [数据库账号与部署流程第 5 节](./database-accounts.md#5-搬到新服务器恢复备份)。

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

下文 `mail.example.com` 和 `no-reply@mail.example.com` 均为示例，部署时替换为自己在 Resend 已验证的发件域名及对应地址；`smtp.resend.com` 为服务商地址，保持不变。

```text
BLOG_MAIL_ENABLED=true
BLOG_MAIL_FROM="青禾边 <no-reply@mail.example.com>"
BLOG_MAIL_FRONTEND_BASE_URL=https://blog.example.com
BLOG_SMTP_HOST=smtp.resend.com
BLOG_SMTP_PORT=587
BLOG_SMTP_USERNAME=resend
BLOG_SMTP_PASSWORD=<Resend API Key>
BLOG_SMTP_AUTH=true
BLOG_SMTP_STARTTLS=true
BLOG_SMTP_CONNECTION_TIMEOUT=5000
BLOG_SMTP_READ_TIMEOUT=10000
BLOG_SMTP_WRITE_TIMEOUT=10000
```

- 当前生产邮件服务使用 Resend SMTP；实际替换后的发件域名必须在 Resend 控制台显示为已验证。
- `BLOG_MAIL_FROM` 使用已验证域名下的发件地址，例如 `no-reply@mail.example.com`，可带显示名称；`.env` 中建议整体加引号。
- Resend SMTP 固定使用 `smtp.resend.com:587`、用户名 `resend`、STARTTLS；`BLOG_SMTP_PASSWORD` 填 Resend API Key，建议只授予发信权限。
- 生产环境的 `BLOG_MAIL_FRONTEND_BASE_URL` 必须使用站点 HTTPS 地址，用于生成留言页和退订链接。
- 密码或授权码保存在服务器受限 `.env.prod` 中，由 Compose 注入生产环境变量，不写入 Git、镜像或日志。
- 生产环境默认启用邮件通知；配置不完整时后端拒绝启动，避免回复已创建但通知长期静默失败。
- 邮件功能已启用但运行时无法取得邮件发送器时，后端记录包含留言 ID、回复 ID 和失败原因的结构化错误日志，不记录收件邮箱或留言正文，也不回滚已经提交的管理员回复。
- `BLOG_SMTP_CONNECTION_TIMEOUT`、`BLOG_SMTP_READ_TIMEOUT`、`BLOG_SMTP_WRITE_TIMEOUT` 的单位均为毫秒，默认分别为 5000、10000、10000，避免网络异常时邮件线程长期阻塞而无法进入重试。
- 开发环境默认关闭邮件；本地需要实际发信时，通过环境变量显式设置 `BLOG_MAIL_ENABLED=true` 并提供完整 SMTP 配置。
- 生产环境启用邮件时，`BLOG_MAIL_FRONTEND_BASE_URL` 必须显式配置为完整 HTTPS 地址；缺失、使用 HTTP 或地址格式不合法时后端拒绝启动。
- 留言与评论通知统一为 `multipart/alternative`，同时携带 `text/plain` 与 `text/html`。
- HTML 邮件使用项目内集中模板、内联样式和兼容邮件客户端的简单布局，并为不支持 HTML 的客户端保留语义一致的纯文本版本；不依赖 Resend 托管模板，保证模板进入 Git、可本地测试和可回滚。
- 回复通知 HTML 使用居中白色卡片、`#5f9d64` 顶部品牌区和同色文字链接；正文以收件人昵称开头，原留言、评论和回复内容使用图二风格的斜纹引用块，欢迎语中的站点名称链接到 `BLOG_MAIL_FRONTEND_BASE_URL` 首页。评论主题为“你在[文章标题]的评论有了新的回复”，留言主题为“你在留言板的评论有了新的回复”。
- 模板必须对昵称、文章标题、留言、评论和回复内容执行 HTML 转义；不得包含 JavaScript、表单或依赖外部 CSS。

## 8. 域名与 HTTPS

- 生产环境必须启用 HTTPS。
- 独立共享 Nginx 容器负责公网 80/443、TLS 和按域名分流；项目 Nginx 通过独立 Docker 入口网络接收内部 HTTP，并负责前端和 API。
- 前端静态资源与后端 API 按同域部署设计；生产环境由 `Nginx` 托管前端产物，并将 `/api/**` 反向代理到后端服务。
- 前端构建产物包含 `public/50x.html`；Nginx 对面向用户的页面请求配置 HTTP 500、502、503、504
  静态兜底页。仅页面代理 location 开启上游错误拦截，`/api/**` 保留 API 错误响应，不返回
  50x HTML。
- 50x 页面不依赖 Vue 或后端，只提供重试和返回首页；静态图片占位后续资源放在 `frontend/public/img/errors/`，通过根路径 URL 加载。
- `VITE_SITE_LAUNCHED_AT` 在生产 `.env.prod` 中配置为带时区的首次正式公开运行时间，并通过 Compose build args 编入 Nginx 前端镜像；该值是公开前端配置，不含凭据。首次上线后保持不变；调整时间需重新构建前端镜像。
- Refresh Token 由后端通过 `Set-Cookie` 写入 `refresh_token` HttpOnly Cookie，`Nginx` 不应丢弃或改写该响应头。
- `refresh_token` Cookie 路径固定为 `/api/v1/auth`，只随认证相关接口发送；生产 HTTPS 环境必须带 `Secure`，并保持 `HttpOnly`、`SameSite=Lax`。
- 若后端需要根据请求协议决定是否添加 `Secure`，反向代理应正确传递 `X-Forwarded-Proto` 等代理头。
- `Nginx` 已对 `POST /api/v1/messages` 配置来源 IP 限流；还应对文章和评论点赞/取消路径配置
  每秒 2 次、突发 10 次，对 `POST /api/v1/auth/login` 配置每秒 1 次、突发 5 次。新增路径的
  边缘限流超限返回 HTTP 429 和通用业务码 `199429`。
- 点赞接口还需由应用层使用 Redis 按 `userId` 或匿名 token 哈希限制每 60 秒最多 30 次点赞/取消操作；登录接口由 Redis 按用户/IP 组合与来源 IP 统计失败次数。若 Nginx 前置 CDN 或负载均衡，必须先配置可信代理的真实客户端地址解析，否则边缘限流会把代理地址当作所有用户的来源。
- `Nginx` 必须覆盖并重新生成 `X-Real-IP`、`X-Forwarded-For` 等客户端地址请求头，后端只信任由受控反向代理写入的代理头，不能直接信任公网请求自行携带的同名头。
- 当前设计不依赖跨域 CORS；若未来改成前后端跨站点部署，需要重新评估 `SameSite=None; Secure`、携带凭证的 CORS 配置和 CSRF 防护策略。
- 域名解析 TTL 不要设得太长，推荐先使用 `300s` 到 `600s`，迁移稳定后可按需调高。
- 迁移服务器、切换负载均衡或更换 CDN 前，提前降低 TTL。

公网端口由 `compose.ingress.yaml` 的共享 Nginx 独占，映射 `80:80`、`443:443`。
`compose.prod.yaml` 中的项目 Nginx 只监听容器内 HTTP 80，不发布宿主机端口，也不再挂载证书。
原 `NGINX_HTTP_PORT`、`NGINX_HTTPS_PORT`、`NGINX_TLS_CERT_DIR` 不再使用。
HTTP 到 HTTPS 的重定向由共享入口完成；内部 HTTP 代理仍向后端传递 `X-Forwarded-Proto: https`，
保持生产 Secure Cookie 与站点 URL 语义。入口与项目均使用 12m 请求体上限。

Refresh Token Cookie 的 `Secure` 属性通过后端配置区分环境：

```yaml
blog:
  auth:
    refresh-cookie:
      secure: true
```

- 开发环境如果使用本地 HTTP，可配置为 `false`，避免浏览器因 `Secure` 拒绝发送 Cookie。
- 生产环境启用 HTTPS 后必须配置为 `true`，确保 `refresh_token` 只随 HTTPS 请求发送。

生产式 Compose 使用 Spring Boot Actuator 的 `/actuator/health/readiness` 作为 backend 健康检查；该端点只返回健康状态，不展示数据库、Redis 或其他内部细节。项目 Nginx 使用内部 HTTP `/nginx-health` 检查自身服务，并等待 backend 通过 readiness 后再启动流量代理。共享入口的健康检查独立于项目状态。

共享入口使用独立项目 `blog-ingress`，管理 `blog-prod-edge`、`blog-staging-edge` 两个网络，分别只接入对应项目的 Nginx。参考子网为 `172.30.10.0/29`、`172.30.11.0/29`，入口地址分别为 `.2`；部署前检查与现有 Docker/VPN/主机路由是否重叠，若改网段需同步项目可信代理地址。项目通过 `BLOG_EDGE_NETWORK`、`BLOG_WEB_ALIAS`、`BLOG_INGRESS_IP` 选择网络、唯一 DNS 别名及可信入口 IP；不信任整个私网或任意客户端提供的地址头。共享入口先删除外来的 `Forwarded` 并覆盖 X-Forwarded-*，项目 Nginx 恢复真实 IP 后重新生成给后端的头。

完整安装、首次签证、续期和启停步骤见 [共享 Nginx 容器操作手册](./shared-nginx-ingress.md)。证书仅挂载到共享入口，挂载整个 `/etc/letsencrypt` 以保留 live/archive 链接；宿主机 Certbot 使用 webroot 验证，续期成功后只 reload 共享入口。生产与 staging 分别签证，先上线 staging 不要求正式域名已准备好。

### 8.1 一次性生产管理员初始化

生产环境不加载 `db/devdata`，管理员通过 `bootstrap-admin` 专用 Spring profile 和
`compose.bootstrap.yaml` 中的 `admin-bootstrap` 一次性任务创建。普通生产部署只读取
`compose.prod.yaml`，不会解析 bootstrap 服务或临时密码文件，也不会创建或重置管理员。
Flyway、镜像和仓库中均不保存固定的 `admin/123456`。

部署者必须确认 `BLOG_BOOTSTRAP_ADMIN_EMAIL` 是自己控制的邮箱。bootstrap 信任此确认，不发送验证码；新管理员写入 `email_verified=true` 和初始化时的 UTC `email_verified_at`，后续修改邮箱仍需验证码。已有管理员不会自动回填；确认历史账号邮箱后单独修正该账号，不能重复 bootstrap 或清空数据库来覆盖它。

先按共享入口手册启动 `blog-ingress`，创建 edge 网络；项目环境文件须提供
`BLOG_EDGE_NETWORK`、`BLOG_WEB_ALIAS`、`BLOG_INGRESS_IP`。首次 bootstrap 不依赖公网证书，
完整项目对外验收前需为对应域名启用共享入口 HTTPS 模板。

先在部署目录外创建临时密码文件。使用 `read -s` 避免密码进入 Shell 历史：

```bash
install -d -m 700 "$HOME/deploy/sanjuu-blog-prodtest/secrets"
umask 077
read -rsp "Bootstrap admin password: " BOOTSTRAP_ADMIN_PASSWORD
printf '%s' "$BOOTSTRAP_ADMIN_PASSWORD" \
  > "$HOME/deploy/sanjuu-blog-prodtest/secrets/bootstrap-admin-password.txt"
unset BOOTSTRAP_ADMIN_PASSWORD
```

在仓库外的 `.env.prodtest` 中配置非敏感身份信息和密码文件的宿主机绝对路径：

```dotenv
BLOG_BOOTSTRAP_ADMIN_USERNAME=prod_admin
BLOG_BOOTSTRAP_ADMIN_NICKNAME=站点管理员
BLOG_BOOTSTRAP_ADMIN_EMAIL=admin@example.com
BLOG_BOOTSTRAP_ADMIN_PASSWORD_PATH=/home/<user>/deploy/sanjuu-blog-prodtest/secrets/bootstrap-admin-password.txt
```

执行前分别校验日常配置和加入 bootstrap overlay 后的合并配置：

```bash
docker compose \
  --env-file "$HOME/deploy/sanjuu-blog-prodtest/.env.prodtest" \
  -f compose.prod.yaml config -q

docker compose \
  --env-file "$HOME/deploy/sanjuu-blog-prodtest/.env.prodtest" \
  -f compose.prod.yaml \
  -f compose.bootstrap.yaml \
  config -q
```

先启动 PostgreSQL，再显式合并 bootstrap overlay 并运行一次初始化任务：

```bash
docker compose \
  --env-file "$HOME/deploy/sanjuu-blog-prodtest/.env.prodtest" \
  -f compose.prod.yaml up -d --wait postgres

docker compose \
  --env-file "$HOME/deploy/sanjuu-blog-prodtest/.env.prodtest" \
  -f compose.prod.yaml \
  -f compose.bootstrap.yaml \
  run --rm --build admin-bootstrap
```

该任务先依赖一次性 `db-prepare` 完成 Flyway 和应用授权；不得添加 `--no-deps`。bootstrap 本身使用受限应用账号且关闭 Flyway。
任务只接受 `prod,bootstrap-admin` profile 组合，执行时会完成与注册接口一致的用户名、邮箱、
密码校验，并使用 BCrypt 保存密码。以下情况会以非零状态退出：配置缺失或不合法、密码文件
不存在或不可读、系统中已存在任意管理员、用户名或邮箱已被占用、数据库写入失败。日志只记录
新管理员 ID 和用户名，不记录邮箱或密码。

`bootstrap_admin_password.txt` 保持宿主机 `600` 权限即可。由于该临时文件
通常以 `root:root` 挂载，而正式 backend 镜像使用 UID `10001` 运行，bootstrap overlay 会让
这个一次性任务以 `root` 读取该文件；这不会改变长期 backend 的非 root 运行用户，也不会把
密码写入环境变量或日志。

看到 `ADMIN_BOOTSTRAP_SUCCESS` 后立即删除密码文件：

```bash
rm "$HOME/deploy/sanjuu-blog-prodtest/secrets/bootstrap-admin-password.txt"
```

`BLOG_BOOTSTRAP_ADMIN_*` 变量只会被 `compose.bootstrap.yaml` 使用，可以在初始化完成后从
`.env.prodtest` 手动删除，也可以保留非敏感的用户名、昵称、邮箱和已失效的文件路径；普通
`compose.prod.yaml` 不会读取它们。真正必须删除的是保存明文密码的临时文件。

随后只使用日常生产 Compose 启动长期服务：

```bash
docker compose \
  --env-file "$HOME/deploy/sanjuu-blog-prodtest/.env.prodtest" \
  -f compose.prod.yaml \
  up -d --build --wait --wait-timeout 180
```

第二次运行 bootstrap 必须因“系统中已存在管理员”而失败，这属于预期的一次性保护，不能通过
删除或降级现有管理员来绕过。管理员凭据遗失时应走受控的密码恢复流程，而不是重复执行初始化。

`compose.bootstrap.yaml` 只用于每个独立环境首次初始化管理员，不参与日常启动或压测负载。它额外定义
一次性 `admin-bootstrap` 服务，并通过文件挂载读取临时密码；以 `run --rm` 启动时，该次任务容器完成后会被移除。
数据库已有管理员时任务会拒绝执行，不能用它重置或覆盖管理员账号。

### 8.2 参考容器资源配置与 P2 服务器验收

4 vCPU / 8 GB / 50 GB SSD 是一组资源参考规格，不是硬性服务器要求；实际 CPU、内存、磁盘和带宽以部署环境及测试记录为准。`compose.prod.yaml` 统一维护容器 CPU、内存和进程限制，验收和正式运行使用相同参数；旧 compose.perf.yaml 已由仅隔离行为的 compose.acceptance.yaml 替代。

| 服务 | CPU 上限 | 内存上限 | 进程配置 |
| --- | --- | --- | --- |
| backend | 2 | 2560m | Xms512m / Xmx1536m / metaspace256m / direct128m；Hikari 最大 10、最小空闲 2 |
| PostgreSQL | 1.5 | 2g | shared_buffers=512MB，max_connections=60 |
| Redis | 0.25 | 512m | maxmemory=256mb、noeviction；保留 AOF 与密码认证 |
| Nginx | 0.25 | 256m | 静态资源及 HTTPS 代理 |
| 共享入口 Nginx | 0.5 | 256m | 公网 TLS、域名分流；单独 Compose 项目 |

一套项目容器内存上限约 5.25 GiB，加共享入口约 5.5 GiB；两套项目的上限不能简单看成一套预算。剩余内存供 Linux / Docker、文件缓存、构建短时开销和后续监控使用。CPU 上限不是专用核或资源预留；这些参数应结合实际服务器和压测结果复核。未部署 Prometheus/Grafana 时，本轮只能证明无监控负载下的表现；监控接入后应复测稳定性。构建镜像和备份不要与正式压测同时进行。

验收使用独立 `blog-p2-acceptance` 项目，数据库固定 `blog_p2_performance`、Redis DB 2、项目私有卷与测试凭据。功能预发布时允许两套项目短时并行，但需监控整机资源；不要在正式业务同机运行时直接进行容量极限压测。记录其他实例与监控负载，必要时改用独立压测主机。测试数据不进入正式业务库；正式项目使用新凭据及正式库初始化。

`compose.acceptance.yaml` 显式将四个验收服务的 `restart` 覆盖为字符串 `"on-failure:3"`：不随服务器 / Docker 重启自动启动，主进程以非零退出码失败时最多重试 3 次，正常退出后不自动重启；健康检查失败本身和依赖容器重启均不会触发其他容器联动重启。正式项目与共享入口保留 `unless-stopped`；一次性 bootstrap 保持 `restart: "no"`。已创建的验收容器需在同步后重新执行合并 Compose 的 up，使策略生效。

完整从零步骤、TLS、bootstrap 顺序与压测命令见 [P2 服务器验收操作手册](./P2-server-acceptance.md)，P2 独立脚本见 [performance/p2](../performance/p2/README.md)。运行配置示例位于 performance/p2；仅示例可提交，实际 .env / properties / Token / Cookie / 报告不得提交。P2 脚本允许仓库外路径，或仓库内已被 Git 忽略且未跟踪的配置/报告路径；服务器环境文件仍按手册保存在仓库外。

验收的 HTML/API 读取经受控 HTTPS 入口；应用原子限流经 SSH 隧道到 `127.0.0.1:18080`，不对公网暴露数据库、Redis或 backend。compose.acceptance.yaml 关闭真实邮件，OSS/Turnstile 仍需独立测试配置，不能对真实第三方服务并发压测。外部服务性能替身未准备的场景应明确留空，不把验证码失败视为限流通过。

50 GB 系统盘起步时尽量留 10～15 GB 空闲，监控磁盘和 inode；Docker 日志已轮转，定期检查镜像与构建缓存，不使用删除全部卷的命令清理空间。后续 Prometheus 可先用 7 天历史 / 约 3 GB 历史容量，WAL/head 还需额外空间；备份需要异机或 OSS 副本，同机数据盘不能代替备份。

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

### 9.1 P2 上线准备

P2 功能、自动化测试和全量验收稳定后再锁定正式服务器变量和服务商配置，避免开发阶段过早绑定
生产环境。正式上线前必须完成：

- 数据库密码、Redis 密码、JWT 密钥、OSS 与 SMTP 凭据使用仓库外的 `.env.prod`，目录 / 文件权限分别为 `700` / `600`；配置不入 Git、镜像或日志，运维只使用必要的 Docker 管理权限
- 每日 `pg_dump`，本机保留最近 7 天、最近 4 周周备份，并上传异机或对象存储；每季度至少完成一次独立数据库恢复演练
- SMTP 发信域名按服务商要求配置 SPF、DKIM，DMARC 先使用 `p=none` 观察再收紧策略
- 使用 Certbot、acme.sh 或等价 ACME 客户端自动续期站点证书，续期后 reload Nginx 并执行健康检查
- OSS 自定义图片域名由 Certbot 的自动 DNS 验证插件续签，续期后通过独立 deploy hook 调用 OSS PutCname 上传证书链与私钥并更新既有域名绑定；不通过共享 Nginx 转发 OSS。DNS 手工 TXT 验证不能自动续期，Nginx reload 也不能替代 OSS 证书部署。安装、RAM 权限、独立凭据及故障重试见 [共享入口手册第 6 节](./shared-nginx-ingress.md#6-证书自动续期)。
- 至少监控 HTTPS 可用性、容器停止、磁盘阈值、备份失败和邮件持续失败，并配置外部告警渠道
- Prometheus + Grafana 作为上线后迭代，不阻塞首次上线；上线前不得完全依赖人工查看日志

Grafana 负责展示和告警，不执行数据库备份或业务表清理。数据库备份由服务器 cron / systemd timer 调用 `pg_dump`，配套异机保存、任务失败告警及恢复演练；本次不新增备份执行脚本。

应用在非 test、非 bootstrap-admin 环境启用每日 UTC 03:30 的安全数据维护：安全事件保留 90 天，认证会话在 `expires_at` 过期超过 7 天后删除，未到期的撤销会话不删除。每表每次最多删除 100 批、每批 1000 条，积压留待下一轮；不清理后台操作审计。可用 `blog.security-maintenance.enabled=false` 关闭调度，保留期和 cron 可由同前缀配置调整。普通刷新成功只累计 `blog.auth.refresh.success` 指标，RT 缺失不写安全事件；其他异常暂不聚合。数据库删除后的空间通常供后续写入复用，不保证立即归还操作系统，仍需监控表、索引和备份大小。

以下配置已在 `backend/src/main/resources/application.yaml` 中显式列出；生产环境继承通用配置，可通过 Spring Boot 配置覆盖。`test` 和 `bootstrap-admin` profile 始终不启用定时调度。

```yaml
blog:
  security-maintenance:
    enabled: true
    cron: "0 30 3 * * *" # UTC，北京时间 11:30
    event-retention-days: 90
    session-retention-days: 7
```

P2 上线新增配置至少包括：

```text
BLOG_TURNSTILE_SECRET_KEY
VITE_TURNSTILE_SITE_KEY
BLOG_VISITOR_COOKIE_SECURE=true
```

Turnstile site key 可以进入前端构建配置，secret key 只保存在服务器 `.env.prod` 并注入后端，不进入任何 `VITE_*` 变量。
本地 `dev` profile 默认使用 Cloudflare 官方测试密钥；测试验证响应不携带 action，因此开发环境默认不校验 hostname 和 action，`prod` profile 则强制配置并校验两者。
匿名访客 Cookie 在生产环境必须启用 `Secure`、`HttpOnly`、`SameSite=Lax`，并使用根路径 `/`。

### 9.2 上线后迭代

- 接入 Prometheus + Grafana，补充应用、JVM、PostgreSQL、Redis、Nginx 和业务指标仪表盘
- 根据实际使用情况评估游客点赞与账号点赞合并、点赞反作弊和 SMTP 退信自动处理
- 增加头像审核：拒绝后回退为默认头像并发送管理员消息，不联动隐藏已有评论或留言

## 10. 发布前检查清单

SEO 使用站点级 `BLOG_SITE_URL`（例如 `https://blog.example.com`），在根目录环境文件维护一份，
Compose 显式传入后端。前端无需另配域名；本地直接启动 Spring Boot 时，在 IDE/启动环境中设置该变量，
Spring Boot 不自动读取根目录 `.env`。站点名由 `BLOG_APP_NAME` 提供；友链申请资料描述和 Logo 地址分别由
`BLOG_DESC` 和 `BLOG_LOGO_URL` 提供。生产 Compose 要求四项均显式非空，站点地址和 Logo 地址必须使用 HTTPS；
dev/test 使用各自 profile 的明确本地资料，站点地址允许 localhost HTTP，Logo 地址允许 HTTP(S)，不依赖生产环境文件。

正式生产的仓库外 `.env.prod` 必须填写以下友链申请资料，不能留空；本地历史演练的 `.env.prodtest` 同样适用：

| 变量 | 需要填写的值 | 约束 |
| --- | --- | --- |
| `BLOG_APP_NAME` | 对外展示的网站名称 | 去除首尾空白后非空 |
| `BLOG_SITE_URL` | 网站规范 origin | HTTPS，例如 `https://blog.example.com`，不带路径、查询或片段 |
| `BLOG_DESC` | 友链申请时展示的网站描述 | 非空纯文本 |
| `BLOG_LOGO_URL` | 站点头像或 Logo 的公开图片地址 | HTTPS 绝对 URL |

四项由 `compose.prod.yaml` 的 backend 和 `compose.bootstrap.yaml` 的管理员初始化服务共同读取。
仅更新环境文件时，需要重新创建 backend 容器使 Spring Boot 读取新配置；无需重建前端镜像。

友链申请资料中的本站头像使用 `frontend/public/img/site-avatar.jpg`，Vite 构建后由项目 Nginx 直接提供，
正式地址配置为 `BLOG_LOGO_URL=https://blog.example.com/img/site-avatar.jpg`（替换为实际域名，URL 不包含 `/public`）。
该图片供其他网站嵌入，应允许站外 Referer 及无 Referer 请求；OSS 启用防盗链不影响这个同站点静态文件。
新增或替换图片需要重新构建并部署项目 nginx 镜像，保持 `/img/site-avatar.jpg` 路径稳定；
如同时修改 `BLOG_LOGO_URL`，还需重新创建 backend 容器。真实环境文件不会随示例模板更新，需在服务器同步填写。

后端 `BLOG_SEO_HTML_TEMPLATE` 指定入口模板，开发默认为 `file:../frontend/index.html`。
生产 nginx 容器启动时将本次构建的 `index.html` 原子写入共享模板卷，后端只读挂载并按请求读取，
JS/CSS 静态资源仍由 nginx 提供；前端部署须保留上一版本带哈希的静态资源直到旧页面请求结束。
nginx 将公开页面、文章详情、`/sitemap.xml` 和 `/robots.txt` 转发后端，不按 User-Agent 区分爬虫。
首页只使用 `/`；旧 `/home` 也转发到后端，由后端以 308 重定向至 `/` 并保留查询参数。开发 Vite 代理保留上游重定向状态和 Location，不在代理内部跟随。
`/archives` 由 Nginx/Vite 转发后端生成首屏元信息，query 不生成独立 canonical；归档 API 允许爬虫渲染读取但 JSON 保持 noindex。
`/rss.xml` 也由 nginx 转发后端并保留 `application/rss+xml` 响应类型；本地开发由 Vite SEO 中间件代理该 feed 路径。
后端仅注入 HTML head 元信息，不生成正文；文章内容由 Vue 渲染。部署后检查页面源代码可见 canonical/OG，
Vue 页面只请求一次文章详情且不会先展示另一套文章布局。
无效文章返回带 noindex 的前端入口和 HTTP 404，Nginx 透传此响应，Vue 启动后显示布局内文章错误状态；开发 Vite 同样转换该 404 入口并保留状态码。
模板尚未就绪时公开 HTML 返回 503；发布后检查模板与资源属于同一次前端构建。
robots 允许爬虫读取 Vue 公开渲染所需的 API；Nginx 在通用 API 和独立留言接口 location
统一返回 `X-Robots-Tag: noindex`，JSON 可加载但不单独收录，公开 HTML 不加此头。
根目录 `.gitattributes` 将 `frontend/docker/40-share-seo-template.sh` 固定为 LF，
确保 Windows checkout 后复制到 Linux 镜像的启动脚本仍可执行。

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
