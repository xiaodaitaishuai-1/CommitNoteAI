package com.commitnoteai.ai

import com.google.gson.Gson
import com.google.gson.JsonSyntaxException
import java.net.URI
import java.net.http.HttpClient
import java.net.http.HttpRequest
import java.net.http.HttpResponse
import java.time.Duration

class ModelListClient(
    private val httpClient: HttpClient = HttpClient.newBuilder()
        .connectTimeout(Duration.ofSeconds(15))
        .build(),
    private val gson: Gson = Gson(),
) {
    fun fetchModels(apiBaseUrl: String, apiKey: String): List<String> {
        val request = HttpRequest.newBuilder()
            .uri(URI.create(modelsUrl(apiBaseUrl)))
            .timeout(Duration.ofSeconds(30))
            .header("Authorization", "Bearer $apiKey")
            .GET()
            .build()
        val response = httpClient.send(request, HttpResponse.BodyHandlers.ofString())
        if (response.statusCode() !in 200..299) {
            throw IllegalStateException("获取模型列表失败 (${response.statusCode()}): ${response.body()}")
        }
        return parseModels(response.body(), gson)
    }

    companion object {
        fun parseModels(body: String): List<String> = parseModels(body, Gson())

        private fun parseModels(body: String, gson: Gson): List<String> {
            val parsed = try {
                gson.fromJson(body, Map::class.java)
            } catch (_: JsonSyntaxException) {
                throw IllegalStateException("模型列表响应不是有效 JSON")
            }
            val data = parsed["data"] as? List<*> ?: throw IllegalStateException("模型列表响应缺少 data")
            val models = data.mapNotNull { item ->
                (item as? Map<*, *>)?.get("id") as? String
            }.map { it.trim() }
                .filter { it.isNotBlank() }
                .distinct()
                .sortedWith(compareBy<String> { !it.contains("gpt", ignoreCase = true) }.thenBy { it.lowercase() })

            if (models.isEmpty()) {
                throw IllegalStateException("模型列表为空")
            }
            return models
        }

        private fun modelsUrl(baseUrl: String): String {
            val normalized = baseUrl.trim().trimEnd('/')
            return if (normalized.endsWith("/models")) normalized else "$normalized/models"
        }
    }
}
