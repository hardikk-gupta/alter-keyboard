#!/usr/bin/env python3
"""Check that an ALTER Keyboard APK is signed by the shared ALTER key (scripts/signing_certificate.sha1).

Usage: scripts/check_apk_signature.py [path/to.apk]
Defaults to build/outputs/apk/stable/debug/alter-keyboard-stable-debug.apk. Needs ANDROID_HOME with build-tools.
"""
import os
from pathlib import Path
import re
import subprocess
import sys

ROOT = Path(__file__).resolve().parents[1]


def find_apksigner():
    sdk = os.environ.get('ANDROID_HOME') or os.environ.get('ANDROID_SDK_ROOT')
    if not sdk:
        raise SystemExit('Set ANDROID_HOME to your Android SDK.')
    tools = sorted((Path(sdk) / 'build-tools').glob('*/apksigner'))
    if not tools:
        raise SystemExit('apksigner not found under $ANDROID_HOME/build-tools.')
    return tools[-1]


def signing_certificate(signer, apk):
    out = subprocess.run([str(signer), 'verify', '--print-certs', str(apk)], cwd=ROOT, check=True, capture_output=True, text=True).stdout
    match = re.search(r'Signer #1 certificate SHA-1 digest: ([0-9a-f]+)', out)
    return ':'.join(re.findall('..', match[1].upper())) if match else None


def main():
    apk = Path(sys.argv[1]) if len(sys.argv) > 1 else ROOT / 'build/outputs/apk/stable/debug/alter-keyboard-stable-debug.apk'
    expected = (ROOT / 'scripts/signing_certificate.sha1').read_text().strip()
    actual = signing_certificate(find_apksigner(), apk)
    if actual != expected:
        # Phones only accept an update signed by the key they installed with.
        raise SystemExit(f'{apk.name} is signed by {actual}, not the shared ALTER key {expected}. Phones would refuse it as an update. '
                         'Set ALTER_SIGNING_KEYSTORE_B64 or place the shared debug.keystore in the repo root (docs/SIGNING.md).')
    print(f'{apk.name}: signed by the shared ALTER key {actual}.')


if __name__ == '__main__':
    main()
