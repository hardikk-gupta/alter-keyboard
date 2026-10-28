package org.futo.inputmethod.latin.uix.settings.pages

import android.Manifest
import android.content.pm.PackageManager
import android.text.format.Formatter
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.runtime.DisposableEffect
import androidx.compose.ui.text.font.FontWeight
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.lifecycle.compose.LocalLifecycleOwner
import com.aistudio.altermind.voice.SpeechChoice
import org.futo.inputmethod.latin.uix.actions.AlterVoiceNavPath
import org.futo.inputmethod.latin.uix.settings.AlterType
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.material3.MaterialTheme
import org.futo.inputmethod.latin.uix.settings.AlterSettingsGroup
import org.futo.inputmethod.latin.uix.settings.CollapsibleSection
import org.futo.inputmethod.latin.uix.settings.DropDownPickerSettingItem
import androidx.compose.foundation.layout.Arrangement
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
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.semantics.selected
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
import org.futo.inputmethod.latin.uix.settings.useDataStore

/** Compact model row: name, size/selection, and one action. */
@Composable
private fun ModelRow(
    title: String, key: String, files: List<ModelSpec>, installed: Boolean, inUse: Boolean,
    onUse: (() -> Unit)?, onDelete: () -> Unit
) {
    val context = LocalContext.current
    val states by ModelDownloads.states.collectAsState()
    val download = states[key]
    Column {
        SettingItem(title = title, compact = true,
            modifier = Modifier.semantics { selected = inUse },
            subtitle = (if (inUse) stringResource(R.string.alter_model_in_use) + " · " else "") +
                Formatter.formatShortFileSize(context, files.sumOf { it.downloadBytes }),
            onClick = if (installed) onUse else null) {
            OutlinedButton(onClick = {
                when {
                    download is DownloadState.Running -> ModelDownloads.cancel(key)
                    !installed -> ModelDownloads.start(context, key, files)
                    else -> onDelete()
                }
            }) {
                Text(stringResource(when {
                    download is DownloadState.Running -> R.string.alter_model_cancel
                    !installed -> R.string.alter_model_download
                    else -> R.string.alter_model_delete
                }))
            }
        }
        when (download) {
            is DownloadState.Running -> LinearProgressIndicator(
                progress = { if (download.total > 0) download.bytes.toFloat() / download.total else 0f },
                modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 4.dp))
            is DownloadState.Failed -> Text(download.message, color = MaterialTheme.colorScheme.error,
                modifier = Modifier.padding(horizontal = 16.dp, vertical = 4.dp))
            null -> {}
        }
    }
}

/** Where ALTER voice setup stands; drives the home summary, the home setup card and this page's setup steps. */
enum class AlterVoiceState { NeedsMic, NeedsModel, Ready }
data class AlterVoiceStatus(val state: AlterVoiceState, val modelName: String?)

private fun readVoiceStatus(context: android.content.Context): AlterVoiceStatus {
    val mic = ContextCompat.checkSelfPermission(context, Manifest.permission.RECORD_AUDIO) == PackageManager.PERMISSION_GRANTED
    val models = SpeechModels.get(context)
    val ready = runCatching { models.checkInstalled() }.isSuccess
    val name = if (models.selected == VoiceSettings.DEVICE_SPEECH_ID) context.getString(R.string.alter_device_speech_name)
        else speechChoices(models.catalog).find { it.id == models.selected }?.name
    return AlterVoiceStatus(when { !mic -> AlterVoiceState.NeedsMic; !ready -> AlterVoiceState.NeedsModel; else -> AlterVoiceState.Ready },
        if (ready) name else null)
}

/**
 * Re-read on every resume (permission granted in Android settings), whenever a download changes state, and whenever
 * [selection] changes (pass the selected model id where the screen can change it).
 */
@Composable
fun rememberAlterVoiceStatus(selection: Any? = null): AlterVoiceStatus {
    val context = LocalContext.current
    val downloads by ModelDownloads.states.collectAsState()
    var status by remember { mutableStateOf(readVoiceStatus(context)) }
    LaunchedEffect(downloads, selection) { status = readVoiceStatus(context) }
    val lifecycle = LocalLifecycleOwner.current.lifecycle
    DisposableEffect(lifecycle) {
        val observer = LifecycleEventObserver { _, event -> if (event == Lifecycle.Event.ON_RESUME) status = readVoiceStatus(context) }
        lifecycle.addObserver(observer)
        onDispose { lifecycle.removeObserver(observer) }
    }
    return status
}

