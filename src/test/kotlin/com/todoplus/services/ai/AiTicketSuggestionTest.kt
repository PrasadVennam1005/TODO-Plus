package com.todoplus.services.ai

import com.todoplus.models.TodoItem
import com.todoplus.parser.TodoParser
import com.todoplus.ui.dialogs.AiJiraTicketDialog
import org.junit.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertTrue

class AiTicketSuggestionTest {

    @Test
    fun `test buildPrompt generates structured prompt with context`() {
        val todo = TodoItem(
            description = "Refactor authentication token validation logic",
            filePath = "/src/auth/TokenService.kt",
            lineNumber = 42,
            fullText = "// TODO: Refactor authentication token validation logic"
        )
        val enclosingCode = """
            fun validateToken(token: String): Boolean {
                // TODO: Refactor authentication token validation logic
                return token.startsWith("ey")
            }
        """.trimIndent()

        val prompt = AiTicketSuggestionService.buildPrompt(todo, enclosingCode)

        assertTrue(prompt.contains("Refactor authentication token validation logic"))
        assertTrue(prompt.contains("/src/auth/TokenService.kt (Line 42)"))
        assertTrue(prompt.contains("return token.startsWith(\"ey\")"))
        assertTrue(prompt.contains("\"summary\":"))
        assertTrue(prompt.contains("\"description\":"))
        assertTrue(prompt.contains("\"issueType\":"))
    }

    @Test
    fun `test parseSuggestionJson parses valid clean JSON`() {
        val rawJson = """
            {
              "summary": "[Refactor] Modernize JWT token verification",
              "description": "Refactor token parsing to use verified RSA keys.\n\nAcceptance Criteria:\n- Validate expiration\n- Reject expired tokens",
              "issueType": "Story",
              "priority": "High"
            }
        """.trimIndent()

        val result = AiTicketSuggestionService.parseSuggestionJson(rawJson)
        assertTrue(result.isSuccess)
        val suggestion = result.getOrThrow()

        assertEquals("[Refactor] Modernize JWT token verification", suggestion.summary)
        assertTrue(suggestion.description.contains("Refactor token parsing"))
        assertEquals("Story", suggestion.issueType)
        assertEquals("High", suggestion.priority)
    }

    @Test
    fun `test parseSuggestionJson handles markdown fenced json`() {
        val markdownJson = """
            ```json
            {
              "summary": "Fix database connection leak",
              "description": "Ensure connections are closed in finally block",
              "issueType": "Bug",
              "priority": "Highest"
            }
            ```
        """.trimIndent()

        val result = AiTicketSuggestionService.parseSuggestionJson(markdownJson)
        assertTrue(result.isSuccess)
        val suggestion = result.getOrThrow()

        assertEquals("Fix database connection leak", suggestion.summary)
        assertEquals("Ensure connections are closed in finally block", suggestion.description)
        assertEquals("Bug", suggestion.issueType)
        assertEquals("Highest", suggestion.priority)
    }

    @Test
    fun `test injectIssueIdIntoComment handles various comment patterns`() {
        // Case 1: Simple colon TODO
        val line1 = "    // TODO: implement distributed cache"
        val updated1 = AiJiraTicketDialog.injectIssueIdIntoComment(line1, "PROJ-101")
        assertEquals("    // TODO(issue:PROJ-101): implement distributed cache", updated1)

        // Case 2: Parenthesized metadata already present
        val line2 = "    // TODO(@john due:2026-11-01): sanitize query inputs"
        val updated2 = AiJiraTicketDialog.injectIssueIdIntoComment(line2, "SEC-404")
        assertEquals("    // TODO(@john due:2026-11-01 issue:SEC-404): sanitize query inputs", updated2)

        // Case 3: Bare TODO with space
        val line3 = "    // TODO refactor legacy dispatcher"
        val updated3 = AiJiraTicketDialog.injectIssueIdIntoComment(line3, "ARCH-99")
        assertEquals("    // TODO(issue:ARCH-99) refactor legacy dispatcher", updated3)

        // Case 4: Python style hash comment
        val line4 = "    # TODO: add retry with exponential backoff"
        val updated4 = AiJiraTicketDialog.injectIssueIdIntoComment(line4, "NET-502")
        assertEquals("    # TODO(issue:NET-502): add retry with exponential backoff", updated4)
    }

    @Test
    fun `test pre-commit unlinked TODO filtering`() {
        val parser = TodoParser("issue:([A-Za-z0-9_-]+)")
        val lines = listOf(
            "// TODO: unlinked task for future sprint",
            "// TODO(issue:PROJ-123): already tracked task",
            "// [x] TODO: completed task from last sprint",
            "val x = 10",
            "// TODO(@alice): unlinked developer note"
        )

        val parsedTodos = parser.parseLines(lines, "/src/Sample.kt")
        val unlinked = parsedTodos.filter { it.issueId.isNullOrBlank() && !it.isCompleted }

        assertEquals(2, unlinked.size)
        assertTrue(unlinked.any { it.description.contains("unlinked task for future sprint") })
        assertTrue(unlinked.any { it.description.contains("unlinked developer note") })
        assertFalse(unlinked.any { it.description.contains("already tracked task") })
        assertFalse(unlinked.any { it.description.contains("completed task") })
    }
}
