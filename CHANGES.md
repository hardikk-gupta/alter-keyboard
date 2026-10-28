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

## PR 6: ALTER app icon, ALTER app screens, full documentation (Oct 19, 2026)

- New launcher icon: a light keyboard with a yellow spacebar on `#121212` (adaptive, with a monochrome layer). Also on
  the setup screen and the training notification.
- The app's screens follow the ALTER app's settings design whatever keyboard theme is picked: ALTER colours, 26sp
  page headings with a Lucide back arrow, grouped 20dp SlateGrayCard sections, 16sp/12sp rows, 36dp icon tiles with a
  chevron, neutral tips and download buttons.
- `README.md` rewritten for ALTER Keyboard (install, features, settings, build, layout); FUTO's README kept as
  `docs/FUTO_README.md`. `docs/DESIGN.md`, `MODIFICATIONS.md` and `AGENTS.md` updated.

Checks run: `./gradlew assembleStableDebug -PalterAbis=arm64-v8a :alter-voice:testDebugUnitTest` passed; the launcher
foreground was rendered to an image and checked. Not run: screenshots of the settings screens or any phone check.

## PR 7: ALTER branding, FUTO kept to the required corner (Oct 19, 2026)

- User-visible "FUTO Keyboard" text now says ALTER Keyboard (setup, crash, newer-version and dev-build messages, VI
  theme name, activation announcement), in English and all translations (only those strings are touched).
- Help no longer links to FUTO's website, docs, Discord, chat, GitHub issues or email; the attribution line is the last
  item of Help, and it was removed from the ALTER voice page footer.
- README opens as ALTER Keyboard; FUTO appears only in its License section and the upstream README link.
- Kept, as the license requires: payment page and reminders, Credits, LICENSE.md, NOTICE, MODIFICATIONS.md, copyright
  notices, the release-notes attribution line.

Checks run: `./gradlew assembleStableDebug -PalterAbis=arm64-v8a :alter-voice:testDebugUnitTest` passed (9 tests). Not run: phone check.
- Review fixes: localized app names (shown in Android's input-method picker) say ALTER in every language, including
  setup text without a space before the keyboard word; FUTO's update notice with its keyboard.futo.tech button is gone
  from the settings home; crash reporting (which mails FUTO) stays off unless configured. Re-ran the checks above plus
  `compileStableReleaseKotlin`: passed.

## PR 8: settings sitemap and user flows (Oct 19, 2026)

UI/UX pass using the UI/UX Pro Max skill's `ux` guidelines (primary task first, progressive disclosure, grouped
navigation, empty states with guidance, multi-step progress, 48 dp targets):

- Settings home grouped into Voice, Typing, Look & feel and More; FUTO's "check for updates" item removed; a "Set up
  ALTER voice" card with one button until voice works; the ALTER voice row shows live status.
- ALTER voice page reordered to the setup flow: numbered steps (allow microphone, download the recommended model,
  auto-used when done), then Speech (with language), Keyboard, Cleanup.
- New `docs/SITEMAP.md` (sitemap, first-run, dictation and pill-mode flows, rules applied); README, AGENTS and
  MODIFICATIONS updated.

Checks run: `./gradlew assembleStableDebug -PalterAbis=arm64-v8a :alter-voice:testDebugUnitTest` passed (9 tests). Not run: phone check.

## ALTER flat-eye voice action (Oct 19, 2026)

The full keyboard's ALTER voice action now uses the existing flat-eye mark (`ic_alter_ai_doodle_closed`) instead of
its microphone icon. The selected keyboard theme still supplies its tint; the voice-input accessibility label and
tap behavior stay the same. Based on latest `main` at `0db147354`.

Checks run: `./gradlew --no-daemon --max-workers=4 --continue assembleStableDebug :alter-voice:testDebugUnitTest`:
app Kotlin/Java compilation, resources and dexing completed; all 9 voice tests passed (no failures or skips).
APK assembly failed only at `:app:validateSigningStableDebug` because the shared signing key is unavailable.
APK signature verification and phone checks were not run. `git diff --check` passed.

## Keep only the newest APK release (Oct 19, 2026)

`build-apk.yml`: after publishing a release on `main`, the workflow deletes every older `build-N` release with its tag
and every APK artifact from earlier runs; new artifacts expire after 3 days. Asked for to save space on GitHub.
AGENTS.md notes it.

