package com.todoplus.services.ai

import com.intellij.openapi.application.ApplicationManager
import com.intellij.openapi.fileEditor.FileDocumentManager
import com.intellij.openapi.project.Project
import com.intellij.openapi.util.TextRange
import com.intellij.openapi.vfs.LocalFileSystem
import com.intellij.openapi.vfs.VirtualFileManager
import com.todoplus.models.TodoItem
import java.net.URI
import java.net.http.HttpClient
import java.net.http.HttpRequest
import java.net.http.HttpResponse
import java.time.Duration
import java.util.regex.Pattern

enum class AiProvider(val displayName: String) {
    GEMINI("Google Gemini"),
    OPENAI("OpenAI"),
    CLAUDE("Anthropic Claude"),
    OLLAMA("Ollama / Local (OpenAI Compatible)")
}

data class AiTicketSuggestion(
    val summary: String,
    val description: String,
    val issueType: String = "Task",
    val priority: String = "Medium"
)

object AiTicketSuggestionService {

    private val LOG = com.intellij.openapi.diagnostic.Logger.getInstance(AiTicketSuggestionService::class.java)

    private val client = HttpClient.newBuilder()
        .connectTimeout(Duration.ofSeconds(15))
        .build()

    fun extractEnclosingCode(project: Project, todo: TodoItem, contextRadius: Int = 15): String {
        return ApplicationManager.getApplication().runReadAction<String> {
            try {
                val virtualFile = LocalFileSystem.getInstance().findFileByPath(todo.filePath)
                    ?: VirtualFileManager.getInstance().findFileByUrl("file://${todo.filePath}")
                    ?: return@runReadAction ""
                val document = FileDocumentManager.getInstance().getDocument(virtualFile) ?: return@runReadAction ""
                val lineCount = document.lineCount
                if (lineCount == 0) return@runReadAction ""

                val targetLine = (todo.lineNumber - 1).coerceIn(0, lineCount - 1)
                val startLine = (targetLine - contextRadius).coerceAtLeast(0)
                val endLine = (targetLine + contextRadius).coerceAtMost(lineCount - 1)

                val startOffset = document.getLineStartOffset(startLine)
                val endOffset = document.getLineEndOffset(endLine)
                document.getText(TextRange(startOffset, endOffset))
            } catch (e: Exception) {
                ""
            }
        }
    }

    fun buildPrompt(todo: TodoItem, enclosingCode: String): String {
        return """
            You are an expert technical lead and software architect.
            Analyze the following TODO comment and its surrounding code to draft a structured Jira ticket.
            
            TODO Comment:
            ${todo.fullText.ifBlank { "// TODO: " + todo.description }}
            File: ${todo.filePath} (Line ${todo.lineNumber})
            
            Surrounding Code Context:
            ${enclosingCode.ifBlank { "// Context unavailable" }}
            
            Return ONLY a JSON object with the following schema:
            {
              "summary": "Short, actionable Jira title (e.g. [Refactor] ... or [Feature] ...)",
              "description": "Comprehensive explanation of what needs to be done, background context, and bullet-point Acceptance Criteria",
              "issueType": "Task", // or Story, Bug, Improvement
              "priority": "High"   // or Highest, High, Medium, Low
            }
        """.trimIndent()
    }

    fun generateTicketSuggestion(
        todo: TodoItem,
        enclosingCode: String,
        provider: AiProvider,
        apiKey: String,
        modelName: String,
        customEndpoint: String
    ): Result<AiTicketSuggestion> {
        val prompt = buildPrompt(todo, enclosingCode)

        return try {
            when (provider) {
                AiProvider.GEMINI -> callGemini(prompt, apiKey, modelName)
                AiProvider.OPENAI -> callOpenAi(prompt, apiKey, modelName, customEndpoint.ifBlank { "https://api.openai.com/v1" })
                AiProvider.CLAUDE -> callClaude(prompt, apiKey, modelName)
                AiProvider.OLLAMA -> callOpenAi(prompt, apiKey, modelName, customEndpoint.ifBlank { "http://localhost:11434/v1" })
            }
        } catch (e: Exception) {
            LOG.warn("Failed to generate AI ticket suggestion", e)
            Result.failure(e)
        }
    }

    private fun callGemini(prompt: String, apiKey: String, model: String): Result<AiTicketSuggestion> {
        if (apiKey.isBlank()) return Result.failure(IllegalArgumentException("Gemini API Key is required."))
        val modelToUse = model.ifBlank { "gemini-1.5-flash" }
        val url = "https://generativelanguage.googleapis.com/v1beta/models/$modelToUse:generateContent?key=$apiKey"

        val jsonPayload = """
            {
              "contents": [{
                "parts": [{ "text": ${escapeJson(prompt)} }]
              }],
              "generationConfig": {
                "responseMimeType": "application/json"
              }
            }
        """.trimIndent()

        val request = HttpRequest.newBuilder()
            .uri(URI.create(url))
            .header("Content-Type", "application/json")
            .POST(HttpRequest.BodyPublishers.ofString(jsonPayload))
            .timeout(Duration.ofSeconds(20))
            .build()

        val response = client.send(request, HttpResponse.BodyHandlers.ofString())
        if (response.statusCode() !in 200..299) {
            return Result.failure(RuntimeException("Gemini API returned ${response.statusCode()}: ${response.body()}"))
        }

        val rawText = extractGeminiResponseText(response.body())
        return parseSuggestionJson(rawText)
    }

