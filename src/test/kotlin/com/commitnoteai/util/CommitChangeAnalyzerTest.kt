package com.commitnoteai.util

import com.commitnoteai.model.CommitChangeSnapshot
import kotlin.test.Test
import kotlin.test.assertContains
import kotlin.test.assertEquals

class CommitChangeAnalyzerTest {
    @Test
    fun `analyze groups mixed ad changes and recommends ads scope`() {
        val analysis = CommitChangeAnalyzer.analyze(
            listOf(
                change("admob/src/main/java/com/snapverse/ads/InterstitialAdController.kt", "added"),
                change("app/src/main/java/com/snapverse/SnapVerseApplication.kt", "modified"),
                change("app/build.gradle.kts", "modified"),
                change("README.md", "modified"),
                change("AGENTS.md", "modified"),
            ),
        )

        assertEquals("feat", analysis.suggestedType)
        assertEquals("ads", analysis.suggestedScope)
        assertContains(analysis.moduleGroups["admob"].orEmpty(), "admob/src/main/java/com/snapverse/ads/InterstitialAdController.kt")
        assertContains(analysis.moduleGroups["app"].orEmpty(), "app/src/main/java/com/snapverse/SnapVerseApplication.kt")
        assertContains(analysis.moduleGroups["gradle/config"].orEmpty(), "app/build.gradle.kts")
        assertContains(analysis.moduleGroups["docs"].orEmpty(), "README.md")
        assertContains(analysis.priorityHints, "初始化入口")
        assertContains(analysis.priorityHints, "Gradle 配置")
        assertContains(analysis.priorityHints, "文档同步")
    }

    @Test
    fun `analyze recommends admob scope when only admob module changes`() {
        val analysis = CommitChangeAnalyzer.analyze(
            listOf(
                change("admob/src/main/java/com/snapverse/ads/AppOpenAdController.kt", "added"),
                change("admob/src/main/res/layout/view_snapverse_native_ad.xml", "added"),
            ),
        )

        assertEquals("feat", analysis.suggestedType)
        assertEquals("admob", analysis.suggestedScope)
    }

    @Test
    fun `analyze recommends docs type for documentation only changes`() {
        val analysis = CommitChangeAnalyzer.analyze(
            listOf(
                change("README.md", "modified"),
                change("CLAUDE.md", "modified"),
            ),
        )

        assertEquals("docs", analysis.suggestedType)
        assertEquals(null, analysis.suggestedScope)
    }

    private fun change(path: String, type: String): CommitChangeSnapshot {
        return CommitChangeSnapshot(
            path = path,
            changeType = type,
            beforeSnippet = null,
            afterSnippet = null,
            originText = null,
        )
    }
}
