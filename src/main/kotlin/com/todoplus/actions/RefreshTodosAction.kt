package com.todoplus.actions

import com.intellij.openapi.actionSystem.AnAction
import com.intellij.openapi.actionSystem.AnActionEvent
import com.intellij.openapi.components.service
import com.intellij.openapi.wm.ToolWindowManager
import com.todoplus.services.TodoScannerService

/**
 * Action to refresh/scan project for TODOs
 */
class RefreshTodosAction : AnAction("Refresh TODOs", "Scan project for TODO items", null) {

    override fun getActionUpdateThread(): com.intellij.openapi.actionSystem.ActionUpdateThread = com.intellij.openapi.actionSystem.ActionUpdateThread.EDT

    override fun actionPerformed(e: AnActionEvent) {
        val project = e.project ?: return
        
        // Asynchronously request single-flight project scan; updates central cache and broadcasts to all tool windows
        val scanner = project.service<TodoScannerService>()
        scanner.requestProjectScan()
    }
}

