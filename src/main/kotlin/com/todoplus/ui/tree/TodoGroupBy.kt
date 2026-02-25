package com.todoplus.ui.tree

enum class TodoGroupBy(val displayName: String) {
    NONE("No Grouping"),
    FILE("File"),
    ASSIGNEE("Person"),
    PRIORITY("Priority"),
    CATEGORY("Category")
}
