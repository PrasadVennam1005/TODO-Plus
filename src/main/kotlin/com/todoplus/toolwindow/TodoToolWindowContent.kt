package com.todoplus.toolwindow

import com.intellij.ide.BrowserUtil
import com.intellij.notification.NotificationGroupManager
import com.intellij.notification.NotificationType
import com.intellij.openapi.application.ApplicationManager
import com.intellij.openapi.components.service
import com.intellij.openapi.fileChooser.FileChooserFactory
import com.intellij.openapi.fileChooser.FileSaverDescriptor
import com.intellij.openapi.fileEditor.FileEditorManager
import com.intellij.openapi.fileEditor.OpenFileDescriptor
import com.intellij.openapi.progress.ProgressIndicator
import com.intellij.openapi.progress.ProgressManager
import com.intellij.openapi.progress.Task
import com.intellij.openapi.project.Project
import com.intellij.openapi.vfs.LocalFileSystem
import com.intellij.openapi.vfs.VirtualFile
import com.intellij.openapi.vfs.VirtualFileManager
import com.intellij.openapi.vfs.newvfs.BulkFileListener
import com.intellij.openapi.vfs.newvfs.events.VFileEvent
import com.intellij.ui.Gray
import com.intellij.ui.components.JBLabel
import com.intellij.ui.components.JBPanel
import com.intellij.ui.components.JBScrollPane
import com.intellij.ui.components.JBTextField
import com.intellij.ui.dualView.TreeTableView
import com.intellij.util.messages.MessageBusConnection
import com.intellij.util.ui.tree.TreeUtil
import com.intellij.openapi.actionSystem.*
import com.intellij.icons.AllIcons
import com.intellij.openapi.ui.SimpleToolWindowPanel
import com.intellij.ui.SearchTextField
import com.intellij.util.ui.JBUI
import com.todoplus.exporter.TodoExporter
import com.todoplus.models.TodoItem
import com.todoplus.services.TodoScannerService
import com.todoplus.ui.tree.TodoGroupBy
import com.todoplus.ui.tree.TodoTreeModelBuilder
import com.todoplus.ui.tree.TodoTreeTableColumns
import java.awt.*
import java.awt.event.MouseAdapter
import java.awt.event.MouseEvent
import javax.swing.*

import javax.swing.tree.DefaultMutableTreeNode

/**
 * Content panel for the TODO++ tool window
 */
class TodoToolWindowContent(private val project: Project) {

        private val treeTable: TreeTableView
    private val groupByDropdown = JComboBox(TodoGroupBy.entries.toTypedArray())
    private val mainPanel: SimpleToolWindowPanel
    private val statusLabel: JBLabel
    private val allTodos = mutableListOf<TodoItem>()
    private val filteredTodos = mutableListOf<TodoItem>()
    
    // Filter controls
    private val priorityFilter = JComboBox<String>()
    private val assigneeFilter = JBTextField()
    private val categoryFilter = JBTextField()
    private val searchField = SearchTextField()
    
    // Scope control
    private val scopeDropdown = JComboBox(arrayOf("Project", "Current File"))

