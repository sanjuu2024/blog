# 使用独立 Nginx 容器作为共享入口

所有命令在服务器执行。宿主机只需 Docker、Compose 插件和 Certbot；使用具有 sudo 权限的普通用户，不加入 docker 组。示例域名为 `blog.example.com` / `staging.example.com`，示例根目录为 `/opt/blog-platform`，执行前统一替换。真实配置和证书留在仓库外。

```text
公网 80/443 → blog-ingress / nginx（唯一 TLS 入口）
  ├─ blog-prod-edge → blog-prod-web:80 → 正式 backend / 数据库 / Redis
  └─ blog-staging-edge → blog-staging-web:80 → staging backend / 数据库 / Redis
```

两个项目各保留自己的 Nginx，用于前端、SEO 模板和 API。它们不发布宿主机端口，不挂载证书。共享入口只连接两个 edge 网络，不连接任何 backend_net；只有项目 Nginx 同时连接本项目的 edge 和 backend_net。

## 1. 准备 DNS、端口和目录

先为 staging 的实际域名添加 A 记录，指向服务器公网 IPv4；正式域名可以之后准备。安全组放行 80/443，SSH 保持自己的访问规则。首次不套 CDN；未配置 IPv6 时不添加 AAAA。

```bash
sudo docker version
sudo docker compose version
sudo ss -lntp | grep -E ':(80|443)\b' || true
sudo docker network ls
ip route
```

共享入口将使用 `172.30.10.0/29`、`172.30.11.0/29`。检查现有 Docker 网络的 Subnet（必要时执行 `sudo docker network inspect 网络名`），确认不与 Docker/VPN/路由重叠；冲突时先改 `compose.ingress.yaml` 的子网、网关和入口静态 IP，再同步后文 `BLOG_INGRESS_IP`。不要删除已有网络来腾网段。

若之前已安装了专供本项目使用的宿主机 Nginx，先执行 `sudo systemctl disable --now nginx`，释放 80/443；没有安装则跳过。已经运行的项目 Nginx 也需切换到新配置，才能释放公网端口。安装 Certbot：

```bash
sudo apt-get update
sudo apt-get install -y certbot
```

在已检出本次版本的仓库根目录执行：

```bash
sudo install -d -o "$(id -un)" -g "$(id -gn)" /opt/blog-platform
mkdir -p /opt/blog-platform/ingress/templates /opt/blog-platform/ingress/snippets
cp compose.ingress.yaml /opt/blog-platform/ingress/compose.yaml
cp docker/ingress/ingress.env.example /opt/blog-platform/ingress/.env
cp docker/ingress/templates/default.conf.template /opt/blog-platform/ingress/templates/
cp docker/ingress/snippets/proxy.conf /opt/blog-platform/ingress/snippets/
chmod 600 /opt/blog-platform/ingress/.env
sudo install -d -m 0755 /etc/letsencrypt /var/www/certbot
```

编辑仓库外 `/opt/blog-platform/ingress/.env`：将两个域名改为自己控制的域名，核对 templates、snippets、Certbot 和 webroot 的绝对路径。此时只复制 HTTP 默认模板；尚未签证时不要复制 HTTPS 模板。

## 2. 启动共享入口和独立网络

当前 shell 定义（新开终端需重新定义）：

```bash
ing() {
  sudo docker compose --env-file /opt/blog-platform/ingress/.env \
    -p blog-ingress -f /opt/blog-platform/ingress/compose.yaml "$@"
}
ing config -q
ing up -d --wait --wait-timeout 120
ing exec -T nginx nginx -t
ing ps
```

该项目会创建两个 edge 网络，之后才能启动博客 Compose。初始入口支持 HTTP ACME 验证，HTTPS 暂时拒绝握手；与项目是否已运行无关，不会依赖尚未创建的 backend。

## 3. 签发并启用 staging HTTPS

只为已就绪的 staging 域名签证，无需提前签正式站证书：

```bash
sudo certbot certonly --webroot -w /var/www/certbot \
  --cert-name staging.example.com -d staging.example.com
sudo certbot certificates
```

确认生成 `/etc/letsencrypt/live/staging.example.com/fullchain.pem` 和 `privkey.pem`。如果使用不同 cert-name 或旧证书目录带后缀，修改下文复制模板中的路径。共享入口挂载整个 `/etc/letsencrypt`，live 到 archive 的链接保持有效，不需要复制证书文件。

在仓库根目录复制 staging 模板，然后让运行中的容器重新生成配置并检查、reload：

