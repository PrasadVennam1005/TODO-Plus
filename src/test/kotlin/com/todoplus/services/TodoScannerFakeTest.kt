package com.todoplus.services

import org.junit.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertTrue

class TodoScannerFakeTest {

    // Simulating the exact path filtration logic inside TodoScannerService
    private fun isPathIgnored(path: String, ignoredDirs: List<String>): Boolean {
        return ignoredDirs.any { ignoredDir ->
            path.contains("/$ignoredDir/") || path.endsWith("/$ignoredDir")
        }
    }

    @Test
    fun `test ignored directories path filtering`() {
        val ignoredDirs = listOf("build", "node_modules", ".idea")

        // Should be ignored
        assertTrue(isPathIgnored("/Users/project/build/classes/main.kt", ignoredDirs))
        assertTrue(isPathIgnored("/Users/project/node_modules/library/index.js", ignoredDirs))
        assertTrue(isPathIgnored("/Users/project/.idea/workspace.xml", ignoredDirs))
        assertTrue(isPathIgnored("/Users/project/build", ignoredDirs))

        // Should NOT be ignored (substring matches that aren't exact directories should be safe)
        assertFalse(isPathIgnored("/Users/project/build_scripts/script.sh", ignoredDirs))
        assertFalse(isPathIgnored("/Users/project/src/my_node_modules.js", ignoredDirs))
        assertFalse(isPathIgnored("/Users/project/src/main.kt", ignoredDirs))
    }
}
