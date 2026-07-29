# Qoder 风格提交 Diff 采集实施计划

> **供智能编码代理执行：** 使用 `subagent-driven-development`（推荐）或 `executing-plans` 按任务执行。每个复选框是一个独立的提交前检查点。

**目标：** 将 CommitNoteAI 的提交上下文从前后文件片段改为 Qoder 同款、受容量约束的 unified diff，并将已选未跟踪文件纳入生成依据。

**架构：** 平台层从 Commit Workflow 获取版本化和未跟踪变更，通过 IntelliJ patch API 逐文件写出 unified diff。纯 Kotlin 层解析 diff 的文件头、hunk 与增删行，以此生成符号、事实、提示词和生成后事实校验的共同证据。

**技术栈：** Kotlin 2.3、IntelliJ Platform 251+、`IdeaTextPatchBuilder`、`UnifiedDiffWriter`、JUnit 5/Kotlin Test。

---

## 文件变更表

| 文件 | 职责 |
| --- | --- |
| `src/main/kotlin/com/commitnoteai/model/CommitGenerationModels.kt` | 定义 diff 快照、跳过项与采集结果。 |
| `src/main/kotlin/com/commitnoteai/util/CommitChangeCollector.kt` | Qoder 对齐的收集、过滤、patch 输出与限额。 |
| `src/main/kotlin/com/commitnoteai/util/UnifiedDiffEvidence.kt` | 新增的纯 Kotlin unified diff 解析器。 |
| `src/main/kotlin/com/commitnoteai/util/CommitChangeAnalyzer.kt` | 基于 `diffText` 推断模块与提示。 |
| `src/main/kotlin/com/commitnoteai/util/ChangedSymbolExtractor.kt` | 从 `+`、`-` 行提取符号。 |
| `src/main/kotlin/com/commitnoteai/util/CommitChangeFactExtractor.kt` | 用 diff 恢复的前后文本提取 XML/Kotlin 事实。 |
| `src/main/kotlin/com/commitnoteai/util/CommitMessageFactChecker.kt` | 只从 diff 证据过滤生成结果。 |
| `src/main/kotlin/com/commitnoteai/util/CommitPromptBuilder.kt` | 输出 unified diff、跳过项与截断状态。 |
| `src/main/kotlin/com/commitnoteai/ai/CommitNoteGenerator.kt` | 在 HTTP 请求前判断无可用 diff。 |
| `src/main/kotlin/com/commitnoteai/vcs/GenerateCommitMessageAction.kt` | 传递 `includedUnversionedFiles`，支持仅选未跟踪文件。 |
| `src/main/kotlin/com/commitnoteai/vcs/CommitNoteCheckinHandler.kt` | 保持旧提交面板兼容，未跟踪文件参数默认为空。 |

### Task 1: 建立 Diff 数据模型

**Files:**
- Modify: `src/main/kotlin/com/commitnoteai/model/CommitGenerationModels.kt`
- Create: `src/test/kotlin/com/commitnoteai/model/CommitGenerationModelsTest.kt`

- [ ] **Step 1: 写失败的模型测试。**

```kotlin
@Test
fun `collection exposes usable diffs and truncation metadata`() {
    val collection = CommitChangeCollection(
        changes = listOf(CommitChangeSnapshot("src/Login.kt", "modified", "@@ -1 +1 @@\n-old\n+new")),
        skippedChanges = listOf(CommitChangeSkip("assets/logo.png", "binary")),
        isTruncated = true,
    )

    assertEquals(1, collection.changes.size)
    assertEquals("binary", collection.skippedChanges.single().reason)
    assertTrue(collection.isTruncated)
}
```

- [ ] **Step 2: 验证测试失败。**

Run: `./gradlew.bat test --tests "com.commitnoteai.model.CommitGenerationModelsTest" -PlocalIdePath="E:\go\GoLand 2026.1"`

Expected: FAIL，`CommitChangeCollection` 与 `CommitChangeSkip` 未定义。

- [ ] **Step 3: 替换片段模型。**

```kotlin
data class CommitChangeSnapshot(val path: String, val changeType: String, val diffText: String)
data class CommitChangeSkip(val path: String, val reason: String)
data class CommitChangeCollection(
    val changes: List<CommitChangeSnapshot>,
    val skippedChanges: List<CommitChangeSkip> = emptyList(),
    val isTruncated: Boolean = false,
)
data class CommitPromptPayload(
    val currentDraft: String,
    val changeCollection: CommitChangeCollection,
    val outputStyle: String = "tongyi",
    val customInstructions: String = "",
    val projectContext: String = "",
)
```

删除 `beforeSnippet`、`afterSnippet`、`originText`，使所有旧消费者在本次迁移中显式报错。

- [ ] **Step 4: 验证测试通过并提交。**

