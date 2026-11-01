# Nemotron streaming experiment

Nemotron Q4 is one of the retained on-device recognizers. Open Voice settings → Speech,
download **Nemotron · Q4**, then tap its row to select it. The download is 495,831,520 bytes;
allow additional RAM for inference (catalog estimates: 3 GB minimum device RAM, 6 GB recommended).
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

Cleanup follows the LFM/Alter policy and remains opt-in. Explicit Hindi (`hi-IN`) bypasses model
cleanup. No mode receives script conversion. Short/simple text also skips model inference.

## Phone acceptance still required

Use the same English/Hindi/Hinglish recordings with Nemotron and Android recognition; compare
English separately with Moonshine Medium and both Parakeets.
Measure first visible text, completion delay, word accuracy, self-corrections, RAM and heat.
Test short silence, one-word input, a long recording, pause/resume, hiding during processing,
retry and cancellation. Include names, numbers and notebook/loadbook examples. Keep the
existing preferred model unless this comparison demonstrates an improvement. Host inference
and Android compilation cannot establish phone speed or competitive accuracy.

## Language selection and failure diagnostics

English maps to `en-US`, Hindi to `hi-IN` and Automatic to a null native hint. All use Nemotron itself;
no separate Hindi model, routing download or transliteration is inserted. Selecting Hindi can make
English speech appear phonetically in Hindi script; it is not a request to translate into English.

PR #31 adds native operation/status messages and locale information at start failures, with a short
foreground error Toast. It does not force Automatic to English. Both Android ABI libraries are rebuilt.
The English-only phone report has not been reproduced in host tests. See
[streaming verification](STREAMING_RECOGNIZER_CHECKS.md) and the dated [handoff](../HANDOFF.md)
for current main/PR state. PR #23's test counts above are historical, not the current suite count.
