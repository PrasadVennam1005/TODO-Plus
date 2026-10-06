# How to Use TODO++

Transform standard TODO comments into an intelligent, AI-augmented task management system directly inside JetBrains IDEs.

---

## 🚀 Quick Start

1. **Open the Tool Window**: Click the **TODO++** tab at the bottom of your IDE (or `View > Tool Windows > TODO++`).
2. **Write a TODO**: In any code file, type `// TODO: Fix this later`.
3. **Live Auto-Scan**: TODO++ automatically indexes your comments as you type with sub-10ms incremental scanning.
4. **Full Scan**: Click the **🔍 Scan Project** button in the tool window toolbar to refresh the entire solution.

---

## 📝 Syntax & Metadata Guide

TODO++ parses comments in your code across 20+ programming languages. You can embed metadata inside parentheses `(...)` immediately after the TODO keyword.

### 1. Basic Reminder
```kotlin
// TODO: Simple reminder
# FIXME: Fix before release
/* NOTE: Remember to verify null check */
```

### 2. Priority, Assignee & Category
Organize your tasks with standard tags:
```kotlin
// TODO(priority:critical): Fix production authentication crash
// TODO(@john): Assigned to John
// TODO(category:refactor): Clean up legacy controller
```
* **Priorities**: `CRITICAL` (🟣), `HIGH` (🔴), `MEDIUM` (🟠), `LOW` (🟢), or your own custom priority tags.
* **Assignee**: Starts with `@` (e.g. `@alice`, `@team`).
* **Category**: Any label (e.g. `refactor`, `bug`, `perf`, `security`).

### 3. 📅 Due Dates & Overdue Highlighting
Set deadlines for your tasks. Overdue items are automatically highlighted in **bold red** with optional balloon alerts:
```kotlin
// TODO(due:2026-10-15): Release milestone deadline
// TODO(due:today): Must finish before end of sprint
// TODO(due:tomorrow): Prepare for team sync
```

### 4. 🔗 Issue Linking
Link TODOs directly to external trackers (Jira, GitHub, YouTrack, etc.):
```kotlin
// TODO(issue:PROJ-101): Refactor database query
// TODO(issue:#42): Fix race condition in worker
```
* **Auto-detection**: If configured, mentioning IDs in the description also links automatically (e.g. `// TODO: Resolve PROJ-101`).
* **Jump to Tracker**: Double-click or right-click any linked task and select **Open in Issue Tracker** to open the ticket in your default browser.

### 5. 🏷️ Custom Tags & Key-Value Pairs
Add arbitrary metadata tags:
```kotlin
// TODO(risk:high estimate:4h): Complex refactoring
// TODO(reviewer:@alice type:security): Security audit needed
```

### 6. ⚡ Power User Combos
Combine all tags into a comprehensive task definition:
```kotlin
// TODO(@john priority:critical due:today issue:PROJ-101 category:bug): Fix memory leak in auth pool
```

* **Live Template**: Type `todo+` and press `Tab` or `Space` to generate an enriched TODO template instantly.
* **Quick Fix (Intention Action)**: Position the cursor on any basic `// TODO: fix this`, press **`Alt + Enter`** (or `Option + Enter`), and select **"Upgrade to TODO++ format"**.

---

## 🤖 AI-Powered Jira Ticket Assistant (New in v2.4.0)

Stop unlinked and forgotten TODO comments from crossing sprints without Jira tickets!

### How to Draft Jira Tickets with AI:
1. In the **TODO++ tool window**, select any unlinked TODO task.
2. Click the **🤖 Suggest Jira** button on the toolbar (or right-click -> **Draft Jira Ticket with AI**).
3. TODO++ inspects the TODO comment and extracts surrounding code context ($\pm 15$ lines).
4. The configured AI model analyzes the code and generates:
   * **Structured Summary**: Concise, standardized title for Jira.
   * **Issue Type**: Automatically classified (e.g., `Bug`, `Task`, `Story`, `Technical Debt`).
   * **Description**: Detailed markdown breakdown including code snippet, context, and potential solution.
   * **Acceptance Criteria**: Bulleted checklist of verifiable conditions.
5. An interactive **Modal Preview Dialog** appears:
   * Inspect and customize any fields.
   * Toggle **"Update code comment in-place with assigned issue key"**.
6. Click **Create in Jira**:
   * Creates the issue via Jira REST API.
   * Automatically updates your source code comment in-place (e.g., `// TODO(issue:PROJ-101): ...`).

### Supported AI Providers:
* **Google Gemini**: Gemini 1.5 Flash (default, fast) or Gemini 1.5 Pro.
* **OpenAI**: GPT-4o, GPT-4o-mini.
* **Anthropic Claude**: Claude 3.5 Sonnet.
* **Ollama / Local LLMs**: Any OpenAI-compatible local endpoint (e.g., `http://localhost:11434/v1` with `llama3`, `mistral`, or `codellama`).

