package com.commitnoteai.model

data class CommitChangeSnapshot(
    val path: String,
    val changeType: String,
    val beforeSnippet: String?,
    val afterSnippet: String?,
    val originText: String?,
)

data class CommitPromptPayload(
    val currentDraft: String,
    val changes: List<CommitChangeSnapshot>,
    val outputStyle: String = "tongyi",
    val customInstructions: String = "",
)

data class GeneratedCommitMessage(
    val title: String,
    val bodyLines: List<String>,
)

data class CommitMessageWirePayload(
    val title: String? = null,
    val bodyLines: List<String>? = emptyList(),
)
