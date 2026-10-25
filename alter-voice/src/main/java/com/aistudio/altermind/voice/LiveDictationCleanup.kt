package com.aistudio.altermind.voice

import kotlinx.coroutines.*
import kotlinx.coroutines.flow.*

/** Provisional whole-transcript edits: later ASR revisions can retract any earlier wording. */
@OptIn(FlowPreview::class)
internal class LiveDictationCleanup(scope: CoroutineScope, raw: StateFlow<String>, recording: StateFlow<Boolean>,
    private val polish: suspend (String) -> String) {
    @Volatile private var cached: Pair<String, String>? = null
    @Volatile private var pending: Pair<String, CompletableDeferred<String>>? = null
    private val job = scope.launch {
        raw.debounce(1200).collectLatest { transcript ->
            if (!recording.value || transcript.isBlank() || transcript.length > 4000) return@collectLatest
            val source = cleanDictation(transcript)
            if (source.isBlank()) return@collectLatest
            val result = CompletableDeferred<String>()
            pending = source to result
            try {
                val edited = polish(source)
                ensureActive()
                cached = source to edited
                result.complete(edited)
            } catch (e: CancellationException) { result.cancel(e); throw e }
            catch (e: Throwable) { result.completeExceptionally(e) } // Never interrupt capture for a preview failure.
            finally { if (pending?.second === result) pending = null }
        }
    }
    fun cancel() { job.cancel() }
    suspend fun finish(raw: String): String {
        // Finish must not cancel/restart an identical edit that is already in progress.
        pending?.takeIf { it.first == raw }?.let { inFlight ->
            try {
                val edited = inFlight.second.await()
                job.cancelAndJoin()
                return edited
            } catch (e: CancellationException) { currentCoroutineContext().ensureActive() }
            catch (_: Throwable) { /* Final cleanup below retains the usual bounded fallback. */ }
        }
        job.cancelAndJoin()
        return cached?.takeIf { it.first == raw }?.second ?: polish(raw)
    }
}
