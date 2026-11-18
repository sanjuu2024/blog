# Cloudflare DNS 与表情 CDN 迁移手册

适用于已将域名添加至 Cloudflare，并完成权威 nameserver 切换的部署。下文以 ccsanjuu.com 和 sticker.ccsanjuu.com 为例，请替换为实际域名。本流程为表情域名启用代理，并迁移已有 OSS 证书的 DNS 验证方式。主站与 OSS 域名的原代理策略保持原样。所有服务器命令在新版仓库根目录执行，按实际目录替换下列两个变量，不读取或输出私有环境文件。

```bash
ingress_root=/opt/blog-platform/ingress
certificates_root=/opt/blog-platform/certificates
```

如果你的入口实际位于 /opt/ingress，则只将 ingress_root 改成该目录，保留现有 compose.yaml、.env、templates 和 snippets。

## 1. 配置分别放在哪里

| 配置 | 位置 | 生效时机 |
| --- | --- | --- |
| 公开表情 CDN 地址 | frontend/.env.production 的 VITE_STICKER_BASE_URL | 前端构建时，修改后重新部署 nginx 镜像 |
| 开发／测试同源表情 | 不配置或将 VITE_STICKER_BASE_URL 设为空 | 前端构建／开发启动时 |
| 入口域名 | 仓库外 ingress/.env 的 INGRESS_STICKER_HOST | 重建共享入口容器后 |
| Cloudflare DNS API Token | /etc/letsencrypt/cloudflare-dns.ini，root 0600 | Certbot 进行 DNS 验证时 |
| OSS 证书部署 AccessKey | 原 /etc/blog-platform/oss-certificate.json | OSS deploy hook 执行时 |

前端 .env.production 不用于 Docker Compose 或 Certbot。VITE_ 值进入公开 JavaScript；不要向其中写入 Cloudflare Token、OSS 密钥或私钥。frontend/.dockerignore 排除 .env.local 和 .env.*.local、node_modules、dist，生产公开配置仍会进入构建阶段。

现有 frontend/Dockerfile 已构建前端并复制静态资源，不需要新增入口 Nginx Dockerfile，也不需要再复制一份表情到宿主机。新增模板通过共享入口原有的 templates 挂载读取。Dockerfile 提供可选的 VITE_STICKER_BASE_URL build arg；未传时使用 .env.production。需要同源 Docker 构建时可显式执行：

```bash
prod build --build-arg VITE_STICKER_BASE_URL= nginx
```

prod 是已有生产 Compose 函数；不要为此重新定义项目名或更换数据库卷。

## 2. 安装官方 Cloudflare DNS 插件

继续使用原证书虚拟环境和 /etc/letsencrypt，不新建第二套证书存储。Cloudflare DNS 插件用于迁移原本依赖 DNS 验证的 OSS 证书；blog 和本次新增 sticker 的证书均采用 HTTP-01 webroot，不依赖 DNS 插件，同样由 Certbot 自动续期。requirements 保留旧阿里云插件，以便尚未迁 DNS 的证书继续续签；不再需要为 Cloudflare 编写自定义 DNS 插件。

```bash
sudo "$certificates_root/venv/bin/pip" install -r docker/ingress/requirements-certificates.txt
sudo "$certificates_root/venv/bin/certbot" plugins
sudo "$certificates_root/venv/bin/certbot" certificates
```

插件列表应包含 dns-cloudflare。若尚无虚拟环境，先按共享入口手册 6.2.2 创建，再安装本 requirements。官方插件和 Certbot 均固定为 5.9.0。

可先执行不连接 DNS、ACME 或 OSS 的迁移脚本测试：

```bash
python3 -B -m unittest discover -s docker/ingress -p test_cloudflare_migration.py -v
```

在 Cloudflare 创建 API Token，权限使用 Zone / DNS / Edit 和 Zone / Zone / Read，资源范围限制到 ccsanjuu.com；如果一张证书还包含其他 zone 的域名，Token 也需覆盖对应的权威 zone。Token 的实际值只写进服务器凭据文件：