```bash
cp docker/ingress/templates/staging.conf.template /opt/blog-platform/ingress/templates/
ing exec -T nginx sh -c '/docker-entrypoint.d/20-envsubst-on-templates.sh && nginx -t && nginx -s reload'
```

只有检查成功才 reload；命令失败时修正模板后重试，不重启带错误配置的入口。此时若 staging 项目尚未启动，访问页面得到带 noindex 的 502 是预期，下一步启动后应恢复 200。staging 的 HTTP、HTTPS、错误响应均由入口加 `X-Robots-Tag: noindex, nofollow`。

staging HTTPS 入口隐藏上游的 `X-Robots-Tag`，再统一添加上述响应头，避免与验收项目的防收录规则重复。公网响应应只出现一条该头；正式入口继续透传 API、404 等上游响应原有的 noindex，不使用此隐藏规则。

## 4. 接入 staging 项目

在仓库外 `.env.acceptance` 中设置，P2 示例已提供这些值：

```dotenv
BLOG_EDGE_NETWORK=blog-staging-edge
BLOG_WEB_ALIAS=blog-staging-web
BLOG_INGRESS_IP=172.30.11.2
```

原 `NGINX_HTTP_PORT`、`NGINX_HTTPS_PORT`、`NGINX_TLS_CERT_DIR` 可以从私有配置中移除；新项目 Nginx 不读这些值。数据库、JWT、OSS 等继续按 [P2 服务器手册](./P2-server-acceptance.md) 填写；先初始化管理员，再启动完整项目。普通启动命令为：

```bash
sudo docker compose --env-file /opt/blog-platform/acceptance/.env.acceptance \
  -p blog-p2-acceptance -f compose.prod.yaml -f compose.acceptance.yaml \
  up -d --wait --wait-timeout 180
```

入口在网络 `blog-staging-edge` 上固定使用 `172.30.11.2`，项目 Nginx 只信任该地址传来的 `X-Real-IP`。入口覆盖外部 X-Forwarded-\* 并删除 `Forwarded`，避免伪造来源；容器间 HTTP 向后端固定传递公网 HTTPS 语义，Secure Cookie 仍然启用。

## 5. 之后接入正式项目

### 5.1 准备正式环境文件

正式项目使用独立检出目录及仓库外 `.env.prod`。在准备好的正式版本仓库根目录，以 `.env.prod.example` 为模板创建配置；下面的 `cp -n` 只在目标不存在时复制，已有文件直接编辑，不覆盖凭据：

```bash
install -d -m 0700 /opt/blog-platform/production
(umask 077; cp -n .env.prod.example /opt/blog-platform/production/.env.prod)
chmod 600 /opt/blog-platform/production/.env.prod
```

填写模板中的全部 `CHANGE_ME` 和示例域名。`.env.prod.example` 是正式 Compose 的完整输入模板；根目录 `.env.example` 供本地开发参考，`performance/p2/acceptance.env.example` 供独立验收使用，不复制两份变量再互相覆盖。环境文件不会被 Spring Boot 自动读取，下面通过 Compose 的 `--env-file` 加载。当前数据库初始化、Flyway 和后端共用 `POSTGRES_USER`，填写本模板不等于已实现数据库管理 / 应用账号分离。

网络和镜像示例为：

```dotenv
BLOG_EDGE_NETWORK=blog-prod-edge
BLOG_WEB_ALIAS=blog-prod-web
BLOG_INGRESS_IP=172.30.10.2
BLOG_POSTGRES_IMAGE=blog-postgres:v1.0.0
BLOG_BACKEND_IMAGE=blog-backend:v1.0.0
BLOG_NGINX_IMAGE=blog-nginx:v1.0.0
```

三个 `BLOG_*_IMAGE` 是本次计划发布 `v1.0.0` 的定制构建产物标签；bootstrap 与长期 backend 使用同一个后端镜像，只切换启动 profile。先按 5.2 构建确定的源码，再启动容器；镜像标签不会让 `up` 自动重新编译，也不会自动创建 Git 标签。正式发布后不再覆盖该版本标签，后续发布更换版本或 Commit 标签。Redis 和共享入口 Nginx 使用仓库内锁定的官方镜像 digest。已有私有 `.env.prod` 若仍显式设置旧镜像标签，需手动更新，Compose 默认值不会覆盖环境文件。历史卷逻辑名不改；如果以前未使用 `-p`，实际项目名为 `blog-prodtest`，更新该已有部署时显式保留旧项目名，不能仅靠改名迁移数据。

### 5.2 先限制入口，再启动正式站

