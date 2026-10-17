---
target: skirt calculator screen
total_score: 34
max_score: 40
na_heuristics:
p0_count: 0
p1_count: 0
target_identity: "file:C:\\Users\\jralm\\AndroidStudioProjects\\Molda\\app\\src\\main\\java\\com\\almeidajr\\molda\\skirt\\ui\\SkirtCalculatorScreen.kt"
target_fingerprint: "sha256:b578380ec262838e8aff3968cec6cd4c6b4a61174e22760363a37fbd6d47f4ac"
target_path: "C:\\Users\\jralm\\AndroidStudioProjects\\Molda\\app\\src\\main\\java\\com\\almeidajr\\molda\\skirt\\ui\\SkirtCalculatorScreen.kt"
timestamp: 2026-10-17T14-38-47Z
slug: r-molda-skirt-ui-skirtcalculatorscreen-kt-7a5c2c0c
---
Method: design review only (detector not applicable, native platform). Evidence: Molda_API_36 phone, dynamic colour, reference case; tutorial captures light/dark/reduced motion from the Rive redesign pass.

| # | Heuristic | Score | Key Issue |
|---|---|---|---|
| 1 | Visibility of System Status | 4 | Keyboard hides, card revealed, announced once |
| 2 | Match System / Real World | 3 | Diagram still reads 2→1→3→4 from the centre |
| 3 | User Control and Freedom | 3 | Back/Concluir/Editar medidas solid |
| 4 | Consistency and Standards | 4 | Colour roles now pair with their surfaces; one key across card, diagram and tutorial |
| 5 | Error Prevention | 3 | Unchanged |
| 6 | Recognition Rather Than Recall | 4 | Number, line and value visible together inside the card |
| 7 | Flexibility and Efficiency | 3 | Card taller than a phone screen; diagram below the fold after Calculate |
| 8 | Aesthetic and Minimalist Design | 3 | Calm results; tutorial steps carry long instruction text |
| 9 | Error Recovery | 3 | Unchanged |
| 10 | Help and Documentation | 4 | Theme-correct tutorial on the user's own numbers; contextual disclosures |
| **Total** | | **34/40** | **Good** |

Specificity: strongly product-specific. The numbered line-style key now runs from card to diagram to tutorial, and the tutorial draws in the app's theme in light and dark.

P2 Tutorial steps are text-heavy under a pinned drawing: Marking and Cutting carry 5–7 paragraphs in the ~40% scroll region, so instructions need several scrolls per step. Fix: lead each step with one imperative sentence, move caveats behind a short "Detalhes" disclosure. /impeccable clarify
P3 Cutting card is taller than a phone screen: after Calculate the radii show, the diagram starts below the fold, and the quarter-circle leaves an empty lower-right area. Fix: cap the diagram height on compact phones, or tuck the "sem escala" note into the empty quadrant. /impeccable layout

Minor
- Hem lines 3 and 4 hug the fabric edge in the Marking step; little breathing room.
- Rive line numbers don't scale with font size (text carries all values).
- .impeccable/design.json not yet synced with the tutorial section of DESIGN.md.
