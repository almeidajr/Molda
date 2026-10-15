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