Checks run: workflow YAML parsed (`python3 -c 'import yaml'`). The cleanup itself runs on the first `main` build after
merge.

## Recoverable dictation and full-width thinking field (Oct 21, 2026)

- Whisper/Moonshine capture appends PCM to one private temporary draft before decoding. Pause/resume, a lost input
  field, a hidden keyboard, calls/microphone interruptions and process recreation preserve captured audio. Reopening
  shows a paused pill without opening the microphone or inserting text; Play continues capture and the centre
  processes the saved draft. Switching keyboard surfaces preserves the draft too.
- Pause/resume is at the far right of the rectangle, separated from the strip's discard cross. Explicit discard or
  accepted editor insertion removes the draft. A refused insertion or late result keeps it for recovery; protected
  fields hide recovery and disable voice. The app-private draft is excluded from Android backup, with a fifteen-minute
  PCM limit and bounded atomic metadata. Android's recognizer can recover only delivered text, which its model
  description now explains.
- Thinking uses yellow/gray cells across the entire pill, with a still frame for reduced motion. No new dependencies,
  models or native libraries were added. Agent instructions and user/design/flow documentation reflect the new behavior.

Checks run: `./gradlew --no-daemon --max-workers=4 --continue assembleStableDebug :alter-voice:testDebugUnitTest`.
All 20 tests passed (9 existing, 11 recovery/animation tests; no failures or skips). App Kotlin/Java compilation,
resources and dexing completed; APK assembly failed only at `validateSigningStableDebug` because the shared signing
keystore is unavailable. `git diff --check` passed. APK signature verification and real-phone checks (calls,
notification shade, Back/field switches, process kill, touch targets and accessibility) were not run.

## External voice controls and original orb motion (Oct 21, 2026)

From phone screenshots: Play/Pause now sits outside the pill's right-hand curve, opposite the external discard cross.
The two controls share 18dp icons, rounded 2-unit strokes, keyboard-theme tint, ripple shape and 48dp-wide hit areas
(the full available bar height). Pause also remains available outside the border while processing. The fixed black
pill-mode bar keeps light controls. The paused status marker is also smaller.

The processing rectangle now reuses the original AICSS S1 orb choreography and round white/yellow dots across its
width, replacing the newer square-cell wave. Existing small orbs retain their default 3×3 shape and timing; reduced
motion uses static centre dots. Documentation and animation regression checks are updated.

Checks run: `./gradlew --no-daemon --max-workers=4 --continue assembleStableDebug :alter-voice:testDebugUnitTest`.
All 20 tests passed, with no failures or skips; app Kotlin/Java compilation, resources and dexing completed.
APK assembly failed only at signing validation because the shared keystore is unavailable. `git diff --check` passed.
APK signature verification and real-phone visual/accessibility checks were not run.

## Toolbar-sized rounded Play/Pause icons (Oct 21, 2026)

Play/Pause now uses the toolbar's 20dp icon size and fills more of its 24-unit viewport, fixing the visibly undersized
symbols. Pause uses outlined bars with explicit 2-unit corner radii; Play uses a curved triangle. Both keep 2-unit
strokes and the keyboard theme tint. The shared discard control also uses 20dp; external positions and 48dp-wide
touch areas are unchanged. User and design documentation reflects the corrected sizing and geometry.

Checks run: `./gradlew --no-daemon --max-workers=4 --continue assembleStableDebug :alter-voice:testDebugUnitTest`.
All 20 tests passed, with no failures, errors or skips; app compilation, resources and dexing completed. APK assembly
failed only at signing validation because the shared keystore is unavailable. `git diff --check` passed.
APK signature verification and real-phone visual/accessibility checks were not run.

## Simple voice controls and background clipboard completion (Oct 21, 2026)

Pause now uses two single round-ended strokes, matching the discard cross's 2-unit line style. Play is a simple
triangle outline with round joins. Both remain 20dp, outside the pill. A paused pill shows a still white line instead
of another Pause icon; the live wave stays yellow and processing keeps the existing full-width orb motion.

Closing the keyboard pauses pending/live microphone capture, but lets finished audio continue transcription and
cleanup in the IME service scope. The detached session copies its final text to the system clipboard and preserves
the completed private draft. Reopening reuses the active session, preventing competing controllers over the same
draft; background results never enter a new editor automatically. Explicit Pause still stops processing. Service
teardown stops work and leaves the draft recoverable. Agent, user, design and flow documentation is updated.

