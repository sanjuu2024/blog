# P2：在目标服务器从零部署并验收

资源参考规格：4 vCPU / 8 GB RAM / 50 GB SSD；不是硬性服务器要求，按实际购置的规格执行并记录。以下以 Ubuntu 24.04 LTS、具有 sudo 权限且未加入 docker 组的普通用户为例；其他发行版使用对应的 Docker 安装步骤，不照搬 Ubuntu 软件源。

服务器运行共享入口、博客与采样，自己的电脑/WSL 运行 JMeter。测试数据放独立项目 `blog-p2-acceptance`，正式上线用 `blog-prod`，共享入口独立为 `blog-ingress`。P1 数据和脚本不参与本流程。两套项目可短时并行做功能预发布；正式业务同机运行时不进行容量极限压测，并记录其他实例和监控的资源开销。

验收项目的 PostgreSQL、Redis、backend 和项目 Nginx 均使用 `restart: "on-failure:3"`：不随服务器 / Docker 重启自动启动，主进程以非零退出码失败时最多重试 3 次，正常退出后不自动重启；健康检查失败本身不会触发重启，也不会联动重启其他容器。需要启动验收项目时手动执行 `dc up -d --wait --wait-timeout 180`。共享入口与正式项目仍使用 `unless-stopped`。如果服务器此前已创建验收容器，同步配置后执行一次上述 dc up，让新策略应用到已有容器；仅更新 YAML 不会改变服务器现有容器。

## 1. 登录服务器、网络与系统信息

先在服务商控制台配置 SSH 密钥及访问规则，SSH 仅允许自己的来源。共享入口开放 80/443，以支持访问和 ACME webroot 验证；staging 通过 noindex 禁止索引，若增加访问控制要保留 ACME 路径可用。数据库、Redis、backend 及两个项目 Nginx 不开放公网端口。

确认已用普通用户登录，`whoami` 显示 `deploy`（或你设置的部署用户名）、`sudo whoami` 显示 root，再继续。以下 Git、配置文件编辑、Python 和报告写入均由普通用户执行，只有系统操作及 Docker 命令通过 sudo 提权。已经完成的软件安装与系统检查不用重复执行。

登录后执行：

```bash
sudo apt-get update
sudo apt-get install -y ca-certificates curl git openssl python3 certbot sysstat
uname -a
cat /etc/os-release
lscpu
free -h
df -h
df -i
```

将实测填到清单 20.2，原 WSL 20 线程/15 GiB 记录只用于工具调试。记录额外 swap、镜像盘容量和可用空间；50 GB 系统盘尽量留 10～15 GB 空闲，性能报告主要保存在压测机。先不要进行磁盘压力或公网饱和流量测试。

## 2. 安装 Docker Engine 和 Compose

若 `sudo docker version`、`sudo docker compose version` 已正常工作，记录版本后跳过安装。Ubuntu 24.04 官方仓库安装示例：

```bash
sudo install -m 0755 -d /etc/apt/keyrings
sudo curl -fsSL https://download.docker.com/linux/ubuntu/gpg -o /etc/apt/keyrings/docker.asc
sudo chmod a+r /etc/apt/keyrings/docker.asc
echo "deb [arch=$(dpkg --print-architecture) signed-by=/etc/apt/keyrings/docker.asc] https://download.docker.com/linux/ubuntu $(. /etc/os-release && echo "$VERSION_CODENAME") stable" | sudo tee /etc/apt/sources.list.d/docker.list > /dev/null
sudo apt-get update
sudo apt-get install -y docker-ce docker-ce-cli containerd.io docker-buildx-plugin docker-compose-plugin
```

安装后验证，无需加入 docker 组或为此重新登录：

```bash
sudo docker version
sudo docker compose version
```

本流程保持 Docker 操作显式使用 sudo，不修改 Docker socket 权限，也不授予免密 sudo。访问控制以服务商安全组及实际监听地址一起核对，不只看 UFW 是否开启。

