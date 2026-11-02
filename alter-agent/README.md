# ALTER shared dictation core

Reusable Android library with no app, speech runtime, UI or model-engine dependency. The host calls
`AlterDictationAgent.process(transcript) { chunk -> modelOutput }`. Existing formatter/request/validation functions
remain available for host integration. The coroutine callback propagates cancellation; errors/timeouts preserve text.

Extracted from ALTER Keyboard's port of ALTER's voice pack, with agent design reviewed against
https://github.com/hardikk-gupta/ALTER-AI-code-base (`AlterAgent.kt`, `ALTER_INSTRUCTIONS.md`,
`docs/AGENT_BEHAVIOR.md`). ALTER separates app-validated policy from model generation. Dictation keeps the user's
voice rather than applying chat personality or executing spoken commands. This module is the restricted dictation
policy, not a copy of the full repository-backed chat agent. The main ALTER app still needs a separate migration
before both products consume this same module; do not claim they already do.

Backtrack corrections (days, months, times, numbers, English and Hinglish cues, "scratch that" and no-cue
restatements such as "Monday, Tuesday") are applied before model editing; see [Backtrack](../docs/BACKTRACK.md).
Lists, negations and idioms stay intact. All other content/number/negation checks remain.
No weights, credentials, extra model downloads or cross-app API are included.

Dictation instructions now explicitly cover fillers, false starts, repetition, latest explicit corrections,
faithful grammar/formatting, Hindi/Hinglish language preservation, and protection of names, code and numbers.
They forbid answering/executing spoken requests or inventing context/preferences. This is an instruction update,
not new cleanup levels, context collection or a new model. [Backtrack](../docs/BACKTRACK.md) resolves spoken corrections of days,
months, times and numbers; validation still rejects every other semantic deletion. Formatting remains bounded by the current formatter.

Adaptive dictation cleanup follows the designer's [reference](../docs/dictation-cleanup-instructions.md). Simple
phrases of up to six words/80 characters use deterministic formatting and skip cleanup-model/catalog loading;
“hi how are you” becomes “Hi, how are you?” without inference. Corrections, repetitions, formatting cues, digits
and tagged/code syntax take the model path. Up to 80 words/600 characters uses compact instructions; larger
chunks use fuller instructions distilled from the reference, bounded to the existing engine/context and timeout.
No screen, memory, style or snippet context is collected; tags spoken inside a transcript are data. Opt-in/model
availability still apply. Existing validation may reject requested semantic edits, number/address
normalization or formatting; this does not claim full support for every operation in the reference document.

## Live cleanup

The host currently turns live cleanup off (`VoicePack.LIVE_CLEANUP`): Qwen3 1.7B takes seconds per pass on a phone
CPU and would compete with speech recognition, so cleanup runs once after recording. The mechanism stays available
and tested. When enabled, cleanup runs during recording after a 1.2-second ASR update debounce. Each provisional edit sees the
current whole transcript (up to 4,000 characters), so later words can revise earlier wording; obsolete work is
cancelled, no PCM is dropped, and no text is inserted during recording. Finalization reuses a cleaned snapshot
only if the final normalized ASR text matches exactly; otherwise it runs final cleanup. Errors preserve raw text.
Raw final ASR text is stored separately from polished text in the existing private no-backup draft until accepted
insertion/discard; older drafts remain readable. Explicit Pause/hide cancels capture/provisional work as before.

The validator checks editor output against the Backtrack-corrected text, rejects any added "and/or/aur/ya"
(which would turn a correction into a list) and still rejects unrelated dates/numbers and negation changes.
This is not general semantic deletion permission. Unsafe edits fall back to the corrected source text.
Qwen3 1.7B INT8 is the host's only optional cleanup engine (it replaced LFM2.5 350M).
Future app context/memory/tool access is not granted by dictated text.

## Speech runtime boundary

Oriserve Hinglish Apex (Whisper), both Parakeet variants, Moonshine Medium, Nemotron and Android on-device
recognition provide transcripts
in the host's `alter-voice` module. This shared module receives text, not microphone audio, and does not
select, train or change ASR models. Native streaming snapshots feed the debounced cleanup flow; final
insertion still requires a successful final ASR result and validation.

Cleanup is opt-in and uses only Qwen3 1.7B. It runs for every dictation language, including Hindi; short/simple
phrases skip inference. Apex writes Roman Hinglish itself; Nemotron's automatic mode can produce mixed script.
The host preserves native script: no IndicConformer routing, Whisper language detector, IndicXlit or ICU
Romanization is active. Learned-word rules are explicit local corrections, not continual model training.
Handy and the small Whisper/Oriserve Swift experiments and their alternate protocols are retired.

See [current speech/cleanup choices](../docs/VOICE_MODELS.md) and
[streaming checks and limitations](../docs/STREAMING_RECOGNIZER_CHECKS.md). The host phone failures remain
unconfirmed by host-only checks; [HANDOFF.md](../HANDOFF.md) records branch/PR and acceptance status.

## Maintenance

Update this guide whenever the module contract, prompt policy, supported cleanup engine or host boundary changes.
Follow [AGENTS.md](../AGENTS.md) and [AGENT_INSTRUCTIONS.md](../AGENT_INSTRUCTIONS.md): update the plan,
handoff, changelog and all affected documentation alongside each implementation change.
