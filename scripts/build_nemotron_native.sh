#!/usr/bin/env bash
# Rebuild pinned CPU Nemotron runtime; Gradle consumes the committed binaries only.
set -euo pipefail
ROOT="$(cd "$(dirname "$0")/.." && pwd)"
SDK="${ANDROID_HOME:?Set ANDROID_HOME}"
NDK="$SDK/ndk/28.2.13676358"
CMAKE="$SDK/cmake/3.22.1/bin/cmake"
for ABI in arm64-v8a x86_64; do
  OUT="$ROOT/build/native/alter_nemotron/$ABI"
  "$CMAKE" -S "$ROOT/alter-voice/native-nemotron" -B "$OUT" -G Ninja \
    -DCMAKE_MAKE_PROGRAM="$SDK/cmake/3.22.1/bin/ninja" \
    -DCMAKE_TOOLCHAIN_FILE="$NDK/build/cmake/android.toolchain.cmake" \
    -DANDROID_ABI="$ABI" -DANDROID_PLATFORM=android-26 -DANDROID_STL=c++_static \
    -DCMAKE_BUILD_TYPE=Release -DANDROID_SUPPORT_FLEXIBLE_PAGE_SIZES=ON
  "$CMAKE" --build "$OUT" --target alter_nemotron -- -j4
  mkdir -p "$ROOT/alter-voice/src/main/jniLibs/$ABI"
  "$NDK/toolchains/llvm/prebuilt/linux-x86_64/bin/llvm-strip" --strip-unneeded \
    -o "$ROOT/alter-voice/src/main/jniLibs/$ABI/libalter_nemotron.so" "$OUT/libalter_nemotron.so"
  chmod 644 "$ROOT/alter-voice/src/main/jniLibs/$ABI/libalter_nemotron.so"
done
