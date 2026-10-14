# Integrated accessibility and drafting checks (#38)

Status: **pending human results**. Automated checks cannot establish audible
TalkBack, switch access or successful drafting on paper. Record actual outcomes
below; leave unavailable checks pending.

## Build and device

- Production commit: `548efa72f4b08073d8cbddc9f3a675314550d357`.
- Debug APK: `C:/Users/jralm/AndroidStudioProjects/Molda-issue38-evidence/Molda-issue35-human-check.apk`.
- SHA-256: `ED5747DA6C896BA75B5CBDC4B4D8856D5B8B3507BEA58F857C3D52B81D190A3C`.
- Requires Android 16 / API 36 or newer. Confirm before installation.
- Tester/date, physical device/model, Android/API, screen size: **pending**.
- TalkBack version, keyboard/switch equipment, text scale, appearance and
  wallpaper palette: **pending**.

## Short session

1. Calculate manually with waist **80**, ease **2**, finished length **60**,
   waist seam allowance **1**, hem allowance **2**, all in cm. Expected displayed
   reference radii: **13,05 / 73,05 cm**; cutting radii: **12,05 / 75,05 cm**.
   Transfer all four radii from one center to paper and label each line. Explain
   ease versus seam/hem allowances. Record mistakes, hesitation and corrections.
2. Clear waist and calculate. Correct the announced error with the keyboard
   open. Change length after calculating: stale dimensions must disappear.
   Inspect diagram text and expand/collapse formulas.
3. Create a measurement profile with name, waist, explicit date and notes.
   Reuse it: only waist changes, with the profile source identified. Override
   waist manually: source changes, saved profile remains unchanged. Cancel a
   dirty editor and cancel named deletion before confirming deletion separately.
4. With audible TalkBack, repeat manual calculation, error correction, profile
   selection, override and save/delete. Record spoken units/errors, result and
   source announcements, diagram equivalent and logical traversal; note duplicate
   or missing announcements. Verify focus after picker/dialog dismissal.
5. With a physical keyboard and then switch access, reach every action: profile
   management/create/edit/date/calendar/save/cancel/actions/delete/confirm,
   picker/select/dismiss/create/retry when available, calculator fields/calculate/
   edit/formulas. Record visible focus and final states without touch assistance.
6. Repeat core calculation/profile journeys with animations disabled and 200%
   text. Check wrapping, separated targets, keyboard/system bars, portrait and
   landscape/resizing. On a foldable, verify actual hinge clearance and draft
   preservation; otherwise record this physical check unavailable. Check system
   light/dark and at least three distinctly different wallpaper palettes.

## Outcome record

| Check | Pass/fail/pending | Observations / defect |
| --- | --- | --- |
| Device compatibility and APK identity | Pending | |
| Correct transfer of four radii to paper | Pending | |
| Ease/allowance understanding | Pending | |
| Profile reuse in participant workflow | Pending | |
| Audible TalkBack calculation/errors/results/source/diagram | Pending | |
| TalkBack profile save/delete and dismissal focus | Pending | |
| Every action with physical keyboard and visible focus | Pending | |
| Every action with switch access and dismissal focus | Pending | |
| Equivalent states with animations disabled | Pending | |
| 200% text, themes/palettes, orientation/insets | Pending | |
| Physical foldable hinge (if available) | Pending | |

Participant feedback: **pending**. Record what helped, what confused, any wrong
line/value copied, retrabalho and whether profile reuse fits their usual work.
Do not infer garment fit or treat the schematic diagram as a calibrated printout.

Return the completed table, actual inputs/radii transferred, configuration and
participant feedback to the implementation orchestrator. #38 remains incomplete
until required human results are recorded and actionable defects are resolved.
