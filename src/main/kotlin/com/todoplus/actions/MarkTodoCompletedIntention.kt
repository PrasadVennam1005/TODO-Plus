package com.todoplus.actions

import com.intellij.codeInsight.intention.IntentionAction
import com.intellij.openapi.editor.Editor
import com.intellij.openapi.project.Project
import com.intellij.psi.PsiComment
import com.intellij.psi.PsiFile

class MarkTodoCompletedIntention : IntentionAction {

    override fun getText(): String = "Mark TODO as Completed"

    override fun getFamilyName(): String = "TODO++"

    override fun isAvailable(project: Project, editor: Editor?, file: PsiFile?): Boolean {
        if (editor == null || file == null) return false

        val element = file.findElementAt(editor.caretModel.offset) ?: return false
        val comment = (element as? PsiComment) ?: (element.parent as? PsiComment) ?: return false

        val text = comment.text
        val isTodo = text.contains("TODO", ignoreCase = true) || text.contains("FIXME", ignoreCase = true)
        val isDone = text.contains("DONE", ignoreCase = true) || text.contains("COMPLETED", ignoreCase = true)

        return isTodo && !isDone
    }

    override fun invoke(project: Project, editor: Editor?, file: PsiFile?) {
        if (editor == null || file == null) return

        val element = file.findElementAt(editor.caretModel.offset) ?: return
        val comment = (element as? PsiComment) ?: (element.parent as? PsiComment) ?: return

        val lineNumber = editor.document.getLineNumber(comment.textOffset) + 1
        val todoScanner = project.getService(com.todoplus.services.TodoScannerService::class.java)
        val virtualFile = file.virtualFile ?: return
        val todos = todoScanner.scanFile(virtualFile)

        val todoItem = todos.find { it.lineNumber == lineNumber }
        if (todoItem != null) {
            ToggleTodoCompletedAction.toggleCompletion(project, todoItem)
        } else {
            // Fallback direct text replacement if scanItem not indexed yet
            val currentText = comment.text
            val updatedText = currentText.replace(Regex("(?i)(//|#|--|/\\*)\\s*(TODO|FIXME)\\b"), "$1 DONE")
            if (updatedText != currentText) {
                val factory = com.intellij.psi.PsiFileFactory.getInstance(project)
                val dummyFile = factory.createFileFromText("dummy.txt", file.fileType, updatedText)
                val newComment = dummyFile.firstChild
                if (newComment is PsiComment) {
                    comment.replace(newComment)
                }
            }
        }
    }

    override fun startInWriteAction(): Boolean = true
}
