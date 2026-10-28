package com.aistudio.altermind.voice

import kotlinx.coroutines.currentCoroutineContext
import kotlinx.coroutines.ensureActive
import java.io.File

const val INDIC_HINDI_MODEL = "indicconformer-hi-int8"

/** Hindi mode routes these English/general engines to the separately downloaded Hindi recognizer. */
internal fun effectiveSpeechModel(selected: String?, language: String?): String? =
    if (language == "hi-IN" && selected in setOf("parakeet-110m-ctc-int8", "parakeet-110m-tdt-fp16", "nemotron-3.5-streaming-q4"))
        INDIC_HINDI_MODEL else selected

// Append ONNX ModelProto.metadata_props fields: vocab_size=257, subsampling_factor=4,
// normalize_type=per_feature. Tensor data, graph and learned weights remain byte-identical.
internal val indicSherpaMetadata: ByteArray =
    "72110a0a766f6361625f73697a65120332353772170a1273756273616d706c696e675f666163746f72120134721d0a0e6e6f726d616c697a655f74797065120b7065725f66656174757265"
        .chunked(2).map { it.toInt(16).toByte() }.toByteArray()
internal const val INDIC_ADAPTED_BYTES = 137677388L
internal const val INDIC_ADAPTED_SHA256 = "411567662991f2b57e6f46348e200d5c55ebdba3083fed4c56b9020acefaecbd"

/** The catalog's original ONNX remains immutable; stage and verify the metadata-only adapted copy. */
internal suspend fun prepareIndicConformer(store: ModelStore, graph: ModelSpec, directory: File): File {
    check(directory.isDirectory || directory.mkdirs()) { "Could not prepare private Hindi model storage." }
    val target = File(directory, "indicconformer-hi-${graph.sha256}-sherpa-v1.onnx")
    if (target.isFile && target.length() == INDIC_ADAPTED_BYTES && ModelStore.sha256(target) == INDIC_ADAPTED_SHA256) return target
    val staging = File(directory, "indicconformer-hi-adapting.onnx")
    try {
        copySpeechComponent(store, graph, staging)
        currentCoroutineContext().ensureActive()
        staging.appendBytes(indicSherpaMetadata)
        check(staging.length() == INDIC_ADAPTED_BYTES && ModelStore.sha256(staging) == INDIC_ADAPTED_SHA256) {
            "Hindi model adaptation failed verification. Your recording is saved."
        }
        currentCoroutineContext().ensureActive()
        check(!target.exists() || target.delete())
        check(staging.renameTo(target)) { "Could not activate the Hindi recognizer." }
        return target
    } finally { staging.delete(); File(staging.path + ".part").delete() }
}
