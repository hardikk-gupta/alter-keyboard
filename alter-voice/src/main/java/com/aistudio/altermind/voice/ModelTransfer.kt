package com.aistudio.altermind.voice

import kotlinx.coroutines.currentCoroutineContext
import kotlinx.coroutines.ensureActive
import okhttp3.Call
import okhttp3.OkHttpClient
import okhttp3.Request
import java.io.File
import java.io.FileOutputStream
import java.io.IOException

/** Streaming transfer is separate from scheduling so Range and partial-file behavior can be tested. */
class ModelTransfer(private val client: OkHttpClient) {
    @Volatile private var call: Call? = null
    fun cancel() { call?.cancel() }
    suspend fun download(spec: ModelSpec, part: File, available: () -> Long, progress: suspend (Long) -> Unit) {
        part.parentFile!!.mkdirs()
        if (part.length() > spec.downloadBytes) check(part.delete())
        var offset = part.length()
        require(available() >= ModelStore.requiredSpace(spec, offset)) { "Not enough storage. Free space and retry." }
        if (offset == spec.downloadBytes) return
        val request = Request.Builder().url(spec.downloadUrl).header("Accept-Encoding", "identity")
            .apply { if (offset > 0) header("Range", "bytes=$offset-") }.build()
        val transfer = client.newCall(request)
        call = transfer
        try {
            transfer.execute().use { response ->
                if (response.code == 429 || response.code >= 500) throw IOException("Model server is temporarily unavailable")
                require(response.code == 200 || response.code == 206) { "Model source is unavailable (HTTP ${response.code}). Please retry later." }
                if (response.code == 206) {
                    require(validRange(response.header("Content-Range"), offset, spec.downloadBytes)) { "Model source returned an invalid resume range" }
                } else if (offset > 0) {
                    // The server ignored Range: overwrite, never append a whole file to a partial file.
                    require(available() >= ModelStore.requiredSpace(spec, 0) - offset) { "Not enough storage to restart this download" }
                    offset = 0
                }
                val body = requireNotNull(response.body) { "Empty model download" }
                val length = body.contentLength()
                require(length < 0 || length == spec.downloadBytes - offset) { "Model source returned an unexpected size" }
                var downloaded = offset
                var lastProgress = 0L
                FileOutputStream(part, offset > 0).use { output ->
                    body.byteStream().use { input ->
                        val buffer = ByteArray(256 * 1024)
                        while (true) {
                            currentCoroutineContext().ensureActive()
                            val count = input.read(buffer)
                            if (count < 0) break
                            require(downloaded + count <= spec.downloadBytes) { "Model download exceeds expected size" }
                            output.write(buffer, 0, count)
                            downloaded += count
                            val now = System.currentTimeMillis()
                            if (now - lastProgress >= 750) {
                                require(available() >= maxOf(ModelStore.STORAGE_BUFFER, spec.downloadBytes / 5)) { "Storage is running low. Free space and retry." }
                                progress(downloaded)
                                lastProgress = now
                            }
                        }
                    }
                    output.fd.sync()
                }
                require(downloaded == spec.downloadBytes) { "Model download was interrupted. Retry to resume." }
                progress(downloaded)
            }
        } finally { call = null }
    }
    companion object {
        fun validRange(value: String?, offset: Long, total: Long): Boolean {
            val match = Regex("bytes (\\d+)-(\\d+)/(\\d+)").matchEntire(value ?: "") ?: return false
            return match.groupValues[1].toLongOrNull() == offset && match.groupValues[2].toLongOrNull() == total - 1 &&
                match.groupValues[3].toLongOrNull() == total
        }
    }
}
