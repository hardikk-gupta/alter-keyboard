# Pinned Nemotron CPU runtime

Upstream: https://github.com/handy-computer/transcribe.cpp
Commit: 8facde6fcd8fb9caa585a5788ab450df6e8ec7db

`transcribe/` contains the upstream include, src, cmake and CPU ggml build sources,
plus their root CMake and license files, unchanged. Tools, examples, tests and unused
GPU backends are omitted. The parent CMake supplies the include directory needed when
embedding upstream as a subproject. `alter_nemotron.cpp` is ALTER's JNI adapter.
Upstream MIT license and bundled dependency notices are preserved in the source tree;
`licenses/transcribe.cpp-MIT.txt` also records the runtime license.

Model: handy-computer/nemotron-3.5-asr-streaming-0.6b-gguf
Revision: 53ce288f69326b415aeef030cfcb02a08a8096dd
File: nemotron-3.5-asr-streaming-0.6b-Q4_K_M.gguf
Bytes: 495831520
SHA-256: 41c99fa5fb6f3d35f68e79adc3e755eca2232a8d921178bd647b71194792b8fd
Base model: nvidia/nemotron-3.5-asr-streaming-0.6b
License: OpenMDW-1.1; exact model LICENSE and NOTICE are in `licenses/`.
Model weights are never bundled. This license is distinct from the MIT runtime license.

Build both Android ABIs using `scripts/build_nemotron_native.sh` (NDK 28.2.13676358,
CMake 3.22.1, API 26, Release, static C++ runtime, hidden symbols and 16 KB page alignment).
