# ALTER Keyboard design

## Decision: FUTO's keyboard, ALTER's skin

ALTER Keyboard works exactly like FUTO Keyboard: FUTO's themes and theme picker, settings, toolbar actions (undo,
redo, clipboard, emoji, text editing) and layouts stay. Since Oct 19, 2026 the default skin is the **ALTER** theme
(`theme/presets/AlterTheme.kt`): PitchBlack background, SlateGrayCard keys, RaisedSurface pressed and function keys,
LightText labels, MutedText hints and AlterYellow as the one accent (enter key, highlights). FUTO's other themes stay
selectable. ALTER's voice surfaces use ALTER's design below.

## Launcher icon

`res/mipmap-anydpi-v26/ic_launcher*.xml` (adaptive; minSdk 26, so no bitmap icons are used): a light `#ECECEC`
keyboard outline with 11 keys and an AlterYellow `#FFBD00` spacebar (`drawable/ic_alter_launcher_foreground.xml`) on
ALTER's `#121212` launcher background, plus a white monochrome layer for Android 13 themed icons. The setup screen shows
the same mark in its own colours; the training notification uses the monochrome layer. FUTO's icon is not used.

## App screens (settings, setup, payment)

The app's own screens follow the ALTER app's settings design (DESIGN_SYSTEM.md in ALTER-AI-code-base, "App
redesign, pass 1"), whatever keyboard theme is picked: `UixThemeAuto` always applies the ALTER colours, and
`uix/settings/AlterStyle.kt` holds the shared pieces.

- Page heading 26/34sp semibold, one Lucide arrow-left back icon immediately left of it.
- Each menu's settings sit in one grouped section: an optional 12sp label above one borderless 20dp SlateGrayCard
  surface with 16dp page gutters.
- Rows: 16/24sp title, concise 12/18sp secondary line; navigation rows lead with a 36dp tile (6% foreground fill,
  10dp corners, 20dp icon) and end with a Lucide chevron-right.
- Tips: a neutral RaisedSurface note with 16dp corners and 14sp muted text; warnings keep the error colours.
- Yellow means selection, activity and the brand; download/use controls are neutral outlined buttons.
- FUTO's payment page keeps all its content and actions; only the shared styling applies to it.

## ALTER voice surfaces

Tokens, from `ui/theme/Color.kt` in the ALTER app (`AlterColors` in `alter-voice`):

| Token | Hex | Use |
| --- | --- | --- |
| PitchBlack | `#000000` | Pill bar background |
| SlateGrayCard | `#18181B` | Pill body |
| LightText | `#ECECEC` | Pill bar icons |
| DisabledText | `#71717A` | Disabled mic (password fields) |
| AlterYellow | `#FFBD00` | The one accent: the live voice wave |
| ErrorRed | `#EF4444` | Voice error triangle only |
| Orb ink | `#F5F5F5` | Monochrome logo and idle icons |

- Pill: 12 dp corners, SlateGrayCard body inside a thin (1 dp) AlterYellow boundary at 60% (25% when disabled), the
  ALTER mark of every voice surface. No glass, blur or gradients. Touch targets 48 dp.
- Idle: the flat-eyes ALTER mark (`ic_alter_ai_doodle_closed`) tinted `#F5F5F5`, plus the Lucide mic.
- Listening: one AlterYellow line driven by the real microphone RMS, flat in silence (ALTER's `voiceWavePoints`).
- Listening/paused/thinking: Pause/Play is outside the right-hand curve; the discard cross is outside the left.
  Both controls use 20dp icons to match the toolbar, with 2-unit strokes, 12dp ripple corners and keyboard-theme tint
  (light ink on the fixed black pill-mode bar). Like the discard cross, Pause is two single vertical strokes and Play
  is a simple triangle outline, with round caps/joins. The 48dp-wide hit areas use the full
  available bar height. Only the bounded status surface has vertical padding, so the controls align. Paused shows a
  still white horizontal line in the centre; tapping it processes saved dictation. Play continues capture, and Pause
  can also suspend processing.
- Thinking: the original AICSS S1 orb choreography (1.7-second cycle, 700ms radial delay per cell, 180ms centre lead,
  eased swelling and fading) is reused across a three-row rectangular lattice. Round dots move from white to yellow
  as they light. The column count adapts to fill the pill; the small orb on other screens keeps its original 3×3 shape.
- Error: the red Lucide triangle-alert, no text; TalkBack reads the error and "Tap to try again".
- Animations off: the waveform is still and the processing field shows static centre dots lit against dim white dots.

## Keyboard UX

- **Start mode** (Settings → ALTER voice): Full keyboard (default) or Pill. Applied each time a text field gets focus.
- **Pill mode:** 56 dp PitchBlack bar. Left: FUTO's globe (input-method picker). Centre: the pill. Right: Lucide
  keyboard button back to the full keyboard. No other controls.
- **Full keyboard:** FUTO's keyboard with the ALTER skin. The voice action uses ALTER's flat-eye mark
  (`ic_alter_ai_doodle_closed`), tinted by the selected keyboard theme. It adds no layer: the suggestions in the action
  bar row are replaced by ALTER's voice strip (a cancel ✕ and the bounded pill with the wave, orb or error) while the
  expand button, pinned actions and keys stay where they are. Tapping the flat-eye action again acts like tapping the
  strip. A pinned "Minimize" action (FUTO's chevron-down icon) switches to pill mode.
- **Pill taps:** idle → record; listening → finish; paused → process saved dictation; thinking → pause processing;
  error → retry saved dictation, or record again if nothing was captured. Pause/Play outside the right edge pauses/resumes
  capture. The full keyboard's ✕ explicitly discards the draft.
- Text goes in through `commitText` at the cursor, replacing any selection, and is never sent.
- Losing focus, hiding the keyboard, or losing microphone input pauses capture. Finished audio continues decoding
  and cleanup in the IME service scope when the keyboard closes; the final text is copied to the system clipboard
  and retained in the private draft, without insertion into a different editor. Reopening reuses the running session
  until completion. Explicit Pause still suspends processing. One temporary draft in
  private `noBackupFilesDir/dictation-draft` survives controller/process recreation. Reopening after interruption shows
  it paused; no automatic capture or insertion. downloaded recognizers retain PCM (maximum fifteen minutes); Android recognition only
  retains text delivered by its callbacks. No stop is requested merely because a notification shade is opened.
- Final text remains durable until the editor accepts insertion. Explicit discard removes metadata and audio;
  keyboard surface changes preserve them. Clipboard copying retains them too. If Android destroys the IME service,
  background processing stops and the saved draft remains recoverable. Drafts stay until accepted insertion or discard, with no recording history.
- Password fields (`TYPE_TEXT_VARIATION_PASSWORD` and the other password variations) and
  `IME_FLAG_NO_PERSONALIZED_LEARNING` always open the full keyboard with voice disabled and no recovered draft shown.
- If the microphone permission or a speech model is missing, tapping the mic opens Settings → ALTER voice.

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
selection stays unchanged. Hindi-script runs are now converted offline to English letters before cleanup. The 320 ms
streaming profile is a model setting, not measured phone latency. Speed and accuracy against
Moonshine still require phone testing. See [Nemotron experiment](NEMOTRON_EXPERIMENT.md).

#### Streaming state and cleanup contract

Nemotron uses the same recording wave, external pause/resume, paused white line, error priority and full-width
processing animation as the other downloaded recognizers. Loading its model must not show the processing
animation during recording startup. Interim transcript updates can trigger enabled, debounced cleanup; they
never insert text. Finish flushes the ASR stream before final cleanup/insertion. Native feed/finalization errors
preserve the draft for retry. The model's language selector supports automatic, English and Hindi hints;
Hindi-script runs are converted to Latin/ASCII before live cleanup and final insertion.

Basic Romanization uses ICU4J 76.1 (Unicode License V3). Downloading IndicXlit enables neural
Hindi word conversion instead, preserving existing English text. Both run offline before cleanup;
neither changes the speech-recognition weights. See the neural Romanization section below.

### Parakeet 110M CTC and TDT (English)

ALTER voice → All speech models includes Parakeet 110M INT8 CTC (104 MB archive) and TDT FP16
(229 MB GGUF). Both run offline with two CPU threads and greedy decoding, using bounded-window
periodic previews. They share the NVIDIA base checkpoint but use different decoders and precision;
the raw source checkpoint is recorded as provenance. Existing choices stay
unchanged; Hindi/Hinglish still require a multilingual model. Phone speed/accuracy are unverified.
See [the Parakeet guide](PARAKEET_EXPERIMENT.md) for configuration, recovery and validation limits.

### Hindi engine for Parakeet and Nemotron

Download IndicConformer Hindi INT8 (~138 MB) under ALTER voice → All speech models. Explicit
Hindi mode uses it for either Parakeet variant or Nemotron, with Roman-letter output through
downloaded IndicXlit, or basic ICU rules when it is absent. The actual Hindi engine is shown in settings; the base model is
remembered for English/Automatic mode. Missing Hindi files require download, without a silent
recognizer fallback. Parakeet itself remains English-only; the separate Automatic routing download now provides
English/Hindi detection and engine selection for both variants.
Transliteration is independent of recognition and does not fine-tune Parakeet or prove natural code-switching.
See [Hindi pipeline](INDIC_HINDI_PIPELINE.md) for configuration, recovery and testing.

### Automatic English/Hindi with either Parakeet

Keep the selected Parakeet model and choose Automatic language. A one-time routing download
(~240 MB total, or ~103 MB extra when Hindi is already installed) provides Whisper Tiny LID
and IndicConformer Hindi. English uses the saved CTC/TDT model; Hindi uses IndicConformer.
Nemotron is the optional mixed/uncertain fallback. Missing mixed-language fallback preserves
the recording with an error. Sparse language detection is experimental and can miss short
or mixed phrases; it is not Whisper Tiny transcription or guaranteed natural Hinglish.
See [automatic routing](AUTOMATIC_LANGUAGE_ROUTING.md) for setup, asset deletion and recovery behavior.

### Neural Hindi Romanization

Download **IndicXlit · Hindi to Roman** once in ALTER voice (about 47 MB). The official AI4Bharat
Indic-to-Roman model then converts Nemotron's live/final Hindi words before Alter cleanup;
English text stays unchanged. Other downloaded recognizers share this conversion. Without the
complete download, basic ICU conversion remains available. This is pretrained word transliteration,
not speech-model fine-tuning or a repair for wrongly recognized words. See [IndicXlit setup and limits](INDICXLIT_TRANSLITERATION.md).
