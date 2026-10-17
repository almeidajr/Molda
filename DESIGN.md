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
  dark-raw-calico: "#17130B"
  dark-calico-low: "#201B13"
  dark-calico-mid: "#241F17"
  dark-walnut-ink: "#ECE1D4"
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
  extra-large: "32dp"
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
  top-app-bar:
    backgroundColor: "{colors.raw-calico}"
    textColor: "{colors.walnut-ink}"
    typography: "{typography.title-large}"
    height: "64dp"
  fab-create-profile:
    backgroundColor: "{colors.mulberry-wash}"
    textColor: "{colors.on-mulberry-wash}"
    typography: "{typography.label-large}"
    rounded: "{rounded.medium}"
    height: "56dp"
  measurement-field:
    textColor: "{colors.walnut-ink}"
    typography: "{typography.body-large}"
    rounded: "{rounded.small}"
  section-group:
    backgroundColor: "{colors.calico-low}"
    textColor: "{colors.walnut-ink}"
    rounded: "{rounded.medium}"
    padding: "16dp"
  cutting-card:
    backgroundColor: "{colors.heather-wash}"
    textColor: "{colors.on-heather-wash}"
    typography: "{typography.headline-small}"
    rounded: "{rounded.extra-large}"
    padding: "24dp"
  boundary-callout:
    backgroundColor: "{colors.calico-high}"
    textColor: "{colors.walnut-ink}"
    typography: "{typography.body-large}"
    rounded: "{rounded.medium}"
    padding: "16dp"
  tutorial-step-bar:
    backgroundColor: "{colors.calico-mid}"
    textColor: "{colors.faded-walnut}"
    typography: "{typography.label-large}"
    padding: "8dp 16dp"
  modal-surface:
    backgroundColor: "{colors.calico-high}"
    textColor: "{colors.walnut-ink}"
    rounded: "{rounded.extra-large}"
---

# Design System: Molda

## Overview

**Creative North Star: "The Tailor's Notebook"**

Molda should feel like the drafting notebook a skilled tailor keeps next to the cutting table. The pages are warm, the figures are exact, and a worked explanation always sits in the margin. Every screen is a page you can check: the inputs at the top, the assumptions stated, the results set apart on a cutting card, and the formulas available on request. The character is tactile and confident. Primary actions are firm pill shapes you can rely on, and grouped content reads like swatches laid on raw calico, stacked by tone rather than lifted with shadows.

The system is native Material 3, themed rather than reinvented. Dynamic color is the runtime default, so the dyed-fabric palette is the fallback identity. The notebook character must also survive in shape, spacing and hierarchy when the wallpaper replaces the plum. Density is calm: one readable column on compact windows, generous 16dp rhythm, and two panes only when the content truly fits. Motion is Material's own and never moves a dimension while someone is reading it.

**Key Characteristics:**
- Dynamic color first; the warm Mulberry/Lichen/Calico palette is the fallback.
- Flat tonal layering; no decorative shadows, no nested cards.
- All four calculated radii have equal typographic weight; their labels carry the meaning.
- Reference and cutting lines are told apart by line style and label, never by color alone.
- System sans-serif on the full Material type scale, in sp, so it scales to 200%.

## Colors

A muted, warm palette taken from hand-dyed cloth: mulberry thread on unbleached calico, with a lichen-green accent.

### Primary
- **Mulberry Dye** (`mulberry-dye`): the filled primary actions, "Calcular" and "Salvar", plus the text-button labels. In dark mode it becomes the paler **Dark Mulberry Dye** (`dark-mulberry-dye`).
- **Mulberry Wash** (`mulberry-wash`): the primary container. It is used for the profile-creation FAB and nothing else.

### Secondary
- **Heather Plum** (`heather-plum`) / **Heather Wash** (`heather-wash`): the secondary role. Heather Wash, with **On Heather Wash** (`on-heather-wash`) text, is the cutting card and the tonal "Tutorial de dobra do tecido" button. It is a different role from the calico input groups, so the results read as their own page even when dynamic color replaces the palette.

