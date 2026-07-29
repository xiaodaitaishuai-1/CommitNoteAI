package com.commitnoteai.util

import com.commitnoteai.model.CommitChangeSnapshot
import com.intellij.openapi.project.Project
import com.intellij.openapi.vcs.VcsException
import com.intellij.openapi.vcs.changes.BinaryContentRevision
import com.intellij.openapi.vcs.changes.Change
import com.intellij.openapi.vcs.changes.ContentRevision

object CommitChangeCollector {
    private const val MAX_CHANGES = 20

    fun collect(project: Project?, changes: List<Change>): List<CommitChangeSnapshot> {
        return changes.take(MAX_CHANGES).map { change ->
            val beforeRevision = change.beforeRevision
            val afterRevision = change.afterRevision
            val snippets = ChangedSnippetExtractor.extract(
                before = readContent(beforeRevision),
                after = readContent(afterRevision),
            )

            CommitChangeSnapshot(
                path = resolvePath(change),
                changeType = describeChange(change),
                beforeSnippet = snippets.before,
                afterSnippet = snippets.after,
                originText = project?.let { change.getOriginText(it) },
            )
        }
    }

    private fun resolvePath(change: Change): String {
        val filePath = change.afterRevision?.file ?: change.beforeRevision?.file
        return filePath?.path ?: change.virtualFile?.path ?: "<unknown>"
    }

    private fun describeChange(change: Change): String {
        return when {
            change.isRenamed -> "renamed"
            change.isMoved -> "moved"
            change.beforeRevision == null -> "added"
            change.afterRevision == null -> "deleted"
            else -> "modified"
        }
    }

    private fun readContent(revision: ContentRevision?): String? {
        if (revision == null || revision is BinaryContentRevision) {
            return null
        }
        return try {
            revision.content
        } catch (_: VcsException) {
            null
        }
    }
}
