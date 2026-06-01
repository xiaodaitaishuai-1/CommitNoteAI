package com.commitnoteai.settings.ui

import com.commitnoteai.ai.ModelListClient
import com.commitnoteai.platform.PasswordSafeBridge
import com.commitnoteai.settings.CommitNoteSettings
import com.intellij.credentialStore.CredentialAttributes
import com.intellij.openapi.application.ApplicationManager
import com.intellij.openapi.options.Configurable
import com.intellij.openapi.options.ConfigurationException
import com.intellij.openapi.progress.ProgressIndicator
import com.intellij.openapi.progress.ProgressManager
import com.intellij.openapi.progress.Task
import com.intellij.openapi.ui.Messages
import com.intellij.ui.JBColor
import com.intellij.ui.components.JBTextArea
import com.intellij.ui.components.JBTextField
import com.intellij.util.ui.JBUI
import java.awt.GridBagConstraints
import java.awt.GridBagLayout
import java.awt.Insets
import javax.swing.JButton
import javax.swing.JComboBox
import javax.swing.JComponent
import javax.swing.JLabel
import javax.swing.JOptionPane
import javax.swing.JPanel
import javax.swing.JScrollPane
import javax.swing.JTextField

class CommitNoteConfigurable : Configurable {
    private val settings = ApplicationManager.getApplication().getService(CommitNoteSettings::class.java)

    private val baseUrlField = JBTextField()
    private val modelField = JBTextField()
    private val temperatureField = JBTextField()
    private val reasoningEffortBox = JComboBox(arrayOf("low", "medium", "high"))
    private val outputStyleBox = JComboBox(arrayOf("通译灵码风格", "简洁风格", "详细风格", "Trae 风格"))
    private val customInstructionsArea = JBTextArea(4, 40).apply {
        lineWrap = true
        wrapStyleWord = true
        emptyText.text = "例如：重点描述广告缓存、状态管理、删除冗余逻辑；不要写空话；正文要具体到类名和方法名。"
    }
    private val apiKeyField = JBTextField()
    private val fetchModelsButton = JButton("获取模型列表")
    private val hintLabel = JLabel("已保存的 Key 会脱敏显示；输入新 Key 后点击 Apply 可替换。").apply {
        foreground = JBColor.GRAY
    }
    private val credentialAttributes = CredentialAttributes("CommitNoteAI", "api-key")

    private val panel = JPanel(GridBagLayout()).apply {
        border = JBUI.Borders.empty(16)
    }

    override fun getDisplayName(): String = "CommitNoteAI"

    override fun createComponent(): JComponent = panel.also { buildPanel() }

    override fun isModified(): Boolean {
        return baseUrlField.text.trim() != settings.apiBaseUrl ||
            modelField.text.trim() != settings.model ||
            temperatureField.text.trim().toDoubleOrNull() != settings.temperature ||
            (reasoningEffortBox.selectedItem as? String) != settings.reasoningEffort ||
            selectedOutputStyle() != settings.outputStyle ||
            customInstructionsArea.text.trim() != settings.customInstructions ||
            hasNewApiKeyInput()
    }

    override fun apply() {
        val baseUrl = baseUrlField.text.trim()
        val model = modelField.text.trim()
        val temperature = temperatureField.text.trim().toDoubleOrNull()
            ?: throw ConfigurationException("Temperature 必须是数字")

        if (baseUrl.isBlank()) {
            throw ConfigurationException("API Base URL 不能为空")
        }
        if (model.isBlank()) {
            throw ConfigurationException("Model 不能为空")
        }
        if (temperature !in 0.0..2.0) {
            throw ConfigurationException("Temperature 必须在 0.0 到 2.0 之间")
        }

        settings.apiBaseUrl = baseUrl
        settings.model = model
        settings.temperature = temperature
        settings.reasoningEffort = reasoningEffortBox.selectedItem as? String ?: "medium"
        settings.outputStyle = selectedOutputStyle()
        settings.customInstructions = customInstructionsArea.text

        val apiKey = apiKeyField.text.trim()
        if (apiKey.isNotBlank() && !isMaskedApiKeyValue(apiKey)) {
            PasswordSafeBridge.setPassword(credentialAttributes, apiKey)
        }
    }

