# Parakeet 110M TDT FP16

Original checkpoint: https://huggingface.co/nvidia/parakeet-tdt_ctc-110m
Creator: NVIDIA; original source revision 431a349 as recorded by the converter.
Community conversion: https://huggingface.co/handy-computer/parakeet-tdt_ctc-110m-gguf
Pinned revision: 1c3f0d5378fdd358905cc410a93ed9e64f52d170
Artifact: parakeet-tdt_ctc-110m-F16.gguf
Bytes: 229334560
SHA-256: 965cc3f8e171286ecb4d4e6646b407a1782a97da9aed22f88d1e0ec6c96b2358

License: [CC-BY-4.0](Parakeet-110M-CC-BY-4.0.txt). The converter's exact
[NOTICE](Parakeet-TDT-NOTICE.txt) is retained. Conversion repackages tensors and drops the auxiliary
CTC head, keeping the TDT/RNN-T transducer decoder. FP16 weights are used; this is not an ALTER
fine-tune, and the number of trained parameters is not increased by choosing the larger format.
Only the model file downloads at runtime. No weights are bundled in Git or the APK.

The pinned model card reports 2.43% WER for F16 on the full LibriSpeech test-clean split
(2,620 utterances), greedy TDT/RNN-T decoding without an external language model. It reports the
same WER for F32, which is approximately twice the size. These are the converter's benchmarks,
not ALTER phone measurements, not Hindi/Hinglish results, and not a direct comparison with our
sherpa-onnx INT8 CTC configuration. Larger downloads do not establish higher phone accuracy.

Runtime: the existing CPU-only transcribe.cpp implementation at commit
8facde6fcd8fb9caa585a5788ab450df6e8ec7db, already used for Nemotron. It has a separate MIT runtime
license. The added offline JNI path creates a fresh session per bounded window, uses up to two
CPU threads, accepts 16 kHz mono PCM and returns UTF-8 only after a successful, uncancelled run.
Cancellation sets an atomic flag consumed by the native abort callback. Rebuild both Android
ABIs using `scripts/build_nemotron_native.sh`; Gradle still consumes committed prebuilts only.
