package com.aistudio.altermind.voice

import android.content.Context
import org.json.JSONObject
import java.net.URI

enum class ModelType { GENERATION, EMBEDDING, SPEECH, TEXT_CLEANUP }
data class ModelSpec(
    val id: String, val displayName: String, val tier: String, val description: String,
    val family: String, val version: String, val repository: String, val filename: String,
    val downloadUrl: String, val downloadBytes: Long, val sha256: String, val license: String,
    val modelType: ModelType, val capabilities: Set<String>, val minimumRamBytes: Long,
    val recommendedRamBytes: Long, val backend: String, val cpuFallback: Boolean, val contextTokens: Int
)

class ModelCatalog(val models: List<ModelSpec>) {
    fun find(id: String): ModelSpec = models.singleOrNull { it.id == id } ?: error("Unknown local model")
    companion object {
        private val segment = Regex("[a-zA-Z0-9][a-zA-Z0-9._-]{0,127}")
        fun safeSegment(value: String): Boolean = segment.matches(value) && value != "." && value != ".."
        fun parse(json: String): ModelCatalog {
            val root = JSONObject(json)
            require(root.getInt("schemaVersion") == 1) { "Unsupported model catalog" }
            val array = root.getJSONArray("models")
            val models = (0 until array.length()).map { index ->
                val item = array.getJSONObject(index)
                fun strings(key: String) = item.getJSONArray(key).let { a -> (0 until a.length()).map { a.getString(it) }.toSet() }
                ModelSpec(item.getString("id"), item.getString("displayName"), item.getString("tier"), item.getString("description"),
                    item.getString("family"), item.getString("version"), item.getString("repository"), item.getString("filename"),
                    item.getString("downloadUrl"), item.getLong("downloadBytes"), item.getString("sha256"), item.getString("license"),
                    ModelType.valueOf(item.getString("modelType")), strings("capabilities"), item.getLong("minimumRamBytes"),
                    item.getLong("recommendedRamBytes"), item.getString("backend"), item.getBoolean("cpuFallback"), item.getInt("contextTokens"))
                    .also { spec ->
                        require(listOf(spec.id, spec.version, spec.filename).all(::safeSegment)) { "Unsafe model path" }
                        val uri = URI(spec.downloadUrl)
                        require(uri.scheme == "https" && !uri.host.isNullOrBlank() && uri.userInfo == null) { "Model source must use HTTPS" }
                        require(spec.downloadBytes in 1..20_000_000_000L)
                        val validFormat = when (spec.modelType) {
                            ModelType.SPEECH -> "SPEECH_TRANSCRIPTION" in spec.capabilities && when (spec.family) {
                                "ParakeetCTC" -> spec.filename.endsWith(".tar.bz2") && "OFFLINE_CTC" in spec.capabilities
                                "Nemotron" -> spec.filename.endsWith(".gguf") && "STREAMING" in spec.capabilities
                                else -> spec.filename.substringAfterLast('.') in setOf("bin", "ort", "json")
                            }
                            ModelType.TEXT_CLEANUP -> if (spec.family == "Handy") spec.filename.endsWith(".gguf")
                                else spec.filename.endsWith(".litertlm")
                            else -> spec.filename.endsWith(".litertlm")
                        }
                        require(validFormat) { "Model format does not match its runtime." }
                        require(Regex("[a-f0-9]{64}").matches(spec.sha256)) { "Model checksum is required" }
                        require(spec.backend in setOf("CPU", "GPU") && spec.contextTokens > 0)
                        require(spec.minimumRamBytes > 0 && spec.recommendedRamBytes >= spec.minimumRamBytes)
                    }
            }
            require(models.map { it.id }.distinct().size == models.size) { "Duplicate model IDs" }
            return ModelCatalog(models)
        }
        fun load(context: Context) = parse(context.assets.open("alter_voice_models.json").bufferedReader().use { it.readText() })
    }
}
