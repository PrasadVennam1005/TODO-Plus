package com.todoplus.parser

import com.todoplus.models.Priority
import com.todoplus.models.TodoItem

/**
 * Parser for extracting enhanced TODO comments from code
 * 
 * Supports formats:
 * - // TODO: Simple todo
 * - // TODO(@john): Todo with assignee
 * - // TODO(priority:high): Todo with priority
 * - // TODO(category:bug): Todo with category
 * - // TODO(@john priority:high category:bug): Full format
 */
class TodoParser(private val issuePattern: String = "") {

    companion object {
        fun getTodoPattern(customKeywords: List<String> = emptyList()): Regex {
            val baseKeywords = listOf("TODO", "FIXME", "DONE", "COMPLETED")
            val allKeywords = (baseKeywords + customKeywords).filter { it.isNotBlank() }.distinct().map { Regex.escape(it) }
            val joined = allKeywords.joinToString("|")
            return Regex(
                """(?://|#|--|/\*)\s*($joined)\s*(?:\((.*?)\))?\s*:\s*(.*?)(?=(?://|#|--|/\*)\s*(?:$joined)\b|${'$'})""",
                RegexOption.IGNORE_CASE
            )
        }

        private val TODO_PATTERN: Regex
            get() {
                val custom = try {
                    com.todoplus.settings.TodoSettingsService.getInstance().state.customKeywords.map { it.keyword }
                } catch (e: Throwable) {
                    emptyList()
                }
                return getTodoPattern(custom)
            }

        // Pattern to extract assignee: @username
        private val ASSIGNEE_PATTERN = Regex("""@(\w+)""")

        // Pattern to extract priority: priority:level
        private val PRIORITY_PATTERN = Regex("""priority:(\w+)""", RegexOption.IGNORE_CASE)

        // Pattern to extract category: category:type
        private val CATEGORY_PATTERN = Regex("""category:(\w+)""", RegexOption.IGNORE_CASE)
    }

    /**
     * Parse a single line of code for TODO/DONE comments (returns first match for backward compatibility)
     */
    fun parseLine(line: String, filePath: String, lineNumber: Int): TodoItem? {
        return parseAllFromLine(line, filePath, lineNumber).firstOrNull()
    }

    /**
     * Parse all TODO/DONE comments present on a single line
     */
    fun parseAllFromLine(line: String, filePath: String, lineNumber: Int): List<TodoItem> {
        val matches = TODO_PATTERN.findAll(line).toList()
        if (matches.isEmpty()) return emptyList()

        return matches.map { matchResult ->
            val keyword = matchResult.groupValues[1].uppercase()
            val metadataStr = matchResult.groupValues[2]
            var description = matchResult.groupValues[3].trim()

            // Remove trailing comment end symbols if block comment
            description = description.removeSuffix("*/").trim()

            // Extract all metadata
            val (assignee, priority, category, explicitIssueId, dueDate, tags) = extractMetadata(metadataStr)
            
            // If explicit issue ID not found, try to find in description using configured pattern
            var finalIssueId = explicitIssueId
            if (finalIssueId == null && issuePattern.isNotEmpty()) {
                 try {
                     val regex = Regex(issuePattern)
                     val match = regex.find(description)
                     if (match != null) {
                         finalIssueId = match.value
                     }
                 } catch (e: Exception) {
                     // Invalid regex, ignore
                 }
            }

            val isCompletedKeyword = keyword == "DONE" || keyword == "COMPLETED"
            val isCompletedTag = tags["status"]?.lowercase() == "done" || tags["status"]?.lowercase() == "completed" || tags["done"]?.lowercase() == "true"
            val isCompleted = isCompletedKeyword || isCompletedTag

            TodoItem(
                description = description,
                assignee = assignee,
                priority = priority,
                category = category,
                issueId = finalIssueId,
                tags = tags,
                dueDate = dueDate,
                filePath = filePath,
                lineNumber = lineNumber,
                fullText = line.trim(),
                isCompleted = isCompleted
            )
        }
    }

