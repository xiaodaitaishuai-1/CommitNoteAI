# Inline Typewriter Commit Message Design

## Goal

Replace the generated-message preview dialog with direct, animated insertion into the existing commit-message editor.

## User Flow

1. The user invokes CommitNoteAI from the commit workflow with selected changes.
2. The action generates and formats a commit message in the background.
3. On success, the existing commit-message text is cleared.
4. The generated message is written into that same editor with a typewriter animation.
5. When the animation finishes, the action becomes available again.

## Implementation

`GenerateCommitMessageAction` will no longer construct or show `CommitMessagePreviewDialog`. Instead, it will use a Swing `Timer` on the UI thread to repeatedly advance `TypewriterText.State` and assign the current visible text to `CommitWorkflowUi.commitMessageUi.text`.

The action remains guarded by its existing `generating` flag throughout generation and animation so the user cannot start a second generation before the first one has finished. The timer updates text in chunks rather than one character per tick, keeping long commit messages responsive while retaining the typewriter effect.

The preview dialog class is left in place for this change because it is not part of the active flow; deleting it is unnecessary scope expansion.

## Error Handling

If generation fails, the existing commit message remains untouched and the existing error dialog is shown. The action guard is released in every success and failure path.

## Validation

Update source-level action tests to prove the dialog is absent and inline typewriter insertion is used. Retain and extend unit tests for chunked `TypewriterText` progression, including non-positive chunk sizes and completion bounds.
