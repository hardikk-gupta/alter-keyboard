package com.aistudio.altermind.voice

import com.k2fsa.sherpa.onnx.SpokenLanguageIdentification
import com.k2fsa.sherpa.onnx.SpokenLanguageIdentificationConfig
import com.k2fsa.sherpa.onnx.SpokenLanguageIdentificationWhisperConfig
import kotlinx.coroutines.currentCoroutineContext
import kotlinx.coroutines.ensureActive

const val AUTO_ROUTING_DOWNLOAD = "automatic-hindi-english"
internal const val NEMOTRON_MULTILINGUAL = "nemotron-3.5-streaming-q4"
private const val AUTO_DRAFT_PREFIX = "auto-en-hi:"

internal fun automaticRoutingEnabled(model: String?, language: String?) = language == "auto" &&
    model in setOf("parakeet-110m-ctc-int8", "parakeet-110m-tdt-fp16")
internal fun routingDraftModel(model: String?, language: String?): String? =
    if (automaticRoutingEnabled(model, language)) AUTO_DRAFT_PREFIX + model else model
internal fun speechRecoveryMessage(model: String): String = if (model.startsWith(AUTO_DRAFT_PREFIX))
    "Restore Automatic language and the same Parakeet model to resume this recording, or discard it."
    else if (model.startsWith("parakeet-")) "Restore English-only language and the saved Parakeet model to resume this recording, or discard it."
    else "Select $model again to resume this recording, or discard it."

fun languageIdentificationSpecs(catalog: ModelCatalog) = catalog.models.filter {
    it.modelType == ModelType.LANGUAGE_DETECTION && "WHISPER_TINY_LID" in it.capabilities
}
fun automaticRoutingSpecs(catalog: ModelCatalog) = languageIdentificationSpecs(catalog) +
    catalog.models.filter { it.family == "IndicConformer" && "INDIC_HI_CTC" in it.capabilities }

enum class AutomaticSpeechLanguage { ENGLISH, HINDI, MIXED, UNCERTAIN }
internal data class AutomaticSpeechRoute(val model: String, val language: AutomaticSpeechLanguage)

/** Real neural labels only: a speech gate skips silence, never guesses language from acoustics. */
internal suspend fun classifyAutomaticSpeech(audio: FloatArray, englishModel: String, multilingualAvailable: Boolean,
    detect: (FloatArray) -> String): AutomaticSpeechRoute {
    val labels = mutableListOf<String>()
    if (audio.size >= 8000) {
        val probes = mutableListOf(audio.copyOfRange(0, minOf(audio.size, 48000)))
        if (audio.size >= 96000) probes += audio.copyOfRange(audio.size - 48000, audio.size)
        for (probe in probes) {
            currentCoroutineContext().ensureActive()
            if (!hasAudibleSpeech(probe)) continue
            labels += detect(probe).trim().lowercase(java.util.Locale.ROOT)
            currentCoroutineContext().ensureActive()
        }
    }
    val language = when {
        labels.isNotEmpty() && labels.all { it == "en" } -> AutomaticSpeechLanguage.ENGLISH
        labels.isNotEmpty() && labels.all { it == "hi" } -> AutomaticSpeechLanguage.HINDI
        "en" in labels && "hi" in labels -> AutomaticSpeechLanguage.MIXED
        else -> AutomaticSpeechLanguage.UNCERTAIN
    }
    val target = when (language) {
        AutomaticSpeechLanguage.ENGLISH -> englishModel
        AutomaticSpeechLanguage.HINDI -> INDIC_HINDI_MODEL
        AutomaticSpeechLanguage.MIXED -> {
            check(multilingualAvailable) { "Mixed Hindi/English needs the Nemotron download in Voice settings. Your recording is saved."
            }
            NEMOTRON_MULTILINGUAL
        }
        AutomaticSpeechLanguage.UNCERTAIN -> if (multilingualAvailable) NEMOTRON_MULTILINGUAL else englishModel
    }
    return AutomaticSpeechRoute(target, language)
}

/** Same Whisper LID API used by Sayso; our decisions never fabricate confidence probabilities. */
internal class AutomaticLanguageIdentifier private constructor(private val detector: SpokenLanguageIdentification) {
    fun detect(samples: FloatArray): String {
        val stream = detector.createStream()
        try {
            stream.acceptWaveform(samples, 16000)
            return detector.compute(stream)
        } finally { stream.release() }
    }
    fun close() = detector.release()
    companion object {
        suspend fun open(store: ModelStore, specs: List<ModelSpec>): AutomaticLanguageIdentifier {
            check(specs.size == 2 && specs.all(store::installed)) { "Download Automatic Hindi/English routing in Voice settings." }
            for (spec in specs) store.verify(spec)
            currentCoroutineContext().ensureActive()
            val encoder = specs.single { it.filename.contains("encoder") }
            val decoder = specs.single { it.filename.contains("decoder") }
            return AutomaticLanguageIdentifier(SpokenLanguageIdentification(config = SpokenLanguageIdentificationConfig(
                whisper = SpokenLanguageIdentificationWhisperConfig(encoder = store.file(encoder).absolutePath,
                    decoder = store.file(decoder).absolutePath, tailPaddings = 0),
                numThreads = minOf(2, Runtime.getRuntime().availableProcessors()).coerceAtLeast(1),
                provider = "cpu", debug = false
            )))
        }
    }
}