    /**
     * Parse multiple lines from a file, extracting all TODO items (including multi-line comments)
     */
    fun parseLines(lines: List<String>, filePath: String): List<TodoItem> {
        val result = mutableListOf<TodoItem>()
        var i = 0

        while (i < lines.size) {
            val currentLine = lines[i]
            val itemsOnLine = parseAllFromLine(currentLine, filePath, i + 1)

            if (itemsOnLine.isNotEmpty()) {
                val lastItemIndex = itemsOnLine.size - 1
                val multiLineItems = itemsOnLine.toMutableList()

                // Check for multi-line comment continuation on subsequent lines for the last TODO on this line
                val lastItem = multiLineItems[lastItemIndex]
                val descBuilder = StringBuilder(lastItem.description)
                var nextLineIdx = i + 1

                while (nextLineIdx < lines.size) {
                    val nextLine = lines[nextLineIdx]
                    val trimmed = nextLine.trim()

                    val isCommentLine = trimmed.startsWith("//") || trimmed.startsWith("#") || trimmed.startsWith("--") || trimmed.startsWith("*")
                    if (!isCommentLine) break

                    if (TODO_PATTERN.containsMatchIn(nextLine)) break

                    val continuationText = trimmed
                        .removePrefix("//")
                        .removePrefix("#")
                        .removePrefix("--")
                        .removePrefix("*")
                        .removeSuffix("*/")
                        .trim()

                    if (continuationText.isNotBlank()) {
                        descBuilder.append("\n").append(continuationText)
                        nextLineIdx++
                    } else {
                        break
                    }
                }

                if (nextLineIdx > i + 1) {
                    multiLineItems[lastItemIndex] = lastItem.copy(description = descBuilder.toString())
                    i = nextLineIdx - 1
                }

                result.addAll(multiLineItems)
            }
            i++
        }

        return result
    }

    private data class Metadata(
        val assignee: String? = null,
        val priority: Priority? = null,
        val category: String? = null,
        val issueId: String? = null,
        val dueDate: java.time.LocalDate? = null,
        val tags: Map<String, String> = emptyMap()
    )

    /**
     * Extract all metadata components from the metadata string
     */
    private fun extractMetadata(metadataStr: String): Metadata {
        if (metadataStr.isBlank()) return Metadata()

        var assignee: String? = null
        var priority: Priority? = null
        var category: String? = null
        var issueId: String? = null
        var dueDate: java.time.LocalDate? = null
        val tags = mutableMapOf<String, String>()

        // Use custom splitter to respect quotes
        val parts = splitMetadata(metadataStr)

        for (part in parts) {
            when {
                // key:value (even if it starts with @, e.g. @priority:HIGH)
                part.contains(":") -> {
                    val keyVal = part.split(":", limit = 2)
                    if (keyVal.size == 2) {
                        val key = keyVal[0].lowercase().removePrefix("@")
                        var value = keyVal[1]
                        
                        // Strip quotes if present
                        if (value.startsWith("\"") && value.endsWith("\"") && value.length >= 2) {
                            value = value.substring(1, value.length - 1)
                        }

                        when (key) {
                            "priority" -> priority = Priority.parse(value)
                            "category" -> category = value
                            "assignee", "assigned" -> assignee = value.removePrefix("@")
                            "due" -> dueDate = parseDueDate(value)
                            "issue" -> issueId = value
                            else -> tags[key] = value
                        }
                    }
                }
                // @assignee (standalone word starting with @)
                part.startsWith("@") -> {
                    assignee = part.substring(1)
                }
            }
        }

        return Metadata(assignee, priority, category, issueId, dueDate, tags)
    }

    /**
     * Split metadata string by spaces, ignoring spaces inside quotes
     */
    private fun splitMetadata(str: String): List<String> {
        val result = mutableListOf<String>()
        val currentToken = StringBuilder()
        var inQuote = false

        for (char in str) {
            when {
                char == '"' -> {
                    inQuote = !inQuote
                    currentToken.append(char)
                }
                char.isWhitespace() -> {
                    if (inQuote) {
                        currentToken.append(char)
                    } else if (currentToken.isNotEmpty()) {
                        result.add(currentToken.toString())
                        currentToken.clear()
                    }
                }
                else -> currentToken.append(char)
            }
        }
        
        if (currentToken.isNotEmpty()) {
            result.add(currentToken.toString())
        }
        
        return result
    }

    private fun parseDueDate(value: String): java.time.LocalDate? {
        return try {
            when (value.lowercase()) {
                "today" -> java.time.LocalDate.now()
                "tomorrow" -> java.time.LocalDate.now().plusDays(1)
                else -> java.time.LocalDate.parse(value) // Expects YYYY-MM-DD
            }
        } catch (e: Exception) {
            null
        }
    }
}