## 3. 检出验收版本，建立仓库外配置目录

```bash
sudo install -d -o "$(id -un)" -g "$(id -gn)" /opt/blog-platform
git clone YOUR_GIT_REMOTE /opt/blog-platform/repo
cd /opt/blog-platform/repo
# 替换为准备验收、CI 已通过且包含这些脚本的实际 Commit。
git switch --detach YOUR_ACCEPTANCE_COMMIT
git rev-parse HEAD
mkdir -p /opt/blog-platform/acceptance/secrets /opt/blog-platform/acceptance/evidence
chmod 700 /opt/blog-platform/acceptance /opt/blog-platform/acceptance/secrets
cp performance/p2/acceptance.env.example /opt/blog-platform/acceptance/.env.acceptance
chmod 600 /opt/blog-platform/acceptance/.env.acceptance
```

将 `YOUR_GIT_REMOTE` 替换为你有权访问的 Git remote。已有仓库时只需更新并切换目标版本，不重复 clone；不要覆盖未提交工作。正式压测前提交脚本，服务器和压测机检出同一 SHA。构建、备份与正式跑压错开执行。

## 4. 共享 Nginx、TLS 和环境变量

先按 [共享 Nginx 容器操作手册](./shared-nginx-ingress.md) 第 1～3 步创建独立入口、两个 edge 网络并签发 staging 证书，再继续本节。所有命令在服务器执行；不安装宿主机 Nginx，不让项目容器发布 80/443，不再复制证书给项目。正式站证书及项目可在之后单独接入。

示例域名为 `staging.example.com` / `blog.example.com`；在仓库外换成自己控制的域名。首次仅 DNS 解析、不套 CDN，未配置 IPv6 时不添加 AAAA。共享入口的配置模板和证书目录留在仓库外；真实域名与密钥不提交。

从 `performance/p2/acceptance.env.example` 复制的私有 `.env.acceptance` 中填写：

- 数据库名固定 `POSTGRES_DB=blog_p2_performance`；数据库、Redis、JWT 使用验收专用凭据。
- `BLOG_SITE_URL`、`BLOG_MAIL_FRONTEND_BASE_URL` 均为实际 staging HTTPS origin；Logo 可先用其 `/favicon.svg`；Turnstile hostname 与实际域名一致。
- `BLOG_EDGE_NETWORK=blog-staging-edge`、`BLOG_WEB_ALIAS=blog-staging-web`、`BLOG_INGRESS_IP=172.30.11.2` 与共享入口一致。若第 1 步因网段冲突更改了 IP，必须同步此处。
- 原 `NGINX_HTTP_PORT`、`NGINX_HTTPS_PORT`、`NGINX_TLS_CERT_DIR` 不再使用。项目 Nginx 不发布宿主机端口；应用并发测试仅使用 backend 的 `127.0.0.1:18080` SSH 隧道。
- OSS 配置在 Compose 与后端启动时必填，即使读取压测不上传。纯读取阶段可保留格式合法的 HTTPS endpoint、非空 bucket、凭据占位和 HTTP(S) public URL；图片上传验收需专用测试 bucket 的真实最小权限凭据，不用生产密钥。
- 读取阶段 Turnstile 关闭，无需有效 Cloudflare 密钥；Compose 与前端构建仍要求非空 secret、hostname 和 site key，保留验收占位值。占位值不能用于验收真实游客留言挑战。
- 邮件默认关闭；真实邮件、OSS 上传和 Turnstile 只做受控低频功能验收，性能替身未准备的并发场景保持未勾选。
- bootstrap 管理员示例为 `acceptance-admin`，部署者须确认邮箱归自己控制；初始化会直接标记邮箱已验证并记录 UTC 确认时间，不发送验证码，不自动修改已有管理员。密码放临时文件；seed 自动选择有效管理员。
- `VITE_SITE_LAUNCHED_AT` 填验收站起算时间；正式站首次公开运行起点另定，后续部署不重置。
- 所有 `CHANGE_ME` 和 example.com 项按实际配置处理。实际环境文件和压测 properties 保存在仓库外。

