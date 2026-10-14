# Molda UI/UX specification snapshot

Archived specification from [issue #22](https://github.com/almeidajr/Molda/issues/22),
implemented in [PR #30](https://github.com/almeidajr/Molda/pull/30). The issue is
the canonical specification; the original body is preserved below, including
its observations of the app before the redesign. See the
[design record](ui-ux-design.md) for interview decisions and the
[validation report](validation/ui-ux-22.md) for implementation evidence and
pending checks.

## Problem Statement

Sewists using Molda during paper drafting need to enter or reuse measurements,
calculate full-circle skirt dimensions, and transfer clear, checkable results to
paper. The current single scrolling page places the entire measurement-profile
manager between Calculate and its results. Adding profiles increases the distance
to results. Secondary and destructive actions have the same visual emphasis as
Calculate, deletion is immediate, profile forms lack Cancel, and wide windows
stretch a single column.

These source-observed interaction problems impede the intended workflow. Their
practical impact has not yet been measured in a sewist session.

## Solution

Redesign the existing offline, Brazilian Portuguese pattern calculation assistant
using standard Material Design 3 without custom branding. Launch into a grouped
calculator form with optional measurement-profile selection. Bring valid results
into view on the same screen, distinguish reference and cutting dimensions, and
keep the diagram, construction limits, and checkable formulas accessible.

Provide separate profile management with safe editing and deletion, explicit
measurement dates, clear storage outcomes, and predictable navigation. Adapt the
layout to available space and enlarged text, preserve in-progress work through
navigation and Android restoration, and support TalkBack and keyboard operation.
This issue refines the existing MVP specification in #13; it does not expand its
garment, language, units, storage, or Android-version scope.

## User Stories

1. As a sewist, I want Molda to open directly into the calculator, so that I can continue drafting without navigating a home screen.
2. As a sewist, I want to calculate without creating a measurement profile, so that a one-off calculation needs no setup.
3. As a sewist, I want body measurement, garment choices, and cutting allowances grouped in one form, so that I can review their different purposes together.
4. As a sewist, I want waist-measuring guidance beside the waist input, so that I enter the body measurement at the intended waistband position.
5. As a sewist, I want finished-length guidance beside its input, so that I exclude waistband height and allowances.
6. As a sewist, I want to explicitly enter waist ease and allowances, including zero, so that the app makes no hidden drafting assumptions.
7. As a Brazilian sewist, I want decimal-comma input and centimetre labels, so that I use familiar notation.
8. As a sewist, I want an available Calculate action even when inputs are incomplete, so that I can discover what needs correction.
9. As a sewist, I want errors to first appear when I attempt calculation, so that an untouched form does not immediately criticize missing inputs.
10. As a sewist, I want specific errors beneath affected inputs, so that I understand how to correct them.
11. As a sewist, I want the first invalid input brought into view and focused, so that I can start correcting the form immediately.
12. As a sewist, I want corrected fields revalidated when I leave them after a calculation attempt, so that I receive timely feedback.
13. As a sewist, I want invalid signs, malformed or non-finite numbers, and impossible cutting radii rejected, so that I never draft from invalid results.
14. As a sewist, I want successful calculation to bring results into view, so that profile management does not stand between my action and its outcome.
15. As a sewist, I want reference radii grouped separately from cutting radii, so that I transfer the correct dimensions to paper.
16. As a sewist, I want all four labelled radii equally readable in centimetres with two decimal places, so that I can use every dimension consistently.
17. As a sewist, I want a compact summary of the five calculation inputs beside results, so that I can check which assumptions produced them.
18. As a sewist, I want a labelled diagram with distinguishable line patterns, so that I understand reference and cutting lines without relying on color.
19. As a sewist using TalkBack, I want a textual equivalent of the diagram, so that I can understand the same geometry.
20. As a sewist, I want zero-allowance coincident lines explained, so that I understand why reference and cutting radii can match.
21. As a sewist, I want construction limits beside results, so that I know which drafting work remains my responsibility.
22. As a sewist, I want detailed formulas available in an initially collapsed explanation, so that I can check the arithmetic without unnecessary scrolling.
23. As a sewist, I want an Edit measurements action, so that I can return to the form and adjust my choices.
24. As a sewist, I want prior results removed immediately when any input changes, so that I cannot mistake stale dimensions for current ones.
25. As a sewist, I want Use profile beside the waist input, so that measurement reuse is available where I need it.
26. As a sewist, I want a profile picker showing name, waist, and measurement date, so that I can select the intended measurement.
27. As a sewist, I want selection to copy only waist and return to the calculator, so that garment choices remain intact.
28. As a sewist, I want to see which profile supplied the waist, so that I can check the source of that measurement.
29. As a sewist, I want manually changing waist to identify it as a manual measurement, so that the app does not misattribute an override to a saved profile.
30. As a sewist, I want calculator edits to leave saved profiles untouched, so that one-off adjustments do not overwrite body measurements.
31. As a sewist, I want profile edits or deletion to leave existing calculations untouched, so that saved-data changes do not silently alter my drafting work.
32. As a sewist, I want to dismiss the profile picker without changing anything, so that I can return to direct input.
33. As a sewist, I want a separate profile-management screen, so that maintaining measurements does not interrupt calculation results.
34. As a sewist, I want management rows to open editing and overflow menus to offer Edit and Delete, so that secondary actions have predictable locations.
35. As a sewist, I want a New profile floating action button, so that the main management action is easy to find.
36. As a sewist, I want profile rows without internal IDs, so that the app shows useful measurement information rather than storage details.
37. As a sewist, I want duplicate names allowed and relevant notes shown when name, waist, and date coincide, so that I can keep measurements for people with the same name.
38. As a sewist, I want a dedicated profile form with Save and Cancel, so that I can deliberately finish or abandon an edit.
39. As a sewist, I want confirmation before discarding unsaved profile changes, so that accidental navigation does not lose my work.
40. As a sewist, I want a blank required measurement date with both calendar selection and editable date entry, so that I explicitly record when the measurement was taken.
41. As a sewist, I want past and today's dates accepted and future dates rejected, so that the date represents a measurement already taken.
42. As a sewist, I want no age-based measurement warning, so that the app does not invent unsupported freshness rules.
43. As a sewist, I want successful creation from the picker to return there without automatic selection, so that I choose when to apply the new measurement.
44. As a sewist, I want deletion confirmed with the profile's name, so that I understand which saved information will be removed.
45. As a sewist, I want a failed save to preserve my entered data and saved records, so that I can retry without starting over.
46. As a sewist, I want save/delete success reported only after storage succeeds, so that I can trust operation outcomes.
47. As a sewist, I want loading, empty, and load-error states with appropriate actions, so that I understand why no profiles are currently available.
48. As a sewist, I want manual calculation to remain available when profiles are loading or unavailable, so that a storage problem does not prevent direct calculation.
49. As a sewist, I want device-only storage and potential data loss explained, so that I understand the absence of backup or synchronization.
50. As a sewist, I want Back to dismiss the keyboard or overlay before leaving a screen, so that navigation is predictable.
51. As a sewist, I want my draft preserved through profile navigation, rotation, restoration, and window resizing, so that interruptions do not require re-entry.
52. As a sewist, I want a single-column layout on compact windows and inputs beside results when space permits, so that the app remains readable across phone and wide windows.
53. As a sewist, I want profile lists beside the editor when space permits, so that I can manage measurements efficiently on larger windows.
54. As a sewist, I want content kept away from fold hinges and system UI, so that inputs and actions remain usable.
55. As a sewist, I want standard Material 3 styling following system light/dark mode and dynamic color, so that the app feels familiar and fits my device preferences.
56. As a sewist, I want main actions visually stronger than secondary actions, so that I can identify what to do next.
57. As a sewist using TalkBack, I want meaningful labels, headings, and outcome/error announcements, so that I can complete every workflow independently.
58. As a sewist using a keyboard, I want logical focus order and visible focused fields, so that I can operate the app without relying on touch.
59. As a sewist using enlarged text, I want usable layouts at 200% text size and reliable touch targets, so that text scaling does not hide essential controls or information.
60. As a sewist, I want the app to respect system animation settings, so that motion does not obstruct my work.
61. As the initial reviewer, I want to draft from the results and reuse a profile, so that I can assess clarity, correct transfer to paper, and usefulness in my usual workflow.

## Implementation Decisions

- Keep the existing application module, feature-first organization, MVVM and unidirectional immutable UI state. Modify the app shell/navigation, calculator presentation and state, profile presentation and state, date validation, explanation/diagram layout, and standard Material theme as required by these interactions. Preserve calculation geometry and existing local persistence; no schema change is required by the agreed design.
- Launch into the calculator with access to separate measurement-profile management. Use hierarchical navigation without an added home screen, bottom navigation bar, or navigation drawer. The profile editor is a dedicated screen on compact windows and can be a pane when sufficient space exists.
- Keep five explicit inputs in one form: waist circumference, waist ease, finished skirt length, waist seam allowance, and hem allowance. Group waist as body measurement; ease and length as garment choices; allowances as cutting allowances. All values remain centimetres, with pt-BR decimal-comma support. No hidden defaults for ease or allowances.
- Retain positive finite waist/length, nonnegative finite ease/allowances, positive waist cutting radius, and finite-result rules. Calculate explicitly, retain full intermediate precision, and format only displayed radii to two decimal places.
- Validate initially on Calculate; focus and reveal the first invalid field and show field-specific supporting errors. After that attempt, revalidate corrected fields when focus leaves them. Keep Calculate available to explain incomplete input. Non-field calculation errors must remain understandable and accessible.
- Successful calculation brings same-screen results into view. Display equally readable groups "Linhas de referência" and "Linhas de corte", with all four labelled radii, a compact five-input summary, the diagram, and construction limits. "Editar medidas" returns to the form. Detailed formulas start collapsed and remain available for checking.
- Retain explicit distinctions between waist ease, seam allowance, and hem allowance. Explain coincident lines for zero allowances and the common-center transfer to paper. The diagram is schematic and not calibrated or printable. Woven fabric, waistband, and closure assumptions remain; panel layouts, closure seam allowances, waistband drafting, fabric yardage, and fit guarantees remain excluded.
- Input changes immediately withdraw previous results until a fresh successful calculation. On wide windows before calculation, show "Preencha as medidas e toque em Calcular." Preserve the current inputs through navigation, rotation, Android process recreation/restoration, and resizing. Draft restoration is transient continuity, not saved projects or history.
- Use a Material modal bottom sheet for profile selection, with name, waist, and measurement date. Selection copies only waist, identifies the source profile, and returns to the calculator. A manual waist change switches attribution to "Medida manual". Keep garment choices intact. Profile changes never silently update or remove copied calculation values; source attribution must not imply a live link to the saved profile.
- Management rows open editing; a "Novo perfil" FAB provides creation and per-profile overflow menus provide Edit/Delete. Do not expose internal IDs. Continue to use stable identity independently of display names. Permit duplicate names; show notes in the picker when name, waist, and date coincide. Optional notes are not a uniqueness constraint.
- Profile forms retain required name/nickname, positive finite waist, required measurement date, and optional notes. Provide Save/Cancel, keep errors near fields, and confirm discard of unsaved changes through either Cancel or Back. Preserve form data on save failure. Avoid duplicate mutations during in-flight storage operations.
- Offer a Material date picker alongside editable `dd/mm/aaaa` entry, initially blank on creation. Allow past/today and reject future measurement dates without age-based warnings. This tightens current syntax-only date validation. Do not silently modify, delete, or hide previously saved profiles with future dates; apply the rule when validating an explicit save and explain required correction. Date tests must control the date boundary deterministically.
- Confirm deletion in a Material dialog naming the profile. No undo mechanism. Failed deletion preserves saved data and existing calculator inputs/results. Display success only after storage completes.
- Dismissing the picker leaves the calculation unchanged. Successful creation initiated from the picker returns to it without automatically applying the new measurement. Back first dismisses the keyboard or active overlay, then moves editor to profiles to calculator; honor discard confirmation for dirty editors. Preserve the draft when entering/leaving adaptive panes.
- Show profile loading progress, an optional-profile explanation and "Novo perfil" when empty, and inline load errors with Retry. Manual calculation remains usable throughout. Mutation failures stay near the affected action; successful storage operations use a brief snackbar. Explain local-only storage and potential loss on the management screen and creation form. Never interpret a load failure as an empty database or reset records.
- Use standard Material 3 components, typography, shapes, semantic color roles, and system light/dark/dynamic color without branding or appearance settings. Use outlined text fields; reserve filled buttons for the main action, tonal/text buttons for secondary actions, the creation FAB, and Material confirmation dialogs. Keep motion purposeful and honor system animation settings.
- Compact windows use one column; sufficiently wide windows show inputs alongside results and profile lists alongside the editor. Decide usable panes from available space and text scale, keep readable content widths, avoid fold hinges, and preserve state when resizing. Do not force side-by-side layouts at the expense of 200% text usability.
- Support TalkBack headings, labels and calculation/error announcements, logical keyboard focus order, focused inputs above the keyboard, at least 48dp interactive touch targets, and usable 200% text layouts. Preserve diagram textual alternatives and line patterns so information never depends only on color. Handle system bars and IME insets without duplicated padding.
- Keep Android API 36 minimum and offline app-private storage with the existing no-backup/no-sync contract. Fetch current official library/API guidance via Context7 during implementation rather than pinning unverified APIs or adding frameworks in this spec.

## Testing Decisions

- The owner confirmed the following seams during specification synthesis: existing Compose tests through the real application are the primary boundary; retain existing pure numerical and real-storage integration tests; reuse the controlled failing profile repository for failure scenarios. Add no new test framework or per-class test layers.
- Good tests assert user-observable actions, visible content, accessible semantics, navigation, restored state, returned domain outcomes, or durable storage effects. Avoid private-helper assertions, implementation call counts, duplicated formulas as numerical oracles, arbitrary delays, and coverage quotas. Prefer the highest existing useful seam.
- Prior art includes the current SkirtCalculatorTest, MeasurementProfilesTest, ProfileFailuresTest, ProfileStorageTest, MeasurementParsingTest, and CalculateSkirtRadiiTest. Extend/update behavior coverage and selectors for changed navigation and copy rather than retaining assertions for the old combined page or internal IDs. Use existing JUnit 4, AndroidJUnit4, and Compose test conventions.
- Calculator UI tests cover direct/manual and profile-based calculation, explicit zero values, decimal commas, first-submit validation, first-error visibility/focus, blur revalidation after an attempt, all result groups and input summary, collapsed/expanded formulas, diagram equivalence, Edit measurements, and immediate result invalidation. Assert manual calculation works during profile loading/failure.
- Profile journey tests cover picker dismissal and selection, source attribution and manual override, unchanged garment inputs, explicit edits without calculator write-through, duplicate-name rows and collision notes, create-from-picker return without selection, management menus/FAB, Save/Cancel/Back, discard confirmation, named deletion confirmation, and no mutation before confirmation.
- Date coverage verifies editable entry and picker equivalence, blank/invalid dates, past/today/future boundaries using a deterministic current date, and preservation/readability of legacy saved future-dated profiles with correction required on explicit save.
- Reuse failure injection to verify loading/retry and save/update/delete failure outcomes, preservation of draft and saved data, absence of false success, and successful outcomes only after completion. Retain real-storage reopening tests for persistence and identity; do not replace them with mocks.
- Preserve independent numerical regression checks. For waist 80 cm, ease 2 cm, finished length 60 cm, waist seam allowance 1 cm, and hem allowance 2 cm, reference/cutting radii display as 13,05; 73,05; 12,05; and 75,05 cm. Retain zero-allowance and invalid-cutting-radius cases; no geometry changes are intended.
- Verify profile navigation, keyboard/overlay Back order, dirty-editor handling, rotation, Android state restoration and actual process recreation where needed, and resizing across single/multi-pane layouts. Activity recreation alone is insufficient evidence of process-death restoration.
- Check compact and wide windows, readable widths, fold-hinge avoidance, system light/dark/dynamic colors, system bar/IME handling, TalkBack traversal and announcements, hardware keyboard navigation, 48dp targets, 200% text, non-color diagram distinctions, and system animation settings. Use existing device-test configuration facilities and targeted manual device checks; no new screenshot framework is mandated.
- During implementation run the repository formatter, relevant quality checks, JVM tests, debug build, and connected device tests. Report unavailable checks and device execution gaps explicitly. Do not claim visual or accessibility verification solely from source inspection.
- After technical checks, conduct the already planned session with the Brazilian Portuguese-speaking sewist: transfer radii to paper, distinguish reference/cutting lines, compare usual calculations, reuse profiles, and record mistakes, confusion, and usefulness. Verify her API 36 device compatibility before treating the feedback build as usable by her. This human feedback gate does not block publishing the implementation-ready specification.

## Out of Scope

- Custom branding, custom palettes/fonts, appearance settings, a home dashboard, bottom navigation, or a navigation drawer.
- Additional garment types, circle fractions, drafting systems, units, languages, or Android versions below API 36.
- Printable/calibrated pattern generation, automatic patterns, fit diagnosis/guarantees, panel layouts, closure seam allowances, waistband drafting, and fabric yardage.
- Accounts, client administration, networking, analytics, cloud sync, backup/export, saved projects/calculation history, search, undo deletion, age-based measurement warnings, additional body measurements, or forced unique profile names.
- New test frameworks, broad architecture/persistence rewrites, speculative extension points, or implementation as part of publishing this specification.

## Further Notes

- All twenty design decisions and final shared understanding were confirmed by the owner. The repository's UI/UX design record preserves the interview outcome; #13 remains the baseline MVP specification. This issue provides the confirmed interaction refinements, including explicit calculation, restoration, and future-date rejection.
- No relevant ADR exists, and no domain vocabulary changed. These reversible UI decisions do not require a new ADR. Use the established terms pattern calculation assistant, measurement profile, full-circle skirt, waist ease, seam allowance, hem allowance, and finished skirt length.
- Mathematical checks establish arithmetic, not fit or practical usefulness. No observed drafting session has yet validated the proposed experience. One participant gives an initial signal rather than proof for the entire audience.
- Current profiles are device-local and can be lost if app data is cleared or the device replaced. This communicated limitation never permits ignoring persistence errors or deleting old records during validation changes.
- Official guidance: [Material 3 for Jetpack Compose](https://m3.material.io/develop/android/jetpack-compose), [Android Compose documentation](https://developer.android.com/develop/ui/compose/documentation), and [Compose accessibility defaults](https://developer.android.com/develop/ui/compose/accessibility/api-defaults).
