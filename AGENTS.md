# AGENTS.md

本文件是给 Agent 使用的执行指令，不是给项目开发者阅读的规范说明。

你在这个仓库中的职责，是基于 `docs/` 下的产品与技术文档，直接、安全、可验证地推进实现。你的目标不是输出泛泛建议，而是做出与现有文档一致、结构清晰、可通过验证的改动。

## 1. 先看什么

接到任务后，先把 `docs/` 视为事实来源。默认按以下顺序读取和服从：

1. `docs/PRD.md`
2. `docs/database-design.md`
3. `docs/api-design.md`
4. `docs/openapi.yaml`

如果代码、文档、用户当前要求之间存在冲突：

- 先以用户当前明确要求为准
- 其次以 `docs/` 为准
- 若 `docs/` 之间不一致，先修正文档，再改代码

不要在未读文档的情况下拍脑袋设计接口、表结构或页面行为。

## 2. 必须长期记住的业务事实

在本仓库工作时，默认以下事实始终成立，除非用户显式要求修改文档并改变设计：

- 角色有 `管理员`、`用户`、`游客`
- 只有管理员可以发文
- 文章状态为 `DRAFT`、`PUBLISHED`、`OFFLINE`
- API 使用版本前缀 `/api/v1/**`
- 分类为两级结构
- 一级分类示例：`技术`、`学业`、`消遣`、`杂记`
- 文章只能绑定二级分类，不能绑定一级分类
- 文章正文同时存储三种形态：
  - `content_md`：Markdown，编辑源
  - `content_html`：由 Markdown 转译得到，前台渲染用
  - `content_text`：由 Markdown 提取纯文本得到，全文搜索用

如果你发现实现与这些事实不一致，不要自作主张绕过，先修正实现或同步文档。

## 3. 默认工作方式

除非用户明确只是讨论、提问、评审或要方案，否则默认用户希望你直接落地修改。

### 3.1 Git commit message 请求

当用户要求“根据目前 git 暂存区内容生成 commit message”或类似表述时，默认等同于以下完整要求：

- 先检查当前 `git status`、`git diff --cached --stat`、`git diff --cached --name-only` 和最近几条提交风格。
- 生成符合 Angular / Conventional Commits 规范的 commit message。
- commit subject 使用英文，格式如 `feat(scope): summary`。
- long description 使用中文，整体风格与仓库近期 commit message 保持一致。
- body 使用短横线列表，每行尽量不超过 100 个字符，避免触发 commitlint。
- 根据暂存区内容给出推荐分支命名。
- 如果暂存区代码存在明显问题、拆分风险、格式问题或提交范围不清，先提出修改建议；用户改完后再给最终 commit message。

### 3.2 范围不清时必须确认

如果用户的指令同时包含较大的目标词和较小的具体范围，例如“完成用户模块”与“只检查/补全 DTO、VO、参数校验、实体字段填充”等同时出现，不要默认扩大实现范围。

遇到这类模糊指令时，必须先向用户确认本次要做的具体边界，尤其要确认是否允许新增或修改 controller、service、mapper、数据库 migration、接口文档和测试。

如果用户只要求处理 DTO、VO、参数校验或实体注解，就只处理这些文件；除非用户明确授权，不要顺手实现完整接口闭环。

### 3.3 超出范围改动必须确认

如果在执行过程中发现需要修改超出用户原始要求范围之外的文件、代码或行为，必须先停止并向用户确认具体修改内容。

未经用户明确同意，绝对不允许私自修改一大堆额外文件、顺手重构、批量格式化、补充额外功能、调整无关样式或改动与当前请求无关的代码。

如果为了验证或修复必须触碰额外文件，必须先说明：

- 为什么当前任务需要这些额外改动
- 预计会修改哪些文件
- 修改内容大致是什么
- 是否会影响暂存区或已有未提交改动

只有在用户确认后，才可以继续做这些超出原始范围的改动。

处理任务时默认采用以下顺序：

1. 读取相关文档与现有代码
2. 判断影响范围：前端、后端、数据库、文档、测试
3. 如果涉及数据模型变更，先创建 Flyway migration
4. 先改后端和数据层，再改前端
5. 同步更新 `docs/` 与 `openapi.yaml`
6. 执行对应验证
7. 汇报结果，说明改了什么、验证了什么、还有什么没做

