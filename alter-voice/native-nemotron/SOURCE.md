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

The bridge maps automatic language selection to a null native hint (the literal string
`auto` is not accepted by the generic language gate). English/Hindi map to `en-US`/`hi-IN`.
Snapshots cross JNI as UTF-8 byte arrays. A successful final flush is required; failed or
cancelled finalization returns no accepted output. The model's original language/script
is preserved. See [experiment results](../../docs/NEMOTRON_EXPERIMENT.md).

## Parakeet TDT offline adapter

The same pinned CPU runtime also serves `ParakeetTdtNative` for the English-only Parakeet 110M
FP16 GGUF. It uses offline `transcribe_run`, not Nemotron's streaming API. Each bounded 20-second
window gets a fresh native session and abort callback, at most two CPU threads in the app, and
UTF-8 output after successful completion. Cancellation/failure never accepts partial results.
Existing prepare/cancel/close methods operate on the shared engine holder; each selected model
has its own handle. Nemotron's streaming capability gate and profile remain unchanged.

Parakeet weights are CC-BY-4.0, distinct from Nemotron's OpenMDW license. See
[Parakeet TDT provenance](../../licenses/Parakeet-TDT-PROVENANCE.md). Both arm64-v8a and x86_64
libraries must be rebuilt and committed when this adapter changes.

JNI streaming failures now throw an operation-specific `IllegalStateException` using
`transcribe_status_string`: session setup, start (including the requested locale), feed,
finalize and snapshot. Successful language mapping and model weights are unchanged;
automatic still passes a null hint and Hindi passes `hi-IN`, never an English fallback.
Both committed Android ABI libraries are rebuilt with this adapter change.
