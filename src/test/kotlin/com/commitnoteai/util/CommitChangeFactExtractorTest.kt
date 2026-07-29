package com.commitnoteai.util

import com.commitnoteai.model.CommitChangeSnapshot
import kotlin.test.Test
import kotlin.test.assertContains

class CommitChangeFactExtractorTest {
    @Test
    fun `extract describes xml control and attribute replacements by id`() {
        val facts = CommitChangeFactExtractor.extract(
            listOf(
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
        )

        assertContains(facts, "将 buttonBack 从 Button 调整为 AppCompatImageButton")
        assertContains(facts, "将 buttonBack 的 android:text 替换为 android:src")
    }

    @Test
    fun `extract describes kotlin class field visibility and removed debug log`() {
        val facts = CommitChangeFactExtractor.extract(
            listOf(
                CommitChangeSnapshot(
                    path = "app/src/main/java/com/abandon/drama/ui/fragments/DramaHomeFragment.kt",
                    changeType = "modified",
                    beforeSnippet = """
                        class DramaHomeFragment : Fragment() {
                            Log.d("DramaHomeFragment", "subtitle")
                        }
                    """.trimIndent(),
                    afterSnippet = """
                        class DramaHomeFragment : Fragment() {
                            textSectionSubtitle.visibility = View.GONE
                        }
                    """.trimIndent(),
                    originText = null,
                ),
            ),
        )

        assertContains(facts, "在 DramaHomeFragment 中将 textSectionSubtitle.visibility 设为 View.GONE")
        assertContains(facts, "移除 DramaHomeFragment 中的 Log.d 调试输出")
    }

    @Test
    fun `extract describes added drawable and changed values resources`() {
        val facts = CommitChangeFactExtractor.extract(
            listOf(
                CommitChangeSnapshot(
                    path = "app/src/main/res/drawable/ic_back.xml",
                    changeType = "added",
                    beforeSnippet = null,
                    afterSnippet = "<vector android:width=\"24dp\" />",
                    originText = null,
                ),
                CommitChangeSnapshot(
                    path = "app/src/main/res/values/strings.xml",
                    changeType = "modified",
                    beforeSnippet = "<string name=\"old_name\">Old</string>",
                    afterSnippet = "<string name=\"new_name\">New</string>",
                    originText = null,
                ),
            ),
        )

        assertContains(facts, "新增 drawable 资源 ic_back.xml")
        assertContains(facts, "更新 values 资源 strings.xml")
    }
}
