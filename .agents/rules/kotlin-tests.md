---
paths:
  - "app/src/test/**"
  - "app/src/testFixtures/**"
  - "app/src/androidTest/**"
---

# Kotlin tests

These rules cover every Kotlin test: local JVM tests in `app/src/test` and device
tests in `app/src/androidTest`. The reasoning behind fakes and idling-aware Room
is in [ADR 0004](../../docs/adr/0004-fakes-for-screen-tests-and-idling-aware-room-for-journeys.md).

Write tests for valuable observable behavior, not coverage. Before adding a
test, identify the regression or incorrect behavior it would catch. Prioritize
business rules, non-trivial branches, boundaries, error handling, invariants,
state transitions, and regressions. Trivial getters, constructors, delegation,
and framework behavior do not need tests merely because code changed.

**Test at the useful boundary.** Exercise the public API or the user-visible UI.
Assert returned values, state, visible content, semantics, navigation, or stored
data at the repository boundary, never private methods or internal call order.
One test covers one behavioral concept; multiple assertions are fine when they
describe that outcome. Use minimal, relevant arrange data and reuse existing
helpers.

**Match Molda's stack.** JUnit 4 is the runner in both source sets; do not move
to JUnit 5. Use `app/src/test` for isolated JVM logic and AndroidJUnit4 in
`app/src/androidTest` when Android runtime behavior matters, with the Compose
testing APIs for UI. Keep pure business logic testable without an emulator.

| Test kind                         | Data layer                                                           | Coroutines                      |
|-----------------------------------|----------------------------------------------------------------------|---------------------------------|
| Domain and ViewModel (local)      | Shared fakes                                                         | `runTest`, `MainDispatcherRule` |
| Repository and migration (device) | Real Room                                                            | `runTest`                       |
| Screen (device)                   | Shared fakes bound through Hilt                                      | None needed                     |
| Journey, end to end (device)      | Production repositories from `RepositoryModule` on idling-aware Room | None needed                     |

## Names and structure

Name classes `<Subject>Test`. Name methods `subject_condition_outcome` with
underscores, for example `readValue_whenSensorIsOffline_returnsLastKnownReading`;
no backtick names, so Gradle `--tests` filters stay simple. Group tests by
cohesive subject; one class per production class is not required.

Mark the phases with `// Given`, `// When` and `// Then` comments:

- Omit `// Given` when the test has no setup.
- Write `// When/Then` when the action is the assertion's argument.

```kotlin
class TemperatureCalibrationTest {
    @Test
    fun calibrate_withOffsetBelowMinimum_clampsToMinimum() {
        // Given
        val reading = 2.0

        // When
        val calibrated = calibrate(reading = reading, offset = -5.0, minimum = 0.0)

        // Then
        assertEquals(0.0, calibrated, absoluteTolerance = 1e-9)
    }

    @Test
    fun calibrate_withZeroOffset_keepsReading() {
        // When/Then
        assertEquals(2.0, calibrate(reading = 2.0, offset = 0.0, minimum = 0.0), absoluteTolerance = 1e-9)
    }
}
```

The example illustrates naming and structure; it does not introduce an app API.
For repeated cases, use JUnit 4 parameterized tests when they reduce
duplication and keep clear failure names.

## Assertions

Use `kotlin.test` (`assertEquals`, `assertTrue`, `assertIs`, `assertFailsWith`,
...) in both source sets, as Now in Android does; Detekt rejects `org.junit.Assert`
imports. JUnit 4 stays the runner.
Floating-point comparisons name their tolerance with `absoluteTolerance`, so the
accepted precision is explicit.

## Test doubles