### Tertiary
- **Lichen Green** (`lichen-green`) / **Lichen Wash** (`lichen-wash`): set aside for diagram accents. The cutting card's diagram draws in its own ink; the tutorial uses Lichen Wash for the layer being folded and Lichen Green for the cut trace while it plays. Any lichen accent sits alongside labels and line styles, never replacing them.

### Neutral
- **Raw Calico** (`raw-calico`): the screen background and base surface. **Dark Raw Calico** (`dark-raw-calico`) in dark mode.
- **Calico Low** (`calico-low`): grouped input surfaces and profile lists.
- **Calico Mid** (`calico-mid`): the tutorial player's fixed step bar.
- **Calico High** (`calico-high`): the "Limites do cálculo" callout, the frame around the tutorial drawing, and overlays, sheets and dialogs wherever Material defaults choose it.
- **Walnut Ink** (`walnut-ink`): essential text and diagram strokes. **Dark Walnut Ink** (`dark-walnut-ink`) in dark mode.
- **Faded Walnut** (`faded-walnut`): supporting text, guidance and secondary labels on calico surfaces only. It is `onSurfaceVariant`, so it never sits on the Heather Wash cutting card.
- **Basting Outline** (`basting-outline`): essential field boundaries. **Basting Faint** (`basting-faint`): decorative separators only.
- **Snip Red** (`snip-red`) / **Snip Red Wash** (`snip-red-wash`): validation and failure feedback.

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
- **Headline Medium** (400, 28/36sp): profile-screen headings. The calculator's title lives in its top app bar.
- **Headline Small** (500, 24/32sp): each calculated radius value, with tabular figures where the system font has them. All four are the same size and weight.
- **Title Large** (400, 22/28sp): the app bar title, section headings such as "Medida do corpo", "Escolhas da peça" and "Margens de corte", the cutting card's "Raios para o molde" and each tutorial step title. They are marked as headings in semantics.
- **Title Medium** (500, 16/24sp): profile names and component titles, including the cutting card's "Linhas de referência", "Linhas de corte" and "Diagrama das linhas" headings.
- **Body Large** (400, 16/24sp): field text and explanations.
- **Body Medium** (400, 14/20sp): supporting text and field errors.
- **Label Large** (500, 14/20sp): action labels, line numbers on the card and diagram, and the tutorial's "Etapa n de 6".

### Named Rules
**The Equal Radii Rule.** All four radii share Headline Small. No result is bolder than another; the labels say which is a reference line and which is a cutting line.

**The Never-Truncate Rule.** Never truncate drafting values, errors or essential action labels. Keep the value and its "cm" together with a non-breaking space, format with pt-BR decimal commas, and avoid fixed-height text containers.

## Layout

Spacing uses a five-step scale: micro (4dp) for closely associated details, small (8dp) for label/value and icon/text pairs, medium (16dp) for field gaps, group padding and compact margins, large (24dp) between sections and panes, and extra-large (32dp) for major transitions. Material components keep their own internal padding.

Compact windows show one readable column. Two panes appear only when the available width is at least `360dp × max(fontScale, 1) × 2 + 24dp`, with a 24dp gap and a maximum readable pane width of 640dp. This is a content-fit rule, not a device test. Inputs sit beside the cutting card, and the profile list sits beside its editor. The layout avoids hinges, so large text can fall back to one pane. One column spans a flat fold and keeps to one side of a half-open or separating one; two panes split at the fold, or share one side when they cannot fit beside it. The full-screen tutorial player is one column of at most 640dp, and it keeps to one side of a half-open or separating vertical fold but spans a flat one. Primary actions stay in the form flow, never floating over fields or results. The app runs edge to edge and handles system-bar and IME insets.

**The Fits-Or-Stacks Rule.** If two panes would squeeze either one below 360dp at the current font scale, stack them.

