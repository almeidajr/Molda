---
name: Molda
description: An explained pattern-calculation assistant for sewists drafting on paper.
colors:
  mulberry-dye: "#784F82"
  on-mulberry-dye: "#FFFFFF"
  mulberry-wash: "#FBD7FF"
  on-mulberry-wash: "#5F3869"
  heather-plum: "#6A596C"
  heather-wash: "#F2DBF3"
  on-heather-wash: "#514154"
  lichen-green: "#4F644A"
  lichen-wash: "#D1E9C9"
  raw-calico: "#FFF8F3"
  calico-low: "#FEF2E5"
  calico-mid: "#F8ECDF"
  calico-high: "#F2E6D9"
  walnut-ink: "#201B13"
  faded-walnut: "#4E4539"
  basting-outline: "#807667"
  basting-faint: "#D2C5B4"
  snip-red: "#BA1A1A"
  snip-red-wash: "#FFDAD6"
  dark-mulberry-dye: "#E7B6F0"
  dark-heather-wash: "#514154"
  dark-on-heather-wash: "#F2DBF3"
  dark-lichen-green: "#B5CDAE"
  dark-lichen-wash: "#384C34"
  dark-raw-calico: "#17130B"
  dark-calico-low: "#201B13"
  dark-calico-mid: "#241F17"
  dark-calico-high: "#2F2921"
  dark-walnut-ink: "#ECE1D4"
  dark-snip-red: "#FFB4AB"
typography:
  headline-medium:
    fontFamily: "System sans-serif (Roboto)"
    fontSize: "28sp"
    fontWeight: 400
    lineHeight: "36sp"
  headline-small:
    fontFamily: "System sans-serif (Roboto)"
    fontSize: "24sp"
    fontWeight: 500
    lineHeight: "32sp"
  title-large:
    fontFamily: "System sans-serif (Roboto)"
    fontSize: "22sp"
    fontWeight: 400
    lineHeight: "28sp"
  title-medium:
    fontFamily: "System sans-serif (Roboto)"
    fontSize: "16sp"
    fontWeight: 500
    lineHeight: "24sp"
  body-large:
    fontFamily: "System sans-serif (Roboto)"
    fontSize: "16sp"
    fontWeight: 400
    lineHeight: "24sp"
  body-medium:
    fontFamily: "System sans-serif (Roboto)"
    fontSize: "14sp"
    fontWeight: 400
    lineHeight: "20sp"
  label-large:
    fontFamily: "System sans-serif (Roboto)"
    fontSize: "14sp"
    fontWeight: 500
    lineHeight: "20sp"
rounded:
  small: "8dp"
  medium: "16dp"
  large: "16dp"
  extra-large: "28dp"
  full: "9999dp"
spacing:
  micro: "4dp"
  small: "8dp"
  medium: "16dp"
  large: "24dp"
