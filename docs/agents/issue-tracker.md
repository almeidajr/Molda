# Issue tracker: GitHub

Issues and specs live in GitHub Issues for `almeidajr/Molda`.
Use the `gh` CLI from the repository root.

## Conventions

- Create: `gh issue create --title "..." --body-file <file>`
- Read: `gh issue view <number> --json number,title,body,labels,comments`
- List: `gh issue list --state open` with appropriate label filters.
- Comment: `gh issue comment <number> --body-file <file>`
- Apply/remove labels: `gh issue edit <number> --add-label "..."` / `--remove-label "..."`
- Close: `gh issue close <number> --comment "..."`

Use UTF-8 files for multiline bodies. Infer the repository from
the Git remote, or pass `--repo almeidajr/Molda` explicitly.

When a skill says "publish to the issue tracker", create a GitHub issue.
When it says "fetch the relevant ticket", read the GitHub issue.

## Pull requests as a triage surface

**PRs as a request surface: no.**

## Wayfinding operations

A map is one issue labeled `wayfinder:map`. Child tickets use
`wayfinder:<type>` labels: research, prototype, grilling, or task.

Link children using native GitHub sub-issues where available.
Otherwise, add children to a task list in the map and put
`Part of #<map>` at the top of each child body.

Use native issue dependencies where available. Otherwise, record
`Blocked by: #<number>` at the top of the child body.
A ticket is unblocked when all its blockers are closed.

Choose the first open, unassigned, unblocked child in map order.
Claim it with `gh issue edit <number> --add-assignee @me`.
Resolve it by commenting with the result, closing it, and adding
a result summary and link to the map's Decisions-so-far section.