## Elevation & Depth

The system is flat and depth comes from tone. Surfaces separate by stepping through the calico container ramp, from Raw Calico to Calico Low (inputs) to Calico High (callouts and overlays). The cutting card steps out of the ramp onto Heather Wash, the secondary container, so the results never depend on one calico tone. There are no drop shadows apart from what Material components such as the FAB and modal sheet bring by default.

**The Swatch Stack Rule.** Group content with a tonal surface, never a shadow, and never nest one grouped surface inside another.

## Shapes

Corners are softly rounded and generous. Grouped surfaces, the boundary callout and the FAB use the medium shape (16dp). The cutting card and modal sheets use extra-large (28dp). Primary and text buttons have fully rounded pill ends. Outlined fields keep Material's standard small corner. The tutorial player frame uses large (16dp). The one recurring motif is the quarter arc from the drafting diagram, which mirrors the folded fabric.

## Components

### Buttons
Firm, pill-shaped and easy to trust under the thumb.
- **Shape:** fully rounded ends (`rounded.full`), at least 48dp tall, and the button grows with its text.
- **Primary:** a filled Mulberry Dye button with an On-Mulberry label, used for the single main action on a page ("Calcular", "Salvar"). "Calcular" stays enabled for incomplete input so it can explain what needs fixing.
- **Primary placement:** "Calcular" spans the full width at the end of the form.
- **Tonal:** a Heather Wash button for the one prominent secondary path, "Tutorial de dobra do tecido". It leads the action row under the cutting card, with "Editar medidas" beside it.
- **Text:** the dominant secondary action ("Perfis de medidas", "Usar perfil", "Editar medidas", "Anterior", "Cancelar", disclosures, dialog actions), with a Mulberry Dye label and Material ripple.
- **Destructive:** named, explicit wording in a confirmation dialog, never an unlabelled icon.

### Floating action button
- **Profile creation:** an extended FAB in Mulberry Wash with medium corners. It is the screen's only FAB and appears only on profile management.

### Section groups
- **Corner Style:** medium (16dp).
- **Background:** Calico Low for input groups and profile rows.
- **Shadow Strategy:** none; see Elevation & Depth.
- **Internal Padding:** 16dp, with 16dp gaps between fields.
- **Content:** a Title Large heading, then fields. The group itself is not interactive.

### Cutting card (signature)
The answer as one surface, readable from arm's length at the cutting table.
- **Surface:** Heather Wash with extra-large corners (28dp), 24dp padding and no shadow. Nothing inside it is a nested card.
- **Content:** the heading "Raios para o molde", then "Linhas de referência" (lines 1 and 3) and "Linhas de corte" (lines 2 and 4). Each radius shows its diagram number, a stroke sample of its line style and its label in Body Large, above the value in Headline Small, all in On Heather Wash, such as "13,05 cm". The drafting diagram follows under "Diagrama das linhas", drawn on the card's own surface, so each number and its line are read together.
- **Arrangement:** on a compact window the diagram sits directly after the radii. When the card's content width holds two columns of 260dp × max(fontScale, 1) plus a 24dp gap, the diagram moves beside the radii. This is a content-fit rule like the panes.
- **Key:** the radius rows are the diagram's only key. The drawing carries numbers only, with no second key beneath it.
- **Once only:** each value appears on the card and nowhere else on the page until someone opens "Fórmulas", whose lines end in their results. TalkBack hears one merged announcement per radius, for example "Linha 1, linha contínua. Costura da cintura: 13,05 cm". The heading is a polite live region, so a successful calculation is announced once.
- **Reveal:** a successful Calcular hides the keyboard and scrolls the card into view; when the card is taller than the window, its top and all four radii come first. Any input change removes it.
- **After the card:** one action row with the tonal tutorial button and the "Editar medidas" text button, which wraps to two lines at large text; the "Medidas utilizadas (cm)" summary as one wrapping line in Body Medium (the unit is stated once, in its heading, and a non-breaking space keeps each value with its label); the "Limites do cálculo" callout on Calico High; then the two disclosures, 8dp apart as one pair. The page column keeps 16dp between these blocks.

