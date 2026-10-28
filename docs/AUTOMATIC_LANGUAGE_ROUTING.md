# Automatic English/Hindi routing for both Parakeet models

## Set up once

1. Select **Parakeet 110M INT8 CTC** or **Parakeet 110M TDT FP16** as the English model.
2. Leave Dictation language on **Automatic**.
3. Download **Automatic Hindi/English routing** in ALTER voice. Its combined size is ~240 MB:
   Whisper Tiny language-identification encoder/decoder (~103 MB) plus IndicConformer Hindi
   graph/vocabulary (~138 MB). Existing verified Hindi files are reused, so the new download
   is ~103 MB if Hindi is already installed. The detector is not a selectable transcription model.
4. For mixed/uncertain speech, also download **Nemotron 3.5 Streaming Q4** under All speech models.
   It is optional for clearly detected English/Hindi and reused if already installed.

No per-utterance language change is required. English uses the exact selected CTC/TDT decoder;
Hindi uses the separate IndicConformer recognizer. Hindi-script output uses the existing offline
ICU Romanization. This does not train or add Hindi weights to Parakeet. Explicit English remains
English-only without the routing downloads. Explicit Hindi mode keeps the previous direct Hindi
routing. Nemotron selected as the primary recognizer retains its native multilingual streaming
Automatic mode and does not require the extra language detector.

## What Sayso does and what we adapted

Reference: [Sayso EarlyLidRouter](https://github.com/stevengonsalvez/sayso/blob/559429e52f41beaa2e3fd698c208551116b40617/app/src/main/kotlin/ai/sayso/dictation/models/EarlyLidRouter.kt),
commit `559429e52f41beaa2e3fd698c208551116b40617`, MIT (license retained in `licenses/Sayso-MIT.txt`).
Sayso uses sherpa-onnx's Whisper spoken-language-identification API on early audio, then selects
Parakeet or a dedicated IndicConformer. Its catalog labels routing highly experimental. It is
not a demonstrated word-level language-switching or natural Hinglish system. Its 0.95 confidence
value is assigned in code, not a measured posterior from that API; ALTER does not copy that score.

Our implementation uses the same public sherpa LID API with separate, revision/size/hash-pinned
multilingual Whisper Tiny INT8 ONNX files. CPU, up to two threads, tail padding zero; no token
file or transcription through Whisper Tiny is needed. The existing Whisper GGML speech download
is a different artifact and cannot substitute for these ONNX detector files.

## Decision and audio behavior

- The existing bounded-window preview scheduler supplies full 16 kHz audio windows, up to
  20 seconds with one-second overlap. Capture and PCM journaling continue independently.
- Classification uses copied first-three-second audio; at six seconds or longer it also probes
  the last three seconds. Silent probes are skipped; clips under half a second are uncertain.
  It does not use pitch/energy to infer language. The API supplies labels, not probabilities.
- English labels keep the selected Parakeet. Hindi labels choose IndicConformer. Conflicting
  English/Hindi labels suggest mixed/changing language and use installed Nemotron. Without
  Nemotron, an error preserves the draft and asks for the missing download.
- Unknown/unsupported/short input uses installed Nemotron when available; otherwise it retains
  the configured English recognizer with an uncertain-route notice. That fallback can misrecognize
  short Hindi speech. It is not reported as confidently English.
- Classification never consumes the decoder's prefix or edits the recorded samples. The complete
  window is passed to the selected recognizer. Routing is reassessed on later previews/windows;
  intermediate previews may change. Finalization still requires a successful full final result.
- Only one ASR decoder is active. The small detector stays cached separately; private unpacked/
  adapted speech files are retained across automatic route changes to avoid repeated extraction.
  Explicit model selection, release or deletion closes engines and clears derived caches.
- LID inference has no native abort entry point; cancellation is checked before/after each bounded
  probe. No late decision is published. TDT/Nemotron retain native aborts; CTC waits for its bounded
  decode. Model switches and detection add processing time, measured by the adaptive preview cadence.

## Settings, errors and recovery

The model row identifies the saved English model, with a separate automatic-routing download row
and last-route notice. Missing required detector/Hindi files prevent starting automatic Parakeet
rather than pretending Hindi routing is enabled. Deleting the bundle removes only the detector,
retaining the independent Hindi model and English choice; Automatic then requires re-download.

Automatic drafts store the base model plus routing-policy identity (`auto-en-hi:<base-id>`), not a
single per-window recognizer ID. Explicit-English and direct-Hindi drafts remain distinct. Changing
language/model while active is guarded, and selection is revalidated after waiting for setup ownership.
Resuming an Automatic draft requires Automatic plus the same base choice. Older plain-Parakeet
saved drafts use explicit English to preserve their original recognition policy. No recording history,
training, cloud routing or automatic insertion of recovered text is added.

## Limits and validation

This is dominant-language routing, not reliable word-level code-switching detection. A short Hindi
word inside an English phrase, language changes between probes, or classifier mistakes can be
missed. Nemotron helps with detected mixed/uncertain cases, but natural Hinglish and phone accuracy
are unverified. Rule-based Romanization still uses phonetic spellings, not an IndicXlit transformer.

Exact INT8 detector files matched all pinned hashes/sizes. Host sherpa-onnx and the exact Android
API JAR with matching host JNI identified real English and Hindi three-second samples as en/hi.
These are functional smoke tests, not a confidence/accuracy rate or phone latency benchmark.
All 62 combined voice tests passed, including both English decoders, Hindi routing, mixed/unknown/
short/silent cases, copied-probe PCM preservation, cancellation and routing-policy draft recovery.
Android compilation/dexing passed; assembly failed only at missing shared-key signing validation.
No substitute signing key was created.

Phone acceptance still needs 10–30 English/Hindi/Hinglish samples, short words, language changes
within/across windows, silence, missing/deleted downloads, pause/hide/calls, saved retry, last-route
labels, and cold/warm latency, RAM, battery and thermals. No phone installation or accuracy claim
is made from the host tests.
