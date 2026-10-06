# AI-DLC Phase 2: Component Detailed Design

**Project**: TODO++  
**Status**: APPROVED  

---

## 🎨 Component Design Specifications

### 1. `TodoScannerService` API & Centralized Cache

```kotlin
@Service(Service.Level.PROJECT)
class TodoScannerService(private val project: Project) {
    /**
     * Scans project files for TODO items, reporting progress to indicator.
     * Updates in-memory cache and broadcasts on MessageBus.
     */
    fun scanProject(indicator: ProgressIndicator? = null): List<TodoItem>

    /**
     * Incremental scan for a single file. Updates cache entry and broadcasts.
     */
    fun updateFileCache(file: VirtualFile): List<TodoItem>

    /**
     * Removes deleted file entry from cache and broadcasts.
     */
    fun removeFileFromCache(filePath: String): List<TodoItem>

    /**
     * Returns flattened list of all currently cached TODOs.
     */
    fun getCachedTodos(): List<TodoItem>

    /**
     * Returns whether an initial project scan has populated the cache.
     */
    fun isCacheInitialized(): Boolean

    /**
     * Clears internal cache.
     */
    fun clearCache()

    /**
     * Scans single virtual file for TODO items. Respects maxFileSizeMb cap.
     */
    fun scanFile(file: VirtualFile): List<TodoItem>

    /**
     * Computes statistics break-down by priority and assignment.
     */
    fun getStatistics(todos: List<TodoItem>): TodoStatistics

    /**
     * Initiates or coalesces a background project scan.
     * Guarantees at most ONE background task runs at any time.
     */
    fun requestProjectScan(onComplete: (List<TodoItem>) -> Unit = {})
}
```

### 2. `TodoChangeListener` MessageBus Topic

```kotlin
interface TodoChangeListener {
    companion object {
        val TOPIC = Topic.create("TODO++ Change Topic", TodoChangeListener::class.java)
    }
    fun onTodosUpdated(todos: List<TodoItem>)
}
```

#### Single-Flight Scan Coalescer Logic:
```kotlin
@Volatile private var activeIndicator: ProgressIndicator? = null
private val isScanRunning = AtomicBoolean(false)
private val pendingRescan = AtomicBoolean(false)

fun requestProjectScan(onComplete: (List<TodoItem>) -> Unit = {}) {
    if (isScanRunning.compareAndSet(false, true)) {
        ProgressManager.getInstance().run(object : Task.Backgroundable(project, "Scanning for TODOs", true) {
            override fun run(indicator: ProgressIndicator) {
                activeIndicator = indicator
                try {
                    val todos = scanProject(indicator)
                    ApplicationManager.getApplication().invokeLater { onComplete(todos) }
                } finally {
                    activeIndicator = null
                    isScanRunning.set(false)
                    if (pendingRescan.getAndSet(false)) {
                        requestProjectScan(onComplete)
                    }
                }
            }
            override fun onCancel() {
                activeIndicator = null
                isScanRunning.set(false)
            }
        })
    } else {
        // Coalesce request: another scan is already running; mark rescan pending
        pendingRescan.set(true)
    }
}
```

---

### 2. `TodoSettingsService.State` Model

```kotlin
class State {
    var priorities: MutableList<PriorityConfig> = mutableListOf(...)
    var customKeywords: MutableList<CustomKeywordConfig> = mutableListOf(...)
    var enableAudioFeedback: Boolean = true
    var issueUrlTemplate: String = ""
    var issuePattern: String = "[A-Z]+-\\d+"
    var ignoredDirectories: MutableList<String> = mutableListOf(
        "build", "node_modules", ".idea", ".git", "out", "dist", "bin", "obj",
        "target", ".gradle", "vendor", ".next", ".nuxt", "coverage", ".venv", "venv", "__pycache__", ".cargo"
    )
    var maxFileSizeMb: Int = 5
    var completionBehavior: String = BEHAVIOR_MARK_DONE
    var htmlExport: HtmlExportSettings = HtmlExportSettings()

    // Integration & Webhook settings
    var githubToken: String = ""
    var githubRepoOwner: String = ""
    var githubRepoName: String = ""
    var jiraBaseUrl: String = ""
    var jiraEmail: String = ""
    var jiraApiToken: String = ""
    var jiraProjectKey: String = ""
    var slackWebhookUrl: String = ""
    var discordWebhookUrl: String = ""

    // AI Jira Ticket Assistant Settings
    var aiProvider: String = "GEMINI"
    var aiApiKey: String = ""
    var aiModelName: String = "gemini-1.5-flash"
    var aiCustomEndpoint: String = "http://localhost:11434/v1"
    var enablePreCommitTodoCheck: Boolean = true
}
```

