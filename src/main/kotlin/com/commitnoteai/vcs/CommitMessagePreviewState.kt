package com.commitnoteai.vcs

data class CommitMessagePreviewState(
    val originalDraft: String,
    val generatedMessage: String,
) {
    fun replacementText(editedMessage: String): String = editedMessage.trim()

    fun withGeneratedMessage(message: String): CommitMessagePreviewState {
        return copy(generatedMessage = message)
    }
}
