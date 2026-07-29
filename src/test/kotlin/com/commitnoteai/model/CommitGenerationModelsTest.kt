package com.commitnoteai.model

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

class CommitGenerationModelsTest {
    @Test
    fun `collection exposes usable diffs and truncation metadata`() {
        val collection = CommitChangeCollection(
            changes = listOf(
                CommitChangeSnapshot(
                    path = "src/Login.kt",
                    changeType = "modified",
                    diffText = "@@ -1 +1 @@\n-old\n+new",
                ),
            ),
            skippedChanges = listOf(CommitChangeSkip("assets/logo.png", "binary")),
            isTruncated = true,
        )

        assertEquals(1, collection.changes.size)
        assertEquals("binary", collection.skippedChanges.single().reason)
        assertTrue(collection.isTruncated)
    }
}
