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
  draft. Reopening shows a paused pill; playback/recording and insertion require explicit interaction. The draft is
  excluded from Android backup, bounded to fifteen minutes, and deleted after accepted insertion or explicit discard.
  Android's recognizer provides no recoverable raw audio, so only its recognized text can be retained and continued.
- **Voice processing design:** the yellow/gray thinking lattice fills the pill's rectangle rather than a small orb.
  Reduced motion uses a still frame. Pause/resume sits at the opposite end from the strip's discard cross.

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
