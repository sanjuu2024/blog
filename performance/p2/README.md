# 独立 P2 压测套件

从服务器初始化开始请按 [P2 服务器验收手册](../../docs/P2-server-acceptance.md) 执行。4 vCPU / 8 GB / 50 GB SSD 是资源参考规格；项目资源限制在 compose.prod.yaml，验收与正式部署使用相同参数。先按 [共享入口手册](../../docs/shared-nginx-ingress.md) 启动独立 Nginx 容器，公网 TLS 经独立 edge 网络转发到项目 HTTP Nginx；项目不映射宿主机端口、不挂载证书。compose.acceptance.yaml 隔离数据和外发行为，入口与验收项目均禁止索引。此目录不调用 performance/p1/ 下的 P1 文件。

```text
performance/p2/
  acceptance.env.example   服务器 Compose 输入示例
  p2.properties.example    压测机 JMeter 配置示例
  read.jmx                 所有读取计划
  concurrency.jmx          应用层并发计划
  p2-*.groovy              初始化、请求选择、断言和并发核对
  run.py                   跨平台运行、汇总和 CSV/Markdown 导出
  collect_resources.py     被测 Linux 主机资源采样
  test_runner.py           汇总测试与真实 JMeter 的本地替身自检
  data/{seed,cleanup,verify}.sql  独立数据与只读快照
```

## 配置与数据

实际 .env、properties、JTL、Token/Cookie 和报告只保存在仓库外。服务器与压测机使用同一已提交 SHA，properties 的 targetCommit 填服务器 git rev-parse HEAD。数据库必须是独立 `blog_p2_performance`，数据以 `[P2-PERF]`、`p2_perf_user_`、`p2-perf-article-` 标识；seed 自行生成100文章、2000评论、500留言、100用户、通知/点赞/每日统计/投递/安全事件，不包含 P1 数据。`staging.example.com`、`deploy`、`/opt/blog-platform` 和 `/home/loadtester/blog-p2` 均为文档示例，部署时换成自己的值。

seed.sql 只 include 同目录 cleanup.sql；迁移必须由 Flyway 完成，脚本不改变表结构。先 bootstrap 有效管理员，seed 自动选择，不强制叫 admin。基础用户密码123456仅供隔离测试。seed 的邮件数据仅用于读取，不调度发信，也不能用来证明邮件重试。

## 请求计划与轮数

普通读取预热120秒、30秒升压、正式300秒×3轮，指标各取中位数，保留最差轮与所有错误；预热只检查功能。并发各阶段1轮，固定基线稳定性1800秒1轮；容量每档300秒，最后稳定档1800秒。短调试不是验收证据。

| 场景           | 线程 / 总RPS         | 内容                                                            |
| -------------- | -------------------- | --------------------------------------------------------------- |
| P2-BASE-01～05 | 10 / 10、10、5、5、5 | 文章列表、详情、中文搜索、评论、留言                            |
| P2-BASE-06     | 20 / 15              | 列表35%、详情30%、搜索15%、评论10%、留言10%                     |
| P2-BASE-07     | 10并发               | 同用户同文章：1成功、9个429/106004，SQL新增1                    |
| P2-BASE-08     | 10并发               | 同来源游客留言：1成功、9个429/108004，SQL新增1；须Turnstile替身 |
| P2-BASE-09     | 10 / 10              | 固定混合浏览30分钟                                              |
| P2-PERF-01     | 10 / 5               | 年份汇总，上海/UTC/洛杉矶轮换                                   |
| P2-PERF-02     | 10 / 10              | 年/分类/标签分页，pageSize10/20、页码1/2                        |
| P2-PERF-03     | 5 / 2                | 管理员Dashboard                                                 |
| P2-PERF-04     | 10 / 10合计          | 两用户通知列表/未读数                                           |
| P2-PERF-05     | 5 / 5合计            | 投递、安全事件与筛选                                            |
| P2-PERF-06     | 10并发               | 共享Cookie访问独立文章，浏览/当日聚合只+1                       |
| P2-PERF-07     | 每阶段10并发         | 单游客POST→DELETE→10游客POST，分阶段SQL核对                     |
| P2-PERF-08     | 1线程31次            | 同用户交替文章/评论点赞，60秒共享额度；前30成功、31次103010     |
| P2-PERF-09     | 10并发               | FAILED投递，1成功、9个400/199001；须本地SMTP及有效来源          |
| P2-PERF-10     | 每阶段10并发         | ALL_USERS草稿并发发布：1成功9拒绝；全部已读验证不串用户         |
| P2-PERF-11     | 10 / 5合计           | 首页、文章HTML、sitemap、RSS、友链；不自动加载JS/图片           |
| P2-PERF-12     | 20 / 5→10→20→40      | 公开混合容量探测及最后稳定档30分钟                              |

默认只运行12组读取（BASE01～06和PERF01～05/11），约204分钟，初始化/HTML导出另计。不默认发起写入或调用外部服务。性能 GET 详情会按实际访客写浏览，visitorMode=per-thread 固定每线程主体，fresh 每次新访客；两者结果必须区分，不能与 P1 无Cookie结果混淆。

