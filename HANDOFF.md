# ALTER Keyboard handoff

Updated: November 1, 2026. Repository: [hardikk-gupta/alter-keyboard](https://github.com/hardikk-gupta/alter-keyboard).
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

## Branch and review snapshot

At the November 1 inspection:

- `main`: `065b035d6`, merge of [PR #30](https://github.com/hardikk-gupta/alter-keyboard/pull/30).
- Active branch: `codex/fix-streaming-recognizers`.
- Open [PR #31](https://github.com/hardikk-gupta/alter-keyboard/pull/31): checked Moonshine streaming,
  visible failure messages, native Nemotron error reasons, rebuilt ABIs and matching documentation.
- Runtime implementation commit: `e8fadc111`; documentation synchronization is added on that branch.
- These are snapshots, not permanent assertions. Fetch/inspect Git and PR status at the next session.
  Do not merge, close or replace PR #31 automatically; the repository owner merges.

## Product state

The app is a FUTO-derived full Android keyboard with ALTER voice UI and a shared restricted dictation agent.
It does not require the main ALTER app and does not execute dictated commands or send messages automatically.
Only these speech choices remain: Parakeet CTC INT8, Parakeet TDT FP16, Nemotron Q4,
Moonshine Medium, and Android on-device recognition when available. LFM2.5 350M is the only optional cleanup model.
Parakeet/Moonshine are English-only. Nemotron and Android retain their own supported language modes.
No external language detector, Hindi recognizer routing or Romanization is active.

PR #30 removed retired downloads/UI/runtime paths and the empty icon gutter. Audio drafts survive upgrades;
retired-model PCM can be explicitly reprocessed with a remaining downloaded recognizer. Capture resumption
still requires matching recording identity. Keep the FUTO attribution, license, payment features and notices.

## Pending runtime fix: PR #31

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

## Next agent actions

1. Verify current branch/main/PR state and read the plan before edits.
2. Keep documentation work with PR #31 while it remains open; reconcile against main if the owner merges it.
3. After the matching APK is available, obtain the visible failure message, selected model/language,
   APK version and reproduction steps from the phone. Do not claim an unseen failure is diagnosed.
4. Test English, Hindi and mixed speech as applicable, pause/resume, interruption, saved-audio recovery,
   background processing/clipboard delivery, private fields and install-over-update with the shared key.
5. Implement only evidence-supported fixes. Native source changes require both ABI rebuilds.
6. Immediately update plan, handoff, changelog and all affected Markdown with actual checks and limitations.

## Open limitations

Google's exact current Android recognizer weights are not verified as publicly downloadable; Android's
recognition provider varies by device. Android output already goes through Alter's cleanup policy, with the
existing opt-in/short-phrase/explicit-Hindi conditions. The system provider can play its own recording tone;
there is no supported per-app mute switch, and no global phone audio muting is implemented.
