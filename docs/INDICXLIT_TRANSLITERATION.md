# IndicXlit Hindi-to-Roman conversion

> Retired from the app on October 28, 2026. This document is historical; this model/helper is no longer
> available or used. See [current voice configuration](VOICE_MODELS.md).


Nemotron remains the multilingual speech recognizer. Download **IndicXlit · Hindi to Roman**
in Settings → ALTER voice once (47,421,034 bytes, approximately 47 MB). It is then used automatically
for Hindi text from Nemotron's live revisions and final result, and the downloaded recognizers'
bounded-window results. It does not change the selected speech model or language routing.
English/Latin text, capitalization, URLs and identifiers bypass the neural converter unchanged.
Android system speech recognition is not part of this downloaded-recognizer integration.

Pipeline: audio → selected/routed recognizer → IndicXlit Hindi word conversion → Alter cleanup → text.
All inference runs offline. Nothing is sent to an AI4Bharat server. This uses the official pretrained
Indic-to-Roman v1.0 weights, not the Roman-to-Indic model and not a new fine-tune.

The actual app decoder with the exported weights converts:

```
Baby मैं घर जा रहा हूँ, then I'll call you.
Baby main ghar ja raha hun, then I'll call you.
```

The neural model itself produces `jaa`, `rahaa`, and `huun`. Narrow spelling preferences for the
source words `जा`, `रहा`, and `हूँ`/`हूं` produce `ja`, `raha`, and `hun`; they never shorten arbitrary
names or rewrite whole sentences. Additional user-requested preferences map the exact words
`बेबी`, `चाहिए`, `कॉल`, `यू`, `आइल`, and `देन` to `baby`, `chahiye`, `call`, `you`, `ail`, and `dena`
when the model returns their known alternate spellings. These are preferences after inference, not
claims that the raw pretrained model produces those spellings. Extra recognized words remain: `मैं बस घर` becomes `main buss ghar`.
If Nemotron's raw text already contains an incorrect word, transliteration cannot infer the lost audio.
If the recognizer already returns Roman text such as `mem`, this converter leaves that Latin text alone.

## Model and runtime

- AI4Bharat IndicXlit v1.0, approximately 11M parameters; MIT code and weights.
- FP32 encoder and decoder exported from the official trusted Fairseq checkpoint, plus vocabulary.
  Model weights are hosted in the dedicated `indicxlit-v1.0-onnx-1` GitHub release, never in Git or the APK.
- Three pinned size/SHA-256 downloads through the existing resumable private model store; each is
  reverified before opening native inference. Partial downloads do not enable the neural path.
- ONNX Runtime Android 1.23.2, CPU, two intra-op threads, one inter-op thread.
- Four-beam word decoding, at most 64 normalized input characters and 48 output steps. The app's
  constrained decoding is not an assertion of bit-for-bit parity with upstream Fairseq beam search.
- Up to 1,024 word results cached in memory and reused across live revisions; no persistent training data.
  Normalize decomposed Hindi nukta forms and remove joiners before tokenization.
- Cancellation checks between decoder calls and before publishing a conversion. Overlong/unsupported
  words and incomplete inference raise an actionable error, preserving the recording rather than truncating text.
- Without the complete download, the existing basic ICU conversion remains available, as stated in settings.
  Delete the IndicXlit row to return to ICU; deletion is guarded by the existing speech setup owner,
  releases native sessions/caches, and retains the independent recognizer downloads.

Moonshine 0.1.5 contains a reduced ONNX Runtime 1.23.2. A Gradle artifact transform removes only that
AAR's redundant `libonnxruntime.so`; the official full 1.23.2 runtime supplies the single shared core.
Moonshine's own JNI libraries/classes stay unchanged. No `pickFirst`, NDK/CMake or Python build step is used.
The official ORT core and Java JNI libraries have 16 KB ELF load alignment on arm64.

## Reproducing the host checks

`licenses/IndicXlit-PROVENANCE.md` records source hashes and output hashes. The exporter is a separate,
host-only tool; install CPU torch 2.5.1, onnx 1.17.0, and onnxruntime 1.20.1 in an isolated environment.
Download and extract the official release archive, then run:

```
python scripts/export_indicxlit.py EXTRACTED_CHECKPOINT OUTPUT_DIRECTORY
```

The exporter rejects an unexpected checkpoint before loading pickle, copies all learned FP32 weights,
checks vocabulary sizes, and verifies ONNX/eager tensor parity for four words and three prefix lengths.
The manual `Publish IndicXlit model assets` workflow reproduces the export, verifies exact catalog
bytes, and uploads the dedicated model release using the repository token, independently of APK signing.
It also emits `source.txt` and `target.txt` for the host probe; only the two graphs and `vocab.json` are
app downloads. To probe the actual compiled Kotlin converter, compile/run `scripts/IndicXlitHostProbe.java`
with the voice module's debug Kotlin classes, ONNX Runtime Java 1.23.2 host JAR, Kotlin stdlib 2.4.0,
coroutines 1.8.1, ICU4J 76.1 and the SDK android.jar on the classpath. Pass the output directory as its argument.

Validation in this cloud environment:

- 66 voice unit tests passed, including Hindi boundaries, preserved English/numbers/extra words and spelling scope.
- Actual Kotlin/ORT host probe passed the full sample, cached repeat, extra-word retention, cancellation,
  and overlong-word error. First uncached sample was about 544 ms on this host; cached repeat below 1 ms.
  These are not Android measurements or accuracy benchmarks.
- Android compilation, dex and native merging passed. Both packaged ABIs contain byte-identical official
  ORT 1.23.2 core/JNI files. Full `assembleStableDebug` stops at the unavailable shared signing key.

Phone memory use, mixed-language accuracy, capture/preview latency, Moonshine coexistence and installation
still require real-device acceptance. No claim that word transliteration repairs ASR errors or matches Wispr Flow.
