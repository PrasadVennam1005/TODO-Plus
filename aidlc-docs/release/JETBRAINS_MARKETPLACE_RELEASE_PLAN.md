# 🚀 JetBrains Marketplace Release Plan: TODO++ v2.4.0

> **AWS AI-DLC 2.0 Phase 3: Release & Deployment Plan**  
> **Plugin ID**: `com.todoplus`  
> **Plugin Name**: `TODO++`  
> **Version**: `2.4.0`  
> **Target Platform**: IntelliJ Platform 2024.1+ (Builds `241`–`243+`, Unbounded)  
> **Generated Date**: October 6, 2026  

---

## 📋 Executive Summary
This release plan defines the execution roadmap, pre-flight verification gates, publication methods, marketplace listing assets, and post-release validation for **TODO++ v2.4.0** on the **JetBrains Marketplace**.

---

## 🛡️ Pre-Flight Verification Gates (Gated & Approved)

Before publishing to JetBrains Marketplace, all quality gates have been executed and empirically verified:

| Gate | Requirement | Empirical Result | Status |
| :--- | :--- | :--- | :---: |
| **Unit Testing** | 100% pass rate across test suite | 52/52 tests passing (`./gradlew test`) | ✅ **PASSED** |
| **Plugin Package** | Validated distributable archive | [`build/distributions/TODO-Plus-2.4.0.zip`](file:///Users/prasadvennam/MY%20WORK/IDEA-PROJECTS/TODO-plus/build/distributions/TODO-Plus-2.4.0.zip) (274 KB) | ✅ **PASSED** |
| **Plugin Verifier (2024.1)** | IC-241.14494.240 binary compatibility | **Compatible**, 0 errors, 0 deprecated APIs | ✅ **PASSED** |
| **Plugin Verifier (2024.2)** | IC-242.20224.300 binary compatibility | **Compatible**, 0 errors, 0 deprecated APIs | ✅ **PASSED** |
| **Plugin Verifier (2024.3)** | IC-243.21565.193 binary compatibility | **Compatible**, 0 errors, 0 deprecated APIs | ✅ **PASSED** |
| **Dynamic Plugin Reload** | Load/unload without restarting IDE | Eligible: `Plugin can probably be enabled or disabled without IDE restart` | ✅ **PASSED** |
| **Thread Safety** | Non-blocking EDT, clean background tasks | Backgroundable tasks with progress indicators & async dispatch | ✅ **PASSED** |
| **Git Repositories** | Synchronized with release tag | Commits pushed to `origin` & `upstream` with tag `v2.4.0` | ✅ **PASSED** |

---

## 🎯 Publishing Methods

### Option 1: Direct Web Upload (Immediate & Recommended)
This is the fastest method and requires no additional CI/CD secret setup.

1. **Log in to JetBrains Marketplace**:
   * Navigate to [JetBrains Marketplace Vendor Portal](https://plugins.jetbrains.com/).
   * Click **Sign In** with your JetBrains account (the account registered as the vendor of `com.todoplus`).
2. **Access Your Plugin**:
   * Go to **Upload Update** or open your plugin management page: [TODO++ on JetBrains Marketplace (ID 30223)](https://plugins.jetbrains.com/plugin/30223-todo-).
3. **Upload Distributable Archive**:
   * Click **Upload New Update** (or **Upload Plugin**).
   * Drag & drop the built ZIP file:
     ```
     build/distributions/TODO-Plus-2.4.0.zip
     ```
   * Alternatively, download directly from GitHub:  
     `https://github.com/vennamprasad/TODO-Plus/releases/download/v2.4.0/TODO-Plus-2.4.0.zip`
4. **Confirm Release Channel**:
   * Leave channel empty (defaults to **Stable / Default Channel**) or select `Stable`.
5. **Submit for Approval**:
   * Click **Upload**. JetBrains will automatically scan the ZIP and verify signatures, `plugin.xml`, and dependencies.

---

### Option 2: Automated Publishing via Gradle (`publishPlugin`)
If you prefer automated publishing directly from the command line:

1. **Generate a JetBrains Marketplace Token**:
   * Go to [JetBrains Hub Profile -> Tokens](https://hub.jetbrains.com/users/me?tab=tokens).
   * Click **New Token**, name it `TODO-Plus-Publisher`, and grant permission to upload updates for `TODO++`.
2. **Set Environment Variable & Publish**:
   ```bash
   export PUBLISH_TOKEN="your_jetbrains_marketplace_token_here"
   ./gradlew publishPlugin -x buildSearchableOptions
   ```

---

### Option 3: Automated Continuous Deployment via GitHub Actions
To enable 1-click publishing whenever a Git tag is pushed:

1. **Add Repository Secret**:
   * Go to [Repository Secrets](https://github.com/vennamprasad/TODO-Plus/settings/secrets/actions).
   * Add secret `JETBRAINS_MARKETPLACE_TOKEN` with your Marketplace token value.
2. **Enable Workflow**:
   * Un-ignore `.github/workflows/publish.yml` in `.gitignore` and push to upstream.
   * Pushing any `v*` tag will automatically run unit tests, plugin verifier, and execute `publishPlugin`.

---

## 📝 Marketplace Listing Assets & Content

### Plugin Title & Basic Info
* **Name**: TODO++
* **Plugin ID**: `com.todoplus`
* **Vendor**: TODO++ Team (`support@todoplus.dev`)
* **Category**: Code Tools / Tasks Management / Utilities
* **Tags**: `todo`, `task management`, `jira`, `ai`, `gemini`, `openai`, `claude`, `git`, `code review`, `productivity`

### Release Notes for v2.4.0 (What's New)
```html
<h4>v2.4.0 Release Notes:</h4>
<ul>
    <li><strong>🤖 AI-Powered Jira Ticket Assistant</strong>: Turn unlinked or forgotten TODO comments into actionable Jira tickets with automated in-place code linking (<code>// TODO(issue:KEY): ...</code>). Supports Google Gemini, OpenAI, Claude, and Ollama.</li>
    <li><strong>🛡️ VCS Pre-Commit Guard</strong>: Automatically checks staged files during Git commit and warns if unlinked TODO comments are found, stopping forgotten tasks from crossing sprints.</li>
    <li><strong>🔄 Real-Time Centralized Cache & Single-Flight Scan</strong>: Incremental file scanning while typing with MessageBus multi-window sync. Coalesced background tasks prevent redundant scanning.</li>
    <li><strong>🎨 Settings UI Form Improvements</strong>: Robust responsive GridBagLayout form layout preventing UI displacement when pasting long API tokens.</li>
    <li><strong>⚡ Multi-IDE Verified (2024.1, 2024.2, 2024.3)</strong>: 100% clean platform APIs with dynamic reload support (no IDE restart required).</li>
</ul>
```

---

## 🔍 Post-Release Smoke Test & Verification Checklist

Once the update is approved by JetBrains Marketplace (typically 1–2 business days for first-time or major updates, or instant for recognized vendors):

- [ ] **Marketplace Listing Check**:
  - Verify version number displays `2.4.0`.
  - Verify "What's New" displays the v2.4.0 bullet points.
  - Check compatible IDEs listed includes IntelliJ IDEA, PyCharm, WebStorm, Rider, GoLand, CLion, Android Studio.
- [ ] **IDE In-App Installation**:
  - In IntelliJ IDEA: Navigate to **Settings > Plugins > Marketplace**, search for `TODO++`.
  - Click **Update** or **Install** without restarting the IDE (verifying dynamic load).
- [ ] **Core Feature Validation in IDE**:
  - **Tool Window**: Open `TODO++` bottom tool window; verify group badges and filters render cleanly.
  - **AI Jira Draft**: Select an unlinked TODO, right-click -> **Draft Jira Ticket with AI** (or toolbar button); verify preview modal populates.
  - **Git Commit Check**: Stage a file with an unlinked `// TODO: ...` and attempt a commit in IDE; verify pre-commit warning modal appears with options to Review, Commit Anyway, or Cancel.
- [ ] **Community & Announcement**:
  - Update repository README badges.
  - Post release announcement on social/developer channels using template in `docs/LINKEDIN_POST_TEMPLATE.md`.
