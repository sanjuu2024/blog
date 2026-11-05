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

正式项目使用独立检出目录及 `.env.prod`，避免测试版本覆盖正式容器挂载的配置。正式域名 DNS 就绪后，单独签证：

```bash
sudo certbot certonly --webroot -w /var/www/certbot \
  --cert-name blog.example.com -d blog.example.com
cp docker/ingress/templates/production.conf.template /opt/blog-platform/ingress/templates/
ing exec -T nginx sh -c '/docker-entrypoint.d/20-envsubst-on-templates.sh && nginx -t && nginx -s reload'
```

正式 `.env.prod` 的入口配置为：

```dotenv
BLOG_EDGE_NETWORK=blog-prod-edge
BLOG_WEB_ALIAS=blog-prod-web
BLOG_INGRESS_IP=172.30.10.2
```

使用正式数据库、凭据和管理员，先完成该库 bootstrap，再启动：

```bash
sudo docker compose --env-file /opt/blog-platform/production/.env.prod \
  -p blog-prod -f compose.prod.yaml up -d --wait --wait-timeout 180
```

入口使用 Docker DNS 动态解析项目别名，项目 Nginx 重建换 IP 后无需修改入口配置。项目停止时只影响对应域名，不影响另一个站点或 ACME 验证。

## 6. 证书自动续期

安装本仓库的续期成功 hook，脚本只检查并 reload 共享入口，不重启项目或复制私钥：

```bash
sudo install -d -m 0755 /etc/letsencrypt/renewal-hooks/deploy
sudo install -m 0755 docker/ingress/reload-after-renewal.sh \
  /etc/letsencrypt/renewal-hooks/deploy/20-blog-ingress-reload
sudo systemctl enable --now certbot.timer
sudo certbot renew --dry-run --run-deploy-hooks
sudo systemctl list-timers certbot.timer
```

hook 默认读取 `/opt/blog-platform/ingress/.env` 和 `compose.yaml`；若改部署目录，安装前同步修改脚本。只有 dry-run、hook 和定时器均验证成功才记录自动续期完成。webroot 不占用额外监听端口，staging 项目关闭时也可续证；共享入口、域名 DNS 和 HTTP 80 仍须可访问。旧证书如果使用 standalone，需先通过 `certbot reconfigure --cert-name 实际证书名 --webroot -w /var/www/certbot` 迁移验证方式并重新 dry-run。

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
