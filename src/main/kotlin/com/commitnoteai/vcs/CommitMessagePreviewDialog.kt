package com.commitnoteai.vcs

import com.intellij.openapi.application.ApplicationManager
import com.intellij.openapi.progress.ProgressIndicator
import com.intellij.openapi.progress.ProgressManager
import com.intellij.openapi.progress.Task
import com.intellij.openapi.project.Project
import com.intellij.openapi.ui.DialogWrapper
import com.intellij.openapi.ui.Messages
import com.intellij.ui.components.JBLabel
import com.intellij.ui.components.JBTextArea
import com.intellij.util.ui.JBUI
import java.awt.BorderLayout
import java.awt.GridLayout
import javax.swing.JButton
import javax.swing.JComponent
import javax.swing.JPanel
import javax.swing.JScrollPane

class CommitMessagePreviewDialog(
    private val project: Project?,
    originalDraft: String,
    generatedMessage: String,
    private val regenerateMessage: () -> String,
) : DialogWrapper(project) {
    private var state = CommitMessagePreviewState(
        originalDraft = originalDraft,
        generatedMessage = generatedMessage,
    )
    private val originalArea = JBTextArea(displayDraft(originalDraft), 12, 42).apply {
        isEditable = false
        lineWrap = true
        wrapStyleWord = true
    }
    private val generatedArea = JBTextArea(generatedMessage, 12, 42).apply {
        lineWrap = true
        wrapStyleWord = true
    }
    private val regenerateButton = JButton("重新生成")
    private val statusLabel = JBLabel("可编辑 AI 生成结果，点击替换后写入提交框。")

    val editedMessage: String
        get() = state.replacementText(generatedArea.text)

    init {
        title = "预览提交记录"
        setOKButtonText("替换")
        regenerateButton.addActionListener { regenerate() }
        init()
    }

    override fun createCenterPanel(): JComponent {
        val content = JPanel(BorderLayout(0, 8)).apply {
            border = JBUI.Borders.empty(8)
        }
        val previewPanel = JPanel(GridLayout(1, 2, 8, 0)).apply {
            add(labeledArea("当前提交信息", originalArea))
            add(labeledArea("AI 新提交信息", generatedArea))
        }
        val footer = JPanel(BorderLayout(8, 0)).apply {
            add(regenerateButton, BorderLayout.WEST)
            add(statusLabel, BorderLayout.CENTER)
        }

        content.add(previewPanel, BorderLayout.CENTER)
        content.add(footer, BorderLayout.SOUTH)
        return content
    }

    private fun regenerate() {
        setBusy(true)
        ProgressManager.getInstance().run(object : Task.Backgroundable(project, "CommitNoteAI 重新生成提交记录", false) {
            override fun run(indicator: ProgressIndicator) {
                try {
                    val regenerated = regenerateMessage()
                    ApplicationManager.getApplication().invokeLater {
                        state = state.withGeneratedMessage(regenerated)
                        generatedArea.text = regenerated
                        generatedArea.caretPosition = 0
                        statusLabel.text = "已重新生成，可继续编辑后替换。"
                        setBusy(false)
                    }
                } catch (error: Throwable) {
                    ApplicationManager.getApplication().invokeLater {
                        statusLabel.text = "重新生成失败"
                        setBusy(false)
                        Messages.showErrorDialog(project, error.message ?: "重新生成提交记录失败", "CommitNoteAI")
                    }
                }
            }
        })
    }

    private fun setBusy(busy: Boolean) {
        regenerateButton.isEnabled = !busy
        setOKActionEnabled(!busy)
        statusLabel.text = if (busy) {
            "正在重新生成提交记录..."
        } else {
            statusLabel.text
        }
    }

    private fun labeledArea(label: String, area: JBTextArea): JComponent {
        return JPanel(BorderLayout(0, 4)).apply {
            add(JBLabel(label), BorderLayout.NORTH)
            add(JScrollPane(area), BorderLayout.CENTER)
        }
    }

    private fun displayDraft(value: String): String {
        return value.ifBlank { "（当前提交信息为空）" }
    }
}
