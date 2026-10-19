# ALTER Keyboard design

## Decision: FUTO's keyboard look stays

ALTER Keyboard looks and works exactly like FUTO Keyboard. The handoff's restyle step (ALTER theme as default,
Lucide toolbar icons, restyled settings) was dropped at the owner's request (Oct 19, 2026). FUTO's themes, theme
picker, keys, toolbar icons and settings are unchanged. Only ALTER's voice surfaces use ALTER's design.

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

- Pill: 40 dp tall, 12 dp corners, no outline, no glass, blur, gradients or multicolour effects. Touch targets 48 dp.
- Idle: the flat-eyes ALTER mark (`ic_alter_ai_doodle_closed`) tinted `#F5F5F5`, plus the Lucide mic.
- Listening: one AlterYellow line driven by the real microphone RMS, flat in silence (ALTER's `voiceWavePoints`).
- Thinking: the AICSS Orbs S1 lattice (3×3 dots, 6-unit pitch on a 28-unit stage at 20 dp, 1.7 s cycle, centre leads
  by 180 ms, resting ink 20% on dark).
- Error: the red Lucide triangle-alert, no text; TalkBack reads the error and "Tap to try again".
- Animations off: the wave and orb show still frames (the orb lights only its centre dot).

## Keyboard UX

- **Start mode** (Settings → ALTER voice): Full keyboard (default) or Pill. Applied each time a text field gets focus.
- **Pill mode:** 56 dp PitchBlack bar. Left: FUTO's globe (input-method picker). Centre: the pill. Right: Lucide
  keyboard button back to the full keyboard. No other controls.
- **Full keyboard:** FUTO's keyboard as built. The mic action opens a 56 dp strip above the keys with the pill states
  while the keys stay visible. A pinned "Minimize" action (FUTO's chevron-down icon) switches to pill mode.
- **Pill taps:** idle → record; listening → finish; thinking → cancel; error → retry the same recording held in memory
  (no new mic session), or record again when nothing was captured.
- Text goes in through `commitText` at the cursor, replacing any selection, and is never sent.
- Recording stops and is discarded when the field loses focus or the keyboard hides.
- Password fields (`TYPE_TEXT_VARIATION_PASSWORD` and the other password variations) and
  `IME_FLAG_NO_PERSONALIZED_LEARNING` always open the full keyboard with the mic disabled.
- If the microphone permission or a speech model is missing, tapping the mic opens Settings → ALTER voice.