依次生成数据库密码、Redis 密码和 JWT 密钥，填入对应变量；不要把结果放进报告或提交仓库：

```bash
openssl rand -hex 24
openssl rand -hex 24
openssl rand -base64 48
```

JWT 密钥是 Base64 编码后的随机字节，与种子用户的 BCrypt 密码哈希无关。证书续期按共享入口手册第 6 步使用 Certbot webroot、deploy hook 和定时器；staging 项目关闭也能续证，证书更新只需 reload 共享入口。

## 5. 定义 Compose 命令，构建与初始化管理员

在服务器仓库根目录的当前 shell 定义：

```bash
dc() {
  sudo docker compose --env-file /opt/blog-platform/acceptance/.env.acceptance \
    -p blog-p2-acceptance -f compose.prod.yaml -f compose.acceptance.yaml "$@"
}
boot() {
  sudo docker compose --env-file /opt/blog-platform/acceptance/.env.acceptance \
    -p blog-p2-acceptance -f compose.prod.yaml -f compose.bootstrap.yaml \
    -f compose.acceptance.yaml "$@"
}
dc config -q
dc build postgres
dc build backend
dc build nginx
dc up -d --wait postgres redis
```

dc / boot 函数内部已使用 sudo，后文直接调用 `dc ...` / `boot ...`，不用写 `sudo dc`。新开服务器终端时需重新进入仓库并定义这些函数；不把整个 shell 切到 root。

首次构建会下载依赖并编译，可能耗时较长。镜像构建不受运行中容器 memory/cpus 配置完全约束；本机完成构建再传镜像也是可选流程，但必须记录镜像与 Commit 对应关系。

建立临时密码文件（输入不回显，不把密码直接写在命令行）：

```bash
read -rsp 'P2 bootstrap password: ' p2_bootstrap_password
printf '\n'
(umask 077; printf '%s' "$p2_bootstrap_password" > /opt/blog-platform/acceptance/secrets/bootstrap-password)
unset p2_bootstrap_password
boot config -q
boot run --rm --no-deps admin-bootstrap
```

看到 `ADMIN_BOOTSTRAP_SUCCESS` 才继续。此步骤执行 Flyway 并创建该独立库的管理员，不需要注册接口或 P1 初始化数据；一次性 bootstrap 关闭邮件与 Turnstile，不依赖外部挑战服务配置。不要绕过 Flyway 手动建表。用同一个文件再运行一次命令应明确拒绝已有管理员，验证后删除该临时文件：

```bash
rm /opt/blog-platform/acceptance/secrets/bootstrap-password
dc up -d --wait --wait-timeout 180
dc ps
```

删除的只是刚创建的明文临时密码文件；保存管理员凭据到自己的密码管理器。普通 dc 不使用 bootstrap secret 文件，之后不再执行 boot。

## 6. 冒烟与资源限制核对

```bash
curl -I https://staging.example.com/
curl -fsS 'https://staging.example.com/api/v1/articles?pageNum=1&pageSize=10'
for path in / /articles/1 /rss.xml /sitemap.xml /robots.txt; do
  curl -sS -D - -o /dev/null "https://staging.example.com${path}" | grep -i '^X-Robots-Tag:.*noindex'
done
curl -sS -D - -o /dev/null 'https://staging.example.com/api/v1/articles?pageNum=1&pageSize=10' | grep -i '^X-Robots-Tag:.*noindex'
dc exec backend curl -fsS http://localhost:8080/actuator/health/readiness
dc exec postgres sh -c 'psql -U "$POSTGRES_USER" -d blog_p2_performance -Atc "SELECT current_database()"'
sudo docker inspect "$(dc ps -q backend)" --format 'Memory={{.HostConfig.Memory}} NanoCpus={{.HostConfig.NanoCpus}}'
dc exec backend sh -c 'printf "%s\n" "$JAVA_TOOL_OPTIONS"'
dc stats --no-stream
```

