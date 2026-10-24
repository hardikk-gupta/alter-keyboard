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
