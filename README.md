# Molda

Kotlin Android app using Jetpack Compose and Material 3.

## Development

Install Android SDK 37 and a JDK capable of launching Gradle (JDK 25 recommended).
The committed daemon criteria select JDK 25. Keep your SDK path in ignored
`local.properties`; CI uses the Android SDK environment. Devices need API 36+.

Run from the repository root in PowerShell:

```powershell
.\gradlew.bat spotlessApply
.\gradlew.bat spotlessCheck :app:detekt :app:lintDebug :app:testDebugUnitTest :app:assembleDebug --continue
.\gradlew.bat :app:connectedDebugAndroidTest
```

On macOS/Linux use `bash ./gradlew`. Device tests require a running emulator or
connected device. CI runs quality checks and one API 36 emulator on every PR
and push to main; reports are retained for seven days. Configure branch protection
to require `Quality` and `Device tests (API 36)` after their first CI run.

## Conventions

Spotless + ktlint and `.editorconfig` define formatting, including Kotlin build
scripts. Apply formatting locally; CI only checks it. Detekt handles Kotlin
analysis without its optional formatting rules, and Android Lint treats warnings
as errors. Only dependency update notices (`GradleDependency` and
`NewerVersionAvailable`) are disabled: Dependabot handles reviewed upgrades,
so a newly published release cannot break an unchanged PR. No baselines:
fix findings or explain a narrow suppression.
Detekt currently uses a pinned 2.x alpha for the Gradle 9/JDK 25 toolchain;
review compatibility when updating it.

Use JUnit 4 in `app/src/test` for logic and AndroidJUnit4/Compose tests in
`app/src/androidTest` for UI behavior. Name classes `*Test` and methods after
the behavior being checked. Cover branches and regressions; there is no coverage
percentage gate. The arithmetic template is temporary scaffolding, not app coverage;
replace it when application logic exists. The device smoke test checks the launch UI.

Use `gradle/libs.versions.toml` for dependency/plugin versions and the Compose
BOM for Compose libraries. The settings-only Foojay plugin remains in
`settings.gradle.kts` because settings plugins resolve before the project catalog.
Dependabot opens weekly Gradle and Actions updates; review and merge manually.
Coordinate AGP, Gradle, Kotlin, JDK, and Detekt upgrades and run both CI jobs.
Keep the wrapper distribution checksum updated when upgrading Gradle.

Keep IDE state, SDK paths, build output, credentials, and signing keys out of Git.
Git normalizes source files to LF and Windows batch files to CRLF.
Use imperative commit subjects and describe validation in PRs; include screenshots
when changing the UI.
