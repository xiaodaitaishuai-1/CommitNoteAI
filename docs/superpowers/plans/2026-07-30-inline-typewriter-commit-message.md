# Inline Typewriter Commit Message Implementation Plan

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:subagent-driven-development (recommended) or superpowers:executing-plans to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** Replace generated-message preview dialogs with inline typewriter insertion in the existing commit-message editor.

**Architecture:** A small Swing-timer helper owns the animation and calls each existing UI's text setter. Both `GenerateCommitMessageAction` and the legacy `CommitNotePanel` retain background generation, but start this helper on success and remain disabled until its completion callback.

**Tech Stack:** Kotlin 2.3, IntelliJ Platform, Swing `Timer`, Kotlin Test/JUnit 5, Gradle.

---

## File structure

| File | Responsibility |
| --- | --- |
| `src/main/kotlin/com/commitnoteai/vcs/TypewriterText.kt` | Pure bounded state progression. |
| `src/main/kotlin/com/commitnoteai/vcs/CommitMessageTypewriter.kt` | UI-thread timer that applies text frames. |
| `src/main/kotlin/com/commitnoteai/vcs/GenerateCommitMessageAction.kt` | Current commit workflow inline insertion. |
| `src/main/kotlin/com/commitnoteai/vcs/CommitNoteCheckinHandler.kt` | Legacy check-in panel inline insertion. |
| `src/test/kotlin/com/commitnoteai/vcs/TypewriterTextTest.kt` | State-progression bounds coverage. |
| `src/test/kotlin/com/commitnoteai/vcs/GenerateCommitMessageActionSourceTest.kt` | Current flow no-dialog wiring guard. |
| `src/test/kotlin/com/commitnoteai/vcs/CommitNoteCheckinHandlerSourceTest.kt` | Legacy flow no-dialog wiring guard. |

Leave `CommitMessagePreviewDialog.kt`, `CommitMessagePreviewState.kt`, and their existing test untouched: they are inactive and deletion is outside this behavior change.

### Task 1: Cover bounded typewriter state

**Files:**
- Modify: `src/test/kotlin/com/commitnoteai/vcs/TypewriterTextTest.kt`
- Modify: `src/main/kotlin/com/commitnoteai/vcs/TypewriterText.kt` only if a test fails

- [ ] **Step 1: Add bounds tests**

```kotlin
@Test
fun `step treats zero and negative chunk sizes as one character`() {
    val initial = TypewriterText.State(target = "abc", visibleLength = 0)
    assertEquals("a", TypewriterText.step(initial, 0).visibleText)
    assertEquals("a", TypewriterText.step(initial, -3).visibleText)
}

@Test
fun `step never advances beyond target length`() {
    val result = TypewriterText.step(TypewriterText.State("abc", 2), 99)
    assertEquals(3, result.visibleLength)
    assertEquals("abc", result.visibleText)
    assertTrue(result.isComplete)
}
```

- [ ] **Step 2: Run the test as characterization coverage**

Run: `./gradlew.bat test --tests "com.commitnoteai.vcs.TypewriterTextTest"`

Expected: `BUILD SUCCESSFUL`; current `coerceAtLeast(1)` and `coerceAtMost(target.length)` already satisfy these requirements.

- [ ] **Step 3: Retain the minimal bounded implementation**

```kotlin
fun step(state: State, chunkSize: Int): State {
    val nextLength = (state.visibleLength + chunkSize.coerceAtLeast(1)).coerceAtMost(state.target.length)
    return state.copy(visibleLength = nextLength)
}
```

- [ ] **Step 4: Re-run focused tests and commit**

Run: `./gradlew.bat test --tests "com.commitnoteai.vcs.TypewriterTextTest"`

Expected: `BUILD SUCCESSFUL` with three passing tests.

```bash
git add src/test/kotlin/com/commitnoteai/vcs/TypewriterTextTest.kt
git commit -m "test(vcs): cover typewriter text bounds"
```

