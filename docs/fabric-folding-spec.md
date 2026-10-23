## Problem Statement

A beginner can obtain full-circle skirt radii in Molda but cannot see how fabric folds turn circular marking and cutting into a full-circle skirt shape. The existing paper-drafting explanation does not teach this physical sequence.

## Solution

Offer a beginner tutorial from calculator results, beside the existing drafting explanation. Use Rive animations and written Brazilian Portuguese instructions to demonstrate flat fabric, folding, marking, cutting, and unfolding. Let users advance at their own pace and show their calculated radii in accurate proportions during marking and cutting.

## User Stories

1. As a beginner sewist, I want to open the tutorial beside my results, so that I can connect the lesson to my calculation.
2. As a beginner sewist, I want to see the starting fabric arrangement, so that I understand what to prepare.
3. As a beginner sewist, I want to see each fold in sequence, so that I can reproduce the arrangement.
4. As a beginner sewist, I want folded edges and the marking origin identified, so that I measure from the correct place.
5. As a beginner sewist, I want to see where to mark the waist and hem, so that I can connect the radii to the fabric.
6. As a beginner sewist, I want cutting lines distinguished from stitching and finished-hem lines, so that I retain the intended allowances.
7. As a beginner sewist, I want to see the circular cuts and unfolding, so that I understand how the full-circle shape emerges.
8. As a sewist, I want the tutorial to use my current calculated radii, so that its labels match my results.
9. As a sewist, I want marking and cutting diagrams to preserve radius proportions, so that garment changes produce meaningful visual changes.
10. As a sewist, I want the drawing fitted to my screen and labelled as not actual size, so that I do not mistake it for a physical cutting template.
11. As a sewist, I want illustrative fabric dimensions distinguished from calculated pattern dimensions, so that I do not assume my fabric will fit.
12. As a beginner sewist, I want each step to play as soon as I reach it and to pause it with a tap, so that I see the motion without looking for a button and can study one frame.
13. As a beginner sewist, I want previous and next controls that play the step shown, so that I can review difficult steps.
14. As a sewist who does not rely on motion, I want written instructions for every step, so that I can understand the full sequence without watching animation.
15. As a Brazilian sewist, I want instructions in pt-BR and dimensions in centimetres, so that the tutorial matches the calculator.
16. As a sewist, I want an explicit sufficient-fabric assumption, so that I know actual fabric fit has not been checked.
17. As a beginner sewist, I want to know that the unfolded circle is not a complete garment pattern, so that I do not treat the demonstration as complete construction guidance.
18. As a beginner sewist, I want the opening, closure allowances, and waistband instruction requirements explained before cutting garment fabric, so that I understand the unfinished construction decisions.
19. As a sewist, I want the guide to remain usable offline, so that I can consult it in my sewing workspace.

## Implementation Decisions

