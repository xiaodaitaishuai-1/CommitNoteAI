package com.commitnoteai.util

import com.commitnoteai.model.CommitPromptPayload
import com.commitnoteai.settings.CommitNoteSettings

object CommitPromptBuilder {
    fun build(payload: CommitPromptPayload): String {
        val style = CommitNoteSettings.normalizeOutputStyle(payload.outputStyle)
        val analysis = CommitChangeAnalyzer.analyze(payload.changes)
        val changedSymbols = ChangedSymbolExtractor.extract(payload.changes)
        val changeFacts = CommitChangeFactExtractor.extract(payload.changes)
        val builder = StringBuilder()
        builder.appendLine("请根据下面的代码变更生成提交记录。")
        builder.appendLine("输出必须是严格 JSON，格式如下：")
        builder.appendLine("""{"title":"type(scope): 中文提交标题","bodyLines":["中文要点一","中文要点二"]}""")
        builder.appendLine()
        builder.appendLine("要求：")
        builder.appendLine("- title 必须使用 Conventional Commit 风格：type(scope): 中文标题。")
        builder.appendLine("- type 从 feat、fix、refactor、chore、docs、test、style 中选择。")
        builder.appendLine("- scope 优先从路径、模块、类名或业务域推断；无法判断时可以省略 scope。")
        builder.appendLine("- scope 示例：广告整体用 ads；只改 AdMob 适配层时用 admob。")
        builder.appendLine("- 标题不要写完整、全面等过满词，除非变更确实完成了完整能力接入。")
        builder.appendLine("- 示例 title：refactor(ads): 移除 RewardManager 中的冗余方法。")
        builder.appendLine("- bodyLines 返回不带 Markdown 前缀的中文要点。")
        appendTidyChecklistRules(builder)
        appendStyleRules(builder, style)
        builder.appendLine("- bodyLines 每行描述一个具体变更，例如：删除了 updateAdIsShowing 方法。")
        builder.appendLine("- 只描述真实 diff 中出现的类名、方法名、字段名和行为。")
        builder.appendLine("- 不要编造页面流程、返回键处理、状态更新等未出现在 diff 中的内容。")
        builder.appendLine("- 如果只能看到局部改动，就只总结局部改动，不要推断整个文件职责。")
        builder.appendLine("- 正文优先覆盖 addedSymbols 和 removedSymbols。")
        builder.appendLine("- 正文优先使用关键变更事实，写清是什么控件换成什么控件，或哪个类修改了什么。")
        builder.appendLine("- 不要只写调整导航控件、优化页面结构这类泛词；必须带上控件 id、类名、方法名、字段名或资源名。")
        builder.appendLine("- 正文中出现的方法名、字段名、类名必须来自 changed symbols、文件名、项目上下文或真实 diff。")
        builder.appendLine("- 优先写新增能力、接口/运行时扩展、初始化入口、配置依赖、布局资源、文档同步。")
        builder.appendLine("- 不要输出 Markdown，不要输出代码块，不要解释。")
        builder.appendLine()

        appendAnalysis(builder, analysis)
        appendChangedSymbols(builder, changedSymbols)
        appendChangeFacts(builder, changeFacts)

        val projectContext = payload.projectContext.trim()
        if (projectContext.isNotBlank()) {
            builder.appendLine("项目上下文：")
            builder.appendLine("项目上下文只能用于理解模块、术语和提交偏好，不能覆盖真实 diff；提交内容仍必须以已勾选 diff 为准。")
            builder.appendLine(projectContext)
            builder.appendLine()
        }

        val customInstructions = payload.customInstructions.trim()
        if (customInstructions.isNotBlank()) {
            builder.appendLine("额外要求：")
            builder.appendLine(customInstructions)
            builder.appendLine()
        }

        if (payload.currentDraft.isNotBlank()) {
            builder.appendLine("当前草稿：")
            builder.appendLine(payload.currentDraft.trim())
            builder.appendLine()
        }

        builder.appendLine("变更摘要：")
        payload.changes.forEachIndexed { index, change ->
            builder.appendLine("${index + 1}. [${change.changeType}] ${change.path}")
            change.originText?.takeIf { it.isNotBlank() }?.let {
                builder.appendLine("   origin: ${clip(it)}")
            }
            change.beforeSnippet?.takeIf { it.isNotBlank() }?.let {
                builder.appendLine("   before: ${clip(it)}")
            }
            change.afterSnippet?.takeIf { it.isNotBlank() }?.let {
                builder.appendLine("   after: ${clip(it)}")
            }
            builder.appendLine()
        }

        return builder.toString().trim()
    }

