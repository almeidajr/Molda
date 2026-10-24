# Screen tests use repository fakes, and journey tests run Room on idling-aware threads

> The harness detail of how screen tests receive the fakes (ViewModels built on them and passed into the route) is superseded by [ADR-0006](0006-entry-scoped-viewmodels-results-and-dialogs.md): tests receive the fakes through Hilt test bindings. The fakes and the idling-aware Room stay. Since `TestBindingsModule` replaces `RepositoryModule` in every device test, journey tests build the production repositories with `RepositoryModule`'s functions on their `IsolatedStorage`, so Hilt's install of that module is checked only at compile time.

Molda's device tests polled the UI with about 60 `waitUntil` calls and scattered 5 to 15 second timeouts. Most of them waited for Room writes: Room runs its queries on its own executor, which Compose's test synchronization cannot see, so a saved or deleted profile reached the screen at an unknown time. The tests were slow, hard to read and flaky. We removed the cause instead of hiding it behind helpers. Screen tests run on in-memory fakes of `ProfileRepository` and `ProjectRepository`, the existing repository boundary, so a save or a delete shows up in the UI within the same Compose frame. End-to-end journey tests keep the real database, but open it with a query dispatcher backed by an executor Espresso tracks, registered as an idling resource, so Compose waits for Room by itself. `openMoldaDatabase` gains an optional query coroutine context parameter for this; it defaults to Room's own, so production behavior does not change.

## Considered options

- **Keep polling, but through named helpers with one shared timeout.** Rejected: it renames the wall-clock wait without removing it, so tests stay slow and still flake under load.
- **Real Room with an idling resource in every device test.** Rejected: screen tests would pay for database setup and isolation, and would verify Room again instead of the screen. Room is already covered by the repository and migration tests.
- **Fakes everywhere, journeys included.** Rejected: no device test would exercise the wiring from the UI through the `di` modules to the real database, which is what journey tests exist for.

## Consequences

- The fakes are shared by local and device tests. Each takes its seed data in its constructor, exposes its current contents synchronously and can be told to fail.
- Screen tests seed and assert through the fake and need no coroutines.
- Room behavior is verified by the repository and migration tests on `runTest`; screen tests do not prove persistence.
- The only remaining wall-clock waits are for state Compose cannot see (keyboard, window focus, activity finishing, native Rive frames), each through a named helper. The rules live in `.agents/rules/kotlin-tests.md`.
