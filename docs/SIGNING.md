# ALTER Keyboard Signing & Release Guide

## Target Signing Identity

The keyboard must be signed with the same shared key as ALTER, so builds and updates install over each other cleanly.

| Parameter | Value |
| --- | --- |
| SHA-1 Fingerprint | `B5:E5:7C:D6:93:49:2E:96:ED:5E:96:30:0D:7C:AC:38:73:5B:7E:BB` |
| Alias | `androiddebugkey` |
| Key Password | `android` |
| Keystore Password | `android` |
| CI Key Source | `ALTER_SIGNING_KEYSTORE_B64` environment variable (base64 of `debug.keystore`) |
| Local Fallback | Repo root `debug.keystore` (must be git-ignored) |

## Commit-Time Version Code

To prevent downgrade errors across builds:
- Version code = minutes elapsed since `2026-01-01T00:00:00Z` of the commit date.
- Fallback when no `.git` directory exists: current build timestamp in minutes.

## Security Constraints

1. Never commit `.jks`, `.keystore`, `.env`, or `local.properties` files to version control.
2. Verify the APK SHA-1 fingerprint before any release:
   ```bash
   apksigner verify --print-certs path/to/app-release.apk
   ```
   Ensure the output matches `scripts/signing_certificate.sha1`.
