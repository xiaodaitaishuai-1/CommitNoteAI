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

    @Test
    fun `step treats zero and negative chunk sizes as one character`() {
        val initial = TypewriterText.State(target = "abc", visibleLength = 0)

        assertEquals("a", TypewriterText.step(initial, chunkSize = 0).visibleText)
        assertEquals("a", TypewriterText.step(initial, chunkSize = -3).visibleText)
    }

    @Test
    fun `step never advances beyond target length`() {
        val completed = TypewriterText.step(
            TypewriterText.State(target = "abc", visibleLength = 2),
            chunkSize = 99,
        )

        assertEquals(3, completed.visibleLength)
        assertEquals("abc", completed.visibleText)
        assertTrue(completed.isComplete)
    }
}