Checks run: `./gradlew --no-daemon --max-workers=4 --continue assembleStableDebug :alter-voice:testDebugUnitTest`.
All 23 tests passed, with no failures, errors or skips, including new cases for pending/live capture on hide,
background cleanup completion, and durable completed-text recovery. App compilation, resources and dexing completed.
APK assembly failed only at signing validation because the shared keystore is unavailable. `git diff --check` passed.
Real-phone visual, clipboard, lifecycle and accessibility checks and APK signature verification were not run.

## Reusable ALTER dictation core (Oct 24, 2026)

Extracted transcript instructions/examples, formatting, model request data, validation and bounded cleanup orchestration
into `alter-agent`. The keyboard's voice adapter now calls `AlterDictationAgent.process`, supplying local inference;
the core has no app/UI/model-engine dependency and needs no main-app installation. Reviewed the main ALTER repo's
agent and dictation architecture as provenance; this is its restricted dictation policy pattern, not the full chat/tool
agent. The main app has not yet migrated to this shared module. Existing opt-in/model/Hindi behavior remains.

Explicit weekday replacements such as “Friday actually Monday sorry” are resolved before model editing. Ambiguous
alternatives remain intact; other content, numbers and negations retain validation. Exceptions retain the original
transcript. No broader semantic-correction/model-quality claim is made. Updated user/design/flow documentation and
module integration/provenance notes.

Checks: `./gradlew --no-daemon --max-workers=4 --continue assembleStableDebug :alter-voice:testDebugUnitTest`.
All 24 tests passed, including correction, ambiguity, fact-change rejection and fallback regressions. Compilation,
resources and dexing completed; assembly failed only because the shared signing keystore is unavailable. The first
build found a new-module JVM-target mismatch, corrected before the final run. `git diff --check` passed. Phone model
quality, APK signature and main-app integration were not tested.

## Intent-preserving ALTER dictation instructions (Oct 24, 2026)

Expanded shared-agent dictation instructions using the designer's supplied cleanup requirements: fillers, false
starts, explicit latest corrections, faithful grammar/formatting, language/tone preservation including Hinglish,
and protection of names, code, numbers and meaning. Forbid invented context, translation, extra information,
answers and action execution. Added weekday-correction, Hinglish and instruction-isolation worked examples.
This updates policy instructions; conservative validation, model selection, Hindi bypass and formatter limits
remain. Broader semantic corrections are still rejected rather than silently relaxing meaning checks. No context
collection, cleanup levels or claim of reproducing Wispr's proprietary implementation was added.

Validation: `./gradlew --no-daemon --max-workers=4 --continue assembleStableDebug :alter-voice:testDebugUnitTest`.
All 25 tests passed, including language-preserving repetition cleanup and rejection/fallback for changed names,
amounts, facts and unsupported semantic deletions. Compilation/resources/dexing completed; APK assembly failed only
for the missing shared signing keystore. `git diff --check` passed. Phone model-quality/signature checks were not run.

## Adaptive live Whisper previews and model warm-up (Oct 24, 2026)

Normal text-field entry now calls the existing selected-model preloader, avoiding an otherwise unused warm-up path.
It does not capture audio or download weights, and sensitive fields bypass it. Whisper requests its first preview
after two seconds of captured audio instead of four. Later preview intervals adapt to twice the observed decode
cost (minimum four seconds, capped by the bounded window), reducing repeated prefix decoding on slow devices.
Every full window and final tail still runs; queued PCM is never dropped to catch up. Text is still inserted only
when finished. Native CPU binaries, model weights and translation behavior are unchanged.

Checks: `./gradlew --no-daemon --max-workers=4 --continue assembleStableDebug :alter-voice:testDebugUnitTest`.
All 24 tests passed, including a simulated slow decoder checking early first output, skipped redundant previews and
complete final-tail processing. Compilation/resources/dexing completed; APK assembly failed only for the absent shared
signing keystore. `git diff --check` passed. No phone latency, Medium real-time throughput, RAM/thermal behavior or
APK signature was measured; this change does not promise real-time Medium performance.

## Replacement for PR #16 on current main (Oct 24, 2026)

Resolved overlap with merged PR #17 by retaining both shared-agent cleanup and live Whisper regression coverage,
along with both documentation updates. Replacement PR carries the existing shared core/instructions without
removing main's adaptive preview scheduling or preload integration.

