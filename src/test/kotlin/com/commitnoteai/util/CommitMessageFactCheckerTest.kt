package com.commitnoteai.util

import com.commitnoteai.model.CommitChangeSnapshot
import com.commitnoteai.model.GeneratedCommitMessage
import kotlin.test.Test
import kotlin.test.assertEquals

class CommitMessageFactCheckerTest {
    @Test
    fun `check keeps lines that mention changed symbols`() {
        val checked = CommitMessageFactChecker.check(
            message = GeneratedCommitMessage(
                title = "refactor(home): 简化通知权限请求流程",
                bodyLines = listOf(
                    "移除不再使用的 showNotificationPermissionRationaleDialog 函数导入",
                    "直接调用 requestNotificationPermissionDirectly 替代对话框逻辑",
                ),
            ),
            changes = notificationChanges(),
            projectContext = "",
        )

        assertEquals(2, checked.bodyLines.size)
    }

    @Test
    fun `check filters lines without diff evidence`() {
        val checked = CommitMessageFactChecker.check(
            message = GeneratedCommitMessage(
                title = "refactor(home): 简化通知权限请求流程",
                bodyLines = listOf(
                    "调整 HomeActivity 的返回键处理逻辑",
                    "优化页面流程和状态更新",
                    "直接调用 requestNotificationPermissionDirectly 替代对话框逻辑",
                ),
            ),
            changes = notificationChanges(),
            projectContext = "",
        )

        assertEquals(listOf("直接调用 requestNotificationPermissionDirectly 替代对话框逻辑"), checked.bodyLines)
    }

    @Test
    fun `check keeps concrete xml control fact and filters vague ui line`() {
        val checked = CommitMessageFactChecker.check(
            message = GeneratedCommitMessage(
                title = "fix(app): 调整剧集页面导航控件",
                bodyLines = listOf(
                    "调整导航控件",
                    "将 buttonBack 从 Button 调整为 AppCompatImageButton",
                    "将 buttonBack 的 android:text 替换为 android:src",
                ),
            ),
            changes = listOf(
                CommitChangeSnapshot(
                    path = "app/src/main/res/layout/activity_drama_detail.xml",
                    changeType = "modified",
                    beforeSnippet = """
                        <Button
                            android:id="@+id/buttonBack"
                            android:text="@string/back" />
                    """.trimIndent(),
                    afterSnippet = """
                        <androidx.appcompat.widget.AppCompatImageButton
                            android:id="@+id/buttonBack"
                            android:src="@drawable/ic_back" />
                    """.trimIndent(),
                    originText = null,
                ),
            ),
            projectContext = "",
        )

        assertEquals(
            listOf(
                "将 buttonBack 从 Button 调整为 AppCompatImageButton",
                "将 buttonBack 的 android:text 替换为 android:src",
            ),
            checked.bodyLines,
        )
    }

    @Test
    fun `check keeps title when every body line is filtered`() {
        val checked = CommitMessageFactChecker.check(
            message = GeneratedCommitMessage(
                title = "refactor(home): 简化通知权限请求流程",
                bodyLines = listOf("调整 HomeActivity 的返回键处理逻辑"),
            ),
            changes = notificationChanges(),
            projectContext = "",
        )

        assertEquals("refactor(home): 简化通知权限请求流程", checked.title)
        assertEquals(emptyList(), checked.bodyLines)
    }

    @Test
    fun `check keeps lines that mention project context keywords`() {
        val checked = CommitMessageFactChecker.check(
            message = GeneratedCommitMessage(
                title = "refactor(home): 简化通知权限请求流程",
                bodyLines = listOf("调整首页通知权限请求入口"),
            ),
            changes = notificationChanges(),
            projectContext = "home: HomeActivity 或首页相关",
        )

        assertEquals(listOf("调整首页通知权限请求入口"), checked.bodyLines)
    }

    private fun notificationChanges(): List<CommitChangeSnapshot> {
        return listOf(
            CommitChangeSnapshot(
                path = "app/src/main/java/com/clarity/photo/activity/HomeActivity.kt",
                changeType = "modified",
                beforeSnippet = "showNotificationPermissionRationaleDialog(notificationPermissionLauncher)",
                afterSnippet = "notificationPermissionRequestInFlight = true\nrequestNotificationPermissionDirectly(notificationPermissionLauncher)",
                originText = "HomeActivity.kt",
            ),
        )
    }
}
