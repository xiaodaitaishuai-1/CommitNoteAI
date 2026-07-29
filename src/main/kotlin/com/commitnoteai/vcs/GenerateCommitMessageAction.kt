package com.commitnoteai.vcs

import com.commitnoteai.ai.CommitNoteGenerator
import com.intellij.openapi.actionSystem.ActionUpdateThread
import com.intellij.openapi.actionSystem.AnActionEvent
import com.intellij.openapi.actionSystem.CommonDataKeys
import com.intellij.openapi.progress.ProgressIndicator
import com.intellij.openapi.progress.ProgressManager
import com.intellij.openapi.progress.Task
import com.intellij.openapi.project.DumbAwareAction
import com.intellij.openapi.ui.Messages
import com.intellij.openapi.vcs.VcsDataKeys
import com.intellij.openapi.vcs.changes.Change
import com.intellij.vcs.commit.CommitWorkflowUi
import java.util.concurrent.atomic.AtomicBoolean

class GenerateCommitMessageAction : DumbAwareAction() {
    private val generating = AtomicBoolean(false)

    override fun getActionUpdateThread(): ActionUpdateThread = ActionUpdateThread.BGT

    override fun update(e: AnActionEvent) {
        val workflowUi = e.getData(VcsDataKeys.COMMIT_WORKFLOW_UI)
        val hasIncludedChanges = workflowUi?.getIncludedChanges()?.isNotEmpty() == true ||
            workflowUi?.getIncludedUnversionedFiles()?.isNotEmpty() == true
        val isGenerating = generating.get()
        e.presentation.isVisible = workflowUi != null
        e.presentation.isEnabled = hasIncludedChanges && !isGenerating
        e.presentation.description = if (isGenerating) {
            "正在生成提交记录..."
        } else {
            "根据已勾选的变更生成提交记录"
        }
    }

    override fun actionPerformed(e: AnActionEvent) {
        val project = e.getData(CommonDataKeys.PROJECT) ?: return
        val workflowUi = e.getData(VcsDataKeys.COMMIT_WORKFLOW_UI) ?: return
        val changes = workflowUi.getIncludedChanges()
        val unversionedFiles = workflowUi.getIncludedUnversionedFiles()
        if (changes.isEmpty() && unversionedFiles.isEmpty()) {
            Messages.showInfoMessage(project, "当前没有勾选的变更。", "CommitNoteAI")
            return
        }
        if (!generating.compareAndSet(false, true)) {
            return
        }

        val commitMessageUi = workflowUi.commitMessageUi
        val currentDraft = commitMessageUi.text

        ProgressManager.getInstance().run(object : Task.Backgroundable(project, "CommitNoteAI 正在生成提交记录", false) {
            override fun run(indicator: ProgressIndicator) {
                try {
                    val formatted = generateFormatted(project, currentDraft, changes, unversionedFiles)
                    finish(workflowUi) {
                        val dialog = CommitMessagePreviewDialog(
                            project = project,
                            originalDraft = currentDraft,
                            generatedMessage = formatted,
                            regenerateMessage = {
                                generateFormatted(project, currentDraft, changes, unversionedFiles)
                            },
                        )
                        if (dialog.showAndGet()) {
                            commitMessageUi.text = dialog.editedMessage
                        }
                    }
                } catch (error: Throwable) {
                    finish(workflowUi) {
                        Messages.showErrorDialog(project, error.message ?: "生成提交记录失败", "CommitNoteAI")
                    }
                }
            }
        })
    }

    private fun generateFormatted(
        project: com.intellij.openapi.project.Project,
        currentDraft: String,
        changes: List<Change>,
        unversionedFiles: List<com.intellij.openapi.vcs.FilePath>,
    ): String {
        val message = CommitNoteGenerator().generate(project, currentDraft, changes, unversionedFiles)
        return CommitMessageFormatter.format(message)
    }

    private fun finish(workflowUi: CommitWorkflowUi, onUiThread: () -> Unit) {
        com.intellij.openapi.application.ApplicationManager.getApplication().invokeLater {
            try {
                onUiThread()
            } finally {
                if (generating.get()) {
                    generating.set(false)
                }
            }
        }
    }
}
