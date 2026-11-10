# 数据库账号与部署流程

本项目使用两类账号：管理账号负责初始化、Flyway、备份恢复；受限应用账号负责 backend 日常读写及网站管理员 bootstrap。数据库账号与网站的管理员角色无关。真实密码保存在仓库外环境配置中，不提交到 Git。

## 1. 自动执行的内容

- 应用账号缺失时创建；已存在时检查权限，不重置密码。如果环境文件中的密码不匹配，准备失败，需要先确认配置，不通过初始化任务覆盖密码。
- 应用账号不能与管理账号相同，不能是超级用户、数据库/对象所有者或其他角色成员；应用账号名称只允许小写字母开头、后续小写字母/数字/下划线，最多 63 个字符。
- 迁移后授予目标库 CONNECT、public schema USAGE、`blog_` 业务表读写和业务序列 USAGE/SELECT；不授予 TRUNCATE、DDL、角色管理或 Flyway 历史表权限。
- 为实际创建对象的管理账号设置 public schema 默认权限，新表和序列自动继承；每次迁移完成后重新核对既有业务对象授权，并撤销 Flyway 历史表权限。public schema 仅存放本项目对象。
- 撤销目标库的 PUBLIC CONNECT/TEMPORARY 和 public schema 的 PUBLIC CREATE。开发库、测试库分别使用不同应用账号；其他数据库用户需要访问时必须显式授权。
- 业务表结构仍由 Flyway migration 管理。账号和权限是环境相关部署配置，不将密码写入 migration，也不修改已执行的历史文件。

## 2. 本机 dev 和 test

开发库沿用现有 `blog_db` 和数据卷，管理账号沿用现有账号；只新增应用账号，不清空数据。按 `.env.example` 填写以下变量，并显式注入 IDEA/Maven 环境；Spring Boot 不自动读取根目录 `.env`：

| 用途         | 变量                                                                                          |
| ------------ | --------------------------------------------------------------------------------------------- |
| 开发业务连接 | `BLOG_DB_URL`、`POSTGRES_APP_USERNAME`、`POSTGRES_APP_PASSWORD`                               |
| 开发迁移连接 | `POSTGRES_ADMIN_USERNAME`、`POSTGRES_ADMIN_PASSWORD`，URL 仍使用 `BLOG_DB_URL`                |
| 测试业务连接 | `BLOG_TEST_DB_URL`、`POSTGRES_TEST_APP_USERNAME`、`POSTGRES_TEST_APP_PASSWORD`                |
| 测试迁移连接 | `POSTGRES_TEST_ADMIN_USERNAME`、`POSTGRES_TEST_ADMIN_PASSWORD`，URL 仍使用 `BLOG_TEST_DB_URL` |

推荐应用账号为 `blog_dev_app` 和 `blog_test_app`，各自使用独立密码；管理连接在已有本机 Compose 默认配置下使用 `sanjuu`。不要把原管理账号填入业务用户名变量。管理账号和数据库密码不会因修改 Compose 环境而自动改变已有数据卷。

普通后端继续使用 IDEA 的 `dev` profile 启动。Flyway 使用独立管理连接，BEFORE_MIGRATE 创建缺失应用账号，AFTER_MIGRATE 补齐授权并验证应用凭据；数据库初始化成功后业务服务才就绪。新增 migration 后仍然直接启动后端，无需手动执行 SQL 或另跑迁移命令。

集成测试使用 `test` profile，执行相同流程但只连接 `blog_test`；CI 自动创建测试库并注入两套凭据。全新本地环境若还没有测试库，可在启动开发 PostgreSQL 后执行以下命令一次，已存在则跳过，不删除原测试数据：

```bash
docker compose -f compose.dev.yaml exec -T postgres createdb -U sanjuu blog_test
```

单元测试不需要数据库。账号权限集成测试会使用测试管理连接创建独立临时数据库及角色，完成后清理，不改开发库。

## 3. 首次部署

所有命令在服务器项目根目录执行；先按 [共享入口手册](./shared-nginx-ingress.md) 准备网络、HTTPS 和 `prod` / `prodboot` 函数。`.env.prod.example` 中 `POSTGRES_ADMIN_USERNAME/PASSWORD` 是管理凭据，`POSTGRES_APP_USERNAME/PASSWORD` 是应用凭据，密码应不同。

