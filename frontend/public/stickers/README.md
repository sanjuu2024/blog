# 评论与留言预置表情资源

Alus（`alus`）、哔哩哔哩小电视（`bilibili`）、哔哩哔哩表情（`bmoji`）、QQ（`qq`）、微信（`wechat`）的素材与原始清单来自 [Waline Emojis](https://github.com/walinejs/emojis)。保留源图片与条目顺序，整理展示名称、资源前缀和尺寸配置；图片格式包括 PNG、GIF、WebP，界面固定高度并保留原宽高比。

上游仓库根 [LICENSE](https://github.com/walinejs/emojis/blob/main/LICENSE) 为 GNU GPL v3，副本见 [LICENSE.Waline-GPL-3.0.txt](./LICENSE.Waline-GPL-3.0.txt)。上游 README 注明 alus、bilibili、qq 等资源整理自 [MiniValine](https://github.com/MiniValine)。各图像、角色与商标的权利归各自权利人；完整项目声明见 `/THIRD_PARTY_NOTICES.txt` 和仓库 `docs/third-party-notices.md`。

其他表情库的素材获取来源：

- Bangumi 娘（`bangumi`）：[Bangumi 日志](https://bangumi.tv/blog/370650)。
- 蛆音娘（`qyn`）、叶え（かなえ）（`kanae`）、夏目友人帐（`natsume`）、齐木楠雄的灾难（`saikikusuo`）、关于我转生变成史莱姆这档事（`rimuru`）、撕梓咩（`suzume`）、鲨猫（`sharkitty`）：[Bmoji 表情列表](https://bmoji.3mua.cn/list)。

上述新增素材的来源按各自页面记录，Waline 仓库许可证说明对应前述来自 Waline 的五个表情库。各表情图像、角色与商标的权利归各自权利人所有。

## 更新目录

每库使用独立目录，名称为稳定库 ID。`info.json` 中的 `name`、`prefix`、`type`、`icon`、`items` 沿用原有清单结构；`items` 决定收录内容与排序，文件必须为 `{prefix}{item}.{type}`。Bangumi 娘保留大写展示名称，文件前缀统一为小写 `bangumi_`；哔哩哔哩小电视使用 `btv_` 前缀。Git 中记录的文件名大小写必须与清单一致，以兼容 Linux 部署。

可添加本项目的可选字段 `minHeightRem`，用正数设置正文与选择器中的表情图片显示高度（单位 rem）；未配置默认 1.5rem。Alus 为 2rem，夏目友人帐为 4rem，Bangumi 娘、叶え（かなえ）、蛆音娘、转生史莱姆、齐木楠雄、鲨猫及撕梓咩为 3rem。生成脚本校验并将配置传入目录接口，前端切换库时自动应用，图片保留原比例，底部库切换图标保持 1.5rem。

在 frontend 目录执行：

```sh
pnpm stickers:generate
pnpm stickers:check
```

生成结果位于 `backend/src/main/resources/comment-stickers/catalog.json`，前端通过公开评论表情接口读取同一目录。当前共 13 个库、741 个表情。不要手动维护生成文件；更新后同时部署前后端。已被评论引用的库 ID、item 名应保留，否则历史标记会退回普通文本；本次资源前缀整理只更新生成 URL，不改变既有表情标记。

生成脚本为图片与库图标添加稳定的 `?v=内容哈希前16位`，不使用部署时间戳。前端生产配置 `VITE_STICKER_BASE_URL=https://sticker.ccsanjuu.com` 将 `/stickers/` 替换为 CDN 基址；不配置或设为空时继续使用同源图片。CDN 和浏览器分别按七天和一天缓存成功响应，404 不缓存；Cloudflare 缓存键需要保留 v 查询参数。部署、证书及更新缓存流程见仓库 `docs/cloudflare-sticker-cdn.md`。

当前只开放预置表情，不提供用户上传或后台管理。评论和留言保存在各自原有 TEXT 字段，以 `[sticker:库ID:表情ID]` 引用资源。
