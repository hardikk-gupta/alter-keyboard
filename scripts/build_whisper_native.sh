#!/usr/bin/env bash
# Rebuilds ALTER's whisper.cpp bridge (alter-voice/native) into alter-voice/src/main/jniLibs/<abi>/.
#
# arm64-v8a gets three builds of the same source, one per CPU feature level, because ggml selects its int8/fp16
# kernels at compile time:
#   libalter_whisper.so          ARMv8.0 baseline (any 64-bit phone)
#   libalter_whisper_dotprod.so  ARMv8.2 + dotprod + fp16 (most phones since 2018)
#   libalter_whisper_i8mm.so     ARMv8.2 + dotprod + fp16 + i8mm (recent big cores)
# WhisperNative reads /proc/cpuinfo and loads the best one the phone supports. x86_64 (emulators) gets the baseline.
#
# Usage: ANDROID_HOME=/path/to/sdk scripts/build_whisper_native.sh
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

OUT="$ROOT/build/native/alter_whisper"
DEST="$ROOT/alter-voice/src/main/jniLibs"
build() { # <abi> <library name> [GGML_CPU_ARM_ARCH]
  local abi=$1 name=$2 arch=${3:-}
  local dir="$OUT/$abi/$name"
  "$CMAKE" -S "$ROOT/alter-voice/native" -B "$dir" -G Ninja -DCMAKE_MAKE_PROGRAM="$NINJA" \
    -DCMAKE_TOOLCHAIN_FILE="$NDK/build/cmake/android.toolchain.cmake" \
    -DANDROID_ABI="$abi" -DANDROID_PLATFORM=android-26 -DANDROID_STL=c++_static \
    -DCMAKE_BUILD_TYPE=Release -DANDROID_SUPPORT_FLEXIBLE_PAGE_SIZES=ON \
    -DALTER_WHISPER_NAME="$name" ${arch:+-DGGML_CPU_ARM_ARCH=$arch} > /dev/null
  "$CMAKE" --build "$dir" --target alter_whisper -- -j"$(nproc)" > /dev/null
  mkdir -p "$DEST/$abi"
  "$STRIP" --strip-unneeded -o "$DEST/$abi/lib$name.so" "$dir/lib$name.so"
  echo "$abi/lib$name.so: $(stat -c %s "$DEST/$abi/lib$name.so") bytes"
}

build arm64-v8a alter_whisper
build arm64-v8a alter_whisper_dotprod armv8.2-a+dotprod+fp16
build arm64-v8a alter_whisper_i8mm armv8.2-a+dotprod+fp16+i8mm
build x86_64 alter_whisper
