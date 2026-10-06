package com.todoplus.services

import com.intellij.util.messages.Topic
import com.todoplus.models.TodoItem

/**
 * Topic for broadcasting changes to the project TODO cache.
 * Fired when a full project scan finishes or when an incremental file update occurs.
 */
interface TodoChangeListener {
    companion object {
        val TOPIC = Topic.create("TODO++ Cache Updated", TodoChangeListener::class.java)
    }

    /**
     * Called whenever the project TODO cache changes.
     *
     * @param todos The complete, updated list of TODO items across the project.
     */
    fun onTodosUpdated(todos: List<TodoItem>)
}