空业务库文章列表应成功返回空页；通过浏览器登录后台、访问首页/归档/留言/关于/友链、检查 `/home` 的 308、无效文章的 404 Vue 布局、RSS 与 SEO。共享入口与验收项目均设置 noindex，入口生成的 502 等错误也禁止索引；正式正常首页不加此头。逐个验证首页、文章页、RSS、sitemap、robots 和 API 响应含 noindex 头；JMeter properties 保持 `requireNoindex=true`。按共享入口手册验证伪造 IP 头被覆盖、项目 access log 为真实客户端 IP。初始化、前端模板与静态资源均正常后再准备负载数据。

compose.prod.yaml 中的参考资源限制为：backend 2 CPU / 2560m、PG 1.5 CPU / 2g、Redis 0.25 CPU / 512m、项目 Nginx 0.25 CPU / 256m，一套约 5.25 GiB；共享入口另为 0.5 CPU / 256m，合计约 5.5 GiB 上限。两套项目并行需另计资源，不把上限当实际用量或预留。应根据实际服务器及压测结果复核；同机监控正常采集，未部署监控时注明缺少这部分负载并在接入后复测。本任务不新增 Prometheus/Grafana。

## 7. 独立 P2 数据与数据库基线

```bash
dc cp performance/p2/data/. postgres:/tmp/blog-p2-data
dc exec -T postgres sh -c 'psql -U "$POSTGRES_USER" -d blog_p2_performance -v ON_ERROR_STOP=1 -f /tmp/blog-p2-data/seed.sql' \
  > /opt/blog-platform/acceptance/evidence/seed.txt
```

检查命令退出码和 seed.txt 中数据量：100 公开文章（多年份）、2000 评论、500 留言、100 测试用户、1200 通知、21000 收件、300 投递读取样本、2000 安全事件、点赞及每日统计。seed 重跑会删除本轮 `[P2-PERF]` 等专用标识的数据，再从零创建；只允许 `blog_p2_performance`，不调用 P1 SQL。不要在负载运行时 seed，也不要把验收数据迁移到正式库。

把 seed 输出真实 ID 写进压测机私有 `p2.properties`。普通测试账号为 `p2_perf_user_001` / `p2_perf_user_002`，公开固定测试密码 `123456`；因此验收入口应受控，正式环境不使用这些账号。投递 seed 仅供读取，不能作为有效重试来源。

每个写入场景前后保存只读快照：

```bash
dc exec -T postgres sh -c 'psql -U "$POSTGRES_USER" -d blog_p2_performance -f /tmp/blog-p2-data/verify.sql' \
  > /opt/blog-platform/acceptance/evidence/before.txt
# 完成对应阶段后，改成不同的 after-场景-阶段.txt 再执行。
```

## 8. 开始资源采样

服务器另开 SSH，以同一普通用户进入仓库，先完成该终端的 sudo 认证：

```bash
sudo -v
python3 performance/p2/collect_resources.py \
  --sudo-docker --project blog-p2-acceptance \
  --out /opt/blog-platform/acceptance/evidence/resources
```

`--sudo-docker` 只让脚本内部的 Docker 命令使用 `sudo -n docker`，Python 和输出文件仍属于普通用户。采样时不弹密码提示；若 sudo 认证失效，会报错或记录 collectionError，应停止采样、在同一终端重新 `sudo -v` 后重启，不能把缺失样本当正常结果。不需要执行 `sudo python3`。

采样期间不构建、不备份；结束 Ctrl+C。CSV 包含整机、容器、Java RSS、数据库连接和 Redis 内存，限制 JSON 便于复核配置。GC/heap 可通过私有覆盖追加 `-Xlog:gc*:stdout:time,uptime,level,tags`，保持已有四个 JVM 限制，并重建 backend 后记录这组参数。GC 日志保存到仓库外；Java RSS 不能代替堆。

