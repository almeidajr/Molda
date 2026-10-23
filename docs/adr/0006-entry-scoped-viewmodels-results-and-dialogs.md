# Entries own their ViewModels, return results to the entry that asked, and show dialogs as entries

> Supersedes the three planned steps in the consequences of [ADR-0005](0005-navigation-architecture.md) and the way [ADR-0004](0004-fakes-for-screen-tests-and-idling-aware-room-for-journeys.md) gives screen tests their fakes.

ADR-0005 left the entries tied together through shared ViewModels: every ViewModel was created once for the activity in `MoldaRoute` and passed into the sections, a second calculator served the project editor under a fixed key, and the route called one entry's ViewModel on behalf of another (a picked profile, a created profile, a discard follow-up, a project to show). Two sources said whether a profile editor was open, and device tests had to build the ViewModels by hand to pass them in. We follow Navigation 3's recipes and Now in Android instead: a back stack entry owns its state, and entries talk through navigation, never through each other's ViewModels.

Three changes make that true, in this order, because each removes cross-entry calls that the next would otherwise have to keep working:

1. **Results between entries.** An entry returns a value to the entry that asked with Navigation 3's official event-based result API (`rememberResultEventBus`, the bus's entry decorator, `sendResult` and `ResultEffect`); Molda owns no bus, decorator or effect.
   - One bus serves the app: every top-level back stack's entries are decorated with it, and the app's wiring listens on it for results meant for it.
   - The requester is named by a plain result-key string carried in the key of the entry that returns the result. The profile picker and the picker's profile editor carry the key of the calculator that asked, and each calculator listens on its own (`profile/project/<id>` for a project's editor). The route no longer chooses a target from the selected tab or from the stack holding an editor, so two rules can no longer disagree, and the string survives process death with the back stack.
   - A confirmed discard is a result too: the discard dialog's key states what it confirms, and on confirm it sends that follow-up to the app's wiring, which carries it out through the pure transitions of ADR-0005.
2. **Dialogs as entries.** The project save, rename and delete dialogs are navigation entries, opened by pushing a key that carries what they act on (the snapshot or the project id) and closed by leaving the back stack. The back stack is the only record of which dialog is open, and a dialog survives process death.
   - Each keeps Molda's window-overlay presentation (`WindowOverlaySceneStrategy`, marked with `windowOverlay()`), so it stays a Material dialog with its own window and motion.
   - Success messages go through one snackbar callback that the app hands to every section. It launches where the snackbar host lives, so it outlives the entry that asked.
   - A save that should show the new project navigates there directly, and the save entry builds the default project name from the snapshot's pattern draft kind, so the skirt section carries no pattern name.
3. **Entry-scoped ViewModels.** Navigation 3's ViewModel-store entry decorator (`androidx.lifecycle:lifecycle-viewmodel-navigation3`) scopes each entry's ViewModels to that entry, so leaving it releases its draft for good. Entries create them with Hilt, and where a key carries data, with assisted injection that receives the key, as in the official recipe.
   - Keys carry what their ViewModels need, so none needs a pre-load call: guides carry their radii, the project editor key carries the project id and its snapshot, and the calculator's factory takes an optional starting draft. The second calculator, the "load" command and the fixed key go away. The skirt ViewModel still depends on nothing in the project feature.
   - Profiles split into a list ViewModel and one editor ViewModel per editor entry, so the Perfis editor and the picker's editor are independent. Projects split into a list, one opened project, an update command for the project editor, and one small ViewModel per dialog entry.
   - A successful save or delete closes its editor by removing its key, so the back stack is the only record of an open editor and the effect that reconciled the two is deleted.
   - Each editor entry reports its unsaved, saving and unavailable state into an editor-status holder owned by the app while it is composed. The route reads the holder when the sewist acts, so the pure transitions and their `EditorStatus` are unchanged.
   - **Each ViewModel blocks only its own actions:** an editor during its own save, a list's rows during its own delete, a dialog's buttons during its own command. Room serializes writes, and conflicts surface through existing failures: saving a just-deleted profile fails with "no longer exists", and deleting the profile being edited closes its editor.

Two rules hold across all three steps:

- **Commands take no UI callbacks.** A command returns nothing to its caller. Its outcome (saved with the new id, renamed, deleted, updated, profile saved) is state in the entry's own ViewModel, and the entry reacts in an effect that shows the snackbar, removes its key or navigates, as Android's UI events guidance says. A callback can fire after the composition that made it is gone: a save that finished after recreation reported to a dead composition, and the restored save dialog then stored a duplicate project.
- **Device tests are Hilt Android tests.** A custom test runner starts Hilt's test application. Each test class replaces the production storage and clock bindings with its shared fakes (seeded in their constructors, as ADR-0004 requires) and a fixed clock, and the route takes no ViewModel parameters. Journey tests replace the database binding with ADR-0004's idling-aware Room, so the real repositories run on it. The app module is split as needed so tests can replace storage and the clock separately.

## Considered options

- **Keep activity-scoped ViewModels and add keyed ones for the extra cases.** Rejected: drafts leak between entries (a second project opened after the first, a profile created from the picker over a Perfis edit), and each new feature adds another fixed key and pre-load step.
- **A Molda-owned result bus, or results held in a shared ViewModel.** Rejected: the official API is tested and is the library's intended pattern, and a shared ViewModel would again couple entries and block scoping.
- **A requester type in the key instead of a result-key string.** Rejected: the picker would have to know every kind of calculator, where a string is all the bus needs and is already serializable.
- **The recipe's dialog scene strategy for the project dialogs.** Rejected: it replaces the Material dialog window and its motion.
- **ViewModels first, then results and dialogs.** Rejected: scoping a ViewModel to its entry while other entries still call it breaks those calls.
- **Keep building ViewModels by hand in device tests.** Rejected: nothing but the route could give them fakes, so the app under test differed from production. Hilt test bindings create it exactly as production does.
- **Gradle modules per feature.** Still rejected for the reasons in ADR-0005; entries and keys already mark where a boundary would go.

## Consequences

- No entry calls another entry's ViewModel. A new hand-off adds a result key to a key, never a route callback or a shared state field.
- Whatever a ViewModel needs at creation goes into its entry's key, which stays serializable.
- The route wires navigation, results and transitions only. `ProjectsViewModel`'s split also lets it drop its `TooManyFunctions` suppression.
- Screen tests keep ADR-0004's fakes and receive them through Hilt bindings; journey tests still run real Room on idling-aware threads.
- Because a ViewModel blocks only its own actions, two panes can each have work in flight at once.