components:
  button-primary:
    backgroundColor: "{colors.mulberry-dye}"
    textColor: "{colors.on-mulberry-dye}"
    typography: "{typography.label-large}"
    rounded: "{rounded.full}"
    height: "48dp"
  button-text:
    textColor: "{colors.mulberry-dye}"
    typography: "{typography.label-large}"
    rounded: "{rounded.full}"
    height: "48dp"
  button-tonal:
    backgroundColor: "{colors.heather-wash}"
    textColor: "{colors.on-heather-wash}"
    typography: "{typography.label-large}"
    rounded: "{rounded.full}"
    height: "48dp"
  disclosure:
    textColor: "{colors.mulberry-dye}"
    typography: "{typography.label-large}"
    rounded: "{rounded.full}"
    height: "48dp"
  top-app-bar:
    backgroundColor: "{colors.raw-calico}"
    textColor: "{colors.walnut-ink}"
    typography: "{typography.title-large}"
    height: "64dp"
  fab-create-profile:
    backgroundColor: "{colors.mulberry-wash}"
    textColor: "{colors.on-mulberry-wash}"
    typography: "{typography.label-large}"
    rounded: "{rounded.large}"
    height: "56dp"
  measurement-field:
    textColor: "{colors.walnut-ink}"
    typography: "{typography.body-large}"
    rounded: "{rounded.small}"
  section-group:
    backgroundColor: "{colors.calico-low}"
    textColor: "{colors.walnut-ink}"
    typography: "{typography.title-large}"
    rounded: "{rounded.medium}"
    padding: "16dp"
  cutting-card:
    backgroundColor: "{colors.heather-wash}"
    textColor: "{colors.on-heather-wash}"
    typography: "{typography.headline-small}"
    rounded: "{rounded.extra-large}"
    padding: "24dp"
  measurement-summary:
    textColor: "{colors.walnut-ink}"
    typography: "{typography.body-medium}"
  boundary-callout:
    backgroundColor: "{colors.calico-high}"
    textColor: "{colors.walnut-ink}"
    typography: "{typography.body-large}"
    rounded: "{rounded.medium}"
    padding: "16dp"
  profile-row:
    backgroundColor: "{colors.calico-low}"
    textColor: "{colors.walnut-ink}"
    typography: "{typography.title-medium}"
    rounded: "{rounded.medium}"
  picker-sheet:
    backgroundColor: "{colors.calico-low}"
    textColor: "{colors.walnut-ink}"
    rounded: "{rounded.extra-large}"
  picker-row:
    backgroundColor: "{colors.calico-high}"
    textColor: "{colors.walnut-ink}"
    typography: "{typography.title-medium}"
    rounded: "{rounded.medium}"
  modal-surface:
    backgroundColor: "{colors.calico-high}"
    textColor: "{colors.walnut-ink}"
    rounded: "{rounded.extra-large}"
  guide-top-bar:
    backgroundColor: "{colors.raw-calico}"
    textColor: "{colors.walnut-ink}"
    typography: "{typography.title-large}"
    height: "64dp"
  guide-drawing-frame:
    backgroundColor: "{colors.calico-high}"
    rounded: "{rounded.large}"
    padding: "8dp"
  guide-step-lead:
    textColor: "{colors.walnut-ink}"
    typography: "{typography.title-medium}"
  guide-step-detail:
    textColor: "{colors.faded-walnut}"
    typography: "{typography.body-medium}"
  guide-step-bar:
    backgroundColor: "{colors.calico-mid}"
    textColor: "{colors.faded-walnut}"
    typography: "{typography.label-large}"
    padding: "8dp 16dp"
---

# Design System: Molda

## Overview

**Creative North Star: "The Tailor's Notebook"**

Molda should feel like the drafting notebook a skilled tailor keeps next to the cutting table. The pages are warm, the figures are exact, and a worked explanation always sits in the margin. Every screen is a page you can check: the inputs at the top, the assumptions stated, the results set apart on a cutting card, and the formulas available on request. The character is tactile and confident. Primary actions are firm pill shapes you can rely on, and grouped content reads like swatches laid on raw calico, stacked by tone rather than lifted with shadows.

The system is native Material 3, themed rather than reinvented. Dynamic color is the runtime default, so the dyed-fabric palette is the fallback identity. The notebook character must also survive in shape, spacing and hierarchy when the wallpaper replaces the plum. Density is calm: one readable column on compact windows, generous 16dp rhythm, and two panes only when the content truly fits. Interface motion is Material's own and never moves a dimension while someone is reading it; the one choreographed motion is the guide drawing, which plays on each step and pauses with a tap.

**Key Characteristics:**
- Dynamic color first; the warm Mulberry/Lichen/Calico palette is the fallback.
- Flat tonal layering; no decorative shadows, no nested cards.
- All four calculated radii have equal typographic weight; their labels carry the meaning.
- Reference and cutting lines are told apart by line style and label, never by color alone.
- System sans-serif on the full Material type scale, in sp, so it scales to 200%.

## Colors

A muted, warm palette taken from hand-dyed cloth: mulberry thread on unbleached calico, with a lichen-green accent. Every value is a Material role in `ui/theme/Color.kt`; the dark variants are the same roles in the dark fallback scheme.

### Primary
- **Mulberry Dye** (`mulberry-dye`, `primary`): the filled primary actions ("Calcular", "Salvar perfil", "Próximo", "Novo perfil" in the picker) and every text-button label. **Dark Mulberry Dye** (`dark-mulberry-dye`) in dark mode.
- **Mulberry Wash** (`mulberry-wash`, `primaryContainer`): the profile-creation FAB and nothing else.

