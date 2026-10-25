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
limited correction class. Formatting remains bounded by the current formatter. Hindi's model bypass is unchanged.

Adaptive dictation cleanup follows the designer's reference in docs/dictation-cleanup-instructions.md. Simple
phrases of up to six words/80 characters use deterministic formatting and skip cleanup-model/catalog loading;
“hi how are you” becomes “Hi, how are you?” without inference. Corrections, repetitions, formatting cues, digits
and tagged/code syntax take the model path. Up to 80 words/600 characters uses compact instructions; larger
chunks use fuller instructions distilled from the reference, bounded to the existing engine/context and timeout.
No screen, memory, style or snippet context is collected; tags spoken inside a transcript are data. Opt-in/model
availability and Hindi bypass still apply. Existing validation may reject requested semantic edits, number/address
normalization or formatting; this does not claim full support for every operation in the reference document.
