# Changelog

All notable changes to **TODO++** will be documented in this file.

The format follows [Keep a Changelog](https://keepachangelog.com/en/1.0.0/) and this project adheres to [Semantic Versioning](https://semver.org/spec/v2.0.0.html).

---

## [2.0.0] – 2026-07-20

### ✨ New Features
- **Live Auto-Scan**: TODO list updates automatically within 500ms as you type — no manual refresh needed
- **Quick Fix Intentions**: Mark any TODO complete/incomplete directly from the code editor via Alt+Enter
- **Priority Badges**: Colour-coded badges (🟣 Critical / 🔴 High / 🟠 Medium / 🟢 Low) visible in the tree table
- **Copy for Standup**: One-click copy of selected TODOs formatted as a Markdown/Slack checklist
- **Multi-Line TODO Comments**: Indented bullet points beneath a TODO are automatically captured as multi-line descriptions
- **Custom Keywords**: Define your own patterns (`HACK:`, `BUG:`, `NOTE:`, `OPTIMIZE:`) in Settings
- **Sound Effects**: Optional subtle chime and pop audio feedback on task completion
- **Export to PDF**: Generate printable PDF task reports from the toolbar
- **Full Solution Scope**: Scans `.cshtml`, `.razor`, `.vue`, `.svelte`, `.dart`, `.yaml`, `.json`, `.toml`, `.md`, `.env` and all non-binary registered file types
- **Priority Urgency Sorting**: Tree groups now sort CRITICAL → HIGH → MEDIUM → LOW

### 🐛 Bug Fixes
- Fixed strikethrough styling being lost on selected rows in the tree table
- Fixed `SimpleToolWindowPanel` deprecated constructor warning
- Fixed `ColorChooser` deprecation — replaced with `JColorChooser.showDialog`
- Fixed memory leak: `TodoToolWindowContent` now implements `Disposable` and is bound to tool window lifecycle

### 🔧 Improvements
- Scanner now skips files larger than 5MB to prevent `OutOfMemoryError` on large generated assets
- VCS annotation fetching now runs outside the PSI ReadAction lock, preventing UI thread deadlocks
- Added `bin`, `obj`, `target`, `.gradle`, `vendor` to default ignored directories
- Plugin Verifier: **100% Compatible** against IntelliJ Platform 2024.1+ with zero deprecation warnings

---

## [1.9.0] – 2026-06-01

### ✨ New Features
- Multi-selection with batch complete/incomplete via Green Tick (✔) and Red Cross (✖) toolbar buttons
- Single-line multi-TODO parsing — multiple TODOs on one line now each appear as separate entries
- Expanded file extension support including web frameworks and config formats

### 🐛 Bug Fixes
- Fixed scope label to read "Entire Solution / Project"
- Fixed toolbar icon rendering on macOS

---

## [1.0.0] – 2026-01-01

### 🎉 Initial Release
- Project-wide TODO scanner for IntelliJ-based IDEs
- Tree table grouped by File, Assignee, Priority, or Category
- Settings panel with custom patterns, ignored directories, and colour preferences
- VCS blame integration showing author and commit date per TODO