### Secondary
- **Heather Plum** (`heather-plum`) / **Heather Wash** (`heather-wash`, `secondaryContainer`): Heather Wash with **On Heather Wash** (`on-heather-wash`) is the cutting card, the tonal "Guia de dobra do tecido" button and the fabric in the guide drawing. In dark mode the pair inverts to **Dark Heather Wash** (`dark-heather-wash`) under **Dark On Heather Wash** (`dark-on-heather-wash`). It is a different role from the calico groups, so the results read as their own page even when dynamic color replaces the palette.

### Tertiary
- **Lichen Green** (`lichen-green`, `tertiary`) / **Lichen Wash** (`lichen-wash`, `tertiaryContainer`): reserved for the guide drawing. Lichen Wash fills the layer being folded; Lichen Green traces a cut while it plays. Dark mode uses **Dark Lichen Green** (`dark-lichen-green`) and **Dark Lichen Wash** (`dark-lichen-wash`). Lichen always sits beside a line pattern, number or sentence that says the same thing.

### Neutral
- **Raw Calico** (`raw-calico`, `surface`): the screen background, the app bar and the guide player. **Dark Raw Calico** (`dark-raw-calico`).
- **Calico Low** (`calico-low`, `surfaceContainerLow`): input groups, management profile rows, and the profile picker sheet by Material default. **Dark Calico Low** (`dark-calico-low`).
- **Calico Mid** (`calico-mid`, `surfaceContainer`): the guide's fixed step bar and Material's dropdown menu. **Dark Calico Mid** (`dark-calico-mid`).
- **Calico High** (`calico-high`, `surfaceContainerHigh`): the "Limites do cálculo" callout, the guide drawing frame, dialogs, and the picker's profile rows, two tonal steps above their Calico Low sheet. **Dark Calico High** (`dark-calico-high`).
- **Walnut Ink** (`walnut-ink`, `onSurface`): essential text. **Dark Walnut Ink** (`dark-walnut-ink`).
- **Faded Walnut** (`faded-walnut`, `onSurfaceVariant`): supporting text on calico surfaces only, such as the waist source line, the step position and each guide step's "Detalhes".
- **Basting Outline** (`basting-outline`): essential field boundaries. **Basting Faint** (`basting-faint`): decorative separators only.
- **Snip Red** (`snip-red`, `error`) / **Snip Red Wash** (`snip-red-wash`): validation and failure text. **Dark Snip Red** (`dark-snip-red`).

### Named Rules
**The Dynamic-First Rule.** Always read colors from `MaterialTheme.colorScheme` roles; never hard-code a hex value in a screen. The dyed-fabric palette exists in `ui/theme/Color.kt` only as the fallback scheme.

**The Not-By-Color Rule.** No meaning travels on color alone. Reference and cutting lines, errors and states also carry a label, a line style or text.

**The On-Its-Container Rule.** Text and strokes take the "on" role of the surface they sit on. Everything on the cutting card (heading, group titles, labels, values, stroke samples, diagram lines, numbers and the note) is On Heather Wash (`onSecondaryContainer`); quieter elements are quieter through type size and weight, never through a borrowed role such as `onSurfaceVariant`, which drops to grey-on-slate in dark mode. Checked pairs: text at least 4.5:1 and meaningful graphics at least 3:1 in light, dark and dynamic schemes.

## Typography

**Display Font:** System sans-serif (Roboto through `FontFamily.Default`)
**Body Font:** System sans-serif
**Label/Mono Font:** System sans-serif; numbers are not set in a separate face.

**Character:** One honest, legible face, like the neat hand of a notebook. Hierarchy comes from Material roles, not from a decorative typeface.

### Hierarchy
- **Headline Medium** (400, 28/36sp): profile headings ("Perfis de medidas (opcional)", "Novo perfil de medidas", "Usar perfil" on the picker). The calculator's title lives in its top app bar.
- **Headline Small** (500, 24/32sp): the four radius values and nothing else, with tabular figures (`tnum`).
- **Title Large** (400, 22/28sp): the app bar and guide step titles, group headings ("Medida do corpo", "Escolhas da peça", "Margens de corte", "Identificação e medidas") and the cutting card's "Raios para o molde". All are semantic headings.
- **Title Medium** (500, 16/24sp): subheadings ("Linhas de referência", "Linhas de corte", "Diagrama das linhas", "Medidas utilizadas (cm)", "Limites do cálculo"), profile names and picker rows, and each guide step's lead sentence.
- **Body Large** (400, 16/24sp): field text, explanations, and the line labels on the cutting card.
- **Body Medium** (400, 14/20sp): field guidance and errors, the measurement summary, the diagram's note, and guide "Detalhes".
- **Label Large** (500, 14/20sp): action labels, the line numbers on the card and diagram, and "Etapa n de 6".

