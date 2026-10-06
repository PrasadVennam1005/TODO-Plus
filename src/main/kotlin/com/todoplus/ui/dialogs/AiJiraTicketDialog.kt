package com.todoplus.ui.dialogs

import com.intellij.notification.NotificationGroupManager
import com.intellij.notification.NotificationType
import com.intellij.openapi.application.ApplicationManager
import com.intellij.openapi.command.WriteCommandAction
import com.intellij.openapi.fileEditor.FileDocumentManager
import com.intellij.openapi.progress.ProgressIndicator
import com.intellij.openapi.progress.ProgressManager
import com.intellij.openapi.progress.Task
import com.intellij.openapi.project.Project
import com.intellij.openapi.ui.DialogWrapper
import com.intellij.openapi.util.TextRange
import com.intellij.openapi.vfs.LocalFileSystem
import com.intellij.openapi.vfs.VirtualFileManager
import com.intellij.ui.components.JBLabel
import com.intellij.ui.components.JBPanel
import com.intellij.ui.components.JBScrollPane
import com.intellij.ui.components.JBTextArea
import com.intellij.ui.components.JBTextField
import com.intellij.util.ui.JBUI
import com.todoplus.models.TodoItem
import com.todoplus.services.ai.AiTicketSuggestion
import com.todoplus.services.integration.IssueExporterService
import com.todoplus.settings.TodoSettingsService
import java.awt.BorderLayout
import java.awt.Dimension
import java.awt.GridBagConstraints
import java.awt.GridBagLayout
import javax.swing.JComboBox
import javax.swing.JComponent
import javax.swing.JPanel