“受控启动”表示正式项目已经运行，但正式 HTTPS 入口临时只允许部署者当前的公网 IP；其他访问者被拒绝。这不需要开放 backend 端口，也不改应用认证逻辑。共享入口直接面向公网，下面的 allow 地址是自己的实际公网出口 IP，不是 `127.0.0.1`、内网 IP 或服务器 IP；动态 IP 变更后要相应更新。使用 IPv6 访问时另加自己的 IPv6 地址。若入口前另有 CDN，不能直接照搬本节的来源 IP 限制。

正式域名 DNS 就绪后，单独签证并复制模板；此时先不 reload：

```bash
sudo certbot certonly --webroot -w /var/www/certbot \
  --cert-name blog.example.com -d blog.example.com
cp docker/ingress/templates/production.conf.template /opt/blog-platform/ingress/templates/
```

用编辑器打开刚复制到服务器的 `/opt/blog-platform/ingress/templates/production.conf.template`，在原有 `location /` 中临时加入下面三行（仅示意新增部分，保留后续 proxy 配置）；`203.0.113.10` 必须替换成自己的公网 IP：

```nginx
location / {
    allow 203.0.113.10;
    deny all;
    add_header X-Robots-Tag "noindex, nofollow" always;
    # 保留原有 set、proxy_pass 和 include 配置。
}
```

编辑完成后生成配置、检查再 reload：

```bash
ing exec -T nginx sh -c '/docker-entrypoint.d/20-envsubst-on-templates.sh && nginx -t && nginx -s reload'
```

限制只放在正式 HTTPS 的 `location /`，不要改 HTTP 的 ACME 路径或共享默认模板，否则可能影响证书续期。此时博客未启动，允许来源可能暂时得到 502；非允许来源应得到 403。不要在完成受控验收前用仓库模板覆盖这个私有模板，否则会提前移除访问限制。

在正式项目仓库根目录定义日常和首次初始化命令；两者均显式指定正式项目，不叠加 `compose.acceptance.yaml`：

```bash
prod() {
  sudo docker compose --env-file /opt/blog-platform/production/.env.prod \
    -p blog-prod -f compose.prod.yaml "$@"
}
prodboot() {
  sudo docker compose --env-file /opt/blog-platform/production/.env.prod \
    -p blog-prod -f compose.prod.yaml -f compose.bootstrap.yaml "$@"
}
prod config -q
prod build postgres
prod build backend
prod build nginx
prod up -d --wait postgres redis
```

全新正式库按下列步骤初始化管理员；如果从完整备份恢复，先恢复数据库，跳过管理员 bootstrap，不能先让应用把空库迁移一遍再导入备份。下面的密码文件路径应与 `.env.prod` 中 `BLOG_BOOTSTRAP_ADMIN_PASSWORD_PATH` 一致：

```bash
read -rsp 'Production bootstrap password: ' production_bootstrap_password
printf '\n'
(umask 077; printf '%s' "$production_bootstrap_password" > /opt/blog-platform/production/bootstrap-password)
unset production_bootstrap_password
prodboot config -q
prodboot run --rm --no-deps admin-bootstrap
```

只有看到 `ADMIN_BOOTSTRAP_SUCCESS` 后再删除刚创建的临时文件并启动长期服务；初始化失败先处理报错，不重复覆盖已有管理员：

```bash
rm /opt/blog-platform/production/bootstrap-password
prod up -d --wait --wait-timeout 180
prod ps
```

长期凭据使用 `.env.prod`，首次管理员密码继续沿用现有一次性文件挂载，成功后删除临时文件。

### 5.3 受控验证后开放访问

先从允许的公网 IP 浏览正式 HTTPS 首页，完成登录 / 刷新 / 退出、发布 / 搜索、评论 / 通知、图片上传及少量真实邮件、Turnstile 验证；另从不在允许列表的来源确认 403。检查 `prod ps` 的健康状态和容器日志，不仅检查首页能否打开。受控期间，服务器外部监控如果不在 allow 列表会得到 403，这是限制效果，不能将其当作正式可用性通过；开放后再验证外部监测和告警。

监控、每日备份、证书续期均准备并验证后，在私有正式模板中移除临时的 `allow`、`deny all` 和临时 `X-Robots-Tag` 三行，再执行前面的配置生成 / 检查 / reload 命令。不要删除其他 HTTPS 或 proxy 配置。确认正常正式首页没有 `noindex`，API 等资源原有的 noindex 保持不变；再从另一网络检查可访问并执行一次部署后冒烟。正式站访问限制和临时 noindex 的移除是显式开放步骤，不由 `prod up` 自动完成。