        init {
        treeTable = TreeTableView(TodoTreeModelBuilder.buildModel(emptyList(), TodoGroupBy.NONE, TodoTreeTableColumns.createColumns())).apply {
            setDefaultEditor(Any::class.java, null)
            setSelectionMode(ListSelectionModel.SINGLE_SELECTION)
            
            // Custom renderers are now handled by TodoColumnInfo descriptors
            
            // Add click listener for navigation and context menu
            addMouseListener(object : MouseAdapter() {
                override fun mouseClicked(e: MouseEvent) {
                    if (e.clickCount == 2) {
                        navigateToSelectedTodo()
                    }
                    if (SwingUtilities.isRightMouseButton(e)) {
                        val row = rowAtPoint(e.point)
                        if (row >= 0 && row < rowCount) {
                            setRowSelectionInterval(row, row)
                            val node = tree.getPathForRow(row)?.lastPathComponent as? DefaultMutableTreeNode
                            val todo = node?.userObject as? TodoItem
                            if (todo != null) showContextMenu(e, todo)
                        }
                    }
                }
            })
            
            // Premium Tree Settings
            tree.isRootVisible = false
            tree.showsRootHandles = true
        }

        // Configure empty text
        treeTable.emptyText.text = "No TODOs found. Click 'Scan Project' to begin."
        
        // Create filter panel
        val filterPanel = createFilterPanel()

        // Create Action Toolbar (Premium UI)
        val actionGroup = DefaultActionGroup().apply {
            add(object : AnAction("Refresh", "Refresh TODO list from existing scan", AllIcons.Actions.Refresh) {
                override fun actionPerformed(e: AnActionEvent) { performScan() }
            })
            add(object : AnAction("Scan", "Scan selected scope for TODOs", AllIcons.Actions.Find) {
                override fun actionPerformed(e: AnActionEvent) { performScan() }
            })
            addSeparator()
            add(object : AnAction("Export CSV", "Export TODOs to CSV file", AllIcons.ToolbarDecorator.Export) {
                override fun actionPerformed(e: AnActionEvent) { exportTodos("csv") }
            })
            add(object : AnAction("Export Markdown", "Export TODOs to Markdown file", AllIcons.FileTypes.Text) {
                override fun actionPerformed(e: AnActionEvent) { exportTodos("md") }
            })
            add(object : AnAction("Export HTML Dashboard", "Generate and open HTML Dashboard", AllIcons.FileTypes.Html) {
                override fun actionPerformed(e: AnActionEvent) { exportTodos("html") }
            })
            addSeparator()
            add(object : AnAction("Clear Filters", "Clear all search filters", AllIcons.Actions.GC) {
                override fun actionPerformed(e: AnActionEvent) { clearFilters() }
            })
            addSeparator()
            add(object : AnAction("Expand All", "Expand all groups", AllIcons.Actions.Expandall) {
                override fun actionPerformed(e: AnActionEvent) { TreeUtil.expandAll(treeTable.tree) }
            })
            add(object : AnAction("Collapse All", "Collapse all groups", AllIcons.Actions.Collapseall) {
                override fun actionPerformed(e: AnActionEvent) { TreeUtil.collapseAll(treeTable.tree, 0) }
            })
        }
        
        val actionToolbar = ActionManager.getInstance().createActionToolbar("TodoPlusToolbar", actionGroup, true)
        actionToolbar.targetComponent = treeTable

        statusLabel = JBLabel(" Ready. ")
        statusLabel.border = JBUI.Borders.empty(2, 5)
        statusLabel.foreground = Gray._120

        // Create main panel using SimpleToolWindowPanel
        mainPanel = SimpleToolWindowPanel(true, true).apply {
            val topPanel = JBPanel<JBPanel<*>>(BorderLayout()).apply {
                border = JBUI.Borders.customLineBottom(Gray._200)
                add(actionToolbar.component, BorderLayout.WEST)
                add(filterPanel, BorderLayout.CENTER)
            }
            
            toolbar = topPanel
            setContent(JBScrollPane(treeTable as Component))
            
            val bottomPanel = JBPanel<JBPanel<*>>(BorderLayout()).apply {
                border = JBUI.Borders.customLineTop(Gray._200)
                add(statusLabel, BorderLayout.WEST)
            }
            add(bottomPanel, BorderLayout.SOUTH)
        }
        
        // Auto-refresh on file save
        setupAutoRefresh()
        
        // Populate priority filter
        updatePriorityFilter()
    }
    
    private fun updatePriorityFilter() {
        val currentSelection = priorityFilter.selectedItem as? String
        priorityFilter.removeAllItems()
        priorityFilter.addItem("All Priorities")
        

        
        val settings = com.todoplus.settings.TodoSettingsService.getInstance()
        settings.getPriorities().forEach { 
            priorityFilter.addItem(it.name) 
        }
        priorityFilter.addItem("None")
        
        if (currentSelection != null) {
            // Restore selection if possible, or default to All
            // We need to check if the selection still exists in the model
             var found = false
             for (i in 0 until priorityFilter.itemCount) {
                 if (priorityFilter.getItemAt(i) == currentSelection) {
                     priorityFilter.selectedItem = currentSelection
                     found = true
                     break
                 }
             }
             if (!found) priorityFilter.selectedIndex = 0
        } else {
             priorityFilter.selectedIndex = 0
        }
    }

