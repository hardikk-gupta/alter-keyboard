# Experimental Handy live cleanup

ASR remains the user's selected model (including Oriserve Hinglish Swift FP16 or experimental Nemotron streaming). Handy is a separate, explicitly
selected cleanup editor under ALTER voice → Dictation cleanup → Cleanup editor. Download its 229 MB GGUF and
turn cleanup on. Existing LiteRT cleanup remains available. The runtime and model are CPU-only/offline; no cloud
fallback or tools/memories are attached. The main ALTER application is not modified by this keyboard PR.

## Flow

ASR partial updates → 1.2-second debounce → provisional whole-transcript cleanup → validated snapshot.
Final ASR text → reuse/wait for an exactly matching snapshot, or cancel stale work and clean the final transcript.
Only the final result is eligible for insertion/clipboard delivery. Capture journals PCM independently of cleanup.
No audio is dropped. Final raw ASR and polished text are separate fields of the existing private no-backup draft;
accepted insertion or explicit discard removes them with that draft. There is no new recording history.

Live previews are bounded to 4,000 characters. Longer final text uses the existing chunking, eight-second overall
cleanup budget and full-tail retention. Native edits have a local 7.5-second timeout, token/context/output limits,
aborts and no partial-token-budget result. External cancellation is preserved; local timeout is a normal fallback.

## Model contract and validation

Pinned model: MagicNoThief/handy-editor-lfm2.5-350m, revision f1f01e16ab10143f51ffb0fb180ac715dcef964e.
The trained llama.cpp input is ChatML with an explicit empty system turn and no generic instructions/examples.
Transcript bytes are tokenized without recognizing special tokens, so transcript text cannot inject ChatML roles.
Each edit uses a fresh context and greedy decoding; model weights are cached, serialized and released on editor
switch/delete. The JNI runtime uses two CPU threads and hides llama/ggml symbols from the other native runtimes.
This does not guarantee a phone latency or thermal result.

ALTER resolves narrowly supported weekday retractions before editing/validation, including “Friday. Actually, make
it Monday” and “Friday, no wait, Monday”. Negated and quoted/code-like cases remain conservative. Invalid model
edits fall back to source text with only supported deterministic formatting/corrections. Unrelated dates, numbers,
addresses and English/Roman-Hindi negations are protected. This is not general semantic-deletion permission.
The stock editor still receives ALTER's instructions/examples; Handy uses its trained protocol instead.

## Rebuilding

Normal Gradle builds consume committed arm64-v8a/x86_64 binaries and need no NDK/CMake. Native source edits require
NDK 28.2.13676358, CMake 3.22.1 and `ANDROID_HOME=... scripts/build_handy_native.sh`. The regular
`scripts/build_native_libs.sh` also rebuilds Handy. Source pin/license: [native source](../alter-voice/native-handy/SOURCE.md) and
[model provenance](../licenses/Handy-Editor-PROVENANCE.md).
Both ELF libraries use 16 KB load alignment; bundled llama/ggml symbols are not exported.

## Checks performed for this implementation

- Full GGUF download: exact size and SHA-256 matched the pinned catalog and LFS metadata.
- Built both Android ABIs from the vendored source, without changing the existing Whisper/typing binaries.
- Host build of the same JNI source: “Friday no wait Monday” and “Friday actually make it Monday” became Monday;
  an already-correct notebook sentence stayed unchanged; a request for a poem remained dictated text.
- Native abort followed by another edit passed. A token-budget-truncated edit returned no result.
- JVM regressions cover revising earlier provisional words, changed-final cancellation, completed and in-flight
  exact-final reuse, raw-text preservation, and conservative correction/negation acceptance.

The host Hinglish kal/parso sample did not fully resolve the correction and removed a repeated “nahi”. The
strengthened validator rejects that negation removal and retains source text. Handy is English-trained and remains
experimental for Hinglish. Host timings varied substantially under concurrent build load and are not phone data.
No claim of reliable Hinglish semantic cleanup or Nothing Phone (2) latency is made.

## Phone acceptance still needed

Download/select Handy; retain Oriserve FP16 as ASR. Dictate the Friday→Monday case with the correction after a pause,
then repeat with Hinglish, names/numbers, already-correct text and longer dictation. Confirm no insertion before
Finish, correct final insertion, pause/hide/call recovery and discard behavior. Compare cold and warm end-to-end
latency with cleanup disabled/stock/Handy, plus memory, battery and thermals. Verify the installed APK signature.