Run: `./gradlew.bat test --tests "com.commitnoteai.model.CommitGenerationModelsTest" -PlocalIdePath="E:\go\GoLand 2026.1"`

Expected: PASS。

```bash
git add src/main/kotlin/com/commitnoteai/model/CommitGenerationModels.kt src/test/kotlin/com/commitnoteai/model/CommitGenerationModelsTest.kt
git commit -m "refactor(model): represent commit context as unified diffs"
```

### Task 2: 实现可测试的 unified diff 解析器

**Files:**
- Create: `src/main/kotlin/com/commitnoteai/util/UnifiedDiffEvidence.kt`
- Create: `src/test/kotlin/com/commitnoteai/util/UnifiedDiffEvidenceTest.kt`

- [ ] **Step 1: 写多 hunk 失败测试。**

```kotlin
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
    assertContains(evidence.afterText, "newTitle")
    assertEquals(listOf("fun newLogin() = true", "val newTitle = \"new\""), evidence.addedLines)
}
```

- [ ] **Step 2: 验证失败。**

Run: `./gradlew.bat test --tests "com.commitnoteai.util.UnifiedDiffEvidenceTest" -PlocalIdePath="E:\go\GoLand 2026.1"`

Expected: FAIL，`UnifiedDiffEvidence` 未定义。

- [ ] **Step 3: 实现解析器。**

```kotlin
data class UnifiedDiffEvidence(
    val path: String,
    val addedLines: List<String>,
    val removedLines: List<String>,
    val contextLines: List<String>,
) {
    val beforeText: String get() = (contextLines + removedLines).joinToString("\n")
    val afterText: String get() = (contextLines + addedLines).joinToString("\n")

    companion object { fun parse(diffText: String): UnifiedDiffEvidence }
}
```

只读取 hunk 中的 `+`、`-`、空格前缀行，跳过 `+++`、`---` 文件头和 hunk 标题。空 patch 文件说明必须返回空行集合而非抛异常。追加新增文件与空 patch 的测试。

- [ ] **Step 4: 验证并提交。**

Run: `./gradlew.bat test --tests "com.commitnoteai.util.UnifiedDiffEvidenceTest" -PlocalIdePath="E:\go\GoLand 2026.1"`

Expected: PASS。

```bash
git add src/main/kotlin/com/commitnoteai/util/UnifiedDiffEvidence.kt src/test/kotlin/com/commitnoteai/util/UnifiedDiffEvidenceTest.kt
git commit -m "feat(util): parse unified diff evidence"
```

### Task 3: 将分析、符号、事实和校验迁移到 Diff

**Files:**
- Modify: `src/main/kotlin/com/commitnoteai/util/CommitChangeAnalyzer.kt`
- Modify: `src/main/kotlin/com/commitnoteai/util/ChangedSymbolExtractor.kt`
- Modify: `src/main/kotlin/com/commitnoteai/util/CommitChangeFactExtractor.kt`
- Modify: `src/main/kotlin/com/commitnoteai/util/CommitMessageFactChecker.kt`
- Modify: `src/test/kotlin/com/commitnoteai/util/CommitChangeAnalyzerTest.kt`
- Modify: `src/test/kotlin/com/commitnoteai/util/ChangedSymbolExtractorTest.kt`
- Modify: `src/test/kotlin/com/commitnoteai/util/CommitChangeFactExtractorTest.kt`
- Modify: `src/test/kotlin/com/commitnoteai/util/CommitMessageFactCheckerTest.kt`

- [ ] **Step 1: 将既有测试构造改为 diff。**

```kotlin
private fun change(path: String, type: String, diff: String) = CommitChangeSnapshot(path, type, diff)

assertContains(symbols.addedSymbols, "newName")
assertContains(symbols.removedSymbols, "oldName")
```

XML 控件替换测试使用 `-<Button ...>` 与 `+<AppCompatImageButton ...>`；Kotlin 事实测试使用 `-Log.d(...)` 与 `+textSectionSubtitle.visibility = View.GONE`。

- [ ] **Step 2: 验证迁移前测试失败。**

Run: `./gradlew.bat test --tests "com.commitnoteai.util.CommitChangeAnalyzerTest" --tests "com.commitnoteai.util.ChangedSymbolExtractorTest" --tests "com.commitnoteai.util.CommitChangeFactExtractorTest" --tests "com.commitnoteai.util.CommitMessageFactCheckerTest" -PlocalIdePath="E:\go\GoLand 2026.1"`

Expected: FAIL，旧字段不再存在。

- [ ] **Step 3: 实现单一证据来源。**

```kotlin
val evidence = UnifiedDiffEvidence.parse(change.diffText)
addedSymbols += extractFromText(evidence.addedLines.joinToString("\n"))
removedSymbols += extractFromText(evidence.removedLines.joinToString("\n"))
```

