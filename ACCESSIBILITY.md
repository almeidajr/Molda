# Accessibility

Molda is for sewists turning body measurements into drafting dimensions. That
includes people who use TalkBack, Switch Access, a keyboard, large text, or who
turn animations off. If something in Molda stops you from calculating a pattern,
saving a profile or project, or following a guide, that is a bug we want to fix.

This document covers what we aim for, what contributors check, how to report a
barrier, and the limitations we know about.

## Priorities

- **Target:** we work toward [WCAG 2.2 Level AA](https://www.w3.org/TR/WCAG22/)
  as applied to mobile apps, and Android's
  [accessibility guidance](https://developer.android.com/guide/topics/ui/accessibility).
  This is a goal, not a conformance claim: Molda has not had an accessibility audit.
- **Screen readers:** TalkBack reads headings, fields, actions and results in a
  logical order; fields keep their labels, units and errors; a finished
  calculation and new errors are announced.
- **Other input:** every action works with Switch Access, a keyboard or a mouse,
  with visible focus and no gesture-only actions.
- **Text and display:** text scales to 200% without clipping or overlap; light,
  dark and dynamic colour schemes meet contrast thresholds; meaning never relies
  on colour alone.
- **Motion:** the folding guide's animation can be paused and follows Android's
  *Remove animations* setting.
- **Diagrams:** every number and line in a drawing is also given in text.

## Contributor expectations

UI changes follow the [accessibility and acceptance checks](docs/ui-design-system.md#accessibility-and-acceptance)
in the UI design system. In practice:

- Check changed screens with TalkBack and at 200% font size, and say so in the PR.
- Give standalone icon actions a specific Portuguese label; mark decorative
  icons as decorative.
- Keep interactive targets at least 48dp.
- Cover new semantics (labels, states, announcements) in device tests where they
  carry meaning.
- Android Lint runs all checks, accessibility included, with warnings as errors.

## Reporting a barrier

Open an issue with the
[accessibility barrier form](https://github.com/almeidajr/Molda/issues/new?template=accessibility.yml).
When you can, include what you were trying to do, what went wrong, your device,
Android and Molda versions, and the assistive technology or settings you use.
Screenshots are optional, and you never need to tell us about a disability.

### Severity

The maintainer assigns or confirms severity during triage; you don't have to.

- **Critical:** you can't complete a core task, such as calculating a pattern.
- **Serious:** you can complete it only with great difficulty or a workaround.
- **Moderate:** an annoying or inconsistent experience.
- **Minor:** little effect on use.

### How we respond

Molda has one maintainer, so we don't promise response or fix dates. We will:

- treat the report as expertise, not a complaint, and thank you for it;
- explain next steps, known limitations and any workaround we know of;
- post status changes on the issue;
- ask you, when you're willing, to confirm a fix before closing it.

## Ownership

Accessibility is owned by the maintainer, [@almeidajr](https://github.com/almeidajr),
who triages reports, tracks known barriers and keeps this document current.

## Supported environments

- **Android:** 16 (API 36) or newer, on phones, tablets and foldables, compact
  and wide windows.
- **Assistive technology and settings:** TalkBack, Switch Access, hardware
  keyboard and mouse, font size up to 200%, light/dark theme and dynamic colour,
  *Remove animations*.
- **Tested so far:** automated Compose device tests and checks on an Android 16
  emulator. No assistive-technology user has tested Molda yet.

## Known limitations

- The app is only in Brazilian Portuguese and uses centimetres only.
- The line numbers inside the folding-guide drawing are drawn shapes, not text,
  so they don't grow with the system font size. The written instructions under
  the drawing carry every number as scalable text.

Found another one? Please [report it](#reporting-a-barrier).

## Feedback

Suggestions for this statement or our practices are welcome as an issue or a
pull request. Report active barriers through the form above.