    private fun appendTidyChecklistRules(builder: StringBuilder) {
        builder.appendLine("- 所有正文都使用工整清单风格：短句、统一句式、每条只讲一个变更点。")
        builder.appendLine("- 正文每条以明确动作开头，例如：新增、调整、扩展、移除、修复、更新、重构。")
        builder.appendLine("- 每条只描述一个主要变更，避免一条里堆多个“并且/同时/以及”。")
        builder.appendLine("- 优先写具体类名、方法名、模块名、配置名，不要写优化代码结构、提升体验这类空话。")
        builder.appendLine("- 行文保持短句，单条过长时拆成两条。")
        builder.appendLine("- 不使用编号，不返回 Markdown 前缀，bodyLines 只返回正文文本。")
    }

    private fun appendStyleRules(builder: StringBuilder, style: String) {
        when (style) {
            CommitNoteSettings.OUTPUT_STYLE_SIMPLE -> {
                builder.appendLine("- 输出样式：简洁风格。")
                builder.appendLine("- 只返回一行工整 title，bodyLines 必须返回空数组。")
            }
            CommitNoteSettings.OUTPUT_STYLE_DETAILED -> {
                builder.appendLine("- 输出样式：详细风格。")
                builder.appendLine("- bodyLines 最多 5 条工整要点，适合中等代码变更。")
            }
            CommitNoteSettings.OUTPUT_STYLE_TRAE -> {
                builder.appendLine("- 输出样式：Trae 风格。")
                builder.appendLine("- title 仍使用 Conventional Commit 风格。")
                builder.appendLine("- bodyLines 允许 5 到 8 条工整清单，适合一次提交横跨多个子系统的场景。")
                builder.appendLine("- 尽量覆盖新增、移除、重构、配置、入口、布局、文档等完整变更。")
                builder.appendLine("- 目标示例：")
                builder.appendLine("""  {"title":"feat(admob): 新增完整 AdMob 广告格式支持","bodyLines":["移除无用的 configBoolean 工具函数和 NoOpAdsRuntime 实现","新增开屏 / 插页 / 激励视频 / 原生广告的控制器接口与 AdMob 实现","新增 AdMob 测试广告 ID 常量类 AdMobTestIds","新增原生广告布局模板 view_snapverse_native_ad.xml","新增广告错误信息、监听器等核心工具类 AdsCore","重构 AdsRuntime 接口以暴露所有广告控制器实例","更新 README、CLAUDE.md、AGENTS.md 文档以适配新的广告模块结构"]}""")
            }
            else -> {
                builder.appendLine("- 输出样式：通译灵码风格。")
                builder.appendLine("- bodyLines 返回 2 到 3 条工整要点，适合日常提交；没有正文时返回空数组。")
            }
        }
    }

    private fun appendAnalysis(builder: StringBuilder, analysis: CommitChangeAnalysis) {
        builder.appendLine("变更概览：")
        builder.appendLine("- 文件数量：${analysis.totalChanges}")
        builder.appendLine("- 变更类型统计：${analysis.changeTypeCounts.entries.joinToString { "${it.key}=${it.value}" }}")
        val suggestedTitle = buildString {
            append(analysis.suggestedType)
            analysis.suggestedScope?.let { append("($it)") }
        }
        builder.appendLine("- 建议标题方向：$suggestedTitle")
        if (analysis.priorityHints.isNotEmpty()) {
            builder.appendLine("- 重要变更优先级：${analysis.priorityHints.joinToString("、")}")
        }
        builder.appendLine()

        builder.appendLine("模块分组：")
        analysis.moduleGroups.forEach { (module, paths) ->
            builder.appendLine("[$module]")
            paths.take(8).forEach { path ->
                builder.appendLine("- $path")
            }
        }
        builder.appendLine()
    }

    private fun appendChangedSymbols(builder: StringBuilder, symbols: ChangedSymbols) {
        if (symbols.addedSymbols.isEmpty() && symbols.removedSymbols.isEmpty() && symbols.keptSymbols.isEmpty()) {
            return
        }
        builder.appendLine("关键变更符号：")
        builder.appendLine("- addedSymbols: ${symbols.addedSymbols.take(20).joinToString(", ")}")
        builder.appendLine("- removedSymbols: ${symbols.removedSymbols.take(20).joinToString(", ")}")
        builder.appendLine("- keptSymbols: ${symbols.keptSymbols.take(20).joinToString(", ")}")
        builder.appendLine()
    }

    private fun appendChangeFacts(builder: StringBuilder, facts: List<String>) {
        if (facts.isEmpty()) {
            return
        }
        builder.appendLine("关键变更事实：")
        facts.take(20).forEach { fact ->
            builder.appendLine("- $fact")
        }
        builder.appendLine("- 示例正文：将 buttonBack 从 Button 调整为 AppCompatImageButton")
        builder.appendLine("- 示例正文：在 DramaHomeFragment 中隐藏 textSectionSubtitle")
        builder.appendLine()
    }

    private fun clip(value: String): String {
        val normalized = value.replace("\r\n", "\n").replace('\r', '\n')
        return if (normalized.length <= 800) {
            normalized
        } else {
            normalized.take(800) + "..."
        }
    }
}
