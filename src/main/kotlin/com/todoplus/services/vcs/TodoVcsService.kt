package com.todoplus.services.vcs

import com.intellij.openapi.components.Service
import com.intellij.openapi.diagnostic.Logger
import com.intellij.openapi.project.Project
import com.intellij.openapi.vcs.ProjectLevelVcsManager
import com.intellij.openapi.vcs.annotate.FileAnnotation
import com.intellij.openapi.vfs.VirtualFile
import com.intellij.vcsUtil.VcsUtil
import com.todoplus.models.TodoItem
import java.util.Date
import java.util.concurrent.ConcurrentHashMap

@Service(Service.Level.PROJECT)
class TodoVcsService(private val project: Project) {
    private val logger = Logger.getInstance(TodoVcsService::class.java)

    // Cache: VirtualFilePath -> (CacheKey -> Annotation)
    // CacheKey -> File modification timestamp to invalidate when file changes
    private data class VcsCacheEntry(val timestamp: Long, val annotation: FileAnnotation)
    private val annotationCache = ConcurrentHashMap<String, VcsCacheEntry>()

    fun getVcsDataForTodo(todo: TodoItem, virtualFile: VirtualFile) {
        try {
            val vcsManager = ProjectLevelVcsManager.getInstance(project)
            val vcs = vcsManager.getVcsFor(virtualFile) ?: return

            val annotationProvider = vcs.annotationProvider ?: return
            
            // Check Cache
            val filePath = virtualFile.path
            val currentTimestamp = virtualFile.timeStamp
            var annotation = annotationCache[filePath]?.let { 
                if (it.timestamp == currentTimestamp) it.annotation else null 
            }

            // Fetch if not cached
            if (annotation == null) {
                annotation = annotationProvider.annotate(virtualFile)
                annotation.let {
                    annotationCache[filePath] = VcsCacheEntry(currentTimestamp, it)
                }
            }

            // Extract Data
            annotation.let {
                // Annotations are 0-indexed, while our TodoItem lineNumber is 1-indexed (or vice versa, needs validation)
                // In IntelliJ, document lines are 0-indexed. TodoItem lines are typically 1-indexed in UI but let's check Scanner.
                // Assuming TodoItem.lineNumber is 1-indexed based on typical UI display, so annotation needs - 1
                val docLine = (todo.lineNumber - 1).coerceAtLeast(0)

                // Some annotation providers might not have data for all lines
                if (docLine < annotation.lineCount) {
                    val revision = annotation.getLineRevisionNumber(docLine)
                    if (revision != null) {
                        // Extract author precisely using VcsFileRevision if available
                        val fileRevision = annotation.revisions?.find { it.revisionNumber == revision }
                        var authorStr = fileRevision?.author

                        // Fallback to tooltip regex parsing if needed
                        if (authorStr.isNullOrBlank()) {
                            val tooltip = annotation.getToolTip(docLine) ?: ""
                            val authorMatch = Regex("Author:\\s+([^<\\n]+)").find(tooltip)
                            if (authorMatch != null) {
                                authorStr = authorMatch.groupValues[1].trim()
                            } else {
                                // Last resort fallback (avoid returning "commit", find first word if no Author tag)
                                authorStr = tooltip.split("\\s+".toRegex()).firstOrNull { it.isNotBlank() && it.lowercase() != "commit" } ?: "Unknown"
                            }
                        }

                        todo.vcsAuthor = authorStr
                        todo.vcsDate = annotation.getLineDate(docLine)
                    } else {
                        todo.vcsAuthor = "Uncommitted"
                        todo.vcsDate = Date()
                    }
                } else {
                    todo.vcsAuthor = "Uncommitted"
                    todo.vcsDate = Date()
                }
            }
        } catch (e: Exception) {
            logger.warn("Failed to fetch VCS data for ${virtualFile.name}", e)
        }
    }
    
    fun clearCache() {
        annotationCache.clear()
    }
}