### Named Rules
**The Equal Radii Rule.** All four radii share Headline Small. No result is bolder than another; the labels say which is a reference line and which is a cutting line.

**The Never-Truncate Rule.** Never truncate drafting values, errors or essential action labels. Keep the value and its "cm" together with a non-breaking space, format with pt-BR decimal commas, and avoid fixed-height text containers.

## Layout

Spacing uses a four-step scale: micro (4dp) binds a line key to its value, a field to its source line, and summary heading to its text; small (8dp) separates label/value and icon/text pairs, action-row buttons and the disclosure pair; medium (16dp) is page margin, group padding and the gap between blocks and fields; large (24dp) separates panes and the cutting card's sections. Material components keep their own internal padding.

Compact windows show one readable column with 16dp margins. Two panes appear only when the width is at least `360dp × max(fontScale, 1) × 2 + 24dp`, with a 24dp gap, and every pane is capped at 640dp and centred. This is a content-fit rule, not a device test. Inputs sit beside the cutting card, and the profile list sits beside its editor. Folds: one column spans a flat fold and keeps to one side of a half-open or separating one; two panes split at the fold, or share one side when either would fall below a pane plus its gap; a horizontal (tabletop) fold always moves content to the larger half. The full-screen guide player counts as one column even on a wide window. Primary actions stay in the form flow, never floating over fields or results; the profile list reserves 88dp at its end so the FAB never covers a row, and the FAB sits at the start edge in two panes and the end edge on compact. The app runs edge to edge, handles system-bar and IME insets, and lifts content above a visible snackbar.

**The Fits-Or-Stacks Rule.** If placing two panes side by side squeezes either one below 360dp at the current font scale, stack them.

## Elevation & Depth

The system is flat and depth comes from tone. Surfaces step through the calico container ramp, from Raw Calico (page) to Calico Low (groups and rows) to Calico Mid (the step bar) to Calico High (callout, drawing frame, dialogs, picker rows). The cutting card steps out of the ramp onto Heather Wash, so the results never depend on one calico tone. No screen adds a shadow; only Material's own defaults on the FAB and menus remain.

**The Swatch Stack Rule.** Group content with a tonal surface, never a shadow, and never nest one grouped surface inside another.

## Shapes

Corners are softly rounded and generous. Input groups, profile rows (in the list and the picker) and the limits callout use medium (16dp); the FAB and the guide drawing frame use large (16dp); the cutting card, dialogs and the picker sheet use extra-large (28dp). Buttons and disclosures have fully rounded pill ends. Outlined fields keep Material's small corner (8dp). The one recurring motif is the quarter arc from the drafting diagram, which mirrors the folded fabric.

## Components

### Buttons
Firm, pill-shaped and easy to trust under the thumb.
- **Shape:** fully rounded ends, at least 48dp tall, growing with their text.
- **Primary:** filled Mulberry Dye with an On Mulberry label, one per page: "Calcular" (full width at the end of the form, enabled even for incomplete input so it can explain what needs fixing), "Salvar perfil"/"Salvar alterações", the picker's "Novo perfil", and the step bar's "Próximo"/"Concluir".
- **Tonal:** Heather Wash, used once: "Guia de dobra do tecido".
- **Text:** every other action ("Perfis de medidas", "Usar perfil", "Editar medidas", "Voltar ao cálculo", "Anterior", "Cancelar", dialog actions), Mulberry Dye label with Material ripple.
- **Destructive:** reached through a row's labelled "Ações: <nome>" menu, then confirmed by name in a dialog ("Excluir perfil …?", "Confirmar exclusão"); never an unlabelled icon.

