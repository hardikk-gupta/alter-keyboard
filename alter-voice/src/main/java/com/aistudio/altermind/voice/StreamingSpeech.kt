package com.aistudio.altermind.voice

import com.k2fsa.sherpa.onnx.OnlineModelConfig
import com.k2fsa.sherpa.onnx.OnlineRecognizer
import com.k2fsa.sherpa.onnx.OnlineRecognizerConfig
import com.k2fsa.sherpa.onnx.OnlineTransducerModelConfig
import kotlinx.coroutines.currentCoroutineContext
import kotlinx.coroutines.ensureActive
import org.json.JSONObject
import java.io.File

/** One selected decoder. The controller owns capture, draft persistence and final insertion. */
internal interface StreamingSpeech {
    suspend fun recognize(read: suspend () -> FloatArray?, update: (String) -> Unit)
    fun close()
}

internal class SherpaParakeetEngine(files: Map<String, File>) : StreamingSpeech {
    private val recognizer = OnlineRecognizer(config = OnlineRecognizerConfig(
        modelConfig = OnlineModelConfig(
            transducer = OnlineTransducerModelConfig(
                encoder = files.getValue("encoder.int8.onnx").absolutePath,
                decoder = files.getValue("decoder.int8.onnx").absolutePath,
                joiner = files.getValue("joiner.int8.onnx").absolutePath
            ),
            tokens = files.getValue("tokens.txt").absolutePath,
            numThreads = 2
        )
    ))

    override suspend fun recognize(read: suspend () -> FloatArray?, update: (String) -> Unit) {
        val stream = recognizer.createStream()
        val committed = StringBuilder()
        try {
            while (true) {
                currentCoroutineContext().ensureActive()
                val audio = read()
                if (audio == null) stream.inputFinished() else stream.acceptWaveform(audio, 16000)
                while (recognizer.isReady(stream)) {
                    currentCoroutineContext().ensureActive()
                    recognizer.decode(stream)
                    val text = recognizer.getResult(stream).text.trim()
                    if (recognizer.isEndpoint(stream)) {
                        committed.appendWords(text)
                        recognizer.reset(stream)
                        update(committed.toString())
                    } else update(joinWords(committed, text))
                }
                if (audio == null) {
                    update(joinWords(committed, recognizer.getResult(stream).text.trim()))
                    break
                }
            }
        } finally { stream.release() }
    }

    override fun close() = recognizer.release()
}

internal object NeedleNative {
    init { System.loadLibrary("alter_needle_jni") }
    external fun open(weights: ByteArray): Long
    external fun step(handle: Long, audio: FloatArray?, hindi: Boolean): ByteArray
    external fun close(handle: Long)
}

internal class NeedleEngine(file: File, private val hindi: Boolean) : StreamingSpeech {
    private val handle = NeedleNative.open(file.readBytes())

    override suspend fun recognize(read: suspend () -> FloatArray?, update: (String) -> Unit) {
        val committed = StringBuilder()
        val second = FloatArray(16000)
        var samples = 0
        fun step(audio: FloatArray?) {
            val result = JSONObject(NeedleNative.step(handle, audio, hindi).decodeToString())
            committed.appendWords(result.getString("text").trim())
            update(joinWords(committed, result.optString("pending").trim()))
        }
        var stopping = false
        try {
            while (true) {
                currentCoroutineContext().ensureActive()
                val chunk = read() ?: break
                var offset = 0
                while (offset < chunk.size) {
                    val count = minOf(second.size - samples, chunk.size - offset)
                    chunk.copyInto(second, samples, offset, offset + count)
                    samples += count
                    offset += count
                    if (samples == second.size) {
                        currentCoroutineContext().ensureActive()
                        step(second)
                        samples = 0
                    }
                }
            }
            if (samples > 0) step(second.copyOf(samples))
            stopping = true
            step(null)
        } finally {
            // Stop clears the native agreement buffer, also after a cancelled or failed session.
            if (!stopping) runCatching { NeedleNative.step(handle, null, hindi) }
        }
    }
    override fun close() = NeedleNative.close(handle)
}

private fun StringBuilder.appendWords(words: String) {
    if (words.isEmpty()) return
    if (isNotEmpty()) append(' ')
    append(words)
}

private fun joinWords(prefix: StringBuilder, tail: String): String =
    if (tail.isEmpty()) prefix.toString() else if (prefix.isEmpty()) tail else "$prefix $tail"
