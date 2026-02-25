package com.todoplus.ui.tree

import com.intellij.ui.treeStructure.treetable.ListTreeTableModelOnColumns
import com.todoplus.models.TodoItem
import javax.swing.tree.DefaultMutableTreeNode

object TodoTreeModelBuilder {

    fun buildModel(todos: List<TodoItem>, groupBy: TodoGroupBy, columns: Array<com.intellij.util.ui.ColumnInfo<*, *>>): ListTreeTableModelOnColumns {
        val root = DefaultMutableTreeNode("Root")
        
        if (todos.isEmpty()) {
            return ListTreeTableModelOnColumns(root, columns)
        }

        when (groupBy) {
            TodoGroupBy.NONE -> {
                todos.forEach { root.add(DefaultMutableTreeNode(it)) }
            }
            TodoGroupBy.FILE -> {
                val grouped = todos.groupBy { it.getFileName() }
                grouped.toSortedMap().forEach { (file, fileTodos) ->
                    val groupNode = DefaultMutableTreeNode(file)
                    fileTodos.forEach { groupNode.add(DefaultMutableTreeNode(it)) }
                    root.add(groupNode)
                }
            }
            TodoGroupBy.ASSIGNEE -> {
                val grouped = todos.groupBy { 
                    when {
                        it.assignee != null -> "@${it.assignee}"
                        it.vcsAuthor != null -> it.vcsAuthor
                        else -> "Unassigned"
                    }
                }
                grouped.toSortedMap(compareBy { it?.lowercase() ?: "" }).forEach { (assignee, userTodos) ->
                    val groupNode = DefaultMutableTreeNode(assignee ?: "Unassigned")
                    userTodos.forEach { groupNode.add(DefaultMutableTreeNode(it)) }
                    root.add(groupNode)
                }
            }
            TodoGroupBy.PRIORITY -> {
                val grouped = todos.groupBy { it.priority?.name ?: "None" }
                // Custom sort for priorities would be ideal, but alphabetical is fine for grouping keys initially
                grouped.toSortedMap().forEach { (priority, prioTodos) ->
                    val groupNode = DefaultMutableTreeNode(priority)
                    prioTodos.forEach { groupNode.add(DefaultMutableTreeNode(it)) }
                    root.add(groupNode)
                }
            }
            TodoGroupBy.CATEGORY -> {
                val grouped = todos.groupBy { it.category ?: "Uncategorized" }
                grouped.toSortedMap().forEach { (category, catTodos) ->
                    val groupNode = DefaultMutableTreeNode(category)
                    catTodos.forEach { groupNode.add(DefaultMutableTreeNode(it)) }
                    root.add(groupNode)
                }
            }
        }

        return ListTreeTableModelOnColumns(root, columns)
    }
}