- Extend the existing skirt-results UI; keep the paper-drafting explanation available.
- Create tutorial animations in the existing Rive project and follow its repository instructions for authoring, verification, inspection, and visual review. Do not upload assets to a Rive account as part of previewing.
- Use the existing validated calculation results; do not duplicate radius formulas or add fabric inputs.
- Preserve the ratios of unrounded calculated radii in marking and cutting geometry using uniform scaling to the available viewport. Labels retain the calculator's centimetre formatting. Show coincident lines accurately when allowances are zero; do not exaggerate allowance spacing to improve visibility.
- Earlier flat-fabric and folding stages use an illustrative piece large enough for the pattern. Explicitly distinguish that illustration from actual fabric dimensions.
- Each step plays as soon as its drawing is ready, on opening and on every previous or next, with written step explanations. It loops continuously with a completed-pose hold and a smooth reverse. The drawing frame is the control: a tap pauses on the current frame and another continues from it, with a badge showing what a tap does and the same toggle for TalkBack, keyboard and switch access. A pause belongs to one step; entering a different step plays it. With system animations off, each step shows its completed result and the drawing is no control. Accessible controls and text must convey the lesson without depending on animation or color alone.
- The drawing follows the app theme. Its artboards paint no background of their own, and the app binds four view-model colours from Material roles: `ink` (onSecondaryContainer), `fabric` (secondaryContainer), `fabricFold` (tertiaryContainer, the layer being folded) and `accent` (tertiary, the cut trace while it plays). Marking and cutting reuse the cutting card's line key: 1 solid, 2 dashed, 3 dotted and 4 dash-dot, numbered 1 to 4 at the arc ends. Fabric edges keep the written conventions (folds solid, selvedges dotted, raw ends dashed). Each step's resting frame is its completed result, which is what reduced motion shows.
- Each step's written instructions lead with one imperative sentence naming the action to take now, using the numbered line key where it matters. Brief context that changes that action follows: the current radii and line roles while marking, the arcs to cut and not cut, coincident lines and the common origin. Supporting detail sits behind a collapsed "Detalhes" disclosure that reuses the results' disclosure pattern: edge and grain conventions, illustrative-fabric and not-actual-size notes, paper or scrap practice pending validation, the separate opening, closure-allowance and waistband instructions, and playback. The cutting step's context also states briefly, in its own words, that the opening, closure allowances and waistband need separate instructions before cutting garment fabric. The unfolding step uses one lead covering both unfold actions in order: undo the second fold, opening the two upper layers upward, then the first, taking the top layer leftward. When system animations are disabled, the system-motion note appears visibly above the step instructions instead of in "Detalhes"; when they are enabled, it does not appear. Radius values appear once, on the marking step, and the step title is not repeated in the body.
- Keep tutorial dimensions consistent with the active valid calculation; input edits must not leave an actionable tutorial showing stale results.
- Bundle tutorial assets for offline use and retain the app's existing Android compatibility requirements.
- Verify measurement-driven geometry with the available Rive tooling and Android runtime before completing implementation. Rive is the confirmed authoring choice; if accurate geometry cannot be delivered, surface the limitation rather than silently replacing it with schematic geometry.
- Verify the sewing sequence against trustworthy sewing references before finalizing the folds, edge labels, marking origin, and cuts. Construction details are not yet technically validated.
- Before the demonstration can be treated as guidance for cutting garment fabric, explicitly explain that the opening, closure allowances, and waistband require separate instructions. Do not imply guaranteed fit or a complete garment pattern.
- No profile schema, persistence, or calculator formula changes are required.

## Testing Decisions

- Proposed primary automated seam: the existing Android Compose calculator flow, using real tutorial assets and runtime integration. Extend its established behavior-focused UI testing approach rather than testing private playback methods or Rive node names.
- Exercise opening from valid results, step progression, autoplay on entry, tap-to-pause, readable written instructions, dimensional labels, scope notices, and changed-input handling. Assert visible results and accessible semantics.
- Verify the tutorial is usable at supported window sizes and large text settings using the existing calculator UI test patterns.
- UI text assertions alone cannot establish geometric accuracy. Render representative calculations and compare displayed radius proportions against independently computed ratios, including changed skirt lengths and zero allowances. Use Rive verification/inspection and rendered-frame review; introduce a small geometry-level check only if a deterministic proportional mapping needs separate coverage.
- Follow the existing Rive project's compile, inspect, and screenshot-review requirements for edited animation assets. Inspect intermediate folding stages as well as the unfolded result.
- Rendered-pixel checks classify ink, fabric and the frame by the colours the current theme binds, and include a dark-theme pass, so a background painted by the animation file fails them. The looping-playback check samples a fixed span of test-clock time rather than racing a wall-clock timeout.
- Before release, require a sewist to follow the tutorial on paper or scrap fabric and confirm the folds, marking origin, cuts, and unfolded result. Record corrections and repeat practical validation if the physical sequence changes.
- Test public behavior and user-visible geometry, not implementation structure. Reuse the calculator's existing instrumented UI coverage; no new test framework is proposed.
- The product owner accepted this testing approach when authorizing publication.

## Out of Scope

- Checking actual fabric dimensions, fabric yardage, or choosing alternative layouts and panel divisions.
- Making pleats in the finished skirt.
- Physical 1:1 cutting templates, calibrated printing, or printable pattern generation.
- Teaching waistband construction, opening construction, or closure seam allowances in this tutorial; identifying the need for separate instructions is included.
- Other circle fractions, garment types, fit diagnosis, or guaranteed fit.
- New measurement-profile fields, saved projects, or calculation history.

## Further Notes

- Product discovery was confirmed on 2026-10-15. This feature extends the original MVP's paper-drafting scope and includes beginner guidance.
- Keep actual fabric-fit checks with alternative layouts, pleat tutorials, physical 1:1 templates, and waistband instructions on the follow-up roadmap. Their implementation order is not decided.
- Proportional on-screen geometry is confirmed; physical screen dimensions are not intended to match centimetres on fabric.
- Measurement-driven Rive geometry remains a feasibility requirement, and practical sewing validation remains a release gate. Neither has been completed during specification.
