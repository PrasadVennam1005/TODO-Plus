package com.todoplus.ui.tree

import com.intellij.util.ui.ColumnInfo
import com.todoplus.models.Priority
import com.todoplus.models.TodoItem
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import javax.swing.tree.DefaultMutableTreeNode

class TodoTreeModelBuilderTest {

    private val columns = arrayOf<ColumnInfo<*, *>>(TodoTreeTableColumns.ItemColumn())
    
    private val todo1 = TodoItem(
        description = "Task 1",
        lineNumber = 1,
        filePath = "/src/test/File1.kt",
        assignee = "john",
        priority = Priority.HIGH,
        category = "bug",
        fullText = "// TODO: Task 1"
    )
    
    private val todo2 = TodoItem(
        description = "Task 2",
        lineNumber = 2,
        filePath = "/src/test/File1.kt",
        vcsAuthor = "jane",
        priority = Priority.LOW,
        category = "feature",
        fullText = "// TODO: Task 2"
    )
    
    private val todo3 = TodoItem(
        description = "Task 3",
        lineNumber = 3,
        filePath = "/src/test/File2.kt",
        assignee = "john",
        vcsAuthor = "john",
        priority = Priority.HIGH,
        fullText = "// TODO: Task 3"
    )
    
    private val todos = listOf(todo1, todo2, todo3)

    @Test
    fun `test buildModel with NONE grouping`() {
        val model = TodoTreeModelBuilder.buildModel(todos, TodoGroupBy.NONE, columns)
        val root = model.root as DefaultMutableTreeNode
        
        assertEquals("Root should have 3 children", 3, root.childCount)
        
        val firstChild = root.getChildAt(0) as DefaultMutableTreeNode
        assertEquals("First child should be todo1", todo1, firstChild.userObject)
    }

    @Test
    fun `test buildModel with FILE grouping`() {
        val model = TodoTreeModelBuilder.buildModel(todos, TodoGroupBy.FILE, columns)
        val root = model.root as DefaultMutableTreeNode
        
        assertEquals("Root should have 2 file groups", 2, root.childCount)
        
        val file1Group = root.getChildAt(0) as DefaultMutableTreeNode
        assertEquals("First group should be File1.kt", "File1.kt", file1Group.userObject)
        assertEquals("File1 should have 2 todos", 2, file1Group.childCount)
        
        val file2Group = root.getChildAt(1) as DefaultMutableTreeNode
        assertEquals("Second group should be File2.kt", "File2.kt", file2Group.userObject)
        assertEquals("File2 should have 1 todo", 1, file2Group.childCount)
    }

    @Test
    fun `test buildModel with ASSIGNEE grouping`() {
        val model = TodoTreeModelBuilder.buildModel(todos, TodoGroupBy.ASSIGNEE, columns)
        val root = model.root as DefaultMutableTreeNode
        
        assertEquals("Root should have 2 assignee groups", 2, root.childCount)
        
        val johnGroup = root.getChildAt(0) as DefaultMutableTreeNode
        assertEquals("First group should be @john", "@john", johnGroup.userObject)
        assertEquals("john should have 2 todos", 2, johnGroup.childCount)
        
        val janeGroup = root.getChildAt(1) as DefaultMutableTreeNode
        assertEquals("Second group should be jane (vcsAuthor)", "jane", janeGroup.userObject)
        assertEquals("jane should have 1 todo", 1, janeGroup.childCount)
    }

    @Test
    fun `test buildModel with PRIORITY grouping`() {
        val model = TodoTreeModelBuilder.buildModel(todos, TodoGroupBy.PRIORITY, columns)
        val root = model.root as DefaultMutableTreeNode
        
        assertEquals("Root should have 2 priority groups", 2, root.childCount)
        
        val highGroup = root.getChildAt(0) as DefaultMutableTreeNode
        assertEquals("First group should be HIGH", "HIGH", highGroup.userObject)
        assertEquals("HIGH should have 2 todos", 2, highGroup.childCount)
        
        val lowGroup = root.getChildAt(1) as DefaultMutableTreeNode
        assertEquals("Second group should be LOW", "LOW", lowGroup.userObject)
        assertEquals("LOW should have 1 todo", 1, lowGroup.childCount)
    }

    @Test
    fun `test buildModel with CATEGORY grouping`() {
        val model = TodoTreeModelBuilder.buildModel(todos, TodoGroupBy.CATEGORY, columns)
        val root = model.root as DefaultMutableTreeNode
        
        assertEquals("Root should have 3 category groups (including Uncategorized)", 3, root.childCount)
        
        val uncatGroup = root.getChildAt(0) as DefaultMutableTreeNode
        assertEquals("First alphabetically is Uncategorized", "Uncategorized", uncatGroup.userObject)
        assertEquals("Uncategorized should have 1 todo (todo3)", 1, uncatGroup.childCount)
        
        val bugGroup = root.getChildAt(1) as DefaultMutableTreeNode
        assertEquals("Second should be bug", "bug", bugGroup.userObject)
        assertEquals("bug should have 1 todo (todo1)", 1, bugGroup.childCount)
        
        val featureGroup = root.getChildAt(2) as DefaultMutableTreeNode
        assertEquals("Third should be feature", "feature", featureGroup.userObject)
        assertEquals("feature should have 1 todo (todo2)", 1, featureGroup.childCount)
    }
}
