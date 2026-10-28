package com.aistudio.altermind.voice

import ai.moonshine.voice.JNI
import java.io.Closeable
import kotlinx.coroutines.currentCoroutineContext
import kotlinx.coroutines.ensureActive

/** Check the C API directly: the SDK convenience methods discard feed/start/stop errors. */
internal class MoonshineSpeech private constructor(private val handle: Int) : Closeable {
    fun setKeyterms(terms: List<String>) = checkStatus("set vocabulary", JNI.moonshineTranscriberSetKeyterms(handle, terms.joinToString(",")))
    fun decode(audio: FloatArray): String = requireNotNull(JNI.moonshineTranscribeWithoutStreaming(handle, audio, 16000, 0)) {
        "Moonshine could not transcribe the recording. Your audio is saved."
    }.text()

    suspend fun stream(read: suspend () -> FloatArray?, update: (String) -> Unit) {
        val stream = JNI.moonshineCreateStream(handle, 0)
        check(stream >= 0) { failure("create a recording stream", stream) }
        consumeMoonshineStream(read, update,
            start = { checkStatus("start recording", JNI.moonshineStartStream(handle, stream)) },
            feed = { checkStatus("accept audio", JNI.moonshineAddAudioToStream(handle, stream, it, 16000, 0)) },
            poll = { force -> requireNotNull(JNI.moonshineTranscribeStream(handle, stream,
                if (force) JNI.MOONSHINE_FLAG_FORCE_UPDATE else 0)) {
                "Moonshine could not ${if (force) "finish" else "decode"} the recording. Your audio is saved."
            }.text() },
            stop = { checkStatus("stop recording", JNI.moonshineStopStream(handle, stream)) },
            free = { JNI.moonshineFreeStream(handle, stream) })
    }
    override fun close() = JNI.moonshineFreeTranscriber(handle)
    companion object {
        fun open(directory: String): MoonshineSpeech {
            JNI.ensureLibraryLoaded()
            val handle = JNI.moonshineLoadTranscriberFromFiles(directory, JNI.MOONSHINE_MODEL_ARCH_MEDIUM_STREAMING, emptyArray<Any>())
            check(handle >= 0) { failure("load Medium", handle) }
            return MoonshineSpeech(handle)
        }
        private fun checkStatus(operation: String, code: Int) {
            check(code == JNI.MOONSHINE_ERROR_NONE) { failure(operation, code) }
        }
        private fun failure(operation: String, code: Int) =
            "Moonshine could not $operation: ${JNI.moonshineErrorToString(code)} ($code). Your recording is saved."
    }
}

/** One fresh stream, checked feeds, adaptive previews, and exactly one final stop/flush. */
internal suspend fun consumeMoonshineStream(read: suspend () -> FloatArray?, update: (String) -> Unit,
    start: () -> Unit, feed: (FloatArray) -> Unit, poll: (Boolean) -> String, stop: () -> Unit, free: () -> Unit) {
    var started = false
    var stopped = false
    var pending = 0
    var interval = 8000 // 0.5s floor, stretched by measured decoder cost up to 5s.
    try {
        currentCoroutineContext().ensureActive()
        start(); started = true
        while (true) {
            currentCoroutineContext().ensureActive()
            val audio = read() ?: break
            feed(audio); pending += audio.size
            if (pending >= interval) {
                val before = System.nanoTime()
                val text = poll(false)
                currentCoroutineContext().ensureActive()
                update(cleanDictation(text))
                interval = ((System.nanoTime() - before) / 1_000_000_000.0 * 16000).toInt().coerceIn(8000, 80000)
                pending = 0
            }
        }
        currentCoroutineContext().ensureActive()
        stopped = true // A failed stop must not be called a second time in cleanup.
        stop()
        val final = poll(true)
        currentCoroutineContext().ensureActive()
        update(cleanDictation(final))
    } finally {
        if (started && !stopped) runCatching { stop() }
        free()
    }
}