---

### 3. `IssueExporterService` API Spec

```kotlin
object IssueExporterService {
    fun createGitHubIssue(todo: TodoItem, token: String, owner: String, repo: String): Result<String>
    fun createJiraIssue(
        todo: TodoItem, 
        baseUrl: String, 
        email: String, 
        apiToken: String, 
        projectKey: String,
        customSummary: String? = null,
        customDescription: String? = null,
        customIssueType: String? = null
    ): Result<String>
}
```

---

### 4. `AiTicketSuggestionService` API Spec

```kotlin
enum class AiProvider(val displayName: String) {
    GEMINI("Google Gemini"),
    OPENAI("OpenAI"),
    CLAUDE("Anthropic Claude"),
    OLLAMA("Ollama / Local (OpenAI Compatible)")
}

data class AiTicketSuggestion(
    val summary: String,
    val description: String,
    val issueType: String = "Task",
    val priority: String = "Medium"
)

object AiTicketSuggestionService {
    fun generateTicketSuggestion(
        todo: TodoItem,
        enclosingCode: String,
        provider: AiProvider,
        apiKey: String,
        modelName: String,
        endpointUrl: String
    ): Result<AiTicketSuggestion>
}
```

---

### 5. `AiJiraTicketDialog` UI Spec

- **Dialog Wrapper**: `DialogWrapper(project, true)`
- **Header**: Displays file path, line number, and original TODO comment text.
- **Fields**:
  - `Summary`: Pre-populated `JBTextField` with AI-generated title.
  - `Issue Type`: Dropdown with `Task`, `Story`, `Bug`, `Improvement`.
  - `Priority`: Dropdown with `Highest`, `High`, `Medium`, `Low`, `Lowest`.
  - `Description`: Scrollable `JBTextArea` with acceptance criteria and code snippet context.
- **Action Buttons**:
  - `Cancel`: Discards proposal without modifying code.
  - `Create Issue & Link in Code`: Dispatches to Jira Cloud via `IssueExporterService`, gets created issue key (e.g. `PROJ-123`), and runs `WriteCommandAction` to insert `(issue:PROJ-123)` into the original TODO comment line.

---

### 6. `TodoCheckinHandlerFactory` VCS Spec

```kotlin
class TodoCheckinHandlerFactory : CheckinHandlerFactory() {
    override fun createHandler(panel: CheckinProjectPanel, commitContext: CommitContext): CheckinHandler
}
```
- Filters files staged for commit.
- Parses lines for TODO comments lacking `issueId`.
- Prompts the user: *"Found X unlinked TODO comments in commit. Would you like to generate Jira tickets with AI before committing?"*
- Allows 1-click launch of `AiJiraTicketDialog` or proceeding with commit.

---

### 7. Settings UI (`TodoSettingsConfigurable`)

- **Priority Levels Panel**: `JBList` with `ToolbarDecorator`.
- **Ignored Directories Panel**: `JBList` for user-defined folder exclusions.
- **Issue Tracker & Webhooks Panel**: GitHub Token, Jira Credentials, Slack/Discord Webhooks.
- **AI Assistant Panel**: Provider dropdown (Gemini / OpenAI / Claude / Ollama), API Key field (`JPasswordField`), Model Name (`JBTextField`), Custom Endpoint URL, and Pre-commit Check checkbox.

