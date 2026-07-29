package com.commitnoteai.ai

import com.commitnoteai.model.CommitChangeSnapshot
import kotlin.test.Test
import kotlin.test.assertContains
import kotlin.test.assertEquals

class CommitMessageParsingTest {
    @Test
    fun `parse extracts json payload from fenced content`() {
        val body = """
            {
              "choices": [
                {
                  "message": {
                    "content": "```json\n{\"title\":\"fix(login): 修复登录按钮状态\",\"bodyLines\":[\"调整按钮禁用逻辑\"]}\n```"
                  }
                }
              ]
            }
        """.trimIndent()

        val message = CommitNoteGenerator().generateResponseForTest(body)
        assertEquals("fix(login): 修复登录按钮状态", message.title)
        assertEquals(listOf("调整按钮禁用逻辑"), message.bodyLines)
    }

    @Test
    fun `parse falls back to plain text`() {
        val body = """
            {
              "choices": [
                {
                  "message": {
                    "content": "fix(login): 修复登录按钮状态\n调整按钮禁用逻辑"
                  }
                }
              ]
            }
        """.trimIndent()

        val message = CommitNoteGenerator().generateResponseForTest(body)
        assertEquals("fix(login): 修复登录按钮状态", message.title)
        assertEquals(listOf("调整按钮禁用逻辑"), message.bodyLines)
    }

    @Test
    fun `request body includes reasoning effort`() {
        val body = CommitNoteGenerator.createChatRequestBodyForTest(
            model = "gpt-5.2",
            temperature = 0.2,
            reasoningEffort = "high",
            userPrompt = "变更摘要",
        )

        assertContains(body, """"reasoning":{"effort":"high"}""")
    }

    @Test
    fun `parse filters unsupported body lines when changes are provided`() {
        val body = """
            {
              "choices": [
                {
                  "message": {
                    "content": "{\"title\":\"refactor(home): 简化通知权限请求流程\",\"bodyLines\":[\"调整 HomeActivity 的返回键处理逻辑\",\"直接调用 requestNotificationPermissionDirectly 替代对话框逻辑\"]}"
                  }
                }
              ]
            }
        """.trimIndent()

        val message = CommitNoteGenerator().generateResponseForTest(
            body = body,
            changes = listOf(
                CommitChangeSnapshot(
                    path = "app/src/main/java/com/clarity/photo/activity/HomeActivity.kt",
                    changeType = "modified",
                    beforeSnippet = "showNotificationPermissionRationaleDialog(notificationPermissionLauncher)",
                    afterSnippet = "requestNotificationPermissionDirectly(notificationPermissionLauncher)",
                    originText = "HomeActivity.kt",
                ),
            ),
        )

        assertEquals(listOf("直接调用 requestNotificationPermissionDirectly 替代对话框逻辑"), message.bodyLines)
    }
}
