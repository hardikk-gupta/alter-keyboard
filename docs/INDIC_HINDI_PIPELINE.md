# Hindi recognition and Roman-letter output

## What runs

Hindi audio → AI4Bharat IndicConformer Hindi (community INT8 ONNX export) → Devanagari transcript
→ downloaded IndicXlit (basic ICU when absent) → optional selected cleanup → text field.

This is an additional Hindi recognizer, not a fine-tune or Hindi capability inside Parakeet.
Both Parakeet variants remain English-only. Explicit Hindi mode routes the selected Parakeet INT8
CTC, Parakeet FP16 TDT or Nemotron choice through the downloaded Hindi engine. English/Automatic
mode restores the saved base choice. Automatic mode with either Parakeet now uses the
[automatic language-routing package](AUTOMATIC_LANGUAGE_ROUTING.md); Explicit English stays single-engine.
Nemotron's native Hindi support remains available in Automatic mode.

## Setup and recovery

1. Open ALTER voice → All speech models and download **IndicConformer Hindi · INT8** (~138 MB,
   model plus vocabulary). Both components must complete and pass verification.
2. Keep/select the preferred Parakeet or Nemotron choice, then set Dictation language to Hindi.
   The current-model row shows IndicConformer and a hint explains the routing. If Hindi files are
   missing, the setup card recommends that download; it does not silently choose another recognizer.
3. Dictate normally. Hindi-script output is converted to English letters before live/final delivery.
4. Switch to English to use the saved base model directly, or Automatic for the separate
   language-detector routing package with either Parakeet. Nemotron Automatic remains natively multilingual.

IndicConformer can also be selected directly as a Hindi-only recognizer. Whisper/Oriserve,
Moonshine and Android device recognition are not redirected. Model-based outputs share script
conversion, preserving any already-Latin content. Device recognition remains on its existing path.
Changing language during active recording/processing is rejected with an error. Drafts record the
actual effective recognizer ID; retry requires that same recognizer. Hiding/pausing still preserves
PCM in the existing private draft. Deleting an effective Hindi model releases its cache while
preserving the saved base choice, and requires a re-download before Hindi mode can be used again.

## Configuration

- 16 kHz mono PCM, 80-dimensional NeMo features, per-feature normalization, zero dither.
- INT8 CTC on the existing sherpa-onnx 2.0.1 static-ORT runtime, up to two CPU threads, greedy decode.
- Bounded 20-second/one-second-overlap windows and adaptive periodic previews; not native streaming.
- Two pinned source files: model.int8.onnx and vocab.txt, with exact sizes/SHA-256.
- The community graph lacks sherpa's vocabulary/subsampling/normalization metadata. A private
  staged copy appends three standard ONNX metadata properties (75 bytes); graph/tensor bytes and
  learned weights remain unchanged. The adapted copy has a separate fixed length/hash check.
  The original downloaded artifact is immutable. Failed/cancelled adaptation removes staging files.
- About 138 MB download storage plus a 138 MB adapted private copy; existing download free-space
  checks remain. Releasing or explicitly changing the selected model deletes its adapted copy; automatic
route changes retain derived caches. Selecting again after release
  can require re-adaptation. No weights are bundled, no cloud fallback or native build step is added.

The selected cleanup editor remains separate. Explicit Hindi mode retains the existing cleanup-model
bypass; script conversion still runs. This changes the alphabet, not the meaning or recognition accuracy.

## Transliteration choice and limits

Download **IndicXlit · Hindi to Roman** (~47 MB) to use the official AI4Bharat neural converter.
It is automatically shared by Nemotron and the other downloaded recognizers. See
[IndicXlit configuration and validation](INDICXLIT_TRANSLITERATION.md). Without the complete download,
ICU4J remains the basic offline path. Sanscript JavaScript is not embedded and no speech model is fine-tuned.

ICU uses phonetic spellings, which can differ from conversational Hinglish (e.g. मैं → maim).
Already-English strings, names and identifiers are preserved by the script converter, but the
Hindi recognizer may itself spell English loanwords in Devanagari. Neither natural code-switching,
English spelling restoration nor improved accuracy on the user's phone is guaranteed.

## Validation

The exact adapted INT8 graph and vocabulary produced Hindi text on a public Oriserve sample
(`वह बस दिन में कितनी बार चलती है`) through sherpa-onnx 2.0.1 host inference and the exact
Android API JAR with matching host JNI. Silence returned empty text; repeated fresh streams worked.
The graph's metadata and deterministic adapted checksum were independently verified with ONNX tools.

The initial Hindi integration's 52 voice unit tests passed, including Hindi routing for both Parakeet variants/Nemotron,
restoring English/Automatic choices, preserving unrelated/device/no-selection choices, failed
adaptation cleanup, immutable source bytes and mixed-text transliteration. Android compilation/dexing
passed; signing failed only because the shared debug.keystore is unavailable. No alternate key created.

Phone acceptance still needs short/long Hindi, pauses/calls, interrupted downloads, missing/deleted
Hindi components, actual model labels, saved-audio retry, language switching and comparisons with
Oriserve/Nemotron on the same Hindi/Hinglish recordings. Measure cold/warm latency, word accuracy,
RAM and thermals. Host smoke tests establish compatibility, not phone accuracy percentages.

The current combined automatic-routing suite and detector validation are recorded in the
[automatic routing guide](AUTOMATIC_LANGUAGE_ROUTING.md).