### Task 2: Add reusable Swing animation and wire the current action

**Files:**
- Create: `src/main/kotlin/com/commitnoteai/vcs/CommitMessageTypewriter.kt`
- Modify: `src/main/kotlin/com/commitnoteai/vcs/GenerateCommitMessageAction.kt`
- Modify: `src/test/kotlin/com/commitnoteai/vcs/GenerateCommitMessageActionSourceTest.kt`

- [ ] **Step 1: Replace the retired dialog test with a failing inline-animation assertion**

```kotlin
@Test
fun `action fills existing editor through typewriter without a preview dialog`() {
    val source = Files.readString(Path.of("src/main/kotlin/com/commitnoteai/vcs/GenerateCommitMessageAction.kt"))
    assertFalse(source.contains("CommitMessagePreviewDialog"))
    assertFalse(source.contains("dialog.showAndGet()"))
    assertContains(source, "CommitMessageTypewriter.start")
    assertContains(source, "commitMessageUi.text = text")
    assertContains(source, "generating.set(false)")
}
```

Remove the old `action previews generated message instead of directly replacing draft` test.

- [ ] **Step 2: Run the action source test to verify it fails**

Run: `./gradlew.bat test --tests "com.commitnoteai.vcs.GenerateCommitMessageActionSourceTest"`

Expected: FAIL because the action currently creates `CommitMessagePreviewDialog`.

- [ ] **Step 3: Add the shared timer helper**

Create `src/main/kotlin/com/commitnoteai/vcs/CommitMessageTypewriter.kt`:

```kotlin
package com.commitnoteai.vcs

import javax.swing.Timer

object CommitMessageTypewriter {
    private const val delayMs = 24
    private const val chunkSize = 2

    fun start(target: String, updateText: (String) -> Unit, onCompleted: () -> Unit) {
        var state = TypewriterText.State(target, 0)
        updateText(state.visibleText)
        if (state.isComplete) {
            onCompleted()
            return
        }
        val timer = Timer(delayMs, null)
        timer.addActionListener {
            state = TypewriterText.step(state, chunkSize)
            updateText(state.visibleText)
            if (state.isComplete) {
                timer.stop()
                onCompleted()
            }
        }
        timer.start()
    }
}
```

- [ ] **Step 4: Replace action dialog handling with inline animation**

Replace the success block in `GenerateCommitMessageAction` with:

```kotlin
onUiThread {
    CommitMessageTypewriter.start(
        target = formatted,
        updateText = { text -> commitMessageUi.text = text },
        onCompleted = { generating.set(false) },
    )
}
```

Rename `finish` to `onUiThread`; it must only execute `ApplicationManager.getApplication().invokeLater(onUiThread)`. In the error callback call `generating.set(false)` before `Messages.showErrorDialog`, preserving the draft on failure.

- [ ] **Step 5: Verify focused tests and commit**

Run: `./gradlew.bat test --tests "com.commitnoteai.vcs.TypewriterTextTest" --tests "com.commitnoteai.vcs.GenerateCommitMessageActionSourceTest"`

Expected: `BUILD SUCCESSFUL`.

```bash
git add src/main/kotlin/com/commitnoteai/vcs/CommitMessageTypewriter.kt src/main/kotlin/com/commitnoteai/vcs/GenerateCommitMessageAction.kt src/test/kotlin/com/commitnoteai/vcs/GenerateCommitMessageActionSourceTest.kt
git commit -m "feat(vcs): type generated message into commit editor"
```

### Task 3: Migrate the legacy check-in panel

**Files:**
- Modify: `src/main/kotlin/com/commitnoteai/vcs/CommitNoteCheckinHandler.kt`
- Create: `src/test/kotlin/com/commitnoteai/vcs/CommitNoteCheckinHandlerSourceTest.kt`

- [ ] **Step 1: Add a failing source-level test**

