# Domain Docs

## Before exploring

Read the root `GLOSSARY.md` and relevant ADRs under `docs/adr/`.

If these do not exist, proceed silently. Do not suggest creating
them upfront. The domain-modeling skill creates them when terms
or decisions are resolved.

## Layout

This is a single-context repository:

- `GLOSSARY.md`: shared domain vocabulary.
- `docs/adr/`: architecture decision records.

## Use the glossary's vocabulary

Use defined terms in issue titles, proposals, hypotheses, and tests.
Avoid synonyms the glossary explicitly excludes.

If a needed concept is missing, reconsider the terminology or
note the gap for domain-modeling.

## Flag ADR conflicts

Explicitly identify any existing ADR that a proposal contradicts
and explain why the decision should be reconsidered.