Validation: `./gradlew --no-daemon --max-workers=4 --continue assembleStableDebug :alter-voice:testDebugUnitTest`.
All 26 tests passed (no failures, errors or skips); compilation/resources/dexing completed. Assembly failed only at
signing validation for the missing shared keystore. `git diff --check` passed. Phone/signature checks were not run.

## Oriserve Hindi2Hinglish Swift Q8 test option (Oct 25, 2026)

Added “Oriserve Hinglish Swift · Q8” to All speech models using the existing explicit download/select/delete flow.
This is Oriserve's Whisper Base Hindi2Hinglish fine-tune, distributed as an AirNote community GGML Q8 conversion.
The 81,768,585-byte artifact is pinned to revision d4f718b3eb4f3f8026a54c315bcfc54277225b02 and its exact SHA-256.
The original and conversion cards declare Apache-2.0; full license and source/conversion provenance are included.
No weights are bundled. It uses the existing Whisper runtime, streaming schedule, recovery and ALTER cleanup.
Hindi decoding is fixed for this specialized model as documented upstream; descriptions disclose Latin-script
Hinglish output and this override. Other Whisper choices retain the selected language. Updated user/design/flow docs.

Validation: downloaded the entire Q8 artifact outside the checkout and verified its size and SHA-256 against both
the catalog and Hugging Face LFS metadata. Built a temporary host-only loader from the bundled whisper.cpp source;
it successfully loaded the model as multilingual Whisper Base (6 audio layers, 512 state). No native source or
committed Android binaries changed. This checks model loading, not Android latency or recognition quality.
`./gradlew --no-daemon --max-workers=4 --continue assembleStableDebug :alter-voice:testDebugUnitTest`: all 27 tests
passed (no failures, errors or skips), including specialized/general model-language routing. Compilation/resources/
dexing completed; assembly failed only at signing validation for the missing shared keystore. `git diff --check`
passed. Phone inference/download acceptance, speed, thermals, accuracy and APK signatures were not tested.

## Voice startup, visible errors and smaller cleanup requests (Oct 25, 2026)

Separated microphone startup from post-capture processing. Starting now shows a still white line with accessible
“Starting microphone” text, followed by the real-RMS yellow wave; the thinking animation no longer flashes merely
because a capture job was queued. Processing represents finished audio. Retained the full-keyboard screen-awake
behavior explicitly for starting/recording/processing after separating these states.

Error state now overrides recoverable paused/processing state and hides Play/Pause, restoring the red warning icon.
A new recording with ten seconds of inaudible PCM fails with the no-speech error instead of waiting for model
warm-up/decoding; microphone failures publish an error before capture teardown. Drafts are preserved. Tapping a
no-speech error starts capture rather than repeatedly decoding silence; other recoverable failures retry saved audio.

ALTER requests now send three relevant worked examples (grammar plus question, correction or Hinglish, and an
instruction-isolation example) instead of all eight. Meaning-preserving instructions, validators, full input,
timeout and fallback remain. This reduces prompt size, without a claim of measured phone speed/quality improvement.

Validation: `./gradlew --no-daemon --max-workers=4 --continue assembleStableDebug :alter-voice:testDebugUnitTest`.
All 30 tests passed (no failures, errors or skips), including new startup/error precedence, silence during warm-up,
and relevant-example selection checks. Compilation/resources/dexing completed; assembly failed only for the
missing shared signing keystore. `git diff --check` passed. Phone microphone/lifecycle/visual/model-quality/latency
acceptance and APK signature verification were not run.

## Adaptive cleanup instructions and original-precision ASR comparison (Oct 25, 2026)

Saved the designer's supplied cleanup document as docs/dictation-cleanup-instructions.md, a reference rather than
instructions to execute transcript content. Simple inputs up to six words/80 characters skip cleanup-model/catalog
loading and use deterministic formatting; the greeting “hi how are you” becomes “Hi, how are you?” without inference.
Corrections, repetition, formatting cues, digits and tagged/code syntax keep short text on the model path. Ordinary
inputs up to 80 words/600 characters use compact instructions; larger chunks use fuller instructions distilled from
the supplied reference. Context is absent: transcript tags cannot supply app/screen/style/vocabulary instructions.
Full input, validator, timeout/fallback, opt-in/model availability and Hindi bypass are retained. This does not
implement every requested semantic normalization/formatting operation; existing validation remains conservative.

