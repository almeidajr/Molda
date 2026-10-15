# Feature roadmap

This records product direction from the fabric-folding discovery session. It is not an implementation specification; issues and specs live in GitHub Issues.

## Current discovery: full-circle skirt fabric-folding tutorial

Status: discovery scope confirmed by the product owner on 2026-10-15. Specification published as [issue #41](https://github.com/almeidajr/Molda/issues/41), labelled `ready-for-agent`. Implementation has not started.

- Teach beginners the sequence from flat fabric through folding, marking, cutting, and unfolding.
- Create the tutorial animations with Rive, following `animations/AGENTS.md`.
- Assume fabric large enough to hold the pattern and state that condition explicitly.
- Open the tutorial from the calculator results, beside the existing drafting explanation.
- Play each Rive step on demand, with replay, previous, and next controls and a written explanation.
- Preserve the proportions of the current calculation's radii in the marking and cutting diagrams, resizing the drawing to fit the screen. Label dimensions and state that the drawing is not actual size.
- Use an illustrative piece large enough for the pattern in the flat-fabric and folding stages; distinguish its illustrative size from calculated pattern dimensions.
- Demonstrate folding and circular cuts, then explicitly explain that the opening, closure allowances, and waistband require separate instructions before cutting garment fabric. The unfolded circle is not a complete garment pattern.
- Verify the sequence against sewing references, then require a sewist to follow it on paper or scrap fabric and confirm the folds, marking origin, cuts, and unfolded result before release.
- Measurement-driven geometry in Rive still needs technical verification.
- This extends the original MVP's paper-drafting guidance; it does not change the historical scope recorded in `product-discovery.md`.

## Follow-up features

- Check actual fabric dimensions and choose alternative layouts when the pattern does not fit. This is separate from the first folding tutorial.
- Teach how to make pleats in the finished skirt. This is separate from folding fabric before cutting.
- Provide a physical 1:1 cutting template. This is separate from an on-screen drawing with accurate proportions.
- Provide waistband instructions to consult before cutting garment fabric.

Follow-up priority and implementation details are not yet decided.
