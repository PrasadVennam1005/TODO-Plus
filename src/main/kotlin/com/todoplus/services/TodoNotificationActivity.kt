package com.todoplus.services

import com.intellij.notification.NotificationGroupManager
import com.intellij.notification.NotificationType
import com.intellij.openapi.actionSystem.AnAction
import com.intellij.openapi.actionSystem.AnActionEvent
import com.intellij.openapi.application.ApplicationManager
import com.intellij.openapi.components.service
import com.intellij.openapi.progress.ProgressIndicator
import com.intellij.openapi.progress.ProgressManager
import com.intellij.openapi.progress.Task
import com.intellij.openapi.project.Project
import com.intellij.openapi.startup.ProjectActivity
import com.intellij.openapi.wm.ToolWindowManager
import com.intellij.openapi.project.DumbService
import java.time.LocalDate

class TodoNotificationActivity : ProjectActivity {
    override suspend fun execute(project: Project) {
        // Schedule execution for when the project is smart (indexes are ready)
        // This prevents blocking project initialization or throwing "Cannot init component state"
        DumbService.getInstance(project).runWhenSmart {
            ProgressManager.getInstance().run(object : Task.Backgroundable(project, "Checking pending TODOs", true) {
            override fun run(indicator: ProgressIndicator) {
                try {
                    val scanner = project.service<TodoScannerService>()
                    val todos = scanner.scanProject()
                    
                    val today = LocalDate.now()
                    val dueDateTodos = todos.filter { it.dueDate != null }
                    val overdueTodos = dueDateTodos.filter { it.dueDate!!.isBefore(today) }
                    val justDueToday = dueDateTodos.filter { it.dueDate!!.isEqual(today) }

                    if (overdueTodos.isNotEmpty() || justDueToday.isNotEmpty()) {
                        val message = buildString {
                            if (overdueTodos.isNotEmpty()) {
                                append("${overdueTodos.size} overdue")
                            }
                            if (overdueTodos.isNotEmpty() && justDueToday.isNotEmpty()) {
                                append(" and ")
                            }
                            if (justDueToday.isNotEmpty()) {
                                append("${justDueToday.size} due today")
                            }
                        }

                        ApplicationManager.getApplication().invokeLater {
                            NotificationGroupManager.getInstance()
                                .getNotificationGroup("TODO++ Notifications")
                                .createNotification("TODOs Need Attention", message, NotificationType.WARNING)
                                .addAction(object : AnAction("View TODOs") {
                                    override fun actionPerformed(e: AnActionEvent) {
                                        val toolWindow = ToolWindowManager.getInstance(project).getToolWindow("TODO++")
                                        toolWindow?.show()
                                    }
                                })
                                .notify(project)
                        }
                    }
                } catch (e: Exception) {
                    val log = com.intellij.openapi.diagnostic.Logger.getInstance(TodoNotificationActivity::class.java)
                    log.warn("Failed to notify TODOs", e)
                }
            }
        })
        }
    }
}
