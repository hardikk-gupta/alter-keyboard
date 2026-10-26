# whisper.cpp source provenance

Unmodified CPU build subset of ggml-org/whisper.cpp v1.9.5, commit
`d1be6fde11ac6e0407606b4e42fe72d34add8037`.

Includes upstream root CMake, cmake/, src/, include/, and ggml CMake/core/CPU
sources and licenses. Excludes disabled accelerator backends, samples, tests,
benchmarks and model files. `alter_whisper.cpp` is Alter's JNI bridge.

MIT license: whisper/LICENSE. CPU only, no OpenMP/GPU/KleidiAI downloads.
Native symbols are hidden in libalter_whisper to avoid collisions with LiteRT-LM.

This bridge serves Whisper and the Oriserve GGML conversions. Nemotron GGUF speech uses
its separate [transcribe.cpp runtime](../native-nemotron/SOURCE.md); Handy GGUF cleanup uses
its separate [llama.cpp runtime](../native-handy/SOURCE.md). Keep their symbols isolated.
Rebuild this bridge for both committed ABIs with `scripts/build_native_libs.sh` from the
repository root; normal Gradle builds consume the committed binaries only.