    private fun callOpenAi(prompt: String, apiKey: String, model: String, baseUrl: String): Result<AiTicketSuggestion> {
        val cleanBaseUrl = baseUrl.trimEnd('/')
        val url = "$cleanBaseUrl/chat/completions"
        val modelToUse = model.ifBlank { "gpt-4o-mini" }

        val jsonPayload = """
            {
              "model": ${escapeJson(modelToUse)},
              "messages": [
                {"role": "system", "content": "You are a technical lead. Respond only with valid JSON."},
                {"role": "user", "content": ${escapeJson(prompt)}}
              ],
              "response_format": {"type": "json_object"}
            }
        """.trimIndent()

        val requestBuilder = HttpRequest.newBuilder()
            .uri(URI.create(url))
            .header("Content-Type", "application/json")
            .POST(HttpRequest.BodyPublishers.ofString(jsonPayload))
            .timeout(Duration.ofSeconds(20))

        if (apiKey.isNotBlank()) {
            requestBuilder.header("Authorization", "Bearer $apiKey")
        }

        val response = client.send(requestBuilder.build(), HttpResponse.BodyHandlers.ofString())
        if (response.statusCode() !in 200..299) {
            return Result.failure(RuntimeException("OpenAI-compatible API returned ${response.statusCode()}: ${response.body()}"))
        }

        val rawText = extractOpenAiContent(response.body())
        return parseSuggestionJson(rawText)
    }

    private fun callClaude(prompt: String, apiKey: String, model: String): Result<AiTicketSuggestion> {
        if (apiKey.isBlank()) return Result.failure(IllegalArgumentException("Anthropic API Key is required."))
        val modelToUse = model.ifBlank { "claude-3-5-sonnet-20241022" }
        val url = "https://api.anthropic.com/v1/messages"

        val jsonPayload = """
            {
              "model": ${escapeJson(modelToUse)},
              "max_tokens": 1024,
              "messages": [
                {"role": "user", "content": ${escapeJson(prompt)}}
              ]
            }
        """.trimIndent()

        val request = HttpRequest.newBuilder()
            .uri(URI.create(url))
            .header("x-api-key", apiKey)
            .header("anthropic-version", "2023-06-01")
            .header("Content-Type", "application/json")
            .POST(HttpRequest.BodyPublishers.ofString(jsonPayload))
            .timeout(Duration.ofSeconds(20))
            .build()

        val response = client.send(request, HttpResponse.BodyHandlers.ofString())
        if (response.statusCode() !in 200..299) {
            return Result.failure(RuntimeException("Claude API returned ${response.statusCode()}: ${response.body()}"))
        }

        val rawText = extractClaudeContent(response.body())
        return parseSuggestionJson(rawText)
    }

    fun parseSuggestionJson(json: String): Result<AiTicketSuggestion> {
        return try {
            val cleanJson = json.trim().removePrefix("```json").removePrefix("```").removeSuffix("```").trim()

            val summary = extractJsonField(cleanJson, "summary") ?: "Address TODO item"
            val description = extractJsonField(cleanJson, "description") ?: cleanJson
            val issueType = extractJsonField(cleanJson, "issueType") ?: "Task"
            val priority = extractJsonField(cleanJson, "priority") ?: "Medium"

            Result.success(AiTicketSuggestion(
                summary = summary,
                description = description,
                issueType = issueType,
                priority = priority
            ))
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    private fun extractGeminiResponseText(body: String): String {
        val pattern = Pattern.compile("\"text\"\\s*:\\s*\"((?:\\\\\"|[^\"])*)\"")
        val matcher = pattern.matcher(body)
        if (matcher.find()) {
            return unescapeJson(matcher.group(1))
        }
        return body
    }

    private fun extractOpenAiContent(body: String): String {
        val pattern = Pattern.compile("\"content\"\\s*:\\s*\"((?:\\\\\"|[^\"])*)\"")
        val matcher = pattern.matcher(body)
        if (matcher.find()) {
            return unescapeJson(matcher.group(1))
        }
        return body
    }

    private fun extractClaudeContent(body: String): String {
        val pattern = Pattern.compile("\"text\"\\s*:\\s*\"((?:\\\\\"|[^\"])*)\"")
        val matcher = pattern.matcher(body)
        if (matcher.find()) {
            return unescapeJson(matcher.group(1))
        }
        return body
    }

    private fun extractJsonField(json: String, fieldName: String): String? {
        val pattern = Pattern.compile("\"$fieldName\"\\s*:\\s*\"((?:\\\\\"|[^\"])*)\"")
        val matcher = pattern.matcher(json)
        if (matcher.find()) {
            return unescapeJson(matcher.group(1))
        }
        return null
    }

    private fun escapeJson(text: String): String {
        val escaped = text
            .replace("\\", "\\\\")
            .replace("\"", "\\\"")
            .replace("\n", "\\n")
            .replace("\r", "\\r")
            .replace("\t", "\\t")
        return "\"$escaped\""
    }

    private fun unescapeJson(text: String): String {
        return text
            .replace("\\\"", "\"")
            .replace("\\\\", "\\")
            .replace("\\n", "\n")
            .replace("\\r", "\r")
            .replace("\\t", "\t")
    }
}
