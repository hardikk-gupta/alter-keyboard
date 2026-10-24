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
