package com.commitnoteai.util

import com.commitnoteai.model.CommitChangeSnapshot

data class ChangedSymbols(
    val addedSymbols: List<String>,
    val removedSymbols: List<String>,
    val keptSymbols: List<String>,
) {
    val allSymbols: Set<String>
        get() = (addedSymbols + removedSymbols + keptSymbols).toSet()
}

object ChangedSymbolExtractor {
    private val identifierPattern = Regex("""\b[A-Za-z_][A-Za-z0-9_]*\b""")
    private val ignoredWords = setOf(
        "abstract", "annotation", "as", "break", "by", "catch", "class", "companion", "const", "constructor",
        "continue", "data", "do", "else", "enum", "false", "final", "finally", "for", "fun", "if", "import",
        "in", "init", "inline", "inner", "interface", "internal", "is", "lateinit", "null", "object", "open",
        "operator", "out", "override", "package", "private", "protected", "public", "return", "sealed", "super",
        "suspend", "tailrec", "this", "throw", "true", "try", "typealias", "val", "var", "when", "where", "while",
        "void", "static", "new", "extends", "implements", "boolean", "int", "long", "float", "double", "char",
        "byte", "short", "switch", "case", "default",
    )

    fun extract(changes: List<CommitChangeSnapshot>): ChangedSymbols {
        val beforeSymbols = linkedSetOf<String>()
        val afterSymbols = linkedSetOf<String>()
        changes.forEach { change ->
            val evidence = UnifiedDiffEvidence.parse(change.evidenceText())
            beforeSymbols += extractFromText(evidence.removedLines.joinToString("\n"))
            afterSymbols += extractFromText(evidence.addedLines.joinToString("\n"))
            afterSymbols += extractFromPath(change.path)
        }

        return ChangedSymbols(
            addedSymbols = (afterSymbols - beforeSymbols).sorted(),
            removedSymbols = (beforeSymbols - afterSymbols).sorted(),
            keptSymbols = beforeSymbols.intersect(afterSymbols).sorted(),
        )
    }

    private fun extractFromText(value: String?): Set<String> {
        if (value.isNullOrBlank()) return emptySet()
        return identifierPattern.findAll(value)
            .map { it.value }
            .filter { isUsefulSymbol(it) }
            .toSet()
    }

    private fun extractFromPath(path: String): Set<String> {
        val fileName = path.replace('\\', '/').substringAfterLast('/').substringBeforeLast('.')
        return if (isUsefulSymbol(fileName)) setOf(fileName) else emptySet()
    }

    private fun isUsefulSymbol(value: String): Boolean {
        val normalized = value.trim()
        return normalized.length >= 3 && normalized.lowercase() !in ignoredWords
    }
}
