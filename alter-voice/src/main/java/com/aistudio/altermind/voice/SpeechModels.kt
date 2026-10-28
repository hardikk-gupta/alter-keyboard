package com.aistudio.altermind.voice

import android.content.Context
import android.os.Build
import kotlinx.coroutines.*
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import java.io.File

data class SpeechChoice(val id: String, val name: String, val description: String, val files: List<ModelSpec>) {
    val bytes get() = files.sumOf { it.downloadBytes }
    fun installed(store: ModelStore) = files.all(store::installed)
}
fun speechChoices(catalog: ModelCatalog): List<SpeechChoice> {
    val standalone = catalog.models.filter { it.modelType == ModelType.SPEECH && it.family in setOf("Nemotron", "ParakeetCTC", "ParakeetTDT") }
        .map { SpeechChoice(it.id, it.displayName, it.description, listOf(it)) }
    val files = catalog.models.filter { it.family == "Moonshine" && "MOONSHINE_MEDIUM_STREAMING" in it.capabilities }
    val moonshine = if (files.isEmpty()) emptyList() else listOf(SpeechChoice("moonshine-en-medium",
        "Moonshine Medium", "English", files))
    return standalone + moonshine
}

/** Downloaded ASR beside the brain, plus an explicit live-device selector. No cloud/Gemma speech fallback. */
class SpeechModels(private val context: Context) : DictationSpeech {
    private val mutex = Mutex()
    private val prefs = voicePrefs(context)
    val catalog by lazy { ModelCatalog.load(context) }
    val store by lazy { ModelStore(context) }
    @Volatile private var nemotron = 0L
    @Volatile private var parakeetTdt = 0L
    private var parakeet: ParakeetSpeech? = null
    private var moonshine: ai.moonshine.voice.Transcriber? = null
    private var loaded: String? = null
    @Volatile private var activeOwner: Any? = null
    fun clearSelection() { prefs.edit().remove("speech_model").apply() }
    val recordingSelected get() = prefs.getString("speech_model", null)?.takeIf { it in retainedSpeechIds }
    override val selected get() = if (prefs.getBoolean("device_speech", false)) DEVICE_SPEECH else recordingSelected
    override fun checkInstalled() {
        if (selected == DEVICE_SPEECH) {
            check(deviceSpeechAvailable(context)) { "On-device recognition is unavailable. Install a system offline recognizer/language, or select a downloaded model." }
            return
        }
        val choice = speechChoices(catalog).find { it.id == selected }
            ?: error("Download and select a speech model in ALTER Keyboard settings → ALTER voice.")
        check(choice.installed(store)) { "Download the selected speech model in Voice settings." }
    }

