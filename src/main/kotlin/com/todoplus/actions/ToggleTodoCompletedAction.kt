package com.todoplus.actions

import com.intellij.openapi.command.WriteCommandAction
import com.intellij.openapi.fileEditor.FileDocumentManager
import com.intellij.openapi.project.Project
import com.intellij.openapi.util.TextRange
import com.intellij.openapi.vfs.LocalFileSystem
import com.todoplus.models.TodoItem

object ToggleTodoCompletedAction {

    fun toggleCompletion(project: Project, todo: TodoItem, onComplete: () -> Unit = {}) {
        setCompletionForMultiple(project, listOf(todo), !todo.isCompleted, onComplete)
    }

    fun setCompletionForMultiple(
        project: Project,
        todos: List<TodoItem>,
        markAsCompleted: Boolean,
        onComplete: () -> Unit = {}
    ) {
        if (todos.isEmpty()) return

        val settings = com.todoplus.settings.TodoSettingsService.getInstance()
        val isDeleteMode = markAsCompleted && settings.getState().completionBehavior == com.todoplus.settings.TodoSettingsService.BEHAVIOR_DELETE_COMMENT

        val byFile = todos.groupBy { it.filePath }

        WriteCommandAction.runWriteCommandAction(project, "Batch Update TODO Completion", null, Runnable {
            for ((filePath, items) in byFile) {
                val virtualFile = LocalFileSystem.getInstance().findFileByPath(filePath)
                    ?: com.intellij.openapi.vfs.VirtualFileManager.getInstance().findFileByUrl("file://$filePath")
                    ?: continue

                val psiFile = com.intellij.psi.PsiManager.getInstance(project).findFile(virtualFile)
                val document = FileDocumentManager.getInstance().getDocument(virtualFile)
                    ?: (psiFile?.let { com.intellij.psi.PsiDocumentManager.getInstance(project).getDocument(it) })
                    ?: continue

                // Process lines in reverse order (bottom to top)
                val sortedItems = items.sortedByDescending { it.lineNumber }

                for (todo in sortedItems) {
                    if (todo.isCompleted == markAsCompleted && !isDeleteMode) continue
                    if (todo.lineNumber <= 0 || todo.lineNumber > document.lineCount) continue

                    var targetLineIndex = todo.lineNumber - 1
                    var currentLineText = document.getText(TextRange(document.getLineStartOffset(targetLineIndex), document.getLineEndOffset(targetLineIndex)))

                    if (!currentLineText.contains(todo.description, ignoreCase = true)) {
                        val minLine = maxOf(0, targetLineIndex - 15)
                        val maxLine = minOf(document.lineCount - 1, targetLineIndex + 15)
                        for (idx in minLine..maxLine) {
                            val candidateText = document.getText(TextRange(document.getLineStartOffset(idx), document.getLineEndOffset(idx)))
                            if (candidateText.contains(todo.description, ignoreCase = true)) {
                                targetLineIndex = idx
                                currentLineText = candidateText
                                break
                            }
                        }
                    }

                    val lineStart = document.getLineStartOffset(targetLineIndex)
                    val lineEnd = document.getLineEndOffset(targetLineIndex)

                    if (isDeleteMode) {
                        val deleteEnd = if (targetLineIndex < document.lineCount - 1) {
                            document.getLineStartOffset(targetLineIndex + 1)
                        } else {
                            lineEnd
                        }
                        document.deleteString(lineStart, deleteEnd)
                    } else {
                        val updatedLineText = if (!markAsCompleted) {
                            var text = currentLineText.replace(Regex("(?i)(//|#|--|/\\*)\\s*(DONE|COMPLETED)\\b"), "$1 TODO")
                            text = text.replace(Regex("(?i)status:done"), "status:todo")
                            text = text.replace(Regex("(?i)status:completed"), "status:todo")
                            text = text.replace(Regex("(?i)done:true"), "done:false")
                            text
                        } else {
                            var text = currentLineText.replace(Regex("(?i)(//|#|--|/\\*)\\s*(TODO|FIXME)\\b"), "$1 DONE")
                            if (!text.contains(Regex("(?i)\\bDONE\\b"))) {
                                if (text.contains("(") && text.contains(")")) {
                                    text = text.replace(")", " status:done)")
                                }
                            }
                            text
                        }

                        if (updatedLineText != currentLineText) {
                            document.replaceString(lineStart, lineEnd, updatedLineText)
                        }
                    }
                }

                FileDocumentManager.getInstance().saveDocument(document)
            }
        })

        com.intellij.openapi.application.ApplicationManager.getApplication().invokeLater {
            onComplete()
        }
    }
}