## 命令

仓库根目录执行。Linux 使用 python3，以下压测机路径以 `/home/loadtester/blog-p2` 为例；Windows 使用 python，路径换成 `C:/blog-p2/...`，必要时加 `--jmeter D:/Tools/apache-jmeter-5.6.3/bin/jmeter.bat`。

```bash
# 短调试：不满足验收时长。
python3 performance/p2/run.py --properties /home/loadtester/blog-p2/p2.properties \
  --out /home/loadtester/blog-p2/results --scenarios P2-PERF-01,P2-PERF-02 \
  --warmup-seconds 2 --run-seconds 5 --formal-runs 1 --ramp-seconds 1

# 全部读取，三轮中位数；HTML报告可选。
python3 performance/p2/run.py --properties /home/loadtester/blog-p2/p2.properties \
  --out /home/loadtester/blog-p2/results --html-report

# 应用层并发需SSH隧道、layer=application、allowWrites=true。
python3 performance/p2/run.py --properties /home/loadtester/blog-p2/application.properties \
  --out /home/loadtester/blog-p2/results --scenarios P2-BASE-07,P2-PERF-06,P2-PERF-07,P2-PERF-08 \
  --pause-between-phases

# 30分钟固定基线、容量探测和30分钟稳定档。
python3 performance/p2/run.py --properties /home/loadtester/blog-p2/p2.properties \
  --out /home/loadtester/blog-p2/results --scenarios P2-BASE-09,P2-PERF-12 --html-report

# 重新汇总已有结果。
python3 performance/p2/run.py --summarize /home/loadtester/blog-p2/results/实际时间戳目录
```

P2-PERF-10 另设 allowAllUsers=true，安排在通知读取以后。P2-BASE-08 需 turnstileStub=true 和有效可控测试 token；禁止把关闭挑战的结果计作完整场景。P2-PERF-09 需 smtpStub=true、仅本地SMTP的部署覆盖和真实有效FAILED来源；保持整个并发批次PENDING（如替身延迟完成），防止瞬间失败后产生新的合法重试。默认acceptance关闭外发邮件；真实Resend/Cloudflare/OSS不参与高并发。

Nginx边缘限流另经HTTPS突发验证429/199429；应用并发只经SSH后端，运行器拒绝layer=nginx的写入一致性计划。不能用边缘拒绝、验证码失败充当应用限流通过。

## 导出与核对

每轮JTL、日志、metadata.json及run-results.csv总会保存；summary.csv/summary.md为可填写表，指定--html-report才生成HTML，省略可减少本机报告开销。CSV不保存响应正文/头/Token/Cookie，metadata只含测试标识、资源ID和访客哈希。主机资源用另一个服务器终端执行：

```bash
sudo -v
python3 performance/p2/collect_resources.py --sudo-docker --project blog-p2-acceptance \
  --out /opt/blog-platform/acceptance/evidence/resources
```

普通用户不加入 docker 组；`--sudo-docker` 使内部调用使用 `sudo -n docker`，Python 与报告仍由普通用户运行和持有。认证失效时先停止采样，在同一终端重新 `sudo -v` 再启动，不在后台等待密码。

共享入口独立运行，另用共享入口手册的 `ing stats --no-stream` 记录；整机 CSV 包含它和其他项目的总开销。staging 停止后共享入口仍运行，证书续期不依赖项目。公网读取保留 `requireNoindex=true` 断言，SSH 应用层测试不要求 Nginx 响应头。

导出容器限额和整机/容器CPU、内存、Java RSS、数据库连接、Redis内存、OOM/重启CSV；heap/GC另结合日志。Docker100%约占一个逻辑核；hostCpuPct是全部可见CPU归一化，dbActive排除采样连接。采样失败记录collectionError，不把空值当零。

写入前后运行data/verify.sql保存快照。点赞三阶段使用--pause-between-phases，在暂停时检查metadata.visitorHash对应明细：POST后一条、DELETE后零，多主体+10。全员通知应覆盖全部未注销用户（含禁用用户），排除发送管理员；SQL 输出应收人数和缺失/额外收件记录，差异应为空。浏览统计同时比总量和上海当日聚合。N+1另用低频SQL日志检查pageSize10/20，Dashboard查询不能随周期桶数线性增加。

HTTP_PASS只表示自动HTTP正确，不代表数据库/资源验收。并发标记DB_PENDING，资源/GC/SQL数量要补到清单20.5。每轮百分位取中位数，不取百分位平均；meanMs只是平均响应时间。任一轮失败必须保留；容量某档延迟/错误/吞吐不足90%停止升档，CPU/内存越线还需人工停止。40RPS通过只说明至少达到该档。

## 自检

```bash
python3 performance/p2/test_runner.py ResultTests -v
python3 performance/p2/test_runner.py --jmeter jmeter -v
```

第二条用真实JMeter加载两套计划，针对本地HTTP替身运行，不访问业务服务；耗时数分钟。不能当作服务器验收。SQL及资源采样需要实际PostgreSQL/Linux环境验证。
