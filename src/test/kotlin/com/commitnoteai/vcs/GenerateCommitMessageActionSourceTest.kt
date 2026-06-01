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
    fun `action keeps draft until typewriter starts and uses chinese background title`() {
        val source = Files.readString(Path.of("src/main/kotlin/com/commitnoteai/vcs/GenerateCommitMessageAction.kt"))

        assertContains(source, "CommitNoteAI 正在生成提交记录")
        assertContains(source, "commitMessageUi.text = \"\"")
        assertContains(source, "startTypewriter")
    }
}