入口使用 Docker DNS 动态解析项目别名，项目 Nginx 重建换 IP 后无需修改入口配置。项目停止时只影响对应域名，不影响其他域名或 ACME 验证。

## 6. 证书自动续期

同一台服务器的 `certbot renew` 会检查其 `/etc/letsencrypt/renewal/` 中登记的全部证书，只有临近续期的证书才会重新签发。它不会自动接管 acme.sh、手动复制来的证书，也不会自动把文件上传到阿里云 OSS。这里把流程分成两条：站点证书续签后 reload 共享 Nginx；OSS 域名证书续签后调用 OSS API 更新其证书绑定。两者共用一个 Certbot 定时器，不依赖博客项目是否运行。

定时器负责定期检查，DNS 插件只在 Certbot 决定续签时执行域名验证，不自己设置续签日期。当前工具版本优先参考证书机构通过 ARI 提供的续签窗口；没有可用窗口或显式配置时，一般在证书有效期剩余约三分之一时续签，例如 90 天证书约在到期前 30 天。短于 10 天的证书采用约剩余一半有效期的默认规则；实际检查计划通过 `systemctl list-timers --all certbot.timer` 查看，不依赖人工等到固定某一天。

### 6.1 站点证书和共享入口

安装续期成功 hook；先将脚本中的 `/opt/blog-platform` 换成实际部署根目录。hook 只检查并 reload 共享入口，不重启项目或复制私钥：

```bash
sudo install -d -m 0755 /etc/letsencrypt/renewal-hooks/deploy
sudo install -m 0755 docker/ingress/reload-after-renewal.sh \
  /etc/letsencrypt/renewal-hooks/deploy/20-blog-ingress-reload
sudo systemctl enable --now certbot.timer
sudo certbot renew --cert-name staging.example.com --dry-run --run-deploy-hooks
sudo systemctl list-timers certbot.timer
```

hook 默认读取 `/opt/blog-platform/ingress/.env` 和 `compose.yaml`；若改部署目录，安装前同步修改脚本。这里的 `--cert-name` 换成实际站点证书名，正式站可再单独验证一次；先不测试尚未配置自动 DNS 验证的 OSS 证书，全部证书的统一验证放在 6.4。只有 dry-run、hook 和定时器均验证成功才记录自动续期完成。webroot 不占用额外监听端口，staging 项目关闭时也可续证；共享入口、域名 DNS 和 HTTP 80 仍须可访问。旧证书如果使用 standalone，需先通过 `certbot reconfigure --cert-name 实际证书名 --webroot -w /var/www/certbot` 迁移验证方式并重新 dry-run。

### 6.2 OSS 域名：确认 DNS 验证可以自动执行

6.2～6.4 是在服务器上做一次配置，以后由定时任务自动执行。先完成 6.1，再依次完成这三节；不要只完成 6.2 就认为 OSS 证书已经能自动更新。

| 章节 | 你现在要做的事                                            | 配好后自动完成的事                                    |
| ---- | --------------------------------------------------------- | ----------------------------------------------------- |
| 6.2  | 安装 DNS 插件，填写 DNS AccessKey，修改现有证书的验证方式 | 续签时自动添加验证用 TXT 记录，验证后清理本次记录     |
| 6.3  | 填写 OSS 证书部署配置，测试一次真实上传，安装部署脚本     | OSS 域名证书续签成功后，把新证书上传并绑定到 OSS      |
| 6.4  | 让系统定时任务使用带插件的 Certbot，验证整个流程          | 定时检查本站和 OSS 证书，按需续签，再执行对应部署脚本 |

以下适用于图片域名直接绑定普通 OSS bucket，且 DNS 托管在阿里云云解析 DNS。若 DNS 托管在其他服务商，6.2 要使用对应插件；若域名实际接入 CDN/DCDN，6.3 要更新相应产品的证书，不能使用 OSS bucket 绑定接口。

所有命令在服务器执行；涉及 `docker/ingress/...` 的相对路径时，当前目录必须是项目仓库根目录。示例部署根目录 `/opt/blog-platform` 和图片域名 `img.example.com` 要统一替换为自己的值。这里安装的是宿主机工具，不需要重新构建博客镜像或停止博客。

#### 6.2.1 查出图片域名对应的证书名称

```bash
sudo certbot certificates
```

找到 `Domains` 包含自己图片域名的那张证书，记下它的 `Certificate Name`。例如图片域名为 `img.example.com`，证书名称可能是 `img.example.com-0001`；通配符证书也可能使用其他名称。后文的 `--cert-name` 和 JSON 中的 `certificate_name` 都填写这个名称，不凭域名猜。

