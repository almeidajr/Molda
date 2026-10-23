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

- `skirt/` — full-circle skirt calculator. `domain/CalculateSkirtRadii.kt` holds the pure math; `ui/` has the screen, `SkirtCalculatorViewModel`, and the Rive-driven `FabricFoldingGuide`.
- `profile/` — measurement profiles. `domain/ProfileRepository` is the boundary; `data/LocalProfileRepository` implements it (a `Flow` query plus commands returning `Result`); `ui/` has a `ProfileListViewModel` per list entry (Perfis, each picker) and a `ProfileEditorViewModel` per editor entry.
- `project/` — saved projetos: a named snapshot of a pattern draft's measurements, assumptions and stored radii, never recalculated on open. `domain/ProjectRepository` is the boundary, `data/LocalProjectRepository` implements it, and `ui/ProjectsViewModel` owns the list and the save, rename and delete dialogs.

Both repositories share one Room 3 database, `data/room/MoldaDatabase.kt` (`molda.db`), opened in `di/DatabaseModule`. The schema is exported to `app/schemas/`; every schema change needs a real `Migration`, never destructive fallback (`docs/adr/0002`).

Entry flow: `MainActivity` (calls `Rive.init`) → `MoldaRoute.kt`, following Navigation 3's recipes (`docs/adr/0005`; `docs/adr/0006` for results, dialogs and ViewModels):

- `ui/navigation/TopLevelBackStacks.kt` holds one back stack per destination (Moldes, Projetos, Guias, Perfis), and `ui/navigation/Navigator.kt` changes them.
- Each feature's `navigation` package holds its `internal` `NavKey`s (its API: any feature may navigate to them); its `ui/<Feature>Navigation.kt` adds an `EntryProviderScope<NavKey>.<feature>Section(...)` showing only that feature's UI. Entries where two features' UI meet live in the root `SharedEntries.kt`.
- Entries return results to the entry that asked through Navigation 3's result API, addressed by the result-key string their key carries; project dialogs are window-overlay entries; entries own their ViewModels; commands take no UI callbacks, the entry reacts to its ViewModel's state (`docs/adr/0006`).
- `MoldaTransitions.kt` decides cross-feature transitions (discard prompts, re-selecting a destination, Back over an editor) as pure functions, tested locally.
- `MoldaRoute` wires the sections, the shared overlays (profile picker, discard dialog) and the transitions, deciding each from the status of the editor the action concerns; `MoldaShell.kt` hosts the navigation suite and one `NavDisplay`, which shows list-detail panes on wide windows and avoids hinges (`ui/adaptive/AdaptiveContent.kt`).

## Rive animation

`animations/` is a separate Rive CLI project with its own `AGENTS.md` (read it before editing there). Its build output `build/animations.riv` is copied manually to `app/src/main/res/raw/fabric_folding.riv`; keep the two in sync when changing the animation.

## Docs

Product and design rationale live in `docs/` (e.g. `fabric-folding-spec.md`, `ui-design-system.md`, `roadmap.md`) and domain terms in `GLOSSARY.md`. Specs and validation evidence live in GitHub Issues, not in the repo.
