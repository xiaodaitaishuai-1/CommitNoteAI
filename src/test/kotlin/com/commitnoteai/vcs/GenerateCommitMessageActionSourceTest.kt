package com.commitnoteai.vcs

import java.nio.file.Files
import java.nio.file.Path
import kotlin.test.Test
import kotlin.test.assertContains
import kotlin.test.assertFalse

class GenerateCommitMessageActionSourceTest {
    @Test
    fun `action does not use commit message loading spinner`() {
        val source = Files.readString(Path.of("src/main/kotlin/com/commitnoteai/vcs/GenerateCommitMessageAction.kt"))

        assertFalse(source.contains("commitMessageUi.startLoading()"))
        assertFalse(source.contains("commitMessageUi.stopLoading()"))
    }

    @Test
    fun `action previews generated message instead of directly replacing draft`() {
        val source = Files.readString(Path.of("src/main/kotlin/com/commitnoteai/vcs/GenerateCommitMessageAction.kt"))

        assertContains(source, "CommitNoteAI 正在生成提交记录")
        assertContains(source, "CommitMessagePreviewDialog")
        assertContains(source, "commitMessageUi.text = dialog.editedMessage")
        assertFalse(source.contains("commitMessageUi.text = \"\""))
        assertFalse(source.contains("startTypewriter"))
    }

    @Test
    fun `action includes selected unversioned files in commit generation`() {
        val source = Files.readString(Path.of("src/main/kotlin/com/commitnoteai/vcs/GenerateCommitMessageAction.kt"))

        assertContains(source, "workflowUi.getIncludedUnversionedFiles()")
        assertContains(source, "unversionedFiles")
    }
}
