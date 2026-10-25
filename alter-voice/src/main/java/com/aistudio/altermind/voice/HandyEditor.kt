package com.aistudio.altermind.voice

import android.content.Context
import kotlinx.coroutines.*
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock

internal object HandyNative {
    init { System.loadLibrary("alter_handy") }
    external fun load(path: String): Long
    external fun prepare(handle: Long)
    external fun edit(handle: Long, transcript: ByteArray, maxTokens: Int): ByteArray?
    external fun cancel(handle: Long)
    external fun close(handle: Long)
}

/** One CPU-only Handy edit at a time; aborted/truncated edits never become text. */
internal object HandyEditor {
    private val mutex = Mutex()
    private var handle = 0L
    suspend fun edit(context: Context, spec: ModelSpec, transcript: String): String = mutex.withLock {
        val store = ModelStore(context)
        check(store.installed(spec)) { "Download Handy in ALTER voice settings first." }
        if (handle == 0L) {
            store.verify(spec)
            handle = withContext(Dispatchers.IO) { HandyNative.load(store.file(spec).absolutePath) }
            check(handle != 0L) { "Handy could not load on this device." }
        }
        currentCoroutineContext().ensureActive()
        HandyNative.prepare(handle)
        coroutineScope {
            val inference = async(Dispatchers.IO) {
                HandyNative.edit(handle, transcript.encodeToByteArray(), 512)?.decodeToString()
                    ?: error("Handy was cancelled, exceeded its budget, or could not finish.")
            }
            try {
                val result = withTimeoutOrNull(7500) { inference.await() }
                    ?: error("Handy exceeded its time budget.")
                result.also { check(it.isNotBlank()) { "Handy returned no text." } }
            }
            finally {
                HandyNative.cancel(handle)
                withContext(NonCancellable) { inference.cancelAndJoin() }
            }
        }
    }
    suspend fun release() = mutex.withLock {
        if (handle != 0L) { HandyNative.close(handle); handle = 0L }
    }
}
