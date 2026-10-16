---
paths:
  - "app/src/test/**"
  - "app/src/androidTest/**"
---

# Kotlin tests

Write tests for valuable observable behavior, not coverage. Before adding a
test, identify the regression or incorrect behavior it would catch. Prioritize
business rules, non-trivial branches, boundaries, error handling, invariants,
state transitions, and regressions. Trivial getters, constructors, delegation,
and framework behavior do not need tests merely because code changed.

**Test at the useful boundary.** Exercise the public API whenever possible.
Assert returned values, state, or meaningful external effects rather than
private methods or internal call order. One test covers one behavioral concept;
multiple assertions are fine when they describe that outcome. Use minimal,
relevant arrange data and reuse existing helpers. Prefer real value objects and
small fakes; use mocks when an interaction is itself the observable contract.

**Match Molda's stack.** Use JUnit 4 in `app/src/test` for isolated JVM logic.
Use AndroidJUnit4 in `app/src/androidTest` when Android runtime behavior matters,
and the existing Compose testing APIs for UI interactions and semantics. Use
integration tests when correctness materially depends on persistence, files,
networking, serialization, or wiring. Keep pure business logic testable without
an emulator. A mocked database or serializer does not verify that integration.

**Descriptive names.** Use `<Subject>Test`, matching the repository's singular
suffix. Name methods for the behavior, using the existing underscore convention,
for example `readValue_whenSensorIsOffline_returnsLastKnownReading`. Group tests
by cohesive subject or behavior; a rigid one-class-per-production-class rule
is unnecessary. Keep existing names unless already changing the test.

```kotlin
class TemperatureCalibrationTest {
    @Test
    fun calibrate_withOffsetBelowMinimum_clampsToMinimum() {
        val calibratedTemperature = calibrate(
            reading = 2.0,
            offset = -5.0,
            minimum = 0.0,
        )

        assertEquals(0.0, calibratedTemperature, 0.0)
    }
}
```

The example illustrates naming and structure; it does not introduce an app API.
Use JUnit 4 imports (`org.junit.Test`, `org.junit.Assert`). For repeated cases,
use JUnit 4 parameterized tests when they reduce duplication and retain clear
failure names; do not migrate to JUnit 5 just to translate .NET theories.

**Deterministic execution.** Control clocks, randomness, external input, and
scheduling when behavior depends on them. When coroutine logic needs testing,
prefer `kotlinx-coroutines-test` with `runTest` and shared test schedulers over
sleeping or custom scheduler code. Add the test dependency only when needed,
with a version compatible with production coroutines. Verify cancellation or
ordering when they are part of the contract. Use Compose's synchronization and
test clock for UI timing; avoid arbitrary delays.

**UI behavior.** Exercise user actions and assert visible content, semantics,
enabled state, navigation, or other outcomes users rely on. Prefer accessible
semantics to implementation-specific selectors. Add test tags only when useful
nodes cannot otherwise be selected reliably. Test recreation, state restoration,
or different window sizes when the changed behavior depends on them.

Run the checks relevant to the change using the commands in [README.md](../../README.md).
Device tests require a connected device or emulator. Report checks actually run
and any meaningful gap; test counts and coverage percentages are not completion
criteria. Follow [Kotlin style](kotlin-style.md) for test code too.
