package com.aistudio.altermind.voice

import com.k2fsa.sherpa.onnx.*
import kotlinx.coroutines.currentCoroutineContext
import kotlinx.coroutines.ensureActive
import org.apache.commons.compress.archivers.tar.TarArchiveInputStream
import org.apache.commons.compress.compressors.bzip2.BZip2CompressorInputStream
import java.io.File

internal const val PARAKEET_PACKAGE = "sherpa-onnx-nemo-parakeet_tdt_ctc_110m-en-36000-int8"
internal data class ParakeetComponent(val bytes: Long, val sha256: String)
internal val parakeetComponents = mapOf(
    "model.int8.onnx" to ParakeetComponent(131652171, "9177a9146cf32ee0cc8152276ef95116f312018d316be37ccf57f7efea81fc1a"),
    "tokens.txt" to ParakeetComponent(9953, "450e56bd2f036fe5b6aa821865838cc5aa9d8b0106134ce9a9ba0664abe6cd10")
)

/** Only two whitelisted regular files are written; caller has verified the downloaded archive. */
internal suspend fun extractParakeet(archive: File, target: File,
    components: Map<String, ParakeetComponent> = parakeetComponents) {
    check(target.mkdirs() || target.isDirectory)
    val seen = mutableSetOf<String>()
    TarArchiveInputStream(BZip2CompressorInputStream(archive.inputStream().buffered())).use { tar ->
        var entries = 0
        var total = 0L
        while (true) {
            currentCoroutineContext().ensureActive()
            val entry = tar.nextTarEntry ?: break
            check(++entries <= 128 && entry.size in 0..160_000_000L) { "Invalid Parakeet archive." }
            total += entry.size
            check(total <= 160_000_000L) { "Parakeet archive is too large." }
            val name = entry.name.removePrefix("./").removePrefix("$PARAKEET_PACKAGE/")
            val spec = components[name] ?: continue
            check(entry.isFile && !entry.isSymbolicLink && !entry.isLink && seen.add(name) && entry.size == spec.bytes) {
                "Invalid Parakeet component."
            }
            val output = File(target, name)
            output.outputStream().use { out ->
                val buffer = ByteArray(256 * 1024)
                var copied = 0L
                while (true) {
                    currentCoroutineContext().ensureActive()
                    val count = tar.read(buffer)
                    if (count < 0) break
                    copied += count
                    check(copied <= spec.bytes)
                    out.write(buffer, 0, count)
                }
                check(copied == spec.bytes)
            }
            check(ModelStore.sha256(output) == spec.sha256) { "Parakeet component checksum mismatch." }
        }
    }
    check(seen == components.keys) { "Parakeet archive is incomplete." }
}

internal suspend fun prepareParakeet(archive: File, target: File): File {
    val valid = parakeetComponents.all { (name, spec) ->
        val file = File(target, name)
        file.isFile && file.length() == spec.bytes && ModelStore.sha256(file) == spec.sha256
    }
    if (valid) return target
    val partial = File(target.parentFile, target.name + ".part")
    partial.deleteRecursively()
    try {
        extractParakeet(archive, partial)
        currentCoroutineContext().ensureActive()
        check(!target.exists() || target.deleteRecursively())
        check(partial.renameTo(target)) { "Could not prepare Parakeet files." }
        return target
    } finally { partial.deleteRecursively() }
}

internal class ParakeetSpeech private constructor(private val recognizer: OfflineRecognizer) {
    suspend fun decode(samples: FloatArray): String {
        currentCoroutineContext().ensureActive()
        val stream = recognizer.createStream()
        try {
            stream.acceptWaveform(samples, 16000)
            currentCoroutineContext().ensureActive()
            // The CTC API has no native abort callback. Cancellation is checked before and after each bounded window.
            recognizer.decode(stream)
            currentCoroutineContext().ensureActive()
            return recognizer.getResult(stream).text
        } finally { stream.release() }
    }
    fun close() = recognizer.release()

    companion object {
        suspend fun open(archive: File, directory: File): ParakeetSpeech {
            val files = prepareParakeet(archive, directory)
            currentCoroutineContext().ensureActive()
            return openFiles(File(files, "model.int8.onnx"), File(files, "tokens.txt"))
        }
        fun openFiles(model: File, tokens: File): ParakeetSpeech {
            return ParakeetSpeech(OfflineRecognizer(config = OfflineRecognizerConfig(
                featConfig = FeatureConfig(sampleRate = 16000, featureDim = 80, dither = 0f),
                modelConfig = OfflineModelConfig(
                    nemo = OfflineNemoEncDecCtcModelConfig(model = model.absolutePath),
                    tokens = tokens.absolutePath,
                    numThreads = minOf(2, Runtime.getRuntime().availableProcessors()).coerceAtLeast(1),
                    provider = "cpu", debug = false
                ), decodingMethod = "greedy_search"
            )))
        }
    }
}
