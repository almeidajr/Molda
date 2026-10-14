# Measurement profile design validation (#37)

The isolated ticket worktree started from integration commit
`af327fd934567ab371d3afc88a5e3b6185a028b6` (#36). Presentation changes reuse
its generated palette, typography and shape roles. Persistence, profile identifiers,
waist-only copying, calculation geometry, state ownership and navigation are unchanged.

## Acceptance evidence

| Requirement | Implementation / observable checks |
| --- | --- |
| Shared Expressive presentation | Screen/picker headings use 28/36sp; grouped editor and rows use semantic surfaceContainerLow with 16dp corners, 16dp padding and 16dp gaps. Existing Material date picker, dialogs, overflow menu and creation FAB consume shared theme roles; modal corners inherit 28dp. Primary picker creation uses the existing rounded Material Button. Installed public Material press/focus feedback is retained, without animations on dimensions or new dependencies. |
| Profile identity and duplicates | Name, waist and explicit measurement date remain visible. Picker notes distinguish otherwise identical profiles; management now displays notes too. Names may repeat and database identifiers remain hidden. New journey creates two identical profiles with different notes and selects via the intended note. |
| Waist-only selection and source | Existing pick/manual override/edit/delete journeys assert only waist is copied, source is identified and saved edits/deletion leave current calculation untouched. |
| Picker dismissal and creation return | Existing journeys cover unchanged calculator draft on dismissal and return after creation without automatic selection. |
| Explicit, safe dates | Editable dd/mm/aaaa entry now works independently of device locale, alongside the Material calendar icon; blank/invalid/future boundaries and cancel preservation are tested. Guidance explains the date is when measurement happened and excludes future dates. No storage values or calendar timestamp handling changed. |
| Persistence/loading/failure | Existing save/load/delete failure journeys retained: no early success, duplicate writes prevented, entered drafts and saved data preserved, Retry and manual calculation available. |
| Confirmation and announcements | Named deletion and shared Cancel/Back unsaved-discard protection retained. Save/delete success uses the existing single Snackbar announcement; delete failures belong exclusively to the affected row, including when another editor is open in a wide layout. |
| Navigation and adaptation | Existing navigation/process-restoration, recreation, resizing, active-editor Back, list/editor panes, IME/inset and hinge rules retained. Presentation introduces no destination. |
| Large text and separate targets | New appearance journey uses a long name at 200% text in static light/dark and emulator dynamic light/dark. It reaches notes, separate row action, creation FAB and editor save; checks row/create/action/save targets are at least 48dp. Row action is below details with an 8dp gap, allowing details and action names to wrap independently. |
| Rendered contrast | The appearance journey obtains actual name/note TextLayoutResult foreground and captured background, asserting at least 4.5:1 in each of the four theme modes. Generated fallback palette evidence is in calculator-design-validation.md. |

## TDD and validation

Tests use the already confirmed Compose UI journey seam, with real local profile
storage and the existing date input helpers. Numerical and storage implementations
are unchanged. Five vertical slices were observed red then green:

1. `managementRows_showNotesToDistinguishOtherwiseIdenticalProfiles` failed because
   the saved note was absent from management, then passed when rows displayed notes.
2. `newProfile_explainsExplicitDateAndOptionalNotes_beforeSaving` failed on the
   missing group heading, then passed with the grouped form and date/notes guidance.
3. `directDateEntry_usesDayMonthYear_andFailedEditsPreserveSavedDate` failed because
   the old read-only date field exposed no SetText action. The field is now editable
   through the existing onEdit/parser path; invalid/future text remains entered,
   valid dd/mm/aaaa saves correctly even on an en-US device, and a rejected date edit
   leaves the persisted original date unchanged after discard/reopen. This repairs
   a prior gap against ui-ux-design.md/ui-ux-spec.md, which explicitly require editable
   entry alongside the calendar. Existing calendar tests now open the calendar icon;
   their explicit-entry helper types directly without locale conversion. Material
   input-mode tests still follow its device locale order, independently of the form.
4. `wideDeleteFailure_announcesOnce_andKeepsOtherEditorDraft` failed with two copies
   of the same delete error (both live regions). The editor now excludes errors owned
   by deleteId; the journey passes with exactly one polite live region, retained
   editor draft, the saved profile still visible and no false deletion success.
5. The direct-date journey was extended with `12/10/0000`: red showed the error
   missing because JDK's proleptic-year parser accepted year zero. Explicit-save
   validation now requires year >= 1, matching the calendar. Green preserves that
   invalid draft, rejects future dates and verifies the saved original date after a
   rejected edit. Loading/parsing existing records is unchanged; no migration or
   silent correction/deletion was introduced.

Presentation journeys live in ProfilePresentationTest to keep the existing large
measurement journey suite within Detekt limits. Small presentation sections were
extracted to satisfy method limits; no analysis suppression was added.

Validation on 2026-10-14:

- `gradlew.bat spotlessApply` ran before quality validation.
- `gradlew.bat spotlessCheck :app:detekt :app:lintDebug :app:testDebugUnitTest :app:assembleDebug --continue`: passed. Detekt reports zero findings; lint reports no issues; eight unchanged JVM tests pass.
- `gradlew.bat :app:connectedDebugAndroidTest --no-configuration-cache`: passed **54/54**, zero failures/errors/skips on emulator `emulator-5554` (API 37), in 15m29s. XML counts: calculator 19, measurement journeys 18, failures 7, navigation 2, presentation 4 (including all four theme variants/rendered contrast), storage 4.
- After that full run, the final two small acceptance fixes above passed focused red/green verification: wide delete announcement **1/1** (24s) and direct date/year-zero/persisted-date preservation **1/1** (42s), with all quality checks rerun successfully. The full suite was not repeated after these final two fixes; integrated #38 validation runs the final tree, which now contains 55 journeys.
- `pre-commit run --all-files --show-diff-on-failure`: passed; final commit hooks also run hygiene and Conventional Commit validation.
- Preserved local full-run XML: `app/build/reports/issue37-full-results.xml`. Focused green reports: `issue37-wide-announcement-results.xml` and `issue37-direct-date-results.xml` in the same reports directory. Generated build reports are ignored, not committed.

Two earlier filtered runs reached native focus wait timeouts in existing calendar
journeys (one during calendar focus acquisition, one after dismissal while waiting
for activity focus). These were synchronization waits, rather than changed-data or
navigation assertions. A fresh isolated Back journey passed without a production
change. The subsequent full run passed both calendar/Back journeys after the date
entry correction. No claim is made that rerunning
alone establishes the underlying intermittent native-focus cause.

Class-list command filtering selected only the first class on this Windows toolchain;
configuration-cache reuse also retained an older class selection. Use an unfiltered
`connectedDebugAndroidTest --no-configuration-cache` run and verify XML per-class
counts to establish full-suite coverage.

## Remaining human checks

Audible TalkBack (including state announcement duplication), physical keyboard/switch
navigation and focus comfort, disabled-animation device use, several different
wallpaper palettes, real hinge/device layouts, ordinary editor/control/border contrast
and a real seamstress drafting session require integrated human validation. Automated
semantics, emulator screenshots and one dynamic wallpaper palette do not establish
these. The integrated #38 checklist records these checks and their outcomes. Local
API 37 results do not replace required API 36 CI.
