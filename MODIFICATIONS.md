# Modifications & Licensing Notice

**ALTER Keyboard is a modified version of FUTO Keyboard by FUTO Holdings, Inc.**

## FUTO License Compliance

This project is derived from the FUTO Keyboard codebase (`org.futo.inputmethod.latin`), available at [https://github.com/futo-org/android-keyboard](https://github.com/futo-org/android-keyboard).

- **License:** FUTO Source First License 1.1 (and Apache 2.0 for original AOSP components).
- **Personal and Non-Commercial:** This software is for personal, non-commercial use and is distributed free of charge.
- **Payment Features:** FUTO payment features, including the payment settings screen, `PaymentCompleteActivity`, and payment reminders are retained and operational in accordance with license requirements.
- **Copyrights:** All original FUTO and AOSP copyright notices are retained in source headers.

## Summary of Modifications

1. **Rebranding & Identity:**
   - App and IME Name: `ALTER Keyboard`
   - Application ID: `com.aistudio.altermind.keyboard`
   - Custom app icons and brand assets based on the ALTER visual identity.

2. **Design System & Theming:**
   - Integrated ALTER's design system tokens: `PitchBlack` (#000000), `SlateGrayCard` (#18181B), `RaisedSurface` (#262626), `AlterYellow` (#FFBD00), `ErrorRed` (#EF4444), and Lucide icons.
   - S1 thinking orb animation for processing states.
   - Live audio RMS wave visualization during dictation.

3. **Pill Mode & Minimize Action:**
   - Dedicated 56dp minimized pill mode providing one-tap voice input with Idle, Listening, Thinking, and Error states.
   - Toggle action to switch between Pill Mode and the Full Keyboard.

4. **ALTER Voice Pack (`alter-voice/`):**
   - Direct integration of ALTER's on-device voice transcription (Moonshine and Whisper live engines).
   - Text post-processing and cleanup using LFM2.5 350M INT8 on LiteRT-LM.
   - Direct text insertion into focused input via `currentInputConnection.commitText()`.
   - Disabled FUTO's proprietary bundled voice model in favor of dynamic on-demand model downloads.

5. **Update Checker:**
   - Disabled FUTO update checker mechanism to avoid conflicting upstream updates.
