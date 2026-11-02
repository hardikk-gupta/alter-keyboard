# Development and verification

Read [AGENTS.md](../AGENTS.md), [HANDOFF.md](../HANDOFF.md) and [SIGNING.md](SIGNING.md).
Project modules: `app`, `alter-voice`, `alter-agent`, `voiceinput-shared`. Use the committed Gradle wrapper;
keep Google AI Studio layout, identity, FUTO attribution and payment behavior unchanged.

## Android build

Install JDK 21, Android SDK Platform 35 and Build Tools 35; configure `JAVA_HOME`/`ANDROID_HOME` for those
installations. The Gradle wrapper supplies the project's Gradle version. An environment may provide an
activation script, but no `/workspace` or `/tmp` script is required by the repository build instructions.
Keep machine-specific `local.properties`, keystores and credentials out of Git.

```sh
./gradlew assembleStableDebug
./gradlew assembleStableDebug -PalterAbis=arm64-v8a
./gradlew :alter-voice:testDebugUnitTest
```

APK: `app/build/outputs/apk/stable/debug/app-stable-debug.apk`.
When the shared signing key is absent, this diagnostic invocation allows independent checks to continue:

```sh
./gradlew --continue assembleStableDebug :alter-voice:testDebugUnitTest
```

It still fails signing and does not establish an installable update. Supply the authorized shared key
as described in SIGNING.md; do not create a replacement key for local validation.
With a signed APK, run `python3 scripts/check_apk_signature.py PATH_TO_APK` before update-install checks.
The existing GitHub APK workflow can generate a one-off key if the shared secret is absent; that fallback
cannot update an app signed with a different key. A successful CI build is not proof of key continuity.

## Native changes

Normal Gradle builds use committed arm64-v8a/x86_64 prebuilts and do not invoke NDK, CMake or Python.
For native source changes only, install NDK `28.2.13676358` and CMake `3.22.1` under the SDK:

```sh
scripts/build_nemotron_native.sh
```

This rebuilds both ABI libraries for Nemotron and the shared offline Parakeet TDT adapter. Commit both
binaries with the native source change; retain source pins and license notices. The aggregate
`scripts/build_native_libs.sh` also includes typing and archived Whisper/Handy runtimes. Those archived
voice libraries are excluded from the current APK and must not be re-enabled accidentally.
The pinned sherpa JAR/JNI pair is imported together by `scripts/import_sherpa_runtime.py` when that runtime changes.

## Model and host checks

Download models only at runtime into private storage using the pinned catalog. Do not put weights in
Git, Gradle or the APK. [VOICE_MODELS.md](VOICE_MODELS.md) defines the current supported choices.
[STREAMING_RECOGNIZER_CHECKS.md](STREAMING_RECOGNIZER_CHECKS.md) describes the optional host-native
probe and its exact SDK source pin. Host reproduction may need temporary models/tooling, but the
instructions, source pins, test code, results and limitations must remain committed.

Phone acceptance separately covers real microphone capture, language behavior, errors, paused draft recovery,
background completion, private fields, typing/insertion, signing and update installation. No host test establishes
phone latency, accuracy or silent system-provider behavior.

## Documentation-only changes

Review [the inventory](MARKDOWN_INDEX.md), check relative links/paths and `git diff --check`, and ensure active
instructions match code. Do not rewrite license/upstream/history files merely to produce a diff. Record the
review and actual checks in HANDOFF.md/PLAN.md/CHANGES.md. No Android test rerun is needed when runtime,
configuration, models and native binaries are unchanged.