    private fun createFilterPanel(): JPanel {
        val panel = JPanel(FlowLayout(FlowLayout.RIGHT, 10, 2)).apply {
            border = JBUI.Borders.empty(2, 5)
        }

        // Group By dropdown
        panel.add(JLabel("Group By:"))
        groupByDropdown.addActionListener { applyFilters() }
        panel.add(groupByDropdown)

        // Scope dropdown
        panel.add(JLabel("Scope:"))
        scopeDropdown.addActionListener { performScan() }
        panel.add(scopeDropdown)

        // Priority filter
        panel.add(JLabel("Priority:"))
        priorityFilter.addActionListener { applyFilters() }
        panel.add(priorityFilter)

        // Assignee filter
        panel.add(JLabel("Person:"))
        assigneeFilter.columns = 8
        assigneeFilter.toolTipText = "Author/Assignee..."
        panel.add(assigneeFilter)

        // Category filter
        panel.add(JLabel("Category:"))
        categoryFilter.columns = 8
        categoryFilter.toolTipText = "Bug/Feature..."
        panel.add(categoryFilter)
        
        // Search text field (Premium UI)
        searchField.toolTipText = "Search description..."
        panel.add(searchField)
        
        // Apply filter on enter key
        val applyAction = java.awt.event.ActionListener { applyFilters() }
        assigneeFilter.addActionListener(applyAction)
        categoryFilter.addActionListener(applyAction)
        
        // Add listener to search field's text editor
        searchField.textEditor.addActionListener(applyAction)

        val filterButton = JButton("Apply").apply {
            addActionListener(applyAction)
            isOpaque = false
        }
        panel.add(filterButton)

        return panel
    }

    fun getContent(): JComponent = mainPanel

    private fun performScan() {
        if (scopeDropdown.selectedItem == "Current File") {
            scanCurrentFile()
        } else {
            scanProject()
        }
    }

    private fun scanCurrentFile() {
        val editor = FileEditorManager.getInstance(project).selectedTextEditor
        val document = editor?.document
        val file = document?.let { com.intellij.openapi.fileEditor.FileDocumentManager.getInstance().getFile(it) }
        
        if (file == null) {
            statusLabel.text = "No active file selected."
            return
        }
        
        ProgressManager.getInstance().run(object : Task.Backgroundable(project, "Scanning Current File", true) {
            override fun run(indicator: ProgressIndicator) {
                indicator.text = "Scanning ${file.name}..."
                indicator.isIndeterminate = true
                
                try {
                    val scanner = project.service<TodoScannerService>()
                    val foundTodos = scanner.scanFile(file)
                    
                    ApplicationManager.getApplication().invokeLater {
                        allTodos.clear()
                        allTodos.addAll(foundTodos)
                        
                        applyFilters()
                        updateStatistics()
                    }
                } catch (e: Exception) {
                    ApplicationManager.getApplication().invokeLater {
                        statusLabel.text = "Error scanning file: ${e.message}"
                    }
                }
            }
        })
    }

    private fun scanProject() {
        ProgressManager.getInstance().run(object : Task.Backgroundable(project, "Scanning for TODOs", true) {
            override fun run(indicator: ProgressIndicator) {
                // Background thread
                indicator.text = "Scanning project files..."
                indicator.isIndeterminate = true
                
                try {
                    val scanner = project.service<TodoScannerService>()
                    // Run scanning (service handles Read Actions internally)
                    val foundTodos = scanner.scanProject()
                    
                    // Update UI on EDT
                    ApplicationManager.getApplication().invokeLater {
                        allTodos.clear()
                        allTodos.addAll(foundTodos)
                        
                        applyFilters()
                        updateStatistics()
                    }
                } catch (e: Exception) {
                    ApplicationManager.getApplication().invokeLater {
                        statusLabel.text = "Error scanning project: ${e.message}"
                    }
                }
            }
        })
    }

