#!/usr/bin/env bash
# Needle publishes an Android arm64 static library. Keep it separate so a session can unload it.
set -euo pipefail
cd "$(dirname "$0")/.."
bin="${ANDROID_HOME:?}/ndk/28.2.13676358/toolchains/llvm/prebuilt/linux-x86_64/bin"
out=alter-voice/src/main/jniLibs/arm64-v8a
tmp=$(mktemp -d)
trap 'rm -rf "$tmp"' EXIT
url=https://huggingface.co/Cactus-Compute/needle3/resolve/d6c7d6a2e9655505e0f8e23339356dd1705900eb/android-arm64
curl --fail --location "$url/libneedle.a" -o "$tmp/libneedle.a"
echo "7aa6de6132a22bf45f5e903425feb66f159ad46f47fa558921bc4fd02c3a2fa0  $tmp/libneedle.a" | sha256sum -c -
"$bin/aarch64-linux-android26-clang++" -shared -static-libstdc++ -Wl,-u,needle_load \
  -Wl,--version-script=alter-voice/native-needle/exports.map -Wl,-z,defs -Wl,-soname,libalter_needle_engine.so \
  "$tmp/libneedle.a" -lm -llog -o "$tmp/libalter_needle_engine.so"
"$bin/aarch64-linux-android26-clang" -shared -fPIC -O2 -Wl,-z,defs \
  alter-voice/native-needle/bridge.c -ldl -o "$tmp/libalter_needle_jni.so"
for lib in libalter_needle_engine libalter_needle_jni; do
  "$bin/llvm-strip" -o "$out/$lib.so" "$tmp/$lib.so"
done
