# Contributing

## Install fast local hooks

Install [uv](https://docs.astral.sh/uv/getting-started/installation/) and Git.
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
uv run --no-project --python 3.12 python scripts/test_hooks.py
```

Installation enables both `pre-commit` and `commit-msg` by default. The first run
downloads isolated hook environments; later runs check staged files quickly.
Hooks do not run Gradle or devices. Quality runs hygiene over all tracked files
and exercises the fixtures on Linux; use the same fixture command on Windows.
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
commits. Existing main history is left untouched. Hook fixtures demonstrate all
eleven types, unrestricted scopes, breaking syntax, dependency subjects, and failures.

## Update hook tooling

Verified pins: [pre-commit 4.6.2](https://github.com/pre-commit/pre-commit/releases/tag/v4.6.2),
[pre-commit-hooks 6.0.0](https://github.com/pre-commit/pre-commit-hooks/releases/tag/v6.0.0),
and [conventional-pre-commit 4.4.0](https://github.com/compilerla/conventional-pre-commit/releases/tag/v4.4.0).
Python 3.12 satisfies these tools' runtime requirements.

Dependabot's Gradle/Actions coverage does not update these pins. Review upstream
release notes and compatibility, use `pre-commit autoupdate --freeze`, and verify
each resulting SHA against its release (`git ls-remote <repo> refs/tags/<tag>
refs/tags/<tag>^{}`; annotated tags use the peeled commit). Keep release comments.
Update the CLI version together in this document, `.pre-commit-config.yaml`, and
Quality's install command. Reinstall the CLI with uv, rerun fixtures on Windows
and Linux, all-file hygiene, and the existing Gradle/device checks. Review changes
before committing; keep Actions pinned to full SHAs when updating them.
