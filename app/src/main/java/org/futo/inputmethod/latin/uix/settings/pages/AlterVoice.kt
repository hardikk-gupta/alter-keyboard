package org.futo.inputmethod.latin.uix.settings.pages

import android.Manifest
import android.content.pm.PackageManager
import android.text.format.Formatter
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.ui.draw.clip
import org.futo.inputmethod.latin.uix.settings.AlterSettingsGroup
import org.futo.inputmethod.latin.uix.settings.CollapsibleSection
import org.futo.inputmethod.latin.uix.settings.DropDownPickerSettingItem
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Button
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.RadioButton
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableLongStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.core.content.ContextCompat
import androidx.navigation.NavHostController
import com.aistudio.altermind.voice.DownloadState
import com.aistudio.altermind.voice.ModelDownloads
import com.aistudio.altermind.voice.ModelSpec
import com.aistudio.altermind.voice.SpeechModels
import com.aistudio.altermind.voice.VoicePack
import com.aistudio.altermind.voice.VoiceSettings
import com.aistudio.altermind.voice.speechChoices
import kotlinx.coroutines.launch
import org.futo.inputmethod.latin.R
import org.futo.inputmethod.latin.uix.ALTER_END_ON_SILENCE
import org.futo.inputmethod.latin.uix.ALTER_START_MODE
import org.futo.inputmethod.latin.uix.ALTER_START_MODE_FULL
import org.futo.inputmethod.latin.uix.ALTER_START_MODE_PILL
import org.futo.inputmethod.latin.uix.settings.ScreenTitle
import org.futo.inputmethod.latin.uix.settings.ScrollableList
import org.futo.inputmethod.latin.uix.settings.SettingItem
import org.futo.inputmethod.latin.uix.settings.SettingToggleDataStore
import org.futo.inputmethod.latin.uix.settings.Tip
import org.futo.inputmethod.latin.uix.settings.useDataStore

/** One model row: download with progress, cancel, use, delete. */
@Composable
private fun ModelRow(
    title: String, subtitle: String, key: String, files: List<ModelSpec>, installed: Boolean, inUse: Boolean,
    onUse: (() -> Unit)?, onDelete: () -> Unit
) {
    val context = LocalContext.current
    val states by ModelDownloads.states.collectAsState()
    val download = states[key]
    val total = files.sumOf { it.downloadBytes }
    SettingItem(
        title = title,
        subtitle = listOf(subtitle, Formatter.formatShortFileSize(context, total)).filter { it.isNotBlank() }.joinToString(" · "),
        icon = if (onUse != null) {{ RadioButton(selected = inUse, onClick = if (installed && !inUse) onUse else null) }} else null,
        subcontent = {
            when (download) {
                is DownloadState.Running -> {
                    if (download.verifying) Text(stringResource(R.string.alter_model_verifying))
                    LinearProgressIndicator(progress = { if (download.total > 0) download.bytes.toFloat() / download.total else 0f },
                        modifier = Modifier.padding(vertical = 4.dp))
                }
                is DownloadState.Failed -> Text(download.message)
                null -> {}
            }
        }
    ) {
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            when {
                download is DownloadState.Running -> OutlinedButton(onClick = { ModelDownloads.cancel(key) }) {
                    Text(stringResource(R.string.alter_model_cancel))
                }
                !installed -> OutlinedButton(onClick = { ModelDownloads.start(context, key, files) }) {
                    Text(stringResource(R.string.alter_model_download))
                }
                else -> OutlinedButton(onClick = onDelete) { Text(stringResource(R.string.alter_model_delete)) }
            }
        }
    }
}

