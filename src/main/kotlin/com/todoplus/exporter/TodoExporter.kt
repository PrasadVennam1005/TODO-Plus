package com.todoplus.exporter

import com.todoplus.models.TodoItem

/**
 * Service to export TODO items to different formats
 */
class TodoExporter {

    /**
     * Export TODOs to CSV format
     */
    /**
     * Export TODOs to CSV format
     */
    fun exportToCsv(todos: List<TodoItem>): String {
        val sb = StringBuilder()
        // Header
        sb.append("Status,Priority,Due Date,Assignee,Category,Description,File,Line\n")
        
        // Data
        for (todo in todos) {
            sb.append(if (todo.isCompleted) "Done" else "Todo").append(",")
            sb.append(escapeCsv(todo.priority?.name ?: "")).append(",")
            sb.append(escapeCsv(todo.dueDate?.toString() ?: "")).append(",")
            sb.append(escapeCsv(todo.assignee ?: "")).append(",")
            sb.append(escapeCsv(todo.category ?: "")).append(",")
            sb.append(escapeCsv(todo.description)).append(",")
            sb.append(escapeCsv(todo.filePath)).append(",")
            sb.append(todo.lineNumber).append("\n")
        }
        
        return sb.toString()
    }

    /**
     * Export TODOs to Markdown format
     */
    fun exportToMarkdown(todos: List<TodoItem>): String {
        val sb = StringBuilder()
        sb.append("# TODO List Export\n\n")
        
        // Group by priority
        val byPriority = todos.groupBy { it.priority }
        
        // Get sorted priorities from settings to ensure order
        val settings = com.todoplus.settings.TodoSettingsService.getInstance()
        val sortedPriorities = settings.getPriorities().map { com.todoplus.models.Priority(it.name) }
        
        // Export known priorities in order
        for (priority in sortedPriorities) {
            val items = byPriority[priority]
            if (!items.isNullOrEmpty()) {
                val pName = priority.name
                val icon = when(pName.uppercase()) {
                    "HIGH", "CRITICAL" -> "🔴"
                    "MEDIUM" -> "🟠"
                    "LOW" -> "🟢"
                    else -> "⚪"
                }
                appendMarkdownSection(sb, "$icon $pName Priority", items)
            }
        }
        
        // Export priorities NOT in settings (e.g. if settings changed but todos exist)
        val unknownPriorities = byPriority.keys.filterNotNull().filter { it !in sortedPriorities }
        for (priority in unknownPriorities) {
             appendMarkdownSection(sb, "⚪ ${priority.name} Priority", byPriority[priority])
        }
        
        // No Priority
        appendMarkdownSection(sb, "⚪ No Priority", byPriority[null])
        
        return sb.toString()
    }
    
    private fun appendMarkdownSection(sb: StringBuilder, title: String, items: List<TodoItem>?) {
        if (items.isNullOrEmpty()) return
        
        sb.append("## $title\n\n")
        for (todo in items) {
            val checkbox = if (todo.isCompleted) "[x]" else "[ ]"
            val assigneeStr = if (todo.assignee != null) "**@${todo.assignee}** " else ""
            val categoryStr = if (todo.category != null) "[${todo.category}] " else ""
            val dueStr = if (todo.dueDate != null) "📅 ${todo.dueDate} " else ""
            
            sb.append("- $checkbox $dueStr$assigneeStr$categoryStr${todo.description} (`${todo.getFileName()}:${todo.lineNumber}`)\n")
        }
        sb.append("\n")
    }
    
    private fun escapeCsv(value: String): String {
        var result = value
        if (result.contains(",") || result.contains("\"") || result.contains("\n")) {
            result = result.replace("\"", "\"\"")
            result = "\"$result\""
        }
        return result
    }