```bash
sudo install -d -m 0700 /etc/letsencrypt
if ! sudo test -e /etc/letsencrypt/cloudflare-dns.ini; then
  sudo install -m 0600 docker/ingress/cloudflare-dns.ini.example /etc/letsencrypt/cloudflare-dns.ini
fi
sudoedit /etc/letsencrypt/cloudflare-dns.ini
sudo chmod 600 /etc/letsencrypt/cloudflare-dns.ini
```

在编辑器中将 dns_cloudflare_api_token 的 CHANGE_ME 值替换为 Token。不要把 Token 放在命令行参数、截图或终端输出中。

## 3. 迁移已有 OSS 域名证书

从 certbot certificates 查出覆盖 OSS 域名的实际 Certificate Name，可能带 -0001 后缀。下列示例 img.ccsanjuu.com 必须替换为查到的名称；保持现有 OSS JSON 的 certificate_name 不变。

```bash
sudo install -m 0644 docker/ingress/migrate_cloudflare_dns.py "$certificates_root/migrate_cloudflare_dns.py"
sudo "$certificates_root/venv/bin/python" "$certificates_root/migrate_cloudflare_dns.py" \
  --cert-name img.ccsanjuu.com \
  --credentials /etc/letsencrypt/cloudflare-dns.ini \
  --certbot "$certificates_root/venv/bin/certbot" --check
```

--check 只检查本地输入，不调用 DNS、ACME 或 OSS。确认通过后去掉 --check：

```bash
sudo "$certificates_root/venv/bin/python" "$certificates_root/migrate_cloudflare_dns.py" \
  --cert-name img.ccsanjuu.com \
  --credentials /etc/letsencrypt/cloudflare-dns.ini \
  --certbot "$certificates_root/venv/bin/certbot"
```

