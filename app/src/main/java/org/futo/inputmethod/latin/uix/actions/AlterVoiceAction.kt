package org.futo.inputmethod.latin.uix.actions

import android.Manifest
import android.content.Context
import android.content.pm.PackageManager
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.width
import androidx.core.content.ContextCompat
import com.aistudio.altermind.voice.DictationController
import com.aistudio.altermind.voice.SpeechModels
import com.aistudio.altermind.voice.ui.AlterVoicePill
import com.aistudio.altermind.voice.ui.PillState
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.launch
import org.futo.inputmethod.latin.R
import org.futo.inputmethod.latin.uix.Action
import org.futo.inputmethod.latin.uix.ALTER_END_ON_SILENCE
import org.futo.inputmethod.latin.uix.KeyboardManagerForAction
import org.futo.inputmethod.latin.uix.UixActionKeyboardManager
import org.futo.inputmethod.latin.uix.getSetting
import org.futo.inputmethod.latin.uix.settings.SettingsActivity
import org.futo.inputmethod.latin.uix.utils.ModelOutputSanitizer

/** Settings page for ALTER's voice pack; also opened when dictation cannot start (permission, no model). */
const val AlterVoiceNavPath = "alterVoice"

/**
 * One dictation session, shared by the full keyboard's mic strip and the pill bar. Text is inserted through the
 * keyboard's input connection (commitText at the cursor, replacing any selection) and is never sent.
 */
class AlterDictationSession(
    private val context: Context,
    private val scope: CoroutineScope,
    private val onFinished: (String) -> Unit
) {
    val controller = DictationController(context, scope)
    private val watcher: Job = scope.launch(Dispatchers.Main) {
        var wasProcessing = false
        controller.processing.collect { processing ->
            if (wasProcessing && !processing) {
                val text = controller.text.value
                if (controller.error.value == null && text.isNotBlank()) onFinished(text)
            }
            wasProcessing = processing
        }
    }

    /** Opens settings instead of failing when the microphone or a speech model is missing. */
    private fun readyOrOpenSettings(): Boolean {
        val permitted = ContextCompat.checkSelfPermission(context, Manifest.permission.RECORD_AUDIO) == PackageManager.PERMISSION_GRANTED
        val installed = runCatching { SpeechModels.get(context).checkInstalled() }.isSuccess
        if (permitted && installed) return true
        SettingsActivity.openToNavDest(context, AlterVoiceNavPath)
        return false
    }

    fun start() {
        if (!readyOrOpenSettings()) return
        controller.start(endAfterSilence = context.getSetting(ALTER_END_ON_SILENCE))
    }

    /** Idle: record. Listening: finish. Thinking: cancel. Error: retry the same recording without the mic. */
    fun tap() {
        when {
            controller.recording.value -> controller.stop()
            controller.processing.value -> controller.cancel()
            controller.error.value != null && controller.hasRetryRecording -> controller.retrySavedAudio()
            else -> start()
        }
    }

    /** Recording stops and is discarded when the field loses focus or the keyboard hides. */
    fun close() {
        controller.cancel()
        watcher.cancel()
    }

    @Composable
    fun Pill(disabled: Boolean = false, modifier: Modifier = Modifier.height(40.dp).width(176.dp)) {
        val recording by controller.recording.collectAsState()
        val processing by controller.processing.collectAsState()
        val error by controller.error.collectAsState()
        val level by controller.level.collectAsState()
        val state = when {
            disabled -> PillState.Disabled
            recording -> PillState.Listening
            processing -> PillState.Thinking
            error != null -> PillState.Error
            else -> PillState.Idle
        }
        val description = when (state) {
            PillState.Idle -> stringResource(R.string.alter_pill_idle)
            PillState.Listening -> stringResource(R.string.alter_pill_listening)
            PillState.Thinking -> stringResource(R.string.alter_pill_thinking)
            PillState.Error -> error + " " + stringResource(R.string.alter_pill_error_retry)
            PillState.Disabled -> stringResource(R.string.alter_pill_disabled)
        }
        AlterVoicePill(state, level, onTap = { tap() }, modifier = modifier, description = description)
    }
}

/** Inserts finished dictation at the cursor through a FUTO input transaction (commitText). */
fun KeyboardManagerForAction.commitDictation(text: String) {
    val transaction = createInputTransaction()
    transaction.commit(ModelOutputSanitizer.sanitize(text, transaction.textContext))
    announce(text)
}

/**
 * In the full keyboard the mic turns the suggestion row into ALTER's voice strip; the keys stay as they are.
 * Tapping the mic again acts like tapping the strip (finish, cancel or retry).
 */
val VoiceInputAction = Action(
    icon = R.drawable.mic_fill,
    name = R.string.action_voice_input_title,
    simplePressImpl = { manager, _ ->
        (manager as? UixActionKeyboardManager)?.uixManager?.toggleInlineVoice()
    },
    windowImpl = null
)

/** Switches the full keyboard to ALTER's pill bar. */
val AlterMinimizeAction = Action(
    icon = R.drawable.chevron_down,
    name = R.string.alter_action_minimize,
    simplePressImpl = { manager, _ ->
        (manager as? UixActionKeyboardManager)?.uixManager?.setPillMode(true)
    },
    windowImpl = null
)
