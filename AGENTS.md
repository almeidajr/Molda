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

On macOS/Linux, use `./gradlew`. Configure Android SDK 37; devices require API 36 or newer. Gradle daemon criteria specify JDK 25, while Java source compatibility is 11.

## Coding Style & Naming Conventions

Follow official Kotlin style, configured in `gradle.properties`, with four-space indentation. Use PascalCase for classes, files, and UI composables (`GreetingPreview`); use camelCase for ordinary functions and properties. Keep packages under `com.almeidajr.molda` and use lowercase underscore names for Android resources. Format code with Android Studio. No standalone formatter or Kotlin lint plugin is configured.

## Testing Guidelines

Use JUnit 4 for local tests and AndroidJUnit4 for instrumented tests. Espresso and Compose UI testing dependencies are available. Name test classes with a `Test` suffix and methods descriptively, following examples such as `addition_isCorrect`. Add behavior-focused tests for changed logic and UI interactions. No coverage threshold is configured.

## Commit & Pull Request Guidelines

This checkout contains no Git metadata, so historical commit conventions cannot be verified. Use concise imperative subjects, such as `Add greeting screen tests`. Keep changes focused. Pull requests should explain the change, link relevant issues, report validation commands and results, and include screenshots for visible UI changes.

## Configuration Hygiene

Keep SDK paths in ignored `local.properties`. Do not commit credentials, signing keys, generated build output, or local IDE state.
