package com.commitnoteai.util

import com.commitnoteai.model.CommitChangeSnapshot
import com.commitnoteai.model.CommitPromptPayload
import kotlin.test.Test
import kotlin.test.assertContains
import kotlin.test.assertTrue

class CommitPromptBuilderTest {
    @Test
    fun `build includes change details and json contract`() {
        val prompt = CommitPromptBuilder.build(
            CommitPromptPayload(
                currentDraft = "修复登录页",
                changes = listOf(
                    CommitChangeSnapshot(
                        path = "app/src/main/java/Login.kt",
                        changeType = "modified",
                        beforeSnippet = "old code",
                        afterSnippet = "new code",
                        originText = "Login.kt",
                    ),
                ),
            ),
        )

        assertContains(prompt, "严格 JSON")
        assertContains(prompt, "Conventional Commit")
        assertContains(prompt, "refactor(ads): 移除 RewardManager 中的冗余方法")
        assertContains(prompt, "bodyLines 返回不带 Markdown 前缀的中文要点")
        assertContains(prompt, "修复登录页")
        assertContains(prompt, "Login.kt")
        assertContains(prompt, "old code")
        assertContains(prompt, "new code")
    }

    @Test
    fun `build includes custom instructions`() {
        val prompt = CommitPromptBuilder.build(
            CommitPromptPayload(
                currentDraft = "",
                changes = listOf(sampleChange()),
                customInstructions = "重点描述广告缓存、状态管理、删除冗余逻辑；不要写空话。",
            ),
        )

        assertContains(prompt, "额外要求")
        assertContains(prompt, "重点描述广告缓存、状态管理、删除冗余逻辑；不要写空话。")
    }

    @Test
    fun `build describes tongyi output style`() {
        val prompt = CommitPromptBuilder.build(
            CommitPromptPayload(
                currentDraft = "",
                changes = listOf(sampleChange()),
                outputStyle = "tongyi",
            ),
        )

        assertContains(prompt, "通译灵码风格")
        assertContains(prompt, "2 到 3 条工整要点")
        assertContains(prompt, "Conventional Commit")
    }

    @Test
    fun `build includes tidy checklist rules for every output style`() {
        val prompt = CommitPromptBuilder.build(
            CommitPromptPayload(
                currentDraft = "",
                changes = listOf(sampleChange()),
                outputStyle = "detailed",
            ),
        )

        assertContains(prompt, "工整清单")
        assertContains(prompt, "以明确动作开头，例如：新增、调整、扩展、移除、修复、更新、重构")
        assertContains(prompt, "每条只描述一个主要变更")
        assertContains(prompt, "避免一条里堆多个")
        assertContains(prompt, "优先写具体类名、方法名、模块名、配置名")
        assertContains(prompt, "不要写优化代码结构、提升体验这类空话")
        assertContains(prompt, "不使用编号，不返回 Markdown 前缀")
    }

    @Test
    fun `build strengthens title rules`() {
        val prompt = CommitPromptBuilder.build(
            CommitPromptPayload(
                currentDraft = "",
                changes = listOf(sampleChange()),
                outputStyle = "trae",
            ),
        )

        assertContains(prompt, "标题不要写完整、全面等过满词")
        assertContains(prompt, "广告整体用 ads")
        assertContains(prompt, "只改 AdMob 适配层时用 admob")
    }

    @Test
    fun `build describes simple output style`() {
        val prompt = CommitPromptBuilder.build(
            CommitPromptPayload(
                currentDraft = "",
                changes = listOf(sampleChange()),
                outputStyle = "simple",
            ),
        )

        assertContains(prompt, "简洁风格")
        assertContains(prompt, "bodyLines 必须返回空数组")
    }

    @Test
    fun `build describes detailed output style`() {
        val prompt = CommitPromptBuilder.build(
            CommitPromptPayload(
                currentDraft = "",
                changes = listOf(sampleChange()),
                outputStyle = "detailed",
            ),
        )

        assertContains(prompt, "详细风格")
        assertContains(prompt, "最多 5 条工整要点")
    }

    @Test
    fun `build describes trae output style`() {
        val prompt = CommitPromptBuilder.build(
            CommitPromptPayload(
                currentDraft = "",
                changes = listOf(sampleChange()),
                outputStyle = "trae",
            ),
        )

        assertContains(prompt, "Trae 风格")
        assertContains(prompt, "5 到 8 条工整清单")
        assertContains(prompt, "新增、移除、重构、配置、入口、布局、文档")
        assertContains(prompt, "目标示例")
        assertContains(prompt, "feat(admob): 新增完整 AdMob 广告格式支持")
    }

    @Test
    fun `build includes change overview module groups and title suggestion`() {
        val prompt = CommitPromptBuilder.build(
            CommitPromptPayload(
                currentDraft = "",
                changes = listOf(
                    CommitChangeSnapshot(
                        path = "admob/src/main/java/com/snapverse/ads/RewardedAdController.kt",
                        changeType = "added",
                        beforeSnippet = null,
                        afterSnippet = "class RewardedAdController",
                        originText = null,
                    ),
                    CommitChangeSnapshot(
                        path = "app/src/main/java/com/snapverse/SnapVerseApplication.kt",
                        changeType = "modified",
                        beforeSnippet = null,
                        afterSnippet = "SnapVerseAds.install(this)",
                        originText = null,
                    ),
                    CommitChangeSnapshot(
                        path = "README.md",
                        changeType = "modified",
                        beforeSnippet = null,
                        afterSnippet = "admob module",
                        originText = null,
                    ),
                ),
                outputStyle = "trae",
            ),
        )

        assertContains(prompt, "变更概览")
        assertContains(prompt, "模块分组")
        assertContains(prompt, "建议标题方向：feat(ads)")
        assertContains(prompt, "重要变更优先级")
        assertContains(prompt, "优先写新增能力、接口/运行时扩展、初始化入口、配置依赖、布局资源、文档同步")
        assertContains(prompt, "[admob]")
        assertContains(prompt, "[app]")
        assertContains(prompt, "[docs]")
    }

    @Test
    fun `build clips long snippets`() {
        val longText = "x".repeat(900)
        val prompt = CommitPromptBuilder.build(
            CommitPromptPayload(
                currentDraft = "",
                changes = listOf(
                    CommitChangeSnapshot(
                        path = "a.txt",
                        changeType = "modified",
                        beforeSnippet = longText,
                        afterSnippet = null,
                        originText = null,
                    ),
                ),
            ),
        )

        assertTrue(prompt.contains("..."))
    }

    private fun sampleChange(): CommitChangeSnapshot {
        return CommitChangeSnapshot(
            path = "app/src/main/java/RewardManager.kt",
            changeType = "modified",
            beforeSnippet = "fun updateAdIsShowing()",
            afterSnippet = "loadAndShowRewardAd()",
            originText = "RewardManager.kt",
        )
    }
}
