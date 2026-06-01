package com.commitnoteai.util

import com.commitnoteai.model.GeneratedCommitMessage

object CommitMessageSanitizer {
    private val prefixPattern = Regex("""^\s*(?:[-*]\s+|\d+[.)、]\s*)""")
    private val vagueLines = setOf(
        "优化代码结构",
        "提升用户体验",
        "优化用户体验",
        "提升代码质量",
        "完善相关逻辑",
    )

    fun sanitize(message: GeneratedCommitMessage): GeneratedCommitMessage {
        val bodyLines = linkedSetOf<String>()
        message.bodyLines
            .map { cleanBodyLine(it) }
            .filter { it.isNotBlank() }
            .filterNot { isVagueLine(it) }
            .forEach { bodyLines += it }

        return GeneratedCommitMessage(
            title = cleanTitle(message.title),
            bodyLines = bodyLines.toList(),
        )
    }

    private fun cleanTitle(value: String): String {
        return value.trim()
            .replace("新增完整 ", "新增 ")
            .replace("新增完整", "新增")
            .replace("全面接入", "接入")
            .replace("完整接入", "接入")
    }

    private fun cleanBodyLine(value: String): String {
        return value.trim()
            .replace(prefixPattern, "")
            .trim()
    }

    private fun isVagueLine(value: String): Boolean {
        val normalized = value.trim().removeSuffix("。")
        return normalized in vagueLines
    }
}
