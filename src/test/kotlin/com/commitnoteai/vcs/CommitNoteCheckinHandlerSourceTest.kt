package com.commitnoteai.vcs

import java.nio.file.Files
import java.nio.file.Path
import kotlin.test.Test
import kotlin.test.assertContains
import kotlin.test.assertFalse

class CommitNoteCheckinHandlerSourceTest {
    @Test
    fun `legacy panel fills existing editor through typewriter without preview dialog`() {
        val source = Files.readString(Path.of("src/main/kotlin/com/commitnoteai/vcs/CommitNoteCheckinHandler.kt"))

        assertFalse(source.contains("CommitMessagePreviewDialog"))
        assertFalse(source.contains("showAndGet()"))
        assertContains(source, "CommitMessageTypewriter.start")
        assertContains(source, "checkinPanel.setCommitMessage(text)")
        assertContains(source, "generateButton.isEnabled = true")
    }
}
