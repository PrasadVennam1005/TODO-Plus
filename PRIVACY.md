# Privacy Policy for TODO++

**Last Updated: February 15, 2026**

## Overview

TODO++ ("the Plugin") is developed and maintained by Vennam Prasad ("we", "us", or "our"). We are committed to protecting your privacy.

## Data Collection

**TODO++ does NOT collect, store, or transmit any personal data.**

The plugin operates entirely locally within your IntelliJ IDEA or Android Studio environment and:

- ✅ **Does NOT collect** any personal information
- ✅ **Does NOT track** your usage or behavior
- ✅ **Does NOT transmit** any data to external servers
- ✅ **Does NOT use** analytics or tracking tools
- ✅ **Does NOT store** data outside your local machine

## What the Plugin Does

TODO++ scans your project files locally to find and display TODO comments. All processing happens on your computer. The plugin:

- Reads TODO comments from your source code files
- Displays them in a tool window
- Stores preferences locally in your IDE settings
- All data remains on your machine

## Local Data Storage

The only data stored by TODO++ is:

- **User Preferences**: Filter settings, window positions (stored in your IDE's local configuration)
- **Scan Results**: Cached TODO items (stored temporarily in memory during your IDE session)

All data is stored locally in your IDE's configuration directory and is never transmitted anywhere.

## Third-Party Services & User-Initiated Integrations

TODO++ does NOT operate any backend servers and does NOT transmit telemetry or background data.

All network communication is strictly **user-initiated and opt-in**:

1. **AI Jira Ticket Assistant (Optional)**:
   - When you explicitly click "Draft Jira Ticket with AI", the selected TODO comment and surrounding code context (&plusmn;15 lines) are sent directly from your IDE to your configured AI provider (Google Gemini, OpenAI, Anthropic Claude, or local Ollama).
   - Your API keys and tokens are stored securely in your local IDE configuration/credential store and are never transmitted to us or any unauthorized third party.
2. **Issue Tracker Export (Optional)**:
   - When you explicitly choose to export a task to GitHub Issues or Jira Cloud, the task details are sent directly to the specified repository or Jira Cloud tenant via standard REST APIs using your provided personal access token.
3. **Webhook Notifications (Optional)**:
   - When you trigger Slack or Discord alerts, overdue summary payloads are sent directly to your configured incoming webhook URL.

No data ever passes through any TODO++ intermediary servers. All requests go directly between your local machine and your chosen service endpoints.

## Children's Privacy

TODO++ does not knowingly collect information from anyone under the age of 13. The plugin is designed for professional developers.

## Changes to This Privacy Policy

We may update this Privacy Policy from time to time. Any changes will be reflected in the plugin's documentation and GitHub repository.

## Contact

If you have questions about this Privacy Policy, please:

- Open an issue on GitHub: https://github.com/vennamprasad/TODO-Plus/issues
- Contact: support@todoplus.dev

## Your Rights

Since TODO++ does not collect any personal data, there is no data to access, modify, or delete. All plugin data is stored locally on your machine and can be removed by uninstalling the plugin.

## Compliance

This plugin complies with:
- GDPR (General Data Protection Regulation)
- CCPA (California Consumer Privacy Act)
- Other privacy regulations (as we collect no personal data)

---

**Summary**: TODO++ is a privacy-first plugin. We don't collect anything. All data stays on your computer.
