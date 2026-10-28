# AI4Bharat IndicConformer Hindi INT8

Original model: https://huggingface.co/ai4bharat/indicconformer_stt_hi_hybrid_ctc_rnnt_large
Publisher: AI4Bharat; original model repository revision deada84ce880997c56ee933aa21571d768264700.
The original Hugging Face checkpoint is gated. ALTER does not download or redistribute that checkpoint.

Public community export: https://huggingface.co/OpenVoiceOS/ai4bharat-indicconformer-hi-onnx
Pinned revision: 8960b8611af5b8c375d442d52907360176410c8b
The export card identifies the original AI4Bharat model, declares MIT and targets onnx-asr.
Original code/model license statement: https://github.com/AI4Bharat/IndicConformerASR
Exact MIT license retained in [IndicConformer-MIT.txt](IndicConformer-MIT.txt).

| Download | Bytes | SHA-256 |
| --- | --- | --- |
| model.int8.onnx | 137677313 | c2044177b6d44494fe40be755466845a4c7bd1b5a5d1f6a15b33b2b4ba6011cd |
| vocab.txt | 2700 | 152e58dc9469feb8f74fb006245616ad7cd91b3f7a5ace72ec53f970db10c981 |

These source artifacts remain immutable in private no-backup storage, downloaded on user request.
No weights are bundled in the APK or Git. Community export and INT8 quantization are separate from
ALTER's integration; this is not an AI4Bharat-published sherpa-onnx package or an ALTER fine-tune.

## Runtime adaptation

The runtime is the existing pinned sherpa-onnx 2.0.1 static-ORT CTC path from the Parakeet integration.
A private derived graph appends standard ONNX metadata fields: vocab_size=257,
subsampling_factor=4 and normalize_type=per_feature. Graph/tensor/learned-weight bytes are unchanged.
The adapted file contains the entire original byte prefix plus 75 metadata bytes.
Adapted bytes: 137677388
Adapted SHA-256: 411567662991f2b57e6f46348e200d5c55ebdba3083fed4c56b9020acefaecbd
The app stages and separately verifies this derived file before passing it to native inference.

The runtime's existing licenses/third-party notices remain applicable. Transliteration reuses
ICU4J 76.1 (Unicode License V3), not a new IndicXlit or Sanscript dependency.
[Pipeline guide](../docs/INDIC_HINDI_PIPELINE.md) records routing and validation limits.
