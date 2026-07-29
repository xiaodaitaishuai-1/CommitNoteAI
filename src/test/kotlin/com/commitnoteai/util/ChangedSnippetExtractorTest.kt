package com.commitnoteai.util

import kotlin.test.Test
import kotlin.test.assertContains
import kotlin.test.assertFalse

class ChangedSnippetExtractorTest {
    @Test
    fun `extract focuses on changed notification permission lines`() {
        val before = """
            fun keepUnrelatedOne() = Unit
            fun keepUnrelatedTwo() = Unit
            private fun showHomeNotificationRationaleIfNeeded() {
                pendingShowWelcomeAfterNotificationFlow = true
                showNotificationPermissionRationaleDialog(
                    launcher = notificationPermissionLauncher,
                    onEnable = {
                        notificationPermissionRequestInFlight = true
                    },
                    onCancel = {
                        showWelcomeDialogIfNeeded()
                    }
                )
            }
            private fun showWelcomeDialogIfNeeded() = Unit
        """.trimIndent()
        val after = """
            fun keepUnrelatedOne() = Unit
            fun keepUnrelatedTwo() = Unit
            private fun showHomeNotificationRationaleIfNeeded() {
                pendingShowWelcomeAfterNotificationFlow = true
                notificationPermissionRequestInFlight = true
                requestNotificationPermissionDirectly(notificationPermissionLauncher)
            }
            private fun showWelcomeDialogIfNeeded() = Unit
        """.trimIndent()

        val snippets = ChangedSnippetExtractor.extract(before, after)

        assertContains(snippets.before.orEmpty(), "showNotificationPermissionRationaleDialog")
        assertContains(snippets.after.orEmpty(), "requestNotificationPermissionDirectly")
        assertContains(snippets.after.orEmpty(), "notificationPermissionRequestInFlight")
        assertFalse(snippets.before.orEmpty().contains("keepUnrelatedOne"))
        assertFalse(snippets.after.orEmpty().contains("keepUnrelatedTwo"))
    }
}
