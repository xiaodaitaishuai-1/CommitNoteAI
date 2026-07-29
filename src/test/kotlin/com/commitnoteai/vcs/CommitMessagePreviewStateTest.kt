package com.commitnoteai.vcs

import kotlin.test.Test
import kotlin.test.assertEquals

class CommitMessagePreviewStateTest {
    @Test
    fun `replacement text uses edited preview content`() {
        val state = CommitMessagePreviewState(
            originalDraft = "feat(vcs): 旧草稿",
            generatedMessage = "fix(vcs): 生成提交记录",
        )

        val replacement = state.replacementText("fix(vcs): 手动调整提交记录\n\n- 保留用户编辑")

        assertEquals("fix(vcs): 手动调整提交记录\n\n- 保留用户编辑", replacement)
    }

    @Test
    fun `regenerated state keeps original draft unchanged`() {
        val state = CommitMessagePreviewState(
            originalDraft = "docs: 原始草稿",
            generatedMessage = "docs: 第一次生成",
        )

        val regenerated = state.withGeneratedMessage("docs: 第二次生成")

        assertEquals("docs: 原始草稿", regenerated.originalDraft)
        assertEquals("docs: 第二次生成", regenerated.generatedMessage)
    }
}
