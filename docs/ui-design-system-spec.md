## Problem Statement

Working seamstresses need a calm, consistent pattern calculation assistant whose
measurements, actions and drafting results are easy to read at the cutting table.
Molda's existing flows are agreed, but typography, visual grouping and reusable
component contracts need an explicit Material 3 Expressive system.

## Solution

Apply the confirmed design system to the existing calculator and measurement
profile journeys. Preserve dependable calculations and data safety while making
inputs, reference dimensions and cutting dimensions consistently understandable.

## User Stories

1. As a seamstress, I want the calculator to open immediately, so that I can start drafting quickly.
2. As a seamstress, I want Brazilian Portuguese labels and centimetres, so that dimensions fit my workflow.
3. As a seamstress, I want consistent typography, so that I can distinguish headings, guidance and dimensions.
4. As a seamstress, I want all four radii equally readable, so that no required drafting dimension is overlooked.
5. As a seamstress, I want explicit reference and cutting groups, so that I transfer the correct lines to paper.
6. As a seamstress, I want values with units and two decimal places, so that I can check each dimension precisely.
7. As a seamstress, I want grouped measurements, garment choices and allowances, so that I understand each input.
8. As an occasional user, I want concise contextual explanations, so that I can distinguish ease and allowances.
9. As a seamstress, I want specific field errors, so that I can correct an incomplete calculation.
10. As a seamstress, I want invalid fields brought into view, so that I can recover with the keyboard open.
11. As a seamstress, I want stale results removed when inputs change, so that I never cut using old dimensions.
12. As a seamstress, I want a labelled diagram and expandable formulas, so that I can check the drafting calculation.
13. As a seamstress, I want optional measurement profiles, so that I can reuse waist measurements without an account.
14. As a seamstress, I want a picker showing name, waist and measurement date, so that I select the intended profile.
15. As a seamstress, I want profile selection to change waist only, so that my garment choices remain intact.
16. As a seamstress, I want the measurement source identified, so that I know whether waist came from a profile or manual entry.
17. As a seamstress, I want explicit dates and notes, so that I can distinguish duplicate profile names.
18. As a seamstress, I want drafts retained on failed saves, so that I do not lose entered measurements.
19. As a seamstress, I want named deletion and unsaved-discard confirmations, so that I avoid accidental data loss.
20. As a seamstress, I want loading, empty and retry states, so that profile availability is understandable.
21. As a seamstress, I want manual calculation available when profiles fail, so that I can keep working.
22. As a seamstress, I want consistent Back and dismissal behavior, so that navigation preserves my work.
23. As a seamstress, I want drafts preserved through resizing and Android recreation, so that interruptions do not erase work.
24. As a seamstress, I want offline device-local operation, so that internet access is unnecessary.
25. As an Android user, I want system light/dark and wallpaper colors respected, so that Molda fits my device.
26. As a seamstress, I want restrained expressive shapes and feedback, so that actions are clear and dimensions remain steady.
27. As a tablet or foldable user, I want readable adaptive panes, so that larger windows use space effectively.
28. As a user with large text, I want wrapping and unclipped content, so that every field and dimension stays usable.
29. As a TalkBack user, I want meaningful labels, states and outcome announcements, so that I can calculate independently.
30. As a user with limited dexterity, I want separated targets of at least 48dp, so that actions are reliable.
31. As a keyboard or switch user, I want logical focus order and reachable actions, so that touch gestures are unnecessary.
32. As a user who cannot distinguish colors, I want labelled diagram lines with distinct styles, so that drafting meaning remains clear.
33. As a user who disables animation, I want the same final UI states, so that motion is never required.

## Implementation Decisions

- Modify the existing app theme and calculator/profile presentation; reuse existing state ownership, navigation and Material components.
- Use default Android sans-serif and Material roles: screen headings 28/36sp, section headings 22/28sp, body/fields 16/24sp, supporting text 14/20sp, and all radii 24/32sp with medium emphasis.
- Keep dynamic color and system appearance as runtime defaults. Generate complete fallback light/dark schemes from warm plum seed #67506B with linen-like neutrals and muted sage tertiary tones. Record generated roles and measured contrast before implementing them.
- Consume semantic theme roles rather than raw seed colors. Require 4.5:1 ordinary text and 3:1 large text/meaningful graphical boundaries.
- Use 4/8/16/24/32dp spacing, 16dp compact margins, 24dp pane gaps, 16dp grouped corners, up to 28dp prominent/modal corners and fully rounded primary actions. Retain standard field shapes and Material internal padding.
- Use available Material Expressive feedback and motion, respecting system animation settings and the installed library version. Keep dimensions stationary while read.
- Preserve calculator launch, separate profile management/editor and modal picker; introduce no additional navigation destinations.
- Retain the existing content-fit adaptive rule, hinge avoidance, readable pane limits, font-scale adaptation and draft preservation.
- Define contracts for measurement fields, section groups, actions, profile rows/picker, result groups, diagram, formula disclosure, feedback, date entry and confirmations. Use only applicable states; calculations need no artificial loading state.
- Preserve submit-first validation, waist-only profile selection, manual source labels, result invalidation, duplicate-name handling, explicit dates and safe cancellation/deletion.
- Preserve all four radii, five-input summary, construction limits and collapsed formulas. Existing numerical geometry and persistence contracts remain unchanged.
- Use official Material Symbols vector resources with theme tint and appropriate accessible names; decorative icons avoid duplicate announcements.
- No schema changes, new domain terms or new production dependencies are required by this specification.

## Testing Decisions

- Test observable behavior at the existing Compose UI journey boundary, rather than theme internals or component implementation details. The user confirmed this validation approach in the design interview.
- Reuse the existing skirt calculator and measurement-profile navigation, storage and failure tests as prior art. Retain independent numerical tests unchanged.
- Verify direct calculation, invalid-input recovery, profile reuse, manual override, stale-result removal and all four displayed radii.
- Verify create/edit/cancel/delete, picker dismissal/return, loading/empty/failure states, duplicate profiles and preservation after storage failures.
- Verify drafts through navigation, rotation, process recreation and resizing.
- Exercise compact/wide layouts, light/dark and several dynamic palettes, 200% text, IME/system insets, keyboard/switch focus and disabled animations.
- Measure final rendered text/graphic contrast; inspect diagram textual equivalents and labels. Perform audible TalkBack journeys and a real seamstress drafting session; automated semantics checks alone do not establish usability.
- Prefer the existing UI seam; no new test harness or abstraction is required. Add component-level tests only where journey tests cannot observe a meaningful behavior.

## Out of Scope

Additional garment calculations, automatic pattern generation, accounts, sync,
backup/export, saved projects/history, search, undo, appearance settings, new home
screens or bottom navigation, calculation geometry changes and storage migrations.
The current documentation commit does not implement screens; this issue defines
the subsequent implementation work.

## Further Notes

The product owner accepted the complete design system after Q1–Q9. It supersedes
the earlier standard-visual-system-only/no-custom-branding restriction while
preserving established interaction and data-safety requirements. Palette
generation and contrast validation are implementation prerequisites, not new
product decisions. Physical-device accessibility and seamstress feedback remain
pending; this spec makes no claim that the proposed UI has passed those checks.
