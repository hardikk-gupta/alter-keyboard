# ALTER Keyboard: rules for agents

ALTER Keyboard is FUTO Keyboard with ALTER's voice pack and mic pill on top. This repo holds only the keyboard; it
never depends on the ALTER app being installed. Read `MODIFICATIONS.md`, `docs/DESIGN.md` and `docs/SIGNING.md` first.

## License (FUTO Source First License 1.1)

- Keep `LICENSE.md` and `NOTICE` at the repo root, unchanged. Never alter or remove a FUTO copyright or license notice.
- Keep the line "ALTER Keyboard is a modified version of FUTO Keyboard by FUTO Holdings, Inc." in settings and
  `MODIFICATIONS.md`; record every change to FUTO's behaviour in `MODIFICATIONS.md`.
- Never remove or hide FUTO's payment features: the Payment settings page, `PaymentCompleteActivity` and the payment
  reminders. They may be restyled, never disabled.
- Personal and non-commercial only, shared for free. Never sell it or put it on a paid store listing.
- Every model or library added keeps its own license file under `licenses/`.

## Project layout (must stay a Google AI Studio project)

- Keep AI Studio's layout: `metadata.json`, `settings.gradle.kts`, root `build.gradle.kts`, the keyboard in `app/`
  (`app/build.gradle.kts`, standard `app/src/main` and `app/src/stable`), plus the `alter-voice` and
  `voiceinput-shared` modules. Do not remove `metadata.json` or its `MAJOR_CAPABILITY_SERVER_SIDE_GEMINI_API`;
  `app_name` must equal its `name` ("ALTER Keyboard").
- **No NDK or CMake in Gradle.** AI Studio's container has neither. C/C++ is built by `scripts/build_native_libs.sh`
  (Release, arm64-v8a and x86_64) and the `.so` files are committed to `app/src/main/jniLibs` and
  `alter-voice/src/main/jniLibs`. Change native sources (`app/native/jni`, `alter-voice/native`) only together with
  rebuilt libraries. Never add `externalNativeBuild` back.
- **No Python in builds.** `KeyboardTextsTable.java` is committed; regenerate it with `./gradlew :app:updateLocales`
  after changing layout texts.
- No git submodules and no Git LFS: every file must be an ordinary file in this repo, each under 100 MB.
- Keep AI Studio's signing structure (`release` from `KEYSTORE_PATH`/`STORE_PASSWORD`/`KEY_PASSWORD`,
  `debugConfig` from `debug.keystore` in the repo root). Never commit `local.properties`, `.env`, `*.keystore`, `*.jks`.
- Build with `./gradlew assembleStableDebug`; the APK is `app/build/outputs/apk/stable/debug/app-stable-debug.apk`.

## Identity, signing and updates

- applicationId `com.aistudio.altermind.keyboard`; Java/Kotlin package stays `org.futo.inputmethod.latin`.
- Sign with the shared ALTER key (SHA-1 in `scripts/signing_certificate.sha1`), from `ALTER_SIGNING_KEYSTORE_B64` or a
  git-ignored `debug.keystore`. Never commit a keystore, `.env`, `local.properties` or model weights.
- Version code is minutes since 2026-01-01 UTC of the commit (build time without git), so builds never downgrade.
- FUTO's update checker stays off (`UPDATE_CHECKING = false`): never offer FUTO's APK as an update.

## Design

- The keyboard works exactly like FUTO Keyboard (settings, toolbar actions, themes). Its default skin is ALTER's.
- The app's own screens always use ALTER's settings design (`uix/settings/AlterStyle.kt`); style shared components
  there or in `settings/Components.kt`, not screen by screen. The launcher icon is ALTER's keyboard mark.
- Keep `README.md`, `MODIFICATIONS.md`, `docs/DESIGN.md` and `CHANGES.md` current with every user-visible change.
- Branding: the product is ALTER Keyboard. User-visible text says ALTER, not FUTO, except where FUTO must appear:
  the attribution line (last item of Help, README's License section, release notes), the payment page and payment
  reminders (they pay FUTO and must keep working), the Credits list, `LICENSE.md`, `NOTICE`, `MODIFICATIONS.md` and
  copyright notices in source files. Internal identifiers (package names, file formats) keep their FUTO names.
- Every animation has a still frame when Android's "remove animations" setting is on.
- The ALTER theme is the default skin; FUTO's themes stay selectable. Voice surfaces sit inside the thin AlterYellow
  boundary, and the mic replaces the suggestion row instead of adding a layer (`docs/DESIGN.md`).

## Voice (`alter-voice/`, ported from ALTER's voice pack)

- PCM capture never waits for model warm-up.
- No fake audio level.
- One LiteRT engine at a time, with a fresh engine per cleanup chunk.
- Validate every edit, keep all tail text on timeout, and never strip the word "message".
- Blank or filler-only results are rejected.
- Models download into private no-backup storage on demand from the pinned catalog
  (`alter-voice/src/main/assets/alter_voice_models.json`) with exact size and SHA-256 checks. Never bundle weights.
- Android on-device recognition only on API 31+, never a cloud fallback.
- No recovery cards: on failure the pill shows the red triangle and retries the last recording held in memory.
- Text goes in through the keyboard's input connection (`commitText`), never sent automatically. No accessibility
  service is used.
- Recording stops and is discarded when the field loses focus or the keyboard hides. Password and
  no-personalized-learning fields open the full keyboard with the mic disabled.

## Pull requests

- One PR per step. Create PRs only; never merge them. Hardikk or Chirag merges.
- Append each PR's summary and the checks actually run to `CHANGES.md`.
- Checks for every PR: `./gradlew assembleStableDebug` and the unit tests (`./gradlew :alter-voice:testDebugUnitTest`)
  pass (retry Maven Central HTTP 429 after a pause); `scripts/check_apk_signature.py` passes when the shared key is
  available; nothing secret or any model weight is committed.
- Phone acceptance (typing in real apps, dictation, pill switching, password fields, banking apps, install over the
  previous build) is checked by a person and never claimed from host tests.

## Phone APK

`.github/workflows/build-apk.yml` builds the arm64-v8a APK on every push to `main` and publishes it on the Releases page
(`ALTER-Keyboard-<version>.apk`). The repo cannot hold it as a file: it is over GitHub's 100 MB limit.