    private var warmup: Job? = null
    fun preload() {
        if (DictationController.isActive() || warmup?.isActive == true || selected == null || selected == DEVICE_SPEECH) return
        warmup = scope.launch { try { mutex.withLock { if (!DictationController.isActive()) load() } } catch (e: CancellationException) { throw e } catch (_: Exception) { /* Setup/transcribe exposes a useful error on explicit use. */ } }
    }
    override suspend fun ensureReady() = withContext(Dispatchers.IO) { if (selected != DEVICE_SPEECH) mutex.withLock { load() } }
    suspend fun use(id: String) = withContext(Dispatchers.IO) {
        check(!DictationController.isActive()) { "Finish recording before changing speech models." }
        DictationController.speechSetup {
        mutex.withLock {
            if (id == DEVICE_SPEECH) {
                check(deviceSpeechAvailable(context)) { "On-device recognition is not available on this phone." }
                closeLocked(); prefs.edit().putBoolean("device_speech", true).commit()
                return@withLock
            }
            val choice = speechChoices(catalog).single { it.id == id }
            require(choice.installed(store)) { "Download the speech model first." }
            closeLocked(); load(choice)
            prefs.edit().putString("speech_model", id).putBoolean("device_speech", false).commit()
        }
        }
    }
    suspend fun release() = withContext(Dispatchers.IO) { mutex.withLock { closeLocked() } }
    @Synchronized override fun cancel(owner: Any) {
        if (activeOwner === owner) {
            if (nemotron != 0L) NemotronNative.cancel(nemotron)
            if (parakeetTdt != 0L) NemotronNative.cancel(parakeetTdt)
        }
    }
    private suspend fun load(choice: SpeechChoice = speechChoices(catalog).find { it.id == selected }
        ?: error("Retrying a recording needs a downloaded speech model. Device recognition supports live dictation only.")) {
        require(choice.installed(store)) { "The selected speech model is missing. Download it again in Voice settings." }
        if (loaded == choice.id) { moonshine?.setKeyterms(keyterms()); return }
        closeDecoderLocked()
        choice.files.forEach { store.verify(it) }
        try {
        if (choice.files.singleOrNull()?.family == "ParakeetTDT") {
            parakeetTdt = ParakeetTdtNative.load(store.file(choice.files.single()).absolutePath)
            check(parakeetTdt != 0L) { "Parakeet TDT could not load this verified model on your device." }
        } else if (choice.files.singleOrNull()?.family == "ParakeetCTC") {
            parakeet = ParakeetSpeech.open(store.file(choice.files.single()),
                File(context.noBackupFilesDir, "speech-parakeet/${choice.files.single().version}"))
        } else if (choice.files.singleOrNull()?.family == "Nemotron") {
            require(Build.VERSION.SDK_INT >= 26) { "Nemotron requires Android 8 or later." }
            nemotron = NemotronNative.load(store.file(choice.files.single()).absolutePath)
            check(nemotron != 0L) { "Nemotron could not load this verified model on your device." }
        } else {
            // Android may deny hard links even within private storage. Copy verified components.
            val directory = File(context.noBackupFilesDir, "speech-moonshine/${choice.id}/${choice.files.first().version}").apply { mkdirs() }
            choice.files.forEach { spec ->
                copySpeechComponent(store, spec, File(directory, spec.filename))
            }
            val candidate = ai.moonshine.voice.Transcriber()
            try { candidate.loadFromFiles(directory.absolutePath, ai.moonshine.voice.JNI.MOONSHINE_MODEL_ARCH_MEDIUM_STREAMING); candidate.setKeyterms(keyterms()); moonshine = candidate }
            catch (e: Throwable) { candidate.close(); throw e }
        }
        loaded = choice.id
        } catch (e: OutOfMemoryError) { closeLocked(); throw IllegalStateException("Not enough RAM for this speech model. Try Android on-device recognition or another downloaded model.", e) }
        catch (e: LinkageError) { throw IllegalStateException("The speech runtime cannot start on this device.", e) }
    }
    private fun keyterms(): List<String> = prefs.getString("voice_keyterms", "ALTER\nBRACE").orEmpty()
        .split(',', '\n').map { it.trim().take(60) }.filter { it.isNotBlank() }.distinct().take(32)
    override suspend fun transcribe(audio: FloatArray, owner: Any): String = withContext(Dispatchers.IO) {
        require(audio.isNotEmpty() && audio.size.toLong() <= MAX_DICTATION_SECONDS * 16000L) { "Use audio clips up to 15 minutes." }
        if (!hasAudibleSpeech(audio)) return@withContext "[NO_SPEECH_DETECTED]"
        mutex.withLock {
            activeOwner = owner
            try {
                load()
                // Nemotron keeps its native streaming context over the whole saved recording.
                if (nemotron != 0L) return@withLock decodeLoadedWindow(audio).ifBlank { "[NO_SPEECH_DETECTED]" }
                transcribeSpeechWindows(audio, decode = ::decodeLoadedWindow).ifBlank { "[NO_SPEECH_DETECTED]" }
            } finally { activeOwner = null }
        }
    }
    private suspend fun decodeLoadedWindow(audio: FloatArray): String {
        currentCoroutineContext().ensureActive()
        val raw = if (nemotron != 0L) {
            var offset = 0
            var text = ""
            streamNemotron(read = {
                if (offset == audio.size) null else {
                    val next = minOf(offset + 1600, audio.size)
                    audio.copyOfRange(offset, next).also { offset = next }
                }
            }, update = { text = it })
            text
        } else if (parakeetTdt != 0L) {
            NemotronNative.prepare(parakeetTdt)
            currentCoroutineContext().ensureActive()
            requireNotNull(ParakeetTdtNative.transcribe(parakeetTdt, audio,
                minOf(2, Runtime.getRuntime().availableProcessors()).coerceAtLeast(1))) {
                "Parakeet TDT transcription failed. Your recording is saved."
            }.decodeToString()
        } else if (parakeet != null) parakeet!!.decode(audio)
        else moonshine!!.transcribeWithoutStreaming(audio, 16000).text()
        currentCoroutineContext().ensureActive()
        return cleanDictation(raw)
    }
    suspend fun transcribe(audio: FloatArray): String = transcribe(audio, Any())
    /** Stream Moonshine or Nemotron while collecting from AudioRecord; return false for Parakeet's bounded clip mode. */
    override suspend fun stream(owner: Any, read: suspend () -> FloatArray?, update: (String) -> Unit): Boolean = withContext(Dispatchers.IO) {
        mutex.withLock {
            load()
            if (nemotron != 0L) {
                activeOwner = owner
                try { streamNemotron(read, update); return@withLock true }
                finally { activeOwner = null }
            }
            val engine = moonshine ?: return@withLock false
            activeOwner = owner
            val lines = linkedMapOf<Long, String>()
            val listener = java.util.function.Consumer<ai.moonshine.voice.TranscriptEvent> { event ->
                val line = when (event) {
                    is ai.moonshine.voice.TranscriptEvent.LineStarted -> event.line
                    is ai.moonshine.voice.TranscriptEvent.LineUpdated -> event.line
                    is ai.moonshine.voice.TranscriptEvent.LineTextChanged -> event.line
                    is ai.moonshine.voice.TranscriptEvent.LineCompleted -> event.line
                    else -> null
                }
                if (line != null) { lines[line.id] = line.text; update(cleanDictation(lines.values.joinToString(" "))) }
            }
            engine.addListener(listener)
            val stream = engine.createStream()
            engine.startStream(stream)
            try { while (true) { currentCoroutineContext().ensureActive(); val chunk = read() ?: break; engine.addAudioToStream(stream, chunk, 16000) }; engine.stopStream(stream); true }
            finally { engine.removeListener(listener); runCatching { engine.stopStream(stream) }; engine.freeStream(stream); activeOwner = null }
        }
    }
    private suspend fun streamNemotron(read: suspend () -> FloatArray?, update: (String) -> Unit) {
        currentCoroutineContext().ensureActive()
        NemotronNative.prepare(nemotron)
        try {
            check(NemotronNative.begin(nemotron, nemotronLanguage(prefs.getString("voice_language", "auto")),
                minOf(4, Runtime.getRuntime().availableProcessors()).coerceAtLeast(1))) {
                "Nemotron could not start the selected streaming profile. Your recording is saved."
            }
            consumeNemotronStream(read, update, { NemotronNative.feed(nemotron, it) },
                { NemotronNative.finish(nemotron) }, {})
        } finally { NemotronNative.end(nemotron) }
    }
    @Synchronized private fun closeLocked() {
        closeDecoderLocked()
        File(context.noBackupFilesDir, "speech-moonshine").deleteRecursively()
        File(context.noBackupFilesDir, "speech-parakeet").deleteRecursively()
        File(context.noBackupFilesDir, "speech-indicconformer").deleteRecursively()
    }
    @Synchronized private fun closeDecoderLocked() {
        if (nemotron != 0L) { NemotronNative.close(nemotron); nemotron = 0L }
        if (parakeetTdt != 0L) { NemotronNative.close(parakeetTdt); parakeetTdt = 0L }
        parakeet?.close(); parakeet = null
        moonshine?.close(); moonshine = null; loaded = null
    }
    companion object {
        private val scope = CoroutineScope(SupervisorJob() + Dispatchers.IO)
        private var shared: SpeechModels? = null
        @Synchronized fun get(context: Context) = shared ?: SpeechModels(context.applicationContext).also { shared = it }
    }
}