共享入口也占用整机资源；另用 `ing stats --no-stream`、`ing ps` 记录入口的内存、CPU 与状态。`ing` 定义见共享入口手册，整机采样包含其开销，不把项目四个容器之和当成整机总占用。

磁盘等待、交换活动和网卡吞吐在另外三个服务器终端采集，完成后各按 Ctrl+C；首次报告可能是开机以来平均值，分析时使用后续区间：

```bash
iostat -xz 5 > /opt/blog-platform/acceptance/evidence/iostat.txt
```

```bash
vmstat 5 > /opt/blog-platform/acceptance/evidence/vmstat.txt
```

```bash
sar -n DEV 5 > /opt/blog-platform/acceptance/evidence/network.txt
```

三条命令分别持续运行，不能在同一个终端依次等待它们自动结束。结束时再记录 df -h / df -i 和服务商流量面板；报告文件不放仓库。

## 9. 压测机安装工具和配置

推荐自己的 Windows/WSL 运行，不能在这台被测服务器运行完整 JMeter 后再声称没有工具开销。需要 Java 21、Python 3.10+ 和 JMeter 5.6.3；下载 Apache 官方发行包并校验其 SHA512，把 bin 加 PATH。先确认：

```bash
java -version
python3 --version
jmeter --version
```

压测机检出同一个 Commit，在仓库外复制 `performance/p2/p2.properties.example` 并填写：host=staging.example.com、protocol=https、port=443、layer=nginx、管理员凭据、seed 真实 ID、targetCommit=服务器 SHA。`year=2026` 可命中当前种子，默认时区包含上海/UTC/洛杉矶。配置和结果不能放仓库内；Linux 配置设 600。自己的电脑/WSL 运行 JMeter 不需要 sudo。

从压测机仓库根目录先短测：

```bash
python3 performance/p2/run.py --properties /home/loadtester/blog-p2/p2.properties \
  --out /home/loadtester/blog-p2/results --scenarios P2-PERF-01,P2-PERF-02 \
  --warmup-seconds 2 --run-seconds 5 --formal-runs 1 --ramp-seconds 1
```

短测不作为验收结果。Windows PowerShell 使用 `python`，示例路径换成 `C:/blog-p2/p2.properties`、`C:/blog-p2/results`；JMeter 不在 PATH 时增加 `--jmeter D:/Tools/apache-jmeter-5.6.3/bin/jmeter.bat`。其余参数相同。

## 10. 正式读取：全部重新执行

```bash
python3 performance/p2/run.py --properties /home/loadtester/blog-p2/p2.properties \
  --out /home/loadtester/blog-p2/results --html-report
```

默认执行 12 组基础/扩展读取，每组预热 120 秒、正式 300 秒 × 3 轮，合计约 204 分钟，另加登录和报告生成。可用 `--scenarios` 分批运行。不用 P1 的成功记录代替本轮。p50/p95/p99、RPS 分别取三轮中位数，保留最差轮及所有错误；不平均百分位，任一轮错误都不能被中位数掩盖。

请求经压测机到服务器的公网 HTTPS，延迟包含网络路径开销；记录空载延迟和不同时段的差异。不要临时放宽清单阈值来让结果变绿。需要定位服务端处理能力时另跑 SSH 隧道基线或服务器内单请求诊断，结果分开记录。

## 11. 应用并发与 Nginx 边缘分开

在压测机开另一个终端保持隧道：

```bash
ssh -N -L 18080:127.0.0.1:18080 deploy@SERVER_IP
```

将 `deploy` 和 `SERVER_IP` 替换为实际部署用户名及服务器公网 IPv4。复制私有 properties 成 application.properties，改成 host=127.0.0.1、port=18080、protocol=http、layer=application、allowWrites=true。测试流量在 SSH 加密隧道里，不公开后端端口。先保存数据库基线，再执行：

