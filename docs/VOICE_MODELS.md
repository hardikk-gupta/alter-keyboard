# Voice configuration

Settings → ALTER voice offers:

| Speech model | Language | Download |
| --- | --- | --- |
| Parakeet CTC INT8 | English | ~104 MB |
| Parakeet TDT FP16 | English | ~229 MB |
| Nemotron Q4 | Multilingual, native script | ~496 MB |
| Moonshine Medium | English | ~269 MB |
| Android on-device recognition | Installed system languages | No app model download; Android 12+ and provider required |

LFM2.5 350M is the only optional dictation cleanup editor. Its existing conservative instructions,
validation, timeout fallback and explicit-Hindi bypass remain. Short/simple dictation skips model cleanup.
There is no model training, neural/script transliteration, external language detector or automatic engine switch.
Nemotron's own multilingual Automatic mode and Android's own supported language detection remain native features.

All Whisper/Oriserve variants, Moonshine Tiny/Small, Handy, IndicConformer, Whisper LID and IndicXlit
are removed from selection/downloads and execution. ICU4J/full ONNX Java dependencies are removed.
The obsolete Handy/Whisper voice JNI libraries are excluded from packaging; their archived native sources
and license notices remain in the repository. Moonshine retains its own original reduced ONNX runtime;
Parakeet's separately linked sherpa runtime and Nemotron native runtime are unchanged.

## Settings and upgrade behavior

One row per retained speech choice shows name, size and selected state. Tap a downloaded row to select it;
a separate outlined action downloads/cancels/deletes. There are no model explanation paragraphs, duplicate
recommended-model rows, routing bundles or cleanup model picker. Download progress/errors remain visible.
The language selector applies only to Nemotron and Android. Learned-word editing is collapsed.
The shared settings row reserves an icon column only when it has an icon, restoring left alignment.

Kept model downloads and saved selections remain. Invalid retired speech selections no longer load;
Voice settings can select an already-installed retained model. The cleanup engine is always LFM, independent
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
Check both merged ABI directories exclude `libalter_handy.so`, `libalter_whisper.so` and `libonnxruntime4j_jni.so`.
Verify retained catalog file hashes have not changed, retired downloads are removed without affecting drafts,
and removed-model saved audio can be reprocessed without opening the microphone.
The cloud environment lacks the shared signing key, so signed APK/install acceptance remains separate.
