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
