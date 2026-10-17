---
target: skirt calculator screen
total_score: 27
max_score: 40
na_heuristics:
p0_count: 0
p1_count: 3
target_identity: "file:C:\\Users\\jralm\\AndroidStudioProjects\\Molda\\app\\src\\main\\java\\com\\almeidajr\\molda\\skirt\\ui\\SkirtCalculatorScreen.kt"
target_fingerprint: "sha256:f1ca80558c90fba52888123c723dea8cbbab6c84b805ced67e68e6d5d390c072"
target_path: "C:\\Users\\jralm\\AndroidStudioProjects\\Molda\\app\\src\\main\\java\\com\\almeidajr\\molda\\skirt\\ui\\SkirtCalculatorScreen.kt"
timestamp: 2026-10-16T14-31-10Z
slug: r-molda-skirt-ui-skirtcalculatorscreen-kt-7a5c2c0c
---
Method: design review only (detector not applicable, native platform). Evidence: Molda_API_36 emulator (phone, 1080x2424, dynamic color, light), reference case W80 E2 L60 S1 H2.

| # | Heuristic | Score | Key Issue |
|---|---|---|---|
| 1 | Visibility of System Status | 3 | Results announced via live region; on compact only the results heading is scrolled into view |
| 2 | Match System / Real World | 3 | Exact pt-BR sewing vocabulary; diagram numbering reads 2→1→3→4 from the centre |
| 3 | User Control and Freedom | 3 | Editar medidas, discard dialog, closable tutorial |
| 4 | Consistency and Standards | 3 | Coherent Material; no top app bar; every secondary action is the same faint text button |
| 5 | Error Prevention | 3 | Decimal keyboard, explicit zero, submit-first validation |
| 6 | Recognition Rather Than Recall | 2 | Diagram numbers must be decoded through a legend below; tutorial navigation is below the fold |
| 7 | Flexibility and Efficiency | 2 | IME Next chain and profile reuse; results need a long scroll on every recalculation |
| 8 | Aesthetic and Minimalist Design | 2 | Radii repeated three times; results area is a long wall of same-weight text |
| 9 | Error Recovery | 3 | First invalid field focused, specific messages |
| 10 | Help and Documentation | 3 | Rich inline guidance and formulas; the tutorial is buried |
| **Total** | | **27/40** | **Acceptable (top of band)** |

Design specificity: honest and correct, but category-interchangeable. Under dynamic color it is stock Material blue; the Tailor's Notebook only shows up in the diagram. Shape and hierarchy don't carry the identity.

P1 Radius values are not read as numbers: each radius is one Headline Small string ("Raio da linha de costura da cintura: 13,05 cm"), so labels shout as loud as values and "cm" wraps onto its own line. Fix: split each into a label (bodyMedium/titleMedium) and a value (headline) row, with the value and unit kept together, tabular figures, and an arc-pattern swatch matching the diagram. /impeccable typeset + /impeccable layout
P1 Fabric-folding tutorial is buried and its player is cramped: a faint text button between the boundary note and the explanation; a narrow default Dialog; Anterior/Próximo/Fechar stacked vertically below long text. Fix: promote it to a tonal button beside the results; use a full-width sheet or full-screen dialog; fixed bottom bar with Anterior · Etapa n de 6 · Próximo; keep the animation pinned while text scrolls. /impeccable layout + /impeccable adapt
P1 Results area is a text wall with triple repetition: radii in result groups, again in the diagram legend, again in the formulas; boundary text, the inputs summary and the explanation follow at the same weight. Fix: one source of numbers (result groups carry the line-style key and diagram numbers); legend becomes a compact key; collapse "Medidas utilizadas" to one line; keep boundaries as a single callout. /impeccable distill
P2 Compact calculate lands results half-hidden: bringIntoView targets the heading only, so the radii stayed below the open keyboard after IME Done in the emulator run. Fix: bring the first result group into view and hide the IME on calculate. /impeccable harden
P2 Identity disappears under dynamic color: no top app bar, a floating "Perfis de medidas" link, a small left-aligned Calcular, and input/result surfaces one tone apart. Fix: top app bar with a profiles action; full-width or prominent Calcular; results on a distinct container (secondary/tertiaryContainer) so the answer reads as its own page. /impeccable bolder

Personas
Experienced drafter (project): wants four numbers fast; gets them as sentences, scrolls past the inputs summary and boundaries to reach the diagram, and reads each value twice.
Beginner (project): the tutorial — the thing that teaches them — is a faint link; inside it they scroll past paragraphs to find Próximo for every one of six steps.
Sam (accessibility): live regions, headings and diagram text are strong; the duplicated radii mean TalkBack users hear every value three times.
Casey (one-handed at the cutting table): Calcular sits at the bottom of a long form (good), but tutorial controls drift to the bottom of a scrolling dialog and change position each step.

Minor
- "Circunferência da cintura (cm): 80,00 cm" repeats the unit.
- "Usar perfil" sits under the guidance text, detached from the field; the "Medida manual" status floats below it.
- Diagram uses ~60% of width; labels 1 and 2 collide near the centre at small waist radii.
- The "Tutorial de dobra" and "Mostrar fórmulas" buttons have default height; other tutorial buttons enforce 48dp explicitly, so the treatment is inconsistent.
- The tutorial still image sits on a white panel, not the theme surface.

Questions
- What if the result were a cutting card you could read from arm's length at the table: four big numbers, each with its line style?
- Does the results pane need the full explanation, or should it live behind "Como levar ao papel"?
- What would the tutorial look like as a full-screen player instead of a dialog?
