package com.commitnoteai.util

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

class CommitChangeCollectorTest {
    @Test
    fun `accumulator stops at Qoder entry and character limits`() {
        val result = CommitDiffAccumulator(maxEntries = 2, maxCharacters = 12)
            .add("first", "123456")
            .add("second", "123456")
            .add("third", "ignored")
            .result()

        assertEquals(listOf("first", "second"), result.changes.map { it.path })
        assertTrue(result.isTruncated)
    }

    @Test
    fun `accumulator records skipped binary change`() {
        val result = CommitDiffAccumulator()
            .skip("assets/logo.png", "binary")
            .result()

        assertEquals("binary", result.skippedChanges.single().reason)
    }
}
