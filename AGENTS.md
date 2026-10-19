# AGENTS.md — ALTER Keyboard Instructions for AI Agents

> **CRITICAL RULE**: One PR per step. **Create PRs only; never merge them.** Hardikk or Chirag merges.

## 1. Project Goal & Repositories

Build **ALTER Keyboard**: FUTO Keyboard's full typing keyboard, rebranded to ALTER's design, with ALTER's own voice pack built in.
The repo `hardikk-gupta/alter-keyboard` holds **only the keyboard**. It does not depend on the ALTER app being installed.

### Reference Repositories
- **FUTO Keyboard source (starting point):** `https://github.com/futo-org/android-keyboard` plus its 7 submodules (`libs`, `voiceinput-shared/src/main/ml`, `java/assets/layouts`, `translations`, `java/assets/themes`, `java/res-large`, `java/assets/futo-swipe`).
- **ALTER voice pack & design system (reference):** `hardikk-gupta/ALTER-AI-code-base` on `main` (read-only).

---

## 2. FUTO License Compliance (MANDATORY)

FUTO Keyboard uses the **FUTO Source First License 1.1**, not an open-source license. It allows our use only if ALTER Keyboard stays personal and non-commercial and is shared only for free. The original AOSP code inside it remains Apache 2.0 (see FUTO's `NOTICE`).

1. Keep FUTO's `LICENSE.md` and `NOTICE` at the repo root, unchanged.
2. Maintain `MODIFICATIONS.md` and an "About" line in settings stating:
   > *"ALTER Keyboard is a modified version of FUTO Keyboard by FUTO Holdings, Inc."*
3. **Do not remove or hide FUTO's payment features.** That covers the Payment settings page, `PaymentCompleteActivity` and the payment reminders. Restyle them to match ALTER design tokens, but keep them working.
4. Do not alter or remove any FUTO copyright or license notice in source files.
5. Never sell it or publish it as a commercial product. Never put it on a paid store listing.
6. Every model or library added on top keeps its own license file under `licenses/`: Moonshine, whisper.cpp, LiteRT-LM and the LFM Open License v1.0 for the cleanup model.

---

## 3. Signing and App Identity

The keyboard is signed with the **same shared key as ALTER**, so updates install cleanly over each other.

| Setting | Value |
| --- | --- |
| `applicationId` | `com.aistudio.altermind.keyboard` |
| App and IME name | ALTER Keyboard |
| Signing key SHA-1 | `B5:E5:7C:D6:93:49:2E:96:ED:5E:96:30:0D:7C:AC:38:73:5B:7E:BB` |
| Store / key password, alias | `android` / `android`, `androiddebugkey` |
| Key source | `ALTER_SIGNING_KEYSTORE_B64` environment variable, falling back to git-ignored `debug.keystore` |
| Version code | Minutes since 2026-01-01 UTC of the commit (fallback to build time if no `.git`) |

**Update Checker:** Disable or repoint FUTO's update checker (`org.futo.inputmethod.updates`) so ALTER Keyboard never offers FUTO's APK as an update.

---

## 4. ALTER Voice Pack Integration

The keyboard runs ALTER's voice pack locally inside a self-contained module `alter-voice/`. FUTO's own Whisper input is replaced.

- **Speech-to-text:** Moonshine Tiny/Small/Medium Streaming (English) or Whisper Tiny/Base/Small/Medium (multilingual).
- **Microphone:** Incremental PCM capture, real RMS audio level, silence handling. No fake audio levels! PCM capture never waits for model warm-up.
- **Cleanup:** LFM2.5 350M INT8 on LiteRT-LM 0.18.0. Adds punctuation & capitalization, removes filler words. One LiteRT engine at a time with a fresh engine per cleanup chunk. Validate every edit, keep tail text on timeout, never strip the word "message".
- **Catalog & Models:** Models download on-demand into private no-backup storage with exact byte sizes and SHA-256 checks from pinned catalog. Never bundle model weights into the APK.
- **Input Connection:** Insert text directly via `currentInputConnection.commitText()`. No accessibility service needed.

---

## 5. Design System Tokens & Rules

- **Colors:**
  - `PitchBlack`: `#000000` (Keyboard background)
  - `SlateGrayCard`: `#18181B` (Letter keys, pill body, panels)
  - `RaisedSurface` / `BorderGray`: `#262626` (Function keys, pressed states)
  - `LightText`: `#ECECEC` (Key labels)
  - `MutedText`: `#A1A1AA` (Secondary labels, suggestions, hints)
  - `DisabledText`: `#71717A` (Disabled keys)
  - `AlterYellow`: `#FFBD00` (Enter/action key, active mic, live wave, selected suggestion)
  - `ErrorRed`: `#EF4444` (Voice error triangle only)
  - `Orb ink`: `#F5F5F5` (Thinking orb & monochrome logo)
  - Launcher icon: `#FFCC00` on `#121212`.
- **Shapes & Typography:**
  - System sans (`FontFamily.SansSerif`) throughout.
  - Key corners: 8dp. Pill & panel corners: 12dp. Settings cards: 20dp. Touch targets: >= 48dp.
  - Icons: Lucide icons only.
  - **No outline on the pill.** No glass, blur, gradients, or rainbow color effects on voice surfaces.
  - Respect "remove animations": every animation must have a still frame fallback.
- **Thinking Orb:** AICSS S1 variant (3x3 dots on 6-unit pitch, 28-unit stage scaled to 20dp, 1.7s cycle radiating from center).
- **Voice Wave:** Single AlterYellow line driven by real microphone RMS.

---

## 6. Keyboard Modes & UX

- **Start Mode Setting:** In Settings -> ALTER, user selects default start mode (Pill Mode or Full Keyboard; default: Full Keyboard).
- **Pill Mode (56dp bar):**
  - **Idle:** Flat-eyes mono logo + mic. Tap to start recording. Left: IME globe. Right: Lucide keyboard button.
  - **Listening:** Live yellow wave (real RMS). Tap to finish.
  - **Thinking:** S1 thinking orb. Tap to cancel.
  - **Error:** Red warning triangle. Tap to retry last recording from memory.
- **Full Keyboard:**
  - Full typing, suggestions, swipe, undo/redo, clipboard history, emoji, text editing, language switch.
  - Suggestion strip mic triggers voice recording.
  - "Minimize" action (Lucide `chevron-down`) switches to pill mode.
- **Sensitive Fields:** Password & no-learning fields always open full keyboard with mic disabled.

---

## 7. PR Plan & Workflow

| PR | Title | Core Deliverables |
| --- | --- | --- |
| **PR 1** | Import & Scaffolding | FUTO source + submodules, license files, `MODIFICATIONS.md`, `applicationId`, shared signing & versioning, FUTO update checker disabled. Builds `assembleStableDebug`. |
| **PR 2** | ALTER Design System | ALTER theme as default, Lucide icons, logo & launcher icon, settings restyled with ALTER tokens. Payment pages restyled but functional. |
| **PR 3** | ALTER Voice Module | `alter-voice` module, model download & catalog, cleanup integration, removal of FUTO Whisper voice and 43MB bundled weight, mic wired to `commitText`. |
| **PR 4** | Pill Mode | Start-mode setting, 56dp pill bar with 4 states, Minimize action, password-field fallback rule. |
| **PR 5** | Optimization & Release | arm64-only, minified build (<100MB target), CI build/test workflow. |

### Checks Required Before Submitting Any PR:
1. Build verification (`./gradlew assembleStableDebug` and unit tests pass).
2. Signing check: APK SHA-1 matches `B5:E5:7C:D6:93:49:2E:96:ED:5E:96:30:0D:7C:AC:38:73:5B:7E:BB`.
3. No keystores, `.env`, `local.properties`, or weights committed.
4. Append completed checks and notes to `CHANGES.md`.