class AiJiraTicketDialog(
    private val project: Project,
    private val todo: TodoItem,
    suggestion: AiTicketSuggestion,
    private val onLinked: (String) -> Unit = {}
) : DialogWrapper(project, true) {

    private val summaryField = JBTextField(suggestion.summary)
    private val issueTypeDropdown = JComboBox(arrayOf("Task", "Story", "Bug", "Improvement")).apply {
        selectedItem = suggestion.issueType
    }
    private val priorityDropdown = JComboBox(arrayOf("High", "Highest", "Medium", "Low", "Lowest")).apply {
        selectedItem = suggestion.priority
    }
    private val descriptionArea = JBTextArea(suggestion.description, 10, 50).apply {
        lineWrap = true
        wrapStyleWord = true
    }

    init {
        title = "AI Suggested Jira Ticket"
        setOKButtonText("Create in Jira & Link in Code")
        init()
    }

    override fun createCenterPanel(): JComponent {
        val panel = JBPanel<JBPanel<*>>(BorderLayout(0, 10)).apply {
            preferredSize = Dimension(600, 450)
            border = JBUI.Borders.empty(10)
        }

        // Header info
        val headerPanel = JBPanel<JBPanel<*>>(BorderLayout()).apply {
            border = JBUI.Borders.customLineBottom(com.intellij.ui.Gray._200)
            val infoLabel = JBLabel("<html><b>Source TODO:</b> <code>${todo.fullText.ifBlank { todo.description }}</code><br/>" +
                    "<b>File:</b> ${todo.getFileName()} (Line ${todo.lineNumber})</html>")
            add(infoLabel, BorderLayout.NORTH)
        }
        panel.add(headerPanel, BorderLayout.NORTH)

        // Form Fields
        val formPanel = JPanel(GridBagLayout())
        val gbc = GridBagConstraints().apply {
            fill = GridBagConstraints.HORIZONTAL
            insets = JBUI.insets(4)
        }

        // Summary
        gbc.gridx = 0; gbc.gridy = 0; gbc.weightx = 0.0
        formPanel.add(JBLabel("Summary / Title:"), gbc)
        gbc.gridx = 1; gbc.gridy = 0; gbc.weightx = 1.0
        formPanel.add(summaryField, gbc)

        // Issue Type
        gbc.gridx = 0; gbc.gridy = 1; gbc.weightx = 0.0
        formPanel.add(JBLabel("Issue Type:"), gbc)
        gbc.gridx = 1; gbc.gridy = 1; gbc.weightx = 1.0
        formPanel.add(issueTypeDropdown, gbc)

        // Priority
        gbc.gridx = 0; gbc.gridy = 2; gbc.weightx = 0.0
        formPanel.add(JBLabel("Priority:"), gbc)
        gbc.gridx = 1; gbc.gridy = 2; gbc.weightx = 1.0
        formPanel.add(priorityDropdown, gbc)

        // Description
        gbc.gridx = 0; gbc.gridy = 3; gbc.weightx = 0.0; gbc.anchor = GridBagConstraints.NORTHWEST
        formPanel.add(JBLabel("Description & Criteria:"), gbc)
        gbc.gridx = 1; gbc.gridy = 3; gbc.weightx = 1.0; gbc.weighty = 1.0; gbc.fill = GridBagConstraints.BOTH
        formPanel.add(JBScrollPane(descriptionArea), gbc)

        panel.add(formPanel, BorderLayout.CENTER)
        return panel
    }

    override fun doOKAction() {
        val summary = summaryField.text.trim()
        val description = descriptionArea.text.trim()
        val issueType = issueTypeDropdown.selectedItem as String
        val settings = TodoSettingsService.getInstance().getState()

        if (settings.jiraBaseUrl.isBlank() || settings.jiraEmail.isBlank() || settings.jiraApiToken.isBlank() || settings.jiraProjectKey.isBlank()) {
            NotificationGroupManager.getInstance()
                .getNotificationGroup("TODO++ Notifications")
                .createNotification("Please configure Jira credentials in Settings > Tools > TODO++ first.", NotificationType.WARNING)
                .notify(project)
            return
        }

        super.doOKAction()

        ProgressManager.getInstance().run(object : Task.Backgroundable(project, "Creating Jira Issue", true) {
            override fun run(indicator: ProgressIndicator) {
                indicator.isIndeterminate = true
                indicator.text = "Exporting to Jira..."

                val result = IssueExporterService.createJiraIssue(
                    todo = todo,
                    baseUrl = settings.jiraBaseUrl,
                    email = settings.jiraEmail,
                    apiToken = settings.jiraApiToken,
                    projectKey = settings.jiraProjectKey,
                    customSummary = summary,
                    customDescription = description,
                    customIssueType = issueType
                )

                ApplicationManager.getApplication().invokeLater {
                    result.onSuccess { url ->
                        val issueKey = url.substringAfterLast('/')
                        linkIssueInCode(todo, issueKey)
                        onLinked(issueKey)

                        NotificationGroupManager.getInstance()
                            .getNotificationGroup("TODO++ Notifications")
                            .createNotification(
                                "Jira Issue Created",
                                "Issue <a href=\"$url\">$issueKey</a> created and linked in source code.",
                                NotificationType.INFORMATION
                            )
                            .notify(project)
                    }.onFailure { err ->
                        NotificationGroupManager.getInstance()
                            .getNotificationGroup("TODO++ Notifications")
                            .createNotification("Failed to create Jira issue: ${err.message}", NotificationType.ERROR)
                            .notify(project)
                    }
                }
            }
        })
    }

    private fun linkIssueInCode(todo: TodoItem, issueKey: String) {
        val virtualFile = LocalFileSystem.getInstance().findFileByPath(todo.filePath)
            ?: VirtualFileManager.getInstance().findFileByUrl("file://${todo.filePath}")
            ?: return

        val document = FileDocumentManager.getInstance().getDocument(virtualFile) ?: return

        WriteCommandAction.runWriteCommandAction(project, "Link Jira Ticket in TODO", null, Runnable {
            if (todo.lineNumber <= 0 || todo.lineNumber > document.lineCount) return@Runnable
            val lineIndex = todo.lineNumber - 1
            val startOffset = document.getLineStartOffset(lineIndex)
            val endOffset = document.getLineEndOffset(lineIndex)
            val lineText = document.getText(TextRange(startOffset, endOffset))

            val updatedLine = injectIssueIdIntoComment(lineText, issueKey)
            if (updatedLine != lineText) {
                document.replaceString(startOffset, endOffset, updatedLine)
                FileDocumentManager.getInstance().saveDocument(document)
            }
        })
    }

    companion object {
        fun injectIssueIdIntoComment(originalLine: String, issueKey: String): String {
            // Check if already has parenthetical metadata: // TODO(...) or TODO(...)
            val parenRegex = Regex("""(\bTODO\s*\()([^)]*)(\))""", RegexOption.IGNORE_CASE)
            val match = parenRegex.find(originalLine)
            if (match != null) {
                val prefix = match.groupValues[1]
                val inside = match.groupValues[2].trim()
                val suffix = match.groupValues[3]
                val newInside = if (inside.isBlank()) "issue:$issueKey" else "$inside issue:$issueKey"
                return originalLine.replaceRange(match.range, "$prefix$newInside$suffix")
            }

            // Bare TODO comment: // TODO: description -> // TODO(issue:KEY): description
            val bareRegex = Regex("""(\bTODO\b)(\s*:\s*|\s*)""", RegexOption.IGNORE_CASE)
            val bareMatch = bareRegex.find(originalLine)
            if (bareMatch != null) {
                val separator = bareMatch.groupValues[2]
                val replacement = when {
                    separator.contains(":") -> "TODO(issue:$issueKey): "
                    separator.isNotEmpty() -> "TODO(issue:$issueKey) "
                    else -> "TODO(issue:$issueKey)"
                }
                return originalLine.replaceRange(bareMatch.range, replacement)
            }

            return originalLine
        }
    }
}
