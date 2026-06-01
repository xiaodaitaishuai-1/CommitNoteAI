package com.commitnoteai.vcs

import com.commitnoteai.model.GeneratedCommitMessage

object CommitMessageFormatter {
    fun format(message: GeneratedCommitMessage): String {
        return buildString {
            append(message.title.trim())
            val body = message.bodyLines.map { it.trim() }.filter { it.isNotBlank() }
            if (body.isNotEmpty()) {
                appendLine()
                appendLine()
                append(body.joinToString(separator = "\n") { line ->
                    if (line.startsWith("- ")) line else "- $line"
                })
            }
        }
    }
}
