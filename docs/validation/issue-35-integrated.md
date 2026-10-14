# Integrated Expressive validation (#38)

Production tree under validation: `548efa72f4b08073d8cbddc9f3a675314550d357`,
containing #36 and #37. This ticket's worktree and branch were independently
verified clean and equal to `integration/issue-35` before edits. No calculation,
persistence or navigation implementation is changed by this evidence ticket.
The calendar Back journey explicitly settles the native keyboard before opening
its dialog; its one Back event and disappearance/draft assertions remain.

## Automated acceptance coverage

Existing tests exercise the parent specification's confirmed Compose journey
boundary with real storage and controlled storage failures. Existing independent
numerical tests are unchanged. Do not confuse this coverage with human approval.

| Requirement | Observable boundary / limitation |
| --- | --- |
| Direct calculation and four radii | SkirtCalculatorTest verifies labelled reference/cutting radii, input summary, explicit zeros, independent expected dimensions and formula/diagram descriptions. |
| Invalid recovery and stale results | Calculator submit-first/error/blur/IME/edit journeys verify correction, first-error focus and withdrawal of stale results/explanation. |
| Profile reuse and override | MeasurementProfilesTest exercises waist-only copy, source identification, manual override, duplicate profiles, retained choices and saved measurements. |
| Failures/cancellation/deletion | ProfileFailuresTest covers loading/retry/manual fallback, failed/pending saves/deletes, preserved drafts/records and one wide-layout delete announcement. Measurement journeys cover named confirmation, Cancel/Back and dirty discard. |
| Recreation/resizing | Existing calculator/profile journeys exercise activity recreation, restored picker/editor drafts, compact/wide content and 200% text transitions. Actual process death and device rotation are separate native checks. |
| Insets and focus | Existing calculator/profile keyboard journeys verify reachable focused fields and actions with IME. Automated hardware Tab covers calculator field order; complete physical keyboard and switch access remain human checks. |
| Themes/text/targets | Calculator and profile appearance journeys use fallback light/dark and the emulator's dynamic light/dark at 200% text, rendered radius/name/note contrast and sampled targets at least 48dp. Several native palettes/control/graphic/focus checks are separate evidence. |
| Hinge avoidance | Unchanged AdaptiveContent uses HingePolicy.AlwaysAvoid and HingeSafeContent excludes actual reported hinge bounds. Existing #22 native foldable evidence predates these presentation edits and is not a fresh #35 result. Physical hinge validation remains pending if hardware is unavailable. |

## Checks and evidence

On 2026-10-14, from this isolated worktree:

- `gradlew.bat spotlessApply` ran before validation.
- `gradlew.bat spotlessCheck :app:detekt :app:lintDebug :app:testDebugUnitTest :app:assembleDebug --continue`: passed. Eight JVM tests pass; lint and Detekt report no findings.
- `pre-commit run --all-files --show-diff-on-failure` and
  `pre-commit run check-all-large-files --hook-stage manual --all-files`: passed.
- Unfiltered production-tree `gradlew.bat :app:connectedDebugAndroidTest --no-configuration-cache`: **55/55 passed**, zero failures/errors/skips, 17m49s. Emulator `emulator-5554`, Medium_Phone_API_37.0, API 37, 1080x2400/density420. Per-class XML counts: calculator 19, measurement 18, failure 8, navigation 2, presentation 4, storage 4. This full run installed the original test harness; the later one-line native-IME test correction has focused API 36 coverage below. API 37 evidence does not establish API 36 CI.

Generated XML/HTML/logs/APK stay outside committed source. Reports are under
`app/build/reports/` and `app/build/outputs/androidTest-results/`; native evidence
and feedback APK are under
`C:/Users/jralm/AndroidStudioProjects/Molda-issue38-evidence/`.

## Independent palette/control audit

