package com.commitnoteai.util

import com.commitnoteai.model.CommitChangeSnapshot

data class CommitChangeAnalysis(
    val totalChanges: Int,
    val changeTypeCounts: Map<String, Int>,
    val moduleGroups: Map<String, List<String>>,
    val suggestedType: String,
    val suggestedScope: String?,
    val priorityHints: List<String>,
)

object CommitChangeAnalyzer {
    fun analyze(changes: List<CommitChangeSnapshot>): CommitChangeAnalysis {
        val normalizedPaths = changes.map { it.path.replace('\\', '/') }
        val moduleGroups = normalizedPaths
            .groupBy { moduleForPath(it) }
            .mapValues { (_, paths) -> paths.distinct().sorted() }
            .toSortedMap()
        val changeTypeCounts = changes
            .groupingBy { it.changeType }
            .eachCount()
            .toSortedMap()

        return CommitChangeAnalysis(
            totalChanges = changes.size,
            changeTypeCounts = changeTypeCounts,
            moduleGroups = moduleGroups,
            suggestedType = suggestedType(changes, moduleGroups),
            suggestedScope = suggestedScope(moduleGroups),
            priorityHints = priorityHints(changes),
        )
    }

    private fun moduleForPath(path: String): String {
        val filename = path.substringAfterLast('/')
        return when {
            filename in setOf("README.md", "AGENTS.md", "CLAUDE.md") -> "docs"
            path.endsWith(".gradle.kts") || path.endsWith(".gradle") ||
                filename == "gradle.properties" || path.contains("/gradle/") -> "gradle/config"
            path.startsWith("admob/") || path.contains("/admob/") -> "admob"
            path.startsWith("app/") || path.contains("/app/") -> "app"
            path.endsWith(".xml") -> "resources"
            else -> path.substringBefore('/').ifBlank { "root" }
        }
    }

    private fun suggestedType(
        changes: List<CommitChangeSnapshot>,
        moduleGroups: Map<String, List<String>>,
    ): String {
        val modules = moduleGroups.keys
        val docsOnly = modules.isNotEmpty() && modules.all { it == "docs" }
        val configOnly = modules.isNotEmpty() && modules.all { it == "gradle/config" }
        return when {
            docsOnly -> "docs"
            configOnly -> "chore"
            changes.any { it.changeType == "added" } -> "feat"
            changes.any { it.changeType == "deleted" } -> "refactor"
            else -> "fix"
        }
    }

    private fun suggestedScope(moduleGroups: Map<String, List<String>>): String? {
        val modules = moduleGroups.keys
        val touchesAdmob = "admob" in modules
        val touchesApp = "app" in modules
        val touchesConfigOrDocs = modules.any { it == "docs" || it == "gradle/config" || it == "resources" }
        return when {
            touchesAdmob && (touchesApp || touchesConfigOrDocs || modules.size > 1) -> "ads"
            touchesAdmob -> "admob"
            touchesApp -> "app"
            else -> null
        }
    }

    private fun priorityHints(changes: List<CommitChangeSnapshot>): List<String> {
        val hints = linkedSetOf<String>()
        changes.forEach { change ->
            val path = change.path.replace('\\', '/')
            val filename = path.substringAfterLast('/')
            val changeText = change.diffText.lowercase()
            if (changeText.contains("notificationpermission") ||
                changeText.contains("notification_permission") ||
                changeText.contains("post_notifications")
            ) {
                hints += "通知权限请求"
            }
            if (filename.endsWith("Application.kt")) {
                hints += "初始化入口"
            }
            if (filename.endsWith("Controller.kt") || filename.endsWith("Runtime.kt") || filename.endsWith("AdsRuntime.kt")) {
                hints += "接口/运行时扩展"
            }
            if (path.endsWith(".gradle.kts") || path.endsWith(".gradle") || filename == "gradle.properties") {
                hints += "Gradle 配置"
            }
            if (path.endsWith(".xml")) {
                hints += "布局资源"
            }
            if (filename in setOf("README.md", "AGENTS.md", "CLAUDE.md")) {
                hints += "文档同步"
            }
        }
        return hints.toList()
    }
}