    override fun reset() {
        baseUrlField.text = settings.apiBaseUrl
        modelField.text = settings.model
        temperatureField.text = settings.temperature.toString()
        reasoningEffortBox.selectedItem = settings.reasoningEffort
        outputStyleBox.selectedItem = labelForOutputStyle(settings.outputStyle)
        customInstructionsArea.text = settings.customInstructions
        apiKeyField.text = maskApiKey(PasswordSafeBridge.getPassword(credentialAttributes).orEmpty())
    }

    override fun disposeUIResources() {}

    private fun buildPanel() {
        if (panel.componentCount > 0) return

        baseUrlField.text = settings.apiBaseUrl
        modelField.text = settings.model
        temperatureField.text = settings.temperature.toString()
        reasoningEffortBox.selectedItem = settings.reasoningEffort
        outputStyleBox.selectedItem = labelForOutputStyle(settings.outputStyle)
        customInstructionsArea.text = settings.customInstructions
        apiKeyField.text = maskApiKey(PasswordSafeBridge.getPassword(credentialAttributes).orEmpty())
        fetchModelsButton.addActionListener { fetchModels() }

        addRow("API Base URL", baseUrlField, 0)
        addRow("Model", modelField, 1)
        addButtonRow(fetchModelsButton, 2)
        addRow("Temperature", temperatureField, 3)
        addComboRow("Reasoning Effort", reasoningEffortBox, 4)
        addComboRow("输出样式", outputStyleBox, 5)
        addTextAreaRow("AI 关键词/偏好", customInstructionsArea, 6)
        addRow("API Key", apiKeyField, 7)
        addHintRow(8)
    }

    private fun selectedOutputStyle(): String {
        return when (outputStyleBox.selectedItem as? String) {
            "简洁风格" -> CommitNoteSettings.OUTPUT_STYLE_SIMPLE
            "详细风格" -> CommitNoteSettings.OUTPUT_STYLE_DETAILED
            "Trae 风格" -> CommitNoteSettings.OUTPUT_STYLE_TRAE
            else -> CommitNoteSettings.OUTPUT_STYLE_TONGYI
        }
    }

    private fun labelForOutputStyle(style: String): String {
        return when (CommitNoteSettings.normalizeOutputStyle(style)) {
            CommitNoteSettings.OUTPUT_STYLE_SIMPLE -> "简洁风格"
            CommitNoteSettings.OUTPUT_STYLE_DETAILED -> "详细风格"
            CommitNoteSettings.OUTPUT_STYLE_TRAE -> "Trae 风格"
            else -> "通译灵码风格"
        }
    }

    private fun hasNewApiKeyInput(): Boolean {
        val value = apiKeyField.text.trim()
        return value.isNotBlank() && !isMaskedApiKeyValue(value)
    }

    private fun addRow(labelText: String, field: JTextField, row: Int) {
        val label = JLabel(labelText)
        val constraintsLabel = GridBagConstraints().apply {
            gridx = 0
            gridy = row
            anchor = GridBagConstraints.WEST
            insets = Insets(0, 0, 8, 12)
        }
        val constraintsField = GridBagConstraints().apply {
            gridx = 1
            gridy = row
            weightx = 1.0
            fill = GridBagConstraints.HORIZONTAL
            insets = Insets(0, 0, 8, 0)
        }

        panel.add(label, constraintsLabel)
        panel.add(field, constraintsField)
    }

