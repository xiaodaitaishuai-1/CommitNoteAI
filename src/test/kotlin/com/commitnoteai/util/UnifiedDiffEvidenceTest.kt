package com.commitnoteai.util

import kotlin.test.Test
import kotlin.test.assertContains
import kotlin.test.assertEquals

class UnifiedDiffEvidenceTest {
    @Test
    fun `parse keeps distant hunks and reconstructs both versions`() {
        val evidence = UnifiedDiffEvidence.parse(
            """
            --- a/src/Login.kt
            +++ b/src/Login.kt
            @@ -1 +1 @@
            -fun oldLogin() = false
            +fun newLogin() = true
            @@ -40 +40 @@
            -val oldTitle = "old"
            +val newTitle = "new"
            """.trimIndent(),
        )

        assertEquals("src/Login.kt", evidence.path)
        assertContains(evidence.beforeText, "oldLogin")
        assertContains(evidence.beforeText, "oldTitle")
        assertContains(evidence.afterText, "newLogin")
        assertContains(evidence.afterText, "newTitle")
        assertEquals(
            listOf("fun newLogin() = true", "val newTitle = \"new\""),
            evidence.addedLines,
        )
    }

    @Test
    fun `parse tolerates empty patch entries`() {
        val evidence = UnifiedDiffEvidence.parse("src/Generated.kt (no textual patch)")

        assertEquals("src/Generated.kt", evidence.path)
        assertEquals(emptyList(), evidence.addedLines)
        assertEquals(emptyList(), evidence.removedLines)
    }
}
