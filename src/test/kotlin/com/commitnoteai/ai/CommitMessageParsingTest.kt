package com.commitnoteai.ai

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
}
