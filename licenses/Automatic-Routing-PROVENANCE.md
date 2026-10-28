# Automatic routing detector and reference

Detector base model: OpenAI Whisper Tiny multilingual, MIT.
Converter/export repository: https://huggingface.co/csukuangfj/sherpa-onnx-whisper-tiny
Pinned revision: 65176e2deb88badc814a94058666cadccc29b61c
The OpenAI model license is retained in [Whisper-Tiny-LID-MIT.txt](Whisper-Tiny-LID-MIT.txt).
These are ONNX INT8 conversions used only for spoken-language identification, not dictation ASR.
No ALTER fine-tune or hidden download is performed. Model files are user-downloaded into verified
private no-backup storage, never bundled in Git/APK.

| File | Bytes | SHA-256 |
| --- | --- | --- |
| tiny-encoder.int8.onnx | 12937772 | d24fb083ae3b1041fc24e97971d60e280c9342201fbb67b0ab428a8b4a51a434 |
| tiny-decoder.int8.onnx | 89855401 | d2fece8dd42771f1df975c6c0445770d0c292bf7547c2cae04a6c0cc57540925 |

The on-device runtime is the already pinned sherpa-onnx 2.0.1 static-ORT Java/JNI pair.
Its existing licenses and notices remain applicable; no additional native binaries are introduced.
The download bundle also references the already-pinned IndicConformer graph/vocabulary; existing
installed copies are reused. See [Hindi provenance](IndicConformer-Hindi-PROVENANCE.md).

Architectural reference: https://github.com/stevengonsalvez/sayso
Commit: 559429e52f41beaa2e3fd698c208551116b40617
EarlyLidRouter.kt demonstrates Whisper LID followed by English/Indic ASR routing. The MIT
copyright/permission notice is retained in [Sayso-MIT.txt](Sayso-MIT.txt). ALTER's routing integrates
its own model catalog, PCM windows, cancellation/recovery, end probes and mixed-language fallback.
No hard-coded probability is presented as classifier confidence. See
[the routing guide](../docs/AUTOMATIC_LANGUAGE_ROUTING.md) for behavior and testing limits.
