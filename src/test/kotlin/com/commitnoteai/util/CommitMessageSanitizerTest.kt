package com.commitnoteai.util

import com.commitnoteai.model.GeneratedCommitMessage
import kotlin.test.Test
import kotlin.test.assertEquals

class CommitMessageSanitizerTest {
    @Test
    fun `sanitize removes duplicate body lines and markdown prefixes`() {
        val sanitized = CommitMessageSanitizer.sanitize(
            GeneratedCommitMessage(
                title = "feat(ads): 新增完整 AdMob 广告格式支持",
                bodyLines = listOf(
                    "1. 新增 AppOpenAdController 控制器",
                    "- 新增 AppOpenAdController 控制器",
                    "* 更新 README.md 文档说明",
                ),
            ),
        )

        assertEquals("feat(ads): 新增 AdMob 广告格式支持", sanitized.title)
        assertEquals(
            listOf(
                "新增 AppOpenAdController 控制器",
                "更新 README.md 文档说明",
            ),
            sanitized.bodyLines,
        )
    }

    @Test
    fun `sanitize filters vague body lines`() {
        val sanitized = CommitMessageSanitizer.sanitize(
            GeneratedCommitMessage(
                title = "refactor(ads): 调整广告控制器",
                bodyLines = listOf(
                    "优化代码结构",
                    "提升用户体验",
                    "调整 RewardedAdController 的展示回调",
                ),
            ),
        )

        assertEquals(listOf("调整 RewardedAdController 的展示回调"), sanitized.bodyLines)
    }
}
