package com.commitnoteai.settings

import kotlin.test.Test
import kotlin.test.assertEquals

class CommitNoteSettingsTest {
    @Test
    fun `default reasoning effort is medium`() {
        val state = CommitNoteSettings.State()

        assertEquals("medium", state.reasoningEffort)
    }

    @Test
    fun `default output style is tongyi and custom instructions are empty`() {
        val state = CommitNoteSettings.State()

        assertEquals("tongyi", state.outputStyle)
        assertEquals("", state.customInstructions)
    }

    @Test
    fun `normalize reasoning effort falls back to medium`() {
        assertEquals("medium", CommitNoteSettings.normalizeReasoningEffort("invalid"))
        assertEquals("high", CommitNoteSettings.normalizeReasoningEffort("HIGH"))
    }

    @Test
    fun `normalize output style accepts trae and falls back to tongyi`() {
        assertEquals("tongyi", CommitNoteSettings.normalizeOutputStyle("invalid"))
        assertEquals("simple", CommitNoteSettings.normalizeOutputStyle("SIMPLE"))
        assertEquals("detailed", CommitNoteSettings.normalizeOutputStyle(" detailed "))
        assertEquals("trae", CommitNoteSettings.normalizeOutputStyle("TRAE"))
    }
}
