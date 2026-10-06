package com.todoplus.vcs

import com.intellij.openapi.ui.Messages
import com.intellij.openapi.vcs.CheckinProjectPanel
import com.intellij.openapi.vcs.changes.CommitContext
import com.intellij.openapi.vcs.checkin.CheckinHandler
import com.intellij.openapi.vcs.checkin.CheckinHandlerFactory
import com.intellij.openapi.wm.ToolWindowManager
import com.todoplus.models.TodoItem
import com.todoplus.parser.TodoParser
import com.todoplus.settings.TodoSettingsService

/**
 * VCS Checkin handler that inspects staged files before commit for unlinked TODO comments.
 * Alerts developers before pushing orphan TODOs across sprints.
 */
class TodoCheckinHandlerFactory : CheckinHandlerFactory() {

    override fun createHandler(panel: CheckinProjectPanel, commitContext: CommitContext): CheckinHandler {
        return object : CheckinHandler() {
            override fun beforeCheckin(): ReturnResult {
                val settings = TodoSettingsService.getInstance().getState()
                if (!settings.enablePreCommitTodoCheck) {
                    return ReturnResult.COMMIT
                }

                val project = panel.project
                val files = panel.virtualFiles
                val parser = TodoParser(settings.issuePattern)
                val unlinkedTodos = mutableListOf<TodoItem>()

                for (file in files) {
                    if (file.isDirectory || !file.isValid) continue
                    try {
                        val content = String(file.contentsToByteArray())
                        val todos = parser.parseLines(content.lines(), file.path)
                        val unlinked = todos.filter { it.issueId.isNullOrBlank() && !it.isCompleted }
                        unlinkedTodos.addAll(unlinked)
                    } catch (ignored: Exception) {
                    }
                }

                if (unlinkedTodos.isNotEmpty()) {
                    val count = unlinkedTodos.size
                    val choice = Messages.showYesNoCancelDialog(
                        project,
                        "Found $count unlinked TODO comment(s) without Jira tickets in this commit.\n\n" +
                                "Would you like to open TODO++ to draft Jira tickets with AI before committing?",
                        "TODO++: Unlinked TODOs Detected",
                        "Review in TODO++",
                        "Commit Anyway",
                        "Cancel Commit",
                        Messages.getWarningIcon()
                    )

                    return when (choice) {
                        Messages.YES -> {
                            ToolWindowManager.getInstance(project).getToolWindow("TODO++")?.show()
                            ReturnResult.CANCEL
                        }
                        Messages.NO -> ReturnResult.COMMIT
                        else -> ReturnResult.CANCEL
                    }
                }

                return ReturnResult.COMMIT
            }
        }
    }
}