An independent PowerShell sRGB luminance calculation read the final Color.kt
values, rather than copying prior reported ratios. The actual grouped input and
profile surfaces use surfaceContainerLow; default Material outlined controls
consume outline, primary, error and onSurfaceVariant. Diagram arcs/center/number
labels explicitly use onSurface, with continuous/dashed/dotted/dash-dot patterns
and a full textual equivalent. This is a role audit, not final pixel evidence.

| Final foreground/background pair | Light | Dark | Meaning |
| --- | --- | --- | --- |
| onSurface/surfaceContainerLow | 15.52:1 | 13.27:1 | Group text |
| onSurfaceVariant/surfaceContainerLow | 8.52:1 | 10.09:1 | Supporting text / source |
| outline/surfaceContainerLow | 4.05:1 | 5.39:1 | Unfocused field boundary |
| primary/surfaceContainerLow | 5.88:1 | 10.06:1 | Focused field boundary / label |
| error/surfaceContainerLow | 5.86:1 | 10.08:1 | Invalid field text / boundary |
| onSurface/surface | 16.27:1 | 14.36:1 | Diagram ink / explanation |
| onPrimary/primary | 6.48:1 | 7.73:1 | Primary action text |

All audited ordinary text pairs exceed 4.5:1 and meaningful field/diagram
boundaries exceed 3:1. Transient pressed/focus overlays, dynamic wallpaper
controls and complete rendered graphics still require separate native sampling;
ratios above do not claim those states were measured.

A native API 37 dynamic-light launch and focused waist field were captured and
visually inspected. Solid foreground/background pixels were counted independently
with System.Drawing, excluding antialias fringe colors. Field label and supporting
text `#5D5F68` over grouped `#F4F3FA` measure **5.77:1**; Use profile text and
focused field outline `#4E5E8B` over that background measure **5.78:1**; unfocused
outline `#797A84` measures **3.86:1**. Focused field label, caret and border are
visible. This is one palette/state sample, not an exhaustive focus or graphic
audit. Captures and matching native layout bounds are `compact-launch*` and
`compact-focus*` in the evidence directory. Several different native dynamic
palettes and complete diagram/focus sampling remain pending.

## API 36 calendar Back regression

The #37 API 36 CI run failed `back_dismissesCalendarThenConfirmsDirtyEditor`
because the calendar Cancel node remained after one injected Back. The exact
existing journey reproduced on a freshly booted API 36 emulator, both at default
size and at CI-like 320x640/density160 with animations disabled. Two cold baseline
runs failed; subsequent warm repeats passed (including ten narrow repeats).

Temporary main-thread window probes showed focused TYPE_APPLICATION dialog and
IME-insets false in ten passing runs. Cold native logs nevertheless showed a
user-triggered IME hide while a previous hide transition completed. Replacing
raw Back injection with Espresso.pressBack passed warm repeats but failed the
second cold run; that experiment was rejected and reverted.

The retained minimal test correction calls the existing Espresso.closeSoftKeyboard
before opening the calendar. It establishes the intended dialog-only Back
precondition instead of relying on dialog insets during a native keyboard
transition. It adds no sleeps/retries, extra Back events, assertion removal or
production behavior change. **Three cold API 36 runs and five warm repeats
passed**, all at 320x640/density160 with all animation scales zero. Logs are
`back-cold-hidden-ime*.txt` and `back-hidden-ime-warm-*.txt` in the evidence
directory. Final Spotless/Detekt/lint/JVM/build and hygiene checks passed again.
Exact native event routing is not conclusively established by
these observations, so no deeper framework root cause is claimed.

## Human validation gate

The [build-specific checklist](issue-35-human-checks.md) records APK identity,
device compatibility, expected independently worked numerical values, audible
TalkBack, every keyboard/switch action, paper transfer and participant feedback.
The user confirmed they can perform these checks; results are **pending**.

No audible TalkBack, successful switch navigation, physical foldable clearance or
seamstress drafting session is inferred from semantics, emulation or screenshots.
#38 must remain incomplete until required human evidence arrives, findings are
resolved and the final build is validated. The existing journey supplied the
meaningful red → green signal for the native keyboard precondition correction;
no new test or production abstraction was needed.
