# Fabric folding tutorial

The `Marking` artboard uses native Rive data binding, without scripts. Its default
view model has four number properties named after the calculated radii:
`waistStitching`, `waistCutting`, `finishedHem`, and `hemCutting`. Their values are
**diameters in artboard units**, normalized uniformly so the outer cutting
diameter is 800. The common origin is (24, 24) in a 448 × 448 artboard; Android
uses contain fitting. Normalize the unrounded Double ratios before converting
to Float. Do not enlarge allowance spacing.

The default `play=false` state shows the completed diagram without motion.
Setting `play=true` reveals the arcs over 120 frames at 60 fps. Android creates a
fresh instance and state machine on replay. Line patterns distinguish reference
and cutting lines even in monochrome; zero allowances retain coincident paths.

From this directory, run the verification and inspection commands in AGENTS.md.
Review both the static frame and the completed playback:

```powershell
rive . --screenshot --advance=1
rive . --screenshot --data=play=true --advance=120
```

The generated `build/animations.riv` is copied to the app's raw resource named
`fabric_folding.riv`. Keep that bundled offline asset synchronized with source.
This project contains no scripts and requires no signing or account upload.

The Android calculator UI tests render the real asset and check radius locations
in captured pixels for normal, longer-skirt, and zero-allowance calculations.
Sewing validation of the complete sequence remains the separate release gate.

## Preparation and folding

`Preparation`, `First fold`, and `Second fold` share the `Marking` view model.
Each has its own state machine: `play=false` presents the completed static step,
while `play=true` plays its 120-frame demonstration once. The preparation
animation reveals the lengthwise grain guide. The folding animations pivot a
half about the crease by mirroring its horizontal/vertical scale. The Android
controls recreate the artboard and state machine for every replay and reset
playback on step changes. Written instructions remain available independently.

Flat cloth has selvages left/right and raw ends top/bottom. Move the LEFT
selvage to the RIGHT, then the TOP raw end to the BOTTOM. The retained quarter
is at bottom-right; its TOP and LEFT are closed folds. Its upper-left dot is
the common origin. The marking step zooms that quarter without rotation.
Continuous edges indicate folds, dotted edges selvages, dashed edges raw ends;
written edge positions and layer counts carry the same information without color.

Verify and inspect after every RML change. Review each fold at its start,
intermediate frames (for example 30 and 90), completed playback (120), and
stationary state. Example:

```powershell
rive . --screenshot=build/first-mid.png --artboard="First fold" --data=play=true --advance=30
rive . --screenshot=build/second-end.png --artboard="Second fold" --advance=1
```

See [sewing references](../docs/fabric-folding-references.md) for sources and
limitations. These drawings are illustrative fabric arrangements, not fabric-fit
checks. Practical sewist validation remains outstanding.
