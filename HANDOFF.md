# ALTER Keyboard handoff

Updated: November 6, 2026. Repository: [hardikk-gupta/alter-keyboard](https://github.com/hardikk-gupta/alter-keyboard).
This file, [PLAN.md](PLAN.md), [CHANGES.md](CHANGES.md) and the linked guides are the handoff;
no essential task state should exist only in chat, a local worktree note or `/tmp`.

## Read first

1. [AGENTS.md](AGENTS.md): canonical repository rules and mandatory documentation workflow.
2. [AGENT_INSTRUCTIONS.md](AGENT_INSTRUCTIONS.md): execution and handoff checklist.
3. [PLAN.md](PLAN.md): intended work, decisions, blockers and remaining tasks.
4. [CHANGES.md](CHANGES.md): append-only implementation and validation history.
5. [Current models](docs/VOICE_MODELS.md), [design](docs/DESIGN.md), [sitemap](docs/SITEMAP.md),
   [development setup](docs/DEVELOPMENT.md) and [signing](docs/SIGNING.md).
6. [Markdown inventory](docs/MARKDOWN_INDEX.md): every tracked Markdown file and its maintenance status.

## Current pickup: shared local AI

Verified main is `f5b05d1bc`, merging PR #32; PR #31 is also merged. No open Keyboard PR existed at
inspection. This task uses `codex/shared-ai-inspection` based on that main. Previous PR #32-open claims below
are historical, not instructions to revert Apex/Qwen changes.

[Shared AI inspection](docs/SHARED_LOCAL_AI.md) records exact companion revisions, artifact overlap, adapter
points, security limits and the acceptance plan. The Alter AI agent's exact shared contract SHA/path is unavailable
and the user has confirmed it is not yet published; Alter AI owns publishing it. Per the user's explicit gate, do not invent IPC/AIDL or
claim cross-app reuse. Standalone behavior is untouched. PR #33 records this inspection. Next pickup: obtain the exact SHA, PR URL
and file paths; inspect that revision and report compatibility issues before implementing Keyboard adapters.
Then preserve peer/coordinator trust, lifecycle guarantees and standalone fallback. No companion files changed.

## Product state

The app is a FUTO-derived full Android keyboard with ALTER voice UI and a shared restricted dictation agent.
It does not require the main ALTER app and does not execute dictated commands or send messages automatically.
Before PR #32, the speech choices were Parakeet CTC INT8, Parakeet TDT FP16, Nemotron Q4, Moonshine Medium and
Android on-device recognition, with LFM2.5 350M as the only optional cleanup model. The merged PR #32 adds Oriserve
Hinglish Apex (Roman-letter Hindi + English) and replaces LFM2.5 with Qwen3 1.7B INT8, which also cleans Hindi.
Parakeet/Moonshine are English-only. Nemotron and Android retain their own supported language modes.
No external language detector, Hindi recognizer routing or Romanization is active.

PR #30 removed retired downloads/UI/runtime paths and the empty icon gutter. Audio drafts survive upgrades;
retired-model PCM can be explicitly reprocessed with a remaining downloaded recognizer. Capture resumption
still requires matching recording identity. Keep the FUTO attribution, license, payment features and notices.

## PR #32: Hinglish speech and cleanup

- Apex: Marquestra's Q8_0 GGML (874 MB), pinned. Hindi token, greedy, flash attention, no temperature
  fallback, full 30 s encoder window (shorter windows loop), 28 s saved-audio windows.
- `libalter_whisper.so` is packaged again in three arm64 builds (baseline, dotprod+fp16, +i8mm), built with
  `scripts/build_whisper_native.sh`; `WhisperNative` picks the best for the CPU and falls back to baseline.
- Qwen3 1.7B INT8 `.litertlm` (1.75 GB, pinned) on the existing LiteRT-LM 0.18.0 CPU engine. The Hindi
  bypass is removed and live cleanup during recording is turned off (`VoicePack.LIVE_CLEANUP`).
- Host evidence and limits are in CHANGES.md; nothing was measured on a phone.

Backtrack (Wispr-style spoken corrections) is implemented on top of PR #32: deterministic cue corrections for days,
months, relative days, times and numbers in English and Hinglish, no-cue restatements ("Monday, Tuesday" → Tuesday),
"scratch that", a validator guard against added "and/or/aur/ya", and no editor call when the corrected text is
simple. Qwen3 1.7B turned restatements into lists on the host, so they are deterministic. Rules, never-changed
cases and limits: [docs/BACKTRACK.md](docs/BACKTRACK.md). Not handled: a cue after a verb ("Monday ko rakho nahi
nahi Tuesday ko"), names, "never mind"/"rehne do", undo after insertion.

## PR #31 (merged): streaming recognizer fixes

The user reports Moonshine showing an error icon and Nemotron working only with English selected.
The precise phone-side failure was not visible in the old UI and has not been reproduced on the host.

- Moonshine now uses checked SDK 0.1.5 JNI calls: one stream per recording, adaptive full snapshots,
  one final stop/flush and cleanup after failure/cancellation. Avoid the old convenience wrapper's
  unused default stream, ignored native statuses and duplicate stop.
- Foreground error text appears in a short Toast as well as the error icon. Disabled/private surfaces
  do not display it. The saved draft remains available.
- Nemotron reports native operation/status and requested locale at stream start. Automatic remains a
  null native language hint, Hindi `hi-IN`, English `en-US`; there is no silent English fallback.
- Both arm64-v8a and x86_64 `libalter_nemotron.so` files were rebuilt with the source change.

## Validation evidence and limits

Last runtime verification: October 28, 2026, before this documentation-only update.

- 53 voice unit tests passed (stream finalization, failure handling, cancellation and existing recovery coverage).
- Actual compiled Kotlin/JNI host probe passed Moonshine repeated streaming and saved-audio decoding.
- Pinned Nemotron produced nonempty English/Automatic/Hindi results on English and Hindi clips;
  unsupported-language errors carried the native reason, and subsequent sessions worked.
- Shared TDT host regression passed decoding, input bounds, abort and reuse.
- Tested downloads matched catalog hashes/sizes; both rebuilt Android ABI files matched native merge outputs.
- Android compilation/resources/dex/native merging passed. Full `assembleStableDebug` stopped at the
  missing shared signing keystore. No signed APK/install or real-phone success was established locally.

Reproduction instructions and the committed probe are in
[STREAMING_RECOGNIZER_CHECKS.md](docs/STREAMING_RECOGNIZER_CHECKS.md).
Temporary host models/logs may disappear; they are not a dependency for continuing this project.
Do not commit downloaded weights, raw user audio, credentials, signing keys or unredacted crash dumps.

## November 1 documentation audit

All 46 repository Markdown files are inventoried with current/reference/retired/provenance/upstream roles.
Stale active shared-agent, cleanup, Nemotron and Parakeet claims were corrected; archived source and retired
artifact notes are explicit. Current first-party relative file links resolve. Four pre-existing unresolved
links in preserved upstream/legal copies are listed in [the inventory](docs/MARKDOWN_INDEX.md), without
rewriting those source notices. Root license/NOTICE and preserved upstream content stay byte-identical.

This update changes Markdown only. Runtime files, native binaries and model metadata are unchanged;
no new Android build, inference run, signing or phone verification is claimed for November 1.

## Earlier runtime acceptance actions (still unverified on device)

1. Verify current branch/main/PR state and read the plan before edits.
2. Collect the owner's phone results for the merged Apex and Qwen3 work before tuning further.
3. After the matching APK is available, obtain the visible failure message, selected model/language,
   APK version and reproduction steps from the phone. Do not claim an unseen failure is diagnosed.
4. Test English, Hindi and mixed speech as applicable, pause/resume, interruption, saved-audio recovery,
   background processing/clipboard delivery, private fields and install-over-update with the shared key.
5. Implement only evidence-supported fixes. Native source changes require both ABI rebuilds.
6. Immediately update plan, handoff, changelog and all affected Markdown with actual checks and limitations.

## Open limitations

Google's exact current Android recognizer weights are not verified as publicly downloadable; Android's
recognition provider varies by device. Android output already goes through Alter's cleanup policy, with the
existing opt-in/short-phrase conditions (PR #32 removes the explicit-Hindi bypass). The system provider can play its own recording tone;
there is no supported per-app mute switch, and no global phone audio muting is implemented.