`CommitChangeFactExtractor` 将解析后的 `beforeText` 和 `afterText` 传给现有比较逻辑。`CommitChangeAnalyzer.priorityHints` 与 `CommitMessageFactChecker.Evidence.from` 使用 `change.diffText`；不得保留任何片段字段引用。

- [ ] **Step 4: 验证并提交。**

Run: `./gradlew.bat test --tests "com.commitnoteai.util.CommitChangeAnalyzerTest" --tests "com.commitnoteai.util.ChangedSymbolExtractorTest" --tests "com.commitnoteai.util.CommitChangeFactExtractorTest" --tests "com.commitnoteai.util.CommitMessageFactCheckerTest" -PlocalIdePath="E:\go\GoLand 2026.1"`

Expected: PASS。

```bash
git add src/main/kotlin/com/commitnoteai/util src/test/kotlin/com/commitnoteai/util
git commit -m "refactor(util): derive commit evidence from unified diffs"
```

### Task 4: 实现 Qoder 对齐的 IntelliJ 采集器

**Files:**
- Modify: `src/main/kotlin/com/commitnoteai/util/CommitChangeCollector.kt`
- Create: `src/test/kotlin/com/commitnoteai/util/CommitChangeCollectorTest.kt`

- [ ] **Step 1: 为限额、过滤和 fallback 写失败测试。**

```kotlin
@Test
fun `accumulator stops at Qoder entry and character limits`() {
    val result = CommitDiffAccumulator(maxEntries = 2, maxCharacters = 12)
        .add("first", "123456").add("second", "123456").add("third", "ignored").result()
    assertEquals(listOf("first", "second"), result.changes.map { it.path })
    assertTrue(result.isTruncated)
}
```

再覆盖：二进制标记 `binary`、301 字符单行标记 `single-line-too-large`、空 patch 使用路径说明、新文件 fallback 以 `--- /dev/null` 和 `+++ b/<path>` 开头。

- [ ] **Step 2: 验证失败。**

Run: `./gradlew.bat test --tests "com.commitnoteai.util.CommitChangeCollectorTest" -PlocalIdePath="E:\go\GoLand 2026.1"`

Expected: FAIL，`CommitDiffAccumulator` 或 Qoder 限额逻辑不存在。

- [ ] **Step 3: 实现平台收集和纯限额器。**

```kotlin
internal class CommitDiffAccumulator(
    private val maxEntries: Int = 50,
    private val maxCharacters: Int = 70_000,
) {
    fun add(path: String, diffText: String): CommitDiffAccumulator
    fun skip(path: String, reason: String): CommitDiffAccumulator
    fun result(): CommitChangeCollection
}

fun collect(project: Project, changes: List<Change>, unversionedFiles: List<FilePath> = emptyList()): CommitChangeCollection
```

将每个 `FilePath` 转成 `Change(null, CurrentContentRevision(filePath))`。跳过二进制与超过 300 字符的单行文本。对每项调用：

```kotlin
val patches = IdeaTextPatchBuilder.buildPatch(project, listOf(change), Path.of(project.basePath!!), false, false)
UnifiedDiffWriter.write(project, project.stateStore.projectBasePath, patches, writer, "\n", null, emptyList())
```

逐文件加入，最多 50 条、70,000 字符；超限停止且设置 `isTruncated`。空 patch 加路径说明。新文件构建 patch 抛出 `VcsException` 时，读取最多 70,000 字符并写出 `/dev/null`、`b/path`、hunk 头及 `+` 行的 fallback。单项异常必须记录跳过项后继续。

- [ ] **Step 4: 验证并提交。**

Run: `./gradlew.bat test --tests "com.commitnoteai.util.CommitChangeCollectorTest" --tests "com.commitnoteai.util.UnifiedDiffEvidenceTest" -PlocalIdePath="E:\go\GoLand 2026.1"`

Expected: PASS。

```bash
git add src/main/kotlin/com/commitnoteai/util/CommitChangeCollector.kt src/test/kotlin/com/commitnoteai/util/CommitChangeCollectorTest.kt
git commit -m "feat(vcs): collect Qoder-style unified commit diffs"
```

### Task 5: 接入提示词、生成器和提交窗口

**Files:**
- Modify: `src/main/kotlin/com/commitnoteai/util/CommitPromptBuilder.kt`
- Modify: `src/main/kotlin/com/commitnoteai/ai/CommitNoteGenerator.kt`
- Modify: `src/main/kotlin/com/commitnoteai/vcs/GenerateCommitMessageAction.kt`
- Modify: `src/main/kotlin/com/commitnoteai/vcs/CommitNoteCheckinHandler.kt`
- Modify: `src/test/kotlin/com/commitnoteai/util/CommitPromptBuilderTest.kt`
- Modify: `src/test/kotlin/com/commitnoteai/ai/CommitMessageParsingTest.kt`
- Modify: `src/test/kotlin/com/commitnoteai/vcs/GenerateCommitMessageActionSourceTest.kt`

