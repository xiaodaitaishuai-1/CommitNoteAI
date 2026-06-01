package com.commitnoteai.vcs

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertTrue

class TypewriterTextTest {
    @Test
    fun `step reveals text in chunks until complete`() {
        var state = TypewriterText.State(target = "abcdef", visibleLength = 0)

        state = TypewriterText.step(state, chunkSize = 2)
        assertEquals("ab", state.visibleText)
        assertFalse(state.isComplete)

        state = TypewriterText.step(state, chunkSize = 4)
        assertEquals("abcdef", state.visibleText)
        assertTrue(state.isComplete)
    }
}
