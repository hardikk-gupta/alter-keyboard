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
- Handy native changes (`alter-voice/native-handy`) require rebuilding both committed ABIs with
  `scripts/build_handy_native.sh`; never bundle model weights or add runtime downloads to Gradle.
- Parakeet TDT FP16 shares `alter-voice/native-nemotron`; keep offline sessions separate from the
  streaming path, require successful uncancelled results, and rebuild both ABI libraries.
- Nemotron native changes (`alter-voice/native-nemotron`) require rebuilding both committed ABIs with
  `scripts/build_nemotron_native.sh`; keep runtime symbols isolated from Whisper and Handy.
- **No NDK or CMake in Gradle.** AI Studio's container has neither. C/C++ is built by `scripts/build_native_libs.sh`
  (Release, arm64-v8a and x86_64) and the `.so` files are committed to `app/src/main/jniLibs` and
  `alter-voice/src/main/jniLibs`. Change native sources (`app/native/jni`, `alter-voice/native*`) only together with
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
- Keep `README.md`, `MODIFICATIONS.md`, `docs/DESIGN.md`, `docs/SITEMAP.md` and `CHANGES.md` current with every
  user-visible change. New settings go into the matching home group; keep the primary task (voice) first.
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
- One LiteRT engine at a time, with a fresh engine per cleanup chunk. Handy is opt-in and uses its own pinned
  CPU llama.cpp library; keep fresh contexts, empty-system ChatML framing, aborts and exact-final snapshot reuse.
- Validate every edit, keep all tail text on timeout, and never strip the word "message".
- Blank or filler-only results are rejected.
- Models download into private no-backup storage on demand from the pinned catalog
  (`alter-voice/src/main/assets/alter_voice_models.json`) with exact size and SHA-256 checks. Never bundle weights.
- Nemotron is experimental and opt-in: preserve the existing selection, require successful finalization,
  keep every PCM sample on retry, and never describe its 320 ms model profile as measured phone latency.
  Native Hindi script is Romanized with ICU4J before live/final cleanup; preserve Latin text verbatim.
  This is script conversion, not model training or an accuracy improvement. Phone speed remains unverified.
- Parakeet 110M uses the pinned sherpa-onnx static-ORT JAR/JNI pair; refresh them together with
  `scripts/import_sherpa_runtime.py`. Preserve archive and component hashes, whitelist extraction,
  and bounded-window cancellation checks. English-only; previews are not native streaming.
- Android on-device recognition only on API 31+, never a cloud fallback.
- No recovery cards: interrupted recording/processing returns as a paused pill. Keep one private temporary draft
  in `noBackupFilesDir/dictation-draft`; delete it only after accepted text insertion or explicit discard.
  Downloaded recognizers journal raw PCM; Android on-device recognition can preserve only its recognized text.
- Text goes in through the keyboard's input connection (`commitText`), never sent automatically. No accessibility
  service is used.
- Losing focus or hiding the keyboard pauses capture and preserves the draft. Finished audio keeps processing in
  the IME service scope and copies completed text to the clipboard, retaining the private draft. Reuse the active
  controller when reopening; never resume the mic or insert recovered text automatically. Password and no-personalized-learning fields disable voice and hide recovery.
- Put pause/resume outside the pill's right edge, opposite the external discard cross. Both controls use the same
  simple 2-unit strokes with round caps/joins and theme tint, with full-size touch areas. Pause is two single
  vertical strokes; paused status inside the pill is a still white line. Thinking reuses the original AICSS S1 orb motion
  across the rectangle, with a static frame when animations are disabled.

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
Each run then deletes every older `build-N` release (with its tag) and older APK artifacts, so only the newest APK
is kept. Do not rely on older releases existing.

## Hindi mode routing

- Explicit Hindi mode uses IndicConformer for saved Parakeet CTC/TDT and Nemotron choices;
  keep the saved base choice and display the actual effective engine. Automatic Parakeet routing
  requires the explicit detector/Hindi bundle; Nemotron retains native multilingual Automatic.
- Require both pinned Hindi files, stage and verify the metadata-only derived graph, retain
  immutable downloaded bytes, and never call native inference on an unverified adaptation.
- Shared ICU Romanization preserves already-Latin content. Do not label it IndicXlit/Sanscript
  or model fine-tuning; natural Hinglish/code-switching remain unverified. Guard active language
  changes and keep drafts associated with their actual effective model ID.

## Automatic English/Hindi routing

- Both Parakeet variants use the pinned Whisper ONNX language detector in Automatic mode.
  Tiny is for LID only, not transcription; never invent confidence probabilities or infer language
  from pitch/energy. Sparse probes can misclassify short/mixed speech; retain clear limits in UI/docs.
- Decoder receives every full PCM window; classifier gets copies only. Missing detector/Hindi
  assets block automatic capture. Mixed labels require installed Nemotron or preserve a draft error;
  unknown/short speech may retain the English base when no multilingual model is installed.
- Automatic drafts include the routing policy and base in sessionModelId. Do not equate them
  with explicit-English drafts or change an active session policy. Caches can stay across route
  switches but must release on explicit selection/deletion/release. Keep only one active ASR decoder.
