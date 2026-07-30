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
    fun `action fills existing editor through typewriter without a preview dialog`() {
        val source = Files.readString(Path.of("src/main/kotlin/com/commitnoteai/vcs/GenerateCommitMessageAction.kt"))

        assertContains(source, "CommitNoteAI 正在生成提交记录")
        assertFalse(source.contains("CommitMessagePreviewDialog"))
        assertFalse(source.contains("dialog.showAndGet()"))
        assertContains(source, "CommitMessageTypewriter.start")
        assertContains(source, "commitMessageUi.text = text")
        assertContains(source, "generating.set(false)")
    }

    @Test
    fun `action includes selected unversioned files in commit generation`() {
        val source = Files.readString(Path.of("src/main/kotlin/com/commitnoteai/vcs/GenerateCommitMessageAction.kt"))

        assertContains(source, "workflowUi.getIncludedUnversionedFiles()")
        assertContains(source, "unversionedFiles")
    }
}
