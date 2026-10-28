package com.aistudio.altermind.voice

import android.content.Context
import android.content.pm.PackageManager
import androidx.core.content.ContextCompat
import kotlinx.coroutines.*
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import java.io.File

/** Capture is journaled before decoding. A hide, interruption or pause never discards the pending draft. */
class DictationController internal constructor(private val context: Context, private val scope: CoroutineScope,
    private val speech: DictationSpeech, private val openMicrophone: () -> DictationMicrophone,
    private val polish: suspend (String) -> String, private val drafts: RecordingDraftStore,
    private val canRecord: () -> Boolean, private val dispatcher: CoroutineDispatcher = Dispatchers.IO,
    private val liveCleanup: () -> Boolean = { false }) {
    constructor(context: Context, scope: CoroutineScope) : this(context, scope, SpeechModels.get(context),
        { AndroidDictationMicrophone(context) }, { VoicePack.polish(context, it) }, draftStore(context),
        { ContextCompat.checkSelfPermission(context, android.Manifest.permission.RECORD_AUDIO) == PackageManager.PERMISSION_GRANTED },
        liveCleanup = { VoicePack.cleanupEnabled(context) })

    private val mutableStarting = MutableStateFlow(false)
    val starting = mutableStarting.asStateFlow()
    private val mutableRecording = MutableStateFlow(false)
    val recording = mutableRecording.asStateFlow()
    private val mutablePaused = MutableStateFlow(false)
    val paused = mutablePaused.asStateFlow()
    private val mutableProcessing = MutableStateFlow(false)
    val processing = mutableProcessing.asStateFlow()
    private val mutableText = MutableStateFlow("")
    val text = mutableText.asStateFlow()
    private val mutableCompletedText = MutableStateFlow<String?>(null)
    val completedText = mutableCompletedText.asStateFlow()
    private val mutableError = MutableStateFlow<String?>(null)
    val error = mutableError.asStateFlow()
    private val mutableLevel = MutableStateFlow(0f)
    val level = mutableLevel.asStateFlow()
    private val mutableBackend = MutableStateFlow("")
    val backend = mutableBackend.asStateFlow()
    private val mutableStatus = MutableStateFlow<String?>(null)
    val status = mutableStatus.asStateFlow()
    private var job: Job? = null
    @Volatile private var captureRequested = false
    private var deviceSession: DeviceSpeechSession? = null
    private var draftId: String? = null
    private var unreadableDraft = false
    @Volatile private var stopRequested = false
    @Volatile private var epoch = 0
    val noSpeechError: Boolean get() = mutableError.value == NO_SPEECH_MESSAGE
    val hasRetryRecording: Boolean get() = runCatching { drafts.load()?.hasContent == true }.getOrDefault(false)

    init {
        try {
            drafts.load()?.let {
                draftId = it.id; mutablePaused.value = it.hasContent
                mutableText.value = it.completed ?: it.deviceText
            }
        } catch (e: Exception) { unreadableDraft = true; mutableError.value = "Cannot read the saved recording: ${e.message}" }
    }

    fun start(endAfterSilence: Boolean = true) = launchSession(capture = true, endAfterSilence = endAfterSilence)
    fun retrySavedAudio() = launchSession(capture = false, endAfterSilence = false)

    @Synchronized private fun launchSession(capture: Boolean, endAfterSilence: Boolean) {
        if (!scope.isActive || (job?.isActive == true && !mutablePaused.value)) return
        val saved = try { drafts.load()?.takeIf { it.hasContent } }
            catch (e: Exception) { mutableError.value = e.message; return }
        if (!capture && saved == null) { mutableError.value = "No saved recording is available. Record again."; return }
        if (capture && !canRecord()) { mutableError.value = "Allow microphone access for ALTER Keyboard, then try again."; return }
        fun validateSpeechSelection() {
            if (capture || (saved?.model != DEVICE_SPEECH && saved?.completed == null)) {
                if (saved != null) check(saved.model == speech.selected) { "Select ${saved.model} again to resume this recording, or discard it." }
                speech.checkInstalled()
            }
        }
        try { validateSpeechSelection() }
        catch (e: Exception) { mutableError.value = e.message; return }
        val previous = job
        val generation = ++epoch
        stopRequested = false
        captureRequested = capture
        mutablePaused.value = false; mutableCompletedText.value = null
        mutableError.value = null; mutableText.value = ""; mutableStarting.value = capture; mutableProcessing.value = !capture
        mutableStatus.value = if (capture) "Starting microphone…" else "Processing saved recording…"
        val sessionClaim = Any()
        val task = scope.launch(dispatcher, start = CoroutineStart.LAZY) {
            var microphone: DictationMicrophone? = null
            val preview = if (capture && liveCleanup()) LiveDictationCleanup(this, text, recording, polish) else null
            try {
                // Pause/resume can be tapped quickly; wait for the previous decoder to release the shared engine.
                previous?.join(); ensureActive()
                while (!owner.compareAndSet(null, sessionClaim)) { delay(25); ensureActive() }
                // Model/language setup may have changed the effective engine while this session waited.
                validateSpeechSelection()
                val draft = if (saved == null) drafts.begin(checkNotNull(speech.selected))
                    else if (capture) drafts.resume(saved.id) else saved
                synchronized(this@DictationController) { if (generation == epoch) draftId = draft.id }
                if (!capture && draft.completed != null) {
                    publishCompleted(generation, draft.completed)
                    return@launch
                }
                mutableBackend.value = draft.model
                if (draft.model == DEVICE_SPEECH) {
                    if (!capture) { finishTranscript(generation, draft.id, draft.deviceText, preview); return@launch }
                    val session = DeviceSpeechSession(context)
                    synchronized(this@DictationController) {
                        if (generation != epoch) throw CancellationException("Dictation paused")
                        deviceSession = session
                    }
                    val result = withTimeoutOrNull(900_000) {
                        session.listen(voicePrefs(context).getString("voice_language", "auto") ?: "auto",
                            { partial -> synchronized(this@DictationController) {
                                if (generation == epoch && partial.isNotBlank()) {
                                    drafts.partial(draft.id, partial)
                                    mutableText.value = listOf(draft.prefix, partial).filter { it.isNotBlank() }.joinToString(" ")
                                }
                            } },
                            { if (generation == epoch) mutableLevel.value = it },
                            { active -> if (generation == epoch) { captureRequested = active; mutableStarting.value = false; mutableProcessing.value = !active; mutableRecording.value = active; mutableStatus.value = if (active) "Listening…" else "Finishing on device…" } })
                    } ?: error("Device dictation reached the fifteen-minute limit. Resume the saved text to continue.")
                    synchronized(this@DictationController) { if (generation == epoch) drafts.partial(draft.id, result) }
                    finishTranscript(generation, draft.id, listOf(draft.prefix, result).filter { it.isNotBlank() }.joinToString(" "), preview)
                    return@launch
                }
                coroutineScope {
                    // Existing audio is replayed to the decoder after resume; it is never appended twice to the draft.
                    val chunks = Channel<FloatArray>(Channel.UNLIMITED)
                    if (draft.samples > 0) {
                        drafts.replay(draft.id) { ensureActive(); chunks.trySend(it).getOrThrow() }
                    }
                    val producer = if (capture && draft.samples < RecordingDraftStore.MAX_SAMPLES) launch(dispatcher) {
                        val record = openMicrophone(); microphone = record
                        try {
                            synchronized(this@DictationController) {
                                ensureActive()
                                if (generation != epoch || stopRequested) error(NO_SPEECH_MESSAGE)
                                record.start()
                                mutableStarting.value = false
                                mutableRecording.value = true; mutableStatus.value = "Listening · preparing speech model…"
                            }
                            val buffer = ShortArray(1600)
                            var total = draft.samples; var quiet = 0; var spoken = false
                            var lastSamplesAt = System.nanoTime()
                            while (!stopRequested && total < RecordingDraftStore.MAX_SAMPLES) {
                                ensureActive()
                                val count = record.read(buffer)
                                check(count >= 0) { "The microphone was interrupted. Your recording is paused." }
                                if (count == 0) {
                                    check(System.nanoTime() - lastSamplesAt < 3_000_000_000L) { "The microphone stopped delivering audio. Your recording is paused." }
                                    delay(10); continue
                                }
                                lastSamplesAt = System.nanoTime()
                                val accepted = minOf(count, RecordingDraftStore.MAX_SAMPLES - total)
                                val kept = synchronized(this@DictationController) {
                                    generation == epoch && drafts.append(draft.id, buffer, accepted)
                                }
                                if (!kept) break
                                total += accepted
                                val audio = FloatArray(accepted) { buffer[it] / 32768f }
                                val rms = kotlin.math.sqrt(audio.sumOf { (it * it).toDouble() } / accepted).toFloat()
                                if (generation == epoch) mutableLevel.value = microphoneLevel(rms)
                                if (rms > .003f) { spoken = true; quiet = 0 } else quiet += accepted
                                chunks.send(audio)
                                if ((endAfterSilence && spoken && quiet > 16000 * 1.6) || (!spoken && total - draft.samples >= 10 * 16000)) break
                            }
                            if (!spoken && draft.samples == 0) {
                                if (generation == epoch) mutableError.value = NO_SPEECH_MESSAGE
                                error(NO_SPEECH_MESSAGE)
                            }
                        } catch (e: CancellationException) { throw e }
                        catch (e: Exception) {
                            if (generation == epoch) mutableError.value = e.message ?: "The microphone could not record audio."
                            throw e
                        } finally {
                            chunks.close(); runCatching { record.stop() }
                            if (generation == epoch) { captureRequested = false; mutableStarting.value = false; mutableRecording.value = false; mutableProcessing.value = mutableError.value == null; mutableLevel.value = 0f; mutableStatus.value = "Transcribing on device…" }
                        }
                    } else { if (generation == epoch) captureRequested = false; chunks.close(); null }
                    // Capture starts independently of model loading. Every captured chunk is durable before decoding.
                    speech.ensureReady()
                    if (generation == epoch) mutableStatus.value = if (mutableRecording.value) "Listening…" else "Transcribing on device…"
                    val update: (String) -> Unit = { if (generation == epoch) mutableText.value = it }
                    if (!speech.stream(this@DictationController, { chunks.receiveCatching().getOrNull() }, update)) {
                        transcribeWhisperLive(chunks, { speech.transcribe(it, this@DictationController) }, update)
                    }
                    producer?.join()
                }
                finishTranscript(generation, draft.id, mutableText.value, preview)
            } catch (e: CancellationException) { throw e }
            catch (e: Throwable) {
                currentCoroutineContext().ensureActive()
                synchronized(this@DictationController) {
                    if (generation == epoch) {
                        mutableError.value = e.message ?: "Speech transcription failed."
                        mutablePaused.value = runCatching { drafts.load()?.hasContent == true }.getOrDefault(false)
                    }
                }
            } finally {
                preview?.cancel()
                runCatching { microphone?.stop() }; microphone?.close()
                finishSession(generation, sessionClaim)
            }
        }
        job = task
        task.invokeOnCompletion { finishSession(generation, sessionClaim) }
        task.start()
    }

    private suspend fun finishTranscript(generation: Int, id: String, raw: String, preview: LiveDictationCleanup? = null) {
        currentCoroutineContext().ensureActive()
        if (generation != epoch) return
        mutableStarting.value = false; mutableProcessing.value = true; mutableRecording.value = false; mutableLevel.value = 0f; mutableStatus.value = "Finishing text…"
        synchronized(this) { if (generation == epoch) drafts.raw(id, raw) }
        val transcript = requireDictationSpeech(raw)
        val result = requireDictationSpeech(preview?.finish(transcript) ?: polish(transcript))
        currentCoroutineContext().ensureActive()
        synchronized(this) {
            if (generation == epoch) { drafts.complete(id, result); publishCompleted(generation, result) }
        }
    }
    @Synchronized private fun publishCompleted(generation: Int, text: String) {
        if (generation == epoch) { mutableText.value = text; mutableCompletedText.value = text }
    }
    @Synchronized private fun finishSession(generation: Int, sessionClaim: Any) {
        owner.compareAndSet(sessionClaim, null)
        if (generation == epoch) { mutableStarting.value = false; mutableRecording.value = false; mutableLevel.value = 0f; mutableStatus.value = null; mutableProcessing.value = false }
    }
    fun stop() { stopRequested = true; deviceSession?.stop() }

    /** Keep finished audio processing off-screen, but never let pending/live capture open a hidden microphone. */
    @Synchronized fun continueProcessingWhenHidden(): Boolean {
        if (mutableCompletedText.value != null || (mutableProcessing.value && !mutableRecording.value && !captureRequested)) return true
        pause()
        return false
    }

    /** Freeze both capture and decoding; the next controller restores this draft without recording or inserting. */
    @Synchronized fun pause() {
        epoch++; stopRequested = true
        speech.cancel(this); job?.cancel(); deviceSession?.stop(); deviceSession = null
        mutableStarting.value = false; mutableRecording.value = false; mutableProcessing.value = false; mutableCompletedText.value = null; mutableLevel.value = 0f
        try { drafts.sync(); mutablePaused.value = drafts.load()?.hasContent == true }
        catch (e: Exception) { mutableError.value = "Cannot save the recording: ${e.message}" }
        mutableStatus.value = if (mutablePaused.value) "Recording paused" else null
    }
    /** Only explicit Cancel discards a draft. */
    @Synchronized fun cancel() {
        pause()
        try { if (unreadableDraft) drafts.clear(null) else draftId?.let { drafts.clear(it) }; unreadableDraft = false; draftId = null; mutablePaused.value = false; mutableText.value = ""; mutableError.value = null }
        catch (e: Exception) { mutableError.value = e.message }
    }
    @Synchronized fun acknowledgeInsertion() {
        draftId?.let { drafts.clear(it) }; draftId = null; mutableCompletedText.value = null; mutablePaused.value = false
    }
    fun insertionFailed() { pause(); mutableError.value = "Text could not be inserted. Your recording is saved; tap the paused pill to try again." }

    companion object {
        private val owner = java.util.concurrent.atomic.AtomicReference<Any?>(null)
        private fun draftStore(context: Context) = RecordingDraftStore(File(context.noBackupFilesDir, "dictation-draft"))
        fun hasSavedDraft(context: Context) = runCatching { draftStore(context).load()?.hasContent == true }.getOrDefault(false)
        fun isActive() = owner.get() != null
        internal suspend fun <T> speechSetup(action: suspend () -> T): T {
            val claim = Any()
            check(owner.compareAndSet(null, claim)) { "Finish recording before changing speech models." }
            try { return action() } finally { owner.compareAndSet(claim, null) }
        }
    }
}

/** A decibel meter makes quieter real input visible; zero PCM remains a flat wave. */
internal fun microphoneLevel(rms: Float): Float = ((20f * kotlin.math.log10(rms.coerceAtLeast(.000001f)) + 60f) / 60f).coerceIn(0f, 1f)

internal fun pcm16Samples(pcm: ByteArray): FloatArray {
    val shorts = java.nio.ByteBuffer.wrap(pcm).order(java.nio.ByteOrder.LITTLE_ENDIAN).asShortBuffer()
    return FloatArray(shorts.remaining()) { shorts.get() / 32768f }
}
internal const val NO_SPEECH_MESSAGE = "No speech was heard. Tap to try again."
internal fun requireDictationSpeech(text: String): String = cleanDictation(text).also {
    check(it.isNotBlank() && it != "[NO_SPEECH_DETECTED]") { NO_SPEECH_MESSAGE }
}