脚本使用 Certbot reconfigure，先向测试 ACME 环境验证 DNS 插件，成功后由 Certbot 保存新的续期参数；不直接编辑 renewal/*.conf，不更改证书名称、域名列表或私钥，不 force-renewal。可以重复传 --cert-name 迁移其他已使用 DNS 验证的证书，任意一个失败即停止。仍使用 HTTP webroot 且验证正常的主站／staging 证书不必改为 DNS 验证。

随后单独验证迁移结果：

```bash
sudo "$certificates_root/venv/bin/certbot" renew --cert-name img.ccsanjuu.com --dry-run
```

DNS 验证会真实添加和清理测试 TXT，但使用 ACME 测试环境；原正式证书继续服务。旧 dns-aliyun-safe 凭据仅在确认所有相关证书均已迁移后再另行处理，不提前删除。

OSS 的证书上传 hook 继续保留。DNS 迁移只改变“如何证明域名归属”，没有把 OSS 的证书自动交给 Cloudflare 管理。确认原本的 upload_oss_certificate.py --check 和真实上传均成功后，再验证包含部署 hook 的完整流程：

```bash
sudo "$certificates_root/venv/bin/certbot" renew --dry-run --run-deploy-hooks
sudo systemctl show certbot.service -p ExecStart
sudo systemctl list-timers --all certbot.timer
```

--run-deploy-hooks 会真实 reload 入口，并可能向 OSS 上传当前有效的正式证书。原 certbot.service override 应继续指向该虚拟环境；不要切回缺少插件的系统 Certbot。新 sticker 证书触发 OSS hook 时，上传器会根据 RENEWED_LINEAGE 跳过无关证书。

## 4. 为 sticker 域名准备源站 HTTPS

sticker 沿用主站的 HTTP-01 webroot：Certbot 将验证文件写入宿主机 /var/www/certbot，共享入口通过已挂载目录返回文件。插件不参与这张证书的验证，不需要再建定时任务。自动续期依赖入口正常运行、HTTP 80 可访问，以及验证路径不被跳转、登录、WAF 或缓存规则阻断。

在 Cloudflare DNS 添加 A 记录 sticker，指向源站 IPv4；首次签发先设 DNS only。未配置 IPv6 时不添加 AAAA。在原仓库外 ingress/.env 增加 INGRESS_STICKER_HOST=sticker.ccsanjuu.com。更新 compose.yaml 时保留你现有的网络、子网和路径调整；必须补入本次新增的 INGRESS_STICKER_HOST environment 和 NGINX_ENVSUBST_FILTER 名称。所有 Nginx 模板中的 $uri、$status 等原生变量不能被 envsubst 清空。

先更新 default.conf.template，让 sticker 的 HTTP 验证路径生效；它不引用 sticker 证书，可以在首次签发前加载。下面的 cp 仅适用于默认模板没有私有改动的部署；若服务器上有调整，用编辑器保留原内容，仅补入本次新增的 sticker HTTP server。然后重建入口，使新增环境变量生效：

```bash
cp docker/ingress/templates/default.conf.template "$ingress_root/templates/"
ing config -q
ing up -d --wait --wait-timeout 120
ing exec -T nginx nginx -t
```

先确认 HTTP 验证目录能从公网访问；/var/www/certbot 必须与 ingress/.env 中 INGRESS_ACME_DIR 指向同一个宿主机目录，若原来使用其他路径则沿用实际路径。创建临时探针，从服务器外部请求应返回 acme-probe，然后删除探针：

```bash
sudo install -d -m 0755 /var/www/certbot/.well-known/acme-challenge
printf '%s\n' acme-probe | sudo tee /var/www/certbot/.well-known/acme-challenge/sticker-probe >/dev/null
curl --fail http://sticker.ccsanjuu.com/.well-known/acme-challenge/sticker-probe
sudo rm /var/www/certbot/.well-known/acme-challenge/sticker-probe
```

验证通过后签发证书：

```bash
sudo "$certificates_root/venv/bin/certbot" certonly \
  --webroot -w /var/www/certbot \
  --cert-name sticker.ccsanjuu.com -d sticker.ccsanjuu.com
```

确认 /etc/letsencrypt/live/sticker.ccsanjuu.com/fullchain.pem 和 privkey.pem 已存在后，再安装 HTTPS 模板；模板不能提前引用不存在的证书。

```bash
cp docker/ingress/templates/sticker.conf.template "$ingress_root/templates/"
```

模板与已有 production.conf.template 共用 blog-prod-edge，回源 blog-prod-web:80。路径内部映射为 /stickers/，保留编码文件名和 v 参数，仅允许 PNG、GIF、WebP 的 GET/HEAD；Cookie 和 Authorization 不向静态回源传递。不需要给共享入口挂载博客静态目录或增加新 Docker 网络。

先完成下一节的项目 Nginx 更新，再生成 HTTPS 配置、检查并 reload：

```bash
ing exec -T nginx sh -c '/docker-entrypoint.d/20-envsubst-on-templates.sh && nginx -t && nginx -s reload'
```

启用 HTTPS 和橙云后，再验证这张证书的自动续期及现有 reload hook：

```bash
sudo "$certificates_root/venv/bin/certbot" renew \
  --cert-name sticker.ccsanjuu.com --dry-run --run-deploy-hooks
```

Cloudflare 的代理本身不要求切换为 DNS 验证。让 HTTP 的 `/.well-known/acme-challenge/` 直达源站，不为它配置强制跳转、质询或缓存规则；若启用了 Cloudflare 的全站重定向功能，为此路径排除。DNS-01 是可选替代方案，只有不方便开放 80 或使用通配符证书等场景才需要另行选择。

## 5. 更新目录、缓存规则及应用

在 frontend 目录执行 pnpm stickers:generate、pnpm stickers:check、pnpm stickers:test。生成目录仍返回 /stickers/ 地址，但每个图片文件都有稳定的 SHA-256 前 16 位 v 参数；不把生产域名写入后端 classpath，开发、测试和生产使用同一套目录。

先更新并部署前后端资源，项目 Nginx 需要加载新版 docker/nginx/default.conf；acceptance 使用对应的 default.acceptance.conf。生产构建通过 frontend/.env.production 读取 CDN 域名，所以先确认新域名已能正常返回图片，再对外切换新版前端。

为了先更新源站、再切换浏览器请求，可以分两步部署：先用本手册第 1 节的空 build arg 构建同源版本并部署，确认源站缓存与 CDN 入口；随后构建不传该 arg 的正常生产版本，用新镜像标签发布。也可以在维护窗口内按依赖顺序完成一次发布。沿用原 db-prepare 流程，本次无新 migration。

成功响应设置 Cache-Control: public, max-age=86400, s-maxage=604800；错误包括 404 设置 no-store。源站 /stickers/ 不再回退 index.html。CDN 入口不会代理 HTML、API 或任意其他站点路径。

在 Cloudflare 中确认 Universal SSL 边缘证书已激活，SSL/TLS 使用 Full (strict)，然后将 sticker A 记录改为 Proxied（橙云）。不要为整个 ccsanjuu.com 配置 Cache Everything。可不新增 Cache Rule，PNG、GIF、WebP 默认可缓存；若设置专用规则，只匹配 hostname=sticker.ccsanjuu.com 并遵守源站 TTL。保持标准缓存键包含查询参数，不能启用忽略查询参数或忽略 v 的配置。

普通 img 跨域展示不需要额外 CORS，也不携带认证请求头；不改站点 Cookie 域。若未来需要 Canvas 读取图片像素，再单独定义 CORS。

## 6. 验证和回滚

以下路径换成新版目录中的真实文件和 v 值，不使用本节示例 v 作为实际版本：

```bash
curl -I 'https://sticker.ccsanjuu.com/bilibili/btv_doge.png?v=实际版本'
curl -I 'https://sticker.ccsanjuu.com/bilibili/btv_doge.png?v=实际版本'
curl -I 'https://sticker.ccsanjuu.com/bilibili/missing.png'
curl -I 'https://sticker.ccsanjuu.com/api/v1/users/me'
```

成功图片必须为 image/*、带正确 Cache-Control；首次可能 CF-Cache-Status: MISS，后续同区域可能 HIT 并出现 Age。不存在的图片必须返回 404 和 no-store，不能返回状态为 200 的 SPA 首页；Nginx 自带的 HTML 格式 404 错误页是正常的。API 路径同样拒绝访问。测试真实中文文件名和 GIF 动画，浏览器控制台不应出现 404 或证书错误。

浏览器切库会重新创建 img，但不是每次都重传图片：仍在一天新鲜期内可读内存／磁盘缓存；过期后可能发 If-None-Match / If-Modified-Since 并得到 304。浏览器真的发出请求后，Cloudflare HIT 可避免回源；MISS 才取源站。没有 Cache-Control 时也可能有启发式缓存或 CDN 默认缓存，不能据此保证每次切库都下载，也不能保证缓存多久。开发者工具开启 Disable cache 会改变这项测试结果。

替换图片后必须重新生成目录并同步发布；内容变化会生成新 v，浏览器与边缘使用新键，不需要等七天。紧急修复仍可在 Cloudflare 对精确 URL 清缓存；清除边缘不会自动清掉用户尚未过期的浏览器副本，所以版本参数仍然必要。CDN 只减少请求与回源成本，动画解码开销依然存在。

CDN 不可用时，将 VITE_STICKER_BASE_URL 设为空并重建前端，回到保留的同源 /stickers/ 入口；不修改数据库或旧评论标记。不要先删除源站资源。DNS 仍托管于 Cloudflare 时，不应将 Certbot 验证器回滚到旧阿里云插件。

## 参考

- 官方插件：https://certbot-dns-cloudflare.readthedocs.io/
- Certbot reconfigure：https://eff-certbot.readthedocs.io/en/stable/using.html#modifying-the-renewal-configuration
- 缓存默认行为：https://developers.cloudflare.com/cache/concepts/default-cache-behavior/
- Full (strict)：https://developers.cloudflare.com/ssl/origin-configuration/ssl-modes/full-strict/