在服务器本地查看对应的续期配置，只检查验证方式，不修改此文件：

```bash
sudo grep -E '^(authenticator|manual_auth_hook|manual_cleanup_hook)\s*=' \
  /etc/letsencrypt/renewal/实际证书名称.conf
```

如果是 `authenticator = manual`，且没有自动 DNS auth/cleanup hook，就需要完成下面的插件配置。之前手动添加 TXT 签发成功，只说明证书已经签出来，续签仍需要自动验证能力。如果原来使用的是 `dns-aliyun`，按下面步骤切换为本仓库的 `dns-aliyun-safe`；已经使用 `dns-aliyun-safe` 且验证正常的证书无需重复执行 6.2.4，但仍要确认下面的工具环境和凭据文件可用。

#### 6.2.2 安装带 DNS 插件的 Certbot

虚拟环境可以理解成专用于证书工具的 Python 安装目录，不需要手动激活，也不需要常驻运行。下面同时安装 Certbot、DNS 插件和后面上传证书要用的 OSS SDK；依赖版本见 `docker/ingress/requirements-certificates.txt`。

```bash
sudo apt-get install -y python3-venv openssl
sudo install -d -m 0755 /opt/blog-platform/certificates
sudo python3 -m venv /opt/blog-platform/certificates/venv
sudo /opt/blog-platform/certificates/venv/bin/pip install \
  -r "$PWD/docker/ingress/requirements-certificates.txt"
sudo /opt/blog-platform/certificates/venv/bin/pip install \
  --force-reinstall --no-deps --no-build-isolation \
  "$PWD/docker/ingress/certbot-dns-aliyun-safe"
sudo /opt/blog-platform/certificates/venv/bin/certbot plugins
```

成功标志：最后一条命令的插件列表包含 `dns-aliyun-safe`。旧名称 `dns-aliyun` 同时出现是正常的。

