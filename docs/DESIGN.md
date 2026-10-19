# ALTER Design System for ALTER Keyboard

## Color Palette

| Token | Hex | Role & Usage |
| --- | --- | --- |
| `PitchBlack` | `#000000` | Keyboard background, bottom bar background in pill mode |
| `SlateGrayCard` | `#18181B` | Letter keys, pill body, dropdowns, cards |
| `RaisedSurface` / `BorderGray` | `#262626` | Function keys (shift, delete, symbols), key pressed state |
| `LightText` | `#ECECEC` | Primary key labels, active text |
| `MutedText` | `#A1A1AA` | Secondary labels, long-press hints, suggestions |
| `DisabledText` | `#71717A` | Disabled keys |
| `AlterYellow` | `#FFBD00` | Enter/action key, active mic indicator, live RMS wave, selected suggestion |
| `ErrorRed` | `#EF4444` | Voice error indicator only (warning triangle) |
| `Orb ink` | `#F5F5F5` | Thinking orb dots and monochrome logo inside keyboard |

- **Launcher Icon:** `#FFCC00` brand mark on `#121212` circular/adaptive background.
- **In-Keyboard Brand Mark:** Monochrome `#F5F5F5`.

## Typography

- System sans (`FontFamily.SansSerif`) throughout.
- Hierarchy:
  - 26sp: Settings page titles
  - 16sp: Body text and primary items
  - 14sp: Secondary labels and suggestion row text
  - 12sp: Supporting captions and long-press hints
  - Key labels adhere to standard FUTO / IME sizing.

## Shapes & Geometry

- **Key corners:** 8dp rounded rectangle.
- **Pill & panel corners:** 12dp rounded rectangle.
- **Settings cards:** 20dp.
- **Touch targets:** Minimum 48dp x 48dp.
- **Pill styling:** Solid `SlateGrayCard` body on `PitchBlack` background. **No outline on the pill.**
- **No glassmorphism, blur, gradients, or rainbow color effects** on keyboard or voice surfaces.

## Icons

- Exclusively **Lucide Icons**:
  - `mic`
  - `undo`
  - `redo`
  - `clipboard`
  - `smile` (emoji)
  - `settings`
  - `keyboard`
  - `chevron-down` (minimize)
  - `globe` (IME switch)

## Animations & Accessibility

- Respect Android system "remove animations": every animation must have a still frame fallback.
- **Thinking Orb (AICSS S1 variant):**
  - 3x3 dots on a 6-unit pitch.
  - Drawn on a 28-unit stage scaled to 20dp.
  - 1.7s cycle; wave radiates outward from the center; center leads by 180ms.
  - Resting ink is 20% opacity on dark; when animations are turned off, display still frame with only center dot lit.
- **Voice Wave:**
  - Single `AlterYellow` waveform driven by actual microphone RMS.
  - Flat baseline during silence. Never display fake audio oscillations.

## Keyboard Modes & Interaction Design

### 1. Pill Mode (Minimized)
- Height: 56dp bar docked to bottom of screen.
- Background: `PitchBlack`.
- Layout:
  - Left: System input method switcher (globe icon).
  - Center: ALTER Voice Pill (12dp corners, `SlateGrayCard`).
  - Right: Switch to full keyboard button (Lucide `keyboard`).
- **Pill States:**
  1. **Idle:** Flat-eyes mono logo + Lucide mic. Tapping anywhere starts recording.
  2. **Listening:** Live AlterYellow RMS wave. Tapping stops recording and initiates transcription.
  3. **Thinking:** AICSS S1 thinking orb. Tapping cancels processing.
  4. **Error:** Red warning triangle (`ErrorRed`). Tapping retries last recording held in memory.

### 2. Full Keyboard (Maximized)
- Full QWERTY layout with numeric row, symbol view, and swipe gesture typing.
- Suggestion strip and action row (undo, redo, clipboard history, emoji picker, text selection/editing tools, language switcher, settings).
- Lucide mic in suggestion strip triggers voice recording without minimizing keyboard.
- "Minimize" button (Lucide `chevron-down`) collapses the keyboard into Pill Mode.
- Sensitive / password fields automatically open Full Keyboard mode with mic disabled.
