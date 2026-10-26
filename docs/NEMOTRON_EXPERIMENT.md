# Nemotron streaming experiment

This is an opt-in on-device transcription candidate, not a replacement for the selected
Oriserve/Moonshine model. Open Voice settings → All speech models, download **Nemotron 3.5
Streaming · Q4 · experimental**, then select it. The download is 495,831,520 bytes; allow
additional RAM for inference (catalog estimates: 3 GB minimum device RAM, 6 GB recommended).
Android 8+ and a supported arm64-v8a or x86_64 device are required.

## Pipeline

16 kHz mono PCM → Nemotron cache-aware streaming → raw transcript revisions → existing
Alter live cleanup agent → final flush → validated cleanup → text insertion.
A new native session is created for each recording, while the selected model stays loaded.
Audio feeds are at most 100 ms, and the model profile uses right context 3 (320 ms chunks).
These settings do not promise 320 ms end-to-end latency: compute, buffering, model warm-up
and cleanup add time. Recording never waits for model loading. Draft audio follows the
existing interruption/retry flow. Failed finalization must not accept a partial transcript.
Cancellation sets an atomic native abort flag; the owning coroutine releases the session.

Hindi/English recognition is supported by the model; Romanized Hinglish and code switching
quality are unverified. Hindi can be emitted in Devanagari. Custom vocabulary biasing is
not wired for this runtime. Alter correction memory and cleanup still operate downstream.

## Reproduce

The catalog pins the GGUF revision, exact length and SHA-256. Weights download on demand
into private no-backup storage; no weights or network downloads are part of the build.
Run `scripts/build_nemotron_native.sh` with the configured Android SDK to rebuild both
committed CPU-only libraries. Gradle consumes prebuilts and does not invoke CMake/NDK.
Runtime symbols are hidden to avoid collisions with Whisper and Handy's ggml versions.
See `alter-voice/native-nemotron/SOURCE.md` for source provenance and licenses.

## Phone acceptance still required

Use the same English, Hindi and Hinglish recordings with Nemotron, Moonshine and Oriserve.
Measure first visible text, completion delay, word accuracy, self-corrections, RAM and heat.
Test short silence, one-word input, a long recording, pause/resume, hiding during processing,
retry and cancellation. Include names, numbers and notebook/loadbook examples. Keep the
existing preferred model unless this comparison demonstrates an improvement. Host inference
and Android compilation cannot establish phone speed or competitive accuracy.