    private fun applyFilters() {
        filteredTodos.clear()
        val priorityFilterValue = priorityFilter.selectedItem as String
        val assigneeText = assigneeFilter.text.trim().removePrefix("@").lowercase()
        val categoryText = categoryFilter.text.trim().lowercase()
        val searchText = searchField.text.trim().lowercase()
        
        allTodos.forEach { todo ->
            var matches = true
            
            // Priority filter
            if (priorityFilterValue != "All Priorities") {
                matches = when (priorityFilterValue) {
                    "None" -> todo.priority == null
                    else -> todo.priority?.name == priorityFilterValue
                }
            }
            
            // Assignee filter
            if (matches && assigneeText.isNotEmpty()) {
                val hasAssigneeMatch = todo.assignee?.lowercase()?.contains(assigneeText) == true
                val hasAuthorMatch = todo.vcsAuthor?.lowercase()?.contains(assigneeText) == true
                matches = hasAssigneeMatch || hasAuthorMatch
            }
            
            // Category filter
            if (matches && categoryText.isNotEmpty()) {
                matches = todo.category?.lowercase()?.contains(categoryText) == true
            }
            
            // Search filter
            if (matches && searchText.isNotEmpty()) {
                if (searchText.contains(":")) {
                   // key:value search
                   val parts = searchText.split(":")
                   if (parts.size >= 2) {
                       val key = parts[0].trim()
                       val value = parts[1].trim()
                       
                        matches = when(key) {
                            "priority" -> todo.priority?.name?.lowercase() == value
                            "assignee", "assigned", "author" -> {
                                val hasAssignee = todo.assignee?.lowercase()?.contains(value) == true
                                val hasAuthor = todo.vcsAuthor?.lowercase()?.contains(value) == true
                                hasAssignee || hasAuthor
                            }
                            "category" -> todo.category?.lowercase()?.contains(value) == true
                            else -> todo.tags[key]?.lowercase()?.contains(value) == true
                        }
                   }
                } else {
                    matches = todo.description.lowercase().contains(searchText)
                }
            }
            
            if (matches) {
                filteredTodos.add(todo)
            }
        }
        
                
        val newModel = TodoTreeModelBuilder.buildModel(filteredTodos, groupByDropdown.selectedItem as TodoGroupBy, TodoTreeTableColumns.createColumns())
        treeTable.setModel(newModel)
        TreeUtil.expandAll(treeTable.tree)
        
        // Adjust widths after setting model
        val cols = treeTable.columnModel
        if (cols.columnCount >= 11) {
            cols.getColumn(0).preferredWidth = 350 // Item/Group
            cols.getColumn(1).preferredWidth = 80  // Priority
            cols.getColumn(2).preferredWidth = 100 // Author
            cols.getColumn(3).preferredWidth = 100 // Assignee
            cols.getColumn(4).preferredWidth = 100 // Category
            cols.getColumn(5).preferredWidth = 100 // Issue
            cols.getColumn(6).preferredWidth = 100 // Date
            cols.getColumn(7).preferredWidth = 100 // Due Date
            cols.getColumn(8).preferredWidth = 150 // Tags
            cols.getColumn(9).preferredWidth = 200 // File
            cols.getColumn(10).preferredWidth = 50  // Line
        }
        
        updateStatistics()
    }
    
    private fun updateStatistics() {
        val count = filteredTodos.size
        // Calculate counts for each configured priority
        val priorityCounts = mutableMapOf<String, Int>()
        val settings = com.todoplus.settings.TodoSettingsService.getInstance()
        
        settings.getPriorities().forEach { config ->
            val pCount = filteredTodos.count { it.priority?.name == config.name }
            if (pCount > 0) {
                priorityCounts[config.name] = pCount
            }
        }
        
        val missingPriority = filteredTodos.count { it.priority == null }
        val missingAssignee = filteredTodos.count { it.assignee == null }
        
        val sb = StringBuilder("Found $count TODOs")
        if (count > 0) {
            if (priorityCounts.isNotEmpty()) {
                val stats = priorityCounts.map { "${it.value} ${it.key.lowercase()}" }.joinToString(", ")
                sb.append(" ($stats)")
            }
            
            if (missingPriority > 0 || missingAssignee > 0) {
                sb.append(" | ⚠️ ")
                val warnings = mutableListOf<String>()
                if (missingPriority > 0) warnings.add("$missingPriority need priority")
                if (missingAssignee > 0) warnings.add("$missingAssignee unassigned")
                sb.append(warnings.joinToString(", "))
            }
        }
        
        statusLabel.text = sb.toString()
    }
    
    private fun clearFilters() {
        groupByDropdown.selectedIndex = 0
        priorityFilter.selectedIndex = 0
        assigneeFilter.text = ""
        categoryFilter.text = ""
        searchField.text = ""
        applyFilters()
    }
    
    private fun refreshTodos() {
        performScan()
    }
    
