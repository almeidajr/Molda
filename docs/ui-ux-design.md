# Molda UI/UX design

The confirmed [UI design system](ui-design-system.md) extends this record with
Material 3 Expressive typography, spacing, shape, motion and component contracts.
It supersedes the standard-visual-system-only and no-custom-branding decisions
below with a branded fallback palette while preserving dynamic color and the
existing interaction, navigation and data-safety requirements. Screen
implementation remains outside that design-system phase.

The results redesign in [issue #51](https://github.com/almeidajr/Molda/issues/51)
updates the screen structure and result details below: the calculator gains a
top app bar, results become a cutting card, the explanation and formulas sit
behind disclosures, and the fabric-folding tutorial opens in a full-screen
player instead of a dialog.

Status: confirmed design, implemented in
[PR #30](https://github.com/almeidajr/Molda/pull/30). This record preserves the
design interview and decisions agreed by the product owner. It supplements the
existing MVP discovery and baseline specification in GitHub issue #13.
The canonical UI/UX specification is
[issue #22](https://github.com/almeidajr/Molda/issues/22). See the
[archived validation report](https://github.com/almeidajr/Molda/issues/29#issuecomment-6192570236) for completed checks and pending
accessibility and human-feedback verification.

## Confirmed direction

- Prioritize the paper-drafting journey: enter or reuse measurements, calculate,
  and transfer clear, checkable dimensions to paper.
- Redesign the existing full-circle skirt calculator and measurement-profile
  flows within the agreed MVP scope, including navigation, inputs, validation,
  results, profile editing/deletion, accessibility, and adaptive layouts.
- Use Material Design 3's standard visual system without custom branding.
  Keep the experience calm and precise; visual hierarchy and motion serve the task.
- Retain the established audience, Brazilian Portuguese, centimetres, explicit
  drafting assumptions, optional profiles, and offline device-local storage.

## Confirmed screen structure and calculation flow

- Launch into the calculator under a top app bar titled "Saia godê inteira",
  whose labelled "Perfis de medidas" action opens the separate management
  screen. Place "Usar perfil" and the waist's source just above the waist input.
  Do not introduce an additional home screen; the app bar is not a navigation
  destination.
- Keep the five inputs in one form, grouped into body measurement, garment
  choices, and cutting allowances. Place brief guidance by the relevant fields;
  ease and allowances still require explicit values, including zero.
- End the form with a full-width "Calcular". After a successful calculation,
  hide the keyboard and bring the whole cutting card into view on the same
  screen. The card presents all four labelled radii once, distinguishes
  stitching/reference dimensions from cutting dimensions, and pairs each value
  with its diagram number and line style. "Tutorial de dobra do tecido" and
  "Editar medidas" follow it.
- Keep construction boundaries beside results in one callout. Put the drafting
  explanation ("Como levar os raios ao papel") and the formulas behind
  disclosures that start collapsed, so they remain available for checking.

## Design tree

- Confirmed product journey and scope
  - Confirmed: calculator launch and separate profile management
    - Confirmed: profile selection, creation, editing, and deletion safety
    - Confirmed: bottom-sheet picker, management list actions, explicit date entry
  - Confirmed: single grouped form with contextual guidance
    - Confirmed: submit-first validation and field-level recovery
  - Confirmed: same-screen results, diagram, expandable formulas, edit action
    - Confirmed: invalidate results on input changes; restore current draft
  - Confirmed: loading, empty, error, and success states
  - Confirmed: Back, dismissal, result hierarchy, and profile edge cases
- Confirmed standard Material 3 visual direction
  - Confirmed: standard component hierarchy and system light/dark/dynamic color
  - Confirmed: adaptive layouts, keyboard interaction, and accessibility criteria
- Confirmed: practical usability validation and implementation acceptance criteria
- Confirmed: final shared understanding; design interview complete

## Confirmed interaction and data safety

- "Usar perfil" opens a picker showing name, waist, and measurement date.
  Selection returns to the calculator and fills only waist. Show which profile
  supplied that measurement; manually changing waist changes the source label
  to "Medida manual". Garment choices remain unchanged. Editing or deleting the
  saved profile never silently changes an existing calculation.
- Any calculation input change immediately removes previous results and
  requires Calculate again. Preserve current entered values through profile
  navigation, rotation, and restoration after Android recreates the app.
  Restoring the current draft does not introduce saved calculation history.
- Show validation errors first on Calculate. Bring the first invalid field into
  view and focus it, with specific supporting errors beneath affected fields.
  Following that attempt, revalidate corrected fields when the user leaves them.
  Keep Calculate available so an incomplete form can explain what needs fixing.
- Creating and editing profiles use a dedicated form with Save and Cancel.
  Ask before discarding unsaved changes. Require confirmation naming the profile
  before deletion; no undo mechanism is included in this MVP.
- Preserve entered profile data after a failed save, and report success only
  after storage succeeds.

## Confirmed profile presentation

- Present the profile picker in a Material modal bottom sheet. Picker rows
  select a measurement; show name, waist, and measurement date.
- The separate management screen uses profile rows that open editing, a
  "Novo perfil" FAB, and per-profile overflow menus for Edit/Delete. Do not
  expose internal IDs. Duplicate names remain allowed; measurements and dates
  help distinguish them.
- Offer a Material date picker alongside editable `dd/mm/aaaa` entry. Start
  the measurement-date field blank and require the user to choose the actual
  date rather than silently assigning today.

## Confirmed Material 3 and adaptive behavior

- Follow system light/dark mode and dynamic color. Use standard Material
  typography and shapes, with no custom palette or appearance settings.
- Reserve filled buttons for the main action. Use outlined text fields,
  tonal/text buttons for secondary actions, Material confirmation dialogs,
  and the agreed profile-creation FAB.
- Compact windows show one column. Sufficiently wide windows show calculator
  inputs beside results and may show profile lists beside their editor.
  Adapt to available window space and text size, constrain readable widths,
  avoid fold hinges, and preserve the current draft when resizing. One column
  spans a flat fold and keeps to one side of a half-open or separating one; two
  panes split at the fold, or share one side when they cannot fit beside it.
- Keep the agreed hierarchical navigation; no home screen, bottom navigation
  bar, rail or drawer is needed. The full-screen tutorial player keeps to one
  side of a half-open or separating fold but spans a flat one, and the cutting
  card stays in the results pane.

## Confirmed accessibility acceptance criteria

- Support TalkBack, logical keyboard navigation, at least 48dp touch targets,
  and usable layouts at 200% text size.
- Announce calculation outcomes and errors. Keep the diagram's textual
  equivalent and distinguish lines without relying on color alone.
- Keep focused fields visible above the keyboard and respect system animation
  settings.

## Confirmed states and navigation details

- While loading profiles, show a loading indicator. When empty, explain that
  profiles are optional and offer "Novo perfil". On load failure, show an inline
  error with Retry. Manual calculation remains available in all these states.
- Keep save/delete failures near the affected action and show successful
  operations in a brief snackbar after storage succeeds. Explain device-only
  storage and potential loss on the management screen and creation form.
- Back first closes the keyboard or active overlay, then returns through
  editor to profiles to calculator. Cancel and Back share the unsaved-change
  confirmation. Resizing into a multi-pane layout must not discard the draft;
  Back applies to the active editor rather than removing a visible list pane.
- Dismissing the profile picker leaves the calculation untouched. Creating a
  profile from the picker returns to the picker after a successful save without
  automatically selecting the newly created profile.

## Confirmed result details and edge cases

- Use two clearly labelled groups on the cutting card: "Linhas de referência"
  (waist stitching radius and finished hem radius) and "Linhas de corte" (waist
  and hem cutting radii). Keep all four values equally readable, in centimetres
  with two decimal places, each shown and announced once. Then include the
  tutorial entry, "Editar medidas", a compact summary of all five calculation
  inputs under "Medidas utilizadas (cm)", the construction-limits callout, the
  diagram with its line key, and the collapsed drafting explanation and formulas.
- The diagram's text equivalent describes every line but no longer repeats the
  radius values, which the card states once. The product owner confirmed this
  departure from the earlier "lists all dimensions" contract.
- The tutorial player keeps the drawing pinned while the step's instructions
  scroll (in short windows the drawing scrolls with them) and has a fixed bar:
  "Anterior", "Etapa n de 6", and "Próximo", which becomes "Concluir" on the last
  step. Close, "Concluir" and Back return to the results. Its still frame stays
  white in dark mode because the Rive artboards paint white; this is a known gap.
- Before calculation, the wide-window results pane says
  "Preencha as medidas e toque em Calcular." Input changes remove results;
  no usable-looking stale dimensions remain visible.
- Allow duplicate profile names and past/today measurement dates. Reject future
  measurement dates, without age-based warnings. If names, waist values, and
  dates coincide, show notes in the picker to help distinguish profiles.
  Notes are optional and do not establish profile uniqueness.
- Rejecting future dates was a newly confirmed validation requirement: the
  baseline code validated date syntax but did not reject future dates. Existing
  saved profiles must not be silently changed or deleted during implementation.

## Confirmed validation gate

- Check direct calculation and profile-selection journeys, invalid inputs,
  result invalidation, profile create/edit/cancel/delete, discard confirmation,
  loading/empty/failure/success states, and preservation of data after failures.
- Check navigation, rotation, Android process recreation/restoration, and window
  resizing, preserving the current draft without introducing saved history.
- Check phone and wide-window layouts, system light/dark themes and dynamic
  colors, TalkBack, keyboard navigation, keyboard visibility, touch targets,
  200% text size, diagram alternatives, and system animation settings.
- Retain independent numerical checks, including the discovery reference case:
  waist 80 cm, ease 2 cm, length 60 cm, waist seam allowance 1 cm, hem allowance
  2 cm; displayed reference/cutting radii are 13.05, 73.05, 12.05, and 75.05 cm.
- Follow automated/device checks with the planned sewist drafting session:
  verify understanding of stitching versus cutting lines, correct transfer to
  paper, profile reuse, and usefulness in the participant's usual workflow.
  The participant's API 36 device compatibility still needs verification.

## Implementation scope and review boundaries

The agreed change reaches the app shell/navigation, calculator UI and state,
profile picker/manager/editor and state, date validation, result explanation and
diagram layout, standard Material theming, and connected behavior/device tests.
Update tests when visible copy or navigation changes. Keep repository persistence
and calculation geometry unless a confirmed interaction requires a change.

No custom branding, appearance settings, additional garment calculations,
accounts, sync, backup/export, saved projects/history, search, or undo feature
is introduced. No domain terms changed during this interview, so no glossary
update is needed. These UI decisions are reversible and do not meet the ADR
threshold; the design record remains here.

## Implementation observations before the redesign

At the time of the interview, the calculator, profile manager, results, and
explanations occupied one scrolling column. Profile management sat between
Calculate and the results, so each saved profile increased the distance to the
result. Secondary and destructive actions used the same filled-button emphasis
as Calculate. Deletion had no confirmation or undo; profile forms had no cancel
action. Large windows retained a full-width column. These were source
observations, not findings from a usability session.

## References

- [Existing product discovery](product-discovery.md)
- [Canonical UI/UX specification](https://github.com/almeidajr/Molda/issues/22)
- [Implementation validation](https://github.com/almeidajr/Molda/issues/29#issuecomment-6192570236)
- [Shared domain vocabulary](../GLOSSARY.md)
- [Material 3 for Jetpack Compose](https://m3.material.io/develop/android/jetpack-compose)
- [Android Compose documentation](https://developer.android.com/develop/ui/compose/documentation)

Record reversible interaction decisions here. Add glossary entries only when
domain terms are resolved, and ADRs only for consequential decisions involving
meaningful trade-offs that would otherwise be surprising.
