# Drafting journey validation (#29)

Validated the integrated #22 redesign on Android API 36 emulators. This is
technical verification, not evidence of garment fit or usefulness to a sewist.

## Automated checks

- Existing real-app Compose, real-storage, and controlled repository failure
  tests cover direct calculation, grouped errors/recovery, result reveal/edit/
  invalidation, formulas and diagram alternatives, profile reuse/override,
  duplicate names, dates/legacy records, editing/discard/deletion, navigation,
  loading/empty/retry, and truthful storage outcomes.
- Geometry is unchanged. Independently established numerical results,
  fractional precision, explicit zero allowances, invalid signs/non-finite
  input, and nonpositive cutting radii remain covered by the JVM suite.
- Full API 36 phone run at CI's 320x640/density160, animations disabled:
  **46 executed, 0 failures/errors/skips**. After the final heading-only change,
  both navigation tests passed, including the new heading regression. The full
  suite was not repeated for those semantics flags.
- Final Spotless, Detekt, lintDebug, JVM (**8 tests, 0 failures/errors**), debug
  APK, and repository hygiene checks passed. Final logs: `final-narrow-suite2`,
  `headings-final-green`, and `final-quality4` in the evidence directory.

### Final review corrections

The two low-priority standards findings were corrected after the integrated
`57b838d` review: the profile picker accepts a root modifier, and the root FAB
and adaptive panes share the existing width/font-scale breakpoint.

- Before and after this refactor, the same **6 tests passed, 0 failures/errors**
  on API 36 Fold `emulator-5554`: both navigation/heading tests, picker dismissal
  and recreation, wide-editor creation/discard, resizing with large text, and
  wide-calculator results/invalidation. Logs: `review-baseline-targeted.log`
  and `review-final-targeted.log`. The earlier 46-test phone run and 2-test
  heading run above remain evidence for their earlier production trees.
- Spotless, Detekt, lintDebug, JVM (**8 tests, 0 failures/errors/skips**), debug
  APK and test APK builds, all-file hygiene and manual large-file checks passed.
  Logs: `review-quality.log`, `review-hygiene.log`, `review-hygiene-large.log`.
- The feedback APK below was regenerated from this corrected production tree.
  The full device suite and earlier manual journeys were not repeated for this
  refactor; all participant, TalkBack, and hardware-input gaps remain pending.

## Integration corrections

- The source caption now says **Cintura copiada do perfil**, making the copied
  waist explicit. Existing reuse/restoration/deletion tests retain the source
  without introducing a live link. Focused red/green: 1 failed, then 1 passed.
- Calculator, profile list, and picker titles now expose heading semantics;
  the editor already did. The real-app heading regression failed before the
  flags and passed afterward. The calculator heading/intro follows the existing
  editor-heading component pattern, keeping the form within Detekt's limit.
- CI's 320x640 device exposed two test problems. Forced 1200x900dp content was
  only 810px tall on the local phone, while native IME insets were not scaled;
  its panes had zero height. The existing test helper now retains physical
  viewport height. The original failing wide-editor test then passed.
- A merged profile row's center tap hit its nested Actions button at 320dp.
  The captured popup offered Edit/Delete, rather than opening the editor.
  The two affected tests now tap the unmerged headline, retaining touch
  injection and enabled-state checks. All 7 controlled-failure tests passed
  at 320x640/density160 with animations disabled.

## Actual Android restoration and Back

On the API 36 Fold, HOME put the task in STOPPED with a saved-state bundle;
`am kill com.almeidajr.molda` removed its PID; `am start` brought back the
existing task with a new process. No force-stop or data wipe was used.

- Calculator waist: PID 10523 → 10667, `91,5` restored.
- Profile draft: PID 10811 → 11012, name `Rascunho morte`, waist `82,75`,
  date `12/10/2026`, notes `Papel e lapis` restored. The lower field required
  closing the restored IME and scrolling. Back still confirmed dirty discard.
- Complete calculator: PID 11012 → 11573, all five inputs
  `82,75 / 2 / 60 / 1 / 2` and copied profile name restored. Previous results
  were absent until explicit Calculate, which reproduced
  `13,49 / 73,49 / 12,49 / 75,49 cm`.
- Picker-origin draft on the native phone: PID 7646 → 8134 restored all four
  fields. Saving returned to the picker without selecting the new profile;
  dismissing it left the calculator waist blank.
- Native editor Back dismissed IME, then opened the dirty-discard dialog;
  Back on that dialog retained the editor. Initial kills before STOPPED did
  not kill the process and are excluded from this evidence.

## Device and accessibility observations

- #28 native Fold evidence covers opened/closed/half-opened states, horizontal
  hinge after rotation, IME, and 200% text. Draft values persisted and controls
  stayed in safe regions. Native emulator checks complement simulated layouts.
- TalkBack 16.0 was installed and bound with spoken feedback/touch exploration
  enabled. A screenshot shows its focus highlight around the labelled waist
  input and guidance. Speech audio was unavailable: audible announcements and
  a complete independent TalkBack journey remain pending.
- Native pt-BR calendar labels were Portuguese. Its device clock was October
  14: touching future October 15 left selection blank; selecting October 13
  populated `13/10/2026`. Controlled date-boundary tests use October 13.
- Native light/dark screenshots passed visual inspection. Changing the system
  palette from blue to green changed the app's dynamic colors in both modes.
  At system font scale 2, waist `80` and guidance stayed above the IME;
  lower input labels wrapped and Calculate remained reachable by scrolling.
- Native Tab moved from the management action to waist; the Compose suite
  verifies subsequent field/IME traversal. A physical keyboard/mouse and full
  independent keyboard journey remain unverified.
- 36 fully visible target samples across calculator, editor, picker, and
  calendar measured at least 48x48dp. An initially partially scrolled field
  was rechecked fully visible; its full height was 64dp. See
  `touch-targets-fully-visible.csv`; this is sampling, not an exhaustive audit.
- Wide deletion of the currently edited dirty profile uses a named explicit
  confirmation. No unconfirmed deletion or extra deletion policy was found.

## Feedback build and session

The debug APK requires Android API 36 (Android 16) or newer. The participant's
device compatibility is **pending** until its Android version is checked.
Participant installation and human drafting feedback are **pending**; do not claim fit,
successful paper transfer, or usefulness before the session.

Use [the short session checklist](drafting-feedback.md). The debug feedback APK
is `Molda-feedback-api36.apk` in the evidence directory, SHA-256:
`5B1CC9469170921AAFD157EBD4A770D883F859D4F7F46B5D6872C9BB4BDF7A7C`.
Large APKs, screenshots, native trees, XML test results and logs stay outside Git in
`C:/Users/jralm/AndroidStudioProjects/Molda-worktrees/ticket29-evidence/`;
#28 native evidence is in the adjacent `ticket28-evidence/` directory.
