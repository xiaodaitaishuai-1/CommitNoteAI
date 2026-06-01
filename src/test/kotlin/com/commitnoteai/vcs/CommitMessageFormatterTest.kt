package com.commitnoteai.vcs

import com.commitnoteai.model.GeneratedCommitMessage
import kotlin.test.Test
import kotlin.test.assertEquals

class CommitMessageFormatterTest {
    @Test
    fun `format prefixes body lines as bullet list`() {
        val formatted = CommitMessageFormatter.format(
            GeneratedCommitMessage(
                title = "refactor(ads): 移除 RewardManager 中的冗余方法",
                bodyLines = listOf(
                    "删除了 updateAdIsShowing 方法，该方法仅用于将 adIsShowing 设置为 false",
                    "简化了 RewardManager 类的代码结构",
                    "保持了 loadAndShowRewardAd 方法的功能完整性",
                ),
            ),
        )

        assertEquals(
            """
            refactor(ads): 移除 RewardManager 中的冗余方法

            - 删除了 updateAdIsShowing 方法，该方法仅用于将 adIsShowing 设置为 false
            - 简化了 RewardManager 类的代码结构
            - 保持了 loadAndShowRewardAd 方法的功能完整性
            """.trimIndent(),
            formatted,
        )
    }

    @Test
    fun `format does not duplicate existing bullet prefix`() {
        val formatted = CommitMessageFormatter.format(
            GeneratedCommitMessage(
                title = "fix(commit): 修复提交记录格式",
                bodyLines = listOf("- 保留已有要点前缀"),
            ),
        )

        assertEquals("fix(commit): 修复提交记录格式\n\n- 保留已有要点前缀", formatted)
    }

    @Test
    fun `format drops blank body lines`() {
        val formatted = CommitMessageFormatter.format(
            GeneratedCommitMessage(
                title = "fix(commit): 修复提交记录生成",
                bodyLines = listOf("", "  ", "回填提交框"),
            ),
        )

        assertEquals("fix(commit): 修复提交记录生成\n\n- 回填提交框", formatted)
    }

    @Test
    fun `format title only when body is empty`() {
        val formatted = CommitMessageFormatter.format(
            GeneratedCommitMessage(
                title = "docs(readme): 更新插件说明",
                bodyLines = emptyList(),
            ),
        )

        assertEquals("docs(readme): 更新插件说明", formatted)
    }
}
