# Oriserve Hindi2Hinglish Swift Q8

Original fine-tune/training: Oriserve, https://huggingface.co/Oriserve/Whisper-Hindi2Hinglish-Swift
Base: OpenAI Whisper Base (upstream MIT license remains applicable to upstream material).
GGML conversion/quantization: https://huggingface.co/anish2305/airnote-hinglish-stt-ggml
Conversion revision: d4f718b3eb4f3f8026a54c315bcfc54277225b02
Artifact: ggml-oriserve-hinglish-q8_0.bin (81,768,585 bytes)
SHA-256: 8550803dc10fa9f0b3db5cb884f8580c61067b062a67ee169d5789bcd0faec90
Both published model cards declare Apache-2.0. See Oriserve-Hinglish-Swift-Apache-2.0.txt.
The conversion card recommends Hindi language decoding for romanized output. The uploader describes Q8 as
near-lossless; this is an upstream claim, not an ALTER benchmark. This is a community conversion, not an
Oriserve-published GGML artifact. Models download only on request; no weights are distributed in the APK.

Additional original-precision artifact, same pinned revision/license/converter:
- ggml-oriserve-hinglish-fp16.bin: 147,951,465 bytes
- SHA-256: 4e9caa5f4b0416824d7cbeec22a37ef78a05e4b0189864eed65cd56d81c6b0a8
The uploader recommends FP16 for fidelity. No claim is made that changing precision resolves a particular word
error or improves Android latency. Select explicitly for comparison; existing Q8 selections are preserved.
