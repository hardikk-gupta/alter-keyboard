# Parakeet 110M English experiment

The supplied reference lists NVIDIA's original `parakeet-tdt_ctc-110m` checkpoint and the
sherpa-onnx INT8 CTC conversion of that model. ALTER offers one runnable Android entry,
**Parakeet 110M · INT8 CTC · English**, rather than two duplicate source/deployment entries.
The original checkpoint and conversion are both recorded in [provenance](../licenses/Parakeet-110M-PROVENANCE.md).

## Use

Open ALTER voice → All speech models, download the 104 MB package, then select it explicitly.
Existing selections stay unchanged. This is English-only; use Oriserve/Nemotron for Hindi/Hinglish.
The model runs offline. No weights are bundled in the APK, and no cloud fallback is added.

The verified archive remains in private model storage. Only model.int8.onnx and tokens.txt are
extracted, with additional exact length/hash checks, into private no-backup storage. Extraction
is staged, cancellation cleans the staging directory, and incomplete components are rejected.
Changing/releasing the speech engine deletes extracted copies; selecting again can require
re-extraction. Plan for about 104 MB archive plus 132 MB extracted data and temporary staging.
The existing model downloader's 1 GB free-space buffer remains in effect.

## Configuration and behavior

- sherpa-onnx 2.0.1, INT8 NeMo CTC, CPU, at most two threads.
- 16 kHz mono PCM, 80-dimensional features, zero dither and greedy decoding.
- Offline inference with existing bounded 20-second/one-second-overlap audio windows.
  Periodic previews start after about two seconds of audio when the decoder can keep up;
  cadence adapts to decode cost. This is not cache-aware native streaming.
- PCM capture and draft journaling remain independent of inference. The existing selected
  cleanup editor, correction memory, pause/recovery and final insertion flow still apply.
- Each decode has a fresh native stream, released in a finally block. The loaded recognizer
  is reused and serialized with other speech inference. Native CTC decode has no abort API;
  cancellation is checked before and after each bounded window, so late results are discarded.
  Cancelling waits for the current decode before the recognizer can be released or reused.
- No extra VAD, language routing, FST decoder or unsupported Hindi mode is added.

The static-ORT Android runtime avoids shared ONNX Runtime collisions with Moonshine. Its
prebuilt JAR/JNI entries are pinned by the original AAR checksum; no NDK/CMake is required by Gradle.
ICU transliteration in the separate Nemotron PR does not affect this English-only model.

## Validation and acceptance

Host sherpa-onnx 2.0.1 decoded both WAVs from the exact INT8 package using two CPU threads and
greedy CTC; silence returned empty text and consecutive streams worked. These are host functional
checks, not phone accuracy or latency results. The matching host JNI runtime also passed
a smoke test with the exact Android API JAR, CTC configuration and per-stream release lifecycle.
All 45 voice regressions passed; assembly compiled/dexed and failed only at missing-key signing validation. Compare 10–30 identical English clips with Moonshine
and Nemotron on the phone: cold/warm startup, first text, final tail, names/numbers, long speech,
interruptions, silence, memory, heat and battery. These settings are not a proven best configuration.
