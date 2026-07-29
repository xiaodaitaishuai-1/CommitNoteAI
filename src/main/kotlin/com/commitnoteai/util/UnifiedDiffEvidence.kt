package com.commitnoteai.util

data class UnifiedDiffEvidence(
    val path: String,
    val addedLines: List<String>,
    val removedLines: List<String>,
    val contextLines: List<String>,
) {
    val beforeText: String
        get() = (contextLines + removedLines).joinToString("\n")

    val afterText: String
        get() = (contextLines + addedLines).joinToString("\n")

    companion object {
        fun parse(diffText: String): UnifiedDiffEvidence {
            var path = ""
            var inHunk = false
            val addedLines = mutableListOf<String>()
            val removedLines = mutableListOf<String>()
            val contextLines = mutableListOf<String>()

            diffText.lineSequence().forEach { line ->
                when {
                    line.startsWith("+++ ") -> path = pathFromHeader(line.removePrefix("+++ "))
                    line.startsWith("--- ") && path.isBlank() -> path = pathFromHeader(line.removePrefix("--- "))
                    line.startsWith("@@ ") -> inHunk = true
                    inHunk && line.startsWith("+") -> addedLines += line.drop(1)
                    inHunk && line.startsWith("-") -> removedLines += line.drop(1)
                    inHunk && line.startsWith(" ") -> contextLines += line.drop(1)
                }
            }

            return UnifiedDiffEvidence(
                path = path.ifBlank { diffText.lineSequence().firstOrNull().orEmpty().substringBefore(" (no textual patch)") },
                addedLines = addedLines,
                removedLines = removedLines,
                contextLines = contextLines,
            )
        }

        private fun pathFromHeader(value: String): String {
            return value.trim()
                .removePrefix("a/")
                .removePrefix("b/")
                .takeUnless { it == "/dev/null" }
                .orEmpty()
        }
    }
}
