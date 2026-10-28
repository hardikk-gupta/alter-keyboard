# Parakeet 110M INT8 CTC

Original checkpoint: https://huggingface.co/nvidia/parakeet-tdt_ctc-110m
Creator: NVIDIA. Hybrid FastConformer with TDT and CTC heads, English only.
Model license: CC-BY-4.0; terms retained in `Parakeet-110M-CC-BY-4.0.txt`.
The source checkpoint is provenance, not a second independently trained model.

Android conversion: k2-fsa/sherpa-onnx NeMo CTC export, INT8 quantized ONNX.
Exact release package:
https://github.com/k2-fsa/sherpa-onnx/releases/download/asr-models/sherpa-onnx-nemo-parakeet_tdt_ctc_110m-en-36000-int8.tar.bz2
Archive bytes: 104337827
SHA-256: 17f945007b52ccd8b7200ffc7c5652e9e8e961dfdf479cefcabd06cf5703630b

Extracted files (the app additionally checks each size and checksum):

| File | Bytes | SHA-256 |
| --- | --- | --- |
| model.int8.onnx | 131652171 | 9177a9146cf32ee0cc8152276ef95116f312018d316be37ccf57f7efea81fc1a |
| tokens.txt | 9953 | 450e56bd2f036fe5b6aa821865838cc5aa9d8b0106134ce9a9ba0664abe6cd10 |

Conversion and quantization change the original representation; this is not an ALTER fine-tune.
Weights download on demand and are never committed or bundled in the APK. The archive is privately
cached; only the two whitelisted, verified regular files are extracted into private no-backup storage.
No source tar paths, symlinks or test WAVs are written by the app.

## Android runtime

sherpa-onnx v2.0.1, source commit af2d8d81dbfa61c4cea4c568da78f3982edca005 (Apache-2.0).
Upstream artifact: sherpa-onnx-static-link-onnxruntime-2.0.1.aar
Bytes: 38577050
SHA-256: 07e042d97f3a6bd5b7fe74d4eef0513dc01a714b16c15a903250095aba43a575
Retained entries: classes.jar and arm64-v8a/x86_64 libsherpa-onnx-jni.so only.
Refresh from the pinned artifact with `python3 scripts/import_sherpa_runtime.py`.
Gradle uses the committed JAR and JNI libraries and performs no native compilation/download.

The 64-bit native libraries statically link ONNX Runtime 1.28.2, with no shared libonnxruntime.so
dependency and no exported OrtGetApiBase symbol. This avoids collisions with Moonshine's runtime.
Both binaries have 16 KB ELF load alignment. Their upstream minimum SDK is 21; ALTER stays at 26.
ONNX Runtime's MIT license and third-party notices are retained beside this file. The Java/Kotlin
API and native library come from the same verified release artifact.

The app uses CPU execution, up to two threads, 16 kHz/80-dimensional features, zero dither and greedy
CTC decoding. These are conservative starting settings, not a measured phone optimum.

Archive parsing uses Apache Commons Compress 1.27.1, with Commons IO 2.16.1,
Commons Codec 1.17.1 and Commons Lang 3.16.0 dependencies. Their Apache-2.0
licenses and upstream NOTICE files are retained under `licenses/commons-*`.
