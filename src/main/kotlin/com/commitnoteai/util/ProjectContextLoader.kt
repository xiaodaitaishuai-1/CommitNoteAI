package com.commitnoteai.util

import com.intellij.openapi.project.Project
import java.nio.file.Files
import java.nio.file.Path
import kotlin.io.path.exists
import kotlin.io.path.readText

object ProjectContextLoader {
    const val FILE_NAME = "CommitNoteAI.md"
    private const val MAX_CONTEXT_CHARS = 6_000

    val template: String = """
        # CommitNoteAI Context

        ## 项目定位
        - 这个文件用于告诉 CommitNoteAI 当前项目的模块、业务术语和提交偏好。
        - 这里的信息只作为理解上下文，提交内容仍必须以已勾选 diff 为准。
        - 如果只看到局部改动，只总结局部事实，不推断完整业务流程。

        ## 可靠性规则
        - 只描述真实 diff、changed symbols、文件路径和代码片段中出现的改动。
        - 正文中提到的类名、方法名、字段名、资源名、配置名必须能找到证据。
        - 不写“优化体验”“完善逻辑”“提升稳定性”等没有证据的泛话。
        - 不把局部文件改动写成“完整支持”“全面接入”。
        - 只改文档、测试或配置时，不要写成运行时功能变化。

        ## 模块与 scope
        - app: 应用入口、页面、业务流程。
        - ui: 布局、控件、资源、文案和交互入口。
        - data: 数据模型、仓库、缓存、持久化。
        - network: 接口请求、响应解析、网络配置。
        - ads: 广告整体能力，跨多个广告模块时使用。
        - admob: 只改 AdMob 适配层时使用。
        - settings: 设置页、用户配置、开关、密钥保存。
        - docs: README、AGENTS、CLAUDE、CommitNoteAI 等文档。
        - test: 单元测试、测试样例和断言。

        ## type 选择
        - feat: 新增用户可见能力、入口、配置项或完整可用的新模块。
        - fix: 修复错误行为、解析失败、回填异常、配置校验或兼容问题。
        - refactor: 调整结构、抽取工具、移动职责、删除冗余实现，不改变外部行为。
        - chore: 构建、版本、依赖、Gradle、元数据和维护改动。
        - docs: 只改文档、说明或上下文模板。
        - test: 只新增或调整测试。
        - style: 只改格式、排版、命名风格或无行为差异的代码样式。

        ## 正文偏好
        - 使用中文短句清单，每条只讲一个具体事实。
        - 每条以明确动作开头，例如：新增、调整、扩展、移除、修复、更新、重构。
        - 优先写具体类名、方法名、字段名、配置项、文件名和资源名。
        - 优先覆盖 addedSymbols、removedSymbols 和关键文件。
        - 一条不要堆多个“并且 / 同时 / 以及”，过长时拆成两条。

        ## 高风险表述黑名单
        - 没有直接证据时，不写：页面流程、返回键处理、状态更新、生命周期管理。
        - 没有直接证据时，不写：完整支持、全面接入、端到端打通、全链路优化。
        - 没有直接证据时，不写：提升用户体验、增强稳定性、优化性能、改善交互。
        - 没有直接证据时，不写：修复崩溃、解决异常、避免重复请求、防止状态错乱。

        ## 示例与反例
        好：
        - fix(settings): 修复 API Key 脱敏显示判断
        - 在 CommitNoteConfigurable 中复用已保存的 API Key
        - 避免把脱敏值重新写入 PasswordSafe

        坏：
        - feat(ai): 全面提升提交记录智能生成能力
        - 优化用户体验并提升系统稳定性

        原因：提交记录要写 diff 里能证明的事实，不写无法验证的效果。
    """.trimIndent() + "\n"

    fun load(project: Project?): String {
        val basePath = project?.basePath ?: return ""
        return load(Path.of(basePath))
    }

    fun load(basePath: Path): String {
        return try {
            val contextFile = basePath.resolve(FILE_NAME)
            if (!contextFile.exists()) {
                ""
            } else {
                clip(contextFile.readText(Charsets.UTF_8).trim())
            }
        } catch (_: Throwable) {
            ""
        }
    }

    fun createTemplate(basePath: Path): Boolean {
        val contextFile = basePath.resolve(FILE_NAME)
        if (contextFile.exists()) {
            return false
        }
        Files.writeString(contextFile, template, Charsets.UTF_8)
        return true
    }

    private fun clip(value: String): String {
        return if (value.length <= MAX_CONTEXT_CHARS) value else value.take(MAX_CONTEXT_CHARS) + "..."
    }
}
