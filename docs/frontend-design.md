# 前端设计约定

## 1. 主题模式

前端提供三种主题模式：

- `system`：跟随系统，默认模式
- `light`：浅色模式
- `dark`：深色模式

`system` 只是用户选择的模式，页面实际渲染时会解析为 `light` 或 `dark`。

## 2. Logo 使用

网站 favicon 与页面 Logo 使用同一个 Seedling SVG 资源，统一通过 `AppLogo` 组件在页面中展示。

- favicon、窄屏导航、后台侧边栏折叠态可以只显示图标。
- 首页、登录注册页、常规导航栏优先使用“图标 + 网站名”的组合 Logo。
- `AppLogo` 默认文案与 `VITE_APP_TITLE` 保持一致；未配置时默认显示 `Sanjuu Blog`。
- 后续如需调整站点名称，优先通过 `VITE_APP_TITLE` 配置；如有局部展示差异，再通过 `AppLogo` 组件参数覆盖。

## 3. 按钮交互

网站整体走简洁风格，基础按钮只维护一组主按钮颜色。

按钮颜色约定：

- 默认状态使用稍亮、稍柔和的主色
- hover 状态使用更深的主色
- active 状态与 hover 状态保持一致
- hover 与 active 状态下文字不加粗，只改变颜色

自定义按钮与 Element Plus 主按钮必须共享同一组颜色变量：

- `--app-button-bg` 对应 `--el-color-primary`
- `--app-button-hover` 对应 `--el-color-primary-light-3`
- `--app-button-active` 对应 `--el-color-primary-dark-2`
- `--app-button-text` 对应按钮文字颜色

Element Plus 的 `success`、`warning`、`danger`、`info` 按钮也遵循同样的交互方向：默认色稍亮，hover 与 active 使用更深的同色系颜色。后台管理页面可以在后续按业务语义继续细化这些状态色的具体取值。

## 4. 环境变量

前端环境变量按 Vite 约定管理，只有 `VITE_` 前缀的变量会暴露给前端运行时代码。

仓库可以提交不同环境的非敏感默认配置，例如：

- `.env.development`
- `.env.test`
- `.env.production`

这些文件只存放可公开的环境差异配置，例如 API 基础路径、开发代理目标地址等。生产密钥、私有 Token、云服务密钥等敏感值不能提交到仓库。

本机私有覆盖配置统一放在 `.env.local` 或 `.env.[mode].local` 中，并保持忽略状态。例如：

- `.env.development.local`
- `.env.production.local`

如果某个环境变量未来需要承载敏感信息，提交到仓库的环境文件中只保留空值或示例占位，真实值通过本地配置、部署平台环境变量或密钥管理服务注入。
