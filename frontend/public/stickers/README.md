# 评论与留言预置表情资源

Alus（`alus`）、Bili 小电视（`bilibili`）、Bili 表情（`bmoji`）、QQ（`qq`）、微信（`wechat`）图片和各库 `info.json` 来自 [Waline Emojis](https://github.com/walinejs/emojis)。保留原文件与清单顺序，图片格式包括 PNG、GIF、WebP，界面固定高度并保留原宽高比。

上游仓库根 [LICENSE](https://github.com/walinejs/emojis/blob/main/LICENSE) 为 GNU GPL v3，副本见 [LICENSE.Waline-GPL-3.0.txt](./LICENSE.Waline-GPL-3.0.txt)。上游 README 注明 alus、bilibili、qq 等资源整理自 [MiniValine](https://github.com/MiniValine)。各图像、角色与商标的权利归各自权利人；完整项目声明见 `/THIRD_PARTY_NOTICES.txt` 和仓库 `docs/third-party-notices.md`。

## 更新目录

每库使用独立目录，名称为稳定库 ID。`info.json` 中的 `name`、`prefix`、`type`、`icon`、`items` 保持 Waline 格式；`items` 决定收录内容与排序，文件必须为 `{prefix}{item}.{type}`。

可添加本项目的可选字段 `gridMinWidthRem`，用正数设置选择器网格列的最小宽度（单位 rem）。例如 Alus 使用 `"gridMinWidthRem": 4.5`；未配置的库默认 3rem。生成脚本校验并将配置传入目录接口，前端切换库时自动应用；此设置只调整单元格列宽，图片保持固定高度与原比例。

在 frontend 目录执行：

```sh
pnpm stickers:generate
pnpm stickers:check
```

生成结果位于 `backend/src/main/resources/comment-stickers/catalog.json`，前端通过公开评论表情接口读取同一目录。不要手动维护生成文件；更新后同时部署前后端。已被评论引用的库 ID、item 名和资源路径应保留，否则历史标记会退回普通文本。

当前只开放预置表情，不提供用户上传或后台管理。评论和留言保存在各自原有 TEXT 字段，以 `[sticker:库ID:表情ID]` 引用资源。
