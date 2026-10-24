# Contributing

## Development

Install Android SDK 37 and a JDK capable of launching Gradle (JDK 25 recommended).
The committed daemon criteria select JDK 25. Keep your SDK path in ignored
`local.properties`; CI uses the Android SDK environment. Devices need API 36+.

Run from the repository root in PowerShell:

```powershell
.\gradlew.bat spotlessApply
.\gradlew.bat spotlessCheck :app:detektMain :app:detektTest :app:lintDebug :app:testDebugUnitTest :app:assembleDebug --continue
.\gradlew.bat :app:connectedDebugAndroidTest
```

On macOS/Linux use `bash ./gradlew`. Device tests require a running emulator or
connected device. CI runs quality checks and one API 36 emulator on every PR
and push to main; reports are retained for seven days. Main protection also requires
`PR title` and `PR commits`; see [repository governance](#repository-governance).

## Code quality and dependencies

Spotless + ktlint and `.editorconfig` define formatting, including Kotlin build
scripts. Apply formatting locally; CI only checks it. Detekt runs with type resolution for Kotlin
analysis without its optional formatting rules, and the Kotlin compiler and
Android Lint (all checks, including tests) treat warnings as errors; Kotlin also
reports its extra checks (`-Wextra`), such as unused variables. Lint keeps its
dependency and Gradle update notices on: when a newer release is published,
update the version (and the wrapper checksum for Gradle) instead of suppressing
the notice. `SyntheticAccessor` is disabled because R8 inlines those accessors. No baselines:
fix findings or explain a narrow suppression.
Detekt currently uses a pinned 2.x alpha for the Gradle 9/JDK 25 toolchain;
review compatibility when updating it.
Compose (through the alpha Compose BOM, which also drives Material 3 and Material
adaptive), Navigation 3, lifecycle, window and activity follow their newest
prereleases; return them to stable channels once motion is settled.

Use JUnit 4 in `app/src/test` for logic and AndroidJUnit4/Compose tests in
`app/src/androidTest` for UI behavior. Name classes `*Test` and methods after
the behavior being checked. Cover branches and regressions; there is no coverage
percentage gate.

Use `gradle/libs.versions.toml` for dependency/plugin versions and the Compose
BOM for Compose libraries. The settings-only Foojay plugin remains in
`settings.gradle.kts` because settings plugins resolve before the project catalog.
Dependabot opens weekly Gradle and Actions updates; review and merge manually.
Coordinate AGP, Gradle, Kotlin, JDK, and Detekt upgrades and run both CI jobs.
Keep the wrapper distribution checksum updated when upgrading Gradle.

Keep IDE state, SDK paths, build output, credentials, and signing keys out of Git.
Git normalizes source files to LF and Windows batch files to CRLF.
Describe validation in PRs; include screenshots for UI changes, and follow
[ACCESSIBILITY.md](ACCESSIBILITY.md#contributor-expectations).


## Install fast local hooks

Install [uv](https://docs.astral.sh/uv/getting-started/installation/), Git, and
[Node.js](https://nodejs.org/) 22.12 or newer for manual CLI use.
uv manages the Python runtime and CLI outside this Android project; no Python
project or lockfile is needed. On Windows/PowerShell:

```powershell
winget install --id astral-sh.uv --exact
uv tool install --python 3.12 pre-commit==4.6.2
uv tool update-shell
```

On macOS/Linux:

```sh
curl -LsSf https://astral.sh/uv/install.sh | sh
uv tool install --python 3.12 pre-commit==4.6.2
uv tool update-shell
```

Restart your terminal so the tools are on PATH. From the repository root, on
either platform:

```sh
pre-commit install --install-hooks
pre-commit run --all-files --show-diff-on-failure
pre-commit run check-all-large-files --hook-stage manual --all-files
```

Installation enables both `pre-commit` and `commit-msg` by default. The first run
downloads isolated hook environments; later runs check staged files quickly.
Hooks do not run Gradle or devices. Quality runs hygiene over all tracked files.
CI uses pinned uv 0.13.0 and `uvx --python 3.12 pre-commit@4.6.2`;
local Git hooks use the persistent `uv tool install` executable.
Run the [existing Gradle checks](#development) before submitting work.

The hygiene hooks trim trailing whitespace (preserving Markdown hard breaks),
fix final newlines, parse YAML, reject merge-conflict markers, reject newly added
files larger than 500 KiB (512,000 bytes), and detect private-key markers. CRLF
batch files retain their line endings; no file exclusions are needed. The size
local hook checks staged additions, including during all-file runs. Quality also
runs the manual size hook over committed files so a hook bypass cannot introduce
oversized files. Private-key detection is not a general secret scanner.

If a fixer changes files, review the diff, stage the corrections, and retry the
commit. For a rejected message, edit and retry with
`git commit --edit --file=.git/COMMIT_EDITMSG`. Resolve YAML/conflict/size/key
errors at their source; never commit real keys. A hook bypass does not bypass CI.

## Commit messages

Every new contributor commit uses [Conventional Commits 1.0](https://www.conventionalcommits.org/en/v1.0.0/):
`type(scope)!: imperative subject`. Scope and `!` are optional. Allowed types:
`feat`, `fix`, `refactor`, `perf`, `test`, `docs`, `build`, `ci`, `chore`, `style`, `revert`.
Scopes have no allowlist; prefer short lowercase areas such as `ui`, `theme`,
`build`, `ci`, `deps`, and `deps-dev`. Examples:

```text
feat(ui): add settings screen
fix(theme): restore dark mode contrast
test: check greeting on launch
build(deps): bump AndroidX Compose
build(deps-dev): update ktlint
ci: check repository hygiene
```

Use `!` for an incompatible change and explain its impact and migration in the
body. A `BREAKING CHANGE:` footer is also accepted:

```text
feat(ui)!: replace saved navigation routes

BREAKING CHANGE: old saved routes are discarded; reopen the desired screen.
```

Strict validation rejects missing/unsupported types, empty subjects, and
nonconventional `Merge`, `fixup!`, `squash!`, and `amend!` messages. Finish autosquash
before submitting commits. Rebase onto main instead of making default merge
commits. Existing main history is left untouched. Types must be lowercase;
subjects may use capitals and punctuation. There are no scope or message-length
allowlists. Local hooks and both PR checks share `commitlint.config.cjs`.

## Update hook tooling

Verified pins: [pre-commit 4.6.2](https://github.com/pre-commit/pre-commit/releases/tag/v4.6.2),
[pre-commit-hooks 6.0.0](https://github.com/pre-commit/pre-commit-hooks/releases/tag/v6.0.0),
and [commitlint 21.2.3](https://github.com/conventional-changelog/commitlint/releases/tag/v21.2.3).
Python 3.12 satisfies pre-commit; commitlint requires Node 22.12 or newer.

Dependabot's Gradle/Actions coverage does not update these pins. Review upstream
release notes and compatibility, use `pre-commit autoupdate --freeze`, and verify
each resulting SHA against its release (`git ls-remote <repo> refs/tags/<tag>
refs/tags/<tag>^{}`; annotated tags use the peeled commit). Keep release comments.
Update the CLI version together in this document, `.pre-commit-config.yaml`, and
Quality's install command. Update both exact commitlint package versions together
in `.pre-commit-config.yaml` and the PR workflow. Reinstall the CLI with uv,
verify accepted/rejected messages on Windows and Linux, run all-file hygiene,
and the existing Gradle/device checks. Review changes
before committing; keep Actions pinned to full SHAs when updating them.

## PR message checks

Use the same Conventional Commit policy for PR titles and every PR commit,
including Dependabot and fork contributions. Breaking PRs require `!` in the
title and an impact/migration explanation in the description; commits may also
use a `BREAKING CHANGE:` footer. Fill in the short PR template with the change,
related issues, validation results, and UI evidence when applicable.

The stable required checks are **PR title** and **PR commits**. Opening,
reopening, editing, and pushing/rebasing a PR reruns both checks, including drafts.
An invalid intermediate commit fails even when the title and last commit pass.
Edit a rejected title; amend/rebase rejected commits locally and rerun hooks,
then push the corrected history. Agents need an explicit user request before
any commit or push, including `--force-with-lease`. Existing main history is
excluded using the complete `base..head` Git range. Its count must agree with
GitHub's PR commit count; retrieval errors, shallow history, mismatches, and
base/head changes fail closed. Git retrieval avoids the 250-commit REST list limit.

Both checks and the local hook use `@commitlint/cli` and
`@commitlint/config-conventional` 21.2.3 with default message exemptions disabled.
No custom parser or runner is needed. The config retains eleven conventional
types and rejects whitespace-only subjects, merge/fixup/squash/amend messages;
scope, subject case/punctuation, and message-length restrictions are disabled.
Hook environments install Node 24.21.0 and the two pinned packages without a
Node project or lockfile. CI uses the same Node version and installs packages
into a temporary directory.

After changing the policy, verify all eleven types, optional Unicode/compound
scopes, `!`, breaking footers, and Dependabot subjects through the local
`commit-msg` hook. Verify malformed/empty subjects and an invalid intermediate
commit fail; repeat the same cases through PR checks after deployment. Policy
migration changes uppercase types and editor-comment headers to rejected input;
use lowercase types and clean submitted messages.

The metadata workflow uses `pull_request_target` so its workflow and config come
from the trusted default branch, including for forks and Dependabot. It checks
out only trusted `github.sha`, never PR code, persists no Git credentials, grants
only contents/PR read access, and passes metadata through JSON/files rather than
shell interpolation. PR Git objects are fetched only to read their messages;
the PR tree is never checked out or executed. GitHub workflow commands are
disabled while commitlint displays contributor text. It requests no secrets.
Do not change its checkout to a PR head or merge ref.

Deployment requires an explicit Actions event-policy allowance for this
metadata-only `pull_request_target` workflow. GitHub's public-repository default
may block it: see [the event policy](https://docs.github.com/en/actions/reference/security/securely-using-pull_request_target).
The scoped [metadata event policy](https://github.com/almeidajr/Molda/settings/actions/rules/7293)
was configured and verified active on 2026-10-12. Repository files alone do not
enable this policy or register required checks. After changing the workflow,
verify the allowance, observe successful **PR title**/**PR commits** runs,
and register those exact names in main protection. Exercise
invalid/corrected title edits and intermediate commits on normal, fork, and
Dependabot PRs; verify read-only permissions and merge blocking. Record live
validation evidence and any access limitations in the implementation issue.

## Publishing a beta build or release

Both are signed APKs published as GitHub Releases (see `GLOSSARY.md` and
[ADR 0001](docs/adr/0001-builds-signed-in-ci.md)). The tag names the release
being prepared. A [semver](https://semver.org/) `-beta.N` suffix makes it a beta
build, published as a pre-release. From an up-to-date main whose CI passed:

```sh
git tag v1.0.0-beta.1
git push origin v1.0.0-beta.1
```

Increase `N` for each beta build. Push the plain tag (`v1.0.0`) only after the
release gates in `docs/roadmap.md` pass; the workflow cannot check them.

The `Release` workflow uses the tag without its `v` as `versionName`, and the
workflow run number as `versionCode`. It signs the APK with the
`RELEASE_KEYSTORE_BASE64`, `RELEASE_KEYSTORE_PASSWORD`, and `RELEASE_KEY_ALIAS`
secrets and attaches `molda-<tag>.apk`. Install the APK and smoke-test the
calculator, profiles, and folding guide before sending testers the link.
Record which beta build each practical trial used in its issue.
Never replace the signing key: testers could only install a build signed with a
new key after uninstalling, which deletes their profiles. Keep a backup of the
keystore and its password outside GitHub.

## Repository governance

Repository files provide CI, message validation, hooks, ignore rules, and weekly
Gradle/Actions Dependabot updates. Main protection, merge options, Actions defaults,
the metadata event allowance, and security features are live GitHub settings;
cloning or updating these files does not configure them. Hook pins have their own
[maintenance procedure](#update-hook-tooling).

Submit changes through a PR with conventional messages for every new commit and
its title. Rebase onto current main when the branch is outdated, rerun local checks,
and wait for all required checks on the updated PR. Agents require an explicit
user request before committing or pushing, including `--force-with-lease` after a
rebase. Passing, up-to-date PRs need no reviewer approval. Auto-merge is enabled:
`gh pr merge --squash --auto` merges once every required check passes, and GitHub
deletes the head branch after merging. Use squash merge and
retain the validated PR title as the final subject; GitHub still lets a maintainer
edit that subject, so title validation cannot guarantee the final commit message.
The default squash body is the PR description.

### Manual GitHub setup and verification

Use an account with repository administration access and credentials permitted
to read and update rulesets, repository/Actions settings, and security features.
Organization or enterprise policies can limit these options. Record any denied
setting or unavailable feature in the implementation issue instead of treating
documentation as deployment evidence.

1. Inspect authenticated settings and existing rulesets/branch protection first.
   Observe successful **Quality**, **Device tests (API 36)**, **PR title**, and
   **PR commits** check runs, plus the SonarQube Cloud app's **SonarCloud Code
   Analysis**, before registering their exact names. Verify the
   trusted metadata workflow's scoped
   [event allowance](https://github.com/almeidajr/Molda/settings/actions/rules/7293)
   permits `pull_request_target`; #3 recorded that allowance active on 2026-10-12.
2. In Settings → Rules → Rulesets, reconcile protection into one active ruleset
   targeting `main`. Require a PR with zero approvals, the four checks from the
   GitHub Actions source and **SonarCloud Code Analysis** from the SonarQube Cloud
   app, and branches up to date before merging. Add a code scanning rule for the
   SonarCloud tool that blocks new alerts at **Errors** and security alerts at
   **High or higher**; it shows as a rule, not a required check. Block deletion
   and force pushes, and leave the bypass list empty so routine administrator
   actions also require a passing PR. Remove redundant overlapping protection
   and omit a separate commit-metadata ruleset.
3. In Settings → General → Pull Requests, enable only squash merging. Set the
   default squash subject to the PR title and body to the PR description. Allow
   auto-merge and automatically delete head branches.
4. In Settings → Actions → General, set default workflow permissions to read
   repository contents and packages; disable Actions creating or approving PRs.
   Workflows request only the additional permissions they need. Preserve trusted
   checkout behavior and full-SHA action pins when maintaining workflows.
5. In Settings → Security, enable Dependabot alerts and security updates, secret
   scanning, and push protection. Retain the existing Gradle/Actions update
   configuration and ignore rules. Private-key hooks and supported secret
   patterns cannot detect every credential; keep credentials and signing keys
   out of Git even when these checks pass.
6. Re-read authenticated settings after saving. On disposable PRs, verify failed
   and missing required checks block merging, an outdated branch must be updated,
   and a valid up-to-date PR can squash with no approval. Inspect the merge dialog
   for the title/body defaults. Confirm the main ruleset applies to administrator
   actions without a bypass; use read-only rule inspection and disposable PRs,
   never force-push or delete main to test protection. Record PR/check links,
   settings evidence, and any untested scenarios in #4.

Configuration readback and merge-behavior tests are separate evidence. A configured
ruleset alone does not establish that every scenario in step 6 passed; governance
verification remains pending for any scenario without live evidence in #4.
