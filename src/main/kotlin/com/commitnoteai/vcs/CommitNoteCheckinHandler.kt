@file:Suppress("DEPRECATION")

package com.commitnoteai.vcs

import com.commitnoteai.ai.CommitNoteGenerator
import com.intellij.icons.AllIcons
import com.intellij.openapi.progress.ProgressIndicator
import com.intellij.openapi.progress.ProgressManager
import com.intellij.openapi.progress.Task
import com.intellij.openapi.ui.Messages
import com.intellij.openapi.vcs.CheckinProjectPanel
import com.intellij.openapi.vcs.checkin.CheckinHandler
import com.intellij.openapi.vcs.changes.Change
import com.intellij.openapi.vcs.ui.RefreshableOnComponent
import com.intellij.ui.components.JBLabel
import com.intellij.util.ui.JBUI
import java.awt.BorderLayout
import java.awt.FlowLayout
import javax.swing.JButton
import javax.swing.JComponent
import javax.swing.JPanel

class CommitNoteCheckinHandler(private val panel: CheckinProjectPanel) : CheckinHandler() {
    private val ui by lazy { CommitNotePanel(panel) }

    override fun getBeforeCheckinConfigurationPanel(): RefreshableOnComponent = ui
}

private class CommitNotePanel(
    private val checkinPanel: CheckinProjectPanel,
) : JPanel(BorderLayout()), RefreshableOnComponent {
    private val statusLabel = JBLabel("点击生成提交记录后，会把结果回填到提交框。")
    private val generateButton = JButton("生成提交记录", AllIcons.Actions.Refresh)

    init {
        border = JBUI.Borders.empty(8, 0)
        val row = JPanel(FlowLayout(FlowLayout.LEFT, 8, 0)).apply {
            isOpaque = false
            add(generateButton)
            add(statusLabel)
        }
        add(row, BorderLayout.CENTER)

        generateButton.addActionListener {
            generateCommitMessage()
        }
    }

    override fun getComponent(): JComponent = this

    @Deprecated("Part of the platform refresh contract")
    override fun saveState() {}

    @Deprecated("Part of the platform refresh contract")
    override fun restoreState() {}

    @Deprecated("Part of the platform refresh contract")
    override fun refresh() {}

    private fun generateCommitMessage() {
        val project = checkinPanel.project
        val component = checkinPanel.component
        val changes = checkinPanel.selectedChanges.toList()
        if (changes.isEmpty()) {
            Messages.showInfoMessage(component, "当前没有选中的变更。", "CommitNoteAI")
            return
        }

        val currentDraft = checkinPanel.getCommitMessage()
        generateButton.isEnabled = false
        statusLabel.text = "正在生成提交记录..."

        ProgressManager.getInstance().run(object : Task.Backgroundable(project, "Generating commit message", false) {
            override fun run(indicator: ProgressIndicator) {
                try {
                    val formatted = generateFormatted(currentDraft, changes)

                    com.intellij.openapi.application.ApplicationManager.getApplication().invokeLater {
                        val dialog = CommitMessagePreviewDialog(
                            project = project,
                            originalDraft = currentDraft,
                            generatedMessage = formatted,
                            regenerateMessage = {
                                generateFormatted(currentDraft, changes)
                            },
                        )
                        if (dialog.showAndGet()) {
                            checkinPanel.setCommitMessage(dialog.editedMessage)
                            statusLabel.text = "已替换提交记录"
                        } else {
                            statusLabel.text = "已取消替换"
                        }
                        generateButton.isEnabled = true
                    }
                } catch (error: Throwable) {
                    com.intellij.openapi.application.ApplicationManager.getApplication().invokeLater {
                        statusLabel.text = "生成失败"
                        generateButton.isEnabled = true
                        Messages.showErrorDialog(component, error.message ?: "生成提交记录失败", "CommitNoteAI")
                    }
                }
            }
        })
    }

    private fun generateFormatted(currentDraft: String, changes: List<Change>): String {
        val generator = CommitNoteGenerator()
        val message = generator.generate(checkinPanel.project, currentDraft, changes)
        return CommitMessageFormatter.format(message)
    }
}
