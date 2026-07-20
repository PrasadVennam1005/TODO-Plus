## Security Policy

### Supported Versions

We release patches for security vulnerabilities for the following versions:

| Version | Supported          |
| ------- | ------------------ |
| 2.0.x   | ✅ Yes             |
| 1.9.x   | ✅ Yes (critical fixes only) |
| < 1.9   | ❌ No              |

### Reporting a Vulnerability

If you discover a security vulnerability in TODO++, **please do NOT open a public GitHub issue**.

Instead, report it responsibly via one of these methods:

1. **GitHub Private Advisory** (preferred): Go to the [Security tab](../../security/advisories/new) of this repo and open a private advisory.
2. **Email**: Send details to the repo owner directly.

Please include:
- A description of the vulnerability and its potential impact
- Steps to reproduce the issue
- The plugin version and IDE version you're using

We will acknowledge your report within **48 hours** and aim to release a patch within **7 days** for critical issues.

### Scope

As an IntelliJ Platform plugin, TODO++ does not handle sensitive credentials or network communication. However, we take all reports seriously, including:
- Malicious file read/write via custom regex patterns
- Unintended code execution via parsed TODO comments
- Data leaks from PSI or VCS annotation APIs

Thank you for helping keep the developer community safe! 🙏
