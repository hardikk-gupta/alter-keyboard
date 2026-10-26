# Nemotron 3.5 Streaming 0.6B Q4_K_M

Original model: https://huggingface.co/nvidia/nemotron-3.5-asr-streaming-0.6b
Community conversion: https://huggingface.co/handy-computer/nemotron-3.5-asr-streaming-0.6b-gguf
Pinned revision: 53ce288f69326b415aeef030cfcb02a08a8096dd
Artifact: nemotron-3.5-asr-streaming-0.6b-Q4_K_M.gguf (495,831,520 bytes)
SHA-256: 41c99fa5fb6f3d35f68e79adc3e755eca2232a8d921178bd647b71194792b8fd

Model license: [OpenMDW-1.1](Nemotron-3.5-OpenMDW-1.1.txt).
The converter's [NOTICE](Nemotron-3.5-NOTICE.txt) records tensor conversion and quantization;
these are not a new fine-tune. Weights download on demand and are never bundled in the APK.
The model license is separate from the MIT [transcribe.cpp runtime](transcribe.cpp-MIT.txt).
Runtime pin and rebuild instructions: [native source](../alter-voice/native-nemotron/SOURCE.md).

The 320 ms cache-aware profile describes model geometry, not measured phone latency.
Hindi/English streaming works in host smoke tests, but Hindi recognition is imperfect and
can emit mixed script. Phone speed, code-switching quality and superiority over Moonshine
or Oriserve are unverified. See [experiment results](../docs/NEMOTRON_EXPERIMENT.md).
