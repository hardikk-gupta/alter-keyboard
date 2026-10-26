CPU subset of ggml-org/llama.cpp, revision 2280f0fd163b03d4331d49e08460f66502e35854, MIT.
Includes src/include/cmake and ggml CPU dependencies. Tools, servers, tests and accelerators are excluded.
ALTER's JNI bridge uses Handy's explicit-empty-system ChatML framing, greedy decoding, fresh contexts and abort callbacks.
Android libraries must be rebuilt and committed alongside source edits. Gradle uses prebuilt libraries only.

Vendored source and third-party license notices are preserved byte-for-byte. Upstream whitespace is excluded
from git whitespace checks through the narrow .gitattributes rule; ALTER-owned bridge/build files remain checked.

Rebuild both ABIs with `scripts/build_handy_native.sh` from the repository root (NDK
28.2.13676358, CMake 3.22.1, API 26). The aggregate native script also rebuilds this library.
Handy edits text after ASR; it does not decode Nemotron audio. Nemotron uses a separately
pinned [transcribe.cpp runtime](../native-nemotron/SOURCE.md), with independent ggml symbols.
See [model provenance](../../licenses/Handy-Editor-PROVENANCE.md) and the
[experiment guide](../../docs/HANDY_EXPERIMENT.md) for the trained protocol and validation.
