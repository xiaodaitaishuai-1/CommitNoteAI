package com.commitnoteai.model

data class CommitChangeSnapshot(
    val path: String,
    val changeType: String,
    val diffText: String = "",
    val beforeSnippet: String? = null,
    val afterSnippet: String? = null,
    val originText: String? = null,
) {
    constructor(
        path: String,
        changeType: String,
        beforeSnippet: String?,
        afterSnippet: String?,
        originText: String?,
    ) : this(path, changeType, "", beforeSnippet, afterSnippet, originText)
}

data class CommitChangeSkip(
    val path: String,
    val reason: String,
)

data class CommitChangeCollection(
    val changes: List<CommitChangeSnapshot>,
    val skippedChanges: List<CommitChangeSkip> = emptyList(),
    val isTruncated: Boolean = false,
)

data class CommitPromptPayload(
    val currentDraft: String,
    val changes: List<CommitChangeSnapshot>,
    val outputStyle: String = "tongyi",
    val customInstructions: String = "",
    val projectContext: String = "",
    val changeCollection: CommitChangeCollection = CommitChangeCollection(changes),
)

data class GeneratedCommitMessage(
    val title: String,
    val bodyLines: List<String>,
)

data class CommitMessageWirePayload(
    val title: String? = null,
    val bodyLines: List<String>? = emptyList(),
)
