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
  (light ink on the fixed black pill-mode bar). Play/Pause fills the viewport like the neighboring toolbar symbols; the
  pause bars have 2-unit corner radii and the play triangle uses curved corners. The 48dp-wide hit areas use the full
  available bar height. Only the bounded status surface has vertical padding, so the controls align. Paused shows an 18dp yellow pause marker in the centre;
  tapping the centre processes saved dictation, Play continues capture, and Pause can also suspend processing.
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
- Losing focus, hiding the keyboard, or losing microphone input pauses capture/decoding. One temporary draft in
  private `noBackupFilesDir/dictation-draft` survives controller/process recreation. Reopening shows it paused; no
  automatic capture or insertion. Whisper/Moonshine retain PCM (maximum fifteen minutes); Android recognition only
  retains text delivered by its callbacks. No stop is requested merely because a notification shade is opened.
- Final text remains durable until the editor accepts insertion. Explicit discard removes metadata and audio;
  keyboard surface changes preserve them. Drafts stay until accepted insertion or discard, with no recording history.
- Password fields (`TYPE_TEXT_VARIATION_PASSWORD` and the other password variations) and
  `IME_FLAG_NO_PERSONALIZED_LEARNING` always open the full keyboard with voice disabled and no recovered draft shown.
- If the microphone permission or a speech model is missing, tapping the mic opens Settings → ALTER voice.
