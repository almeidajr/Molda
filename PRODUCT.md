# Product

<!-- impeccable:product-schema 1 -->

## Platform

android

## Users

Two primary audiences, weighted equally:

- **Experienced drafters**: sewists, including working seamstresses, who already draft garments on paper. They want clear, checkable dimensions fast, often at the cutting table.
- **Beginners**: sewists learning the steps from flat fabric through folding, marking, cutting and unfolding.

Both draft for themselves or for people they sew for. Neither audience has been observed in a user session yet (see Evidence on Hand).

## Product Purpose

Molda is a pattern calculation assistant, not an automatic pattern generator. It turns body measurements and explicit drafting assumptions into dimensions a sewist can check and transfer to paper. It then teaches how to apply them to fabric. Success means a sewist drafts correctly from the results, tells stitching/reference lines apart from cutting lines, and has a real reason to come back. Numbers that match alone do not prove usefulness or fit.

## Positioning

Three things together, which a generic circle-skirt calculator does not combine:

1. **Explained, checkable math.** Every result shows its inputs, assumptions and formulas. Reference (stitching) dimensions are kept apart from cutting dimensions. Construction boundaries sit next to the results.
2. **Visual teaching built on the user's own numbers.** The Rive fabric-folding guide keeps the proportions of the current calculation's radii.
3. **Reusable measurement profiles.** Saved body measurements for each person someone sews for are stored only on the device.

It is a native, offline, private app written in Brazilian Portuguese.

## Operating Context

- Paper drafting and fabric cutting, often at a cutting table, with values transferred by hand.
- Fabric is folded in layers before marking and cutting. The guide assumes fabric large enough for the pattern and says so.
- Works offline, with no account. Profiles live only on the device, so clearing app data or replacing the device loses them. The UI must say this.

## Capabilities and Constraints

- Full-circle skirt (saia godê inteira) calculator in woven fabric with waistband and closure. Inputs: waist, explicit waist ease, finished skirt length, waist seam allowance, hem allowance. Outputs: four radii, shown to two decimals in cm. The math stays unrounded until display.
- Measurement profiles hold a name or nickname, waist, measurement date and optional notes. Profiles are optional. A profile fills only the waist field and is never changed silently by a calculation.
- Fabric-folding guide (Rive) opens from the results. It plays step by step with replay, previous and next controls. The drawing is proportional, labelled, and marked as not actual size.
- pt-BR only, centimetres only, decimal commas accepted. Ease and allowances need explicit values, which may be zero. There are no hidden assumptions.
- Android API 36+, Jetpack Compose and Material 3, and adaptive layouts for wide windows and foldables.
- Excluded for now: other circle fractions or garments, printable or 1:1 templates, fit diagnosis, panel layout, closure allowances, waistband drafting, yardage, accounts, sync, backup and calculation history. The roadmap lists follow-ups whose priority is undecided.
- Terminology: follow `GLOSSARY.md`. Fabric folding is not a pleat. Ease is not a seam allowance.

## Brand Commitments

- Name: **Molda**.
- Voice: a precise, calm mentor. It uses plain, exact sewing vocabulary, states limits honestly, never promises fit, and is warm but not chatty.
- Identity already set in `docs/ui-design-system.md`: a calm, warm sewing-studio character, Material 3 Expressive, dynamic color first, with a warm plum fallback palette.

## Evidence on Hand

- Product rationale: `docs/product-discovery.md`, `docs/ui-ux-design.md`, `docs/ui-design-system.md`, `docs/roadmap.md`, `docs/fabric-folding-spec.md`. Specs and validation reports live in GitHub Issues (`almeidajr/Molda`).
- Numerical reference case: W 80, E 2, L 60, S 1, H 2 gives 13.05 / 73.05 / 12.05 / 75.05 cm. This checks arithmetic only, not fit.
- One pt-BR sewist is available for feedback. No user interview, observed workflow, practical drafting session or fabric-folding trial (#45) has happened yet. Do not invent testimonials, usage data or claims of validated fit.

## Product Principles

1. **Checkable over magical.** Show the inputs, assumptions and formulas, and never hide a default.
2. **Reference vs. cutting is sacred.** Never let the two be confused, and never signal them with color alone.
3. **Serve both speed and learning.** Experienced drafters get results fast. Beginners get guided explanations they can open when they need them.
4. **Honest boundaries.** Say what Molda does not cover (waistband, closure, fit) next to the result.
5. **The user's data stays theirs.** Everything is offline and on the device. Profiles change only when the user explicitly updates them.

## Accessibility & Inclusion

TalkBack in a logical reading order. Keyboard and switch access with visible focus and no gesture-only actions. Touch targets of at least 48dp. Layouts stay usable at 200% text size. Calculation results and errors are announced. The diagram has a text equivalent and distinct line styles, not only color. Contrast is checked in light, dark and dynamic color schemes. Motion respects the system animation settings.
