package com.commitnoteai.settings.ui

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertTrue

class CommitNoteConfigurableTest {
    @Test
    fun `mask api key hides only last six characters for long keys`() {
        val masked = CommitNoteConfigurable.maskApiKey("sk-proj-abcdef123456")

        assertEquals("sk-proj-abcdef******", masked)
    }

    @Test
    fun `mask api key hides all characters for short keys`() {
        val masked = CommitNoteConfigurable.maskApiKey("abc12345")

        assertEquals("********", masked)
    }

    @Test
    fun `mask api key keeps empty key empty`() {
        assertEquals("", CommitNoteConfigurable.maskApiKey(""))
    }

    @Test
    fun `masked api key value is not treated as a new key`() {
        assertTrue(CommitNoteConfigurable.isMaskedApiKeyValue("sk-proj-abcdef******"))
        assertFalse(CommitNoteConfigurable.isMaskedApiKeyValue("sk-proj-abcdef123456"))
    }
}
