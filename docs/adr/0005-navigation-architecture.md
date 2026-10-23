# Navigation follows Navigation 3's recipes: state, a Navigator, a section per feature, pure transitions

Until now `MoldaRoute` held every `NavKey`, every entry and every cross-feature flow in one 870-line composable, read together in one place. That made each flow easy to find, but it outgrew the linters: Detekt needed `LongMethod` and `CyclomaticComplexMethod` suppressions, and SonarCloud measured a cognitive complexity of 104 against its limit of 45. The flows could only be tested on a device.

We now follow the conventions of the official Navigation 3 recipes (multiple back stacks, modularized navigation), adapted to one Gradle module:

- **State and commands are separate.** `ui/navigation/TopLevelBackStacks` holds the back stacks and the selected destination and builds the decorated entries; `ui/navigation/Navigator` changes them (`select`, `back`, `push`, `remove`, `popTo`).
- **Keys are a feature's API; sections are its implementation.** A feature's `navigation` package holds its `internal` keys, like the recipe's `api` module, so any feature may navigate to them. Its `ui/<Feature>Navigation.kt` adds an `EntryProviderScope<NavKey>.<feature>Section(...)` that shows only that feature's UI, as `FeatureUiIsolationTest` requires. Sections receive the `Navigator`; screens receive lambdas, never the `Navigator`.
- **The app wires where features meet.** Entries that show two features' UI live in the root package (`SharedEntries.kt`): a saved project's skirt editor and guides, the profile picker over a calculator, and the discard dialog. A section takes a slot when another feature fills part of its screen, as Projetos does for a project's saved result.
- **Cross-feature transitions are pure functions.** `MoldaTransitions.kt` decides what selecting a destination, Back, leaving the example guide or opening an editor does, from the back stack and an `EditorStatus` of unsaved and in-flight work. Each returns a `Transition`, which `MoldaRoute` carries out. Local tests cover every rule.
- **The app shell keeps only wiring.** `MoldaRoute` creates the ViewModels and reads the editor status when the user acts; its `MoldaApp` wires the sections together and carries out transitions. `MoldaShell` holds the navigation suite and `NavDisplay`.

## Considered options

- **Keep one function and suppress the findings.** Rejected: it kept the flows untestable without a device, and every new feature would grow it.
- **Gradle modules per feature with Hilt-multibound entry installers**, as the modular recipe does. Rejected for now: one module makes the `api`/`impl` split and the multibinding pure overhead. Sections already mark where a module boundary would go.

## Consequences

> The three steps below are superseded by [ADR-0006](0006-entry-scoped-viewmodels-results-and-dialogs.md), which records them in the order it carries them out: results, then dialogs, then entry ViewModels.

Three further steps align the rest with the recipes, each its own change:

1. **ViewModels scoped to their entries** (`rememberViewModelStoreNavEntryDecorator`, assisted injection with the key) instead of activity-scoped ViewModels passed into `MoldaRoute`. This splits the shared profile form and the second calculator the project editor uses, and changes the device-test harness, which passes ViewModels in.
2. **Results between entries** instead of shared ViewModels: the picker's chosen profile, a created profile returning to its calculator, and showing a saved project.
3. **Dialogs as entries**: the project save, rename and delete dialogs, and a discard dialog that returns a result instead of carrying its follow-up in its key. This also lets `ProjectsViewModel` drop its `TooManyFunctions` suppression.
