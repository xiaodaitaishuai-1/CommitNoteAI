package com.commitnoteai.ai

import com.commitnoteai.model.CommitMessageWirePayload
import com.commitnoteai.model.CommitChangeSnapshot
import com.commitnoteai.model.CommitPromptPayload
import com.commitnoteai.model.GeneratedCommitMessage
import com.commitnoteai.platform.PasswordSafeBridge
import com.commitnoteai.settings.CommitNoteSettings
import com.commitnoteai.util.CommitChangeCollector
import com.commitnoteai.util.CommitMessageFactChecker
import com.commitnoteai.util.CommitMessageSanitizer
import com.commitnoteai.util.CommitPromptBuilder
import com.commitnoteai.util.ProjectContextLoader
import com.google.gson.Gson
import com.google.gson.JsonSyntaxException
import com.intellij.openapi.application.ApplicationManager
import com.intellij.credentialStore.CredentialAttributes
import com.intellij.openapi.project.Project
import com.intellij.openapi.vcs.changes.Change
import java.net.URI
import java.net.http.HttpClient
import java.net.http.HttpRequest
import java.net.http.HttpResponse
import java.time.Duration

class CommitNoteGenerator(
    private val httpClient: HttpClient = HttpClient.newBuilder()
        .connectTimeout(Duration.ofSeconds(15))
        .build(),
    private val gson: Gson = Gson(),
) {
    fun generate(project: Project?, currentDraft: String, changes: List<Change>): GeneratedCommitMessage {
        val settings = ApplicationManager.getApplication().getService(CommitNoteSettings::class.java)
        val apiKey = PasswordSafeBridge.getPassword(credentialAttributes())
            ?: throw IllegalStateException("请先在 CommitNoteAI 设置页保存 API Key")

        val collectedChanges = CommitChangeCollector.collect(project, changes)
        val projectContext = ProjectContextLoader.load(project)
        val promptPayload = CommitPromptPayload(
            currentDraft = currentDraft,
            changes = collectedChanges,
            outputStyle = settings.outputStyle,
            customInstructions = settings.customInstructions,
            projectContext = projectContext,
        )
        val request = createRequest(settings, apiKey, promptPayload)
        val response = httpClient.send(request, HttpResponse.BodyHandlers.ofString())

        if (response.statusCode() !in 200..299) {
            throw IllegalStateException("接口请求失败 (${response.statusCode()}): ${response.body()}")
        }

        return parseResponse(response.body(), collectedChanges, projectContext)
    }

    fun generateResponseForTest(body: String): GeneratedCommitMessage = parseResponse(body)

    fun generateResponseForTest(
        body: String,
        changes: List<CommitChangeSnapshot>,
        projectContext: String = "",
    ): GeneratedCommitMessage = parseResponse(body, changes, projectContext)

    private fun createRequest(
        settings: CommitNoteSettings,
        apiKey: String,
        payload: CommitPromptPayload,
    ): HttpRequest {
        val body = createChatRequestBody(
            model = settings.model,
            temperature = settings.temperature,
            reasoningEffort = settings.reasoningEffort,
            userPrompt = CommitPromptBuilder.build(payload),
        )

        return HttpRequest.newBuilder()
            .uri(URI.create(chatCompletionsUrl(settings.apiBaseUrl)))
            .timeout(Duration.ofSeconds(60))
            .header("Content-Type", "application/json")
            .header("Authorization", "Bearer $apiKey")
            .POST(HttpRequest.BodyPublishers.ofString(body))
            .build()
    }

    private fun createChatRequestBody(
        model: String,
        temperature: Double,
        reasoningEffort: String,
        userPrompt: String,
    ): String {
        val body = linkedMapOf<String, Any>(
            "model" to model,
            "temperature" to temperature,
            "messages" to listOf(
                mapOf(
                    "role" to "system",
                    "content" to systemPrompt(),
                ),
                mapOf(
                    "role" to "user",
                    "content" to userPrompt,
                ),
            ),
        )
        val normalizedEffort = CommitNoteSettings.normalizeReasoningEffort(reasoningEffort)
        if (normalizedEffort.isNotBlank()) {
            body["reasoning"] = mapOf("effort" to normalizedEffort)
        }
        return gson.toJson(body)
    }

    private fun parseResponse(
        body: String,
        changes: List<CommitChangeSnapshot> = emptyList(),
        projectContext: String = "",
    ): GeneratedCommitMessage {
        val extracted = extractAssistantText(body)
        val sanitized = try {
            val payload = gson.fromJson(extracted, CommitMessageWirePayload::class.java)
            val title = payload.title?.trim().orEmpty()
            if (title.isBlank()) {
                throw JsonSyntaxException("missing title")
            }
            CommitMessageSanitizer.sanitize(GeneratedCommitMessage(
                title = title,
                bodyLines = payload.bodyLines.orEmpty().map { it.trim() }.filter { it.isNotBlank() },
            ))
        } catch (_: JsonSyntaxException) {
            val lines = extracted.lines().map { it.trim() }.filter { it.isNotBlank() }
            if (lines.isEmpty()) {
                throw IllegalStateException("模型返回内容为空")
            }
            CommitMessageSanitizer.sanitize(GeneratedCommitMessage(
                title = lines.first(),
                bodyLines = lines.drop(1),
            ))
        }
        return if (changes.isEmpty()) {
            sanitized
        } else {
            CommitMessageFactChecker.check(sanitized, changes, projectContext)
        }
    }

    private fun extractAssistantText(body: String): String {
        return try {
            val parsed = gson.fromJson(body, Map::class.java)
            val choices = parsed["choices"] as? List<*> ?: return body
            val firstChoice = choices.firstOrNull() as? Map<*, *> ?: return body
            val message = firstChoice["message"] as? Map<*, *>
            val content = message?.get("content") as? String
            if (!content.isNullOrBlank()) {
                return stripCodeFence(content)
            }
            val text = firstChoice["text"] as? String
            if (!text.isNullOrBlank()) {
                return stripCodeFence(text)
            }
            body
        } catch (_: JsonSyntaxException) {
            body
        }
    }

    private fun stripCodeFence(value: String): String {
        val trimmed = value.trim()
        if (trimmed.startsWith("```")) {
            val withoutOpen = trimmed.removePrefix("```json").removePrefix("```").trim()
            return withoutOpen.removeSuffix("```").trim()
        }
        return trimmed
    }

    private fun chatCompletionsUrl(baseUrl: String): String {
        val normalized = baseUrl.trim().trimEnd('/')
        return if (normalized.endsWith("/chat/completions")) {
            normalized
        } else {
            "$normalized/chat/completions"
        }
    }

    private fun systemPrompt(): String = """
        你是一个中文 Git 提交信息助手。
        你的任务是根据代码变更输出严格 JSON，格式为：
        {"title":"type(scope): 中文提交标题","bodyLines":["中文要点一","中文要点二"]}

        规则：
        - title 必须使用 Conventional Commit 风格：type(scope): 中文标题。
        - type 从 feat、fix、refactor、chore、docs、test、style 中选择。
        - scope 优先从路径、模块、类名或业务域推断；无法判断时可以省略 scope，例如 fix: 修复提交记录格式。
        - title 示例：refactor(ads): 移除 RewardManager 中的冗余方法。
        - bodyLines 返回不带 Markdown 前缀的中文要点，具体数量按用户选择的输出样式决定。
        - bodyLines 每行描述一个具体变更，不要以 "-"、"*" 或编号开头。
        - 不要输出 JSON 之外的任何内容。
    """.trimIndent()

    private fun credentialAttributes(): CredentialAttributes {
        return CredentialAttributes("CommitNoteAI", "api-key")
    }

    companion object {
        fun createChatRequestBodyForTest(
            model: String,
            temperature: Double,
            reasoningEffort: String,
            userPrompt: String,
        ): String = CommitNoteGenerator().createChatRequestBody(model, temperature, reasoningEffort, userPrompt)
    }
}

