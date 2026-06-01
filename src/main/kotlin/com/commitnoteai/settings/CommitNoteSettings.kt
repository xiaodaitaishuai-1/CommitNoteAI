package com.commitnoteai.settings

import com.intellij.openapi.application.ApplicationManager
import com.intellij.openapi.components.PersistentStateComponent
import com.intellij.openapi.components.Service
import com.intellij.openapi.components.State
import com.intellij.openapi.components.Storage

@State(name = "CommitNoteSettings", storages = [Storage("commitnoteai.xml")])
@Service(Service.Level.APP)
class CommitNoteSettings : PersistentStateComponent<CommitNoteSettings.State> {
    data class State(
        var apiBaseUrl: String = "https://api.openai.com/v1",
        var model: String = "gpt-5.2",
        var temperature: Double = 0.2,
        var reasoningEffort: String = "medium",
        var outputStyle: String = "tongyi",
        var customInstructions: String = "",
    )

    private var state = State()

    override fun getState(): State = state

    override fun loadState(state: State) {
        this.state = state.copy(
            apiBaseUrl = normalizeBaseUrl(state.apiBaseUrl),
            model = state.model.trim(),
            temperature = state.temperature.coerceIn(0.0, 2.0),
            reasoningEffort = normalizeReasoningEffort(state.reasoningEffort),
            outputStyle = normalizeOutputStyle(state.outputStyle),
            customInstructions = state.customInstructions.trim(),
        )
    }

    var apiBaseUrl: String
        get() = state.apiBaseUrl
        set(value) {
            state = state.copy(apiBaseUrl = normalizeBaseUrl(value))
        }

    var model: String
        get() = state.model
        set(value) {
            state = state.copy(model = value.trim())
        }

    var temperature: Double
        get() = state.temperature
        set(value) {
            state = state.copy(temperature = value.coerceIn(0.0, 2.0))
        }

    var reasoningEffort: String
        get() = state.reasoningEffort
        set(value) {
            state = state.copy(reasoningEffort = normalizeReasoningEffort(value))
        }

    var outputStyle: String
        get() = state.outputStyle
        set(value) {
            state = state.copy(outputStyle = normalizeOutputStyle(value))
        }

    var customInstructions: String
        get() = state.customInstructions
        set(value) {
            state = state.copy(customInstructions = value.trim())
        }

    companion object {
        const val OUTPUT_STYLE_TONGYI = "tongyi"
        const val OUTPUT_STYLE_SIMPLE = "simple"
        const val OUTPUT_STYLE_DETAILED = "detailed"
        const val OUTPUT_STYLE_TRAE = "trae"

        fun getInstance(): CommitNoteSettings = ApplicationManager.getApplication().getService(CommitNoteSettings::class.java)

        private fun normalizeBaseUrl(value: String): String = value.trim().trimEnd('/')

        fun normalizeReasoningEffort(value: String): String {
            val normalized = value.trim().lowercase()
            return if (normalized in setOf("low", "medium", "high")) normalized else "medium"
        }

        fun normalizeOutputStyle(value: String): String {
            val normalized = value.trim().lowercase()
            return if (normalized in setOf(
                    OUTPUT_STYLE_TONGYI,
                    OUTPUT_STYLE_SIMPLE,
                    OUTPUT_STYLE_DETAILED,
                    OUTPUT_STYLE_TRAE,
                )) {
                normalized
            } else {
                OUTPUT_STYLE_TONGYI
            }
        }
    }
}
