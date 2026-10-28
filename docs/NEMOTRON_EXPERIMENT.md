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

Hindi/English recognition is supported by the model; conversational Hinglish spelling and code switching
quality are unverified. Native Hindi output can use Devanagari. Custom vocabulary biasing is
not wired for this runtime. Native output is passed to Alter cleanup without ICU or IndicXlit.
No language detector or external Hindi engine is inserted. Hindi may therefore remain Devanagari;
Roman-Hindi output is not promised. Nemotron weights are unchanged. See [current voice configuration](VOICE_MODELS.md).

## Reproduce

The catalog pins the GGUF revision, exact length and SHA-256. Weights download on demand
into private no-backup storage; no weights or network downloads are part of the build.
Run `scripts/build_nemotron_native.sh` with the configured Android SDK to rebuild both
committed CPU-only libraries. Gradle consumes prebuilts and does not invoke CMake/NDK.
Runtime symbols are hidden to avoid collisions with Whisper and Handy's ggml versions.
See [native provenance](../alter-voice/native-nemotron/SOURCE.md) and
[model provenance](../licenses/Nemotron-3.5-PROVENANCE.md) for source pins and licenses.

## Checks completed for PR #23

- Both committed Android CPU libraries were rebuilt; ELF load segments use 16 KB alignment,
  and only JNI bridge symbols are exported. All 400 vendored files match the upstream pin.
- The production JNI bridge ran public English JFK and Oriserve Hindi audio on the host,
  producing partial and final output; cancellation and a fresh subsequent session passed.
  Hindi was tested with both `hi-IN` and automatic selection. Recognition was imperfect:
  the Hindi-hinted result began “वस”, and automatic mode began “Vaz” before Devanagari text.
  This is functional verification, not an accuracy benchmark.
- The downloaded GGUF matched the catalog's exact size and SHA-256.
- All 39 voice unit tests passed. Android compilation/dexing completed; APK signing failed
  because this environment lacks the shared keystore. No signed install/signature check ran.

Cleanup follows the selected editor's policy and remains opt-in. Explicit Hindi mode
(`hi-IN`) bypasses model cleanup but still receives script conversion. Automatic mode also receives
script conversion for Devanagari runs. Other non-Latin scripts are not converted.

## Phone acceptance still required

Use the same English, Hindi and Hinglish recordings with Nemotron, Moonshine and Oriserve.
Measure first visible text, completion delay, word accuracy, self-corrections, RAM and heat.
Test short silence, one-word input, a long recording, pause/resume, hiding during processing,
retry and cancellation. Include names, numbers and notebook/loadbook examples. Keep the
existing preferred model unless this comparison demonstrates an improvement. Host inference
and Android compilation cannot establish phone speed or competitive accuracy.

## Explicit Hindi mode

The Hindi-mode routing update uses the separately downloaded IndicConformer Hindi recognizer
when Nemotron is the saved base choice and Dictation language is Hindi. Automatic mode keeps
Nemotron's native multilingual decoder. See [the shared Hindi pipeline](INDIC_HINDI_PIPELINE.md).
