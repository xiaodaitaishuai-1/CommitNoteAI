package com.commitnoteai.util

import com.commitnoteai.model.CommitChangeSnapshot
import com.commitnoteai.model.GeneratedCommitMessage

object CommitMessageFactChecker {
    private val riskyClaims = listOf("返回键", "页面流程", "状态更新", "页面处理")

    fun check(
        message: GeneratedCommitMessage,
        changes: List<CommitChangeSnapshot>,
        projectContext: String,
    ): GeneratedCommitMessage {
        val evidence = Evidence.from(changes, projectContext)
        val filteredBody = message.bodyLines.filter { line -> hasEvidence(line, evidence) }
        return message.copy(bodyLines = filteredBody)
    }

    private fun hasEvidence(line: String, evidence: Evidence): Boolean {
        val normalized = line.lowercase()
        val matchedRiskyClaim = riskyClaims.firstOrNull { line.contains(it) }
        val symbolEvidence = evidence.symbols.any { normalized.contains(it.lowercase()) }
        val codeSymbolEvidence = evidence.codeSymbols.any { normalized.contains(it.lowercase()) }
        val keywordEvidence = evidence.keywords.any { it.isNotBlank() && line.contains(it) }
        val factEvidence = evidence.facts.any { fact -> line.contains(fact) || fact.contains(line) }

        if (matchedRiskyClaim != null) {
            val riskyEvidence = evidence.diffText.contains(matchedRiskyClaim) || evidence.contextText.contains(matchedRiskyClaim)
            return codeSymbolEvidence || factEvidence || riskyEvidence
        }
        return factEvidence || symbolEvidence || keywordEvidence
    }

    private data class Evidence(
        val symbols: Set<String>,
        val codeSymbols: Set<String>,
        val facts: Set<String>,
        val keywords: Set<String>,
        val diffText: String,
        val contextText: String,
    ) {
        companion object {
            fun from(changes: List<CommitChangeSnapshot>, projectContext: String): Evidence {
                val changedSymbols = ChangedSymbolExtractor.extract(changes)
                val facts = CommitChangeFactExtractor.extract(changes).toSet()
                val symbols = changedSymbols.allSymbols
                val fileSymbols = changes.map { change ->
                    change.path.replace('\\', '/').substringAfterLast('/').substringBeforeLast('.')
                }.toSet()
                val codeSymbols = symbols - fileSymbols
                val keywords = linkedSetOf<String>()
                val diffText = changes.joinToString("\n") { change ->
                    listOf(change.path, change.evidenceText()).joinToString("\n")
                }
                changes.forEach { change ->
                    val path = change.path.replace('\\', '/')
                    val filename = path.substringAfterLast('/')
                    keywords += filename
                    keywords += filename.substringBeforeLast('.')
                    keywords += path.substringBefore('/').takeIf { it.isNotBlank() }.orEmpty()
                    keywords += extractChineseKeywords(change.evidenceText())
                }
                keywords += facts
                keywords += extractChineseKeywords(projectContext)
                return Evidence(
                    symbols = symbols.filter { it.length >= 3 }.toSet(),
                    codeSymbols = codeSymbols.filter { it.length >= 3 }.toSet(),
                    facts = facts,
                    keywords = keywords.filter { it.length >= 2 }.toSet(),
                    diffText = diffText,
                    contextText = projectContext,
                )
            }

            private fun extractChineseKeywords(value: String): Set<String> {
                return Regex("""[\u4e00-\u9fa5]{2,}""")
                    .findAll(value)
                    .map { it.value }
                    .flatMap { phrase -> phrase.windowed(size = 2, step = 1, partialWindows = false) }
                    .toSet()
            }
        }
    }
}
