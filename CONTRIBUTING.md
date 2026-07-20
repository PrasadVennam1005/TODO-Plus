# Contributing to TODO++

Thank you for your interest in contributing to **TODO++**! 🎉

## Getting Started

1. **Fork** the repository on GitHub
2. **Clone** your fork locally:
   ```bash
   git clone https://github.com/YOUR_USERNAME/TODO-plus.git
   cd TODO-plus
   ```
3. Open the project in **IntelliJ IDEA** or **Android Studio**
4. Run the plugin locally:
   ```bash
   ./gradlew runIde
   ```

## Development Environment

- **JDK 17+** (Zulu or Temurin recommended)
- **IntelliJ IDEA 2024.1+** (Community or Ultimate)
- **Kotlin 1.9+**
- **Gradle 8.x** (wrapper included)

## Running Tests

```bash
./gradlew test
```

## Code Style

- Follow standard **Kotlin coding conventions** (KDoc-style comments for public APIs)
- Keep functions short and single-responsibility
- All new features should have accompanying **unit tests** in `src/test/kotlin/`

## Submitting a Pull Request

1. Create a new branch from `main`:
   ```bash
   git checkout -b feature/your-feature-name
   ```
2. Make your changes and **run the full test suite** (`./gradlew test`)
3. Commit with a clear message:
   ```
   feat: Add my cool new feature
   fix: Resolve edge case in TodoParser
   ```
4. Push your branch and open a **Pull Request** against `main`
5. Describe your change, link any related issues, and wait for review

## Reporting Bugs

Use the [Bug Report](.github/ISSUE_TEMPLATE/bug_report.md) template — include your IDE version, plugin version, and any stack traces from the IDE log.

## Feature Requests

Use the [Feature Request](.github/ISSUE_TEMPLATE/feature_request.md) template — share the problem you're trying to solve, not just the solution you have in mind.

## Code of Conduct

Be kind, constructive, and respectful. We're all here to make developer tools better.

---

Made with ❤️ by the TODO++ community.
