# Practical fabric-folding trial

Use this repeatable protocol with a sewist before releasing the guide.
It is a plan, not evidence that a participant has completed the trial. Record
actual observations, screenshots, build identifiers and results in
[issue #45](https://github.com/almeidajr/Molda/issues/45), not in this document.
Automated UI checks and rendered geometry do not replace this physical trial.

## Preparation

- Recruit a sewist who can read the guide's Brazilian Portuguese instructions.
  Record their relevant experience without unnecessary personal information.
- Identify the installed build: integration commit SHA, APK SHA-256, version,
  device/Android version, language, font scale and date. Use an APK built from
  that commit; do not infer its identity from the app's version alone.
- Supply a ruler, compass or pinned string, pencil, scissors, and three paper
  squares (50, 70 and 45 cm sides). Scrap fabric is an alternative, but record
  material, measured size and any slipping or stretching. Label opposite sides
  as simulated selvages and the remaining sides as raw ends on paper.
- Use paper or scrap only. The guide does not teach the opening, closure
  allowances or waistband. Agree those separate instructions before cutting
  garment fabric. A completed ring is not a complete garment pattern or fit test.
- Keep the observer's expected outcomes below separate from the participant's
  instructions. Ask the participant to follow the app, not this answer sheet.

## Calculator cases

All inputs and radii are centimeters. These deliberately short practice pieces
are not wearable skirt recommendations. Enter each case in the working
calculator and open **Guia de dobra do tecido** beside the results. Use the
displayed dimensions for marking; never trace or measure the screen drawing.

| Case                | Waist | Ease | Finished length | Waist seam allowance | Hem allowance | Paper square |
|---------------------|-------|------|-----------------|----------------------|---------------|--------------|
| A: baseline         | 60    | 2    | 10              | 1                    | 2             | 50 × 50      |
| B: longer           | 60    | 2    | 20              | 1                    | 2             | 70 × 70      |
| C: coincident lines | 60    | 2    | 10              | 0                    | 0             | 45 × 45      |

Independent observer reference, rounded to two decimals:

| Case | Waist stitching radius | Waist cutting radius | Finished hem radius | Hem cutting radius | Unfolded outside / hole diameter |
|------|------------------------|----------------------|---------------------|--------------------|----------------------------------|
| A    | 9.87                   | 8.87                 | 19.87               | 21.87              | 43.74 / 17.74                    |
| B    | 9.87                   | 8.87                 | 29.87               | 31.87              | 63.74 / 17.74                    |
| C    | 9.87                   | 9.87                 | 19.87               | 19.87              | 39.74 / 19.74                    |

The waist stitching circumference is 62 cm in every case; subtracting a 1 cm
waist seam allowance moves the cutting line inward, while adding 2 cm hem
allowance moves the cutting line outward. Case B adds 10 cm only to the two
outer radii. In C, cutting/reference lines coincide without extra spacing.
Choose and record a physical measuring tolerance before starting (for example,
±0.3 cm for these paper diameters); separate measurement precision from any
misunderstanding of the folds or cutting lines.

## Observe the full sequence

For each case, record what the participant actually does, not just whether they
agree with an explanation. Let them use previous, next and pause at their own
pace. Record each request for help, observer intervention, mistaken action and
recovery, including the step and the wording or frame that caused difficulty.
Do not silently coach them into a successful result. Record one pass using
written instructions without playback as well as a pass using animation.

1. **Prepare:** participant identifies simulated selvages left/right and raw
   ends top/bottom, smooths the piece, and checks sufficient room for the arcs.
   Paper dimensions here were chosen for the trial; the app did not check them.
2. **First fold:** left selvage moves onto right selvage. Two layers remain;
   the left edge is closed and the right edges are aligned selvages.
3. **Second fold:** top raw end moves onto bottom raw end. Four layers remain;
   closed folds meet in the upper-left corner, with raw ends below and selvages
   right. Before marking, ask the participant to identify that origin unaided.
4. **Mark:** participant measures all four arcs from that same closed-fold
   corner, distinguishes cutting lines from stitching/finished-hem references,
   and explains why C has only two distinct arcs. Check marked distances with
   the ruler before cutting; record any discrepancies before correcting them.
5. **Cut:** keep layers aligned; cut the waist cutting arc, then the hem cutting
   arc through all four layers. Retain the quarter-annulus, removing the corner
   and outer scraps. Do not cut the straight folded edges or add a closure slit.
   Stop before an unintended cut; record both the attempted action and help.
6. **Unfold:** reverse the second fold upward into a right semicircle, then
   reverse the first fold leftward. Expect one complete annulus with one central
   hole, intact circumference and no straight slit. Measure outside and hole
   diameters across two perpendicular axes against the reference table. Record
   asymmetry, jagged cuts, layer movement and any difference from the animation.
7. **Explain:** without leading yes/no questions, ask what the drawing's scale
   means, whether the app checked their actual fabric, and what is still needed
   before garment cutting. Expect proportional screen geometry, no physical
   1:1 template or fabric-fit check, and separate opening, closure-allowance and
   waistband instructions. Record their words and any missing understanding.
8. **Compare:** moving A to B must keep waist dimensions and allowances while
   increasing the outer radii and changing the displayed proportions. In C,
   verify coincident lines and the retained annulus physically. Ensure the
   guide uses each current calculation rather than the preceding case.

## Corrections and release decision

Stop if instructions cause confusion about the origin, layers, cutting lines,
safe material or construction boundary. Record the problem and exact assistance;
do not count the assisted result as an unassisted success. Correct confirmed
guide defects, rerun affected UI/Rive checks, and repeat the physical sequence
on fresh material with the corrected build whenever it changes. Record both
builds, the correction and the actual retrial outcome. Preserve failed evidence.

The release gate remains outstanding if no participant is available, a case or
understanding check was not performed, a material defect remains unresolved, or
a required retrial has not occurred. Report partial evidence as partial. A pass
requires recorded physical outcomes and understanding checks for the cases,
review of any assistance/deviations, and successful retrials of corrections.
Never infer a pass from an automated check or a prepared checklist.

Use this structure for the issue report:

- **Status:** not run / partial / failed / passed; outstanding release gate.
- **Build and setting:** commit, APK hash, device, Android, language, font scale,
  date, material, participant experience and agreed tolerance.
- **Per case:** inputs, observed folds/origin, marks and cuts, measured diameters,
  written-only/playback use, assistance, deviations and participant explanations.
- **Evidence:** consented photos/notes showing folds, marked quarter, cuts and
  unfolded result, linked from the issue without unnecessary personal details.
- **Corrections:** defect, changed build, relevant automated checks and physical
  retrial observations; list remaining limitations explicitly.
- **References:** [sewing sources and scope](fabric-folding-references.md),
  [animation behavior](../animations/README.md), and the tested parent/tickets.
- **Decision:** who reviewed the observations, whether the gate passed and what
  exact human action remains if it did not.
