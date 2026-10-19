package com.aistudio.altermind.voice

import android.content.Context
import kotlinx.coroutines.*
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.sync.withLock
import okhttp3.OkHttpClient
import java.io.IOException
import java.util.concurrent.ConcurrentHashMap
import java.util.concurrent.TimeUnit

/** Progress of one install: every file of a speech choice, or the single cleanup model. */
sealed interface DownloadState {
    data class Running(val bytes: Long, val total: Long, val verifying: Boolean = false) : DownloadState
    data class Failed(val message: String) : DownloadState
}

/**
 * Downloads into the keyboard's private no-backup storage, on demand, from the pinned catalog. Each file is checked
 * for its exact size and SHA-256 before it counts as installed. A `.part` file resumes after the process restarts.
 */
object ModelDownloads {
    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.IO)
    private val jobs = ConcurrentHashMap<String, Job>()
    private val transfers = ConcurrentHashMap<String, ModelTransfer>()
    private val mutableStates = MutableStateFlow<Map<String, DownloadState>>(emptyMap())
    val states = mutableStates.asStateFlow()

    private val client by lazy {
        OkHttpClient.Builder().followSslRedirects(false)
            .connectTimeout(30, TimeUnit.SECONDS).readTimeout(30, TimeUnit.SECONDS).build()
    }

    fun isRunning(key: String) = jobs[key]?.isActive == true

    /** Installs [files] under [key]. Already-installed files are kept; nothing is replaced while it is in use. */
    fun start(context: Context, key: String, files: List<ModelSpec>) {
        if (isRunning(key)) return
        val app = context.applicationContext
        val store = ModelStore(app)
        val total = files.sumOf { it.downloadBytes }
        mutableStates.update { it + (key to DownloadState.Running(0, total)) }
        jobs[key] = scope.launch {
            try {
                var done = 0L
                for (spec in files) {
                    ensureActive()
                    ModelOperations.lock(spec.id).withLock {
                        if (!store.installed(spec)) {
                            // An unregistered or corrupt final file must never count as an installation.
                            store.file(spec).delete()
                            val transfer = ModelTransfer(client).also { transfers[key] = it }
                            var attempt = 0
                            while (true) {
                                try {
                                    transfer.download(spec, store.partial(spec), store::availableBytes) { bytes ->
                                        mutableStates.update { it + (key to DownloadState.Running(done + bytes, total)) }
                                    }
                                    break
                                } catch (e: IOException) {
                                    if (++attempt >= 3) throw IOException("Download interrupted. Check your connection and retry to resume.", e)
                                    delay(2000L * attempt)
                                }
                            }
                            mutableStates.update { it + (key to DownloadState.Running(done + spec.downloadBytes, total, verifying = true)) }
                            try { store.activate(spec) } catch (e: IllegalArgumentException) { store.partial(spec).delete(); throw e }
                        }
                    }
                    done += spec.downloadBytes
                }
                mutableStates.update { it - key }
            } catch (e: CancellationException) {
                mutableStates.update { it - key }
                throw e
            } catch (e: Exception) {
                mutableStates.update { it + (key to DownloadState.Failed(e.message ?: "Could not download the model")) }
            } finally {
                transfers.remove(key)
                jobs.remove(key)
            }
        }
    }

    fun cancel(key: String) {
        transfers[key]?.cancel()
        jobs[key]?.cancel()
    }

    /** Deletes [files] unless another installed choice still needs them, or a download is using them. */
    suspend fun delete(context: Context, files: List<ModelSpec>, keep: Set<String> = emptySet()) = withContext(Dispatchers.IO) {
        val store = ModelStore(context.applicationContext)
        for (spec in files) {
            if (spec.id in keep) continue
            ModelOperations.lock(spec.id).withLock {
                if (store.directory(spec).parentFile!!.exists()) store.delete(spec)
            }
        }
    }

    fun clearFailure(key: String) { mutableStates.update { it - key } }
}
