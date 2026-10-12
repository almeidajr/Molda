# Contributing

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
Run the [existing Gradle checks](README.md#development) before submitting work.

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
Authenticated inspection of `repos/almeidajr/Molda/actions/policies` returned zero
policies on 2026-10-12; an explicit allowance was not verified. Repository files
alone do not enable this policy or register required checks. After an authorized
merge/deployment, verify the allowance, observe successful **PR title**/**PR
commits** runs, and add those exact names to main protection in #4. Exercise
invalid/corrected title edits and intermediate commits on normal, fork, and
Dependabot PRs; verify read-only permissions and merge blocking. These live runs
remain blocked until deployment; no workflow or settings were published as part
of the local implementation.
