package com.aistudio.altermind.voice

import android.content.Context
import android.content.pm.PackageManager
import androidx.core.content.ContextCompat
import kotlinx.coroutines.*
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import java.io.ByteArrayOutputStream

/**
 * Shared mic owner: downloaded ASR uses PCM; explicit Android on-device recognition owns its mic.
 * In the keyboard there are no recovery cards: a failed recording stays in memory only, for one retry.
 */
class DictationController private constructor(private val context: Context, private val scope: CoroutineScope,
    private val speech: DictationSpeech, private val openMicrophone: () -> DictationMicrophone,
    private val polish: suspend (String) -> String) {
    constructor(context: Context, scope: CoroutineScope) : this(context, scope, SpeechModels.get(context), { AndroidDictationMicrophone(context) }, { VoicePack.polish(context, it) })
    internal constructor(context: Context, scope: CoroutineScope, speech: DictationSpeech, microphone: DictationMicrophone, polish: suspend (String) -> String = { it }) : this(context, scope, speech, { microphone }, polish)
    private val mutableRecording = MutableStateFlow(false)
    val recording = mutableRecording.asStateFlow()
    private val mutableProcessing = MutableStateFlow(false)
    val processing = mutableProcessing.asStateFlow()
    private val mutableText = MutableStateFlow("")
    val text = mutableText.asStateFlow()
    private val mutableError = MutableStateFlow<String?>(null)
    val error = mutableError.asStateFlow()
    private val mutableLevel = MutableStateFlow(0f)
    val level = mutableLevel.asStateFlow()
    private val mutableBackend = MutableStateFlow("")
    val backend = mutableBackend.asStateFlow()
    private val mutableStatus = MutableStateFlow<String?>(null)
    val status = mutableStatus.asStateFlow()
    private var job: Job? = null
    private var claim: Any? = null
    private var deviceSession: DeviceSpeechSession? = null
    @Volatile private var stopRequested = false
    @Volatile private var epoch = 0
    @Volatile private var retryRecording: FloatArray? = null
    val hasRetryRecording: Boolean get() = retryRecording != null
    fun retrySavedAudio() = start(retrySaved = true)
    @Synchronized fun start(endAfterSilence: Boolean = true, retrySaved: Boolean = false) {
        val saved = if (retrySaved) retryRecording else null
        if (retrySaved && saved == null) { mutableError.value = "No saved recording is available. Record again."; return }
        if (job?.isActive == true && mutableProcessing.value) return
        mutableError.value = null; mutableStatus.value = null; mutableText.value = ""
        if (!scope.isActive) return
        if (saved == null && ContextCompat.checkSelfPermission(context, android.Manifest.permission.RECORD_AUDIO) != PackageManager.PERMISSION_GRANTED) {
            mutableError.value = "Allow microphone access for ALTER Keyboard, then try again."; return
        }
        try { if (saved == null) speech.checkInstalled() } catch (e: Exception) { mutableError.value = e.message; return }
        val sessionClaim = Any()
        if (!owner.compareAndSet(null, sessionClaim)) { mutableError.value = "ALTER is preparing voice or already using the microphone. Please wait."; return }
        claim = sessionClaim
        val generation = ++epoch
        stopRequested = false; mutableText.value = ""; mutableProcessing.value = true
        if (saved == null) retryRecording = null
        val device = (saved == null && speech.selected == DEVICE_SPEECH).also { deviceSession = if (it) DeviceSpeechSession(context) else null }
        val nativeSession = deviceSession
        mutableBackend.value = if (device) "Device recognition · on-device" else if (speech.selected.orEmpty().startsWith("moonshine-")) "Moonshine · English" else "Whisper · on-device"
        mutableStatus.value = "Starting microphone…"
        val task = scope.launch(Dispatchers.IO, start = CoroutineStart.LAZY) {
            val original = ByteArrayOutputStream()
            var microphone: DictationMicrophone? = null

            try {
                ensureActive()
                if (nativeSession != null) {
                    val result = withTimeoutOrNull(900_000) {
                        nativeSession.listen(voicePrefs(context).getString("voice_language", "auto") ?: "auto",
                            { if (generation == epoch) mutableText.value = it },
                            { if (generation == epoch) mutableLevel.value = it },
                            { active -> if (generation == epoch) { mutableRecording.value = active; mutableStatus.value = if (active) "Listening…" else "Finishing on device…" } })
                    } ?: error("Device dictation reached the fifteen-minute limit. Start a new recording.")
                    finishTranscript(generation, result)
                    return@launch
                }
                if (saved != null) {
                    mutableStatus.value = "Retrying the last recording…"
                    speech.ensureReady()
                    finishTranscript(generation, speech.transcribe(saved, this@DictationController))
                    retryRecording = null
                    return@launch
                }
                val record = openMicrophone()
                microphone = record
                record.start()
                mutableRecording.value = true
                mutableStatus.value = "Listening · preparing speech model…"
                coroutineScope {
                    // At most fifteen minutes (~58 MB queued float PCM). A slow decoder never pauses capture.
                    val chunks = Channel<FloatArray>(Channel.UNLIMITED)
                    val ready = java.util.concurrent.atomic.AtomicBoolean(false)
                    val producer = launch(Dispatchers.IO) {
                        try {
                            val buffer = ShortArray(1600)
                            var total = 0; var quiet = 0; var spoken = false
                            var lastSamplesAt = System.nanoTime()
                            while (!stopRequested && total < 900 * 16000) {
                                ensureActive()
                                val count = record.read(buffer)
                                check(count >= 0) { "The microphone stopped recording (audio error $count). Try again after closing other recorders." }
                                if (count == 0) {
                                    check(System.nanoTime() - lastSamplesAt < 3_000_000_000L) { "No audio is reaching ALTER Keyboard. Check Android's microphone access and close other recorders." }
                                    delay(10); continue
                                }
                                lastSamplesAt = System.nanoTime()
                                total += count
                                val audio = FloatArray(count) { buffer[it] / 32768f }
                                for (i in 0 until count) { original.write(buffer[i].toInt() and 255); original.write((buffer[i].toInt() shr 8) and 255) }
                                val rms = kotlin.math.sqrt(audio.sumOf { (it * it).toDouble() } / count).toFloat()
                                mutableLevel.value = microphoneLevel(rms)
                                if (rms > .003f) { spoken = true; quiet = 0 } else quiet += count
                                chunks.send(audio)
                                if ((endAfterSilence && spoken && quiet > 16000 * 1.6) || (!spoken && total >= 10 * 16000)) break
                            }
                        } finally {
                            chunks.close(); runCatching { record.stop() }
                            if (generation == epoch) {
                                mutableRecording.value = false; mutableLevel.value = 0f
                                mutableStatus.value = if (ready.get()) "Transcribing on device…" else "Loading speech model…"
                            }
                        }
                    }
                    // Capture/RMS start independently of model initialization; early speech stays in the queue.
                    speech.ensureReady()
                    ready.set(true)
                    mutableStatus.value = if (mutableRecording.value) "Listening…" else "Transcribing on device…"
                    val streamed = speech.stream(this@DictationController, { chunks.receiveCatching().getOrNull() }) { text -> if (generation == epoch) mutableText.value = text }
                    if (!streamed) {
                        transcribeWhisperLive(chunks, { speech.transcribe(it, this@DictationController) },
                            { text -> if (generation == epoch) mutableText.value = text })
                    }
                    producer.join()

                }
                finishTranscript(generation, mutableText.value)
            } catch (e: CancellationException) { throw e }
            catch (e: Throwable) {
                currentCoroutineContext().ensureActive()
                if (generation != epoch) throw CancellationException("Dictation cancelled")
                mutableError.value = e.message ?: "Speech transcription failed."
                if (mutableError.value == NO_SPEECH_MESSAGE) mutableText.value = ""
                // The last recording survives a decoder failure in memory only; a retry reuses it without the mic.
                if (original.size() > 0 && mutableError.value != NO_SPEECH_MESSAGE && generation == epoch) retryRecording = pcm16Samples(original.toByteArray())
            } finally {
                runCatching { microphone?.stop() }; microphone?.close()
                finishSession(generation, sessionClaim)
            }
        }
        job = task
        // Also runs if cancellation happens before the dispatched body enters its try/finally.
        task.invokeOnCompletion {
            finishSession(generation, sessionClaim)
        }
        task.start()
    }
    private suspend fun finishTranscript(generation: Int, raw: String) {
        currentCoroutineContext().ensureActive()
        if (generation != epoch) return
        mutableRecording.value = false; mutableLevel.value = 0f
        mutableStatus.value = "Finishing text…"
        val result = requireDictationSpeech(polish(requireDictationSpeech(raw)))
        currentCoroutineContext().ensureActive()
        if (generation == epoch) mutableText.value = result
    }
    @Synchronized private fun finishSession(generation: Int, sessionClaim: Any) {
        owner.compareAndSet(sessionClaim, null)
        if (generation == epoch) { mutableRecording.value = false; mutableLevel.value = 0f; mutableStatus.value = null; mutableProcessing.value = false }
    }
    fun stop() { stopRequested = true; deviceSession?.stop() }
    @Synchronized fun cancel() {
        epoch++; stopRequested = true
        speech.cancel(this)
        val ending = job
        ending?.cancel(); job = null
        mutableRecording.value = false; mutableProcessing.value = false; mutableText.value = ""; mutableLevel.value = 0f; mutableStatus.value = null
        if (ending == null || ending.isCompleted) claim?.let { owner.compareAndSet(it, null) }
    }
    companion object {
        private val owner = java.util.concurrent.atomic.AtomicReference<Any?>(null)
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

/** A non-empty ASR sentinel is still silence, never editable/user text. */
internal fun requireDictationSpeech(text: String): String = cleanDictation(text).also {
    check(it.isNotBlank() && it != "[NO_SPEECH_DETECTED]") { NO_SPEECH_MESSAGE }
}
