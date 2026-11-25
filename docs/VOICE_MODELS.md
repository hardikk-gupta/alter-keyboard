# Voice configuration

Settings → ALTER voice offers:

| Speech model | Language | Download |
| --- | --- | --- |
| Parakeet Unified 0.6B INT8, 560 ms profile | Streaming English with punctuation | ~663 MB |
| AksharSTT v4 | Hindi/Hinglish, mixed Devanagari and Latin, ARM64 | ~29.5 MB |
| Shruti | Hindi/Hinglish, mixed Devanagari and Latin, ARM64 | ~29.5 MB |
| Oriserve Hinglish Apex | Hindi + English, Roman letters ("kya tum free ho") | ~874 MB |
| Parakeet CTC INT8 | English | ~104 MB |
| Parakeet TDT FP16 | English | ~229 MB |
| Nemotron Q4 | Multilingual, native script | ~496 MB |
| Moonshine Medium | English | ~269 MB |
| Android on-device recognition | Installed system languages | No app model download; Android 12+ and provider required |

Qwen3 1.7B INT8 (~1.75 GB) is the only optional dictation cleanup editor, for English, Hindi and Hinglish; it
replaced LFM2.5 350M, whose languages do not include Hindi. The existing conservative instructions, validation and
timeout fallback remain, and the old rule that skipped cleanup for Hindi is removed. Short/simple dictation skips
model cleanup.
There is no model training, neural/script transliteration, external language detector or automatic engine switch.
Nemotron's own multilingual Automatic mode and Android's own supported language detection remain native features.

The small Whisper/Oriserve Swift variants, Moonshine Tiny/Small, Handy, IndicConformer, Whisper LID and IndicXlit
stay removed (their Hindi was not good enough). ICU4J/full ONNX Java dependencies stay removed. The Handy JNI
library is excluded from packaging; its archived native sources and license notices remain in the repository.
Moonshine retains its own original reduced ONNX runtime; Parakeet retains its separately linked sherpa runtime.
PR #31 updates Nemotron JNI failure reporting without changing decoder weights, architectures or language mapping.

## Oriserve Hinglish Apex and its speed settings

Apex is Oriserve's Whisper large-v3-turbo fine-tune (0.8B parameters, ~700 h of noisy Indian-accented Hindi) that
writes Hindi and English in Roman letters. The app uses the Q8_0 GGML conversion. On Oriserve's published sample
clips the FP16 file gave the same text at about twice the CPU time, so FP16 is not offered.

- Language is fixed to Hindi, as Oriserve recommends; "auto" adds a detection pass and gave worse output.
- One greedy pass with flash attention and no temperature fallback: about half the time of the old settings with
  the same text on the sample clips (host measurement).
- The encoder always gets the full 30 s window. Shorter windows (`audio_ctx`) made this model loop on tokens.
- Saved recordings decode in 28 s windows (1 s overlap) to use each encoder pass fully.
- arm64-v8a ships three builds of the bridge: ARMv8.0 baseline, ARMv8.2 dotprod+fp16, and dotprod+fp16+i8mm.
  `WhisperNative` reads `/proc/cpuinfo` and loads the best one. Before this, the only build had no int8
  dot-product instructions, which made Whisper slow on every phone.

Each dictation therefore costs at least one full encoder pass, whatever its length. Phone latency has not been
measured yet; check it on the phone before relying on Apex for long dictation.

## Settings and upgrade behavior

One row per retained speech choice shows name, size and selected state. Tap a downloaded row to select it;
a separate outlined action downloads/cancels/deletes. There are no model explanation paragraphs, duplicate
recommended-model rows, routing bundles or cleanup model picker. Download progress/errors remain visible.
The language selector applies only to Nemotron and Android. Learned-word editing is collapsed.
The shared settings row reserves an icon column only when it has an icon, restoring left alignment.

