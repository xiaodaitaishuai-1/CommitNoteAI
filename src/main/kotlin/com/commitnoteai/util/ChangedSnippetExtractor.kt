package com.commitnoteai.util

data class ChangedSnippets(
    val before: String?,
    val after: String?,
)

object ChangedSnippetExtractor {
    private const val CONTEXT_LINES = 2
    private const val MAX_SNIPPET_CHARS = 1_500

    fun extract(before: String?, after: String?): ChangedSnippets {
        if (before.isNullOrBlank() || after.isNullOrBlank()) {
            return ChangedSnippets(before = clip(before), after = clip(after))
        }

        val beforeLines = normalize(before).lines()
        val afterLines = normalize(after).lines()
        val prefix = commonPrefixSize(beforeLines, afterLines)
        val suffix = commonSuffixSize(beforeLines, afterLines, prefix)

        val beforeChangedEnd = beforeLines.size - suffix
        val afterChangedEnd = afterLines.size - suffix
        if (prefix >= beforeChangedEnd && prefix >= afterChangedEnd) {
            return ChangedSnippets(before = clip(before), after = clip(after))
        }

        return ChangedSnippets(
            before = focusedSnippet(beforeLines, prefix, beforeChangedEnd),
            after = focusedSnippet(afterLines, prefix, afterChangedEnd),
        )
    }

    private fun focusedSnippet(lines: List<String>, changedStart: Int, changedEndExclusive: Int): String? {
        if (lines.isEmpty()) return null
        val start = (changedStart - CONTEXT_LINES).coerceAtLeast(0)
        val end = (changedEndExclusive + CONTEXT_LINES).coerceAtMost(lines.size)
        return clip(lines.subList(start, end).joinToString("\n"))
    }

    private fun commonPrefixSize(before: List<String>, after: List<String>): Int {
        val max = minOf(before.size, after.size)
        var index = 0
        while (index < max && before[index] == after[index]) {
            index++
        }
        return index
    }

    private fun commonSuffixSize(before: List<String>, after: List<String>, prefix: Int): Int {
        var count = 0
        while (before.size - 1 - count >= prefix &&
            after.size - 1 - count >= prefix &&
            before[before.size - 1 - count] == after[after.size - 1 - count]
        ) {
            count++
        }
        return count
    }

    private fun normalize(value: String): String {
        return value.replace("\r\n", "\n").replace('\r', '\n')
    }

    private fun clip(value: String?): String? {
        val normalized = value?.let { normalize(it) }?.takeIf { it.isNotBlank() } ?: return null
        return if (normalized.length <= MAX_SNIPPET_CHARS) normalized else normalized.take(MAX_SNIPPET_CHARS) + "..."
    }
}
