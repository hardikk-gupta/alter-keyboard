#!/usr/bin/env bash
# Rebuilds ALTER Keyboard's prebuilt native libraries from source and copies them into the jniLibs folders.
#
# Google AI Studio has no Android NDK or CMake, so Gradle never compiles C++. The libraries below are committed
# instead; run this script (with the NDK and CMake installed) whenever their C/C++ sources change:
#   - libjni_latinime.so  FUTO's typing engine (app/native/jni)      -> app/src/main/jniLibs/<abi>/
#   - libalter_whisper.so ALTER's whisper.cpp bridge (alter-voice/native) -> alter-voice/src/main/jniLibs/<abi>/
#
# Usage: ANDROID_HOME=/path/to/sdk scripts/build_native_libs.sh
set -euo pipefail

ROOT="$(cd "$(dirname "$0")/.." && pwd)"
SDK="${ANDROID_HOME:-${ANDROID_SDK_ROOT:-}}"
[ -n "$SDK" ] || { echo "Set ANDROID_HOME to your Android SDK." >&2; exit 1; }
NDK="$SDK/ndk/28.2.13676358"
CMAKE="$SDK/cmake/3.22.1/bin/cmake"
NINJA="$SDK/cmake/3.22.1/bin/ninja"
STRIP="$NDK/toolchains/llvm/prebuilt/linux-x86_64/bin/llvm-strip"
[ -d "$NDK" ] || { echo "Install NDK 28.2.13676358 (sdkmanager \"ndk;28.2.13676358\")." >&2; exit 1; }
[ -x "$CMAKE" ] || { echo "Install CMake 3.22.1 (sdkmanager \"cmake;3.22.1\")." >&2; exit 1; }

OUT="$ROOT/build/native"
for ABI in arm64-v8a x86_64; do
  build() { # <name> <source dir> <library> <destination jniLibs dir> [extra cmake args]
    local name=$1 src=$2 lib=$3 dest=$4; shift 4
    "$CMAKE" -S "$src" -B "$OUT/$name/$ABI" -G Ninja -DCMAKE_MAKE_PROGRAM="$NINJA" \
      -DCMAKE_TOOLCHAIN_FILE="$NDK/build/cmake/android.toolchain.cmake" \
      -DANDROID_ABI="$ABI" -DANDROID_PLATFORM=android-26 -DANDROID_STL=c++_static \
      -DCMAKE_BUILD_TYPE=Release -DANDROID_SUPPORT_FLEXIBLE_PAGE_SIZES=ON "$@" > /dev/null
    "$CMAKE" --build "$OUT/$name/$ABI" --target "${lib#lib}" -- -j"$(nproc)" > /dev/null
    mkdir -p "$dest/$ABI"
    "$STRIP" --strip-unneeded -o "$dest/$ABI/$lib.so" "$(find "$OUT/$name/$ABI" -name "$lib.so" | head -1)"
    echo "$ABI/$lib.so: $(stat -c %s "$dest/$ABI/$lib.so") bytes"
  }
  build latinime "$ROOT/app/native/jni" libjni_latinime "$ROOT/app/src/main/jniLibs"
  build alter_whisper "$ROOT/alter-voice/native" libalter_whisper "$ROOT/alter-voice/src/main/jniLibs"
done
