#!/usr/bin/env python3
"""Import the pinned, upstream static-ORT Android runtime. Never run from Gradle."""
import hashlib
import io
from pathlib import Path
import urllib.request
import zipfile

ROOT = Path(__file__).resolve().parents[1]
URL = "https://github.com/k2-fsa/sherpa-onnx/releases/download/v2.0.3/sherpa-onnx-static-link-onnxruntime-2.0.3.aar"
SHA256 = "7744a3a8f08f017d59c2d8ed8d9b700345561f5abe9f4e06dc6eaef3a0b977b4"

if __name__ == "__main__":
    with urllib.request.urlopen(URL) as response:
        data = response.read(39_999_116)
    if len(data) != 39_999_115 or hashlib.sha256(data).hexdigest() != SHA256:
        raise SystemExit("Upstream Android runtime size/checksum mismatch")
    with zipfile.ZipFile(io.BytesIO(data)) as archive:
        entries = {"classes.jar": "alter-voice/libs/sherpa-onnx-2.0.3.jar"}
        for abi in ("arm64-v8a", "x86_64"):
            entries[f"jni/{abi}/libsherpa-onnx-jni.so"] = f"alter-voice/src/main/jniLibs/{abi}/libsherpa-onnx-jni.so"
        for source, destination in entries.items():
            target = ROOT / destination
            target.parent.mkdir(parents=True, exist_ok=True)
            target.write_bytes(archive.read(source))
            print(destination)
