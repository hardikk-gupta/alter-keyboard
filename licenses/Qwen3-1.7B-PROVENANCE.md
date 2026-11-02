# Qwen3 1.7B dictation cleanup

Original model: Qwen team, https://huggingface.co/Qwen/Qwen3-1.7B (revision 70d244cc86ccca08cf5af4e1e306ecf908b1ad5e,
Apache-2.0). LiteRT-LM conversion: Google AI Edge, https://huggingface.co/litert-community/Qwen3-1.7B (Apache-2.0).
Conversion revision: ebb4eef4c07c0ecf019fa3cf7b501771d44ca21f
Artifact: Qwen3_1.7B.litertlm (1,754,890,240 bytes), dynamic INT8: channel-wise INT8 weights, FP32 activations.
SHA-256: e40599625f3e446d6522674586a40566d6a3a117af608f6e9ea46ac86d326461
See Qwen3-1.7B-Apache-2.0.txt. Runs on the existing LiteRT-LM 0.18.0 CPU engine; downloaded only on request,
never bundled in the APK.

Replaces LFM2.5 350M as the only cleanup editor because LFM2.5's supported languages do not include Hindi. With
the app's own instructions, examples and validator (JVM `litertlm-jvm` 0.18.0, same CPU backend), LFM2.5 inserted
unrelated text on every Hinglish sample and all edits were rejected; Qwen3 produced 9 accepted edits out of 11
samples. Its INT8 file was chosen over the 977 MB INT4 file because the converter's published Galaxy S26 figures
show about twice the prefill speed, and cleanup requests are mostly prompt. Thinking is disabled.
These are host and publisher measurements, not ALTER phone measurements.
