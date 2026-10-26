# Feature roadmap

This records product direction from the fabric-folding discovery session. It is not an implementation specification; issues and specs live in GitHub Issues.

## Current discovery: full-circle skirt fabric-folding guide

Status: discovery scope confirmed by the product owner on 2026-10-15. Specification published as [issue #41](https://github.com/almeidajr/Molda/issues/41). Software for [#42](https://github.com/almeidajr/Molda/issues/42), [#43](https://github.com/almeidajr/Molda/issues/43), and [#44](https://github.com/almeidajr/Molda/issues/44) is integrated, with measurement-driven geometry verified in Rive and Android rendered frames. The complete change is in [PR #46](https://github.com/almeidajr/Molda/pull/46).

The repeatable practical-trial protocol for [#45](https://github.com/almeidajr/Molda/issues/45) is prepared. No participant is currently available; the real sewist paper/scrap-fabric trial remains an outstanding release gate and has not passed.

- Teach beginners the sequence from flat fabric through folding, marking, cutting, and unfolding.
- Create the guide animations with Rive, following `animations/AGENTS.md`.
- Assume fabric large enough to hold the pattern and state that condition explicitly.
- Open the guide from the calculator results, beside the existing drafting explanation.
- Play each Rive step on entry, paused and continued with a tap on the drawing, with previous and next controls and a written explanation.
- Preserve the proportions of the current calculation's radii in the marking and cutting diagrams, resizing the drawing to fit the screen. Label dimensions and state that the drawing is not actual size.
- Use an illustrative piece large enough for the pattern in the flat-fabric and folding stages; distinguish its illustrative size from calculated pattern dimensions.
- Demonstrate folding and circular cuts, then explicitly explain that the opening, closure allowances, and waistband require separate instructions before cutting garment fabric. The unfolded circle is not a complete garment pattern.
- Verify the sequence against sewing references, then require a sewist to follow it on paper or scrap fabric and confirm the folds, marking origin, cuts, and unfolded result before release.
- Measurement-driven geometry has passed technical checks; those checks do not replace practical sewing validation.
- This extends the original MVP's paper-drafting guidance; it does not change the historical scope recorded in `product-discovery.md`.

## Navigation shell

Status: in progress under [#83](https://github.com/almeidajr/Molda/issues/83), following [ADR 0003](adr/0003-top-level-navigation-shell.md). The app opens on the `Moldes` catalog inside a navigation suite (a bar on compact windows, a rail from 600dp) with four destinations, `Moldes` · `Projetos` · `Guias` · `Perfis`, each keeping its own back stack ([#88](https://github.com/almeidajr/Molda/issues/88)). The full-circle skirt calculator opens from the catalog, `Perfis` holds profile management, and `Projetos` shows an empty state until projects can be saved. The `Guias` library with the fabric-folding guide ([#89](https://github.com/almeidajr/Molda/issues/89)), creating a profile from the picker without leaving `Moldes` ([#90](https://github.com/almeidajr/Molda/issues/90)) and the About screen ([#91](https://github.com/almeidajr/Molda/issues/91)) are implemented too.

## Follow-up features

- Check actual fabric dimensions and choose alternative layouts when the pattern does not fit. This is separate from the first folding guide.
- Teach how to make pleats in the finished skirt. This is separate from folding fabric before cutting.
- Provide a physical 1:1 cutting template. This is separate from an on-screen drawing with accurate proportions.
- Provide waistband instructions to consult before cutting garment fabric.
- Tell testers inside the app when a newer beta build is available. Until then, the product owner tells testers about each new beta build.

Follow-up priority and implementation details are not yet decided.
