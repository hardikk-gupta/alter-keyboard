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

## Identity, signing and updates

- applicationId `com.aistudio.altermind.keyboard`; Java/Kotlin package stays `org.futo.inputmethod.latin`.
- Sign with the shared ALTER key (SHA-1 in `scripts/signing_certificate.sha1`), from `ALTER_SIGNING_KEYSTORE_B64` or a
  git-ignored `debug.keystore`. Never commit a keystore, `.env`, `local.properties` or model weights.
- Version code is minutes since 2026-01-01 UTC of the commit (build time without git), so builds never downgrade.
- FUTO's update checker stays off (`UPDATE_CHECKING = false`): never offer FUTO's APK as an update.

## Design

- The keyboard looks and works exactly like FUTO Keyboard. Do not restyle FUTO's keys, themes, toolbar or settings.
- Only ALTER's voice surfaces (the pill, its bar and its settings page) use ALTER's tokens; see `docs/DESIGN.md`.
- Every animation has a still frame when Android's "remove animations" setting is on. No outline on the pill.

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
