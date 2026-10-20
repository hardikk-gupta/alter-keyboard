package com.aistudio.altermind.voice

import android.content.Context
import android.content.Intent
import android.os.Build
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.speech.RecognitionListener
import android.speech.RecognizerIntent
import android.speech.SpeechRecognizer
import kotlinx.coroutines.*
import kotlin.coroutines.resume
import kotlin.coroutines.resumeWithException

internal const val DEVICE_SPEECH = "android-on-device"

internal fun deviceSpeechAvailable(context: Context): Boolean = Build.VERSION.SDK_INT >= 31 &&
    runCatching { SpeechRecognizer.isOnDeviceRecognitionAvailable(context) }.getOrDefault(false)

internal fun deviceSpeechIntent(language: String): Intent = Intent(RecognizerIntent.ACTION_RECOGNIZE_SPEECH).apply {
    putExtra(RecognizerIntent.EXTRA_LANGUAGE_MODEL, RecognizerIntent.LANGUAGE_MODEL_FREE_FORM)
    putExtra(RecognizerIntent.EXTRA_PARTIAL_RESULTS, true)
    putExtra(RecognizerIntent.EXTRA_MAX_RESULTS, 1)
    putExtra(RecognizerIntent.EXTRA_PREFER_OFFLINE, true)
    if (language != "auto") putExtra(RecognizerIntent.EXTRA_LANGUAGE, language)
    else if (Build.VERSION.SDK_INT >= 34) {
        putExtra(RecognizerIntent.EXTRA_ENABLE_LANGUAGE_DETECTION, true)
        putStringArrayListExtra(RecognizerIntent.EXTRA_LANGUAGE_DETECTION_ALLOWED_LANGUAGES, arrayListOf("en-IN", "hi-IN"))
    }
}

internal fun deviceSpeechError(code: Int): String = when (code) {
    SpeechRecognizer.ERROR_INSUFFICIENT_PERMISSIONS -> "Allow microphone access in Android settings."
    SpeechRecognizer.ERROR_LANGUAGE_NOT_SUPPORTED, SpeechRecognizer.ERROR_LANGUAGE_UNAVAILABLE ->
        "This device recognizer does not have the requested language. Install its offline language in system speech settings, or choose Whisper."
    SpeechRecognizer.ERROR_NO_MATCH, SpeechRecognizer.ERROR_SPEECH_TIMEOUT -> NO_SPEECH_MESSAGE
    SpeechRecognizer.ERROR_RECOGNIZER_BUSY -> "The device recognizer is busy. Close other voice apps and try again."
    else -> "Device recognition failed (Android error $code). No cloud fallback was used. Try again or select Whisper/Moonshine."
}

/** Android owns the microphone here. Never run AudioRecord beside it or use the ordinary/cloud factory. */
internal class DeviceSpeechSession(private val context: Context) {
    private val main = Handler(Looper.getMainLooper())
    private var recognizer: SpeechRecognizer? = null
    private var finishDeadline: (() -> Unit)? = null
    @Volatile private var stopped = false
    @Synchronized fun stop() {
        if (stopped) return
        stopped = true; main.post { recognizer?.stopListening(); finishDeadline?.invoke() }
    }
    private fun release() {
        val engine = recognizer; recognizer = null
        if (engine != null) { runCatching { engine.cancel() }; runCatching { engine.destroy() } }
    }

    suspend fun listen(language: String, partial: (String) -> Unit, level: (Float) -> Unit, listening: (Boolean) -> Unit): String =
        withContext(Dispatchers.Main.immediate) {
            check(deviceSpeechAvailable(context)) { "On-device recognition requires Android 12+ and an installed offline recognition service. Choose Whisper/Moonshine on this phone." }
            if (stopped) return@withContext ""
            coroutineScope {
                var deadline: Job? = null
                try {
                    suspendCancellableCoroutine { result ->
                        val engine = if (Build.VERSION.SDK_INT >= 31) SpeechRecognizer.createOnDeviceSpeechRecognizer(context)
                            else error("On-device recognition requires Android 12 or later.")
                        recognizer = engine
                        fun fail(message: String) { if (result.isActive) result.resumeWithException(IllegalStateException(message)) }
                        fun waitForFinal() {
                            deadline?.cancel()
                            deadline = launch { delay(10_000); fail("The device recognizer did not return a final transcript. Try again or choose Whisper/Moonshine.") }
                        }
                        finishDeadline = { listening(false); level(0f); waitForFinal() }
                        result.invokeOnCancellation { main.post { if (recognizer === engine) release() } }
                        engine.setRecognitionListener(object : RecognitionListener {
                            override fun onReadyForSpeech(params: Bundle?) {
                                if (result.isActive) { if (stopped) waitForFinal() else deadline?.cancel(); listening(!stopped) }
                            }
                            override fun onBeginningOfSpeech() { }
                            override fun onRmsChanged(rmsdB: Float) { if (result.isActive && !stopped) level(if (rmsdB.isFinite()) (rmsdB / 10f).coerceIn(0f, 1f) else 0f) }
                            override fun onBufferReceived(buffer: ByteArray?) { }
                            override fun onEndOfSpeech() { if (result.isActive) { listening(false); level(0f); waitForFinal() } }
                            override fun onError(error: Int) { fail(deviceSpeechError(error)) }
                            override fun onPartialResults(results: Bundle?) {
                                if (result.isActive) {
                                    try { partial(cleanDictation(results?.getStringArrayList(SpeechRecognizer.RESULTS_RECOGNITION)?.firstOrNull().orEmpty())) }
                                    catch (e: Exception) { fail("Could not save recognized text: ${e.message}") }
                                }
                            }
                            override fun onResults(results: Bundle?) {
                                val text = cleanDictation(results?.getStringArrayList(SpeechRecognizer.RESULTS_RECOGNITION)?.firstOrNull().orEmpty())
                                if (text.isBlank()) fail(deviceSpeechError(SpeechRecognizer.ERROR_NO_MATCH))
                                else if (result.isActive) result.resume(text)
                            }
                            override fun onEvent(eventType: Int, params: Bundle?) { }
                        })
                        deadline = launch { delay(10_000); fail("The device microphone did not become ready. Check system microphone access and try again.") }
                        engine.startListening(deviceSpeechIntent(language))
                        if (stopped) { engine.stopListening(); waitForFinal() }
                    }
                } finally {
                    deadline?.cancel(); finishDeadline = null
                    release()
                    listening(false); level(0f)
                }
            }
        }
}
