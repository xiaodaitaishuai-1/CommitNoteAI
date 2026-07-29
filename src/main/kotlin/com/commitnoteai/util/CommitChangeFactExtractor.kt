package com.commitnoteai.util

import com.commitnoteai.model.CommitChangeSnapshot

object CommitChangeFactExtractor {
    private const val MAX_FACTS = 30

    private val xmlTagPattern = Regex("""(?s)<([A-Za-z0-9_.]+)\b([^<>]*?)(?:/?>)""")
    private val xmlAttributePattern = Regex("""([A-Za-z_][A-Za-z0-9_.:-]*)\s*=\s*"([^"]*)"""")
    private val classPattern = Regex("""\b(?:data\s+class|enum\s+class|class|object|interface)\s+([A-Z][A-Za-z0-9_]*)""")
    private val methodPattern = Regex("""\bfun\s+([A-Za-z_][A-Za-z0-9_]*)\s*\(""")
    private val fieldPattern = Regex("""\b(?:val|var)\s+([A-Za-z_][A-Za-z0-9_]*)\b""")
    private val visibilityPattern = Regex("""\b([A-Za-z_][A-Za-z0-9_]*)\.visibility\s*=\s*([A-Za-z_][A-Za-z0-9_.]*)""")

    fun extract(changes: List<CommitChangeSnapshot>): List<String> {
        val facts = linkedSetOf<String>()
        changes.forEach { change ->
            facts += extractResourceFacts(change)
            facts += extractXmlFacts(change)
            facts += extractCodeFacts(change)
        }
        return facts.take(MAX_FACTS)
    }

    private fun extractResourceFacts(change: CommitChangeSnapshot): List<String> {
        val path = change.path.replace('\\', '/')
        val filename = path.substringAfterLast('/')
        return when {
            path.contains("/res/drawable/") && change.changeType == "added" -> listOf("新增 drawable 资源 $filename")
            path.contains("/res/drawable/") && change.changeType == "modified" -> listOf("更新 drawable 资源 $filename")
            path.contains("/res/values/") && change.changeType == "added" -> listOf("新增 values 资源 $filename")
            path.contains("/res/values/") && change.changeType == "modified" -> listOf("更新 values 资源 $filename")
            else -> emptyList()
        }
    }

    private fun extractXmlFacts(change: CommitChangeSnapshot): List<String> {
        val path = change.path.replace('\\', '/')
        if (!path.endsWith(".xml")) return emptyList()

        val evidence = UnifiedDiffEvidence.parse(change.evidenceText())
        val beforeElements = parseXmlElements(evidence.beforeText).associateBy { it.id }
        val afterElements = parseXmlElements(evidence.afterText).associateBy { it.id }
        if (beforeElements.isEmpty() || afterElements.isEmpty()) return emptyList()

        val facts = mutableListOf<String>()
        beforeElements.keys.intersect(afterElements.keys).forEach { id ->
            val before = beforeElements.getValue(id)
            val after = afterElements.getValue(id)
            if (before.simpleTag != after.simpleTag) {
                facts += "将 $id 从 ${before.simpleTag} 调整为 ${after.simpleTag}"
            }
            facts += describeAttributeChanges(id, before.attributes, after.attributes)
        }
        return facts
    }

    private fun extractCodeFacts(change: CommitChangeSnapshot): List<String> {
        val path = change.path.replace('\\', '/')
        if (!path.endsWith(".kt") && !path.endsWith(".java")) return emptyList()

        val evidence = UnifiedDiffEvidence.parse(change.evidenceText())
        val before = evidence.beforeText
        val after = evidence.afterText
        val className = classNameFor(path, before, after)
        val facts = mutableListOf<String>()

        val beforeMethods = methodPattern.findAll(before).map { it.groupValues[1] }.toSet()
        val afterMethods = methodPattern.findAll(after).map { it.groupValues[1] }.toSet()
        (afterMethods - beforeMethods).forEach { facts += "在 $className 中新增 $it 方法" }
        (beforeMethods - afterMethods).forEach { facts += "移除 $className 中的 $it 方法" }

        val beforeFields = fieldPattern.findAll(before).map { it.groupValues[1] }.toSet()
        val afterFields = fieldPattern.findAll(after).map { it.groupValues[1] }.toSet()
        (afterFields - beforeFields).forEach { facts += "在 $className 中新增 $it 字段" }
        (beforeFields - afterFields).forEach { facts += "移除 $className 中的 $it 字段" }

        val beforeVisibility = visibilityAssignments(before)
        val afterVisibility = visibilityAssignments(after)
        afterVisibility.filterNot { beforeVisibility.contains(it) }.forEach { (target, value) ->
            facts += "在 $className 中将 $target.visibility 设为 $value"
        }

        if (before.contains(Regex("""\bLog\.d\s*\(""")) && !after.contains(Regex("""\bLog\.d\s*\("""))) {
            facts += "移除 $className 中的 Log.d 调试输出"
        }

        return facts
    }

    private fun parseXmlElements(value: String?): List<XmlElement> {
        if (value.isNullOrBlank()) return emptyList()
        return xmlTagPattern.findAll(value)
            .mapNotNull { match ->
                val tag = match.groupValues[1]
                if (tag.startsWith("/")) return@mapNotNull null
                val attributes = xmlAttributePattern.findAll(match.groupValues[2])
                    .associate { it.groupValues[1] to it.groupValues[2] }
                val id = attributes["android:id"]?.substringAfterLast('/') ?: return@mapNotNull null
                XmlElement(
                    tag = tag,
                    simpleTag = tag.substringAfterLast('.'),
                    id = id,
                    attributes = attributes.filterKeys { it != "android:id" },
                )
            }
            .toList()
    }

    private fun describeAttributeChanges(
        id: String,
        beforeAttributes: Map<String, String>,
        afterAttributes: Map<String, String>,
    ): List<String> {
        val facts = mutableListOf<String>()
        val removed = beforeAttributes.keys - afterAttributes.keys
        val added = afterAttributes.keys - beforeAttributes.keys
        val pairedCount = minOf(removed.size, added.size)
        removed.take(pairedCount).zip(added.take(pairedCount)).forEach { (oldName, newName) ->
            facts += "将 $id 的 $oldName 替换为 $newName"
        }
        removed.drop(pairedCount).forEach { facts += "移除 $id 的 $it" }
        added.drop(pairedCount).forEach { facts += "为 $id 新增 $it=${afterAttributes.getValue(it)}" }

        beforeAttributes.keys.intersect(afterAttributes.keys).forEach { name ->
            val beforeValue = beforeAttributes.getValue(name)
            val afterValue = afterAttributes.getValue(name)
            if (beforeValue != afterValue) {
                facts += "将 $id 的 $name 从 $beforeValue 调整为 $afterValue"
            }
        }
        return facts
    }

    private fun classNameFor(path: String, before: String, after: String): String {
        return classPattern.find(after)?.groupValues?.get(1)
            ?: classPattern.find(before)?.groupValues?.get(1)
            ?: path.substringAfterLast('/').substringBeforeLast('.')
    }

    private fun visibilityAssignments(value: String): Set<Pair<String, String>> {
        return visibilityPattern.findAll(value)
            .map { it.groupValues[1] to it.groupValues[2] }
            .toSet()
    }

    private data class XmlElement(
        val tag: String,
        val simpleTag: String,
        val id: String,
        val attributes: Map<String, String>,
    )
}
