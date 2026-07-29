package com.commitnoteai.util

import com.commitnoteai.model.CommitChangeCollection
import com.commitnoteai.model.CommitChangeSkip
import com.commitnoteai.model.CommitChangeSnapshot
import com.intellij.openapi.project.Project
import com.intellij.openapi.diff.impl.patch.IdeaTextPatchBuilder
import com.intellij.openapi.diff.impl.patch.UnifiedDiffWriter
import com.intellij.openapi.vcs.FilePath
import com.intellij.openapi.vcs.VcsException
import com.intellij.openapi.vcs.changes.BinaryContentRevision
import com.intellij.openapi.vcs.changes.Change
import com.intellij.openapi.vcs.changes.ContentRevision
import com.intellij.openapi.vcs.changes.CurrentContentRevision
import java.io.IOException
import java.io.StringWriter
import java.nio.file.Path

internal class CommitDiffAccumulator(
    private val maxEntries: Int = 50,
    private val maxCharacters: Int = 70_000,
) {
    private val changes = mutableListOf<CommitChangeSnapshot>()
    private val skippedChanges = mutableListOf<CommitChangeSkip>()
    private var characterCount = 0
    private var truncated = false

    fun add(path: String, diffText: String): CommitDiffAccumulator = add(path, diffText, "modified")

    fun add(path: String, diffText: String, changeType: String): CommitDiffAccumulator {
        if (truncated || changes.size >= maxEntries || characterCount + diffText.length > maxCharacters) {
            truncated = true
            return this
        }
        changes += CommitChangeSnapshot(path = path, changeType = changeType, diffText = diffText)
        characterCount += diffText.length
        return this
    }

    fun skip(path: String, reason: String): CommitDiffAccumulator {
        skippedChanges += CommitChangeSkip(path, reason)
        return this
    }

    fun result(): CommitChangeCollection = CommitChangeCollection(changes, skippedChanges, truncated)
}

object CommitChangeCollector {
    private const val MAX_CHANGES = 20
    private const val MAX_SINGLE_LINE_CHARS = 300

    fun collectDiffs(
        project: Project,
        changes: List<Change>,
        unversionedFiles: List<FilePath> = emptyList(),
    ): CommitChangeCollection {
        val accumulator = CommitDiffAccumulator()
        val selectedChanges = changes + unversionedFiles.map { filePath ->
            Change(null, CurrentContentRevision(filePath))
        }
        val basePath = project.basePath ?: return accumulator.result()

        selectedChanges.forEach { change ->
            val path = resolvePath(change)
            val revision = change.afterRevision ?: change.beforeRevision
            if (revision == null) {
                accumulator.skip(path, "missing-revision")
                return@forEach
            }
            if (revision is BinaryContentRevision || revision.file.fileType.isBinary) {
                accumulator.skip(path, "binary")
                return@forEach
            }
            try {
                val content = revision.content.orEmpty()
                if (isSingleLineLargeFile(content)) {
                    accumulator.skip(path, "single-line-too-large")
                    return@forEach
                }
                val patches = IdeaTextPatchBuilder.buildPatch(
                    project,
                    listOf(change),
                    Path.of(basePath),
                    false,
                    false,
                )
                val diffText = if (patches.isEmpty()) {
                    "$path (no textual patch)"
                } else {
                    StringWriter().use { writer ->
                        UnifiedDiffWriter.write(project, Path.of(basePath), patches, writer, "\n", null, emptyList())
                        writer.toString()
                    }
                }
                accumulator.add(path, diffText, describeChange(change))
            } catch (_: VcsException) {
                addNewFileFallback(change, path, accumulator)
            } catch (_: IOException) {
                accumulator.skip(path, "patch-write-failed")
            }
        }
        return accumulator.result()
    }

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

    private fun addNewFileFallback(change: Change, path: String, accumulator: CommitDiffAccumulator) {
        if (change.beforeRevision != null) {
            accumulator.skip(path, "patch-build-failed")
            return
        }
        val content = try {
            change.afterRevision?.content.orEmpty()
        } catch (_: VcsException) {
            accumulator.skip(path, "new-file-read-failed")
            return
        }
        if (content.isBlank()) {
            accumulator.add(path, "$path (no textual patch)", "added")
            return
        }
        val boundedContent = content.take(70_000)
        val lines = boundedContent.split("\n", ignoreCase = false, limit = -1)
        val diff = buildString {
            appendLine("--- /dev/null")
            appendLine("+++ b/$path")
            appendLine("@@ -0,0 +1,${lines.size} @@")
            lines.forEach { appendLine("+$it") }
        }
        accumulator.add(path, diff, "added")
    }

    private fun isSingleLineLargeFile(content: String): Boolean {
        return !content.contains('\n') && !content.contains('\r') && content.length > MAX_SINGLE_LINE_CHARS
    }
}
