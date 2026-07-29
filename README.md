# CommitNoteAI

Android Studio / IntelliJ 平台插件，用来在提交窗口里根据已选变更生成中文提交记录。

## 功能

- 在提交面板里添加“生成提交记录”按钮
- 读取当前勾选的变更
- 通过 OpenAI 兼容接口生成 `title + body`
- 回填到提交框

## 配置

在 Settings / Preferences 里找到 `CommitNoteAI`，填入：

- `API Base URL`
- `Model`
- `API Key`

## 项目上下文

插件会自动读取项目根目录下的 `CommitNoteAI.md`，用于告诉 AI 项目模块、业务词汇和提交偏好。这个文件是可选的，不存在时会直接跳过。

可以在设置页点击 `创建上下文模板` 生成示例文件。上下文只作为辅助信息，最终提交记录仍以已勾选 diff 为准。

示例：

```markdown
# CommitNoteAI Context

## 项目模块
- app: Android 应用入口
- admob: 广告 SDK 封装

## 提交偏好
- 使用中文提交记录
- 标题使用 type(scope): 中文标题
- 正文使用工整短句清单
- 只描述真实 diff 中出现的改动，不要推断未出现的行为

## 常用 scope
- ads: 广告整体能力
- admob: AdMob 适配层
- home: HomeActivity 或首页相关
```

## 准确性兜底

生成前插件会从真实 diff 中提取 changed symbols，例如新增/删除的方法名、字段名和调用名，并把它们写入 prompt。

生成后插件会做一次事实校验，过滤明显没有 diff 证据的正文，减少“返回键处理”“页面流程”“状态更新”等未出现在改动里的内容。

## 构建

```bash
.\gradlew.bat test -PlocalIdePath="E:\go\GoLand 2026.1"
.\gradlew.bat buildPlugin -PlocalIdePath="E:\go\GoLand 2026.1"
```

打包产物在 `build/distributions/CommitNoteAI-0.1.9.zip`。

插件兼容从 `251` 系开始，也就是 Android Studio Narwhal / IntelliJ Platform 2025.1。