### Disclosures
- **Style:** a text button with its label and the Material Symbols `expand_more` chevron, flipped vertically when open. The label wraps before the chevron shrinks.
- **Behaviour:** "Como levar os raios ao papel", "Fórmulas" and each guide step's "Detalhes" start collapsed, keep their state through recreation, and expose "Recolhido" or "Expandido" to TalkBack. The results' two disclosures sit 8dp apart as one pair. "Fórmulas" lists values in centimetres, ends each line in its result, and keeps the note that r is never rounded mid-calculation.

### Section groups
- **Surface:** Calico Low, medium corners (16dp), 16dp padding, 16dp between heading and fields, no shadow.
- **Content:** a Title Large heading, then fields. The group itself is not interactive.

### Inputs / Fields
- **Style:** Material outlined text field with a persistent label that carries the unit ("Circunferência da cintura (cm)"), Body Medium guidance underneath and a decimal keyboard. The waist field has its source line above it, "Medida manual" or "Cintura copiada do perfil: <nome>" in Faded Walnut, beside the "Usar perfil" text button.
- **Focus:** Material's primary outline and label shift; IME Next walks the fields and Done calculates.
- **Error:** Snip Red outline with the specific message in place of the guidance. Errors show first on submit, the first invalid field takes focus, and corrected fields revalidate on blur. A calculation failure appears as Snip Red text above the results, announced assertively.
- **Date:** the profile date field adds a trailing calendar icon button ("Escolher data no calendário") that opens Material's date picker; future dates are not selectable.

### Top app bar
- **Style:** a Material top app bar on the calculator only, titled "Saia godê inteira" (a heading) with the labelled "Perfis de medidas" text action. The title keeps 16dp clear of the action so it wraps at large text instead of running under it.

### Navigation
- **Model:** hierarchical. The app opens on the calculator; profile management is the app bar's action and returns through "Voltar ao cálculo" or Back. "Usar perfil" opens the picker. There is no home screen, bottom bar, rail or drawer, and System Back and predictive Back are always respected. Leaving an edited profile asks "Descartar alterações?" first.
- **Picker:** a Material modal bottom sheet (extra-large top corners, Calico Low by Material default) with the Headline Medium "Usar perfil", a "Cancelar seleção" text button, one row per profile ("Usar perfil: <nome> — <cintura> cm — <data>", Title Medium in Walnut Ink) on Calico High with medium corners, 16dp apart, so each row reads as its own tappable item on the Calico Low sheet in every scheme, and the filled "Novo perfil".

