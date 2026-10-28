# Parakeet 110M English experiment

The supplied reference describes NVIDIA's hybrid `parakeet-tdt_ctc-110m` checkpoint and its
sherpa-onnx INT8 CTC conversion. ALTER now offers two runnable English variants from that base:

| Choice | Download | Decoder/runtime |
| --- | --- | --- |
| Parakeet 110M · INT8 CTC · English | 104 MB archive | INT8 ONNX, sherpa-onnx NeMo CTC |
| Parakeet 110M · TDT FP16 · English | 229 MB GGUF | FP16 TDT/RNN-T, transcribe.cpp |

The original source checkpoint is provenance, not another trained model. These are distinct
runtime/precision/decoder choices. Compare on the same English audio; neither is proven more
accurate in our app. Provenance: [CTC](../licenses/Parakeet-110M-PROVENANCE.md) and
[TDT](../licenses/Parakeet-TDT-PROVENANCE.md).

## Use

Open ALTER voice → All speech models, download either variant, then select it explicitly.
Existing selections stay unchanged. This is English-only; use Oriserve/Nemotron for Hindi/Hinglish.
The model runs offline. No weights are bundled in the APK, and no cloud fallback is added.

For CTC, the verified archive remains in private model storage. Only model.int8.onnx and tokens.txt are
extracted, with additional exact length/hash checks, into private no-backup storage. Extraction
is staged, cancellation cleans the staging directory, and incomplete components are rejected.
Releasing or explicitly changing the selected model deletes extracted copies; automatic
route changes retain them. Selecting again after release can require
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
- No extra VAD or FST decoder is added. A separate language-routing layer can choose the Hindi engine.

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

## FP16 TDT configuration and validation

The TDT model uses the existing pinned transcribe.cpp CPU library with a separate offline JNI
entry point. The GGUF downloads directly into verified private model storage; it needs no archive
extraction or extra runtime dependency. It uses up to two CPU threads, a fresh native session per
window, default greedy TDT decoding and no timestamps. It uses the same bounded 20-second windows
and adaptive periodic preview scheduler as CTC, and supports saved-audio retry. It is not native
streaming and does not add Hindi/Hinglish recognition or automatic fallback.

The model card reports 2.43% WER on LibriSpeech test-clean for FP16, matching its FP32 reference.
FP16 is chosen to avoid the extra FP32 download/RAM while retaining the reported reference WER;
this does not establish a phone accuracy or speed advantage over CTC. Benchmark both on the phone.

The production JNI source passed host FP16 inference on both packaged English clips, fresh-session
reuse, invalid-size input rejection, rejection by Nemotron's streaming loader, in-flight cancellation
and successful inference after cancellation. These checks establish the adapter works on the host,
not on-device performance. Both committed Android ABI libraries were rebuilt. All 45 voice unit
tests passed; Android compilation/dexing passed and signing failed only at the missing shared key.
No alternate signing key was generated. Cancellation is checked natively and by the caller; failed
or cancelled native runs return no output and preserve the private draft for retry.

## Hindi mode

Both Parakeet decoders remain English-only. With the IndicConformer Hindi download installed,
explicit Hindi mode uses that separate recognizer and rule-based Romanization while remembering
the Parakeet choice for English/Automatic mode. Parakeet's learned weights remain unchanged. [Automatic language routing](AUTOMATIC_LANGUAGE_ROUTING.md)
now adds a separately downloaded language detector for either variant. See also
[Hindi pipeline](INDIC_HINDI_PIPELINE.md).