    private fun addComboRow(labelText: String, field: JComboBox<String>, row: Int) {
        val label = JLabel(labelText)
        val constraintsLabel = GridBagConstraints().apply {
            gridx = 0
            gridy = row
            anchor = GridBagConstraints.WEST
            insets = Insets(0, 0, 8, 12)
        }
        val constraintsField = GridBagConstraints().apply {
            gridx = 1
            gridy = row
            weightx = 1.0
            fill = GridBagConstraints.HORIZONTAL
            insets = Insets(0, 0, 8, 0)
        }

        panel.add(label, constraintsLabel)
        panel.add(field, constraintsField)
    }

    private fun addTextAreaRow(labelText: String, field: JBTextArea, row: Int) {
        val label = JLabel(labelText)
        val constraintsLabel = GridBagConstraints().apply {
            gridx = 0
            gridy = row
            anchor = GridBagConstraints.NORTHWEST
            insets = Insets(0, 0, 8, 12)
        }
        val constraintsField = GridBagConstraints().apply {
            gridx = 1
            gridy = row
            weightx = 1.0
            fill = GridBagConstraints.HORIZONTAL
            insets = Insets(0, 0, 8, 0)
        }

        panel.add(label, constraintsLabel)
        panel.add(JScrollPane(field), constraintsField)
    }

    private fun addButtonRow(button: JButton, row: Int) {
        val constraints = GridBagConstraints().apply {
            gridx = 1
            gridy = row
            anchor = GridBagConstraints.WEST
            insets = Insets(0, 0, 8, 0)
        }
        panel.add(button, constraints)
    }

    private fun addHintRow(row: Int) {
        val constraints = GridBagConstraints().apply {
            gridx = 0
            gridy = row
            gridwidth = 2
            anchor = GridBagConstraints.WEST
            insets = Insets(0, 0, 0, 0)
        }
        panel.add(hintLabel, constraints)
    }

    private fun fetchModels() {
        val baseUrl = baseUrlField.text.trim()
        if (baseUrl.isBlank()) {
            Messages.showErrorDialog(panel, "API Base URL 不能为空", "CommitNoteAI")
            return
        }

        val typedApiKey = apiKeyField.text.trim()
        val apiKey = typedApiKey
            .takeUnless { it.isBlank() || isMaskedApiKeyValue(it) }
            ?: PasswordSafeBridge.getPassword(credentialAttributes).orEmpty()
        if (apiKey.isBlank()) {
            Messages.showErrorDialog(panel, "请先输入或保存 API Key", "CommitNoteAI")
            return
        }

        fetchModelsButton.isEnabled = false
        ProgressManager.getInstance().run(object : Task.Backgroundable(null, "Fetching model list", false) {
            override fun run(indicator: ProgressIndicator) {
                try {
                    val models = ModelListClient().fetchModels(baseUrl, apiKey)
                    ApplicationManager.getApplication().invokeLater {
                        showModelChooser(models)
                        fetchModelsButton.isEnabled = true
                    }
                } catch (error: Throwable) {
                    ApplicationManager.getApplication().invokeLater {
                        fetchModelsButton.isEnabled = true
                        Messages.showErrorDialog(panel, error.message ?: "获取模型列表失败", "CommitNoteAI")
                    }
                }
            }
        })
    }

    private fun showModelChooser(models: List<String>) {
        val selection = JOptionPane.showInputDialog(
            panel,
            "选择模型",
            "CommitNoteAI",
            JOptionPane.PLAIN_MESSAGE,
            null,
            models.toTypedArray(),
            modelField.text.trim().takeIf { it in models } ?: models.first(),
        ) as? String
        if (!selection.isNullOrBlank()) {
            modelField.text = selection
        }
    }

    companion object {
        internal fun maskApiKey(value: String): String {
            val trimmed = value.trim()
            if (trimmed.isBlank()) return ""
            return if (trimmed.length <= 8) {
                "*".repeat(trimmed.length)
            } else {
                trimmed.dropLast(6) + "*".repeat(6)
            }
        }

        internal fun isMaskedApiKeyValue(value: String): Boolean {
            return value.trim().contains('*')
        }
    }
}
