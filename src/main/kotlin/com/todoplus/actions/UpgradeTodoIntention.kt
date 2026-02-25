package com.todoplus.actions

import com.intellij.codeInsight.intention.IntentionAction
import com.intellij.openapi.editor.Editor
import com.intellij.openapi.project.Project
import com.intellij.psi.PsiComment
import com.intellij.psi.PsiElement
import com.intellij.psi.PsiFile
import com.intellij.util.IncorrectOperationException

class UpgradeTodoIntention : IntentionAction {

    override fun getText(): String = "Upgrade to TODO++ format"

    override fun getFamilyName(): String = "TODO++"

    override fun isAvailable(project: Project, editor: Editor?, file: PsiFile?): Boolean {
        if (editor == null || file == null) return false
        
        val element = file.findElementAt(editor.caretModel.offset) ?: return false
        return isBasicTodoComment(element)
    }

    private fun isBasicTodoComment(element: PsiElement): Boolean {
        if (element !is PsiComment) return false
        
        val text = element.text
        // Check if it looks like a TODO but missing the metadata block
        val isTodo = text.contains("TODO", ignoreCase = true) || text.contains("FIXME", ignoreCase = true)
        val hasMetadata = text.contains("(") && text.contains(")")
        
        return isTodo && !hasMetadata
    }

    @Throws(IncorrectOperationException::class)
    override fun invoke(project: Project, editor: Editor?, file: PsiFile?) {
        if (editor == null || file == null) return
        
        val element = file.findElementAt(editor.caretModel.offset) as? PsiComment ?: return
        val currentText = element.text
        
        // Find the "TODO" prefix
        val todoRegex = Regex("(?i)(//|#|--)\\s*(TODO|FIXME)\\b:?\\s*")
        val match = todoRegex.find(currentText)
        
        if (match != null) {
            val description = currentText.substring(match.range.last + 1).trim()
            val commentStyle = match.groupValues[1]
            val keyword = match.groupValues[2].uppercase()
            
            // Generate the new text
            val newText = "$commentStyle $keyword(@user priority:CRITICAL): $description"
            
            // Replace the comment
            val factory = com.intellij.psi.PsiFileFactory.getInstance(project)
            val dummyFile = factory.createFileFromText("dummy.txt", file.fileType, newText)
            val newComment = dummyFile.firstChild
            
            if (newComment is PsiComment) {
                element.replace(newComment)
            }
        }
    }

    override fun startInWriteAction(): Boolean = true
}
