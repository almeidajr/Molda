# Molda product discovery

Status: discovery confirmed by the product owner on 2026-10-12; specification published in [issue #13](https://github.com/almeidajr/Molda/issues/13). This document records discovery rather than the implementation specification.

## Product vision

Help sewists who already draft on paper turn body measurements into clear, checkable drafting dimensions, and reuse measurements for people they sew for.

The first MVP delivers this through a full-circle skirt calculator and saved measurement profiles, in Brazilian Portuguese and centimetres. It assists an existing drafting workflow rather than promising a complete pattern or guaranteed fit.

## User problems and evidence

- Repeated arithmetic and uncertainty about inputs or allowances may interrupt drafting.
- Reusing a person's body measurements may reduce repeated entry; saved profiles are an explicit owner requirement, not yet an observed user need.
- One Brazilian, pt-BR-speaking sewist is available for feedback after the MVP exists. No user interview, observed workflow, or practical drafting validation has taken place.
- Radius calculations are the owner's initial example. Their frequency and value to the intended audience remain hypotheses.

## Agreed direction

- Initial audience: sewists who already know basic drafting and regularly draft garments on paper.
- Initial problem hypothesis: repeated arithmetic and uncertainty about measurements or allowances interrupt drafting. This has not yet been observed in a user session.
- Product promise: explained calculations that sewists transfer to paper, with inputs, assumptions, formulas, and results.
- First workflow: a full-circle skirt (saia godê inteira), made in woven fabric with a waistband and closure. Its waist opening uses waist circumference plus explicitly entered waist ease.
- Initial language and units: Brazilian Portuguese and centimetres.
- Saved measurement profiles are required in the MVP. Users can keep multiple profiles for themselves or people they sew for, identified by a name or nickname.
- Initial profile contents: name/nickname, waist circumference, measurement date, and optional notes. Profiles support editing and deletion.
- Profiles work offline and are stored only on the device. No login, cloud sync, backup, or export is included in the MVP; users must understand that clearing app data or replacing the device may lose their profiles.
- Selecting a profile fills the calculation's waist measurement. Changes to calculation inputs do not change the profile; updating a profile is an explicit action.
- Skirt length, ease, and seam/hem allowances belong to the calculation, not the body profile. Saved projects and calculation history are deferred.
- Calculation inputs: waist circumference, explicit waist ease, desired finished skirt length measured from the waist seam, waist seam allowance, and hem allowance.
- Results: waist stitching-line radius, finished hem radius, waist cutting radius, and hem cutting radius. Reference dimensions and cutting dimensions must be clearly distinguished.
- Circular geometry: for waist W, ease E, finished skirt length L, waist seam allowance S, and hem allowance H, use r = (W + E) / (2π); finished hem radius = r + L; waist cutting radius = r - S; hem cutting radius = r + L + H. These are circular reference dimensions, not a complete cutting pattern.
- The sewist handles panel layout, closure seam allowances, and waistband drafting. Fabric-yardage calculations are deferred. Show these boundaries beside results.
- Include a labelled diagram distinguishing stitching and cutting lines, and brief guidance for measuring waist circumference at the intended waistband position.
- Profiles are optional: users can enter a waist measurement directly without creating one.
- Accept pt-BR decimal commas. Waist and finished length must be positive; ease and allowances can be zero but cannot be negative. Require explicit values for ease and allowances, with no hidden assumptions.
- Reject a waist seam allowance that leaves a zero or negative waist cutting radius. Invalid inputs produce clear errors rather than usable-looking results.
- Calculate with unrounded values and display radii to two decimal places in centimetres.
- Keep the repository's minimum Android API at 36. Compatibility with the available reviewer's phone has not been confirmed.
- Validation: gather feedback on a working MVP from the available Brazilian, pt-BR-speaking sewist. Ask her to draft from the results, compare with her usual calculation, and explain anything unclear. Success includes correct use and perceived usefulness; numerical agreement alone does not establish usefulness or fit.
- Demand validation is deferred until MVP feedback; no user workflow has been observed yet.
- Outside the initial MVP: half- and quarter-circle variants, printable pattern generation, fit diagnosis, and additional drafting methods.

## Technical constraints supplied by the owner

- MVVM, Clean Architecture, and unidirectional data flow.
- Feature-first vertical slices with explicit UI, Domain, and Data layers.
- Framework-independent domain logic and use cases in pure Kotlin.
- Jetpack Compose, Material 3, Coroutines, StateFlow, and lifecycle-aware state collection.

## Initial requirements and MVP scope

1. Calculate full-circle skirt dimensions from direct input or a selected profile's waist measurement.
2. Explain the inputs, construction assumptions, formulas, and four radius results in pt-BR, with a labelled diagram.
3. Keep body measurements separate from garment choices and cutting allowances.
4. Create, select, edit, and delete multiple measurement profiles. Profile fields are name/nickname, waist circumference, measurement date, and optional notes.
5. Persist profiles locally across closing and reopening the app. Calculations and profile operations work offline.
6. Validate input before presenting results; preserve calculation precision until display.
7. Keep profile updates explicit and separate from changes made for a calculation.
8. Support Android API 36 and newer.

## Explicit exclusions

- Other circle fractions, garment types, or drafting systems.
- Automatic or printable pattern generation, fit diagnosis, or guaranteed fit.
- Panel layouts, closure seam allowances, waistband drafting, and fabric yardage.
- Additional body-measurement fields, client administration, accounts, cloud sync, backup/export, saved projects, or calculation history.
- Other languages or units, and a full drafting course.

## Validation and readiness

The first feedback build is ready when calculation and profile workflows work offline, profiles survive closing/reopening, invalid inputs produce clear errors, and radii pass independent numerical checks. A numerical reference case is W = 80 cm, E = 2 cm, L = 60 cm, S = 1 cm, H = 2 cm: displayed radii should be 13.05, 73.05, 12.05, and 75.05 cm respectively. This example checks arithmetic, not garment fit.

For practical feedback, ask the participant to draft from the results, compare with her usual calculation, and explain unclear inputs or outputs. Capture whether she can distinguish stitching and cutting lines, transfer the dimensions correctly, reuse a profile, and identify a real situation where she would use Molda again. Record mistakes and requested changes before expanding scope.

## Risks and unresolved evidence

- Calculation friction and profile reuse have not been validated as valuable user problems.
- One participant provides an initial signal, not evidence for the entire audience.
- Circular reference geometry has not been checked through a practical drafting session; construction and fitting remain the sewist's responsibility.
- Requiring API 36 might exclude the available participant or other intended users.
- Local-only profiles may be lost when app data is cleared or a device is replaced. The experience must communicate this limitation.

## Next gate

The product owner has confirmed the shared vision, initial requirements, and MVP scope. The implementation specification is published as [GitHub issue #13](https://github.com/almeidajr/Molda/issues/13), labelled `ready-for-agent`, with owner-confirmed testing boundaries. This discovery document records the product rationale; the issue is the specification. Publishing the specification does not start implementation.

## Research references

- [Müller & Sohn: Taking Measurements](https://www.muellerundsohn.com/en/allgemein/taking-measurements/): measured and calculated dimensions depend on the drafting system.
- [NMSU: Pattern Alteration](https://pubs.nmsu.edu/_c/C228/index.html): measurement and fit workflows distinguish ease from seam allowances.
- [BERNINA: Quarter Circle Skirt](https://blog.bernina.com/en/wp-content/uploads/sites/4/2024/03/Quarter-Circle-Skirt-How-to-draft.pdf): a concrete reference for a possible radius calculation workflow.

These references inform discovery. Molda has selected explicit full-circle geometry for its initial calculator; it has not yet been validated through a practical drafting session.