    /**
     * Export TODOs to a beautiful HTML Dashboard.
     *
     * @param todos   the items to render
     * @param config  optional user customisations; defaults to [HtmlExportConfig.default]
     *                which produces the identical output as before this change.
     */
    @JvmOverloads
    fun exportToHtml(
        todos: List<TodoItem>,
        config: HtmlExportConfig = HtmlExportConfig.default()
    ): String {
        val criticalCount = todos.count { it.priority?.name?.uppercase() == "CRITICAL" }
        val highCount     = todos.count { it.priority?.name?.uppercase() == "HIGH" }
        val medCount      = todos.count { it.priority?.name?.uppercase() == "MEDIUM" }
        val lowCount      = todos.count { it.priority?.name?.uppercase() == "LOW" }

        val title = config.pageTitle.ifBlank { "TODO++ Dashboard" }

        // ── Section 1: <head> + stats cards ─────────────────────────────────
        val sb = StringBuilder()
        sb.append("""
            <!DOCTYPE html>
            <html lang="en">
            <head>
                <meta charset="UTF-8">
                <meta name="viewport" content="width=device-width, initial-scale=1.0">
                <title>$title</title>
                <style>
                    :root {
                        --bg-main: #0d1117;
                        --bg-card: #161b22;
                        --text-main: #c9d1d9;
                        --border-color: #30363d;
                        --primary: #58a6ff;
                        --critical: #8957e5;
                        --high: #f85149;
                        --medium: #d29922;
                        --low: #3fb950;
                    }
                    body {
                        font-family: -apple-system, BlinkMacSystemFont, "Segoe UI", Helvetica, Arial, sans-serif;
                        background-color: var(--bg-main);
                        color: var(--text-main);
                        line-height: 1.5;
                        margin: 0;
                        padding: 40px;
                    }
                    .container { max-width: 1200px; margin: 0 auto; }
                    .header {
                        display: flex;
                        justify-content: space-between;
                        align-items: center;
                        margin-bottom: 30px;
                        border-bottom: 1px solid var(--border-color);
                        padding-bottom: 20px;
                    }
                    .header h1 { margin: 0; color: white; }
                    .stats { display: flex; gap: 20px; margin-bottom: 40px; }
                    .stat-card {
                        background: var(--bg-card);
                        border: 1px solid var(--border-color);
                        border-radius: 6px;
                        padding: 20px;
                        flex: 1;
                        text-align: center;
                        box-shadow: 0 4px 6px rgba(0,0,0,0.1);
                    }
                    .stat-value { font-size: 32px; font-weight: bold; margin-bottom: 5px; }
                    .stat-label { font-size: 14px; color: #8b949e; text-transform: uppercase; }
                    .stat-CRITICAL { color: var(--critical); }
                    .stat-HIGH     { color: var(--high); }
                    .stat-MEDIUM   { color: var(--medium); }
                    .stat-LOW      { color: var(--low); }

                    table {
                        width: 100%;
                        border-collapse: collapse;
                        background: var(--bg-card);
                        border-radius: 6px;
                        overflow: hidden;
                        border: 1px solid var(--border-color);
                        box-shadow: 0 4px 6px rgba(0,0,0,0.1);
                    }
                    th, td { padding: 12px 16px; text-align: left; border-bottom: 1px solid var(--border-color); }
                    th { background-color: #1c2128; font-weight: 600; color: white; }
                    tr:last-child td { border-bottom: none; }
                    tr:hover { background-color: rgba(255,255,255,0.02); }

                    .badge {
                        padding: 4px 8px; border-radius: 20px;
                        font-size: 12px; font-weight: 600; display: inline-block;
                    }
                    .badge-CRITICAL { background: rgba(137,87,229,0.2);  color: #bc8cff; border: 1px solid var(--critical); }
                    .badge-HIGH     { background: rgba(248,81,73,0.2);   color: #ff7b72; border: 1px solid var(--high); }
                    .badge-MEDIUM   { background: rgba(210,153,34,0.2);  color: #e3b341; border: 1px solid var(--medium); }
                    .badge-LOW      { background: rgba(63,185,80,0.2);   color: #56d364; border: 1px solid var(--low); }
                    .badge-NONE     { background: rgba(139,148,158,0.2); color: #8b949e; border: 1px solid var(--border-color); }

                    .tag-list { display: flex; gap: 6px; flex-wrap: wrap; }
                    .tag {
                        background: #21262d; border: 1px solid var(--border-color);
                        padding: 2px 6px; border-radius: 4px; font-size: 11px; color: #8b949e;
                    }
                    .code-ref { font-family: monospace; color: #a5d6ff; font-size: 13px; }
                    ${config.customCss}
                </style>
            </head>
            <body>
                <div class="container">
                    <div class="header">
                        <h1>$title</h1>
                        <span style="color:#8b949e">Generated on ${java.time.LocalDate.now()}</span>
                    </div>
        """.trimIndent())

        // Stats section — user override or built-in cards
        if (config.statsHtmlOverride.isNotBlank()) {
            sb.append(
                config.statsHtmlOverride
                    .replace("{{TOTAL}}", todos.size.toString())
                    .replace("{{CRITICAL}}", criticalCount.toString())
                    .replace("{{HIGH}}", highCount.toString())
                    .replace("{{MEDIUM}}", medCount.toString())
                    .replace("{{LOW}}", lowCount.toString())
            )
        } else {
            sb.append("""
                    <div class="stats">
                        <div class="stat-card">
                            <div class="stat-value">${todos.size}</div>
                            <div class="stat-label">Total Active tasks</div>
                        </div>
                        <div class="stat-card">
                            <div class="stat-value stat-CRITICAL">$criticalCount</div>
                            <div class="stat-label">Critical Priority</div>
                        </div>
                        <div class="stat-card">
                            <div class="stat-value stat-HIGH">$highCount</div>
                            <div class="stat-label">High Priority</div>
                        </div>
                        <div class="stat-card">
                            <div class="stat-value stat-MEDIUM">$medCount</div>
                            <div class="stat-label">Medium Priority</div>
                        </div>
                        <div class="stat-card">
                            <div class="stat-value stat-LOW">$lowCount</div>
                            <div class="stat-label">Low Priority</div>
                        </div>
                    </div>
            """.trimIndent())
        }

        // ── Section 2: list / table rows ────────────────────────────────────
        // Sort by priority (CRITICAL → LOW → NONE), then by due date
        val sortedTodos = todos.sortedWith(
            compareBy<TodoItem> {
                when (it.priority?.name?.uppercase()) {
                    "CRITICAL" -> 0; "HIGH" -> 1; "MEDIUM" -> 2; "LOW" -> 3; else -> 4
                }
            }.thenBy { it.dueDate?.toString() ?: "9999-99-99" }
        )

        val rowsHtml = StringBuilder()
        for (todo in sortedTodos) {
            val pName      = todo.priority?.name?.uppercase() ?: "NONE"
            val badgeClass = "badge-$pName"

            val tagsHtml = StringBuilder("""<div class="tag-list">""")
            if (todo.category != null) tagsHtml.append("""<span class="tag">cat: ${todo.category}</span>""")
            if (todo.dueDate != null) {
                val isOverdue = todo.dueDate.isBefore(java.time.LocalDate.now())
                val style = if (isOverdue) "color: #ff7b72; border-color: #f85149" else ""
                tagsHtml.append("""<span class="tag" style="$style">due: ${todo.dueDate}</span>""")
            }
            if (todo.issueId != null) tagsHtml.append("""<span class="tag" style="color:#a5d6ff; border-color:#58a6ff">ref: ${todo.issueId}</span>""")
            todo.tags.forEach { (k, v) -> tagsHtml.append("""<span class="tag">$k: $v</span>""") }
            tagsHtml.append("</div>")

            val desc     = todo.description.replace("<", "&lt;").replace(">", "&gt;")
            val file     = todo.getFileName().replace("<", "&lt;").replace(">", "&gt;")
            val assignee = todo.assignee ?: todo.vcsAuthor ?: "-"

            rowsHtml.append("""
                <tr>
                    <td><span class="badge $badgeClass">${todo.priority?.name ?: "-"}</span></td>
                    <td style="color:white; font-weight:500;">$desc</td>
                    <td style="color:#8b949e">${if (assignee != "-") "@$assignee" else "-"}</td>
                    <td>${if (tagsHtml.length > 25) tagsHtml.toString() else "-"}</td>
                    <td><span class="code-ref">$file:${todo.lineNumber}</span></td>
                </tr>
            """.trimIndent())
        }

        if (config.listHtmlOverride.isNotBlank()) {
            sb.append(config.listHtmlOverride.replace("{{ROWS}}", rowsHtml.toString()))
        } else {
            sb.append("""
                    <table>
                        <thead>
                            <tr>
                                <th>Priority</th>
                                <th>Task Description</th>
                                <th>Assignee</th>
                                <th>Tags &amp; Categories</th>
                                <th>Location</th>
                            </tr>
                        </thead>
                        <tbody>
                            $rowsHtml
                        </tbody>
                    </table>
            """.trimIndent())
        }

        // ── Section 3: footer + closing tags ────────────────────────────────
        sb.append("""
                </div>
                ${if (config.footerHtml.isNotBlank()) config.footerHtml else ""}
            </body>
            </html>
        """.trimIndent())

        return sb.toString()
    }

    /**
     * Export TODOs to a clean printable PDF Report.
     * Uses an [HtmlExportConfig] that overrides the title and injects print CSS.
     */
    fun exportToPdf(todos: List<TodoItem>): String {
        val printCss = """
            @media print {
                body { background: #fff !important; color: #000 !important; padding: 20px; }
                .stat-card, table { background: #fff !important; border: 1px solid #ccc !important; }
                th { background: #f0f0f0 !important; color: #000 !important; }
                td { color: #000 !important; }
                h1 { color: #000 !important; }
            }
        """.trimIndent()
        return exportToHtml(
            todos,
            HtmlExportConfig(pageTitle = "TODO++ Executive Task Report", customCss = printCss)
        )
    }
}
