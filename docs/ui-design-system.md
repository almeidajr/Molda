# Molda UI design system

Status: confirmed design system; Q1–Q9 accepted. No screen implementation is authorized by
this document. Accepted decisions below extend the existing
[UI/UX design](ui-ux-design.md); existing behavior and data-safety requirements
remain in force.

Implementation is specified in [GitHub issue #35](https://github.com/almeidajr/Molda/issues/35).
The results redesign in [issue #51](https://github.com/almeidajr/Molda/issues/51)
supersedes the result groups, inline explanation and dialog tutorial with the
cutting card, disclosures, a full-screen guide player and a calculator top app
bar; this document describes what shipped.

## Accepted direction

- Prioritize working seamstresses calculating dimensions at the cutting table.
  Keep concise contextual explanations available for occasional users.
- Use a calm, warm sewing-studio character. Material 3's default shapes,
  typography and motion emphasize primary actions and results.
- Define the system and component/state specifications in documentation before
  implementing screens.
- Preserve calculator-on-launch, optional measurement profiles, Brazilian
  Portuguese, centimeters, offline storage and hierarchical navigation.
- Keep Material 3's standard visual system without custom branding: dynamic
  color with system light/dark mode.

## Design tree

- Accepted: audience, visual character, documentation scope, existing flows.
- Accepted: typography and equal numerical hierarchy.
- Accepted: dynamic color only. The warm plum fallback was dropped, since
  minSdk 36 always has dynamic color.
- Accepted: restrained expression, spacing and existing adaptive layouts.
- Accepted: existing component inventory, states and accessibility acceptance.
- Accepted: final shared understanding, including palette validation before implementation.

## Typography

Use the Android system sans-serif through Compose's default font family. Keep
the full Material type scale; map the following roles consistently. Sizes and
line heights are in sp and scale with system preferences.

| Use                                                                    | Material role  | Size / line height | Weight  |
|------------------------------------------------------------------------|----------------|--------------------|---------|
| Profile screen heading                                                 | headlineMedium | 28 / 36            | Regular |
| App bar title, section heading, cutting-card heading, guide step title | titleLarge     | 22 / 28            | Regular |
| Profile name / component title / cutting-card group                    | titleMedium    | 16 / 24            | Medium  |
| Field text / explanatory body                                          | bodyLarge      | 16 / 24            | Regular |
| Supporting text / field errors                                         | bodyMedium     | 14 / 20            | Regular |
| Action label, line number, guide step position                         | labelLarge     | 14 / 20            | Medium  |
| Each calculated radius value                                           | headlineSmall  | 24 / 32            | Regular |

Display all four radii with equal size and weight; labels identify their meaning.
On the cutting card each label sits above its value in bodyLarge and
onSurfaceVariant, and values use tabular figures where the system font has them.
Show centimeters and two decimal places using Brazilian Portuguese formatting.
Keep values and units associated with a non-breaking space. Never truncate
drafting values, field errors or essential action labels. Avoid fixed-height
text containers.

## Color system

The app uses the system's dynamic color scheme, light or dark, and has no
palette of its own. Follow system light/dark appearance without adding
appearance settings. The warm studio identity comes from shape, spacing and
hierarchy because wallpaper colors decide the palette.

| Semantic role                             | Use                                                                                                                                             |
|-------------------------------------------|-------------------------------------------------------------------------------------------------------------------------------------------------|
| primary / onPrimary                       | Calculate and Save fills/labels                                                                                                                 |
| primaryContainer / onPrimaryContainer     | Profile-creation FAB                                                                                                                            |
| secondaryContainer / onSecondaryContainer | Cutting card; tonal guide action                                                                                                                |
| surface / onSurface                       | Screen background, top app bar and essential text                                                                                               |
| surfaceContainerLow                       | Grouped input surfaces                                                                                                                          |
| surfaceContainer                          | Guide step bar                                                                                                                                  |
| surfaceContainerHigh                      | Calculation-limits callout; guide drawing frame, which shows through the transparent Rive artboards; overlays where Material defaults select it |
| onSurfaceVariant                          | Supporting text and secondary labels                                                                                                            |
| outline                                   | Essential field boundaries                                                                                                                      |
| outlineVariant                            | Decorative separators                                                                                                                           |
| error / onErrorContainer / errorContainer | Validation and failure feedback                                                                                                                 |
| tertiary / onSurface                      | Diagram accents / textual labels                                                                                                                |

Use role-matched foreground/background pairs from the dynamic scheme. Target at least 4.5:1 for ordinary text, 3:1 for large text and meaningful
graphical boundaries. Verify actual rendered pairs; do not assume pastel fills
or generated schemes make every combination accessible. Diagram meaning always
uses labels and distinct line styles as well as color.

## Spacing, shape and motion

| Token           | Value | Use                                               |
|-----------------|-------|---------------------------------------------------|
| spaceMicro      | 4dp   | Closely associated decorative details             |
| spaceSmall      | 8dp   | Label/value and icon/text separation              |
| spaceMedium     | 16dp  | Field gaps, group padding, compact screen margins |
| spaceLarge      | 24dp  | Section and adaptive-pane separation              |
| spaceExtraLarge | 32dp  | Major content transitions                         |

Keep these as layout tokens; respect Material component internal padding.
Interactive targets are at least 48dp in both dimensions, with separate hit
areas. Forms and buttons grow with text instead of fitting content to fixed heights.

Use Material shape roles: small 8dp, medium 12dp, large 16dp and extraLarge 28dp.
Grouped surfaces use 12dp corners; the cutting card and modal surfaces use
28dp. Primary actions use full rounded ends. Keep standard outlined field
shapes. Use tonal grouping rather than nested cards or decorative shadows.

The app theme is the Material theme with its default shapes, typography and
standard motion scheme, so Material components take their timing from that one
scheme. The expressive scheme's spatial springs made text field labels overshoot
when a field lost focus. Screen
navigation uses Navigation 3's built-in transitions for now. Motion communicates
pressed state, disclosure and navigation continuity; it never delays calculation
or makes dimensions move while being read. Respect system animation settings and
show the same final state when motion is disabled. Hand-tuned timing (durations,
delays, easing curves) is not allowed.

## Navigation and adaptive layout

Keep the calculator as the launch destination. Its top app bar shows the screen
title and the labeled "Perfis de medidas" action; the bar is not a navigation
destination. "Usar perfil" and the waist's source sit just above the waist
field. Selection uses a modal bottom sheet; editing and creation use a dedicated
profile form. The fabric-folding guide opens as a full-screen player owned by
the same hierarchical route; its close action, "Concluir", system Back and
predictive Back return to the results.
Retain the existing hierarchical Back, unsaved-discard and picker-return rules
in [the behavior specification](ui-ux-design.md).

Compact windows have one readable column. Preserve the existing adaptive rule:
two panes require `360dp × max(fontScale, 1) × 2 + 24dp` of available content
width, with a 24dp gap and a 640dp maximum readable pane width. This is a
content-fit rule, not a device-type test. Inputs sit beside results; a profile
list may sit beside its active editor. Avoid hinges and preserve drafts when
window size or orientation changes. Large text can return the layout to one pane.
One column spans a flat fold and keeps to one side of a half-open or separating
one; two panes split at the fold, or share one side when they cannot fit beside it.
The cutting card follows the results pane. The guide player is one column of
at most 640dp that keeps to one side of a half-open or separating vertical
fold, spans a flat one, and keeps above or below a horizontal one. When its window is under 560dp tall, the drawing scrolls with the
instructions instead of staying pinned, so the fixed step bar and the text both
keep room.

Apply system-bar and keyboard insets. Scroll focused fields and their errors
into view. Primary actions stay in the form flow so they cannot cover fields or
results. No new home screen, bottom navigation, drawer or navigation rail is
needed for the accepted destination hierarchy; the top app bar and the guide
player do not change that.

## Navigation shell

Status: **approved by the product owner on
[#87](https://github.com/almeidajr/Molda/issues/87)**; implementation follows in
[#83](https://github.com/almeidajr/Molda/issues/83)'s later sub-issues; [#88](https://github.com/almeidajr/Molda/issues/88) ships the suite, Moldes, Projetos and Perfis. It
supersedes the "calculator on launch", "no home screen, bottom navigation,
drawer or navigation rail" and "Perfis de medidas app-bar action" lines above,
following [ADR 0003](adr/0003-top-level-navigation-shell.md). The comps (compact
and wide, light and dark, with annotations) were reviewed on #87.

### Decisions recorded on #87

- Perfis uses the `group` glyph.
- The Projetos empty state only defines a project and offers "Ver moldes"; it
  says nothing about when saving arrives.
- On compact windows the navigation bar is hidden while the guide player is open.
- Navigation labels are always shown and auto-size down to a 14sp floor to fit
  the item width at large text.
- On wide windows `Guias` shows the "Escolha um guia na lista." placeholder until
  a guide is chosen, even with a single guide.
- About adds a "Licenças de código aberto" row that opens the licenses screen;
  no GitHub repository link yet. The update rows (#209–#213) sit between
  "Versão" and it.
- Perfis is titled "Perfis de medidas" in the top app bar; "(opcional)" moves into
  the storage warning.

### Destinations

| Order | Label    | Material Symbol (unselected / selected) | Root screen                                                                      | Back at root       |
|-------|----------|-----------------------------------------|----------------------------------------------------------------------------------|--------------------|
| 1     | Moldes   | `design_services` / filled              | Catalog of pattern drafts; start destination                                     | Exits the app      |
| 2     | Projetos | `folder` / filled                       | Saved projects, most recently changed first; the empty state when there are none | Switches to Moldes |
| 3     | Guias    | `menu_book` / filled                    | Library of guides                                                                | Switches to Moldes |
| 4     | Perfis   | `group` / filled                        | Measurement profiles (existing list-detail)                                      | Switches to Moldes |

Glyphs are Material Symbols Outlined, weight 400, 24dp, stored as XML vectors
(`ic_<name>` and `ic_<name>_filled`) with no theme tint. The filled glyph marks
the selected destination. `group` was chosen over a single avatar because
profiles belong to the people someone sews for, not to an account.

### Component contracts

| Component                    | Content and interaction                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                             | States, accessibility and recovery                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                         |
|------------------------------|-----------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------|--------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------|
| Navigation suite             | `NavigationSuiteScaffold` around the Navigation 3 `NavDisplay`. Compact (<600dp): short navigation bar, 64dp, `surfaceContainer`. Medium and expanded: collapsed wide navigation rail, 96dp, `surface`, items top-aligned, no FAB or menu in the rail. Labels always shown, auto-sizing down to 14sp to fit the item width.                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                         | Selected item: `secondaryContainer` pill with the filled glyph in `onSecondaryContainer`, label in `secondary`; unselected in `onSurfaceVariant`. Selection is never color alone (the pill measures 1.11–1.97:1 against its container): filled glyph, label color and selected semantics carry it. Icons are decorative; the label is the accessible name. Keyboard: one focus group, arrow keys move, Enter or Space selects. Re-selecting pops to the root. Items ≥90 × 64dp.                                                                                                          |
| Top app bar (root screens)   | 64dp, Title Large heading with the destination's name ("Moldes", "Projetos", "Guias", "Perfis de medidas"), then the About action: `info` icon button, 48dp, "Sobre o Molda".                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                       | Title wraps at large text and keeps 16dp from the action. After switching destination, focus lands on the title.                                                                                                                                                                                                                                                                                                                                                                                                                                                                           |
| Top app bar (pushed screens) | Up arrow (`arrow_back`, "Voltar") and the screen title; no About action. The skirt calculator no longer carries "Perfis de medidas".                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                | Up and system Back pop the current destination's stack.                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                    |
| Catalog / library entry      | Whole-row target on `surfaceContainerLow`, 12dp corners, 16dp padding, ≥88dp tall: a 56dp `secondaryContainer` thumbnail (a 24dp icon, a skirt for the skirt and folded fabric for fabric folding, decorative), the name in Title Medium, one Body Medium sentence in `onSurfaceVariant`, and a decorative `chevron_right`. A one-sentence Body Medium intro sits above the entries.                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                | One merged TalkBack node announced as a button. Only existing moldes and guides are listed; no "em breve" entries. Wide windows: an adaptive grid with a 320dp minimum column, at most 1040dp wide. In a list-detail pane, the selected entry fills with `secondaryContainer`, drops the chevron and is exposed as selected.                                                                                                                                                                                                                                                               |
| Example label (guide)        | Pinned with the drawing at the top of a guide opened from Guias: `surfaceContainerHighest`, 16dp corners, decorative `info` glyph, "Exemplo — não são as suas medidas" in Label Large, the example inputs ("Cintura 80, folga 2, comprimento 60, margens 1 e 2 (cm)") in Body Small, and the "Calcular a minha saia" text button.                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                   | Never scrolls away and never appears when the guide carries a calculation's radii. A neutral surface on purpose: `secondaryContainer` means "your result" and the tertiary roles belong to the drawing. "Calcular a minha saia" switches to Moldes and opens the calculator, keeping any draft. The guide keeps one filled action ("Próximo").                                                                                                                                                                                                                                             |
| Guide player in the shell    | The existing full-screen player, renamed "Guia de dobra do tecido" (entry button) and "Fechar guia" (close). From a result it opens on the Moldes stack with the calculated radii; from Guias it opens on the Guias stack with the example radii.                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                   | Compact: the navigation bar is hidden while the player is open (`NavigationSuiteScaffoldState.hide()`); the fixed step bar stays the only bottom control. Wide: the rail stays; from Guias the guide is the list-detail detail pane with no close button, and the placeholder reads "Escolha um guia na lista.".                                                                                                                                                                                                                                                                           |
| Projetos empty state         | Start-aligned in the content column: 48dp `folder` glyph (decorative, `onSurfaceVariant`), Title Large "Nenhum projeto salvo", Body Large definition "Um projeto guarda, com um nome, as medidas, as escolhas e o resultado de um molde, mesmo que o perfil usado mude depois.", and the filled "Ver moldes".                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                       | No dates, promises or statement about when saving arrives. "Ver moldes" switches to Moldes. Wide: one pane, at most 640dp, centered.                                                                                                                                                                                                                                                                                                                                                                                                                                                        |
| Perfis root                  | The existing list, editor, picker and deletion flow. The title moves to the top app bar ("Perfis de medidas"); the in-content "Perfis de medidas (opcional)" heading and the "Voltar ao cálculo" button go; the storage warning starts with "Opcionais."                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                            | The "Novo perfil" FAB sits 16dp above the navigation bar on compact and at the start edge of the list pane in two panes. "Criar perfil" from the picker opens the editor on the Moldes stack, as #83 specifies.                                                                                                                                                                                                                                                                                                                                                                            |
| About screen                 | Pushed on the current destination's stack: up arrow, title "Sobre o Molda", "Molda" in Headline Medium, a Body Large description of the product and its limits, an offline/on-device line, a divider, a "Versão" list row with `versionName`, the update rows, and a "Licenças de código aberto" row (decorative `license` glyph and `chevron_right`, whole-row 56dp target) that opens the licenses screen. No GitHub repository link yet.                                                                                                                                                                                                                                                                                                                                                                                                                                                         | Local builds show no update rows.                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                          |
| About update rows            | Between "Versão" and "Licenças de código aberto" ([ADR 0007](adr/0007-self-updates-from-github-releases.md)): a status row ("Verificando atualizações…", "O Molda está atualizado", "Versão X disponível", or "Não foi possível verificar atualizações" with "Confira a conexão e tente de novo."); for an available update, its download action, "Ver a página da versão" (trailing `open_in_new`) and, always, "Verificar atualizações" (leading `refresh`). The download action is "Atualizar" (`download`), then "Baixando a atualização…" with a linear progress indicator (indeterminate until the size is known), then "Instalar" (`install_mobile`); a failure reads "O download falhou" with why ("Confira a conexão e tente de novo." or "O arquivo baixado não confere com o publicado.") and "Tentar novamente" (`refresh`). Actions are whole-row 56dp targets with decorative glyphs. | The status row and a failure row are each one merged TalkBack node. "Verificar atualizações" is disabled while checking. A failed check is never an error color. A download that finishes while Molda is in the foreground starts the install on any screen; one that finished in the background waits for "Instalar". Installing asks first when an editor has unsaved changes, then, without the "install unknown apps" permission, opens Molda's setting and, on returning without it, explains in a dialog ("Permita instalar a atualização") with "Tentar novamente" and "Cancelar". |
| Startup update notice        | On each cold start with an update available, the app's one snackbar reads "Versão X disponível" with "Atualizar", or "Instalar" when it is already downloaded, and a dismiss action.                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                | Stays until acted on or dismissed, and never comes back before the next cold start. Nothing shows while the update downloads, when up to date, on a failed check or on a local build. Its actions take the same paths as About's.                                                                                                                                                                                                                                                                                                                                                          |

### Layout per window size

- **Compact (<600dp):** navigation bar; one column; 16dp margins.
- **Medium (600–839dp):** rail; panes follow the existing content-fit rule
  measured beside the rail (`360dp × max(fontScale, 1) × 2 + 24dp`), so a
  medium window shows one pane.
- **Expanded (≥840dp):** rail; two panes where the rule fits: `Guias` list beside
  the guide, Perfis list beside the editor, calculator inputs beside the cutting
  card.
- **Foldables:** the rail stays at the start edge of the left half; two panes
  split at a vertical fold, or share the larger side or stack when the left half
  beside the rail cannot hold a pane. In tabletop posture content moves to the
  larger half and no navigation item straddles the fold. A flat fold is no fold.

### Accessibility checks for the shell

Verify bar, rail and example-label contrast in light and dark dynamic schemes.
At 200% text the 12sp label becomes about 24sp, and "Projetos" is about
90dp, the width of one bar item on a 360dp phone: labels never ellipsize or
wrap and auto-size down to a 14sp floor when the scaled size would overflow the
item width; the bar grows in height with the text. Verify on a device at 200%
and 360dp.

## Reusable component contracts

These are design contracts, not a requirement to create a wrapper for every
Material component. Reuse the current Compose components and state ownership.

| Component           | Content and interaction                                                                                                                                                                                                                                                                                          | States and recovery                                                                                                                                                                                                                                                                                                                                                                                                                                                           |
|---------------------|------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------|-------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------|
| Measurement field   | Outlined field, persistent label, cm suffix, supporting guidance, decimal keyboard; explicit zero for allowances                                                                                                                                                                                                 | Unedited, focused, entered, invalid; submit-first errors, first invalid field focused, revalidate corrected fields on blur after first attempt                                                                                                                                                                                                                                                                                                                                |
| Section group       | Heading, concise guidance, related fields with 16dp padding and gaps                                                                                                                                                                                                                                             | Content wraps; heading semantics; no extra interactive card wrapper                                                                                                                                                                                                                                                                                                                                                                                                           |
| Top app bar         | Calculator title (heading) and labeled "Perfis de medidas" text action                                                                                                                                                                                                                                          | Title wraps at large text without running under the action; 48dp action target                                                                                                                                                                                                                                                                                                                                                                                                |
| Primary action      | Filled "Calcular" (full width at the end of the form) or "Salvar" with visible label                                                                                                                                                                                                                             | Calculate remains enabled for incomplete input; show save progress and prevent duplicate writes; retain values on failure                                                                                                                                                                                                                                                                                                                                                     |
| Secondary action    | Tonal or text button for reuse/edit/cancel                                                                                                                                                                                                                                                                       | Visible pressed/focus feedback; accessible name; destructive actions use explicit wording                                                                                                                                                                                                                                                                                                                                                                                     |
| Profile row         | Name, waist, measurement date; notes when needed to distinguish identical rows                                                                                                                                                                                                                                   | Picker tap selects; management tap edits with separately labeled overflow; loading, empty, failure/Retry and ready list                                                                                                                                                                                                                                                                                                                                                      |
| Profile picker      | Material modal sheet, profile rows and creation action                                                                                                                                                                                                                                                           | Dismissal changes nothing; creation opens the editor on the Moldes stack, saving selects the new profile and canceling returns to the picker; manual calculation remains available                                                                                                                                                                                                                                                                                           |
| Cutting card        | secondaryContainer, 28dp corners; "Raios para o molde" heading, reference and cutting groups, each radius with diagram number, line-style sample, label and value; then the drafting diagram under "Diagrama das linhas", directly after the radii or beside them when two 260dp × max(fontScale, 1) columns fit | Each value shown and announced once, outside the expanded formulas; heading announces a successful calculation once; Calculate hides the keyboard and brings the whole card into view; hidden after any input change; wide empty pane explains how to calculate                                                                                                                                                                                                               |
| Results follow-up   | One wrapping action row (tonal guide action, "Editar medidas"), then the "Medidas utilizadas (cm)" summary as one wrapping line with the unit stated once, then the "Limites do cálculo" callout                                                                                                                 | "Editar medidas" focuses waist; the callout keeps every honest limit                                                                                                                                                                                                                                                                                                                                                                                                          |
| Drafting diagram    | Square sketch up to 400dp on the cutting card's own surface, with numbered reference/cutting lines in distinct styles; the card's radius rows are its key                                                                                                                                                        | Text equivalent gives each line's number, style and meaning, the drawing order, coincident lines and allowance direction, but not the radius values, which the cutting card states once (a departure from the earlier "lists all dimensions" contract, confirmed by the owner); no color-only information or gesture-only controls                                                                                                                                            |
| Disclosures         | "Como levar os raios ao papel" and "Fórmulas" text buttons with a chevron; both start collapsed                                                                                                                                                                                                                  | Expose "Recolhido"/"Expandido"; 48dp target; content wraps and stays readable with scaled text; label wraps before the chevron shrinks                                                                                                                                                                                                                                                                                                                                        |
| Guide player        | Full screen: close action and step title, pinned Rive drawing that plays on entry and toggles pause on tap (badge shows the action), scrolling instructions, fixed bar with "Anterior", "Etapa n de 6", "Próximo"/"Concluir"                                                                                     | "Anterior" disabled on step 1; "Concluir" closes; Back closes; focus returns to the entry; step survives recreation; each step plays on entry and a pause resets on step change; reduced motion shows each step's final drawing with no toggle or badge; the drawing loads once per player and its frame stays empty, without text, while it loads, with the expressive loading indicator only after 400ms, held at least 500ms; bar fully visible on every step at 200% text |
| Feedback            | Inline field/action errors, loading indicators and brief success snackbar                                                                                                                                                                                                                                        | Announce results/errors without duplicate speech; success only after storage succeeds; errors persist until resolved                                                                                                                                                                                                                                                                                                                                                          |
| Date entry          | Editable dd/mm/aaaa field with Material date picker                                                                                                                                                                                                                                                              | Initially blank; syntax and future-date errors; explicit actual measurement date                                                                                                                                                                                                                                                                                                                                                                                              |
| Confirmation dialog | Named deletion confirmation; unsaved-discard confirmation                                                                                                                                                                                                                                                        | Clear cancel and destructive actions; retain draft after cancel or failed save/delete                                                                                                                                                                                                                                                                                                                                                                                         |

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

## Known gaps

- The guide drawing's line numbers are stroked vector numerals, not text, so
  they do not follow the system font size; the written instructions carry every
  number and value as scalable text.

## Validation status

This is a design record, not a validated UI. Final palettes need measured
contrast checks. Implemented components will require light/dark/dynamic-color,
200% text-size, TalkBack, keyboard, inset and adaptive-layout checks, followed
by a seamstress drafting session. Existing unresolved hardware/accessibility
checks remain unresolved.
