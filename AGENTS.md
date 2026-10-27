# Repository Guidelines

## Project Structure & Module Organization

Molda is a Kotlin Android application with one Gradle module, `app`, using Jetpack Compose and Material 3. Application code lives in `app/src/main/java/com/almeidajr/molda/`; `MainActivity.kt` contains the entry point and initial UI, while `ui/theme/` defines colors, typography, and theming. Android resources are in `app/src/main/res/`, and the manifest is `app/src/main/AndroidManifest.xml`.

Local JVM tests belong in `app/src/test/`; device tests belong in `app/src/androidTest/`. Dependencies and plugin versions are centralized in `gradle/libs.versions.toml`.

## Build, Test, and Development Commands

Run these commands from the repository root in PowerShell:

- `.\gradlew.bat :app:assembleDebug` builds the debug APK.
- `.\gradlew.bat :app:testDebugUnitTest` runs local JUnit tests.
- `.\gradlew.bat :app:connectedDebugAndroidTest` runs instrumented tests on a connected device or emulator.
- `.\gradlew.bat :app:lintDebug` runs Android Lint.
- `.\gradlew.bat :app:installDebug` installs the debug app; launch it from the device or Android Studio.

On macOS/Linux, use `./gradlew`. Configure Android SDK platform 37.2 (`platforms;android-37.2`); devices require API 36 or newer. Gradle daemon criteria specify JDK 25, while Java source compatibility is 11.

## Coding Style & Naming Conventions

Use Spotless + ktlint with `.editorconfig`. Run `.\gradlew.bat spotlessApply` before validation. Use PascalCase for classes, files, and UI composables (`GreetingPreview`); use camelCase for ordinary functions and properties. Keep packages under `com.almeidajr.molda` and use lowercase underscore names for Android resources. Write English, identifiers included, in American spelling; user-facing text is Brazilian Portuguese. See `CONTRIBUTING.md` for quality checks, CI, and dependency-update conventions.

Target zero warnings from every tool, Android Studio inspections included; Kotlin specifics live in [Kotlin style](.agents/rules/kotlin-style.md).

Prefer existing tools and their configuration over custom code; add scripts only when available tools cannot complete the task.

For UI icons, use official Material Symbols XML vector assets from [Google Fonts](https://fonts.google.com/icons), stored in `app/src/main/res/drawable/`. Reuse existing assets and render them with Material 3 `Icon` and `painterResource`, letting Compose apply the theme tint; remove XML theme tint attributes. When no symbol fits a domain glyph, draw it on the Material Symbols 24dp grid as `ic_skirt.xml` does: 2-unit strokes, white stroke color, no tint attribute. Do not add the legacy Compose Material Icons libraries. See [Android icon guidance](https://developer.android.com/develop/ui/compose/graphics/images/material).

When writing or changing Kotlin code, read and follow [Kotlin style](.agents/rules/kotlin-style.md) for functional design, immutability, idiomatic syntax, and library selection.

## Testing Guidelines

Use JUnit 4 for local tests and AndroidJUnit4 for instrumented tests. Espresso and Compose UI testing dependencies are available. Name test classes with a `Test` suffix and methods descriptively, following examples such as `addition_isCorrect`. Add behavior-focused tests for changed logic and UI interactions. No coverage threshold is configured.

When adding or changing tests, read and follow [Kotlin tests](.agents/rules/kotlin-tests.md) for local and device tests: test selection, assertions, coroutines, fakes, and synchronization.

## Commit & Pull Request Guidelines

Before opening a PR or pushing to one, build and run every test locally: the full validation in `CLAUDE.md`, then `.\gradlew.bat :app:connectedDebugAndroidTest`. If no device is connected, start the API 37 emulator CI matches (`emulator -list-avds`, e.g. `Molda_API_37`); never leave device tests for CI to catch. Report any failure, including a suspected flake, instead of opening the PR.

Use Conventional Commit subjects for every new commit and PR title, such as `test(ui): check greeting on launch`. Scope is optional with no allowlist; use `!` and explain impact/migration for breaking PRs. Keep changes focused. Before creating commits, opening PRs, rebasing published history, or pushing, read [CONTRIBUTING.md](CONTRIBUTING.md) for types, examples, validation, and workflow guidance. Commit or push only when the user explicitly requests it.

## Configuration Hygiene

Keep SDK paths in ignored `local.properties`. Do not commit credentials, signing keys, generated build output, or local IDE state.

## Agent skills

### Issue tracker

Issues and specs live in GitHub Issues for `almeidajr/Molda`. See `docs/agents/issue-tracker.md`.

### Triage labels

Use the five default triage labels. See `docs/agents/triage-labels.md`.

### Domain docs

Single-context: root `GLOSSARY.md` and `docs/adr/`. See `docs/agents/domain.md`.