不要只写一半代码就停下，也不要只分析不执行。

## 4. 前端部分你该怎么做

### 4.1 前端技术栈

前端默认技术栈是：

- `Vue 3`
- `Vite`
- `TypeScript`
- `Pinia`
- `Vue Router`
- `Element Plus`
- `Axios`
- `Day.js`
- `Tailwind CSS`
- `VueUse`

新增前端能力时，优先复用这些技术栈和现有项目能力，不要重复造轮子。

### 4.2 前端目录组织原则

前端结构必须按功能模块拆分，不允许把页面、API、类型、状态、组件随意混放。

优先遵循这种结构方向：

```text
frontend/src/
  app/
  router/
  stores/
  api/
  components/
  composables/
  utils/
  constants/
  styles/
  modules/
    auth/
    article/
    category/
    tag/
    user/
    admin/
```

执行时要遵守：

- 业务模块尽量放在 `modules/` 内部
- 通用组件放 `components/`
- 通用组合式逻辑放 `composables/`
- 通用工具放 `utils/`
- 请求统一通过 Axios 实例，不要页面里到处直接写裸请求
- 路由按模块拆分后统一汇总
- Pinia store 按业务域拆分

### 4.3 前端 UI 和库使用原则

- 后台页面优先使用 `Element Plus`
- 不要手写已有成熟组件能力，例如表格、分页、表单、对话框
- `Tailwind CSS` 用于布局、间距、响应式和通用样式增强
- 不要把 `Tailwind` 和 `Element Plus` 的职责搅乱
- `VueUse` 能解决的问题，优先复用，不要自己重复写通用 composable
- 时间展示统一优先使用 `Day.js`

### 4.4 修改前端后的强制动作

只要你改了前端代码，就必须：

1. 找到前端实际可用的命令
  - 优先查看 `frontend/package.json`
2. 至少运行前端编译检查
3. 运行格式检查
4. 如果项目已配置 lint，运行 lint
5. 如果项目已配置测试，至少运行受影响范围测试

如果缺少命令：

- 明确说明仓库当前没有对应脚本
- 不要编造“已通过”

## 5. 后端部分你该怎么做

### 5.1 后端技术栈

后端默认技术栈是：

- `Java 21`
- `Spring Boot 3`
- `MyBatis Plus`
- `Lombok`
- `Hutool`
- `Jackson`
- `Knife4j`
- `Spring Security`

实现时优先复用这些技术栈与框架能力，不要自己造基础设施轮子。

### 5.2 后端目录组织原则

后端必须采用三层架构，结构上至少要清晰体现：

- `controller`
- `service`
- `mapper`
- `model`
- `common`
- `config`

建议方向：

```text
backend/src/main/java/.../
  common/
  config/
  modules/
    auth/
      controller/
      service/
      mapper/
      model/
    user/
      controller/
      service/
      mapper/
      model/
    article/
      controller/
      service/
      mapper/
      model/
    category/
      controller/
      service/
      mapper/
      model/
    tag/
      controller/
      service/
      mapper/
      model/
```

执行时要遵守：

- `controller` 只做参数接收、权限入口、响应封装
- `service` 负责业务逻辑
- `mapper` 负责持久化访问
- 不要让 `controller` 直接调数据库
- 公共异常、响应、枚举、常量、工具统一收敛到 `common`
- 安全、序列化、MyBatis、Knife4j 等配置统一收敛到 `config`

### 5.3 后端实现原则

- 能用 `MyBatis Plus` 的基础能力，就不要重复写 CRUD 轮子
- 能用 `Spring Security` 完成鉴权，就不要另起一套散乱认证逻辑
- 用 `Jackson` 统一 JSON 序列化配置
- 用 `Knife4j` 配套接口文档
- 谨慎使用 `Hutool`，只在确实有价值时使用
- 接口实现必须遵守 `docs/api-design.md` 和 `docs/openapi.yaml`
- 文章保存或更新时，必须由后端根据 `content_md` 生成 `content_html` 和 `content_text`
- 不要过度封装：只有一行、没有复用价值、没有明显命名解释作用的小逻辑，优先直接写在调用处
- 不要过度添加边界判断：保留参数校验、权限校验、业务状态校验、数据一致性校验；已经由 DTO 校验、前置查询或明确业务不变量保证的重复兜底，不要层层堆叠
- 同一个类中，`@Override` 的 public 实现方法优先放在前面，让读者先看到对外能力；private helper 放在 public 方法之后，且 helper 内部若 `A` 调用 `B`，默认把 `B` 写在 `A` 前面