### Disclosures
- **Style:** a text button with the label and a Material Symbols chevron that flips when open. The label wraps before the chevron shrinks.
- **Behaviour:** "Como levar os raios ao papel", "Fórmulas" and each tutorial step's "Detalhes" start collapsed, keep their state through recreation, and expose "Recolhido" or "Expandido" to TalkBack. The formulas keep the note that r is never rounded mid-calculation.

### Inputs / Fields
- **Style:** Material outlined text field with a persistent label, a "cm" suffix, supporting guidance and a decimal keyboard.
- **Focus:** Material's primary outline and label shift.
- **Error:** Snip Red outline with a specific message underneath. Errors show first on submit, the first invalid field takes focus, and corrected fields revalidate on blur.

### Top app bar
- **Style:** a Material top app bar on the calculator, titled "Saia godê inteira" (a heading), with the labelled "Perfis de medidas" text action. The title wraps at large text sizes and never runs under the action.

### Navigation
- **Style:** hierarchical. The app opens on the calculator, profile management is the app bar's labelled action, and "Usar perfil" sits with the waist's source ("Medida manual" or the profile name) just above the waist field. The app bar is not a navigation destination. There is no home screen, bottom bar, rail or drawer. System Back and predictive Back are always respected.
- **Picker:** a Material modal bottom sheet (extra-large corners, Calico High).

### Drafting Diagram (signature)
A square quarter-circle drawing on a Canvas that fills its column of the cutting card up to 400dp, in On Heather Wash (`onSecondaryContainer`) with 2dp strokes; number labels sit on Heather Wash discs. The cutting card's radius rows are its key. Coincidence notes and the "not to scale" line (Body Medium, same ink) sit 8dp under the drawing. The four arcs are numbered 1 to 4 and distinguished by line style: solid for the waist stitching line, a long dash (12/6) for waist cutting, a dot (2/5) for the finished hem, and dash-dot (12/5/2/5) for hem cutting. Arc spacing is schematic: the waist arcs are drawn wider than to scale so small allowances and lines 1 and 2 stay legible. Waist and hem numbers sit at opposite ends of their arcs so 1 and 2 never collide. The figure is one TalkBack node whose text equivalent states the common centre, the order of the lines, each line's style and meaning, coincident lines, which way each allowance moves, and that it is not to scale. It deliberately omits the radius values, which are heard once, on the cutting card. The owner confirmed this departure from the earlier "lists all dimensions" contract.

