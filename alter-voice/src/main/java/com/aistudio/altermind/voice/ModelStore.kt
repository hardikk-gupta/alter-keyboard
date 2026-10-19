package com.aistudio.altermind.voice

import android.content.Context
import android.os.StatFs
import android.util.AtomicFile
import org.json.JSONObject
import java.io.File
import java.security.MessageDigest
import kotlinx.coroutines.ensureActive

/** Registry and multi-GB files live outside Android backup and public storage. */
class ModelStore(private val root: File) {
    constructor(context: Context) : this(File(context.noBackupFilesDir, "models"))
    fun directory(spec: ModelSpec): File {
        require(listOf(spec.id, spec.version, spec.filename).all(ModelCatalog::safeSegment))
        val directory = File(root, "${spec.modelType.name.lowercase()}/${spec.id}/${spec.version}")
        require(directory.canonicalPath.startsWith(root.canonicalPath + File.separator))
        return directory
    }
    fun file(spec: ModelSpec) = File(directory(spec), spec.filename)
    fun partial(spec: ModelSpec) = File(directory(spec), spec.filename + ".part")
    private fun metadata(spec: ModelSpec) = File(directory(spec), "metadata.json")
    fun availableBytes(): Long { root.mkdirs(); return StatFs(root.absolutePath).availableBytes }
    fun installed(spec: ModelSpec): Boolean {
        val file = file(spec)
        return try {
            val record = JSONObject(metadata(spec).readText())
            file.isFile && file.length() == spec.downloadBytes && record.getString("sha256") == spec.sha256 &&
                record.getString("version") == spec.version && record.getString("modelId") == spec.id
        } catch (_: Exception) { false }
    }
    suspend fun verify(spec: ModelSpec, candidate: File = file(spec)) {
        require(candidate.isFile && candidate.length() == spec.downloadBytes) { "Model size does not match. Retry the download." }
        require(sha256(candidate) == spec.sha256) { "Model verification failed. Delete and download again." }
    }
    suspend fun activate(spec: ModelSpec) {
        verify(spec, partial(spec))
        kotlinx.coroutines.currentCoroutineContext().ensureActive()
        require(partial(spec).renameTo(file(spec))) { "Could not install the verified model" }
        val atomic = AtomicFile(metadata(spec))
        val output = atomic.startWrite()
        try {
            output.write(JSONObject().put("modelId", spec.id).put("version", spec.version).put("sha256", spec.sha256)
                .put("bytes", spec.downloadBytes).put("installedAt", System.currentTimeMillis())
                .put("path", file(spec).absolutePath).put("verified", true).put("capabilities", org.json.JSONArray(spec.capabilities.toList()))
                .toString().toByteArray())
            atomic.finishWrite(output)
        } catch (e: Exception) { atomic.failWrite(output); throw e }
    }
    fun invalidate(spec: ModelSpec) { metadata(spec).delete() }
    fun delete(spec: ModelSpec) { check(directory(spec).parentFile!!.deleteRecursively()) { "Could not delete model" } }
    companion object {
        const val STORAGE_BUFFER = 1_073_741_824L
        fun requiredSpace(spec: ModelSpec, partialBytes: Long): Long =
            (spec.downloadBytes - partialBytes.coerceIn(0, spec.downloadBytes)) + maxOf(STORAGE_BUFFER, spec.downloadBytes / 5)
        suspend fun sha256(file: File): String {
            val digest = MessageDigest.getInstance("SHA-256")
            file.inputStream().buffered().use { input ->
                val buffer = ByteArray(256 * 1024)
                while (true) {
                    kotlinx.coroutines.currentCoroutineContext().ensureActive()
                    val count = input.read(buffer)
                    if (count < 0) break
                    digest.update(buffer, 0, count)
                }
            }
            return digest.digest().joinToString("") { "%02x".format(it) }
        }
    }
}
