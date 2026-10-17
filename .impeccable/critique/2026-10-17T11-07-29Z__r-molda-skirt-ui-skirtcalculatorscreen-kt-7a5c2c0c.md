---
target: skirt calculator screen
total_score: 31
max_score: 40
na_heuristics:
p0_count: 0
p1_count: 0
target_identity: "file:C:\\Users\\jralm\\AndroidStudioProjects\\Molda\\app\\src\\main\\java\\com\\almeidajr\\molda\\skirt\\ui\\SkirtCalculatorScreen.kt"
target_fingerprint: "sha256:e4448b5b5fafc1f99dfe08b2da36a6f59355766e9cb3939da65a3c0b0ea1277f"
target_path: "C:\\Users\\jralm\\AndroidStudioProjects\\Molda\\app\\src\\main\\java\\com\\almeidajr\\molda\\skirt\\ui\\SkirtCalculatorScreen.kt"
timestamp: 2026-10-17T11-07-29Z
slug: r-molda-skirt-ui-skirtcalculatorscreen-kt-7a5c2c0c
closed: true
---
Method: design review only (detector not applicable, native platform). Evidence: Molda_API_36 phone emulator, light/dark, dynamic color, reference case W80 E2 L60 S1 H2, after PR #58.

| # | Heuristic | Score | Key Issue |
|---|---|---|---|
| 1 | Visibility of System Status | 4 | Calculate hides the keyboard, reveals the whole card, announces once |
| 2 | Match System / Real World | 3 | Exact sewing vocabulary; diagram still reads 2→1→3→4 from centre |
| 3 | User Control and Freedom | 3 | Back/Concluir/Editar medidas solid |
| 4 | Consistency and Standards | 3 | Card labels use onSurfaceVariant on secondaryContainer (role mismatch) |
| 5 | Error Prevention | 3 | Unchanged, solid |
| 6 | Recognition Rather Than Recall | 3 | Shared number + line-style key; diagram sits far from the card |
| 7 | Flexibility and Efficiency | 3 | No scroll hunt for results; summary still five lines |
| 8 | Aesthetic and Minimalist Design | 3 | Much calmer; diagram floats untitled after summary and limits |
| 9 | Error Recovery | 3 | Unchanged, solid |
| 10 | Help and Documentation | 3 | Tutorial first-class; still frame white in dark |
| **Total** | | **31/40** | **Good** |

Specificity: now product-specific — the cutting card's numbered line-style key is something only Molda would ship, and it survives dynamic color. Remaining sameness is in the area below the card.

P2 Diagram is orphaned from the card it keys to: order is card → tutorial → Editar → summary → limits → diagram → disclosures; the diagram has no heading and sits two screens below the numbers it explains. Fix: diagram directly after the card (or under a "Desenho" heading), then summary and limits. /impeccable layout
P2 Card label colour role mismatch: labels use onSurfaceVariant on secondaryContainer; in dark they drop to grey-on-slate. Fix: use onSecondaryContainer for labels (emphasis via type, not off-role colour) and verify contrast in light/dark/dynamic. /impeccable polish
P2 Diagram key wraps unevenly (1 alone; 2 and 3 share a row; 4 alone) and its order differs from the card's groups. Fix: a fixed two-column key grouped like the card (reference 1,3 | cutting 2,4). /impeccable layout

Personas
Experienced drafter: numbers readable at a glance; the diagram is a long scroll away.
Beginner: tutorial is obvious; key-to-drawing mapping is weaker because the drawing is far from the card.
Sam: one announcement per radius; check dark-mode label contrast.

Minor
- "Editar medidas" floats alone between the tonal tutorial button and the summary.
- Diagram leaves a large empty quadrant on phones; the inner arcs are small.
- Tutorial still frame stays white in dark mode (needs .riv change).
- "Medidas utilizadas (cm)" is five lines; could be one compact line.

Questions
- What if the diagram sat inside the card's reading flow, so a number and its line are on screen together?
- Should Editar medidas sit beside the tutorial button as one action row?