### Fabric Folding Tutorial (signature)
A full-screen player that replaces the calculator until it closes, at most 640dp wide.
- **Top:** a 48dp close icon button ("Fechar tutorial") beside the step title.
- **Drawing:** the Rive animation in a Calico High frame with large (16dp) corners, with play, replay and pause text buttons. It stays pinned while the instructions scroll; in windows under 560dp tall, such as a landscape phone, it scrolls with them so the text keeps room.
- **Instructions:** each step reads in three layers under the drawing. First comes one imperative lead sentence in Title Medium, the action to take now, readable without scrolling under the pinned drawing on a compact phone at 100% text. Then comes the brief context that changes that action: the radii in key order with which lines are references and which way each allowance moves (marking), which arcs to cut and not to cut (cutting), coincident lines, and the common origin. Last comes a collapsed "Detalhes" disclosure whose supporting detail is set quieter, in Body Medium and Faded Walnut: edge and grain conventions, the illustrative-fabric and not-actual-size notes, practice on paper or scrap pending validation, the separate opening, closure-allowance and waistband instructions, and playback. The cutting step's context also says, in one short sentence, that the opening, closure allowances and waistband need separate instructions before cutting garment fabric. The unfolding lead names both unfold actions in order: the second fold first, then the first. When system animations are off, the "Movimento desativado…" note sits visibly above the lead; with animations on it does not appear. The body never repeats the step title, and each radius value appears once, on the marking step.
- **Step bar:** fixed at the bottom on Calico Mid: "Anterior" (disabled on the first step), "Etapa n de 6", and a filled "Próximo" that becomes "Concluir" on the last step. Above 130% text the position moves above the buttons.
- **Closing:** the close button, "Concluir", system Back and predictive Back all return to the results, with focus back on the tutorial button. The current step survives rotation and recreation.
- **Drawing colour:** the artboards are transparent, so the Calico High frame is the page. The app binds four view-model colours from the theme: fabric is Heather Wash (`secondaryContainer`) and every line, edge, numeral and dot is On Heather Wash (`onSecondaryContainer`), the cutting card's own pair; the layer being folded in a step is Lichen Wash (`tertiaryContainer`); the trace that follows a cut while it plays is Lichen Green (`tertiary`). Light, dark and dynamic schemes all resolve through these roles. Colour never carries meaning alone: shape, line pattern, numbers, motion and the written text say the same.
- **Line key:** the cutting card's: 1 solid (waist stitching), 2 dashed (waist cutting), 3 dotted (finished hem), 4 dash-dot (hem cutting), numbered 1 to 4 on fabric-coloured discs at the arc ends while marking and cutting. The unfolded circle drops the numbers; its waist cutout is too small for them at that scale. Fabric edges keep the written conventions: closed folds solid, selvedges dotted, raw ends dashed, all straight and unnumbered.
- **Motion:** each step moves once, in one clear way, with a gentle ease-in-out and no bounce: the grain arrow grows from the centre, each fold mirrors its layer over the crease, the four arcs sweep from the left fold in line order, cuts trace line 2 then line 4 before the two scraps lift away, and the circle opens one fold at a time. Each loop holds the completed pose, then reverses.
- **Honesty:** the drawing keeps the proportions of the current radii and is labelled as not actual size. The fabric piece is illustrative, a fixed size distinct from the calculated arcs. Zero allowances draw coincident lines. With system animations off, each step's resting frame is its completed result.

## Do's and Don'ts

### Do:
- **Do** take every color from `MaterialTheme.colorScheme` roles so dynamic color, dark theme and the fallback palette all hold.
- **Do** group inputs on Calico Low with 16dp corners and 16dp padding, and put the four radii on the Heather Wash cutting card with 28dp corners.
- **Do** give each page one filled Mulberry Dye primary action and make the other actions text buttons.
- **Do** set every radius in Headline Small with two pt-BR decimals and a non-breaking space before "cm", and show each value once.
- **Do** put formulas and drafting steps behind disclosures that start collapsed and announce their state.
- **Do** keep the tutorial's step bar fixed and fully visible on every step, at every text size.
- **Do** distinguish diagram lines by stroke pattern and number labels, and provide the text equivalent.
- **Do** keep the diagram on the cutting card's own surface, under "Diagrama das linhas", directly after or beside the radii.
- **Do** put the tutorial and "Editar medidas" in one wrapping action row under the card, then the summary, the limits and the disclosures.
- **Do** keep touch targets at 48dp or more and check layouts at 200% text size.

### Don't:
- **Don't** add drop shadows, nested cards or decorative elevation.
- **Don't** add a home screen, bottom navigation bar, rail or drawer, or a second FAB. The top app bar holds a title and an action; it is not navigation.
- **Don't** repeat a radius value in the diagram or its text equivalent, and don't add a separate diagram key; the card's radius rows are the key.
- **Don't** wrap the diagram in a second container inside the card.
- **Don't** make one radius more prominent than the others.
- **Don't** signal reference versus cutting, or error versus valid, by color alone.
- **Don't** use the legacy Compose Material Icons library. Use Material Symbols XML vectors tinted by the theme.
- **Don't** animate a dimension while it is being read, or add bespoke choreography beyond Material motion.
