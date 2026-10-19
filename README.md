# ALTER Keyboard

ALTER Keyboard is FUTO Keyboard's complete typing keyboard with ALTER's voice pack and ALTER's design built in.
It types into any app through Android's input-method system, so it needs no accessibility service (banking apps such
as YONO SBI have nothing to flag), and all speech recognition runs on the phone.

> ALTER Keyboard is a modified version of FUTO Keyboard by FUTO Holdings, Inc. It is licensed under the
> [FUTO Source First License 1.1](LICENSE.md) (personal, non-commercial use, shared only for free); the original AOSP
> code remains Apache 2.0 ([NOTICE](NOTICE)). Every change from FUTO is listed in [MODIFICATIONS.md](MODIFICATIONS.md).
> FUTO's original README is kept as [docs/FUTO_README.md](docs/FUTO_README.md).

## Install on a phone

1. Open the repo's **Releases** page on the phone (log in to GitHub; the repo is private) and download the newest
   `ALTER-Keyboard-<version>.apk`. Each merge to `main` publishes one (`.github/workflows/build-apk.yml`).
2. Open the file and allow installing from your browser when Android asks.
3. Settings → System → Keyboard → On-screen keyboard → turn on **ALTER Keyboard**, then pick it in any text field.
4. For voice: open ALTER Keyboard's settings → **ALTER voice** → allow the microphone and download a speech model
   (Moonshine Tiny, 45 MB, is the smallest; Whisper models are multilingual).

Updates install over the previous build only when both are signed with the same key. Until the repository secret
`ALTER_SIGNING_KEYSTORE_B64` is set, each build gets a one-off key and the old app must be uninstalled first
(see [docs/SIGNING.md](docs/SIGNING.md)).

## What it does

**Full keyboard (FUTO's):** typing, suggestions and autocorrect, swipe typing, undo/redo, cut/copy/paste, clipboard
history, emoji, text editing, number row, many languages and layouts, themes and resizing. Everything FUTO Keyboard
has, behaving the same way.

**ALTER voice (replaces FUTO's voice input):**

- Tap the mic in the action bar: the suggestion row turns into ALTER's voice strip (a ✕ and the pill), with no extra
  layer above the keys. The pill shows a live yellow wave driven by the real microphone level; tap it to finish.
- The thinking orb (ALTER's S1 lattice, lighting from white to yellow) shows while the speech is transcribed and
  cleaned up; tap to cancel. The text is inserted at the cursor (replacing any selection) and is never sent.
- Errors show a red triangle; tap to retry the same recording, held in memory only.
- Speech models: Moonshine Tiny/Small/Medium Streaming (English) or Whisper Tiny/Base/Small/Medium (multilingual),
  or Android's own on-device recognizer (Android 12+). Optional cleanup: the LFM2.5 350M editor adds punctuation and
  capitals, removes fillers and never answers the text. Models download on demand into private storage from a
  pinned list with exact size and SHA-256 checks; nothing is sent to the cloud.

**Pill mode:** a 56 dp bar with only the mic pill, FUTO's globe (switch input method) and a keyboard button. Choose
the start mode in Settings → ALTER voice (Full keyboard by default), or tap **Minimize** (chevron-down, pinned in the
action bar) at any time. Password fields always open the full keyboard with the mic disabled. Recording stops and is
discarded when the field loses focus or the keyboard hides.

**ALTER design:** the default keyboard skin is the ALTER theme (black, dark grey keys, yellow accent; FUTO's themes
stay in the picker), the app's own screens follow the ALTER app's settings design, and the launcher icon is a light
keyboard with a yellow spacebar on ALTER's dark background. Details: [docs/DESIGN.md](docs/DESIGN.md).

**Not included:** the ALTER agent (chat, memory, actions, Gemini). The keyboard holds only ALTER's voice pack and works
without the ALTER app installed.

## Settings

- **ALTER voice** (top of the settings home): Keyboard card (start mode, end recording after silence, dictation
  language), Speech model card (current model; all models under "All speech models" with download, cancel, use and
  delete), Cleanup card (switch and model), storage used and the About line.
- Every FUTO setting is still there: languages, keyboard and typing, swipe, predictive text, actions, themes, help,
  and FUTO's payment page (kept as FUTO's license requires).

## Build

Requirements: JDK 21, Android SDK Platform 35 and Build Tools 35. No NDK or CMake is needed for the app build.

```
./gradlew assembleStableDebug                       # arm64-v8a + x86_64 (emulators)
./gradlew assembleStableDebug -PalterAbis=arm64-v8a # phone APK, as on the Releases page
./gradlew :alter-voice:testDebugUnitTest            # voice-pack regression tests
```

The APK is `app/build/outputs/apk/stable/debug/app-stable-debug.apk`. Signing uses `ALTER_SIGNING_KEYSTORE_B64` or a
git-ignored `debug.keystore` in the repo root (`android` / `android`, alias `androiddebugkey`); a build without either
fails rather than signing with another key. Maven Central sometimes answers HTTP 429: retry after a pause.

Native code is prebuilt because Google AI Studio has no NDK or CMake. After changing C/C++ sources, install NDK
28.2.13676358 and CMake 3.22.1 and run `scripts/build_native_libs.sh`, which rebuilds and replaces:

- `app/src/main/jniLibs/<abi>/libjni_latinime.so`, FUTO's typing engine, from `app/native/jni`;
- `alter-voice/src/main/jniLibs/<abi>/libalter_whisper.so`, ALTER's whisper.cpp bridge, from `alter-voice/native`.

## Repository layout

| Path | Contents |
| --- | --- |
| `metadata.json`, `settings.gradle.kts`, `build.gradle.kts` | Google AI Studio project files |
| `app/` | The keyboard: FUTO's sources in `app/src/main/java` (package `org.futo.inputmethod.latin`), resources, assets, translations, prebuilt AARs (`app/libs`), FUTO's native sources (`app/native`), locale tools (`app/tools`) |
| `alter-voice/` | ALTER's voice pack (`com.aistudio.altermind.voice`): dictation, speech models, cleanup, model catalog and downloads, the pill, orb and wave |
| `voiceinput-shared/` | FUTO's voice module, still needed by FUTO code |
| `docs/` | `DESIGN.md`, `SIGNING.md`, `FUTO_README.md` |
| `licenses/` | Licenses of everything added on top of FUTO |
| `scripts/` | `build_native_libs.sh`, `check_apk_signature.py`, `signing_certificate.sha1` |
| `.github/workflows/build-apk.yml` | Builds the phone APK and publishes it as a Release |

ALTER's additions in `app/` are marked with "ALTER Keyboard" comments; the main ones are
`uix/actions/AlterVoiceAction.kt` (dictation session, mic and Minimize actions), `uix/UixManager.kt` (voice strip and
pill mode), `uix/AlterSettingKeys.kt`, `uix/theme/presets/AlterTheme.kt`, `uix/settings/AlterStyle.kt` and
`uix/settings/pages/AlterVoice.kt`.

## For contributors and agents

Read [AGENTS.md](AGENTS.md) first (license, signing, AI Studio, design, voice and PR rules). Each PR appends its
checks to [CHANGES.md](CHANGES.md). Phone acceptance is checked by a person and never claimed from host tests.
