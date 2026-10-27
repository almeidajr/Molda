# Each behavior has one owning test, and devices run only what needs a device

> Partly supersedes [ADR-0004](0004-fakes-for-screen-tests-and-idling-aware-room-for-journeys.md) and [ADR-0006](0006-entry-scoped-viewmodels-results-and-dialogs.md): screen tests on fakes run on Robolectric instead of a device. The fakes, idling-aware Room for journeys, and Hilt bindings for every test that starts the app all stay.

**Status:** accepted; carried out through [#222](https://github.com/almeidajr/Molda/issues/222).

By October 2026, Molda had 275 device tests. They took about 14 minutes locally and 38 minutes of a 44-minute CI job on a software-rendered emulator. About 60 of them re-asserted a rule a local test already owned, only adding that the string reached the screen. Most of the rest ran on fakes and needed nothing from a device but a composition. Every one of them still paid for the device: an app launch, real keyboard animations, window focus, and the flakes that come with them. Following Now in Android, we adopt two rules:

- **One owner per behavior.** Each behavior is asserted by exactly one test, at the lowest level that can observe it. Higher levels assert only what lower ones cannot see. Tests assert what Molda decides, never what a dependency guarantees.
- **Devices run only what needs a device:**
  - the real keyboard (IME), real key events and window focus
  - native Rive frames
  - real Room: repository, migration and journey tests
  - hinge and fold posture
  - system bars and insets

  Every other screen test runs locally on Robolectric, with the same Hilt bindings and fakes, in a Hilt test activity that shows `MoldaRoute`. `MainActivity` calls `Rive.init`, which loads native code Robolectric cannot run; the test activity skips that call, so a local test that reaches the guide sees its written fallback instead of the drawing, and only native Rive frames need a device.

## Considered options

- **Delete the duplicates, keep screen tests on a device, and decide later.** Rejected: the remaining screen tests would still pay a device's cost and flakes for behavior a JVM composition shows just as well.
- **Move `Rive.init` behind a replaceable initializer, so Robolectric can start `MainActivity`.** Rejected: it adds a production seam only for tests, while a test activity is the pattern Now in Android uses.
- **Screenshot tests (Roborazzi) for appearance.** Rejected: goldens recorded on Windows do not match Linux CI, and Rive cannot render on the JVM. Molda's own layout decisions stay asserted through semantics and bounds.

## Consequences

- Robolectric screen tests use `@RunWith(AndroidJUnit4::class)`, so a test can move between source sets without changing its runner.
- Native Rive rendering is tested on a device, offscreen where possible, so its pixel checks do not depend on screen captures or wall-clock time.
