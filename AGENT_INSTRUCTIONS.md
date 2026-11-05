# Agent operating instructions

[AGENTS.md](AGENTS.md) is canonical. This checklist supplements it; it does not replace technical,
licensing, signing, privacy or review rules. Read [HANDOFF.md](HANDOFF.md) and [PLAN.md](PLAN.md) first.

## Before changing anything

- Inspect the working tree, current main, branch and PR status. Preserve user edits and distinguish merged
  behavior from branch-only changes; do not assume that a PR or host test means the phone has that build.
- Record the user's goal, scope, intended changes, dependencies, unresolved questions and validation plan
  in PLAN.md. Record task-level decisions and tradeoffs, not private chain-of-thought.
- Review the complete [Markdown inventory](docs/MARKDOWN_INDEX.md) and identify every affected document.
  Add a new first-party guide to that inventory as soon as it is introduced.

## As soon as each logical change is made

- Update every affected first-party Markdown file immediately, alongside the code/configuration change.
  Do not defer documentation to the end of the task, a later PR, a chat summary or a local-only handoff.
- Update PLAN.md with what was planned, what is now done, what changed in scope and what remains.
- Update HANDOFF.md with the implementation state, relevant files/PRs, actual checks, blockers and next steps.
- Append to CHANGES.md what changed, why, the relevant decision and validation actually performed.
  Preserve older entries as history; do not silently rewrite old results to match the current test count.
- Reconcile README.md, MODIFICATIONS.md, design/sitemap/model guides, module READMEs, runtime source notes
  and other affected instructions. Remove stale active claims; clearly label retired experiments.
- Review all repository Markdown, including unaffected files. Record why preserved upstream/license,
  provenance or historical files remain unchanged in the inventory instead of making cosmetic edits.
- Keep all handoff material needed by another agent in Git. Temporary artifacts may supplement a committed
  summary/reproduction guide but must never be the only location of essential decisions or plans.

## Before committing or handing back

- Check links, inventory completeness and claims against the actual code and observed PR state.
- Run checks appropriate to the change; documentation-only work needs documentation validation, not
  invented or stale runtime test claims. Report signing/device blocks explicitly.
- Include code and documentation together in the same commit/PR. A change without its plan/handoff/
  changelog/documentation updates is unfinished.
- Push the complete authorized work to the repository branch and create/update the PR. Never merge it.
- Report the PR, completed work, tests and remaining limitations accurately. Do not claim a file is in main
  when it exists only in an open PR, or that a phone issue is fixed from host evidence alone.

No credentials, signing keys, downloaded model weights or private recordings belong in these documents.
Canonical filenames are uppercase `CHANGES.md`, `HANDOFF.md`, `PLAN.md`, `AGENTS.md` and
`AGENT_INSTRUCTIONS.md`; do not create competing case-only copies such as `changes.md` or `handoff.md`.

Shared AI pickup: protocol 1.0 is imported at `13b3fcd3802849751dc12793ee32a05ab1c9768d`.
Preserve `shared-local-ai/v1` byte-for-byte. Discovery/trust groundwork is inactive; no IPC inference works yet.
Resolve the reported canonicalizer dependency before generating identities; see docs/SHARED_LOCAL_AI.md.
