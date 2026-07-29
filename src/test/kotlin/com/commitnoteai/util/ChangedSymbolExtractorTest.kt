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
                    diffText = """
                        @@ -1 +1,2 @@
                        -showNotificationPermissionRationaleDialog(notificationPermissionLauncher)
                        +notificationPermissionRequestInFlight = true
                        +requestNotificationPermissionDirectly(notificationPermissionLauncher)
                    """.trimIndent(),
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
                    diffText = """
                        @@ -1 +1 @@
                        -private fun oldName() = true
                        +private fun newName() = false
                    """.trimIndent(),
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
