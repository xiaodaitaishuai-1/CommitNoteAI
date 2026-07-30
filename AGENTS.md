# 仓库指南

## 项目结构与模块组织

CommitNoteAI 是一个 Kotlin/JVM IntelliJ Platform 插件。生产代码位于 `src/main/kotlin/com/commitnoteai`，按职责划分：`ai/` 包含 OpenAI 兼容客户端和生成逻辑，`vcs/` 负责提交窗口集成与界面行为，`settings/` 存放持久化配置，`util/` 提供 diff 和提示词辅助工具，`model/` 定义共享模型。`src/main/java/com/commitnoteai/platform` 存放 Java 平台桥接代码；插件注册配置在 `src/main/resources/META-INF/plugin.xml`。

测试代码按生产包结构镜像存放在 `src/test/kotlin/com/commitnoteai`。项目上下文与使用说明见 `README.md`、`CommitNoteAI.md`；设计和执行记录存放在 `docs/superpowers/`。

## 构建、测试与开发命令

在 PowerShell 中使用 Gradle Wrapper：

```powershell
.\gradlew.bat test -PlocalIdePath="E:\path\to\Android Studio"
.\gradlew.bat buildPlugin -PlocalIdePath="E:\path\to\Android Studio"
```

`test` 运行 JUnit 5 测试套件。`buildPlugin` 会编译、校验并打包插件，ZIP 产物位于 `build/distributions/CommitNoteAI-x.x.x.zip`。当前最新产物为 `CommitNoteAI-0.1.9.zip`；每次打包前必须在 `gradle.properties` 中更新 `pluginVersion`。`localIdePath` 可选，适合使用本地兼容 IDE 验证。默认平台版本和其余插件元数据也在 `gradle.properties` 中维护。

## 编码风格与命名约定

遵循 Kotlin 官方代码风格（`kotlin.code.style=official`）：四空格缩进、单一职责文件、多行调用和声明保留尾随逗号。类名使用 `PascalCase`，函数和属性使用 `camelCase`。包名保持在 `com.commitnoteai` 下，并将修改放入职责最接近的现有包。解析和转换逻辑优先实现为小型纯函数，IntelliJ API 调用应保持在集成边界。

## 测试要求

使用 JUnit 5 与 `kotlin.test` 断言。测试文件命名为 `*Test.kt`，测试函数采用描述性的反引号名称，例如 ``fun `step never advances beyond target length`()``。修改 VCS 或 AI 逻辑时，应覆盖边界条件及基于 diff 证据的提交信息行为。提交前运行 `test`；当前未配置覆盖率阈值。

## 提交与拉取请求规范

近期历史采用简洁的 Conventional Commit 风格，例如 `feat(vcs): type generated message into commit editor`、`fix(model): preserve legacy change snapshot constructor`、`docs: plan ...`。使用祈使式摘要、可选的小写 scope，并保持每个提交聚焦一项改动。拉取请求需说明用户可见行为、关联相关 issue 或设计记录、列出已执行的验证；修改设置页或提交窗口 UI 时附上截图。不得提交生成的 `build/`、IDE 元数据或 API Key 等密钥。

## Agent 专项约定

需要使用图标时，优先从 [Iconfont](https://www.iconfont.cn/) 或 [Material Symbols](https://fonts.google.com/icons?icon.query=music&icon.style=Rounded) 选择与现有界面风格一致的图标。
