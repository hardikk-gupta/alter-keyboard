# ALTER Keyboard

ALTER Keyboard is a complete typing keyboard with ALTER's on-device voice pack and ALTER's design built in. It types
into any app through Android's input-method system, so it needs no accessibility service (banking apps such as YONO
SBI have nothing to flag), and all speech recognition runs on the phone.

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

**Full keyboard:** typing, suggestions and autocorrect, swipe typing, undo/redo, cut/copy/paste, clipboard history,
emoji, text editing, number row, many languages and layouts, themes and resizing.

**ALTER voice:**

- Tap the ALTER flat-eye icon in the action bar: the suggestion row turns into ALTER's voice strip (a ✕ and the
  pill), with no extra layer above the keys. The pill shows a live yellow wave driven by the real microphone level; tap it to finish.
- Pause/resume sits outside the right edge of the pill, opposite the discard cross. Both use toolbar-sized rounded,
  theme-colored icons with large touch areas. Pause uses two simple round-ended lines, matching the cross stroke.
  A still white line marks the paused pill; tap its centre to
  process what is already recorded, or tap Play to keep recording.
- The original AICSS orb motion fills the rectangle with round white/yellow dots during transcription and cleanup;
  tapping the pill or its external Pause button pauses processing.
  With animations off the cells show a still frame. Text is inserted at the cursor and is never sent.
- Closing the keyboard during processing lets transcription and cleanup finish in the background while the keyboard
  service remains alive. The final text is automatically copied to the clipboard and kept in the private draft.
- If a call interrupts the microphone, or the keyboard hides/loses focus during recording, the draft is saved. Reopening a normal
  text field shows a paused pill; it never records or inserts recovered text automatically. Opening the notification
  shade alone does not request a stop; if Android actually hides the keyboard or interrupts capture, the draft pauses.
- One temporary draft is kept in the app's private, excluded-from-backup storage: raw PCM for Whisper/Moonshine,
  recognized text only for Android's recognizer (Android does not expose its source audio). It survives process restarts
  and is removed after accepted insertion or an explicit ✕ discard, with a fifteen-minute audio limit. Resume uses the
  same speech model; reselect it if changed. No recording history is kept. Errors preserve the draft for retry.
- Speech models: Moonshine Tiny/Small/Medium Streaming (English) or Whisper Tiny/Base/Small/Medium (multilingual),
  or Android's own on-device recognizer (Android 12+). Optional cleanup: the LFM2.5 350M editor adds punctuation and
  capitals, removes fillers and never answers the text. Models download on demand into private storage from a
  pinned list with exact size and SHA-256 checks; nothing is sent to the cloud.

**Pill mode:** a 56 dp bar with only the mic pill, a globe (switch input method) and a keyboard button. Choose
the start mode in Settings → ALTER voice (Full keyboard by default), or tap **Minimize** (chevron-down, pinned in the
action bar) at any time. Password and no-personalized-learning fields always open the full keyboard with voice and
recovery disabled. Switching keyboard surfaces pauses recording; finished audio keeps processing to the clipboard.

**ALTER design:** the default keyboard skin is the ALTER theme (black, dark grey keys, yellow accent; the other themes
stay in the picker), the app's own screens follow the ALTER app's settings design, and the launcher icon is a light
keyboard with a yellow spacebar on ALTER's dark background. Details: [docs/DESIGN.md](docs/DESIGN.md).

**Not included:** the ALTER agent (chat, memory, actions, Gemini). The keyboard holds only ALTER's voice pack and works
without the ALTER app installed.

## Settings

The full sitemap and user flows are in [docs/SITEMAP.md](docs/SITEMAP.md).

- **Home** is grouped: Voice, Typing, Look & feel, More. Until voice works, a "Set up ALTER voice" card sits at the
  top, and the ALTER voice row shows its status ("Ready · model" or the missing step).
- **ALTER voice:** numbered setup steps while needed (allow the microphone, download the recommended model, which is
  used as soon as it finishes), then Speech (model in use, dictation language, all models collapsed), Keyboard (start
  mode, end recording after silence), Dictation cleanup, and storage used.
- Languages, keyboard and typing, swipe, predictive text, actions, themes and help, plus the upstream payment page,
  which the license requires to stay.

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

- `app/src/main/jniLibs/<abi>/libjni_latinime.so`, the typing engine, from `app/native/jni`;
- `alter-voice/src/main/jniLibs/<abi>/libalter_whisper.so`, ALTER's whisper.cpp bridge, from `alter-voice/native`.

## Repository layout

| Path | Contents |
| --- | --- |
| `metadata.json`, `settings.gradle.kts`, `build.gradle.kts` | Google AI Studio project files |
| `app/` | The keyboard: sources in `app/src/main/java` (upstream package `org.futo.inputmethod.latin`), resources, assets, translations, prebuilt AARs (`app/libs`), native sources (`app/native`), locale tools (`app/tools`) |
| `alter-voice/` | ALTER's voice pack (`com.aistudio.altermind.voice`): dictation, speech models, cleanup, model catalog and downloads, temporary recording drafts, the pill, processing field and wave |
| `voiceinput-shared/` | Upstream voice module, still needed by the keyboard code |
| `docs/` | `DESIGN.md`, `SITEMAP.md` (sitemap and user flows), `SIGNING.md`, `FUTO_README.md` (upstream README) |
| `licenses/` | Licenses of the added models and libraries |
| `scripts/` | `build_native_libs.sh`, `check_apk_signature.py`, `signing_certificate.sha1` |
| `.github/workflows/build-apk.yml` | Builds the phone APK and publishes it as a Release |

