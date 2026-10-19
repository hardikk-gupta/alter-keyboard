package org.futo.inputmethod.latin.uix.actions

import org.futo.inputmethod.latin.R
import org.futo.inputmethod.latin.uix.Action

// ALTER Keyboard: FUTO's built-in Whisper voice input is replaced by ALTER's voice pack (AlterVoiceAction.kt).
// The option to hand voice input to another installed voice keyboard is kept.

val SystemVoiceInputAction = Action(
    icon = R.drawable.mic_fill,
    name = R.string.action_system_voice_input_title,
    simplePressImpl = { it, _ ->
        it.triggerSystemVoiceInput()
    },
    persistentState = null,
    windowImpl = null,
    shownInEditor = false
)
