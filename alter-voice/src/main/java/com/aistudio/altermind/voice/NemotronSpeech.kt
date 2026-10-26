package com.aistudio.altermind.voice

import kotlinx.coroutines.currentCoroutineContext
import kotlinx.coroutines.ensureActive

internal object NemotronNative {
    init { System.loadLibrary("alter_nemotron") }
    external fun load(path: String): Long
    external fun prepare(handle: Long)
    external fun begin(handle: Long, language: String, threads: Int): Boolean
    external fun feed(handle: Long, samples: FloatArray): ByteArray?
    external fun finish(handle: Long): ByteArray?
    external fun cancel(handle: Long)
    external fun end(handle: Long)
    external fun close(handle: Long)
}

internal fun nemotronLanguage(language: String?): String = when (language) {
    "en-IN" -> "en-US"
    "hi-IN" -> "hi-IN"
    else -> "auto"
}

/** Feed every PCM sample exactly once, publish revisions, and require a successful final flush. */
internal suspend fun consumeNemotronStream(read: suspend () -> FloatArray?, update: (String) -> Unit,
    feed: (FloatArray) -> ByteArray?, finish: () -> ByteArray?, end: () -> Unit) {
    try {
        while (true) {
            currentCoroutineContext().ensureActive()
            val audio = read() ?: break
            var offset = 0
            while (offset < audio.size) {
                currentCoroutineContext().ensureActive()
                val next = minOf(offset + 1600, audio.size)
                val text = requireNotNull(feed(audio.copyOfRange(offset, next))) { "Nemotron streaming failed. Your recording is saved." }
                currentCoroutineContext().ensureActive()
                update(cleanDictation(text.decodeToString()))
                offset = next
            }
        }
        val final = requireNotNull(finish()) { "Nemotron could not finish. Your recording is saved." }
        currentCoroutineContext().ensureActive()
        update(cleanDictation(final.decodeToString()))
    } finally { end() }
}
