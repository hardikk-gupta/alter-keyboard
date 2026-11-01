# Oriserve Hindi2Hinglish Apex Q8

Original fine-tune/training: Oriserve, https://huggingface.co/Oriserve/Whisper-Hindi2Hinglish-Apex
(model card revision f3214eed20b4e4d4144e739982d911f87b9cb223, Apache-2.0).
Base: OpenAI Whisper large-v3-turbo (upstream MIT license remains applicable to upstream material).
GGML conversion/quantization: https://huggingface.co/Marquestra/Whisper-Hindi2Hinglish-Apex-GGML (Apache-2.0)
Conversion revision: d1de3ff618856e5675c47d3158ca820506fb4d9e
Artifact: ggml-apex-hinglish-q8_0.bin (874,188,075 bytes)
SHA-256: bef6d9d571e9e7bd3cd2ebba8b2d5c9cc4731e210d6750ae6a9c621a87e60d4c
See Oriserve-Hinglish-Apex-Apache-2.0.txt. This is a community conversion, not an Oriserve-published GGML artifact.
The same file is mirrored byte-for-byte at gouravg8/gtm-hi2hing-ggml. Models download only on request; no
weights are distributed in the APK.

Why Q8 and not FP16: on the six sample clips published on Oriserve's model card plus six Oriserve Swift samples,
the converter's FP16 file (1,624,555,275 bytes, SHA-256 84457be08fe5101f3691edbbe5bee977844b2bfdd56a76eec350e9b69bf2ffa2) produced the
same text as Q8 with the bundled whisper.cpp 1.9.5 while taking about twice the CPU time and ten times the load
time on a 4-core x86 host. This is a host measurement, not phone latency or a word error rate.

Decoding: Hindi language token, greedy, no temperature fallback, flash attention, full 30 s encoder window
(shorter windows made this model repeat tokens). Runtime provenance: [Whisper bridge](../alter-voice/native/SOURCE.md).
