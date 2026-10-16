# CLAUDE.md

This file provides guidance to Claude Code (claude.ai/code) when working with code in this repository.

@AGENTS.md

## Commands

Full local validation (what CI checks), after `.\gradlew.bat spotlessApply`:

```powershell
.\gradlew.bat spotlessCheck :app:detektMain :app:detektTest :app:lintDebug :app:testDebugUnitTest :app:assembleDebug --continue
```

Run a single test:

```powershell
.\gradlew.bat :app:testDebugUnitTest --tests "com.almeidajr.molda.skirt.domain.CalculateSkirtRadiiTest"
.\gradlew.bat :app:connectedDebugAndroidTest "-Pandroid.testInstrumentationRunnerArguments.class=com.almeidajr.molda.SkirtCalculatorTest"
```

Lint treats warnings as errors and there are no Detekt/Lint baselines: fix findings or add a narrow, explained suppression.

## Architecture

Single-activity Compose app organized by feature, each split into `domain` / `data` / `ui`:

- `skirt/` — full-circle skirt calculator. `domain/CalculateSkirtRadii.kt` holds the pure math; `ui/` has the screen, `SkirtCalculatorViewModel`, and the Rive-driven `FabricFoldingTutorial`.
- `profile/` — measurement profiles. `domain/ProfileRepository` is the boundary; `data/LocalProfileRepository` implements it with a hand-written `SQLiteOpenHelper` (no Room) on `Dispatchers.IO`.

Entry flow: `MainActivity` (calls `Rive.init`) → `SkirtCalculatorRoute` → `MoldaRoute.kt`, which owns cross-feature navigation state (profile management, picker, editor, discard dialog) via `rememberSaveable` flags, wires both ViewModels, and switches to two-pane layouts on wide windows / avoids hinges (`AdaptiveContent.kt`).

## Rive animation

`animations/` is a separate Rive CLI project with its own `AGENTS.md` (read it before editing there). Its build output `build/animations.riv` is copied manually to `app/src/main/res/raw/fabric_folding.riv`; keep the two in sync when changing the animation.

## Docs

Product and design rationale live in `docs/` (e.g. `fabric-folding-spec.md`, `ui-design-system.md`, `roadmap.md`) and domain terms in `GLOSSARY.md`. Specs and validation evidence live in GitHub Issues, not in the repo.
