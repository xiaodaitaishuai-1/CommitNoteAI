package com.commitnoteai.util

import com.commitnoteai.model.CommitChangeSnapshot
import kotlin.test.Test
import kotlin.test.assertContains
import kotlin.test.assertFalse

class ChangedSymbolExtractorTest {
    @Test
    fun `extract identifies added and removed notification permission symbols`() {
        val symbols = ChangedSymbolExtractor.extract(
            listOf(
                CommitChangeSnapshot(
                    path = "app/src/main/java/com/clarity/photo/activity/HomeActivity.kt",
                    changeType = "modified",
                    beforeSnippet = "showNotificationPermissionRationaleDialog(notificationPermissionLauncher)",
                    afterSnippet = "notificationPermissionRequestInFlight = true\nrequestNotificationPermissionDirectly(notificationPermissionLauncher)",
                    originText = null,
                ),
            ),
        )

        assertContains(symbols.removedSymbols, "showNotificationPermissionRationaleDialog")
        assertContains(symbols.addedSymbols, "requestNotificationPermissionDirectly")
        assertContains(symbols.addedSymbols, "notificationPermissionRequestInFlight")
        assertContains(symbols.keptSymbols, "notificationPermissionLauncher")
    }

    @Test
    fun `extract ignores kotlin and java keywords`() {
        val symbols = ChangedSymbolExtractor.extract(
            listOf(
                CommitChangeSnapshot(
                    path = "app/src/main/java/Foo.kt",
                    changeType = "modified",
                    beforeSnippet = "private fun oldName() = true",
                    afterSnippet = "private fun newName() = false",
                    originText = null,
                ),
            ),
        )

        assertFalse("private" in symbols.addedSymbols)
        assertFalse("fun" in symbols.addedSymbols)
        assertFalse("true" in symbols.removedSymbols)
        assertContains(symbols.removedSymbols, "oldName")
        assertContains(symbols.addedSymbols, "newName")
    }
}
