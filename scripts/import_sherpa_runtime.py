#!/usr/bin/env python3
"""Import the pinned, upstream static-ORT Android runtime. Never run from Gradle."""
import hashlib
import io
from pathlib import Path
import urllib.request
import zipfile

ROOT = Path(__file__).resolve().parents[1]
URL = "https://github.com/k2-fsa/sherpa-onnx/releases/download/v2.0.1/sherpa-onnx-static-link-onnxruntime-2.0.1.aar"
SHA256 = "07e042d97f3a6bd5b7fe74d4eef0513dc01a714b16c15a903250095aba43a575"

if __name__ == "__main__":
    with urllib.request.urlopen(URL) as response:
        data = response.read(38_577_051)
    if len(data) != 38_577_050 or hashlib.sha256(data).hexdigest() != SHA256:
        raise SystemExit("Upstream Android runtime size/checksum mismatch")
    with zipfile.ZipFile(io.BytesIO(data)) as archive:
        entries = {"classes.jar": "alter-voice/libs/sherpa-onnx-2.0.1.jar"}
        for abi in ("arm64-v8a", "x86_64"):
            entries[f"jni/{abi}/libsherpa-onnx-jni.so"] = f"alter-voice/src/main/jniLibs/{abi}/libsherpa-onnx-jni.so"
        for source, destination in entries.items():
            target = ROOT / destination
            target.parent.mkdir(parents=True, exist_ok=True)
            target.write_bytes(archive.read(source))
            print(destination)
