# Molda UI design system

Status: confirmed design system; Q1–Q9 accepted. No screen implementation is authorized by
this document. Accepted decisions below extend the existing
[UI/UX design](ui-ux-design.md); existing behavior and data-safety requirements
remain in force.

Implementation is specified in [GitHub issue #35](https://github.com/almeidajr/Molda/issues/35),
with a [repository snapshot](ui-design-system-spec.md).

## Accepted direction

- Prioritize working seamstresses calculating dimensions at the cutting table.
  Keep concise contextual explanations available for occasional users.
- Use a calm, warm sewing-studio character. Material 3 Expressive shapes,
  typography and motion emphasize primary actions and results.
- Define the system and component/state specifications in documentation before
  implementing screens.
- Preserve calculator-on-launch, optional measurement profiles, Brazilian
  Portuguese, centimetres, offline storage and hierarchical navigation.
- Replace the previous restriction against custom visual branding with the
  fallback palette below, retaining system light/dark mode and dynamic color.

## Design tree

- Accepted: audience, visual character, documentation scope, existing flows.
- Accepted: typography and equal numerical hierarchy.
- Accepted: warm plum fallback and dynamic-color precedence.
- Accepted: restrained expression, spacing and existing adaptive layouts.
- Accepted: existing component inventory, states and accessibility acceptance.
- Accepted: final shared understanding, including palette validation before implementation.

## Typography

Use the Android system sans-serif through Compose's default font family. Keep
the full Material type scale; map the following roles consistently. Sizes and
line heights are in sp and scale with system preferences.

| Use | Material role | Size / line height | Weight |
| --- | --- | --- | --- |
| Screen heading | headlineMedium | 28 / 36 | Regular |
| Section heading | titleLarge | 22 / 28 | Regular |
| Profile name / component title | titleMedium | 16 / 24 | Medium |
| Field text / explanatory body | bodyLarge | 16 / 24 | Regular |
| Supporting text / field errors | bodyMedium | 14 / 20 | Regular |
| Action label | labelLarge | 14 / 20 | Medium |
| Each calculated radius | headlineSmall | 24 / 32 | Medium emphasis |

Display all four radii with equal size and weight; labels identify their meaning.
Show centimetres and two decimal places using Brazilian Portuguese formatting.
Keep values and units associated when wrapping. Never truncate drafting values,
field errors or essential action labels. Avoid fixed-height text containers.

## Color system

Dynamic color is the runtime default. Follow system light/dark appearance without
adding appearance settings. The warm studio identity must also come from shape,
spacing and hierarchy because wallpaper colors can replace the branded palette.

For fallback and design previews, use warm plum seed `#67506B`, linen-like neutral
surfaces and muted sage tertiary tones. Generate complete light/dark Material
schemes with Material Theme Builder before implementing theme tokens. The seed
is an input to palette generation, not an approved button fill. Final generated
hex values and contrast measurements belong in this document before implementation.

| Semantic role | Use |
| --- | --- |
| primary / onPrimary | Calculate and Save fills/labels |
| primaryContainer / onPrimaryContainer | Profile-creation FAB |
| secondaryContainer / onSecondaryContainer | Tonal secondary actions |
| surface / onSurface | Screen background and essential text |
| surfaceContainerLow | Grouped input and result surfaces |
| surfaceContainerHigh | Overlay surfaces where Material defaults select it |
| onSurfaceVariant | Supporting text and secondary labels |
| outline | Essential field boundaries |
| outlineVariant | Decorative separators |
| error / onErrorContainer / errorContainer | Validation and failure feedback |
| tertiary / onSurface | Diagram accents / textual labels |

Use role-matched foreground/background pairs from the generated or dynamic
scheme. Target at least 4.5:1 for ordinary text, 3:1 for large text and meaningful
graphical boundaries. Verify actual rendered pairs; do not assume pastel fills
or generated schemes make every combination accessible. Diagram meaning always
uses labels and distinct line styles as well as color.

## Spacing, shape and motion

| Token | Value | Use |
| --- | --- | --- |
| spaceMicro | 4dp | Closely associated decorative details |
| spaceSmall | 8dp | Label/value and icon/text separation |
| spaceMedium | 16dp | Field gaps, group padding, compact screen margins |
| spaceLarge | 24dp | Section and adaptive-pane separation |
| spaceExtraLarge | 32dp | Major content transitions |

Keep these as layout tokens; respect Material component internal padding.
Interactive targets are at least 48dp in both dimensions, with separate hit
areas. Forms and buttons grow with text instead of fitting content to fixed heights.

Use Material shape roles: small 8dp, medium 12dp, large 16dp and extraLarge 28dp.
Grouped surfaces use 16dp corners; prominent result grouping and modal surfaces
may use 28dp. Primary actions use full rounded ends. Keep standard outlined field
shapes. Use tonal grouping rather than nested cards or decorative shadows.

Use Material interaction feedback and available Expressive motion defaults.
Motion communicates pressed state, disclosure and navigation continuity; it
never delays calculation or makes dimensions move while being read. Respect
system animation settings and show the same final state when motion is disabled.
Avoid bespoke animation choreography; check Expressive API availability against
the installed Material3 version before later implementation.

## Navigation and adaptive layout

Keep the calculator as the launch destination. Expose profile management as a
labelled secondary action; keep "Usar perfil" near waist input. Selection uses
a modal bottom sheet; editing and creation use a dedicated profile form.
Retain the existing hierarchical Back, unsaved-discard and picker-return rules
in [the behavior specification](ui-ux-design.md).

Compact windows have one readable column. Preserve the existing adaptive rule:
two panes require `360dp × max(fontScale, 1) × 2 + 24dp` of available content
width, with a 24dp gap and a 640dp maximum readable pane width. This is a
content-fit rule, not a device-type test. Inputs sit beside results; a profile
list may sit beside its active editor. Avoid hinges and preserve drafts when
window size or orientation changes. Large text can return the layout to one pane.

Apply system-bar and keyboard insets. Scroll focused fields and their errors
into view. Primary actions stay in the form flow so they cannot cover fields or
results. No new home screen, bottom navigation, drawer or navigation rail is
needed for the accepted destination hierarchy.

## Reusable component contracts

These are design contracts, not a requirement to create a wrapper for every
Material component. Reuse the current Compose components and state ownership.

| Component | Content and interaction | States and recovery |
| --- | --- | --- |
| Measurement field | Outlined field, persistent label, cm suffix, supporting guidance, decimal keyboard; explicit zero for allowances | Unedited, focused, entered, invalid; submit-first errors, first invalid field focused, revalidate corrected fields on blur after first attempt |
| Section group | Heading, concise guidance, related fields with 16dp padding and gaps | Content wraps; heading semantics; no extra interactive card wrapper |
| Primary action | Filled "Calcular" or "Salvar" with visible label | Calculate remains enabled for incomplete input; show save progress and prevent duplicate writes; retain values on failure |
| Secondary action | Tonal or text button for reuse/edit/cancel | Visible pressed/focus feedback; accessible name; destructive actions use explicit wording |
| Profile row | Name, waist, measurement date; notes when needed to distinguish identical rows | Picker tap selects; management tap edits with separately labelled overflow; loading, empty, failure/Retry and ready list |
| Profile picker | Material modal sheet, profile rows and creation action | Dismissal changes nothing; creation returns to picker without auto-selection; manual calculation remains available |
| Result group | Reference/cutting heading and two equally emphasized labelled radii per group | Hidden after any input change; wide empty pane explains how to calculate; valid result includes five-input summary |
| Drafting diagram | Labelled reference/cutting lines with distinct styles | Text equivalent lists all dimensions and meaning; no color-only information or gesture-only controls |
| Formula disclosure | Labelled expand/collapse action; formulas initially collapsed | Expose expanded state; formulas wrap and stay readable with scaled text |
| Feedback | Inline field/action errors, loading indicators and brief success snackbar | Announce results/errors without duplicate speech; success only after storage succeeds; errors persist until resolved |
| Date entry | Editable dd/mm/aaaa field with Material date picker | Initially blank; syntax and future-date errors; explicit actual measurement date |
| Confirmation dialog | Named deletion confirmation; unsaved-discard confirmation | Clear cancel and destructive actions; retain draft after cancel or failed save/delete |

Applicable states belong to each contract; synchronous calculations need no
artificial loading state. Profile selection copies waist only. Manual waist edits
show "Medida manual". Profile edits/deletion never silently change an existing
calculation. Drafts survive navigation and Android recreation; this creates no
saved calculation history.

Use official Material Symbols XML vectors from existing resources, with Compose
theme tint. Decorative icons beside text have no duplicate announcement;
standalone icon actions have specific Portuguese accessible names.

## Accessibility and acceptance

- TalkBack traversal follows headings, inputs, actions and results in reading
  order. Field labels, units, errors and profile source remain understandable.
- Keyboard and switch access reach every action with visible focus and no
  gesture-only alternatives. Dialog/sheet dismissal restores sensible focus.
- Support 200% text size without clipping, overlapping targets or inaccessible
  fields. Verify compact and wide windows, landscape and keyboard-open states.
- Announce a successful calculation and newly relevant errors. Expose diagram
  information in text; do not force users to infer reference versus cutting lines.
- Verify light, dark and several dynamic schemes against text/graphic contrast
  thresholds, system-bar legibility and meaningful focus indicators.
- Retain existing calculation, profile failure, cancellation, draft restoration
  and stale-result tests; later UI changes must preserve those behaviors.
- During the drafting session, check that a seamstress correctly transfers
  reference/cutting dimensions to paper and reuses a profile without confusion.

## Implementation boundary

This phase produces this system document only. A later implementation reaches
theme typography/colors/shapes, shared UI components, calculator/results,
profile picker/list/editor and affected UI tests. Calculation geometry and local
storage remain governed by existing specifications. No new domain terms were
resolved, so the glossary stays unchanged. These reversible UI choices do not
meet the ADR threshold.

## Sources and interpretation

- [Material 3 in Compose](https://developer.android.com/develop/ui/compose/designsystems/material3):
  theme roles and Expressive support, including dynamic color.
- [Compose accessibility defaults](https://developer.android.com/develop/ui/compose/accessibility/api-defaults):
  minimum 48dp interactive targets.
- [Compose semantics](https://developer.android.com/develop/ui/compose/accessibility/semantics):
  meaningful labels, states and error semantics.
- Local ui-ux-pro-max searches support clear hierarchy, semantic color and
  Compose accessibility. Generated landing-page layouts and web font/palette
  recommendations did not fit this native calculation tool and were discarded.
- mobile-android-design guidance informs theme roles and native components;
  repository icon rules take precedence over legacy Material Icons examples.

## Validation status

This is a design record, not a validated UI. Final palettes need measured
contrast checks. Implemented components will require light/dark/dynamic-color,
200% text-size, TalkBack, keyboard, inset and adaptive-layout checks, followed
by a seamstress drafting session. Existing unresolved hardware/accessibility
checks remain unresolved.
