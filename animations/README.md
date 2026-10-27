# Fabric folding tutorial

The `Marking` artboard uses native Rive data binding, without scripts. Its default
view model has four number properties named after the calculated radii:
`waistStitching`, `waistCutting`, `finishedHem`, and `hemCutting`. Their values are
**diameters in artboard units**, normalized uniformly so the outer cutting
diameter is 800. The common origin is (24, 24) in a 448 × 448 artboard; Android
uses contain fitting. Normalize the unrounded Double ratios before converting
to Float. Do not enlarge allowance spacing.

The same view model has four color properties: `ink`, `fabric`, `fabricFold` and
`accent`. No artboard paints a background, so the host's surface shows through.
Android binds them from the theme (onSecondaryContainer, secondaryContainer,
tertiaryContainer and tertiary); the authored defaults are Molda's light fallback
palette. The CLI capture renders transparency on a dark backdrop, so preview against
a surface by adding a temporary background shape to a scratch copy, and set dark
colors with `--data=ink=FFF2DBF3 --data=fabric=FF514154` and so on.

Lines follow the cutting card's key: 1 solid (waist stitching), 2 dashed (waist
cutting), 3 dotted (finished hem), 4 dash-dot (hem cutting). Each line's number
sits on a fabric-colored disc at an arc end, placed by multiply converters on the
bound diameter. Numerals are stroked paths, since text would need an embedded font.

The default `play=false` state shows the completed diagram without motion.
Setting `play=true` starts continuous ping-pong playback: the arcs sweep out in
line order over 120 frames at 60 fps, hold for 60 frames, then reverse smoothly to repeat without
a discontinuous wrap. Android sets `play=true` as soon as a step's drawing
appears, and keeps the same artboard, view model, state machine and rendering
surface for pause and system-motion changes; pausing stops the Rive surface on
its current frame rather than changing `play`. The `restart` view-model trigger
alternates between two active states sharing the same timeline, with
`reset="true"` (Rive ignores self-transitions, so distinct states restart the
timeline without replacing the surface). The app no longer fires it, since it
has no replay control.
Setting `play=false` returns to an explicitly keyed completed pose, including
when system motion is disabled during playback. Line patterns distinguish reference
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

The Android device test `FabricFoldingFileTest` checks the bundled asset against
what the app binds: every step's artboard and the view-model properties above,
by name and type. It then binds and renders each artboard offscreen into a
bitmap on Rive's own clock, in light and dark palettes: radius locations for
normal, longer-skirt, and zero-allowance calculations, fabric on an unpainted
frame, a still completed pose with `play=false`, and with `play=true` frames
that keep moving past two cycles without going blank. Sewing validation remains
the separate release gate.

## Preparation and folding

`Preparation`, `First fold`, and `Second fold` share the `Marking` view model.
Each has its own state machine: `play=false` presents the completed static step,
while `play=true` loops its 120-frame demonstration, eased in and out, with a
one-second completed hold and a smooth reverse. Each step plays as soon as its
drawing appears, on opening and on every step change; a tap on the drawing
pauses on the current frame to study it, and another continues from there. The preparation
animation grows the lengthwise grain arrow from its center. The folding animations
pivot a half about the crease by mirroring its horizontal/vertical scale; the layer
being folded is drawn in `fabricFold`. Pausing on Android changes playback
without recreating the viewport. Written instructions remain available
independently; system-disabled motion always shows the completed step, with no
pause control.

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

## Circular cuts and unfolding

`Cutting` retains the marking origin, numbers and four bound radii. A wider
`accent` trace follows the waist cutting arc first, then the hem cutting arc,
**behind** the patterned lines. After both cuts the central and outer scraps lift
away and fade with the traces; their fold-edge segments end at the bound radii.
Static playback shows the cut quarter; it never draws a straight closure cut. After the completed-pose hold, the demonstration reverses smoothly to reset;
this reset is animation playback, not an instruction to undo real cuts. The same
exact paths coincide when allowances are zero.

`Unfolding` uses four copies of that measured quarter-annulus, each with an
even-odd circle-minus-circle fill clipped to its quadrant. The group is uniformly
scaled by 0.5 and centered at (224, 224), fitting the complete circle inside the
same 448-unit viewport. It carries no line numbers: at half scale the waist cutout
is too small for them. It reverses the horizontal fold first (frames 0–84), then
the vertical fold (96–180), by mirroring the upper layers about the common origin.
All four radii remain bound to the current unrounded calculator result. Cutting
and unfolding also hold their completed pose for 60 frames, then reverse to repeat.

Review static, intermediate and finished frames, including changed skirt lengths
and zero allowances. Examples from the project directory:

```powershell
rive . --screenshot=build/cut-mid.png --artboard=Cutting --data=play=true --advance=40
rive . --screenshot=build/cut-end.png --artboard=Cutting --data=play=true --advance=155
rive . --screenshot=build/open-half.png --artboard=Unfolding --data=play=true --advance=95
rive . --screenshot=build/open-end.png --artboard=Unfolding --data=play=true --advance=185
```

Android offscreen-render checks cover both artboards at normal and longer lengths
and zero allowances, checking all unfolded quadrants, the waist cutout and the
retained fabric. UI tests cover the complete calculator flow, step navigation,
autoplay and tap-to-pause, construction warnings and stale results. The layout test exercises light/dark themes, heading semantics, wrapped text,
48 dp controls, construction warnings and stale results through the real native
dialog. Font and window settings are changed on the connected device for the
large-text/size runs; Compose configuration overrides alone do not verify a
separate native dialog window. These runs simulate logical sizes on a phone,
not evidence from tablet hardware. Practical sewing validation is
not replaced by any of these checks and remains outstanding in #45.

### Device layout validation, 2026-10-15

On the connected API 36 Samsung phone, the native dialog flow passed in both
light and dark themes at system font scale 2.0 for these logical windows:
375 × 900 dp (portrait), 1000 × 600 dp and 1200 × 800 dp (landscape). ADB window
size/density overrides simulated those sizes; no tablet hardware was used. The
original font scale 1.0, physical 1080 × 2340 size and density 450 were restored.
The ordinary native-window run also passed. These checks establish reachability,
heading semantics, readable contrast, wrapped text, enabled 48 dp controls,
construction notices and stale-result withdrawal, not screen-reader user testing
or physical sewing validation.