@Composable
fun AlterVoiceScreen(navController: NavHostController) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    val models = remember { SpeechModels.get(context) }
    val choices = remember { speechChoices(models.catalog) }
    val cleanup = remember { VoiceSettings.cleanupSpec(context) }
    val downloads by ModelDownloads.states.collectAsState()

    // Re-read installed state and storage after downloads finish or models change.
    var refresh by remember { mutableIntStateOf(0) }
    LaunchedEffect(downloads.keys) { refresh++ }
    var selected by remember(refresh) { mutableStateOf(models.selected) }
    var storage by remember { mutableLongStateOf(0L) }
    LaunchedEffect(refresh) { storage = VoiceSettings.storageBytes(context) }
    var error by remember { mutableStateOf<String?>(null) }

    fun use(id: String) = scope.launch {
        error = runCatching { models.use(id) }.exceptionOrNull()?.message
        selected = models.selected
    }

    var micGranted by remember {
        mutableStateOf(ContextCompat.checkSelfPermission(context, Manifest.permission.RECORD_AUDIO) == PackageManager.PERMISSION_GRANTED)
    }
    val permission = rememberLauncherForActivityResult(ActivityResultContracts.RequestPermission()) { micGranted = it }

    ScrollableList {
        ScreenTitle(stringResource(R.string.alter_voice_settings_title), showBack = true, navController)

        if (!micGranted) {
            AlterSettingsGroup(null) {
                SettingItem(title = stringResource(R.string.alter_mic_permission_title),
                    subtitle = stringResource(R.string.alter_mic_permission_subtitle),
                    onClick = { permission.launch(Manifest.permission.RECORD_AUDIO) }) {}
            }
        }

        AlterSettingsGroup(stringResource(R.string.alter_group_keyboard)) {
            val startMode = useDataStore(ALTER_START_MODE)
            val modes = listOf(ALTER_START_MODE_FULL, ALTER_START_MODE_PILL)
            val modeNames = listOf(stringResource(R.string.alter_start_mode_full), stringResource(R.string.alter_start_mode_pill))
            DropDownPickerSettingItem(stringResource(R.string.alter_start_mode_title), modes, startMode.value,
                { startMode.setValue(it) }, { modeNames[modes.indexOf(it).coerceAtLeast(0)] })
            SettingToggleDataStore(
                title = stringResource(R.string.alter_end_on_silence_title),
                subtitle = stringResource(R.string.alter_end_on_silence_subtitle),
                setting = ALTER_END_ON_SILENCE
            )
            val languageNames = listOf(
                stringResource(R.string.alter_voice_language_auto),
                stringResource(R.string.alter_voice_language_en_in),
                stringResource(R.string.alter_voice_language_hi_in)
            )
            var language by remember { mutableStateOf(VoiceSettings.language(context)) }
            DropDownPickerSettingItem(stringResource(R.string.alter_voice_language_title), VoiceSettings.LANGUAGES, language,
                { VoiceSettings.setLanguage(context, it); language = it },
                { languageNames[VoiceSettings.LANGUAGES.indexOf(it).coerceAtLeast(0)] })
        }

        AlterSettingsGroup(stringResource(R.string.alter_speech_models_title)) {
            val current = choices.find { it.id == selected }?.name
                ?: if (selected == VoiceSettings.DEVICE_SPEECH_ID) stringResource(R.string.alter_device_speech_name) else null
            SettingItem(title = current ?: stringResource(R.string.alter_speech_model_none),
                subtitle = stringResource(if (current != null) R.string.alter_model_in_use else R.string.alter_speech_model_none_hint)) {}
            error?.let { Tip(it) }
            CollapsibleSection(stringResource(R.string.alter_speech_models_all, choices.size + if (VoiceSettings.deviceSpeechAvailable(context)) 1 else 0)) {
                choices.forEach { choice ->
                    val installed = remember(refresh) { choice.installed(models.store) }
                    ModelRow(choice.name, choice.description.substringBefore(". ").removeSuffix("."), choice.id, choice.files,
                        installed, selected == choice.id,
                        onUse = { use(choice.id) },
                        onDelete = { scope.launch {
                            error = runCatching { VoiceSettings.deleteSpeech(context, choice) }.exceptionOrNull()?.message
                            refresh++
                        } })
                }
                if (VoiceSettings.deviceSpeechAvailable(context)) {
                    SettingItem(
                        title = stringResource(R.string.alter_device_speech_name),
                        subtitle = stringResource(R.string.alter_device_speech_description),
                        icon = { RadioButton(selected = selected == VoiceSettings.DEVICE_SPEECH_ID, onClick = null) },
                        onClick = { use(VoiceSettings.DEVICE_SPEECH_ID) }
                    ) {}
                }
                Tip(stringResource(R.string.alter_speech_models_hint))
            }
        }

        AlterSettingsGroup(stringResource(R.string.alter_cleanup_title)) {
            val cleanupInstalled = remember(refresh) { models.store.installed(cleanup) }
            var cleanupOn by remember(refresh) { mutableStateOf(VoicePack.cleanupEnabled(context)) }
            SettingItem(title = stringResource(R.string.alter_cleanup_toggle), subtitle = stringResource(R.string.alter_cleanup_subtitle),
                disabled = !cleanupInstalled,
                onClick = { cleanupOn = !cleanupOn; VoicePack.setCleanupEnabled(context, cleanupOn) }) {
                Switch(checked = cleanupOn && cleanupInstalled, onCheckedChange = null, enabled = cleanupInstalled)
            }
            ModelRow(cleanup.displayName, "", cleanup.id, listOf(cleanup), cleanupInstalled, inUse = false, onUse = null,
                onDelete = { scope.launch {
                    VoicePack.setCleanupEnabled(context, false)
                    ModelDownloads.delete(context, listOf(cleanup))
                    refresh++
                } })
        }

        Text(
            stringResource(R.string.alter_storage_used, Formatter.formatShortFileSize(context, storage)) + "\n" +
                stringResource(R.string.alter_about_modified),
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.padding(horizontal = 24.dp, vertical = 12.dp)
        )
    }
}
