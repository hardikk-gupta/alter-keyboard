# Current plan

Updated: November 1, 2026. Read [HANDOFF.md](HANDOFF.md) and verify live Git/PR state before continuing.
This records task-level plans and decisions, not private reasoning or a transcript of the chat.

## Current documentation task

The user's requirement: all agent handoffs, instructions, completed changes, planning and remaining work
must live in this GitHub repository. Documentation must stay synchronized immediately with each change.

| Work | Status | Result / next action |
| --- | --- | --- |
| Audit all tracked Markdown | Complete | 46 files inventoried; current, retired, provenance and preserved upstream roles recorded |
| Correct stale active instructions | Complete | Shared-agent README, cleanup status, Nemotron/Parakeet guides and retired source/provenance reconciled |
| Prepare handoff and operating instructions for PR #31 | Complete | HANDOFF.md, AGENT_INSTRUCTIONS.md and this plan included in the documentation change |
| Document reproducible setup and verification | Complete | docs/DEVELOPMENT.md and existing committed runtime probe/check guides |
| Require documentation with every change | Complete | Mandatory immediate all-Markdown review/affected-file updates in AGENTS.md |
| Validate links, inventory and consistency | Complete | 46-file coverage, first-party links, protected-byte checks and whitespace pass; four preserved upstream exceptions documented |

## Runtime work and acceptance

| Work | Status | Evidence / next action |
| --- | --- | --- |
| Simplify voice choices and UI | Merged in PR #30 | Both Parakeets, Nemotron, Moonshine Medium, Android recognition; LFM cleanup only |
| Checked Moonshine streaming / visible errors | Implemented in open PR #31 | 53 unit tests and actual Kotlin/JNI host probes passed on October 28 |
| Native Nemotron failure reasons | Implemented in open PR #31 | Both ABI libraries rebuilt; Automatic/Hindi/English produced host results |
| Merge PR #31 | Owner action pending | Agents create/update PRs; Hardikk or Chirag merges |
| Reproduce Moonshine failure on the phone | Pending after installing the matching build | Capture the new visible error, model/language, APK version and exact steps |
| Reproduce Nemotron English-only report | Pending on phone | Host tests do not reproduce it; do not silently force English |
| Validate signed install and device behavior | Blocked locally / pending on phone | Shared signing key absent from this cloud environment |
| Remove Android provider beep | Platform limitation / unverified | No supported per-app mute API; no global-volume workaround is implemented |

## Decisions to retain

- Do not restore Whisper/Oriserve, Moonshine Tiny/Small, Handy, external language routing,
  IndicConformer, IndicXlit or ICU Romanization without a new product decision.
- Preserve native script, dictation meaning, private drafts and existing selection. Never discard audio
  to test a different model; use the explicit saved-audio recovery path.
- Keep model rows concise. Only actionable error/progress text belongs in that flow.
- LFM cleanup stays optional, short/simple phrases skip inference, and explicit Hindi currently bypasses it.
- Do not describe host success as a phone fix, phone accuracy score or measured mobile latency.

Before implementation, update this file with scope, intended changes, dependencies and validation.
After each logical change, update status and the related handoff/changelog immediately, in the same commit.
