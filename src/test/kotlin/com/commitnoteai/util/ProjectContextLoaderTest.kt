package com.commitnoteai.util

import java.nio.file.Files
import kotlin.io.path.readText
import kotlin.test.Test
import kotlin.test.assertContains
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertTrue

class ProjectContextLoaderTest {
    @Test
    fun `load returns empty string when context file is missing`() {
        val dir = Files.createTempDirectory("commitnoteai-context-missing")

        assertEquals("", ProjectContextLoader.load(dir))
    }

    @Test
    fun `load reads CommitNoteAI context file`() {
        val dir = Files.createTempDirectory("commitnoteai-context-read")
        Files.writeString(dir.resolve("CommitNoteAI.md"), "## 提交偏好\n- 使用中文提交记录")

        val context = ProjectContextLoader.load(dir)

        assertContains(context, "使用中文提交记录")
    }

    @Test
    fun `load clips long context file`() {
        val dir = Files.createTempDirectory("commitnoteai-context-long")
        Files.writeString(dir.resolve("CommitNoteAI.md"), "x".repeat(7_000))

        val context = ProjectContextLoader.load(dir)

        assertEquals(6_003, context.length)
        assertTrue(context.endsWith("..."))
    }

    @Test
    fun `createTemplate writes template only when missing`() {
        val dir = Files.createTempDirectory("commitnoteai-context-template")

        val created = ProjectContextLoader.createTemplate(dir)
        val createdAgain = ProjectContextLoader.createTemplate(dir)
        val content = dir.resolve("CommitNoteAI.md").readText()

        assertTrue(created)
        assertFalse(createdAgain)
        assertContains(content, "# CommitNoteAI Context")
        assertContains(content, "## 项目定位")
        assertContains(content, "## 可靠性规则")
        assertContains(content, "## 模块与 scope")
        assertContains(content, "## 高风险表述黑名单")
        assertContains(content, "## 示例与反例")
        assertTrue(ProjectContextLoader.template.length < 6_000)
    }
}
