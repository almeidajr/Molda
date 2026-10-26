# Beta builds and releases update themselves from GitHub Releases

**Status:** accepted and built: the check from About (#209), the download and installation (#210), the install-permission flow (#211), the unsaved-changes warning (#212) and the startup notice (#213); spec #208.

Molda is distributed only as signed APKs on GitHub Releases ([ADR-0001](0001-builds-signed-in-ci.md)) and will not move to Google Play, so nothing tells a tester or sewist that an update exists. Molda checks the repository's releases itself, then downloads and installs the update in the app, keeping profiles and projects because every APK carries the same signature. We chose in-app install over opening the release page in the browser so that updating takes one confirmation instead of a trip through downloads. That needs the `INTERNET` and `REQUEST_INSTALL_PACKAGES` permissions, which Play's policy would reject for a self-updating app; this is acceptable only because Play is ruled out.

- **Channel.** The installed version decides what is offered (see **Update** in `GLOSSARY.md`): a beta build is offered newer beta builds and releases, a release only newer releases. Versions are compared by SemVer precedence of the tag against `versionName`, because the `versionCode` (the CI run number) is not visible on a GitHub Release.
- **Only CI builds check.** Debug and local builds (`0.0.0-dev`) never check: they are signed with another key, so the installation would fail.
- **Platform services do the heavy work.** The system `DownloadManager` downloads the APK, and Molda finds it again by its file name after process death, storing no state. Molda checks the APK's SHA-256 against the digest GitHub publishes for the asset, then installs through a `PackageInstaller` session, only while it is in the foreground and after the discard prompt when an editor has unsaved changes.
- **Installation follows ADR-0006.** About and the startup notice request an installation as a result on one result key, which the app's wiring turns into the discard prompt or an installation entry on the back stack. The installation entry owns the wait on the "install unknown apps" permission, so returning from Molda's setting continues the installation even after process death. The repository keeps the offered update for the app's lifetime, and one effect outside the entries follows its download while Molda is in the foreground, so the installation starts on whichever screen shows when the download finishes.

## Consequences

- The release workflow's asset name (`molda-vX.Y.Z.apk`) and tag format are now a contract with installed apps: renaming the asset or changing the tag scheme stops existing installations from finding updates.
- Moving to Google Play would require removing this feature from the Play build first.
