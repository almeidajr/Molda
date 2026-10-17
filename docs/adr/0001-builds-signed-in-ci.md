# Beta builds and releases are signed in CI with one long-lived key

Pushing a `v*.*.*` tag makes `.github/workflows/release.yml` build a release APK, sign it with a keystore held in GitHub Secrets, and publish it as a GitHub Release. A tag with a suffix (`v1.0.0-beta.1`) is published as a pre-release, which is a beta build. A plain tag (`v1.0.0`) is published as a release (see `GLOSSARY.md`). Testers keep measurement profiles on their devices, and Android only installs an update over the existing app, keeping its data, when the update carries the same signature. Losing or rotating the key would force testers to uninstall and lose those profiles. So the key never changes, and a backup is kept outside GitHub. The workflow passes in the version (the tag becomes `versionName`, the run number becomes `versionCode`) rather than a plugin or hand edits, which follows common Android practice. We chose CI signing over building on a local machine so that every distributed APK comes from a tagged commit.

## Consequences

- The workflow cannot check release gates. People must not push a plain tag until the gates in `docs/roadmap.md` pass.
- If Molda is later published on Google Play under the same application ID, Play App Signing must use this key, or existing testers cannot update in place.
