# 第三方资源声明

本项目使用到的第三方图标、图片、字体等资源会记录在本文件中，便于后续维护版权与许可证信息。

## 网站 favicon 与页面 Logo

网站 favicon 与页面 Logo 基于 Google Noto Emoji 项目中的 Seedling emoji SVG 调整而来。

- 原始资源：Google Noto Emoji `emoji_u1f331.svg`
- 原始资源链接：https://github.com/googlefonts/noto-emoji/blob/main/svg/emoji_u1f331.svg
- 配色调整参考工具：Iconify Noto Emoji Seedling
- 配色调整页面：https://icon-sets.iconify.design/noto/seedling/
- 上游仓库：https://github.com/googlefonts/noto-emoji
- SVG 资源许可证：https://github.com/googlefonts/noto-emoji/blob/main/svg/LICENSE
- 版权信息：Copyright 2013 Google, Inc. All Rights Reserved.
- 许可证：Apache License, Version 2.0

说明：Google Noto Emoji 仓库根目录的 `LICENSE` 适用于字体软件，许可证为 SIL Open Font License 1.1；本项目使用的是 `svg/` 目录下的 SVG 资源，因此按 `svg/LICENSE` 记录为 Apache License, Version 2.0。

本项目 favicon 与页面 Logo 基于上述开源资源进行配色调整，不表示 Google 或 Iconify 对本项目提供认可、赞助或背书。

## 文字 Logo 字体

网站文字 Logo 使用 Google Fonts 提供的 Long Cang 字体。

- 字体名称：Long Cang
- Google Fonts 页面：https://fonts.google.com/specimen/Long+Cang?selection.family=Long+Cang
- Google Fonts 仓库目录：https://github.com/google/fonts/tree/main/ofl/longcang
- 字体文件：`LongCang-Regular.ttf`
- 字体元数据：https://raw.githubusercontent.com/google/fonts/main/ofl/longcang/METADATA.pb
- 字体许可证：https://raw.githubusercontent.com/google/fonts/main/ofl/longcang/OFL.txt
- 设计者：Chen Xiaomin
- 版权信息：Copyright 2018 The LongCang Project Authors (https://github.com/googlefonts/longcang)
- 许可证：SIL Open Font License, Version 1.1

说明：Long Cang 使用 SIL Open Font License 1.1，可免费用于个人与商业项目，也可用于网站 Logo、图片、页面文本和应用内嵌字体。若项目分发字体文件本身，需要保留对应版权信息与 OFL 许可证文本；使用该字体生成的 Logo 图片、页面截图或其他设计输出不需要按 OFL 继续分发。

## 评论与留言预置表情

评论和留言选择器使用的 Alus（`alus`）、Bili 小电视（`bilibili`）、Bili 表情（`bmoji`）、QQ（`qq`）、微信（`wechat`）五个表情库及其 `info.json` 来源于 [Waline Emojis](https://github.com/walinejs/emojis)，保存在 `frontend/public/stickers/`，通过本站静态资源提供，不从第三方 CDN 热链加载。本项目保留原图片、文件名与清单顺序，另从清单生成供评论和留言共用的目录。

- 上游仓库：https://github.com/walinejs/emojis
- 上游说明：https://github.com/walinejs/emojis/blob/main/README.md
- 上游仓库根许可证：https://github.com/walinejs/emojis/blob/main/LICENSE（GNU GPL v3，副本随资源保存在 `public/stickers/LICENSE.Waline-GPL-3.0.txt`）
- 上游 README 另注明 alus、bilibili、qq 等资源整理自 [MiniValine](https://github.com/MiniValine)。
- 各表情图像、角色与商标的权利归各自权利人；仓库根许可证和资源来源均保留，不将其解释为本项目取得各品牌或角色的所有权或背书。

部署后的来源说明还可通过 `/THIRD_PARTY_NOTICES.txt` 和 `/stickers/README.md` 查看。
