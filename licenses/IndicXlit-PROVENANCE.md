# IndicXlit Indic-to-Roman v1.0

Source: https://github.com/AI4Bharat/IndicXlit (MIT, including pretrained weights).
Official model: https://github.com/AI4Bharat/IndicXlit/releases/download/v1.0/indicxlit-indic-en-v1.0.zip

- Archive: 124,533,637 bytes; SHA-256 `420d23a13cfa953edfb47644bc805de06e97134f25e6d182303a29f55b5a3322`.
- Extracted `transformer/indicxlit.pt`: SHA-256 `45ffecef88d990ae12f915939b91267a3fc8f69f26331baa59f7932005976f28`.
- Host-only exporter: `scripts/export_indicxlit.py`. No training, quantization or optimizer weights in exports.
  Standard six-layer prenorm Transformer encoder/decoder, 256 dimensions, four attention heads,
  1,024 feedforward dimensions, GELU, Fairseq sinusoidal positions and original embeddings/projection.
- Vocabulary order is the official training `lang_list.txt` (English first), using the checkpoint's
  original character dictionaries, four special tokens, and 22 multilingual language symbols.

Published Android graphs (ONNX opset 17, FP32), under the dedicated model release
https://github.com/hardikk-gupta/alter-keyboard/releases/tag/indicxlit-v1.0-onnx-1:

| File | Bytes | SHA-256 |
| --- | ---: | --- |
| encoder.onnx | 20,879,648 | `07a5a3a970f2f04ce5b75e51eea5dd579f7794f176ff0b37455b60151eba1b6c` |
| decoder.onnx | 26,535,207 | `1c7dd0bac96aa2d2d752f2afb393a128c9ddc9e51d6119f7175b3bf244ff0e0e` |
| vocab.json | 6,179 | `acd0707dd66508a5af047f8e3b9a62e6daf10aed1c5e3db824f2328efe075bf5` |

MIT copyright retained in `IndicXlit-MIT.txt`. AI4Bharat reference paper:
Madhani et al. (2022), *Aksharantar: Towards building open transliteration tools for the next billion users*,
https://arxiv.org/abs/2205.03018.

ONNX Runtime Java/Android is pinned to Maven `com.microsoft.onnxruntime:onnxruntime-android:1.23.2`;
retain `ONNX-Runtime-MIT.txt` and `ONNX-Runtime-ThirdPartyNotices.txt`.
Moonshine 0.1.5 already links ORT API 1.23.2. The Gradle transform omits only its reduced duplicate core;
its original package and notices are otherwise unchanged. The full Maven build supplies ORT to both consumers.
The existing statically linked sherpa-onnx runtime remains isolated.
