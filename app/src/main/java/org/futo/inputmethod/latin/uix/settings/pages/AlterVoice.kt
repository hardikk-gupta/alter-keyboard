package org.futo.inputmethod.latin.uix.settings.pages

import android.Manifest
import android.content.pm.PackageManager
import android.text.format.Formatter
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.size
import androidx.compose.runtime.DisposableEffect
import androidx.compose.ui.Alignment
import androidx.compose.ui.text.font.FontWeight
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.lifecycle.compose.LocalLifecycleOwner
import com.aistudio.altermind.voice.SpeechChoice
import com.aistudio.altermind.voice.ui.AlterColors
import org.futo.inputmethod.latin.uix.actions.AlterVoiceNavPath
import org.futo.inputmethod.latin.uix.settings.AlterType
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
    onUse: (() -> Unit)?, onDelete: () -> Unit, primary: Boolean = false, stepNumber: Int? = null
) {
    val context = LocalContext.current
    val states by ModelDownloads.states.collectAsState()
    val download = states[key]
    val total = files.sumOf { it.downloadBytes }
    SettingItem(
        title = title,
        subtitle = listOf(subtitle, Formatter.formatShortFileSize(context, total)).filter { it.isNotBlank() }.joinToString(" · "),
        icon = when {
            stepNumber != null -> {{ StepBadge(stepNumber, done = installed) }}
            onUse != null -> {{ RadioButton(selected = inUse, onClick = if (installed && !inUse) onUse else null) }}
            else -> null
        },
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
                !installed && primary -> Button(onClick = { ModelDownloads.start(context, key, files) }) {
                    Text(stringResource(R.string.alter_model_download))
                }
                !installed -> OutlinedButton(onClick = { ModelDownloads.start(context, key, files) }) {
                    Text(stringResource(R.string.alter_model_download))
                }
                stepNumber != null -> {}
                else -> OutlinedButton(onClick = onDelete) { Text(stringResource(R.string.alter_model_delete)) }
            }
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

/** Explicit Hindi setup uses the Hindi engine; English retains the smallest live recognizer. */
private fun recommendedChoice(choices: List<SpeechChoice>, language: String) =
    choices.find { it.id == if (language == "hi-IN") com.aistudio.altermind.voice.INDIC_HINDI_MODEL else "moonshine-en" } ?: choices.first()

@Composable
fun AlterVoiceScreen(navController: NavHostController) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    val models = remember { SpeechModels.get(context) }
    val choices = remember { speechChoices(models.catalog) }
    var cleanupId by remember { mutableStateOf(VoicePack.cleanupModel(context)) }
    val cleanup = remember(cleanupId) { models.catalog.find(cleanupId) }
    val downloads by ModelDownloads.states.collectAsState()
    val routingFiles = remember { com.aistudio.altermind.voice.automaticRoutingSpecs(models.catalog) }
    val routingNotice by models.routingNotice.collectAsState()

    // Re-read installed state and storage after downloads finish or models change.
    var refresh by remember { mutableIntStateOf(0) }
    LaunchedEffect(downloads.keys) { refresh++ }
    var selected by remember(refresh) { mutableStateOf(models.selected) }
    var storage by remember { mutableLongStateOf(0L) }
    LaunchedEffect(refresh) { storage = VoiceSettings.storageBytes(context) }
    val status = rememberAlterVoiceStatus(selected)
    var error by remember { mutableStateOf<String?>(null) }
    var language by remember { mutableStateOf(VoiceSettings.language(context)) }

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

        // 1. Setup, only while something is missing: numbered steps, one primary action each.
        if (status.state != AlterVoiceState.Ready) {
            AlterSettingsGroup(stringResource(R.string.alter_voice_setup_title)) {
                SettingItem(
                    title = stringResource(R.string.alter_setup_step_mic),
                    subtitle = stringResource(R.string.alter_mic_permission_subtitle),
                    icon = { StepBadge(1, done = status.state != AlterVoiceState.NeedsMic) }
                ) {
                    if (status.state == AlterVoiceState.NeedsMic) {
                        Button(onClick = { permission.launch(Manifest.permission.RECORD_AUDIO) }) {
                            Text(stringResource(R.string.alter_setup_allow))
                        }
                    }
                }
                val base = choices.find { it.id == models.recordingSelected }
                if (models.automaticRouting && base?.installed(models.store) == true) {
                    ModelRow(stringResource(R.string.alter_auto_route_download),
                        stringResource(R.string.alter_auto_route_hint), com.aistudio.altermind.voice.AUTO_ROUTING_DOWNLOAD,
                        routingFiles, installed = routingFiles.all(models.store::installed), inUse = true,
                        onUse = null, onDelete = {}, primary = true, stepNumber = 2)
                } else {
                    val recommended = if (models.automaticRouting && base != null) base else recommendedChoice(choices, language)
                    ModelRow(stringResource(R.string.alter_setup_step_model, recommended.name),
                        stringResource(R.string.alter_setup_step_model_hint), recommended.id, recommended.files,
                        installed = remember(refresh) { recommended.installed(models.store) }, inUse = selected == recommended.id,
                        onUse = null, onDelete = {}, primary = true, stepNumber = 2)
                }
            }
        }

        // 2. Speech: the model in use, its language, and every other model behind one disclosure.
        AlterSettingsGroup(stringResource(R.string.alter_speech_models_title)) {
            val current = choices.find { it.id == selected }?.name
                ?: if (selected == VoiceSettings.DEVICE_SPEECH_ID) stringResource(R.string.alter_device_speech_name) else null
            SettingItem(title = current ?: stringResource(R.string.alter_speech_model_none),
                subtitle = stringResource(if (models.automaticRouting) R.string.alter_auto_route_base else if (current != null) R.string.alter_model_in_use else R.string.alter_speech_model_none_hint)) {}
            val languageNames = listOf(
                stringResource(R.string.alter_voice_language_auto),
                stringResource(R.string.alter_voice_language_en_in),
                stringResource(R.string.alter_voice_language_hi_in)
            )
            DropDownPickerSettingItem(stringResource(R.string.alter_voice_language_title), VoiceSettings.LANGUAGES, language,
                { next -> scope.launch {
                    error = runCatching {
                        VoiceSettings.setLanguage(context, next)
                        language = next
                        selected = models.selected
                        refresh++
                    }.exceptionOrNull()?.message
                } },
                { languageNames[VoiceSettings.LANGUAGES.indexOf(it).coerceAtLeast(0)] })
            if (language == "hi-IN" && selected == com.aistudio.altermind.voice.INDIC_HINDI_MODEL &&
                models.recordingSelected != selected) {
                Tip(stringResource(R.string.alter_hindi_routing_hint))
            }
            if (models.automaticRouting) {
                val routingInstalled = routingFiles.all(models.store::installed)
                val downloadShownInSetup = status.state != AlterVoiceState.Ready &&
                    choices.find { it.id == models.recordingSelected }?.installed(models.store) == true && !routingInstalled
                if (!downloadShownInSetup) {
                    ModelRow(stringResource(R.string.alter_auto_route_download), stringResource(R.string.alter_auto_route_hint),
                        com.aistudio.altermind.voice.AUTO_ROUTING_DOWNLOAD, routingFiles,
                        installed = routingInstalled, inUse = true, onUse = null,
                        onDelete = { scope.launch {
                            error = runCatching { VoiceSettings.deleteAutomaticRouting(context) }.exceptionOrNull()?.message
                            refresh++
                        } })
                }
                routingNotice?.let { Tip(it) }
            }
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

        // 3. How the keyboard behaves with voice.
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
        }

        AlterSettingsGroup(stringResource(R.string.alter_corrections_title)) {
            val corrections = remember { com.aistudio.altermind.voice.VoiceCorrections(context) }
            var entries by remember { mutableStateOf(runCatching { corrections.read() }.getOrDefault(emptyMap())) }
            var from by remember { mutableStateOf("") }
            var to by remember { mutableStateOf("") }
            var failure by remember { mutableStateOf<String?>(null) }
            Text(stringResource(R.string.alter_corrections_help), modifier = Modifier.padding(16.dp))
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
            DropDownPickerSettingItem(stringResource(R.string.alter_cleanup_engine),
                listOf(VoicePack.CLEANUP_MODEL, VoicePack.HANDY_MODEL), cleanupId,
                { cleanupId = it; VoicePack.selectCleanupModel(context, it)
                    if (it != VoicePack.HANDY_MODEL) scope.launch { VoicePack.releaseHandy() }
                },
                { models.catalog.find(it).displayName })
            Text(stringResource(R.string.alter_cleanup_live_note), modifier = Modifier.padding(16.dp))
            val cleanupInstalled = remember(refresh, cleanupId) { models.store.installed(cleanup) }
            var cleanupOn by remember(refresh) { mutableStateOf(VoicePack.cleanupEnabled(context)) }
            SettingItem(title = stringResource(R.string.alter_cleanup_toggle), subtitle = stringResource(R.string.alter_cleanup_subtitle),
                disabled = !cleanupInstalled,
                onClick = { cleanupOn = !cleanupOn; VoicePack.setCleanupEnabled(context, cleanupOn) }) {
                Switch(checked = cleanupOn && cleanupInstalled, onCheckedChange = null, enabled = cleanupInstalled)
            }
            ModelRow(cleanup.displayName, cleanup.description, cleanup.id, listOf(cleanup), cleanupInstalled, inUse = false, onUse = null,
                onDelete = { scope.launch {
                    if (com.aistudio.altermind.voice.DictationController.isActive()) { error = "Pause dictation before deleting a cleanup model."; return@launch }
                    VoicePack.setCleanupEnabled(context, false)
                    com.aistudio.altermind.voice.VoicePack.releaseHandy()
                    ModelDownloads.delete(context, listOf(cleanup))
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

/** Numbered step marker; yellow when done (yellow means progress/selection in ALTER's design). */
@Composable
private fun StepBadge(number: Int, done: Boolean) {
    Box(Modifier.size(28.dp).clip(RoundedCornerShape(14.dp))
        .background(if (done) AlterColors.AlterYellow else MaterialTheme.colorScheme.onSurface.copy(alpha = 0.08f)),
        contentAlignment = Alignment.Center) {
        Text(if (done) "✓" else number.toString(), style = AlterType.Meta.copy(fontWeight = FontWeight.SemiBold),
            color = if (done) AlterColors.PitchBlack else MaterialTheme.colorScheme.onSurface)
    }
}
