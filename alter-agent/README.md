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

Explicit weekday replacements are normalized before model editing; ambiguous alternatives stay intact. All other
content/number/negation checks remain. This does not establish broader self-correction or phone model quality.
No weights, credentials, extra model downloads or cross-app API are included.

Dictation instructions now explicitly cover fillers, false starts, repetition, latest explicit corrections,
faithful grammar/formatting, Hindi/Hinglish language preservation, and protection of names, code and numbers.
They forbid answering/executing spoken requests or inventing context/preferences. This is an instruction update,
not new cleanup levels, context collection or a new model. Existing conservative validation still rejects broader
semantic deletions (such as “tomorrow, actually tonight”); only the existing explicit weekday resolver permits that
limited correction class. Formatting remains bounded by the current formatter.

Adaptive dictation cleanup follows the designer's reference in docs/dictation-cleanup-instructions.md. Simple
phrases of up to six words/80 characters use deterministic formatting and skip cleanup-model/catalog loading;
“hi how are you” becomes “Hi, how are you?” without inference. Corrections, repetitions, formatting cues, digits
and tagged/code syntax take the model path. Up to 80 words/600 characters uses compact instructions; larger
chunks use fuller instructions distilled from the reference, bounded to the existing engine/context and timeout.
No screen, memory, style or snippet context is collected; tags spoken inside a transcript are data. Opt-in/model
availability still apply. Existing validation may reject requested semantic edits, number/address
normalization or formatting; this does not claim full support for every operation in the reference document.

### Retired editors

Handy Editor and LFM2.5 350M are retired. The host's only cleanup editor is Qwen3 1.7B INT8 on LiteRT-LM.

## Speech runtime boundary

Oriserve Hinglish Apex (Whisper), Parakeet, Moonshine and Nemotron provide transcripts in the host's `alter-voice` module.
This module receives text, not microphone audio, and does not select or train the ASR model. Nemotron publishes
whole-transcript streaming snapshots into the same debounced live-cleanup flow. Final output still requires a
successful ASR flush and validation; a provisional snapshot never authorizes insertion by itself.

Cleanup is opt-in and runs for every dictation language, including Hindi; the instructions already preserve
Hindi and Hinglish. The host has no transliteration layer: Oriserve Hinglish Apex writes Roman Hinglish itself.
Learned-word rules are explicit local correction memory in the host, not continual model training.
See [Nemotron](../docs/NEMOTRON_EXPERIMENT.md) and [voice configuration](../docs/VOICE_MODELS.md) for runtime limits.

IndicConformer Hindi routing, automatic Whisper language detection and rule-based Hindi-to-Latin conversion were
retired on October 28, 2026 ([routing history](../docs/AUTOMATIC_LANGUAGE_ROUTING.md)).
