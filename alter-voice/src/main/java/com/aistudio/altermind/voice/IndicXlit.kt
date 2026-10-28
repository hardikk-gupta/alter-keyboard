package com.aistudio.altermind.voice

import ai.onnxruntime.OnnxTensor
import ai.onnxruntime.OrtEnvironment
import ai.onnxruntime.OrtSession
import kotlinx.coroutines.currentCoroutineContext
import kotlinx.coroutines.ensureActive
import org.json.JSONObject
import java.io.Closeable
import java.text.Normalizer

const val INDIC_XLIT_DOWNLOAD = "indicxlit-hindi-roman"
fun indicXlitSpecs(catalog: ModelCatalog) = catalog.models.filter {
    it.modelType == ModelType.TRANSLITERATION && it.family == "IndicXlit"
}

private val hindiWord = Regex("[\\u0900-\\u0963\\u0971-\\u097F][\\u0900-\\u0963\\u0971-\\u097F\\u200C\\u200D]*")

/** Replace only Hindi words. ICU still converts native digits/danda; existing Latin stays verbatim. */
internal fun romanizeWithIndicXlit(text: String, word: (String) -> String): String =
    romanizeHindiText(hindiWord.replace(text) { word(it.value) })

// User-requested spellings for these exact words only, never arbitrary names or long vowels.
internal fun conversationalXlit(source: String, roman: String): String = when {
    source == "जा" && roman == "jaa" -> "ja"
    source == "रहा" && roman == "rahaa" -> "raha"
    source == "बेबी" && roman == "bebi" -> "baby"
    source == "चाहिए" && roman == "chaahiye" -> "chahiye"
    source == "कॉल" && roman == "coll" -> "call"
    source == "यू" && roman == "yuu" -> "you"
    source == "देन" && roman == "daine" -> "dena"
    source == "आइल" && roman == "aail" -> "ail"
    source in setOf("हूँ", "हूं") && roman in setOf("huun", "hoon", "hun") -> "hun"
    else -> roman
}

/** Official IndicXlit weights exported without training. Owned by SpeechModels' inference mutex. */
internal class IndicXlit private constructor(
    private val encoder: OrtSession, private val decoder: OrtSession,
    private val source: Map<String, Int>, private val target: List<String>
) : Closeable {
    private val allowed = target.indices.filter { it == 2 || target[it].matches(Regex("[a-z]")) }
    private val env = OrtEnvironment.getEnvironment()
    private val cache = object : LinkedHashMap<String, String>(128, 0.75f, true) {
        override fun removeEldestEntry(eldest: MutableMap.MutableEntry<String, String>?) = size > 1024
    }
    private data class Beam(val tokens: List<Long>, val score: Double)

    suspend fun convert(text: String): String {
        val job = currentCoroutineContext()
        return romanizeWithIndicXlit(text) { word ->
            job.ensureActive()
            cache[word] ?: decode(word) { job.ensureActive() }.also { cache[word] = it }
        }.also { job.ensureActive() }
    }

    private fun decode(word: String, checkActive: () -> Unit): String {
        // Never truncate long words or silently replace an unsupported spelling.
        val normalized = Normalizer.normalize(word.replace("\u200C", "").replace("\u200D", ""), Normalizer.Form.NFD)
        require(normalized.isNotEmpty() && normalized.length <= 64 && normalized.all { source.containsKey(it.toString()) }) {
            "IndicXlit cannot convert this Hindi word. Your recording is saved; delete the IndicXlit download to use basic conversion."
        }
        val input = longArrayOf(source.getValue("__hi__").toLong()) +
            normalized.map { source.getValue(it.toString()).toLong() }.toLongArray() + longArrayOf(2L)
        OnnxTensor.createTensor(env, arrayOf(input)).use { tokens ->
            encoder.run(mapOf("tokens" to tokens)).use { encoded ->
                val memory = encoded[0] as OnnxTensor
                var active = listOf(Beam(listOf(2L), 0.0))
                val finished = mutableListOf<Beam>()
                repeat(48) {
                    checkActive()
                    val next = mutableListOf<Beam>()
                    for (beam in active) {
                        checkActive()
                        OnnxTensor.createTensor(env, arrayOf(beam.tokens.toLongArray())).use { prefix ->
                            decoder.run(mapOf("tokens" to prefix, "memory" to memory)).use { decoded ->
                                @Suppress("UNCHECKED_CAST")
                                val logits = (decoded[0].value as Array<FloatArray>)[0]
                                // Bounded beam search: suppress BOS, PAD, UNK and language symbols.
                                val max = allowed.maxOf { logits[it].toDouble() }
                                val normalizer = max + kotlin.math.ln(allowed.sumOf { kotlin.math.exp(logits[it] - max) })
                                for (id in allowed.sortedByDescending { logits[it] }.take(4)) {
                                    if (id == 2 && beam.tokens.size == 1) continue
                                    val candidate = Beam(beam.tokens + id.toLong(), beam.score + logits[id] - normalizer)
                                    if (id == 2) finished += candidate else next += candidate
                                }
                            }
                        }
                    }
                    active = next.sortedByDescending { it.score }.take(4)
                    // A live beam's score can only decrease; its longest possible normalized score
                    // bounds whether it could still outrank the best completed candidate.
                    val best = finished.maxByOrNull { it.score / (it.tokens.size - 1) }
                    if (active.isEmpty() || (best != null && active.all {
                        it.score / 48 <= best.score / (best.tokens.size - 1)
                    })) return render(word, checkNotNull(best))
                }
                error("IndicXlit conversion exceeded its word limit. Your recording is saved.")
            }
        }
    }
    private fun render(word: String, beam: Beam): String = conversationalXlit(word,
        beam.tokens.drop(1).dropLast(1).joinToString("") { target[it.toInt()] }).also {
        check(it.isNotBlank()) { "IndicXlit returned an empty word. Your recording is saved." }
    }
    override fun close() { try { encoder.close() } finally { decoder.close(); cache.clear() } }

    companion object {
        suspend fun open(store: ModelStore, files: List<ModelSpec>): IndicXlit {
            require(files.size == 3 && files.all(store::installed)) { "Download all IndicXlit components in Voice settings." }
            files.forEach { store.verify(it) }
            val vocab = JSONObject(store.file(files.single { it.filename == "vocab.json" }).readText())
            fun vocabulary(key: String) = vocab.getJSONArray(key).let { a -> (0 until a.length()).map { a.getString(it) } }
            val source = vocabulary("source"); val target = vocabulary("target")
            require(source.size == 806 && target.size == 54 && "__hi__" in source)
            val env = OrtEnvironment.getEnvironment()
            OrtSession.SessionOptions().use { options ->
                options.setIntraOpNumThreads(2); options.setInterOpNumThreads(1)
                val encoder = env.createSession(store.file(files.single { it.filename == "encoder.onnx" }).absolutePath, options)
                try {
                    val decoder = env.createSession(store.file(files.single { it.filename == "decoder.onnx" }).absolutePath, options)
                    return IndicXlit(encoder, decoder, source.withIndex().associate { it.value to it.index }, target)
                } catch (e: Throwable) { encoder.close(); throw e }
            }
        }
    }
}