ALTER's additions in `app/` are marked with "ALTER Keyboard" comments; the main ones are
`uix/actions/AlterVoiceAction.kt` (dictation session, ALTER voice and Minimize actions), `uix/UixManager.kt` (voice strip and
pill mode), `uix/AlterSettingKeys.kt`, `uix/theme/presets/AlterTheme.kt`, `uix/settings/AlterStyle.kt` and
`uix/settings/pages/AlterVoice.kt`.

## For contributors and agents

Read [AGENTS.md](AGENTS.md) first (license, signing, AI Studio, design, voice and PR rules). Each PR appends its
checks to [CHANGES.md](CHANGES.md). Phone acceptance is checked by a person and never claimed from host tests.

## License

ALTER Keyboard is a modified version of FUTO Keyboard by FUTO Holdings, Inc., used under the
[FUTO Source First License 1.1](LICENSE.md): personal, non-commercial use, shared only for free. The original AOSP code
remains Apache 2.0 ([NOTICE](NOTICE)). Changes from upstream are listed in [MODIFICATIONS.md](MODIFICATIONS.md); the
upstream README is kept as [docs/FUTO_README.md](docs/FUTO_README.md).

### Shared ALTER dictation core

The voice pipeline is audio → transcription → `alter-agent` dictation policy → optional local cleanup model →
validated text output. The reusable core owns instructions/examples, explicit weekday correction handling,
formatting, edit validation, bounded processing and full-transcript fallback. The keyboard supplies model inference
and handles insertion/clipboard delivery. No main-app installation, chat history, memories, tools or cloud fallback
is required. Cleanup remains opt-in and Hindi mode retains its existing bypass. This is a dictation-specific core,
not the main app's full chat/action agent; the main app has not yet migrated to this module.

Dictation instructions now explicitly cover fillers, false starts, repetition, latest explicit corrections,
faithful grammar/formatting, Hindi/Hinglish language preservation, and protection of names, code and numbers.
They forbid answering/executing spoken requests or inventing context/preferences. This is an instruction update,
not new cleanup levels, context collection or a new model. Existing conservative validation still rejects broader
semantic deletions (such as “tomorrow, actually tonight”); only the existing explicit weekday resolver permits that
limited correction class. Formatting remains bounded by the current formatter. Hindi's model bypass is unchanged.

Whisper live scheduling warms the selected downloaded model when a non-sensitive text field opens (no microphone
or download), targets its first preview after two seconds of captured audio, and spaces later previews according
to measured decoder cost. Full windows and the final tail are always decoded; slow previews never drop audio.
This reduces redundant work but does not guarantee real-time Medium inference. Native CPU binaries and model
weights are unchanged; on-phone speed and thermal behavior still require measurement.

Oriserve Hinglish Swift · Q8 is available under Settings → ALTER voice → All speech models. Download its 82 MB
artifact, then select it to test locally. This is Oriserve's Whisper Base Hindi2Hinglish fine-tune, converted to GGML
by the third-party AirNote repository; it outputs romanized Hinglish and uses fixed Hindi decoding regardless of
the general dictation-language selector. It reuses the existing Whisper runtime, live scheduling, recovery and
ALTER cleanup pipeline. The catalog pins a commit, exact size and SHA-256; no weights are bundled and model
selection is explicit. Apache-2.0 attribution is in licenses/. Accuracy and Android speed require phone testing.

Voice startup shows a still white line while the microphone opens, then the real-RMS yellow wave; it does not
show the processing animation. Processing is reserved for finished audio. Errors take precedence over saved-draft
pause/processing state and hide the Play/Pause control. Ten seconds with no audible speech stops capture with the
error icon, including while the model warms up; the draft remains private and saved. Tapping a no-speech error
starts capture again instead of repeatedly decoding silence. Other recoverable errors can retry saved audio.
ALTER cleanup sends three relevant worked examples instead of all eight (with an instruction-isolation example
always included); editing instructions, validation, full transcript and timeout/fallback rules remain unchanged.
Reduced prompt size is not a measured phone latency improvement.

Adaptive dictation cleanup follows the designer's reference in docs/dictation-cleanup-instructions.md. Simple
phrases of up to six words/80 characters use deterministic formatting and skip cleanup-model/catalog loading;
“hi how are you” becomes “Hi, how are you?” without inference. Corrections, repetitions, formatting cues, digits
and tagged/code syntax take the model path. Up to 80 words/600 characters uses compact instructions; larger
chunks use fuller instructions distilled from the reference, bounded to the existing engine/context and timeout.
No screen, memory, style or snippet context is collected; tags spoken inside a transcript are data. Opt-in/model
availability and Hindi bypass still apply. Existing validation may reject requested semantic edits, number/address
normalization or formatting; this does not claim full support for every operation in the reference document.

Oriserve Hinglish Swift · FP16 (148 MB) is also available for explicit comparison with Q8. It uses the same pinned
conversion source and Hindi decoding. The uploader recommends FP16 for fidelity, but recognition errors such as
“notebook”/“loadbook” are not proven to come from quantization. Existing selections are preserved; no blind word
substitution or automatic learning is enabled. Phone comparisons are still required.

Settings → ALTER voice → Learned words lets users explicitly save/delete single-word corrections. Up to 100
confirmed pairs are stored atomically in private no-backup storage and applied in one whole-word pass before
optional cleanup, including when cleanup is disabled. Numbers/negations are protected; URLs, email addresses and
identifiers are not rewritten. Rules apply globally, including legitimate uses of the source word; delete a rule
when inappropriate. This is visible correction memory, not model training, editor monitoring or native ASR biasing.
