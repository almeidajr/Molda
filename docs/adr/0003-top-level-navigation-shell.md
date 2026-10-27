# Molda gets a top-level navigation shell, replacing launch-into-the-calculator

The MVP design (`docs/ui-ux-design.md`) launched straight into the full-circle skirt calculator, with no home screen and no navigation bar, because the skirt was the only feature. Molda is growing into several pattern drafts (moldes), a shared library of guides (guias), saved projects (projetos) and app-wide measurement profiles, so we replaced that decision with Material 3's conventional navigation. A `NavigationSuiteScaffold` holds four top-level destinations, **`Moldes` · `Projetos` · `Guias` · `Perfis`**, shown as a bar on compact windows and a rail on wider ones. The app opens on the `Moldes` catalog. Each destination keeps its own back stack, re-selecting the current destination returns to its root, and Back from another destination's root goes to `Moldes`. Navigation is a typed Navigation 3 back stack, not screen flags.

## Considered options

- **Keep launching into the skirt calculator and add entries to its app bar.** Rejected: every new molde or guide would be buried behind the first one.
- **A single home catalog listing everything.** Rejected: Projetos and Perfis are places the sewist returns to, not items in a list, and Material 3 users expect a navigation bar for that.
- **A separate top-level Tools (Ferramentas) destination.** Rejected for now: every planned tool works on a calculated result, so tools open from that result.

## Consequences

- About and the future in-app update check (GitHub Releases) open from a top-app-bar action, not from a fifth destination.
- The catalog and library list only features that exist; no "coming soon" entries.
- The fabric-folding guide also opens from the `Guias` library, with labeled example dimensions instead of a calculation's radii.