/** Recordings are capped at fifteen minutes, as in ALTER. */
internal const val MAX_DICTATION_SECONDS = 15 * 60

internal fun hasAudibleSpeech(audio: FloatArray): Boolean = audio.count { kotlin.math.abs(it) > .001f } >= minOf(800, audio.size / 8).coerceAtLeast(1)
/** Copy into the native runtime's canonical directory without changing access permissions. */
internal suspend fun copySpeechComponent(store: ModelStore, spec: ModelSpec, target: File) {
    val partial = File(target.parentFile, target.name + ".part")
    try {
        check(target.parentFile!!.isDirectory || target.parentFile!!.mkdirs()) { "Could not prepare private speech storage." }
        store.file(spec).inputStream().use { input -> partial.outputStream().use { output ->
            val buffer = ByteArray(256 * 1024)
            while (true) {
                currentCoroutineContext().ensureActive()
                val count = input.read(buffer)
                if (count < 0) break
                output.write(buffer, 0, count)
            }
        } }
        store.verify(spec, partial)
        currentCoroutineContext().ensureActive()
        check(partial.renameTo(target)) { "Could not install private speech component." }
    } finally { partial.delete() }
}
internal fun wavSamples(wav: ByteArray): FloatArray {
    require(wav.size >= 44 && String(wav, 0, 4) == "RIFF" && String(wav, 36, 4) == "data") { "Expected decoded mono PCM16 WAV." }
    val shorts = java.nio.ByteBuffer.wrap(wav, 44, wav.size - 44).order(java.nio.ByteOrder.LITTLE_ENDIAN).asShortBuffer()
    return FloatArray(shorts.remaining()) { shorts.get() / 32768f }
}
