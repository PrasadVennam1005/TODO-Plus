package com.todoplus.services

import com.todoplus.models.Priority
import com.todoplus.models.TodoItem
import org.junit.Test
import java.util.concurrent.ConcurrentHashMap
import java.util.concurrent.atomic.AtomicBoolean
import java.util.concurrent.atomic.AtomicInteger
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertTrue

class TodoCacheAndSyncTest {

    // Simulating the thread-safe cache logic inside TodoScannerService
    private class FakeTodoCache {
        private val cache = ConcurrentHashMap<String, List<TodoItem>>()
        private val isCacheInitialized = AtomicBoolean(false)

        fun getCachedTodos(): List<TodoItem> = cache.values.flatten()
        fun isCacheInitialized(): Boolean = isCacheInitialized.get()

        fun setInitialScan(filesWithTodos: Map<String, List<TodoItem>>) {
            cache.clear()
            cache.putAll(filesWithTodos)
            isCacheInitialized.set(true)
        }

        fun updateFileCache(filePath: String, items: List<TodoItem>): List<TodoItem> {
            if (items.isNotEmpty()) {
                cache[filePath] = items
            } else {
                cache.remove(filePath)
            }
            return getCachedTodos()
        }

        fun removeFileFromCache(filePath: String): List<TodoItem> {
            cache.remove(filePath)
            return getCachedTodos()
        }
    }

    private fun createTodo(filePath: String, desc: String): TodoItem {
        return TodoItem(
            description = desc,
            filePath = filePath,
            lineNumber = 10,
            fullText = "// TODO: $desc",
            priority = Priority.HIGH
        )
    }



    @Test
    fun `test cache initialization and flattening`() {
        val cache = FakeTodoCache()
        assertFalse(cache.isCacheInitialized())
        assertTrue(cache.getCachedTodos().isEmpty())

        val initialData = mapOf(
            "/repo/src/FileA.kt" to listOf(createTodo("/repo/src/FileA.kt", "Fix auth")),
            "/repo/src/FileB.kt" to listOf(
                createTodo("/repo/src/FileB.kt", "Add caching"),
                createTodo("/repo/src/FileB.kt", "Update styles")
            )
        )

        cache.setInitialScan(initialData)
        assertTrue(cache.isCacheInitialized())
        assertEquals(3, cache.getCachedTodos().size)
    }

    @Test
    fun `test incremental update of single file in cache`() {
        val cache = FakeTodoCache()
        val fileA = "/repo/src/FileA.kt"
        val fileB = "/repo/src/FileB.kt"

        cache.setInitialScan(mapOf(
            fileA to listOf(createTodo(fileA, "Old task in FileA")),
            fileB to listOf(createTodo(fileB, "Task in FileB"))
        ))

        assertEquals(2, cache.getCachedTodos().size)

        // Incrementally update FileA with 2 new tasks
        val updatedFileAItems = listOf(
            createTodo(fileA, "Revised task 1 in FileA"),
            createTodo(fileA, "New task 2 in FileA")
        )
        val afterUpdate = cache.updateFileCache(fileA, updatedFileAItems)

        // Total should now be 3 (2 from FileA, 1 from FileB)
        assertEquals(3, afterUpdate.size)
        val fileADescriptions = afterUpdate.filter { it.filePath == fileA }.map { it.description }
        assertTrue(fileADescriptions.contains("Revised task 1 in FileA"))
        assertTrue(fileADescriptions.contains("New task 2 in FileA"))
        assertFalse(fileADescriptions.contains("Old task in FileA"))

        // FileB items should remain untouched
        val fileBItems = afterUpdate.filter { it.filePath == fileB }
        assertEquals(1, fileBItems.size)
        assertEquals("Task in FileB", fileBItems[0].description)
    }

    @Test
    fun `test file deletion removes entry from cache`() {
        val cache = FakeTodoCache()
        val fileA = "/repo/src/FileA.kt"
        val fileB = "/repo/src/FileB.kt"

        cache.setInitialScan(mapOf(
            fileA to listOf(createTodo(fileA, "Task 1")),
            fileB to listOf(createTodo(fileB, "Task 2"))
        ))

        assertEquals(2, cache.getCachedTodos().size)

        // Delete FileA
        val afterDelete = cache.removeFileFromCache(fileA)
        assertEquals(1, afterDelete.size)
        assertEquals(fileB, afterDelete[0].filePath)
    }

    @Test
    fun `test incremental update removing all todos from a file clears cache entry`() {
        val cache = FakeTodoCache()
        val fileA = "/repo/src/FileA.kt"

        cache.setInitialScan(mapOf(
            fileA to listOf(createTodo(fileA, "Task 1"))
        ))

        assertEquals(1, cache.getCachedTodos().size)

        // FileA edited so it now has 0 todos
        val afterEmpty = cache.updateFileCache(fileA, emptyList())
        assertEquals(0, afterEmpty.size)
    }


    @Test
    fun `test single-flight scan coalescing simulation`() {
        val isScanRunning = AtomicBoolean(false)
        val pendingRescan = AtomicBoolean(false)
        val executionCounter = AtomicInteger(0)

        fun simulateRequestScan() {
            if (isScanRunning.compareAndSet(false, true)) {
                executionCounter.incrementAndGet()
                // Scan is actively running
            } else {
                // Coalesce into pending slot
                pendingRescan.set(true)
            }
        }

        // 1st request starts scan
        simulateRequestScan()
        assertEquals(1, executionCounter.get())
        assertTrue(isScanRunning.get())
        assertFalse(pendingRescan.get())

        // 2nd request arrives while 1st is running -> coalesces
        simulateRequestScan()
        assertEquals(1, executionCounter.get()) // No new background execution
        assertTrue(pendingRescan.get())

        // 3rd request arrives while 1st is running -> coalesces into pending
        simulateRequestScan()
        assertEquals(1, executionCounter.get()) // Still only 1 execution
        assertTrue(pendingRescan.get())

        // 1st finishes, checks pending and triggers exactly 1 follow-up scan
        isScanRunning.set(false)
        if (pendingRescan.getAndSet(false)) {
            simulateRequestScan()
        }

        assertEquals(2, executionCounter.get()) // Exactly 2 total executions for 3 rapid triggers
        assertTrue(isScanRunning.get())
        assertFalse(pendingRescan.get())
    }
}
