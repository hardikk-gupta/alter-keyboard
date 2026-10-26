# Signing: one shared key for every ALTER build, including ALTER Keyboard

Android installs an update only when it is signed by the **same key** as the installed app. Different keys make the
installer fail ("App not installed" / "package conflicts", or it silently closes), so the phone keeps the old ALTER.

ALTER Keyboard (`com.aistudio.altermind.keyboard`) is a separate app from ALTER, signed with the same shared key.
The signing structure is Google AI Studio's: `debugConfig` uses `ALTER_SIGNING_KEYSTORE_B64` when set, else
`debug.keystore` in the repo root; `release` uses `KEYSTORE_PATH`, `STORE_PASSWORD` and `KEY_PASSWORD`, and falls back
to `debugConfig` when they are missing. A local Gradle build without either key fails. The current GitHub APK
workflow has a separate fallback: it generates a one-off key when the secret is missing. That APK cannot update a build signed with the shared key;
set the repository secret for consistent updates. Native/model changes do not change this signing requirement.

## The shared key

- Its SHA-1 is pinned in `scripts/signing_certificate.sha1` (currently `B5:E5:7C:D6:93:49:2E:96:ED:5E:96:30:0D:7C:AC:38:73:5B:7E:BB`,
  the key also used by the main ALTER app for Google Drive sign-in).
- `scripts/check_apk_signature.py` checks a built APK against it.
- The keystore itself is never committed. Every builder provides it in one of two ways:
  1. **Environment variable `ALTER_SIGNING_KEYSTORE_B64`** — the keystore file encoded as base64. Use this for Claude/Codex
     cloud environments (environment settings → environment variables) and GitHub (Settings → Secrets and variables →
     Actions → `ALTER_SIGNING_KEYSTORE_B64`).
  2. **`debug.keystore` in the repo root** — for Android Studio on your own computer. Copy the shared file there.

Store password, key password and alias are the standard debug values: `android` / `android` / `androiddebugkey`.

Make the base64 value from the keystore file:

- macOS/Linux: `base64 -i debug.keystore | tr -d '\n'`
- Windows PowerShell: `[Convert]::ToBase64String([IO.File]::ReadAllBytes("debug.keystore"))`

Keep the keystore and its base64 private: anyone holding it can sign an APK that installs over ALTER on a phone.

## Changing the shared key

Only with the designer's approval: replace `scripts/signing_certificate.sha1`, update the secrets on every builder,
register the new SHA-1 for Google sign-in, and expect every phone to uninstall once.

## Checks without the shared key

For compilation and tests in an environment without the key, run:

```sh
./gradlew --continue assembleStableDebug :alter-voice:testDebugUnitTest
```

Signing validation will still fail; report that explicitly. Compilation/tests do not establish a signed APK
or an installable update. Do not create a replacement key for local validation. Once the shared key is available,
complete the build and run `python3 scripts/check_apk_signature.py <apk-path>` before checking installation.