    private fun showContextMenu(e: java.awt.event.MouseEvent, todo: TodoItem) {
        val popup = JPopupMenu()
        
        // Navigate
        val navigateItem = JMenuItem("Navigate to Code")
        navigateItem.addActionListener { navigateTo(todo) }
        popup.add(navigateItem)
        
        // Open Issue Tracker
        val issueId = todo.issueId
        val settings = com.todoplus.settings.TodoSettingsService.getInstance().getState()
        val template = settings.issueUrlTemplate
        
        if (issueId != null && template.isNotEmpty() && template.contains("{id}")) {
            val openIssueItem = JMenuItem("Open Issue $issueId")
            openIssueItem.addActionListener {
                val url = template.replace("{id}", issueId)
                try {
                    BrowserUtil.browse(url)
                } catch (ex: Exception) {
                    // Ignore browser errors
                }
            }
            popup.add(openIssueItem)
        }
        
        popup.show(e.component, e.x, e.y)
    }

    private fun navigateTo(todo: TodoItem?) {
        if (todo == null) return
        val virtualFile = LocalFileSystem.getInstance().findFileByPath(todo.filePath)
        if (virtualFile != null) {
            // Navigate to file and line
            FileEditorManager.getInstance(project).openTextEditor(
                OpenFileDescriptor(project, virtualFile, todo.lineNumber - 1, 0),
                true
            )
        }
    }
    
    private fun navigateToSelectedTodo() {
        val selectedRow = treeTable.selectedRow
        if (selectedRow != -1) {
            val node = treeTable.tree.getPathForRow(selectedRow)?.lastPathComponent as? DefaultMutableTreeNode
            val todo = node?.userObject as? TodoItem
            if (todo != null) navigateTo(todo)
        }
    }
    

    
    /**
     * Export TODOs to file
     */
    private fun exportTodos(format: String) {
        // Get TODOs to export (filtered or all)
        val todosToExport = filteredTodos.ifEmpty { allTodos }
        
        if (todosToExport.isEmpty()) {
            NotificationGroupManager.getInstance()
                .getNotificationGroup("TODO++ Notifications")
                .createNotification("Nothing to export", "No TODO items found.", NotificationType.WARNING)
                .notify(project)
            return
        }
        
        // Create file descriptor
        val descriptor = FileSaverDescriptor(
            "Export TODOs",
            "Save TODO list as ${format.uppercase()}",
            format
        )
        
        // Show file chooser
        val fileSaver = FileChooserFactory.getInstance().createSaveFileDialog(descriptor, project)
        val virtualFileWrapper = fileSaver.save(null as VirtualFile?, "todos.$format") ?: return
        
        try {
            val file = virtualFileWrapper.file
            val content = when (format) {
                "csv" -> TodoExporter().exportToCsv(todosToExport)
                "md" -> TodoExporter().exportToMarkdown(todosToExport)
                "html" -> TodoExporter().exportToHtml(todosToExport)
                else -> ""
            }
            
            file.writeText(content)
            
            // Auto-open HTML Dashboard
            if (format == "html") {
                BrowserUtil.browse(file.toURI().toString())
            }
            
            NotificationGroupManager.getInstance()
                .getNotificationGroup("TODO++ Notifications")
                .createNotification("Export Successful", "Saved ${todosToExport.size} TODOs to ${file.name}", NotificationType.INFORMATION)
                .notify(project)
                
        } catch (e: Exception) {
            NotificationGroupManager.getInstance()
                .getNotificationGroup("TODO++ Notifications")
                .createNotification("Export Failed", e.message ?: "Unknown error", NotificationType.ERROR)
                .notify(project)
        }
    }
    
    /**
     * Setup auto-refresh on file save
     */
    private fun setupAutoRefresh() {
        val connection: MessageBusConnection = project.messageBus.connect()
        
        // Debounce timer to prevent rapid refreshes
        val refreshTimer = Timer(500) {
            ApplicationManager.getApplication().invokeLater {
                performScan()
            }
        }
        refreshTimer.isRepeats = false
        
        connection.subscribe(VirtualFileManager.VFS_CHANGES, object : BulkFileListener {
            override fun after(events: List<VFileEvent>) {
                // Check if any relevant files were changed
                val relevantChanges = events.any { event ->
                    val file = event.file
                    file != null && !file.isDirectory && isValidFileType(file.name)
                }
                
                if (relevantChanges) {
                    refreshTimer.restart()
                }
            }
        })
    }
    
    private fun isValidFileType(fileName: String): Boolean {
        val extensions = listOf("kt", "java", "xml", "js", "ts", "py", "go", "rs", "cpp", "c", "h", "cs", "swift", "rb", "php", "scala", "groovy")
        return extensions.any { fileName.endsWith(".$it", ignoreCase = true) }
    }
}
