# Modifications

ALTER Keyboard is a modified version of FUTO Keyboard by FUTO Holdings, Inc.

It is based on [FUTO Keyboard](https://github.com/futo-org/android-keyboard) (imported with its history from `master`
at `70a5d390c`) and is distributed under the same [FUTO Source First License 1.1](LICENSE.md): personal,
non-commercial use, shared only for free. The original AOSP code remains under Apache 2.0 (see [NOTICE](NOTICE)).
`LICENSE.md`, `NOTICE` and every FUTO copyright and license notice in source files are kept unchanged.

## What was changed

- **Identity:** application ID `com.aistudio.altermind.keyboard`; visible app and keyboard name "ALTER Keyboard"
  (`java/res/values/strings-appname.xml`). The Java/Kotlin package `org.futo.inputmethod.latin` is unchanged.
- **Versioning and signing:** version code is minutes since 2026-01-01 UTC of the source commit (build time without
  git); debug builds are signed with the shared ALTER key (`docs/SIGNING.md`). FUTO's release signing setup is unused.
- **Updates:** FUTO's update checker is disabled (`UPDATE_CHECKING = false`) so ALTER Keyboard never offers FUTO's APK
  as an update.
- **Kotlin:** the project builds with Kotlin 2.4.0 (was 2.1.0), required by LiteRT-LM 0.18. One type annotation in
  `BuggyBaseInputConnection.kt` was adjusted for the newer compiler.
- **Voice input:** FUTO's built-in Whisper voice input is replaced by ALTER's voice pack in the new `alter-voice/`
  module (Whisper via whisper.cpp, Moonshine, Android on-device recognition, and the optional LFM2.5 cleanup model).
  FUTO's bundled 43 MB Whisper model is left out of the APK. The option to use another installed voice keyboard is kept.
  The `voiceinput-shared` module is still built because other FUTO code depends on it.
- **Pill mode:** a minimized 56 dp bar with ALTER's mic pill, a keyboard button and FUTO's input-method globe, plus a
  "Minimize" action on the full keyboard and a start-mode setting. Password fields always open the full keyboard
  with the mic disabled.
- **Settings:** an "ALTER voice" page replaces the voice input entry on the settings home screen. The help page and
  the ALTER voice page carry the line "ALTER Keyboard is a modified version of FUTO Keyboard by FUTO Holdings, Inc."

- **Project layout (Google AI Studio):** the repo uses AI Studio's Android layout: `metadata.json`,
  `settings.gradle.kts`, a root `build.gradle.kts` and the keyboard as the `app/` module (`app/build.gradle.kts`,
  converted from FUTO's Groovy `build.gradle`; FUTO's translation filter is kept as `app/translations.gradle`).
  FUTO's sources moved under `app/` unchanged (`app/java`, `app/common`, `app/native`, `app/tools`, `app/translations`,
  `app/libs`). FUTO's seven git submodules are included as ordinary files, except FUTO's Whisper model. Only the
  `stable` flavor is kept; the `unstable` and `playstore` flavors, FUTO's instrumented tests (`tests/`), the source
  word lists (`dictionaries/`), the dictionary tools and FUTO's CI/upload scripts were removed from this repo.

- **Google AI Studio build rules:** sources use the standard `app/src/main` / `app/src/stable` layout (FUTO's
  `java/src` and `common/src` merged into `app/src/main/java`, unchanged). AI Studio has no NDK or CMake, so FUTO's
  native typing engine (`libjni_latinime.so`) and ALTER's whisper.cpp bridge (`libalter_whisper.so`) are built from
  their unchanged sources by `scripts/build_native_libs.sh` and committed as prebuilt libraries for arm64-v8a and
  x86_64 only. The generated `KeyboardTextsTable.java` is committed so builds need no Python. minSdk is 26 (Android
  8.0). FUTO's committed test signing key, its Play Store icon, `.gitlab-ci.yml` and `.clang-tidy` were removed.
  Debug and release builds use AI Studio's signing structure; release builds are no longer minified.

- **Look:** the default keyboard skin is the new ALTER theme (`AlterTheme`); FUTO's themes stay selectable. The app's
  own screens always use ALTER's settings design (`uix/settings/AlterStyle.kt`, shared `Components.kt` styling, grouped
  menus). The launcher icon is ALTER's keyboard mark instead of FUTO's (also on the setup screen and the training
  notification).
- **Voice in the full keyboard:** the ALTER flat-eye voice action (replacing the microphone icon) turns the
  suggestion row of FUTO's action bar into ALTER's voice strip (`ActionBar` gained a `voiceStrip` slot); FUTO's above-keyboard voice window is not used.
- **README:** this repo's `README.md` describes ALTER Keyboard; FUTO's original README is kept unchanged as
  `docs/FUTO_README.md`.