本仓库插件继承第三方 [certbot-dns-aliyun](https://github.com/tengattack/certbot-dns-aliyun) 的验证和创建记录能力，两者均不是阿里云官方 Certbot 插件。`dns-aliyun-safe` 查询全部相关分页，只删除记录名、`TXT` 类型和本次挑战值均匹配的唯一记录；不存在时跳过，多条精确匹配时报错且不删除，保留其他已有记录。

首次安装与插件代码更新后都要执行上面的本地扩展安装命令，不能只安装 requirements 或只 `git pull`。`--force-reinstall` 会安装同版本的新代码，`--no-deps` 保留前一步锁定的依赖。需要本地自检时执行以下命令，它不调用真实 DNS API：

```bash
sudo /opt/blog-platform/certificates/venv/bin/python -B \
  docker/ingress/test_dns_aliyun_safe.py -v
```

#### 6.2.3 创建 DNS AccessKey 并写入服务器

在阿里云访问控制 RAM 控制台创建专用于 DNS 验证的身份和 AccessKey，授予以下操作，并按 DNS 服务支持的资源粒度限制授权域名：

- `alidns:DescribeDomains`
- `alidns:DescribeDomainRecords`
- `alidns:AddDomainRecord`
- `alidns:DeleteDomainRecord`

这套 AccessKey 只负责修改 DNS 记录。6.3 会另外创建一套上传证书到 OSS 的 AccessKey；两套都与博客后端上传图片使用的凭据分开，不使用主账号 AccessKey。

在服务器创建并编辑凭据文件。文件已存在时，`touch` 会保留原有内容：

```bash
sudo touch /etc/letsencrypt/aliyun-dns.ini
sudo chmod 600 /etc/letsencrypt/aliyun-dns.ini
sudoedit /etc/letsencrypt/aliyun-dns.ini
```

首次配置时填入以下内容；如果已有这两个字段，检查或更新原值。把两个占位符替换为刚创建的 DNS AccessKey，字段名原样保留：

```ini
dns_aliyun_safe_access_key = CHANGE_ME_DNS_ACCESS_KEY_ID
dns_aliyun_safe_access_key_secret = CHANGE_ME_DNS_ACCESS_KEY_SECRET
```

不用填写 TXT 验证值。每次续签时，Certbot 都会生成新的验证值，由插件自动添加和清理。已有凭据文件若使用旧插件字段，也要改为 `dns_aliyun_safe_` 前缀，AccessKey 本身不因插件改名而改变。

#### 6.2.4 把现有图片证书改成自动 DNS 验证

把下面的 `实际证书名称` 换成 6.2.1 记下的 `Certificate Name`，执行：

```bash
sudo /opt/blog-platform/certificates/venv/bin/certbot reconfigure \
  --cert-name 实际证书名称 --authenticator dns-aliyun-safe \
  --dns-aliyun-safe-credentials /etc/letsencrypt/aliyun-dns.ini \
  --dns-aliyun-safe-propagation-seconds 120
```

这条命令会进行测试续期：插件调用真实 DNS API，自动添加 TXT，等待传播，再完成验证和清理。成功后保存新的续期方式；测试成功不代表已把证书上传到 OSS。

成功标志：命令成功结束，续期配置中的 `authenticator` 已变为 `dns-aliyun-safe`，没有要求手动填写 TXT。期间不要修改图片域名现有的 CNAME；插件操作的是验证用 TXT。

如果图片证书尚未签发，用下面的命令首次申请，并将图片域名换成自己的域名；已经有证书则不要重复执行：

```bash
sudo /opt/blog-platform/certificates/venv/bin/certbot certonly \
  --cert-name img.example.com -d img.example.com \
  --authenticator dns-aliyun-safe \
  --dns-aliyun-safe-credentials /etc/letsencrypt/aliyun-dns.ini \
  --dns-aliyun-safe-propagation-seconds 120
```

### 6.3 OSS 域名：安装续签后的证书部署 hook

这一步让 OSS 使用服务器签出来的证书。证书文件存在服务器上，并不意味着 OSS 已经更新；需要调用 OSS API 上传证书链和私钥。

这里的 hook 就是“续签成功后，Certbot 自动调用的脚本”。脚本使用 Python 和官方 OSS SDK，无需 Java、ossutil 或额外常驻服务。先手动验证上传成功，再安装 hook。

#### 6.3.1 创建 OSS 证书部署 AccessKey

在 RAM 控制台另外创建一个证书部署身份和 AccessKey，并为它配置下面的权限策略。把 `YOUR_IMAGE_BUCKET` 换成图片 bucket 名，其他操作名保持不变；`yundun-cert` 的三项操作用于关联证书服务，也需要保留：

```json
{
  "Version": "1",
  "Statement": [
    {
      "Effect": "Allow",
      "Action": ["oss:ListCname", "oss:PutCname"],
      "Resource": ["acs:oss:*:*:YOUR_IMAGE_BUCKET"]
    },
    {
      "Effect": "Allow",
      "Action": [
        "yundun-cert:DescribeSSLCertificatePrivateKey",
        "yundun-cert:DescribeSSLCertificatePublicKeyDetail",
        "yundun-cert:CreateSSLCertificate"
      ],
      "Resource": ["*"]
    }
  ]
}
```

#### 6.3.2 填写部署配置，安装上传脚本

在仓库根目录执行下面的命令。首次配置时复制示例；文件已存在时保留原内容，然后编辑服务器上的私有配置：

```bash
sudo install -d -m 0700 /etc/blog-platform
if ! sudo test -e /etc/blog-platform/oss-certificate.json; then
  sudo install -m 0600 docker/ingress/oss-certificate.example.json \
    /etc/blog-platform/oss-certificate.json
fi
sudoedit /etc/blog-platform/oss-certificate.json
```

各字段这样填写：

| 字段                | 填写内容                                                 | 示例                                   |
| ------------------- | -------------------------------------------------------- | -------------------------------------- |
| `endpoint`          | bucket 所在地域的 OSS HTTPS endpoint，不填图片自定义域名 | `https://oss-cn-hangzhou.aliyuncs.com` |
| `region`            | bucket 所在地域 ID                                       | `cn-hangzhou`                          |
| `bucket`            | 已绑定图片域名的 bucket 名称                             | `your-image-bucket`                    |
| `domain`            | 一个精确图片域名，不带协议、路径或星号                   | `img.example.com`                      |
| `certificate_name`  | 6.2.1 查到的 `Certificate Name`，可能与域名不同          | `img.example.com-0001`                 |
| `access_key_id`     | 6.3.1 创建的 OSS 证书部署 AccessKey ID                   | 替换示例占位符                         |
| `access_key_secret` | 同一套 OSS 证书部署 AccessKey Secret                     | 替换示例占位符                         |

配置中的图片域名必须已绑定到这个 bucket；证书本身可以是覆盖它的通配符证书。配置文件包含密钥，只留在服务器，不提交或贴进日志。然后安装上传脚本：

```bash
sudo install -m 0644 docker/ingress/upload_oss_certificate.py \
  /opt/blog-platform/certificates/upload_oss_certificate.py
```

#### 6.3.3 检查本地配置和证书

```bash
sudo /opt/blog-platform/certificates/venv/bin/python \
  /opt/blog-platform/certificates/upload_oss_certificate.py \
  --config /etc/blog-platform/oss-certificate.json --check
```

成功标志：输出 `OSS certificate hook: local checks passed; no OSS request sent`。这一步只检查本地配置、证书与私钥是否匹配、证书是否覆盖图片域名且至少剩余一天；不调用 OSS，也不能证明云端权限正确。报错时先按提示修正，再继续。

#### 6.3.4 手动执行一次真实上传

下面这条命令没有 `--check`，会真实上传证书并替换 OSS 上该图片域名的当前证书：

```bash
sudo /opt/blog-platform/certificates/venv/bin/python \
  /opt/blog-platform/certificates/upload_oss_certificate.py \
  --config /etc/blog-platform/oss-certificate.json
```

成功标志：输出 `OSS certificate hook: deployed to ...; request_id=...`。这一步用于确认 RAM 权限、bucket 和域名绑定都正确。脚本先读取当前证书 ID，再附带该 ID 更新，避免覆盖同时被其他人修改的证书；不改 DNS、不上传公开 OSS 对象、不创建新域名绑定。

脚本使用官方 SDK 的 `list_bucket_cname` / `put_bucket_cname`，对应 [ListCname](https://help.aliyun.com/zh/oss/developer-reference/listcname) / [PutCname](https://help.aliyun.com/zh/oss/developer-reference/putcname) API。

#### 6.3.5 安装续签后自动上传的脚本

在仓库根目录执行：

```bash
sudo install -m 0755 docker/ingress/deploy-oss-certificate.sh \
  /etc/letsencrypt/renewal-hooks/deploy/30-blog-oss-certificate
sudoedit /etc/letsencrypt/renewal-hooks/deploy/30-blog-oss-certificate
```

在编辑器中检查脚本的三条绝对路径：虚拟环境 Python、已安装的上传脚本、私有 JSON 配置。使用示例路径时无需改；部署根目录不同则改成实际值，然后保存退出。后续配置写在私有副本中，不需要修改仓库内的示例脚本。

Certbot 续签成功后会调用这个脚本。脚本只处理 JSON 中 `certificate_name` 对应的证书（通过 `RENEWED_LINEAGE` 判断），其他证书会跳过。

失败时会返回非零退出码，并输出本地校验原因或脱敏的云端错误类型、状态码、错误码和 request ID，不输出私钥、AccessKey 或 API 正文。若证书已经续签但上传失败，Certbot 不会自动补跑这次部署；修复权限或网络后，手动重跑 6.3.4 的上传命令即可，不需要重新签证。到期和部署失败由监控告警负责。

### 6.4 一个定时器覆盖站点和 OSS 证书

手动命令成功后，还要让系统定时任务使用同一套工具。这一步不会新建第二个定时器，而是继续使用 Ubuntu 原有的 `certbot.timer`。

`certbot.timer` 负责定时唤起 `certbot.service`；`certbot.service` 再执行 `certbot renew`。如果 service 仍调用系统原来的 Certbot，就找不到刚安装在虚拟环境里的 DNS 插件。

#### 6.4.1 修改定时任务使用的 Certbot 路径

安装了 6.2 的虚拟环境后，执行：

```bash
sudo install -d -m 0755 /etc/systemd/system/certbot.service.d
sudoedit /etc/systemd/system/certbot.service.d/override.conf
```

在这个文件中填写以下内容；使用实际部署根目录替换 `/opt/blog-platform`。若文件已有其他配置，保留无关项，并将 `ExecStart` 调整为下列形式：

```ini
[Service]
ExecStart=
ExecStart=/opt/blog-platform/certificates/venv/bin/certbot -q renew
```

空的 `ExecStart=` 用于清除 service 原来的启动命令，下一行指定新的命令，两行都要保留。保存退出，然后让 systemd 重新读取配置并检查结果：

```bash
sudo systemctl daemon-reload
sudo systemctl cat certbot.service
sudo systemctl show certbot.service -p ExecStart
```

成功标志：`cat` 输出包含上面的 override，`show` 显示最终生效的 `ExecStart` 指向虚拟环境中的 Certbot。它仍读取同一套 `/etc/letsencrypt` 配置，因此网站的 webroot 证书和图片域名的 DNS 验证证书都由它处理；不要在另一套目录重复签发同一批证书。

#### 6.4.2 手动验证整个自动续期流程

先确认 6.1 的 Nginx hook 和 6.3 的 OSS hook 已安装，再执行：

```bash
sudo /opt/blog-platform/certificates/venv/bin/certbot renew \
  --dry-run --run-deploy-hooks
```

这条命令会测试登记的全部证书：网站证书通过 HTTP webroot 验证，图片证书通过 DNS 插件验证；成功后还会执行部署脚本。

其中 ACME 签发使用测试环境，但 deploy hook 使用当前有效的正式证书。也就是说，DNS TXT 会真实添加和清理，Nginx 会真实 reload，OSS API 会真实上传证书。先完成 6.3.3 的本地检查和 6.3.4 的首次上传，再执行本步骤。

成功标志：命令成功结束，所有待测证书续期测试成功，部署 hook 无报错。只看到 DNS 验证成功还不够，仍须检查 hook 的执行结果。

#### 6.4.3 确保定时器已开启，并查看执行记录

```bash
sudo systemctl enable --now certbot.timer
sudo systemctl list-timers --all certbot.timer
sudo journalctl -u certbot.service -n 50 --no-pager
```

`list-timers` 用于查看下次检查时间；`journalctl` 用于查看定时任务的执行日志。刚配置时可能还没有定时执行记录，前一步直接在终端运行的 dry-run 输出也不属于 service 日志。

完成后，平时不需要手动执行续期命令或添加 TXT。定时器定期检查证书，只有达到续签窗口时才重新申请，DNS 插件和部署脚本会在对应阶段自动运行。

#### 6.4.4 核对 OSS 实际提供的证书

在能访问图片域名的终端执行，将域名换成自己的值，查看 OSS 对外提供的证书：

```bash
openssl s_client -connect img.example.com:443 -servername img.example.com \
  -verify_hostname img.example.com -verify_return_error </dev/null 2>/dev/null \
  | openssl x509 -noout -subject -issuer -dates -fingerprint -sha256
```

在服务器执行下面的命令，查看本地证书指纹：

```bash
sudo openssl x509 \
  -in /etc/letsencrypt/live/实际证书名称/fullchain.pem \
  -noout -fingerprint -sha256
```

两次输出的 SHA-256 指纹应一致；云端部署可能有短暂传播时间。再在浏览器打开一张已知图片，确认 HTTPS 能正常访问。失败时先检查 hook 输出和云端部署状态，不用 `--force-renewal` 反复重签来重试上传。本站点和 OSS 的 HTTPS 到期检测都交给监控服务，部署失败也应告警。

完成标准：6.2 自动 DNS 验证成功、6.3 真实上传成功、6.4 完整 dry-run 和 hook 成功、定时器已开启，且图片域名实际提供的证书与本地一致。

后续维护时，保留虚拟环境、DNS 凭据文件和 OSS JSON 配置。`git pull` 不会更新安装在仓库外的文件：依赖变更后重新执行 6.2.2 的依赖安装；插件更新后重新安装本地扩展；上传脚本更新后重新执行 6.3.2 的脚本安装；hook 更新后重新安装并核对其中的实际路径。

## 7. 验证与日常启停

```bash
ing exec -T nginx nginx -t
sudo ss -lntp | grep -E ':(80|443)\b'
curl -I https://staging.example.com/
curl -I https://blog.example.com/
curl -sS -D - -o /dev/null https://staging.example.com/rss.xml
```

只在已启动的域名检查 200；staging 必须有 noindex，正常正式首页不能有 noindex。检查 `sudo docker ps` 的 Ports：只有共享入口发布 80/443，两个项目 Nginx 均无宿主机端口；验收 backend 仅有 `127.0.0.1:18080`。

验证伪造头无法改变项目识别的客户端 IP（在压测机执行，再在服务器看项目 nginx access log，应显示实际压测机 IP）：

```bash
curl -I -H 'X-Real-IP: 198.51.100.9' \
  -H 'X-Forwarded-For: 198.51.100.9' -H 'Forwarded: for=198.51.100.9;proto=http' \
  https://staging.example.com/
```

继续验证合法 1～10 MB 图片上传、HTTPS Secure Cookie、API 429 JSON，以及关闭 staging 后正式站仍可访问。共享入口也有 12m 请求体上限。两套项目同时运行时记录实际内存、CPU；正式业务在同机运行时避免容量极限压测，构建和备份错峰。

停止验收仍执行验收项目的 `dc down`，保留数据库卷；不要对共享入口执行 down。共享入口独立维护，模板修改后先生成配置并执行 nginx -t，再 reload；更换镜像、Compose 限额或域名环境变量时重建入口容器。原始日志、证书和私有配置不提交仓库。
