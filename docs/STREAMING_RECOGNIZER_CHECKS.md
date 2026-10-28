# Moonshine and Nemotron failure diagnostics

The keyboard previously displayed only an error icon. Foreground failures now also show a short Android
Toast with the controller's error message, while retaining the pill, accessibility description and saved draft.
No permanent model explanations or new settings rows are added. Password/disabled surfaces do not show it.

## Moonshine Medium

Use the existing SDK 0.1.5 JNI API directly rather than its `Transcriber` convenience class. That class
creates a default stream when loading, while the keyboard created another stream; it also discards native
start/feed/stop return codes. The keyboard previously stopped a successful stream again in `finally`.

The checked adapter loads the same verified eight components and opens one fresh stream per recording.
It checks native start/feed/stop status, preserves all PCM feeds, polls complete transcript snapshots,
flushes exactly once, and frees the stream on success, failure or cancellation. Full snapshots avoid the
convenience listener's cross-session completed-line deduplication. Saved-audio decoding remains available.
Preview work keeps a 0.5-second audio floor and adapts to processing cost up to five seconds.
These changes address concrete integration weaknesses; they do not prove the unseen phone error's cause.

## Nemotron

Language mapping remains English → `en-US`, Hindi → `hi-IN`, Automatic → null native hint. No Hindi
routing helper, transliteration, model replacement or silent English fallback is introduced.
The JNI adapter now preserves the native error reason instead of returning a generic false/null for
stream setup/feed/finalization failures. A failed start includes the requested locale.

Host tests of the exact pinned model and compiled adapter produced nonempty results for English and Hindi
audio under all three modes. English under Automatic matched English mode for the sample. Hindi under
Automatic began `Vaz दिन…`, while the Hindi hint began `वस दिन…`; this is imperfect recognition, not a
measured phone accuracy score. We did not reproduce the reported English-only behavior on the host.
The new foreground message is needed to locate that phone-specific failure if it persists.

## Checks and reproduction

- Unit suite: 53 tests, including feed preservation, final flush, start/feed/stop failures and cancellation.
- Verify all pinned Moonshine Medium components and Nemotron GGUF length/SHA before testing.
- `scripts/StreamingSpeechHostProbe.java` exercises the actual compiled Kotlin classes through JNI:
  Moonshine repeated fresh streams, keyterms and saved decoding; Nemotron English/Automatic/Hindi on
  both samples, and deliberate unsupported-language failure reporting followed by successful reuse.
- For the host-only Moonshine bridge, use upstream SDK v0.1.5 commit
  `234f60faa0eb388b01cdf7e60aca232af37aefda`'s `moonshine-jni.cpp`, C API header and UTF-8 headers,
  linked to the matching `moonshine-voice==0.1.5` host library. Replace only Android logging macros with
  host stderr macros. Native algorithms/weights and JNI object conversion stay upstream.
- Build the host Nemotron JNI bridge from `alter-voice/native-nemotron/alter_nemotron.cpp` linked to the
  vendored transcribe runtime. Expose it as `libalter_nemotron.so` in the host Java library path.
- Compile the Java probe with the voice/agent debug Kotlin classes, SDK 0.1.5 classes JAR, Kotlin stdlib
  2.4.0, coroutines 1.8.1 and SDK android.jar; run with args: Moonshine directory, Nemotron GGUF,
  English mono PCM16 16kHz WAV, Hindi mono PCM16 16kHz WAV. No host inference tools run in Gradle.
- Rebuild arm64-v8a and x86_64 via `scripts/build_nemotron_native.sh`; Gradle still uses prebuilt binaries.

Host success does not verify ARM phone memory, device audio capture, Hindi/Hinglish accuracy or latency.
The local shared signing key is unavailable, so final APK signing/install acceptance remains separate.