Kept model downloads and saved selections remain. Invalid retired speech selections no longer load;
Voice settings can select an already-installed retained model. The cleanup engine is always Qwen3, independent
of a retired Handy preference. Retired files/partial downloads are removed under idle setup ownership when
Voice settings opens. An active dictation defers that cleanup. This never deletes the private dictation draft.

Old interrupted PCM, including an automatic-routing draft, can be explicitly processed using a selected
remaining downloaded recognizer. Resuming capture still requires the matching session identity, so unrelated
recording policies cannot be appended together. Completed text remains recoverable without the old model.

## Android recognition sound

A successful final result now destroys the recognizer without an extra `cancel()`; cancellation is kept for
actual interruption. ALTER adds no recording tone. The provider may play its own start/end beep, and the public
SpeechRecognizer API has no supported per-app mute setting. Do not globally mute media, notifications or system
sounds, and do not promise silence based on host compilation. Provider behavior requires phone testing.

## Validation

Run `./gradlew --continue assembleStableDebug :alter-voice:testDebugUnitTest`.
Check both merged ABI directories exclude `libalter_handy.so` and `libonnxruntime4j_jni.so`, and that arm64-v8a
contains `libalter_whisper.so`, `libalter_whisper_dotprod.so` and `libalter_whisper_i8mm.so`.
Verify retained catalog file hashes have not changed, retired downloads are removed without affecting drafts,
and removed-model saved audio can be reprocessed without opening the microphone.
The cloud environment lacks the shared signing key, so signed APK/install acceptance remains separate.

## Repository handoff

[HANDOFF.md](../HANDOFF.md) records the dated main/PR state, actual validation and open phone issues;
[PLAN.md](../PLAN.md) records intended work and remaining actions. Streaming implementation details are in
[STREAMING_RECOGNIZER_CHECKS.md](STREAMING_RECOGNIZER_CHECKS.md). Do not treat an open PR's host checks
as a merged build or a confirmed device fix.


## November 25 streaming models and validation

Choose **Parakeet Unified · English** for English or **Akshar v4 · Hindi / Hinglish** on ARM64 for mixed script. Shruti is an alternative on the same runtime. Apex remains the Roman Hinglish choice. Existing selections and saved recording identities survive the update.

The Unified decoder reads the encoder, decoder, joiner and token files directly from their verified private locations. The 560 ms label identifies the export profile. sherpa-onnx 2.0.3 includes the buffered-context and endpoint changes from [PR 4133](https://github.com/k2-fsa/sherpa-onnx/pull/4133). The decoder keeps committed segments across silence and drains the final partial window. [Upstream export](https://huggingface.co/csukuangfj2/sherpa-onnx-nemo-parakeet-unified-en-0.6b-int8-streaming-560ms)

Akshar and Shruti use the Android ARM64 static Needle engine from revision `d6c7d6a2e9655505e0f8e23339356dd1705900eb`. The JNI bridge batches 16 kHz mono PCM into one-second chunks and flushes the remaining samples on stop. Only committed words persist across updates. The Kotlin session uses the local C API directly. Rebuild the ARM64 libraries with `scripts/build_needle_native.sh`. [Akshar model card](https://huggingface.co/byteknotlabs/AksharSTT-V4) · [Shruti model card](https://huggingface.co/udgamlabs/Shruti)

**Host evidence.** All six new catalog files matched their pinned sizes and SHA-256 digests. The compiled Kotlin adapters produced nonempty, repeatable final text in consecutive sessions with each model. Parakeet also transcribed both copies of a sentence separated by three seconds of silence, retaining its final word both times. On Oriserve’s 2.66-second sample ending in `3227568.wav`, both Hindi models wrote “rate” in Latin script where the published sample transcript says “dekh”. This clip alone cannot establish a WER ranking. [Sample and transcript](https://huggingface.co/Oriserve/Whisper-Hindi2Hinglish-Apex)

**Nothing Phone (2).** No physical device was connected. Airplane-mode capture, first partial, stop-to-final latency, WER on representative recordings, peak RAM and sustained thermal performance remain unmeasured. The 560 ms context and one-second batch sizes are configuration values.