For reported word recognition errors, added an explicitly selectable Oriserve Hinglish Swift FP16 option (147,951,465
bytes), pinned to the same conversion revision/license and verified SHA-256. The conversion uploader recommends FP16
for fidelity. Both Oriserve variants use Hindi decoding; Q8 selections remain unchanged. No claim is made that
quantization caused a particular recognition error. No blind replacements, correction-memory feature, model training
or automatic learning were added.

Validation: downloaded and hashed the full FP16 artifact outside the repository; a host-only loader built from the
bundled whisper.cpp source loaded it as multilingual Whisper Base. `./gradlew --no-daemon --max-workers=4 --continue
assembleStableDebug :alter-voice:testDebugUnitTest`: all 31 tests passed with no failures, errors or skips, including
short-text inference bypass, complexity routing, compact/detailed instruction selection, and both variants' language
routing. Existing cancellation coverage uses a nontrivial input to exercise real model-path cancellation. App
compilation/resources/dexing completed; assembly failed only for the missing shared signing key. `git diff --check`
passed. Phone latency, recognition/cleanup quality, FP16-vs-Q8 comparisons and APK signatures were not tested.

## Explicit local dictation correction memory (Oct 25, 2026)

Added Settings → ALTER voice → Learned words: enter the heard/intended word, explicitly remember it, inspect saved
pairs, and delete individual rules. Store up to 100 pairs atomically under private noBackupFilesDir, with a shared
lock for settings/inference access. Apply approved replacements before optional cleanup (also when cleanup is off),
in a single case-insensitive whole-word pass. Sentence punctuation is retained; URLs, emails and identifiers stay
unchanged. Reject multi-word, numeric and listed negation edits. The UI warns that rules apply globally, including
legitimate occurrences of the source word. This is manual correction memory, not automatic editor monitoring,
context-aware disambiguation, native recognition hints, audio retention or model-weight training.

Validation: `./gradlew --no-daemon --max-workers=4 --continue assembleStableDebug :alter-voice:testDebugUnitTest`:
all 32 tests passed without failures/errors/skips, including whole-word/punctuation boundaries, no cascading,
protected edits and empty/deleted-rule behavior. Compilation/resources/dexing completed; assembly failed only at
signing validation for the missing shared keystore. `git diff --check` passed. Phone persistence/settings/dictation
acceptance, backup behavior and APK signature verification were not run.

## Experimental Handy editor and cleanup during recording (Oct 25, 2026)

Added explicitly selectable Handy Editor 350M Q4_K_M beside stock cleanup, pinned by revision/size/SHA-256 with LFM
license and provenance. Added pinned CPU-only llama.cpp source subset, MIT/third-party notices, JNI bridge, repeatable
native build script and rebuilt arm64-v8a/x86_64 prebuilts (16 KB aligned, isolated symbols). Gradle still does not
build native code or bundle/download weights. Oriserve FP16 ASR selection is retained; no Tiny model was added.

Enabled cleanup now processes debounced whole-transcript provisional updates during capture. Later ASR revisions can
retract earlier wording. Finalization reuses or awaits an exact-source edit; changed final text cancels stale work.
Nothing is inserted before Finish. Raw final ASR is saved separately from polished draft text. Old drafts remain
readable; insertion/discard clears the existing private draft. Added narrow “Friday, actually make it Monday” and
“Friday, no wait, Monday” resolution/acceptance without relaxing unrelated facts or negation checks. Updated the
stock example; Handy follows its trained explicit-empty-system protocol instead of generic instructions. Preserve
source on unsafe edits, protect Roman-Hindi negation, and distinguish local model timeouts from external cancellation.

Validation: full 229,315,072-byte Handy download matched catalog/LFS SHA-256. Both Android native ABIs rebuilt from
pinned source; ELF alignment and lack of exported llama/ggml symbols checked. A temporary host build of the same
JNI bridge completed English self-corrections, kept a correct notebook sentence and treated a poem request as text;
real native abort/reuse and token-truncation rejection passed. Hinglish kal/parso cleanup failed the desired semantic
correction and dropped a repeated negation; the validator now rejects that edit. Handy remains experimental for
Hinglish. Host latencies varied under build load; they are not phone benchmarks. `docs/HANDY_EXPERIMENT.md` records
integration details, actual checks and remaining phone acceptance.