### Navigation shell (approved on #87)
Approved by the product owner on #87; the suite, `Moldes`, `Projetos` and `Perfis` are implemented by #88, the `Guias` library and its guide by #89, creating a profile from the picker by #90 and About by #91. It replaces the Navigation model above, the "Perfis de medidas" app-bar action and the "no bottom bar, rail or drawer" Don't once it ships, per `docs/adr/0003-top-level-navigation-shell.md`. Full contracts and the recorded decisions live in `docs/ui-design-system.md` under "Navigation shell".
- **Destinations:** `Moldes` (`design_services`, start), `Projetos` (`folder`), `Guias` (`menu_book`), `Perfis` (`group`); Material Symbols Outlined, filled when selected, labels always shown and auto-sizing down to 14sp at large text. Each keeps its own back stack; re-selecting pops to its root; Back at another root goes to `Moldes`; Back at the `Moldes` root exits.
- **Suite:** a 64dp short navigation bar on Calico Mid (`surfaceContainer`) under 600dp; a 96dp collapsed wide rail on Raw Calico from 600dp. The selected item is a Heather Wash pill around the filled glyph with a Heather Plum (`secondary`) label; selection never rests on the pill's colour alone.
- **Root top app bars:** the destination name as a Title Large heading and an `info` icon button, "Sobre o Molda", which pushes the About screen ("Versão" and a "Licenças de código aberto" row) on the current stack. Pushed screens show an up arrow and no About.
- **Catalog and library entries:** whole-row Calico Low targets with medium corners, a 56dp Heather Wash thumbnail (a skirt for the skirt, a folded square for fabric folding), Title Medium name, one Body Medium sentence and a decorative chevron. Only existing moldes and guides; no "em breve".
- **Example label:** a pinned `surfaceContainerHighest` note at the top of a guide opened from `Guias`, "Exemplo — não são as suas medidas", with the example inputs and the "Calcular a minha saia" text button. Neutral on purpose: Heather Wash means the user's own result.
- **Guide in the shell:** renamed "Guia de dobra do tecido" / "Fechar guia". The compact navigation bar hides while it is open; on wide windows from `Guias` it is the detail pane, with no close button, and the pane shows "Escolha um guia na lista." until a guide is chosen.
- **Projetos:** a start-aligned empty state, "Nenhum projeto salvo", the glossary's definition of a project and a filled "Ver moldes"; nothing about when saving arrives.
- **Saving a project (#92):** an outlined "Salvar projeto" in the result's action row opens a dialog with "Nome do projeto" prefilled as "Saia godê · <perfil> · <data>" (no profile part for a typed waist); a blank name is refused inline, a storage failure is shown in the dialog and keeps the result, and success closes it with the "Projeto salvo em Projetos." snackbar. Projetos then lists one Calico Low row per project, newest change first: the name in Title Medium over "Saia godê · <data>".
- **Opening a project (#93):** a row opens the project read-only under its name as the title: "Medidas de <perfil>, tiradas em <data>." (or "Cintura digitada, sem perfil de medidas."), then the stored cutting card and diagram (never recalculated), the tonal guide entry and the measurements used. The guide opened from it plays on the stored radii and Back returns to the project. On wide windows Projetos is list-detail: "Escolha um projeto na lista." until a row is chosen, the chosen row fills with the secondary container and is selected, and the project pane has no up arrow.
- **Renaming and deleting a project (#95):** each Projetos row and the opened project (under its source line) have an "Ações: <nome>" text button whose menu offers "Renomear projeto" and "Excluir projeto". Renaming reuses the save dialog's name field and blank-name refusal, then moves the project to the top as its latest change with the "Projeto renomeado." snackbar. Deleting asks "Excluir projeto <nome>?" with "Confirmar exclusão" and "Cancelar"; a storage failure keeps the project and the dialog, with the error inside it. Deleting the opened project returns to the list, or to the placeholder on wide windows.
- **Editing a project (#94):** an outlined "Editar" before the opened project's "Ações: <nome>" loads every saved input and its source profile into the skirt calculator, titled "Editar <nome>", on the Projetos stack with its own draft (the Moldes calculator is untouched), and recalculates. The result's action row offers outlined "Salvar", which updates the project in place and returns to it with the "Projeto atualizado." snackbar (a failure keeps the editor and says so), and "Salvar como novo", which opens the save dialog and then shows the new project. Leaving with changes (Back, the up arrow or re-selecting Projetos) asks "Descartar alterações?" with "As alterações deste projeto ainda não foram salvas.", like profile editing.

### Profile list and editor
- **List:** the Headline Medium title, the on-device storage warning, then one Calico Low row per profile (medium corners) with the name in Title Medium, waist and date, any notes, and the "Ações: <nome>" text button. Empty, loading and load-error states are plain text, with a retry action on error.
- **Editor:** a Headline Medium heading, the storage warning when creating, one "Identificação e medidas" section group, then the filled save button above a "Cancelar" text button.
- **FAB:** "Novo perfil", an extended FAB in Mulberry Wash with large corners (16dp). It is the app's only FAB and appears only on profile management, hidden on compact windows while the editor is open.

### Cutting card (signature)
The answer as one surface, readable from arm's length at the cutting table.
- **Surface:** Heather Wash, extra-large corners (28dp), 24dp padding, no shadow, nothing nested.
- **Content:** "Raios para o molde" (Title Large), then "Linhas de referência" (lines 1 and 3) and "Linhas de corte" (lines 2 and 4) in Title Medium, then "Diagrama das linhas" and the drawing on the card's own surface. Sections are 24dp apart, rows within a group 16dp. Each row is a line key (number in Label Large, a 40×16dp stroke sample of its line style, the label in Body Large, 8dp apart) over the value in Headline Small, 4dp below, such as "13,05 cm". Everything is On Heather Wash.
- **Arrangement:** the diagram follows the radii, and moves beside them when the card's content width holds two columns of `260dp × max(fontScale, 1)` plus a 24dp gap. TalkBack still reads all four radii before the diagram.
- **Once only:** each value appears on the card and nowhere else on the page until someone opens "Fórmulas". Each radius is one merged announcement, for example "Linha 1, linha contínua. Costura da cintura: 13,05 cm". The heading is a polite live region, so a successful calculation is announced once.
- **Reveal:** a successful Calcular hides the keyboard and scrolls the card into view; returning from the guide keeps the scroll position. Any input change removes the card.

### Drafting diagram (signature)
A square quarter-circle Canvas filling its column up to 400dp, in On Heather Wash with 2dp strokes and a 3dp centre dot; numbers in Label Large sit on Heather Wash discs. Arcs are numbered 1 to 4 and told apart by line style: solid for waist stitching, long dash (12/6) for waist cutting, dot (2/5) for the finished hem, dash-dot (12/5/2/5) for hem cutting. Spacing is schematic: the waist arcs are drawn wider than to scale so small allowances and lines 1 and 2 stay legible, and waist and hem numbers sit at opposite ends of their arcs. Coincidence notes and the "Esquema sem escala…" note (Body Medium, same ink) sit 8dp below. The card's radius rows are its only key. The figure is one TalkBack node whose text equivalent states the common centre, the order of the lines, each line's style and meaning, coincident lines, which way each allowance moves, and that it is not to scale. It deliberately omits the radius values, which are heard once, on the cutting card; the owner confirmed this departure from the earlier "lists all dimensions" contract.

### Results sequence
Below the cutting card, 16dp apart in the page column:
- **Action row:** one wrapping row, 8dp gaps: the tonal guide button, then "Editar medidas", which returns focus to the waist field. At large text the second button wraps below the first.
- **Measurement summary:** "Medidas utilizadas (cm)" in Title Medium, 4dp above one wrapping Body Medium line of "label: value" pairs joined by " · ". The unit is stated once, in the heading; a non-breaking space keeps each value with its label; TalkBack hears pauses instead of the separators.
- **Limits callout:** "Limites do cálculo" (Title Medium) and its Body Large text on Calico High, medium corners, 16dp padding, 8dp inside.
- **Disclosures:** "Como levar os raios ao papel" and "Fórmulas".

### Fabric folding guide (signature)
A full-screen player that replaces the calculator until it closes, one centred column at most 640dp wide on Raw Calico.
- **Top:** a 64dp row with a 48dp close icon button ("Fechar guia") beside the step title in Title Large.
- **Drawing:** the Rive animation in a Calico High frame with large corners (16dp) and 8dp padding, sized to the lesser of the column width less 32dp and 40% of the window height. With motion on, the whole frame is one pause button clipped to its shape, with Material ripple and visible focus: a tap, Enter or Space pauses on the current frame and continues from it, announced once as the step's drawing, "Reproduzindo" or "Pausado", with the action "Pausar animação" or "Continuar animação". A 40dp Calico Highest circle inset 8dp at the frame's bottom-end shows the action a tap performs with an onSurfaceVariant pause or play glyph; it is decorative for TalkBack. With system animations off, or while loading or on error, the frame is no button and shows no badge or ripple. The drawing stays pinned while the instructions scroll; in windows under 560dp tall, such as a landscape phone, it scrolls with them. The player loads the animation file once, so a step change only swaps the drawing. While anything loads, the frame shows empty at its final size, with no text, fade or synthetic delay, keeping the step's drawing description for TalkBack, and the drawing then appears in place. A cold load takes longer than 300ms on the reference emulator, so a load that lasts 400ms shows Material 3's expressive loading indicator centred in the frame, held for at least 500ms before the drawing appears; quicker loads never show it. Load failures show a plain sentence pointing to the written instructions.
- **Instructions:** 16dp padding and 16dp between paragraphs. When system animations are off, "Movimento desativado no sistema…" sits visibly first; with animations on it does not appear. Each step then reads in three layers. First, one imperative lead in Title Medium, the action to take now, readable under the pinned drawing on a compact phone at 100% text. Second, the brief context that changes that action: on marking, the radii in key order (4dp apart, read as one key), which lines are references and which way each allowance moves; on cutting, which arcs to cut and not to cut and that the opening, closure allowances and waistband need separate instructions before cutting garment fabric; coincident lines; the common origin. Third, a collapsed "Detalhes" disclosure set in Body Medium and Faded Walnut: edge and grain conventions, the illustrative-fabric and not-actual-size notes, practice on paper or scrap pending validation, the construction boundary and how playback works. The unfolding lead names both unfold actions in order. The body never repeats the step title, and the radius values appear once, on the marking step.
- **Step bar:** fixed at the bottom on Calico Mid with 16/8dp padding: "Anterior" (disabled on the first step), "Etapa n de 6" in Label Large and Faded Walnut, and a filled "Próximo" that becomes "Concluir" on the last step. Above 130% text the position moves above the buttons.
- **Closing:** the close button, "Concluir", system Back and predictive Back all return to the results, with focus back on the guide button. The current step survives rotation and recreation.

### The guide's drawing (signature)
- **Colour:** six transparent 448-unit artboards, so the Calico High frame is the page. The app binds four view-model colours from the current scheme: `fabric` is Heather Wash (`secondaryContainer`); `ink`, every line, edge, numeral and dot, is On Heather Wash (`onSecondaryContainer`), the cutting card's own pair; `fabricFold`, the layer being folded, is Lichen Wash (`tertiaryContainer`); `accent`, the trace that follows a cut while it plays, is Lichen Green (`tertiary`). Light, dark and dynamic schemes all resolve through these roles.
- **Line key:** the cutting card's: 1 solid, 2 dashed, 3 dotted, 4 dash-dot, numbered on fabric-coloured discs at the arc ends while marking and cutting. The unfolded circle drops the numbers; its waist cutout is too small for them. Fabric edges keep the written conventions: closed folds solid, selvedges dotted, raw ends dashed, all straight and unnumbered.
- **Proportion:** the four radii are set from the current calculation, normalised to the hem cutting radius, so the drawing keeps their proportions and is labelled not actual size. The fabric piece is illustrative and fixed in size. Zero allowances draw coincident lines.
- **Motion:** each step plays as soon as its drawing appears, until a tap pauses it, and never plays with system animations off, resting on its completed result instead. It moves once in one clear way over three to four seconds, with ease-in-out and decelerating curves and no bounce: the grain arrow grows from the centre, each fold mirrors its layer over the crease, the four arcs sweep in line order, the cuts trace line 2 then line 4 before the scraps lift away, and the circle opens one fold at a time. Each loop holds the completed pose, then plays in reverse.

## Do's and Don'ts

### Do:
- **Do** take every color from `MaterialTheme.colorScheme` roles so dynamic color, dark theme and the fallback palette all hold.
- **Do** group inputs on Calico Low with 16dp corners and 16dp padding, and put the four radii on the Heather Wash cutting card with 28dp corners.
- **Do** give each page one filled Mulberry Dye primary action and make the other actions text buttons; the guide entry is the one tonal button.
- **Do** set every radius in Headline Small with two pt-BR decimals and a non-breaking space before "cm", and show each value once.
- **Do** put formulas, drafting steps and guide details behind disclosures that start collapsed and announce their state.
- **Do** keep the guide's step bar fixed and fully visible on every step, at every text size.
- **Do** distinguish diagram and drawing lines by stroke pattern and number labels, and provide the text equivalent.
- **Do** keep the diagram on the cutting card's own surface, under "Diagrama das linhas", directly after or beside the radii.
- **Do** follow the card with the action row, the summary, the limits callout and the disclosures, in that order.
- **Do** keep touch targets at 48dp or more and check layouts at 200% text size.

### Don't:
- **Don't** add drop shadows, nested cards or decorative elevation.
- **Don't** add a home screen, bottom navigation bar, rail or drawer, or a second FAB. The top app bar holds a title and an action; it is not navigation.
- **Don't** repeat a radius value in the diagram or its text equivalent, and don't add a separate diagram key; the card's radius rows are the key.
- **Don't** wrap the diagram in a second container inside the card.
- **Don't** make one radius more prominent than the others.
- **Don't** signal reference versus cutting, or error versus valid, by color alone.
- **Don't** use the legacy Compose Material Icons library. Use Material Symbols XML vectors tinted by the theme.
- **Don't** animate a dimension while it is being read, or add choreography outside the guide drawing; interface motion stays Material's own.