### 5.4 修改后端后的强制动作

只要你改了后端代码，就必须：

1. 找到后端实际可用的命令
  - 优先查看 `backend/pom.xml`、`mvnw`、`gradlew`
2. 跑编译检查
3. 跑格式检查
4. 跑单元测试
5. 如果功能形成完整闭环，补并跑集成测试

不要把“没有主动跑测试”说成“应该没问题”。

### 5.5 接口开发的测试底线

每次开发后端接口时：

- 必须补单元测试
- 功能完成后必须补集成测试

重点覆盖：

- 注册、登录、刷新 Token、退出登录
- 用户禁用后的登录与刷新
- 文章创建、更新、发布、下线
- 文章只能绑定二级分类
- `content_md -> content_html -> content_text`
- 分类树返回与分类删除校验

## 6. 数据库部分你该怎么做

### 6.1 绝对规则

数据库变更只能通过 Flyway migration 管理。

禁止：

- 手动在数据库工具里改表结构
- 先改代码后补 migration
- 修改已经执行过的历史 migration 来伪装没有变更

### 6.2 遇到数据模型变更时的动作

如果任何功能涉及表、字段、索引、约束变化，你必须先：

1. 创建新的 Flyway migration
2. 命名格式使用 `Vx.x.x__xxx.sql`

例如：

- `V1.0.0__init_schema.sql`
- `V1.0.1__add_article_content_html_and_text.sql`
- `V1.1.0__add_comment_tables.sql`

然后再继续改后端代码和文档。

### 6.3 数据库实现要求

- 数据结构必须与 `docs/database-design.md` 一致
- 若 schema 变化影响接口或产品行为，必须同步更新 `docs/`
- 涉及文章表时，必须保留并正确维护：
  - `content_md`
  - `content_html`
  - `content_text`

## 7. 文档同步规则

以下情况必须同步更新文档：

- PRD 变更
- 数据库结构变更
- 接口字段变更
- 权限模型变更
- 路由变更
- 响应结构变更
- OpenAPI 变更

至少检查：

- `docs/PRD.md`
- `docs/database-design.md`
- `docs/api-design.md`
- `docs/openapi.yaml`

代码改了但文档没同步，不算完成。

## 8. 你不能做什么

你不应该：

- 不读文档直接实现
- 跳过 Flyway 直接改数据库
- 在前端页面中堆砌请求和复杂业务逻辑
- 在后端 `controller` 里写核心业务
- 绕过三层架构直接访问数据库
- 为已有成熟能力重复造轮子
- 实现完接口但不补测试
- 改了接口或表结构却不更新 OpenAPI 和文档
- 声称跑过验证但实际上没跑

## 9. 每次改动后的最低检查清单

做完任务后，你至少要自查：

- 是否与 `docs/` 保持一致
- 是否复用了现有能力，没有重复造轮子
- 是否保持了前后端目录结构清晰
- 是否在需要时先创建了 Flyway migration
- 是否同步更新了文档与 OpenAPI
- 是否跑了前端相关验证
- 是否跑了后端相关验证
- 是否补了后端单元测试
- 功能闭环是否补了集成测试

## 10. 输出结果时怎么汇报

完成任务后，优先汇报：

- 改了什么
- 哪些文件被修改
- 跑了哪些验证
- 哪些验证没跑以及原因
- 是否还存在阻塞项

不要只说“已完成”，要给出可核对的结果。

## 11. 完成定义

只有同时满足以下条件，任务才算真正完成：

- 实现与 `docs/` 一致
- 代码结构符合本文件要求
- 没有明显重复造轮子
- 数据变更已补 Flyway migration
- 文档已同步
- 前端改动已完成编译与格式等验证
- 后端改动已完成编译、格式、测试验证
- 接口开发已补单元测试
- 功能闭环已补集成测试

如果任一条件未满足，你必须明确说明未完成项，而不是把任务包装成已完成。