环境文件与后端维护入口统一使用 ADMIN / APP 命名。PostgreSQL 官方镜像内部仍要求 `POSTGRES_USER`、`POSTGRES_PASSWORD`，Compose 将管理凭据映射到这两个变量；后文 `prod exec ... sh -c` 内使用的是容器内部变量。已有环境文件需改名并保留原值，不要重新生成或修改数据库中的现有密码。

你执行：

```bash
prod config -q
prod build postgres
prod build backend
prod build nginx
prod up -d --wait postgres redis
```

PostgreSQL 在空数据卷中自动创建 `POSTGRES_DB` 和管理账号；尚未创建业务表。然后按共享入口手册准备网站管理员临时密码文件，执行：

```bash
prodboot run --rm admin-bootstrap
```

不要添加 `--no-deps`。Compose 自动等待 `db-prepare` 成功；该服务复用 backend 镜像，但以数据库维护模式运行，只接收数据库配置，创建应用账号、执行 Flyway、授予权限后退出，不启动 HTTP 服务，不依赖 JWT/OSS/邮件配置。bootstrap 随后以应用账号创建网站管理员。成功后删除临时密码文件，再执行：

```bash
prod up -d --wait --wait-timeout 180
prod ps
```

`db-prepare` 为 `restart: "no"` 的一次性服务，成功退出是正常状态。backend 依赖其 `service_completed_successfully`，不持有管理凭据且关闭 Flyway；Nginx 再等待 backend 健康。准备失败则不继续启动 backend，先检查 `prod logs db-prepare`。

## 4. 以后更新版本

获取新版代码，修改本次镜像标签并构建或拉取镜像；`db-prepare` 与 backend 必须使用同一新版镜像。发布前备份，并在维护窗口停止旧的项目 Nginx、backend，数据库和共享入口保留：

```bash
prod stop nginx backend
prod up -d --wait --wait-timeout 180
prod ps
```

Compose 自动用本次镜像运行 `db-prepare`，Flyway 仅执行未完成的 migration，再核对授权；任务成功后启动新版后端和 Nginx。不要使用 `up --no-deps` 绕过准备任务。只执行 `restart` 不会部署新镜像或执行新 migration。

没有新 migration 时准备任务正常结束，不清库、不重建表、不重置密码、不重复网站管理员 bootstrap。普通运行中的容器重启不需要再执行发布流程。

## 5. 搬到新服务器恢复备份

先准备环境文件、共享入口、镜像及备份。空数据卷的 PostgreSQL 会自动建立配置的数据库和管理账号，无需进入容器手动 CREATE DATABASE。此阶段只启动 PostgreSQL、Redis，再运行账号模式：

```bash
prod config -q
prod build postgres
prod build backend
prod build nginx
prod up -d --wait postgres redis
prod run --rm --no-deps db-prepare --database-prepare=accounts
```

账号模式只建立/检查应用账号和数据库连接权限，不创建任何业务表或 Flyway 历史表。然后恢复完整备份（替换备份路径）：

```bash
prod exec -T postgres sh -c \
  'PGPASSWORD="$POSTGRES_PASSWORD" pg_restore \
    --no-password --exit-on-error --single-transaction \
    --no-owner --no-privileges \
    -U "$POSTGRES_USER" -d "$POSTGRES_DB"' \
  < /opt/blog-platform/backups/your-backup.dump

prod up -d --wait --wait-timeout 180
prod ps
```

不要在恢复前启动全部服务或运行完整迁移，以免先建表后导入冲突。`pg_dump` 不包含角色，恢复时的 `--no-owner --no-privileges` 又不导入原所有权和权限；账号模式补齐角色，恢复后的普通 `up` 自动执行必要的新 migration 并重新授权。

恢复后保留原网站管理员，不执行 bootstrap。核对文章、登录、搜索和后台功能后再开放访问。原服务器的数据卷不在此流程中删除。

## 6. 维护边界

管理账号保留高权限以运行包含 zhparser 扩展安装的历史 migration；只有数据库准备任务及受控备份恢复使用它。长期 backend 和管理员 bootstrap 均使用受限应用账号。开发进程为保持自动迁移体验，会同时持有两套凭据；生产业务进程不会。

普通角色密码轮换需要受控修改数据库密码并同步环境文件，不由日常准备任务静默完成。需要改变数据库、schema 或 Flyway 历史表名称时先调整维护逻辑和文档，不直接复用本手册的 public schema 授权规则。