- **Branding:** user-visible text says ALTER Keyboard instead of FUTO Keyboard (setup, crash, version-warning and
  dev-build messages, the "VI" theme name, the activation announcement) in English and all translations. FUTO's
  support links (website, docs, Discord, chat, GitHub issues, email) were removed from Help; the attribution line is
  the last item of Help. Localized app names say ALTER. FUTO's manual-update notice (F-Droid, keyboard.futo.tech) is
  not shown, and crash reports are off unless `crashreporting.properties` configures them, since FUTO's crash dialog
  mails reports to FUTO. The payment page and reminders, the Credits list and internal identifiers keep FUTO's name.

- **Settings structure:** the home is grouped (Voice, Typing, Look & feel, More) with a voice setup card and status,
  FUTO's "check for updates" item is not shown, and the ALTER voice page follows the setup flow (see
  `docs/SITEMAP.md`). Every FUTO page is still reachable, including the payment page.

- **Recoverable dictation:** captures for Whisper/Moonshine append PCM to one private temporary draft before
  decoding. Pause/resume, keyboard hide, focus changes, process restart and microphone interruptions preserve that
  draft. Hiding the keyboard pauses capture, but finished audio keeps processing in the IME service scope and
  automatically copies its final text to the clipboard, retaining the private draft. Reopening reuses active processing
  or shows a paused pill; playback/recording and insertion require explicit interaction. The draft is
  excluded from Android backup, bounded to fifteen minutes, and deleted after accepted insertion or explicit discard.
  Android's recognizer provides no recoverable raw audio, so only its recognized text can be retained and continued.
- **Voice processing design:** the original AICSS S1 orb choreography fills the pill's rectangle with round
  white/yellow dots. Reduced motion uses a still frame. Pause/resume sits outside the right-hand curve, opposite
  the discard cross; both use matching 20dp icons, theme tint and 48dp-wide touch areas. Like the cross, Play/Pause
  uses simple 2-unit strokes with round caps/joins; Pause is two vertical lines. Paused status is a still white line.

## What was not changed

FUTO's keyboard look and behaviour are kept as built: themes, layouts, typing, suggestions, swipe, clipboard, emoji,
text editing, languages and settings. FUTO's payment features (the Payment settings page, `PaymentCompleteActivity`
and the payment reminders) are kept and working.

## Third-party components added

Each keeps its own license under [`licenses/`](licenses):

| Component | License |
| --- | --- |
| whisper.cpp 1.9.5 (CPU subset, `alter-voice/src/main/cpp/whisper`) | MIT |
| Moonshine Voice 0.1.5 and Moonshine models | MIT |
| LiteRT-LM 0.18.0 | Apache 2.0 |
| LFM2.5 350M INT8 cleanup model (downloaded on demand, never bundled) | LFM Open License v1.0 |
| Lucide icon paths | ISC |
| AICSS "Orbs" S1 thinking lattice (native re-implementation) | MIT |

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

### Experimental live Handy cleanup

ALTER voice → Cleanup editor offers Handy Editor 350M Q4_K_M (229 MB) alongside the existing LiteRT editor.
Download, select and enable it explicitly. Oriserve Hinglish Swift FP16 remains the user's ASR choice; no Tiny
model is substituted. Handy runs with a pinned CPU-only llama.cpp JNI library, an explicit empty system turn,
greedy decoding and fresh contexts; its trained input format replaces the generic editor instructions. It is
English-trained; Hinglish quality and Android latency remain unverified. License/provenance are under licenses/.

Enabled cleanup can run during recording after a 1.2-second ASR update debounce. Each provisional edit sees the
current whole transcript (up to 4,000 characters), so later words can revise earlier wording; obsolete work is
cancelled, no PCM is dropped, and no text is inserted during recording. Finalization reuses a cleaned snapshot
only if the final normalized ASR text matches exactly; otherwise it runs final cleanup. Errors preserve raw text.
Raw final ASR text is stored separately from polished text in the existing private no-backup draft until accepted
insertion/discard; older drafts remain readable. Explicit Pause/hide cancels capture/provisional work as before.

The validator now accepts narrowly resolved weekday changes such as “Friday. Actually, make it Monday” and
“Friday, no wait, Monday”, while rejecting unrelated dates/numbers and protecting English/Roman-Hindi negation.
This is not general semantic deletion permission. Handy's real host test did not resolve the tested Hinglish
kal/parso correction; unsafe negation removal falls back to the source. The existing agent rules continue for
the stock editor. Future app context/memory/tool access is not granted by dictated text.

### Experimental Nemotron streaming speech

Voice settings → All speech models includes **Nemotron 3.5 Streaming · Q4 · experimental**
(496 MB download), alongside the existing Whisper, Oriserve and Moonshine choices. It uses
a separate CPU runtime on Android 8+, with live revisions and a final flush; the current
selection stays unchanged. Hindi may appear in Devanagari or mixed script. The 320 ms
streaming profile is a model setting, not measured phone latency. Speed and accuracy against
Moonshine still require phone testing. See [Nemotron experiment](docs/NEMOTRON_EXPERIMENT.md).
