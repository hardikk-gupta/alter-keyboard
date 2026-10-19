# ALTER Keyboard design

## Decision: FUTO's keyboard, ALTER's skin

ALTER Keyboard works exactly like FUTO Keyboard: FUTO's themes and theme picker, settings, toolbar actions (undo,
redo, clipboard, emoji, text editing) and layouts stay. Since Oct 19, 2026 the default skin is the **ALTER** theme
(`theme/presets/AlterTheme.kt`): PitchBlack background, SlateGrayCard keys, RaisedSurface pressed and function keys,
LightText labels, MutedText hints and AlterYellow as the one accent (enter key, highlights). FUTO's other themes stay
selectable. ALTER's voice surfaces use ALTER's design below.

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
| Orb ink | `#F5F5F5` | Thinking orb and monochrome logo |

- Pill: 12 dp corners, SlateGrayCard body inside a thin (1 dp) AlterYellow boundary at 60% (25% when disabled), the
  ALTER mark of every voice surface. No glass, blur or gradients. Touch targets 48 dp.
- Idle: the flat-eyes ALTER mark (`ic_alter_ai_doodle_closed`) tinted `#F5F5F5`, plus the Lucide mic.
- Listening: one AlterYellow line driven by the real microphone RMS, flat in silence (ALTER's `voiceWavePoints`).
- Thinking: the AICSS Orbs S1 lattice (3×3 dots, 6-unit pitch on a 28-unit stage at 20 dp, 1.7 s cycle, centre leads
  by 180 ms, resting ink 20% on dark), in white and yellow: each dot moves from `#F5F5F5` to AlterYellow as it lights.
- Error: the red Lucide triangle-alert, no text; TalkBack reads the error and "Tap to try again".
- Animations off: the wave and orb show still frames (the orb lights only its centre dot).

## Keyboard UX

- **Start mode** (Settings → ALTER voice): Full keyboard (default) or Pill. Applied each time a text field gets focus.
- **Pill mode:** 56 dp PitchBlack bar. Left: FUTO's globe (input-method picker). Centre: the pill. Right: Lucide
  keyboard button back to the full keyboard. No other controls.
- **Full keyboard:** FUTO's keyboard with the ALTER skin. The mic action adds no layer: the suggestions in the action
  bar row are replaced by ALTER's voice strip (a cancel ✕ and the bounded pill with the wave, orb or error) while the
  expand button, pinned actions and keys stay where they are. Tapping the mic again acts like tapping the strip. A
  pinned "Minimize" action (FUTO's chevron-down icon) switches to pill mode.
- **Pill taps:** idle → record; listening → finish; thinking → cancel; error → retry the same recording held in memory
  (no new mic session), or record again when nothing was captured.
- Text goes in through `commitText` at the cursor, replacing any selection, and is never sent.
- Recording stops and is discarded when the field loses focus or the keyboard hides.
- Password fields (`TYPE_TEXT_VARIATION_PASSWORD` and the other password variations) and
  `IME_FLAG_NO_PERSONALIZED_LEARNING` always open the full keyboard with the mic disabled.
- If the microphone permission or a speech model is missing, tapping the mic opens Settings → ALTER voice.