Prefer real value objects and small fakes; mock only when the interaction itself
is the contract
([test doubles](https://developer.android.com/training/testing/fundamentals/test-doubles)).
Fakes sit at the repository boundary: they implement `ProfileRepository` and
`ProjectRepository` and are shared by local and device tests through the test
fixtures in `app/src/testFixtures` (package `com.almeidajr.molda.testing`), not
copied into single test files. A fake takes its seed data in its constructor, exposes its
current contents synchronously for assertions, and can be told to fail for
failure-path tests. A mocked database or serializer does not verify that
integration; Room behavior is covered by the repository and migration tests on
real Room.

## Hilt device tests

Every device test that launches `MainActivity` is a Hilt Android test:
`MoldaTestRunner` starts Hilt's test application, and the app builds its
ViewModels from the test's bindings exactly as in production
([ADR 0006](../../docs/adr/0006-entry-scoped-viewmodels-results-and-dialogs.md)).

- Annotate the class `@HiltAndroidTest`, make `HiltAndroidRule(this)` the first
  rule, `@get:Rule(order = 0)`, and `createLaunchLaterComposeRule()` the second,
  with every other rule ordered after them: the activity injects in `onCreate`.
- No class binds or uninstalls modules. `TestBindingsModule` gives every device
  test's app the repositories and clock held by `TestBindings`: empty fakes, Molda up to date
  and 13/10/2026 unless the test seeds others. One call seeds them and starts
  the app: `launchApp(profiles, projects, clock, size, frame, updates)`, every
  argument optional. Keep a reference typed as the fake when the test sets a gate or
  reads `current`. Starting the app freezes the holder, so assigning a fake
  afterward throws.
- `launchApp` shows the route inside `frame` (the theme by default) and at
  `size` when given; `frame = null` keeps the app's own content, which alone
  shows the installed version. `showRoute` changes the frame later, and
  `recreate()` shows the same content again, so a test never shows it again
  after recreating. Content set with `activity.setContent` directly is not shown
  again after recreating; use `showRoute`.
- Journey tests launch on the production repositories built on their
  `IsolatedStorage` (`RepositoryModule.projectRepository(storage.database)`),
  `IdlingDatabaseModule`'s idling-aware Room. Inject the storage
  (`@Inject lateinit var`, `hiltRule.inject()` in `@Before`) and remove it in
  `@After` with `storage.remove(composeRule.activityRule.scenario)`.

## Coroutines and flows

Follow [Testing Kotlin coroutines](https://developer.android.com/kotlin/coroutines/test)
and [Testing flows](https://developer.android.com/kotlin/flow/test).

- Run coroutine tests with `runTest`, which skips delays on virtual time. Detekt
  rejects `runBlocking` in test sources.
- ViewModel tests replace the main dispatcher with the shared `MainDispatcherRule`
  (after Now in Android), never with handwritten `setMain`/`resetMain`. It takes
  a `TestDispatcher` and defaults to `UnconfinedTestDispatcher`, so simple state
  assertions need no manual scheduling.
- Pass a `StandardTestDispatcher` when the test checks ordering or an
  intermediate state, such as "loading while saving", and advance it explicitly
  with `advanceUntilIdle` or `runCurrent`.
- Device tests do not replace the main dispatcher.
- When the test is about resulting state, start a collector in `backgroundScope`
  (so `WhileSubscribed` flows stay active) and assert `StateFlow.value`.
- When the test is about the sequence of emissions or one-off events, use
  [Turbine](https://github.com/cashapp/turbine) and consume every emission.
  Turbine's timeout is wall-clock time: never use it to wait for state.

## Waiting and synchronization

Control clocks, randomness, external input and scheduling when behavior depends
on them. Never sleep: Detekt rejects `Thread.sleep`, `SystemClock.sleep` and
`CountDownLatch.await` in test sources. Pick the first option that works
([Compose synchronization](https://developer.android.com/develop/ui/compose/testing/synchronization),
[test stability](https://developer.android.com/training/testing/instrumented-tests/stability)):

1. **Compose's automatic synchronization.** Assertions and actions already wait
   for recomposition, layout and Compose-driven work to go idle. Calls that
   bypass Compose do not wait: real key events, activity recreation and
   `runOnUiThread` act on the UI from before the last action. Use the shared
   `pressKey`, `pressBack` and `recreate` helpers (`MoldaNavigation.kt`), and
   use `runOnIdle`, which waits for idle first, to change state on the UI
   thread yourself. Detekt rejects Espresso's `pressBack` in test sources.
2. **`mainClock` control** for anything Compose drives, such as animations:
   `mainClock.autoAdvance = false`, `advanceTimeBy`, `advanceTimeUntil`. Prefer
   virtual time to `waitUntil`.
3. **An idling resource** for background work in tests that run the real stack.
   Journey tests open Room with a query dispatcher on an Espresso-tracked
   executor, so Compose waits for database work by itself.
4. **Named `waitUntil*` helpers**, only for state outside Compose: keyboard
   visibility, window focus, an activity finishing, a native Rive frame.
   They live in the shared device-test support file
   (`app/src/androidTest/java/com/almeidajr/molda/DeviceWaits.kt`), share its
   one timeout constant, and use the built-in variants: `waitUntil` with a
   condition description for window state, `waitUntilAtLeastOneExists`,
   `waitUntilExactlyOneExists` and `waitUntilDoesNotExist` for nodes, never
   hand-rolled `fetchSemanticsNodes()` checks. Detekt rejects `waitUntil*`
   calls in every other test file. Keep native waits out of logic tests: test
   UI that sits behind a native load through a seam that takes the loaded
   state, as `GuideFrame` does for the guide's playback controls.

Never follow a wait with an assertion of the same condition; the wait already
states it. A test that changes a global device setting, such as the animator
duration scale, does it through a JUnit rule that restores the original value
even when the test fails (`AnimatorDurationScaleRule` in the support file).

Keep the keyboard out of tests that press Back in a dialog. While the keyboard
shows or hides, Android registers an IME Back callback on the focused window,
the dialog included, and it takes the Back press. The app cannot see that
state, so no wait covers it: dirty a form through a non-text control instead.
Leave touch mode only through `KeyboardModeRule`, never with a Tab, Enter or
arrow key: touch mode is global, Compose test clicks do not restore it, and
every later test would run as a keyboard user, whose returning window focus
raises the keyboard on a text field. An opening keyboard also shrinks
`imePadding` layouts frame by frame, which can clip a button between scrolling
to it and tapping it, so `awaitKeyboardVisible` and `awaitKeyboardHidden` return
only once the keyboard animation has finished. Close the keyboard first with
`closeKeyboard`, never Espresso's `closeSoftKeyboard` alone, when the test does
not need it open: the system drops a hide that arrives while the keyboard is
still opening, so `closeKeyboard` lets it finish opening first.

## UI behavior

Exercise user actions and assert visible content, semantics, enabled state,
navigation, or other outcomes users rely on. Prefer accessible semantics to
implementation-specific selectors. Add test tags only when useful nodes cannot
otherwise be selected reliably. Test recreation, state restoration, or different
window sizes when the changed behavior depends on them. Screen tests seed data
through the fake's constructor and assert through the fake's current state, so
they need no coroutines.

Run the checks relevant to the change using the commands in [CONTRIBUTING.md](../../CONTRIBUTING.md#development).
Device tests require a connected device or emulator. Report checks actually run
and any meaningful gap; test counts and coverage percentages are not completion
criteria. Follow [Kotlin style](kotlin-style.md) for test code too.