```bash
python3 performance/p2/run.py --properties /home/loadtester/blog-p2/application.properties \
  --out /home/loadtester/blog-p2/results \
  --scenarios P2-BASE-07,P2-PERF-06,P2-PERF-07,P2-PERF-08 --pause-between-phases
```

阶段暂停时在服务器保存 SQL 快照再继续：浏览去重 +1；单主体点赞 POST 后一条、DELETE 后零条；多主体阶段 +10。查看 metadata.json 的具体文章/用户 ID、visitorHash，不能只看总数；脚本只给 HTTP_PASS/DB_PENDING。

P2-PERF-10 需 allowAllUsers=true，安排在读取测试之后，独立执行：

```bash
python3 performance/p2/run.py --properties /home/loadtester/blog-p2/application.properties \
  --out /home/loadtester/blog-p2/results --scenarios P2-PERF-10 --pause-between-phases
```

全员消息覆盖全部未注销用户（含禁用用户），排除发送管理员；通过 SQL 的 expected_recipients / actual_recipients 和 MISSING / UNEXPECTED 空结果核对完整集合。全部已读后核对目标分类未读为零，另一个用户未读不变。

P2-BASE-08 要可控 Turnstile 替身；P2-PERF-09 要本地 SMTP 和有效 FAILED 来源。当前默认部署关闭外发服务，两项未准备时留空；真实 Cloudflare/Resend/OSS 只低频功能验收，不压真实第三方。详细前提见 performance/p2/README.md。

Nginx 边缘限流另经 HTTPS 同来源突发测试，拒绝应为 429/199429；不能要求严格 1 成功9拒绝，因为边缘规则包含 rate 与 burst。应用层测试不能拿这个结果充数。N+1 另以单请求 SQL 日志比较 pageSize=10/20，正式压测时不启用大量 SQL DEBUG。

## 12. 容量与稳定性

```bash
python3 performance/p2/run.py --properties /home/loadtester/blog-p2/p2.properties \
  --out /home/loadtester/blog-p2/results --scenarios P2-BASE-09,P2-PERF-12 --html-report
```

先执行 10 RPS / 30 分钟固定基线，再以 5/10/20/40 RPS 各 5 分钟升档，每档先预热；首次 HTTP、延迟或吞吐不足停止升档，最后 HTTP 稳定档再跑 30 分钟。CPU 持续超过 70%、内存持续超过 80%、OOM/重启、连接耗尽或持续 swap 也应人工停止；脚本不能从压测机自动判定所有服务器资源。

参考资源规格是否足够用实际稳态判断，不以套餐宣传值推定；40 RPS 通过只写“至少达到40 RPS”。检查服务商带宽、流量上限及计费方式，不进行未获授权的持续饱和测试。

## 13. 填表与结束验收

压测机每次输出 run-results.csv、summary.csv、summary.md、JTL、日志；指定 --html-report 后另有 HTML 仪表板。资源采样在服务器，按时间关联；把 summary.md 行复制到 20.5，补资源、SQL 数量、数据库差值与证据路径。原始报告/凭据不提交；未执行项不勾选。

按第21～23节继续完成升级/备份恢复、TLS续证、真实少量邮件与OSS/Turnstile、故障恢复和上线告警。Prometheus/Grafana 尚未配置时不能记录“带监控运行通过”。

验收结束：

```bash
dc down
```

这只停止验收项目，保留数据库卷、报告及入口管理的外部网络；不加 -v，不停止 blog-ingress。按共享入口手册第 5 步在独立检出目录准备正式 `.env.prod`、正式证书、网络配置及新数据库 `blog_prod`、凭据和管理员；正式配置不叠加 compose.acceptance.yaml，不复制性能卷或测试密码。之后可在独立 staging 项目验收新版本，但不要改动正式容器挂载的配置。保留相容镜像用于回滚。