---

## 🛡️ VCS Pre-Commit Guard (New in v2.4.0)

Never accidentally commit orphan TODO comments before a sprint cut!

1. When you commit files through IntelliJ's Git commit dialog (`Cmd + K` / `Ctrl + K`), TODO++ automatically inspects staged files.
2. If unlinked TODO comments (without an issue ticket) are detected, a warning modal appears:
   * **"Review in TODO++"**: Opens the tool window so you can draft Jira tickets with AI before committing.
   * **"Commit Anyway"**: Bypasses the check and proceeds with the commit.
   * **"Cancel Commit"**: Aborts the commit to address TODOs.
3. **Configuration**: Toggle on/off under **Settings > Tools > TODO++ > Enable Pre-Commit TODO Check**.

---

## 🌐 REST Exports & Team Webhooks

### 1-Click Issue Tracker Exports:
* **GitHub Issues**: Right-click any task -> **Export Task to GitHub Issue**. Automatically creates a formatted issue on your repository (`POST /repos/{owner}/{repo}/issues`).
* **Jira Cloud**: Right-click any task -> **Export Task to Jira Issue**. Directly creates a ticket in your Jira project (`POST /rest/api/2/issue`).

### Team Webhook Notifications:
* Click the **📢 Slack / Discord** toolbar button to dispatch instant overdue task alerts formatted with Block Kit (Slack) or Rich Embeds (Discord).

---

## ⚙️ Configuration Guide

Access settings via **Settings/Preferences > Tools > TODO++**.

### 1. AI Assistant Settings
* **AI Provider**: Choose `GEMINI`, `OPENAI`, `CLAUDE`, or `OLLAMA`.
* **API Key**: Enter your API key (stored securely in IntelliJ's credential store).
* **Model Name**: Specify the model identifier (e.g. `gemini-1.5-flash`, `gpt-4o-mini`, `claude-3-5-sonnet`).
* **Local Endpoint URL**: For Ollama/local models (e.g. `http://localhost:11434/v1`).
* **Pre-Commit Check**: Checkbox to enable or disable the VCS pre-commit warning guard.

### 2. Issue Tracker Credentials
* **GitHub Settings**:
  * **Personal Access Token**: Classic PAT (with `repo` scope) or Fine-grained PAT (with `Issues: Read and write`).
  * **Repository Owner & Name**: e.g., `vennamprasad` / `TODO-Plus`.
* **Jira Cloud Settings**:
  * **Base URL**: e.g. `https://mycompany.atlassian.net`
  * **Account Email**: Your Atlassian account email.
  * **API Token**: Atlassian API token.
  * **Project Key**: Jira project key (e.g., `PROJ`).

### 3. Custom Priorities & Colors
* Click **+** to add custom priority labels (e.g. `BLOCKER`, `URGENT`).
* Click the color box to customize color highlights in the tree and editor.
* Reorder items to adjust sort urgency.

### 4. Custom Keywords
* Add custom comment markers (e.g. `HACK:`, `BUG:`, `NOTE:`, `OPTIMIZE:`, `PERF:`).
* Choose custom emoji badge icons.

### 5. Performance & Large Project Scanning
* **Ignored Directories**: Add folder patterns to ignore (defaults include `build`, `node_modules`, `.next`, `dist`, `coverage`, `.venv`).
* **Max File Size Limit**: Configurable cap (default 5 MB) prevents scanning huge generated binaries or minified bundles.
* **Native Exclusions**: Automatically respects `.gitignore` and IDE-excluded directories via `ProjectFileIndex`.

---

## 🔎 Tool Window Features

* **Hierarchy Grouping**: Use the **Group By** dropdown to view tasks grouped by:
  * 📁 **File**
  * 👤 **Assignee**
  * 🎯 **Priority** (sorted by urgency: Critical → High → Medium → Low)
  * 🏷️ **Category**
* **Live Debounced Search**: Type in the search box to filter instantly by description, `@assignee`, or `category`.
* **Multi-Select & Batch Actions**: Hold `Shift` or `Cmd/Ctrl` to select multiple tasks (or entire group headers) and click:
  * **✅ Mark Completed**: Comments out or tags task as done.
  * **↩️ Mark Incomplete**: Reverts task back to active state.
* **Progress Bar & Status**: Real-time completion progress bar (e.g. `75% (9/12)`) in the status bar with high-contrast indicator.
* **Copy for Standup**: 1-click button to format all visible tasks into clean markdown bullets ready to paste into Slack, Teams, or daily standup notes.
* **Export Reports**: Export your tasks into **CSV**, **Markdown**, or an **Executive HTML Dashboard** with charts and statistics.
