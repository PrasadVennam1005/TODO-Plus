# AI-DLC Phase 1: Product & Technical Requirements

**Project**: TODO++ (IntelliJ Platform Plugin)  
**Version**: 2.2.0  
**Status**: ACTIVE  

---

## 🎯 Executive Summary
TODO++ is an enhanced TODO management plugin for IntelliJ-based IDEs (IntelliJ IDEA, PyCharm, WebStorm, Rider, GoLand, etc.). It parses, organizes, filters, exports, and tracks TODO items across single files or whole projects.

---

## 📋 Functional Requirements

### 1. Comment Parsing & Metadata Extraction
- **FR-01**: Parse single-line (`//`, `#`, `--`, `;`) and multi-line (`/* ... */`, `/** ... */`) TODO comments across 15+ programming languages.
- **FR-02**: Extract metadata syntax inside `(...)`: `@assignee`, `priority:CRITICAL|HIGH|MEDIUM|LOW`, `due:YYYY-MM-DD`, `category:text`, `issue:ID`, and custom key-value pairs (`key:value`).
- **FR-03**: Support multi-line continuation comments with indented bullet points.

### 2. Project Scanning & Scope Management
- **FR-04**: Support scanning scope selection ("Current File" vs "Entire Solution / Project").
- **FR-05**: Perform background asynchronous scans without freezing the IDE UI thread.
- **FR-06**: Filter out binary files, unindexed files, and user-ignored directory patterns.
- **FR-07**: Utilize native `ProjectFileIndex` to skip excluded files (`.gitignore`, build output folders).
- **FR-08**: Stream granular progress updates (`Scanning file X of Y: filename`, fraction percentage) to the IDE background task indicator.
- **FR-09**: Support configurable file size cap (`maxFileSizeMb`, default 5 MB).

### 3. Tool Window & UI Management
- **FR-10**: Hierarchical tree table with dynamic grouping by File, Assignee, Priority, or Category.
- **FR-11**: Priority badges (🟣 Critical, 🔴 High, 🟠 Medium, 🟢 Low) and deadline color alerts (Red = Overdue, Orange = Due Soon).
- **FR-12**: Multi-selection and batch mark complete/incomplete actions.
- **FR-13**: Jump-to-source on double-click.

### 4. Issue Tracker & VCS Blame Integration
- **FR-14**: Git blame integration fetching author and commit date per TODO item.
- **FR-15**: Issue tracker linking using custom URL templates (e.g. `https://jira.org/browse/{id}`).

### 5. Exporting & Reporting
- **FR-16**: Standalone interactive HTML dashboard generation with custom CSS and statistics overrides.
- **FR-17**: Printable PDF report export.
- **FR-18**: 1-click Markdown / Slack checklist export for standups.

### 6. REST API Integrations & Webhooks
- **FR-19 (Issue REST Export)**: Export selected TODO tasks directly as new issues to GitHub (`POST /repos/{owner}/{repo}/issues`) or Jira (`POST /rest/api/3/issue`) via authenticated REST APIs.
- **FR-20 (Webhook Alerts)**: Send formatted Overdue TODO alert notifications directly to Slack or Discord webhook endpoints.

### 7. Centralized Cache, Incremental Scanning & Multi-Window Sync
- **FR-21 (Centralized Project Cache)**: Maintain an in-memory, thread-safe cache (`ConcurrentHashMap<String, List<TodoItem>>`) of TODO items indexed by file path inside `TodoScannerService` as the single source of truth.
- **FR-22 (Incremental File Scanning)**: On document typing or file modifications, perform an incremental scan only for the affected `VirtualFile` (< 10ms) and update the cache without triggering full project re-scans.
- **FR-23 (Single-Flight Background Scan Coalescing)**: Enforce a single active background scan at any time. When a new scan is triggered, either cancel the active scan or coalesce pending requests, preventing duplicate "Scanning for TODOs" background tasks and progress windows.
- **FR-24 (Multi-Window MessageBus Sync)**: Broadcast cache updates across all tool window contents via IntelliJ `MessageBus` topic (`TodoChangeListener.TOPIC`) to maintain 100% synchronization across split editors and windows.
### 8. AI-Powered Jira Ticket Assistant
- **FR-26 (AI Context Extraction & Ticket Suggestion)**: Extract TODO comment context (surrounding class/function lines, file path, author) and prompt configured LLMs to generate a structured Jira ticket proposal (Summary, Description with acceptance criteria, Issue Type, Priority).
- **FR-27 (Multi-Provider AI Client)**: Support user-selected AI backends (Gemini, OpenAI, Anthropic Claude, and Ollama/Custom OpenAI-compatible endpoint) configured with API Key, Model, and Endpoint URL in `TodoSettingsService`.
- **FR-28 (Interactive Review & In-Place Code Update)**: Present an interactive dialog (`AiJiraTicketDialog`) permitting users to review and edit AI suggestions before 1-click creation in Jira Cloud. Upon issue creation, automatically update the source code comment in-place (e.g. `// TODO(issue:PROJ-101): ...`).
- **FR-29 (Pre-Commit Checkin Handler)**: Provide a Git commit hook (`CheckinHandlerFactory`) that detects unlinked/orphaned TODOs in staged changes and prompts the user to draft Jira tickets before committing.

---

## ⚙️ Non-Functional Requirements

- **NFR-01 (Performance)**: Full project scan of 1,000+ files must complete in under 5 seconds on standard developer machines without UI lag.
- **NFR-02 (Compatibility)**: 100% binary & API compatibility with IntelliJ Platform SDK 2024.1+ (Build `241+`).
- **NFR-03 (Thread Safety)**: Zero synchronous lock contention or EDT blocking. All PSI/VFS access wrapped in `runReadAction`.
- **NFR-04 (Cross-Platform)**: Path separator handling must work seamlessly across macOS, Linux, and Windows (`\` vs `/`).
- **NFR-05 (Rendering Scalability)**: Tree Table rendering and filtering must complete in under 50ms on datasets of 5,000+ items without locking the Event Dispatch Thread (EDT).
- **NFR-06 (Scan Concurrency & Latency)**: Incremental updates must process within 10ms on background thread without invoking background task dialogs; full scans must never produce multiple concurrent progress windows.
- **NFR-07 (AI API Reliability & Timeout)**: AI network calls must run asynchronously on background threads with configurable timeouts (default 15s) and clear user error handling without blocking the IDE.