- [ ] **Step 1: 写 Prompt 和 Action 失败测试。**

```kotlin
@Test
fun `build renders unified diff and reports truncation`() {
    val prompt = CommitPromptBuilder.build(
        CommitPromptPayload("", CommitChangeCollection(
            changes = listOf(CommitChangeSnapshot("src/Login.kt", "modified", "@@ -1 +1 @@\n-old\n+new")),
            skippedChanges = listOf(CommitChangeSkip("logo.png", "binary")), isTruncated = true,
        )),
    )
    assertContains(prompt, "统一 diff")
    assertContains(prompt, "+new")
    assertContains(prompt, "上下文已截断")
    assertContains(prompt, "logo.png")
}
```

动作源码测试必须断言 `workflowUi.getIncludedUnversionedFiles()` 存在，并且“可用”条件是版本化或未跟踪选择项至少一个非空。

- [ ] **Step 2: 验证失败。**

Run: `./gradlew.bat test --tests "com.commitnoteai.util.CommitPromptBuilderTest" --tests "com.commitnoteai.ai.CommitMessageParsingTest" --tests "com.commitnoteai.vcs.GenerateCommitMessageActionSourceTest" -PlocalIdePath="E:\go\GoLand 2026.1"`

Expected: FAIL，collection 未渲染或未跟踪文件未传递。

- [ ] **Step 3: 接入 collection。**

```kotlin
val collected = CommitChangeCollector.collect(project, changes, unversionedFiles)
if (collected.changes.isEmpty()) {
    throw IllegalStateException("没有可用于生成提交记录的文本变更")
}
```

生成器将 `collected` 写入 `CommitPromptPayload`，事实校验使用 `collected.changes`。提示词逐条输出 `change.diffText`，并说明截断和跳过项。提交动作从 `CommitWorkflowUi` 读取已选变更与 `getIncludedUnversionedFiles()`；两者都为空才禁用。旧 `CheckinProjectPanel` 依赖默认空未跟踪列表。

- [ ] **Step 4: 验证并提交。**

Run: `./gradlew.bat test --tests "com.commitnoteai.util.CommitPromptBuilderTest" --tests "com.commitnoteai.ai.CommitMessageParsingTest" --tests "com.commitnoteai.vcs.GenerateCommitMessageActionSourceTest" -PlocalIdePath="E:\go\GoLand 2026.1"`

Expected: PASS。

```bash
git add src/main/kotlin/com/commitnoteai/util/CommitPromptBuilder.kt src/main/kotlin/com/commitnoteai/ai/CommitNoteGenerator.kt src/main/kotlin/com/commitnoteai/vcs src/test/kotlin/com/commitnoteai/util/CommitPromptBuilderTest.kt src/test/kotlin/com/commitnoteai/ai/CommitMessageParsingTest.kt src/test/kotlin/com/commitnoteai/vcs/GenerateCommitMessageActionSourceTest.kt
git commit -m "feat(ai): generate commit messages from unified diff evidence"
```

### Task 6: 全量验证与插件包验收

**Files:**
- Modify: `README.md`，仅在需要说明“已选未跟踪文本文件”和 diff 限额时修改。

- [ ] **Step 1: 运行完整测试。**

Run: `./gradlew.bat test -PlocalIdePath="E:\go\GoLand 2026.1"`

Expected: BUILD SUCCESSFUL。

- [ ] **Step 2: 构建插件包。**

Run: `./gradlew.bat buildPlugin -PlocalIdePath="E:\go\GoLand 2026.1"`

Expected: BUILD SUCCESSFUL，生成 `build/distributions/CommitNoteAI-<version>.zip`。

- [ ] **Step 3: 在本地 IDE 做真实提交窗口验收。**

1. 选择已修改文本和未跟踪文本，确认生成内容只引用 unified diff 中的路径与增删行。
2. 加入二进制或压缩单行文件，确认它们被跳过且不会使生成失败。
3. 选择超过 50 个文件或超过 70,000 字符差异，确认生成仍可用且 prompt 明确说明截断。
4. 只选择二进制文件，确认不发生模型请求并显示“没有可用于生成提交记录的文本变更”。

- [ ] **Step 4: 仅在 README 有实质变更时提交文档。**

```bash
git add README.md
git commit -m "docs: describe unified commit diff collection"
```

README 不需修改时不创建空提交；保留前五个功能提交和两条 Gradle 成功输出作为验证证据。
