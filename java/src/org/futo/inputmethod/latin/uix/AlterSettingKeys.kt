package org.futo.inputmethod.latin.uix

import android.text.InputType
import android.view.inputmethod.EditorInfo
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.stringPreferencesKey

// ALTER Keyboard additions. ALTER Keyboard is a modified version of FUTO Keyboard by FUTO Holdings, Inc.

const val ALTER_START_MODE_FULL = "full"
const val ALTER_START_MODE_PILL = "pill"

/** The mode the keyboard opens in each time a text field gets focus: the full FUTO keyboard or ALTER's mic pill. */
val ALTER_START_MODE = SettingsKey(
    key = stringPreferencesKey("alter_start_mode"),
    default = ALTER_START_MODE_FULL
)

/** Recording ends by itself after a pause in speech. Off by default: the user taps to finish. */
val ALTER_END_ON_SILENCE = SettingsKey(
    key = booleanPreferencesKey("alter_end_on_silence"),
    default = false
)

/**
 * Password and sensitive fields always open the full keyboard with the mic disabled, whatever the start mode.
 */
fun EditorInfo?.isAlterSensitiveField(): Boolean {
    if (this == null) return false
    val variation = inputType and InputType.TYPE_MASK_VARIATION
    val inputClass = inputType and InputType.TYPE_MASK_CLASS
    val password = when (inputClass) {
        InputType.TYPE_CLASS_TEXT -> variation == InputType.TYPE_TEXT_VARIATION_PASSWORD ||
            variation == InputType.TYPE_TEXT_VARIATION_VISIBLE_PASSWORD ||
            variation == InputType.TYPE_TEXT_VARIATION_WEB_PASSWORD
        InputType.TYPE_CLASS_NUMBER -> variation == InputType.TYPE_NUMBER_VARIATION_PASSWORD
        else -> false
    }
    return password || (imeOptions and EditorInfo.IME_FLAG_NO_PERSONALIZED_LEARNING) != 0
}
