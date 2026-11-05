# ALTER Keyboard: rules for agents

ALTER Keyboard is FUTO Keyboard with ALTER's voice pack and mic pill on top. This repo holds only the keyboard; it
never depends on the ALTER app being installed. Read `HANDOFF.md`, `PLAN.md`, `AGENT_INSTRUCTIONS.md`,
`MODIFICATIONS.md`, `docs/DESIGN.md` and `docs/SIGNING.md` first.

## Mandatory documentation and handoff workflow

- Treat documentation as part of every code, configuration and planning change. **Review all repository
  Markdown and update every affected first-party MD file immediately after each logical change, in the
  same commit/PR.** Do not defer it to the end of a session, a follow-up task or an uncommitted chat summary.
- Before implementation, write the task goal, plan, proposed changes, dependencies and validation in PLAN.md.
  As work changes, record decisions, implemented changes, pending work and blockers there immediately.
- Keep HANDOFF.md current with branch/PR vs main status, implementation state, checks actually run, limitations
  and concrete next actions. All essential handoff files must be tracked in this repository and pushed to GitHub.
- Append each change's purpose, outcome and actual checks to CHANGES.md. Keep historical entries append-only.
- Reconcile README.md, MODIFICATIONS.md, design/sitemap/model guides, module READMEs, runtime source notes
  and agent instructions whenever their content is affected. Record task-level plans/decisions, not private reasoning.
- Maintain docs/MARKDOWN_INDEX.md as the complete Markdown inventory. Add/remove/recategorize entries with
  the files; review unaffected entries too. Preserve immutable licenses, upstream/vendor documentation and
  historical evidence; label retired features instead of presenting them as active or cosmetically rewriting all files.
- Use canonical filenames: AGENTS.md, AGENT_INSTRUCTIONS.md, HANDOFF.md, PLAN.md and CHANGES.md.
  Do not create conflicting lowercase copies. This rule supplements all technical/license rules below.
- Before commit/handoff, validate links, inventory completeness and consistency. Missing documentation makes
  the change unfinished. Documentation-only edits need appropriate documentation checks, not fabricated
  build/device claims. Never store secrets, signing keys, model weights or private recordings in handoff documents.

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
- Whisper native changes (`alter-voice/native`) require `scripts/build_whisper_native.sh`, which rebuilds all three
  arm64 feature-level libraries and x86_64 together.
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
- One LiteRT engine at a time, with a fresh engine per cleanup chunk.
- Validate every edit, keep all tail text on timeout, and never strip the word "message".
- Blank or filler-only results are rejected.
- Models download into private no-backup storage on demand from the pinned catalog
  (`alter-voice/src/main/assets/alter_voice_models.json`) with exact size and SHA-256 checks. Never bundle weights.
- Nemotron is experimental and opt-in: preserve the existing selection, require successful finalization,
  keep every PCM sample on retry, and never describe its 320 ms model profile as measured phone latency.
  Keep the recognizer's native script, without transliteration. Phone speed remains unverified.
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

## Retained voice models

- Offer only Oriserve Hinglish Apex, Parakeet CTC/TDT, Nemotron, Moonshine Medium and Android on-device
  recognition. Qwen3 1.7B INT8 is the only optional editor (it also cleans Hindi); ignore retired cleanup preferences.
- Do not restore external language routing, Hindi helper assets or either Romanization layer.
  Preserve the selected recognizer's native script. Parakeet/Moonshine remain English-only.
- Apex decodes with the Hindi language token, greedy, flash attention, no temperature fallback and the full 30 s
  encoder window (shorter windows make it loop). Rebuild the whisper bridge only with
  `scripts/build_whisper_native.sh`, which produces the baseline, dotprod and i8mm arm64 libraries together.
- Keep settings concise: names, size/selection, download/delete; no explanation paragraphs or duplicate setup rows.
  Share spacing through Components.kt; rows with no icon must not reserve an icon column.
- Remove retired downloads only under idle speechSetup ownership, preserving retained files and drafts.
  Removed-model PCM may be explicitly reprocessed, never appended under a different recording policy.
- The system recognizer owns its tones. Avoid redundant cancel after successful results; never claim silent
  phone behavior from host tests or globally mute other phone audio to hide a provider beep.

## Optional shared AI contract gate

Only implement Keyboard's side. Before cross-app IPC, retrieve and pin the Alter AI agent's exact contract
revision; never invent a second AIDL/schema. If unavailable, document inspection/dependency instead. When the SHA/PR/file paths arrive, inspect that
exact revision and report compatibility issues before implementing against it.
Keep standalone operation, FUTO policy, private fields/drafts and local settings intact. Dictation gains no
memory/tool scopes. Signer configuration is not installed-certificate evidence; fail closed without approved
trust. Do not copy FUTO-derived code into companions. Pinned contract: `13b3fcd3802849751dc12793ee32a05ab1c9768d` (protocol 1.0). Preserve the full
`shared-local-ai/v1` directory unchanged. Current model-identity clarification and adapter status:
docs/SHARED_LOCAL_AI.md. No active inference service is implemented by the trust groundwork.
