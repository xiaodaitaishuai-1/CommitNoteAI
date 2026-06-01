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

## 构建

```bash
.\gradlew.bat test -PlocalIdePath="E:\go\GoLand 2026.1"
.\gradlew.bat buildPlugin -PlocalIdePath="E:\go\GoLand 2026.1"
```

打包产物在 `build/distributions/CommitNoteAI-0.1.6.zip`。

插件兼容从 `251` 系开始，也就是 Android Studio Narwhal / IntelliJ Platform 2025.1。