```kotlin
package com.commitnoteai.vcs

import java.nio.file.Files
import java.nio.file.Path
import kotlin.test.Test
import kotlin.test.assertContains
import kotlin.test.assertFalse

class CommitNoteCheckinHandlerSourceTest {
    @Test
    fun `legacy panel fills existing editor through typewriter without preview dialog`() {
        val source = Files.readString(Path.of("src/main/kotlin/com/commitnoteai/vcs/CommitNoteCheckinHandler.kt"))
        assertFalse(source.contains("CommitMessagePreviewDialog"))
        assertFalse(source.contains("showAndGet()"))
        assertContains(source, "CommitMessageTypewriter.start")
        assertContains(source, "checkinPanel.setCommitMessage(text)")
        assertContains(source, "generateButton.isEnabled = true")
    }
}
```

- [ ] **Step 2: Run it to verify it fails**

Run: `./gradlew.bat test --tests "com.commitnoteai.vcs.CommitNoteCheckinHandlerSourceTest"`

Expected: FAIL because the legacy panel still constructs `CommitMessagePreviewDialog`.

- [ ] **Step 3: Replace the legacy dialog block**

Replace the successful `invokeLater` block in `CommitNoteCheckinHandler.kt` with:

```kotlin
com.intellij.openapi.application.ApplicationManager.getApplication().invokeLater {
    CommitMessageTypewriter.start(
        target = formatted,
        updateText = { text -> checkinPanel.setCommitMessage(text) },
        onCompleted = {
            statusLabel.text = "已填充提交记录"
            generateButton.isEnabled = true
        },
    )
}
```

Keep the catch block unchanged: it must retain the draft, set the error status, re-enable the button, and show the existing error dialog.

- [ ] **Step 4: Run VCS tests and commit**

Run: `./gradlew.bat test --tests "com.commitnoteai.vcs.TypewriterTextTest" --tests "com.commitnoteai.vcs.GenerateCommitMessageActionSourceTest" --tests "com.commitnoteai.vcs.CommitNoteCheckinHandlerSourceTest" --tests "com.commitnoteai.vcs.CommitMessagePreviewStateTest"`

Expected: `BUILD SUCCESSFUL`.

```bash
git add src/main/kotlin/com/commitnoteai/vcs/CommitNoteCheckinHandler.kt src/test/kotlin/com/commitnoteai/vcs/CommitNoteCheckinHandlerSourceTest.kt
git commit -m "feat(vcs): animate legacy commit message insertion"
```

### Task 4: Verify the plugin and real UI behavior

**Files:**
- Verify only; no source changes expected.

- [ ] **Step 1: Run all automated tests**

Run: `./gradlew.bat test`

Expected: `BUILD SUCCESSFUL` with no failing test classes.

- [ ] **Step 2: Build the plugin archive**

Run: `./gradlew.bat buildPlugin`

Expected: `BUILD SUCCESSFUL` and `build/distributions/CommitNoteAI-0.1.9.zip` exists.

- [ ] **Step 3: Test the current commit workflow in a local IntelliJ-based IDE**

1. Enter a non-empty draft and select a text-file change.
2. Invoke CommitNoteAI, then confirm no dialog appears and the generated text clears and fills that same editor progressively.
3. Confirm the action stays disabled until the final frame.
4. Cause a generation failure and confirm the original draft remains while the error dialog is displayed.

- [ ] **Step 4: Test the legacy check-in panel where available**

1. Enter a non-empty draft and select a text-file change.
2. Click its generate button and confirm no dialog appears, the same field fills progressively, and the button re-enables after the final frame.
3. Cause a generation failure and confirm the existing draft remains, an error dialog appears, and the button re-enables.

- [ ] **Step 5: Confirm no unintended changes remain**

Run: `git status --short`

Expected: no output. If acceptance testing required an intentional correction, commit only its affected files with a message describing that correction; never create an empty commit.
