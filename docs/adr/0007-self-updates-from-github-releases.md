# Beta builds and releases update themselves from GitHub Releases

**Status:** accepted, not yet built (spec: #208).

Molda is distributed only as signed APKs on GitHub Releases ([ADR-0001](0001-builds-signed-in-ci.md)) and will not move to Google Play, so nothing tells a tester or sewist that an update exists. Molda checks the repository's releases itself, then downloads and installs the update in the app, keeping profiles and projects because every APK carries the same signature. We chose in-app install over opening the release page in the browser so that updating takes one confirmation instead of a trip through downloads. That needs the `INTERNET` and `REQUEST_INSTALL_PACKAGES` permissions, which Play's policy would reject for a self-updating app; this is acceptable only because Play is ruled out.

- **Channel.** The installed version decides what is offered (see **Update** in `GLOSSARY.md`): a beta build is offered newer beta builds and releases, a release only newer releases. Versions are compared by SemVer precedence of the tag against `versionName`, because the `versionCode` (the CI run number) is not visible on a GitHub Release.
- **Only CI builds check.** Debug and local builds (`0.0.0-dev`) never check: they are signed with another key, so the install would fail.
- **Platform services do the heavy work.** The system `DownloadManager` downloads the APK, and Molda finds it again by its file name after process death, storing no state. Molda checks the APK's SHA-256 against the digest GitHub publishes for the asset, then installs through a `PackageInstaller` session, only while it is in the foreground and after the discard prompt when an editor has unsaved changes.

## Consequences

- The release workflow's asset name (`molda-vX.Y.Z.apk`) and tag format are now a contract with installed apps: renaming the asset or changing the tag scheme stops existing installs from finding updates.
- Moving to Google Play would require removing this feature from the Play build first.
