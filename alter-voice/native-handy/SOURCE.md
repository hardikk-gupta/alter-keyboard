CPU subset of ggml-org/llama.cpp, revision 2280f0fd163b03d4331d49e08460f66502e35854, MIT.
Includes src/include/cmake and ggml CPU dependencies. Tools, servers, tests and accelerators are excluded.
ALTER's JNI bridge uses Handy's explicit-empty-system ChatML framing, greedy decoding, fresh contexts and abort callbacks.
Android libraries must be rebuilt and committed alongside source edits. Gradle uses prebuilt libraries only.

Vendored source and third-party license notices are preserved byte-for-byte. Upstream whitespace is excluded
from git whitespace checks through the narrow .gitattributes rule; ALTER-owned bridge/build files remain checked.
