# Changes

Append-only log, one entry per PR, with the checks actually run.

## PR 1: FUTO import, ALTER voice and pill mode (Oct 19, 2026)

Combines handoff steps 1, 3 and 4; step 2 (ALTER restyle) was dropped so the keyboard keeps FUTO's look.

- Imported FUTO Keyboard `master` (`70a5d390c`) with its history and 7 submodules; `LICENSE.md` and `NOTICE` kept.
- applicationId `com.aistudio.altermind.keyboard`, name "ALTER Keyboard", commit-time version code, shared signing
  key wiring, FUTO update checker off. Kotlin 2.1.0 → 2.4.0 (needed by LiteRT-LM 0.18).
- New `alter-voice` module: ALTER's whisper.cpp bridge, Whisper/Moonshine/device speech, dictation controller,
  LFM2.5 cleanup on LiteRT-LM, pinned catalog and in-process downloads. FUTO's 43 MB Whisper model left out of the APK.
- The mic action now runs ALTER's voice in a strip above the keys. Pill mode, start-mode setting, Minimize action,
  password-field rule, Settings → ALTER voice page, About line.
- Added `MODIFICATIONS.md`, `AGENTS.md`, `docs/DESIGN.md`, `docs/SIGNING.md`, `licenses/`,
  `scripts/check_apk_signature.py`.

Checks run (cloud session, no phone):

- `./gradlew assembleStableDebug`: passed (after Maven Central HTTP 429 retries). APK 170 MB with all four ABIs,
  above GitHub's 100 MB limit, so it is not committed (size work is handoff step 5).
- `./gradlew :alter-voice:testDebugUnitTest`: 9 tests passed (ported ALTER voice-pack regressions).
- `scripts/check_apk_signature.py`: **failed as expected**: the shared key was not available in this environment, so
  the APK was signed with FUTO's test key (`5B:36:8C:…:0F:E5`). Rebuild with `ALTER_SIGNING_KEYSTORE_B64` set.
- Not run: rendered preview images (Roborazzi), FUTO's instrumented tests, any phone acceptance check.

## PR 1, follow-up: Google AI Studio layout (Oct 19, 2026)

AI Studio reported "internal error" when importing the branch. It must stay an AI Studio project, so the repo now
uses ALTER's AI Studio layout (`metadata.json`, `settings.gradle.kts`, root `build.gradle.kts`, `app/` module) and
has no submodules: FUTO's submodule contents are ordinary files. Unused parts were removed (unstable/playstore
flavors, FUTO instrumented tests, word lists, dictionary tools, FUTO CI scripts). Kotlin DSL for `app` and
`alter-voice`; FUTO's `voiceinput-shared` keeps its Groovy build file.

Checks run (cloud session, no phone, no AI Studio access):

- `./gradlew assembleStableDebug :alter-voice:testDebugUnitTest`: passed; 9 tests passed.
- The new APK holds exactly the same 1,876 non-code files as the APK before the move, same package and resources.
- Not verified: the AI Studio import itself.

## PR 3: Google AI Studio build rules (Oct 19, 2026)

AI Studio still reported "internal error" after PR 2. Following AI Studio's own requirements: no NDK/CMake in Gradle
(native libraries prebuilt by `scripts/build_native_libs.sh` and committed for arm64-v8a and x86_64), standard
`app/src/main` layout, AI Studio's signing structure, `app_name` = "ALTER Keyboard", minSdk 26, no Python step in
builds (`KeyboardTextsTable.java` committed), no committed keystore, and FUTO's `.gitlab-ci.yml`/`.clang-tidy` removed.

Checks run (cloud session, no phone, no AI Studio access):

- `scripts/build_native_libs.sh`: built both libraries (Release, 16 KB page alignment, JNI entry points present).
- `./gradlew clean assembleStableDebug :alter-voice:testDebugUnitTest` with the NDK and CMake hidden: passed;
  9 tests passed. APK 146 MB, label "ALTER Keyboard", minSdk 26, only arm64-v8a and x86_64 libraries.
- Same non-code APK contents as before, except two library layouts merged by minSdk 26.
- Signed here with a throwaway local `debug.keystore`; not the shared key.
- Not verified: the AI Studio import itself, or the repository visibility / GitHub app access it needs.

## PR 4: phone APK on the Releases page (Oct 19, 2026)

The phone APK (arm64-v8a, 117 MB) is over GitHub's 100 MB file limit, so it cannot be committed. A GitHub Actions
workflow now builds it on every push to `main` (and on pull requests, as a downloadable artifact) and publishes it as
a GitHub Release. `-PalterAbis=arm64-v8a` limits a build to phone ABIs. Signing uses the
`ALTER_SIGNING_KEYSTORE_B64` repository secret; without it the workflow signs with a one-off key and warns.

Checks run: `./gradlew assembleStableDebug -PalterAbis=arm64-v8a` passed locally (117 MB, arm64-v8a only); the
workflow file parses. The workflow itself runs on GitHub after this is pushed.

## PR 5: mic in the suggestion row, ALTER skin, tidier voice settings (Oct 19, 2026)

From phone feedback on the first APK:

- The mic no longer opens a strip above the keyboard. ALTER's voice strip (cancel ✕ + bounded pill) replaces the
  suggestions in the action bar row; pinned actions and keys stay put. Tapping the mic again acts like the strip.
- The pill now has a thin AlterYellow boundary; the thinking orb lights in white and yellow.
- New ALTER theme (`AlterTheme`) is the default skin; FUTO's themes remain in the picker.
- Settings → ALTER voice is grouped into compact cards: Keyboard (start mode, end on silence, language as
  drop-downs), Speech model (current model, the full list collapsed), Cleanup, then storage and the About line.

Checks run: `./gradlew assembleStableDebug -PalterAbis=arm64-v8a :alter-voice:testDebugUnitTest` passed. Not run:
rendered previews or any phone check of the new strip, theme and settings layout.