@Composable
fun AlterVoiceStatus.summary(): String = when (state) {
    AlterVoiceState.NeedsMic -> stringResource(R.string.alter_voice_status_needs_mic)
    AlterVoiceState.NeedsModel -> stringResource(R.string.alter_voice_status_needs_model)
    AlterVoiceState.Ready -> stringResource(R.string.alter_voice_status_ready, modelName ?: "")
}

/** Settings home: one clear next step until voice works (empty state with guidance), then nothing. */
@Composable
fun AlterVoiceSetupCard(navController: NavHostController) {
    val status = rememberAlterVoiceStatus()
    if (status.state == AlterVoiceState.Ready) return
    AlterSettingsGroup {
        Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
            Text(stringResource(R.string.alter_voice_setup_title), style = AlterType.RowTitle.copy(fontWeight = FontWeight.SemiBold))
            Text(status.summary(), style = AlterType.Supporting, color = MaterialTheme.colorScheme.onSurfaceVariant)
            Button(onClick = { navController.navigate(AlterVoiceNavPath) }) { Text(stringResource(R.string.alter_voice_setup_action)) }
        }
    }
}

@Composable
fun AlterVoiceScreen(navController: NavHostController) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    val models = remember { SpeechModels.get(context) }
    val choices = remember { speechChoices(models.catalog) }
    val cleanup = remember { models.catalog.find(VoicePack.CLEANUP_MODEL) }
    val downloads by ModelDownloads.states.collectAsState()
    // Re-read installed state and storage after downloads finish or models change.
    var refresh by remember { mutableIntStateOf(0) }
    LaunchedEffect(downloads.keys) { refresh++ }
    var selected by remember(refresh) { mutableStateOf(models.selected) }
    var storage by remember { mutableLongStateOf(0L) }
    LaunchedEffect(refresh) { storage = VoiceSettings.storageBytes(context) }
    var error by remember { mutableStateOf<String?>(null) }
    var language by remember { mutableStateOf(VoiceSettings.language(context)) }

    LaunchedEffect(Unit) {
        runCatching { VoiceSettings.removeRetiredDownloads(context) }
        refresh++
    }

    fun use(id: String) = scope.launch {
        error = runCatching { models.use(id) }.exceptionOrNull()?.message
        selected = models.selected
    }

    // A finished download is used straight away when no usable speech model is chosen (none yet, or the saved one
    // was removed or disabled): no extra "Use" tap. Missing Hindi files must not replace a valid base choice.
    LaunchedEffect(refresh) {
        val saved = choices.find { it.id == models.recordingSelected }
        if (models.selected != VoiceSettings.DEVICE_SPEECH_ID && (saved == null || !saved.installed(models.store)))
            choices.firstOrNull { it.installed(models.store) }?.let { use(it.id) }
    }

    val permission = rememberLauncherForActivityResult(ActivityResultContracts.RequestPermission()) { refresh++ }

    ScrollableList {
        ScreenTitle(stringResource(R.string.alter_voice_settings_title), showBack = true, navController)

        AlterSettingsGroup(stringResource(R.string.alter_speech_models_title)) {
            if (ContextCompat.checkSelfPermission(context, Manifest.permission.RECORD_AUDIO) != PackageManager.PERMISSION_GRANTED) {
                SettingItem(title = stringResource(R.string.alter_setup_step_mic), compact = true) {
                    Button(onClick = { permission.launch(Manifest.permission.RECORD_AUDIO) }) {
                        Text(stringResource(R.string.alter_setup_allow))
                    }
                }
            }
            choices.forEach { choice ->
                ModelRow(choice.name, choice.id, choice.files,
                    remember(refresh) { choice.installed(models.store) }, selected == choice.id,
                    onUse = { use(choice.id) },
                    onDelete = { scope.launch {
                        error = runCatching { VoiceSettings.deleteSpeech(context, choice) }.exceptionOrNull()?.message
                        selected = models.selected; refresh++
                    } })
            }
            if (VoiceSettings.deviceSpeechAvailable(context)) {
                SettingItem(title = stringResource(R.string.alter_device_speech_name), compact = true,
                    subtitle = if (selected == VoiceSettings.DEVICE_SPEECH_ID) stringResource(R.string.alter_model_in_use) else null,
                    onClick = { use(VoiceSettings.DEVICE_SPEECH_ID) }) {
                    RadioButton(selected = selected == VoiceSettings.DEVICE_SPEECH_ID,
                        onClick = { use(VoiceSettings.DEVICE_SPEECH_ID) })
                }
            }
            if (selected == VoiceSettings.DEVICE_SPEECH_ID || selected == "nemotron-3.5-streaming-q4") {
                val names = listOf(stringResource(R.string.alter_voice_language_auto),
                    stringResource(R.string.alter_voice_language_en_in), stringResource(R.string.alter_voice_language_hi_in))
                DropDownPickerSettingItem(stringResource(R.string.alter_voice_language_title), VoiceSettings.LANGUAGES, language,
                    { next -> scope.launch {
                        error = runCatching { VoiceSettings.setLanguage(context, next); language = next }.exceptionOrNull()?.message
                    } }, { names[VoiceSettings.LANGUAGES.indexOf(it).coerceAtLeast(0)] })
            }
        }
        error?.let { Text(it, color = MaterialTheme.colorScheme.error, modifier = Modifier.padding(16.dp)) }

        // 3. How the keyboard behaves with voice.
        AlterSettingsGroup(stringResource(R.string.alter_group_keyboard)) {
            val startMode = useDataStore(ALTER_START_MODE)
            val modes = listOf(ALTER_START_MODE_FULL, ALTER_START_MODE_PILL)
            val modeNames = listOf(stringResource(R.string.alter_start_mode_full), stringResource(R.string.alter_start_mode_pill))
            DropDownPickerSettingItem(stringResource(R.string.alter_start_mode_title), modes, startMode.value,
                { startMode.setValue(it) }, { modeNames[modes.indexOf(it).coerceAtLeast(0)] })
            SettingToggleDataStore(
                title = stringResource(R.string.alter_end_on_silence_title),
                setting = ALTER_END_ON_SILENCE
            )
        }

        CollapsibleSection(stringResource(R.string.alter_corrections_title)) {
            val corrections = remember { com.aistudio.altermind.voice.VoiceCorrections(context) }
            var entries by remember { mutableStateOf(runCatching { corrections.read() }.getOrDefault(emptyMap())) }
            var from by remember { mutableStateOf("") }
            var to by remember { mutableStateOf("") }
            var failure by remember { mutableStateOf<String?>(null) }
            androidx.compose.material3.OutlinedTextField(from, { from = it },
                label = { Text(stringResource(R.string.alter_corrections_heard)) }, singleLine = true,
                modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp))
            androidx.compose.material3.OutlinedTextField(to, { to = it },
                label = { Text(stringResource(R.string.alter_corrections_intended)) }, singleLine = true,
                modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp))
            OutlinedButton(onClick = {
                try { corrections.save(from.trim(), to.trim()); entries = corrections.read(); from = ""; to = ""; failure = null }
                catch (e: Exception) { failure = e.message }
            }, enabled = com.aistudio.altermind.voice.validVoiceCorrection(from.trim(), to.trim()),
                modifier = Modifier.padding(horizontal = 16.dp)) { Text(stringResource(R.string.alter_corrections_remember)) }
            failure?.let { Text(it, color = MaterialTheme.colorScheme.error, modifier = Modifier.padding(16.dp)) }
            entries.forEach { (heard, intended) ->
                SettingItem(title = "$heard → $intended", subtitle = "", onClick = null) {
                    OutlinedButton(onClick = {
                        try { corrections.delete(heard); entries = corrections.read(); failure = null }
                        catch (e: Exception) { failure = e.message }
                    }) { Text(stringResource(R.string.alter_model_delete)) }
                }
            }
        }

        // 4. Optional cleanup editor.
        AlterSettingsGroup(stringResource(R.string.alter_cleanup_title)) {
            val cleanupInstalled = remember(refresh) { models.store.installed(cleanup) }
            var cleanupOn by remember(refresh) { mutableStateOf(VoicePack.cleanupEnabled(context)) }
            SettingItem(title = stringResource(R.string.alter_cleanup_toggle), compact = true,
                disabled = !cleanupInstalled,
                onClick = { cleanupOn = !cleanupOn; VoicePack.setCleanupEnabled(context, cleanupOn) }) {
                Switch(checked = cleanupOn && cleanupInstalled, onCheckedChange = null, enabled = cleanupInstalled)
            }
            ModelRow(cleanup.displayName, cleanup.id, listOf(cleanup), cleanupInstalled, inUse = false, onUse = null,
                onDelete = { scope.launch {
                    error = runCatching { VoiceSettings.deleteCleanup(context) }.exceptionOrNull()?.message
                    refresh++
                } })
        }

        Text(
            stringResource(R.string.alter_storage_used, Formatter.formatShortFileSize(context, storage)),
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.padding(horizontal = 24.dp, vertical = 12.dp)
        )
    }
}