`./gradlew --no-daemon --max-workers=4 --continue assembleStableDebug :alter-voice:testDebugUnitTest`: all 36 tests
passed without failures/errors/skips. Compilation/resources/dexing completed; assembly failed only for the missing
shared signing keystore. `git diff --check` passed (the pinned third-party subtree retains upstream whitespace via a narrow attribute rule). No Android hardware inference, phone lifecycle/typing/clipboard,
model quality/latency/thermal acceptance or APK signature verification was performed.

## Experimental Nemotron 3.5 streaming ASR (Oct 26, 2026)

- Added an opt-in, checksum-pinned Nemotron 3.5 0.6B Q4_K_M download (496 MB) in speech settings;
  existing speech selection and cleanup preferences stay unchanged. Hindi can use mixed script.
- Added an isolated, CPU-only transcribe.cpp JNI runtime with a 320 ms cache-aware model profile,
  live transcript revisions, final-tail flushing, cancellation and fresh recording sessions. Retry
  uses the same streaming decoder over saved audio. Model weights are never bundled.
- Preserved upstream MIT notices and the model's separate OpenMDW-1.1 license/NOTICE. Rebuilt both
  committed arm64-v8a and x86_64 libraries with 16 KB alignment and JNI-only exported symbols.
- Validation: 39 voice unit tests passed, including sample continuity, UTF-8 final text, failed
  finalization and cancellation. Actual production JNI bridge passed host inference on public JFK
  English and Oriserve Hindi samples (Hindi hint and automatic selection), including partial/final
  output, cancellation and session reuse. Download size/SHA-256 verified; all 400 vendored files
  match the pinned upstream commit. Hindi sample output is imperfect; no accuracy advantage claimed.
- `assembleStableDebug :alter-voice:testDebugUnitTest --continue` compiled/dexed successfully; only
  signing validation failed because the shared `debug.keystore` is unavailable. No substitute key
  was created; APK signature/install checks and phone speed/accuracy acceptance remain outstanding.

## Documentation refresh after PR #23 (Oct 26, 2026)

- Audited project Markdown and refreshed voice/model summaries, settings navigation, shared-agent boundaries,
  recovery coverage, runtime rebuild guidance, model provenance and the Nemotron validation record.
- Clarified local signing failure versus the existing CI one-off-key fallback; corrected the stale claim that
  the keyboard contains no agent policy. Preserved upstream/vendor docs, license text and historical entries.
- Validation: all 40 local links in 17 changed Markdown documents resolve; `git diff --check` passed. No code or binaries
  changed and no additional Android test run. PR #23's 39 passing voice tests and signing limitation still apply.

## Nemotron Hindi Romanization (Oct 28, 2026)

- Convert Nemotron's Hindi-script runs offline to Latin/ASCII before live cleanup and final output,
  including saved-audio retry. Already-Latin English/identifiers remain unchanged. No model weights changed.
- ICU4J 76.1 supplies consistent conversion on Android 8+; its Unicode V3 license is retained.
  Phonetic spelling differs from casual Hinglish and does not improve ASR word recognition.
- Added mixed-text preservation, digit/punctuation, revision and idempotence regression checks.
- Validation: all 42 voice unit tests passed; Android compilation/dexing passed. Assembly remains
  blocked only by the unavailable shared `debug.keystore`; no substitute key or signed APK was created.

## Parakeet 110M INT8 CTC (Oct 28, 2026)

- Added the document's Android deployment of NVIDIA Parakeet 110M as one explicit English-only
  download; the original checkpoint is recorded as provenance, not a duplicate runnable model.
- CPU INT8 greedy decoding with two threads and 16 kHz/80-dimensional input, private verified
  archive extraction, periodic bounded-window previews and existing recovery/cleanup integration.
- Pinned static-ORT sherpa-onnx Java/JNI artifacts avoid Moonshine's ONNX Runtime library conflict;
  both 64-bit ABI libraries have 16 KB alignment. Added licenses/provenance and an import script.
- Checks: exact model/archive/component checksums verified; sherpa-onnx 2.0.1 host inference passed
  both packaged English WAVs and silence; the Android API JAR with matching host JNI also passed
  config/decode/lifecycle smoke testing. All 45 voice tests passed, including whitelist extraction,
  duplicate/missing components, symlinks and wrong size/hash rejection. Android compilation/dexing passed;
  signing alone failed because the shared debug.keystore is unavailable. No alternate key created.
  Signed install and phone speed/accuracy/cancellation acceptance remain unverified.

