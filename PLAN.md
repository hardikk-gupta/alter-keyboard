# Current plan

Updated: November 6, 2026. Read [HANDOFF.md](HANDOFF.md) and verify live Git/PR state before continuing.
This records task-level plans and decisions, not private reasoning or a transcript of the chat.

## Current task: optional shared local AI (Keyboard only)

Scope: inspect current standalone implementation, retrieve the Alter AI agent's exact contract, then implement
Keyboard provider/client discovery, compatible reuse and local fallback against that contract. Do not edit companions.

- Completed: read current source/rules, fetch main and inspect open work; PR #32 is merged at `f5b05d1bc`.
- Completed: read companion main sources read-only; compare actual artifacts and runtime capabilities.
- Waiting: user confirmed the contract is not yet published; Alter AI owns publishing it. No AIDL, endpoint, trust pins,
  sharing setting or IPC adapter is invented. See [inspection/pickup](docs/SHARED_LOCAL_AI.md).
- Completed: record Keyboard seams, signing uncertainty, companion gaps and device/security acceptance matrix.
- Next: obtain exact commit SHA, PR URL and file paths from Alter AI; inspect that revision, record source hashes
  and report compatibility issues before implementation. Only then implement Keyboard adapters against that contract
  with trust/model/version/lifecycle/isolation tests. No separate protocol.
- Validation now: documentation links/inventory/whitespace and catalog comparison only; no new runtime claims.

The following November 2 task record is historical; its PR is now merged.

## Current task: Hinglish speech and cleanup (PR #32)

The user's requirement: the retained models are good for English but not Hinglish. Add Oriserve's
Hindi2Hinglish Apex with as little compression as still runs well on a phone, and pair it with a cleanup
model that handles Hindi (Qwen3 1.7B was chosen over LFM2.5 350M).

| Work | Status | Result / next action |
| --- | --- | --- |
| Apex Q8_0 as a pinned download on the Whisper bridge | Done in open PR #32 | Q8 = FP16 text on 12 sample clips, half the time; FP16 not offered |
| Whisper decode tuning | Done in open PR #32 | Hindi token, greedy, flash attention, no temperature fallback; full 30 s window kept |
| arm64 feature-level Whisper builds | Done in open PR #32 | Baseline, dotprod+fp16, +i8mm; best chosen from `/proc/cpuinfo` |
| Qwen3 1.7B INT8 replaces LFM2.5 | Done in open PR #32 | Host JVM test: 9/11 accepted on Hinglish/English vs 0 useful for LFM |
| Remove Hindi cleanup bypass; live cleanup off | Done in open PR #32 | Cleanup once after recording; mechanism kept and tested |
| Merge `main` (PR #31) into PR #32 | Done | Conflicts in SpeechModels.kt and docs resolved |
| Phone test of the PR #32 APK | Pending, user | Apex seconds after stop, Hinglish accuracy, cleanup quality/time, RAM |
| Merge PR #32 | Owner action pending | Hardikk or Chirag merges |
| Backtrack / Wispr-style corrections | Implemented on top of PR #32, not yet pushed to a PR | Deterministic cues (EN + Hinglish), restatements, "scratch that", conjunction guard; [docs/BACKTRACK.md](docs/BACKTRACK.md) |
| Backtrack follow-ups | Not started | Cue after a verb, names, "never mind"/"rehne do", undo cleanup after insertion |

## Previous documentation task (PR #31, merged)

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
| Checked Moonshine streaming / visible errors | Merged in PR #31 | 53 unit tests and actual Kotlin/JNI host probes passed on October 28 |
| Native Nemotron failure reasons | Merged in PR #31 | Both ABI libraries rebuilt; Automatic/Hindi/English produced host results |
| Merge PR #31 | Done | Merged as `9145b9dfc` |
| Reproduce Moonshine failure on the phone | Pending after installing the matching build | Capture the new visible error, model/language, APK version and exact steps |
| Reproduce Nemotron English-only report | Pending on phone | Host tests do not reproduce it; do not silently force English |
| Validate signed install and device behavior | Blocked locally / pending on phone | Shared signing key absent from this cloud environment |
| Remove Android provider beep | Platform limitation / unverified | No supported per-app mute API; no global-volume workaround is implemented |

## Decisions to retain

- Do not restore the small Whisper/Oriserve Swift models, Moonshine Tiny/Small, Handy, external language
  routing, IndicConformer, IndicXlit or ICU Romanization without a new product decision. Their Hindi was
  not good enough. Oriserve Apex (PR #32) is the new product decision for Hinglish.
- Preserve native script, dictation meaning, private drafts and existing selection. Never discard audio
  to test a different model; use the explicit saved-audio recovery path.
- Keep model rows concise. Only actionable error/progress text belongs in that flow.
- Cleanup stays optional and uses Qwen3 1.7B; short/simple phrases skip inference; Hindi is cleaned too.
- Backtrack is deterministic, not left to Qwen3 1.7B (it turned "Monday, Tuesday" into a list). Relative days
  collapse only with a cue ("kal parso" and "aaj kal" are idioms).
- Apex keeps the full 30 s encoder window: shorter `audio_ctx` windows made it loop on tokens.
- Do not describe host success as a phone fix, phone accuracy score or measured mobile latency.

Before implementation, update this file with scope, intended changes, dependencies and validation.
After each logical change, update status and the related handoff/changelog immediately, in the same commit.