### Parakeet TDT FP16 follow-up

- Added the requested larger Parakeet 110M TDT FP16 variant (229 MB), with pinned GGUF
  revision/size/hash, CC-BY-4.0 provenance and the converter's NOTICE. Existing selections stay intact.
- Added offline TDT decoding to the existing isolated transcribe.cpp bridge: up to two CPU threads,
  fresh native sessions, bounded 20-second PCM input, abort callbacks, UTF-8 results and saved-audio
  retry through existing previews/cleanup. Both committed Android ABI libraries rebuilt.
- Production JNI source passed host FP16 inference on both English samples, invalid input limits,
  fresh-session reuse, in-flight cancellation/restart and streaming-loader rejection. All 45 voice
  tests passed; Android compilation/dexing passed. Signing remains blocked by the missing shared key.
  No model weights or alternative keys committed. Phone accuracy/speed still require comparison;
  the model card's 2.43% LibriSpeech test-clean WER is an upstream benchmark, not an app accuracy claim.

### PR #26 merge resolution

- Merged main after PR #25, preserving Nemotron Hindi Romanization alongside both Parakeet
  variants, their runtime dependencies, model descriptions and documentation.
- Validation: all 48 combined voice tests passed; Android compilation/dexing passed. Assembly
  failed only at signing validation because the shared debug.keystore is unavailable. No
  replacement key generated. No conflict markers remain; whitespace checks passed.

## IndicConformer Hindi routing and shared Romanization (Oct 28, 2026)

- Added an explicit ~138 MB Hindi engine (AI4Bharat via a pinned OpenVoiceOS INT8 ONNX export),
  with verified vocabulary and a staged/checksummed metadata-only graph adapter for sherpa-onnx.
  Downloaded weights remain unchanged; no model weights or additional native runtimes bundled.
- Explicit Hindi mode routes both Parakeet choices and Nemotron through this engine while
  retaining the saved base choice for English/Automatic. Settings show the actual recognizer
  and explain missing-download/routing behavior. Guard language changes during active dictation.
- Reused offline ICU rules as a shared Hindi-to-English-letter stage before model-based output;
  preserved Latin content and the existing Hindi cleanup-model bypass. No IndicXlit transformer
  or Sanscript JavaScript runtime is included; code-switching accuracy remains unverified.
- Host tests: exact metadata-adapted graph and vocabulary produced Hindi through sherpa-onnx
  and the Android API JAR/matching host JNI; silence and repeated streams passed. All 52 voice
  tests passed, including routing, failed adaptation cleanup/source-byte preservation and
  revalidating saved-audio engine identity after waiting for atomic language/model setup.
  Android compilation/dexing passed; signing alone failed due to the unavailable shared key.
  No replacement key created. Phone Hindi/Hinglish accuracy/latency and install acceptance pending.

## Automatic English/Hindi routing for both Parakeet variants (Oct 28, 2026)

- Adapted Sayso's Whisper spoken-language-identification approach, retaining its MIT attribution,
  with pinned INT8 ONNX detector assets and the existing sherpa runtime. One routing download
  groups detector/Hindi files (~240 MB total; existing Hindi files reused), without bundling weights.
- Automatic language keeps the selected English CTC/TDT decoder, routes Hindi to IndicConformer
  and uses installed Nemotron for mixed/uncertain labels. Mixed without Nemotron preserves audio
  with an actionable error. Unknown/short input can retain the base recognizer with an uncertainty
  notice. No hard-coded confidence, guaranteed code-switching or Whisper Tiny transcription claim.
- Classifier sees copied probes; full PCM windows remain intact. Cache the detector and derived
  speech files across route changes while keeping one ASR decoder active. Keep explicit modes and
  Nemotron's native Automatic path, policy-aware draft identity and cancellation/no-late-result checks.
- Validation: all 63 voice tests passed; exact detector hashes verified. Host sherpa-onnx and the
  exact Android API JAR/matching host JNI classified real English/Hindi samples correctly.
  Android compilation/dexing passed; signing validation alone failed because the shared key is absent.
  No alternate key created. Phone installation, accuracy/latency, short/mixed cases and thermals pending.
- Routed-cache deletion is serialized with setup; warmup cannot reload an engine during deletion.
  Queued saved-audio recovery rechecks missing assets before capture and preserves the private draft.
