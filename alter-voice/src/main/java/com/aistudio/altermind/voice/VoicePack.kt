package com.aistudio.altermind.voice

import android.content.Context

/** ALTER's voice pack inside the keyboard: deterministic formatting always, the LFM2.5 editor when enabled and installed. */
object VoicePack {
    const val CLEANUP_MODEL = "lfm25-350m-cleanup"
    internal const val ENABLED = "dictation_cleanup"

    private var engine: CleanupEngine? = null
    @Synchronized private fun engine(context: Context) =
        engine ?: CleanupEngine(context.applicationContext, ModelStore(context.applicationContext)).also { engine = it }

    fun cleanupEnabled(context: Context) = voicePrefs(context).getBoolean(ENABLED, false)
    fun setCleanupEnabled(context: Context, enabled: Boolean) { voicePrefs(context).edit().putBoolean(ENABLED, enabled).apply() }

    suspend fun polish(context: Context, rawTranscript: String): String {
        val transcript = applyVoiceCorrections(rawTranscript, VoiceCorrections(context).read())
        // No catalog reads, model verification, engine loading or inference for simple short dictation.
        if (dictationCleanupLevel(transcript) == DictationCleanupLevel.BASIC)
            return formatDictation(cleanDictation(transcript))
        val prefs = voicePrefs(context)
        if (!prefs.getBoolean(ENABLED, false) || prefs.getString("voice_language", "auto") == "hi-IN") return formatDictation(cleanDictation(transcript))
        val catalog = ModelCatalog.load(context)
        val spec = catalog.find(CLEANUP_MODEL)
        val store = ModelStore(context)
        if (!store.installed(spec)) return formatDictation(cleanDictation(transcript))
        return AlterDictationAgent.process(transcript) { chunk ->
            val result = StringBuilder()
            engine(context).generate(spec, dictationCleanupRequest(chunk))
                .collect { result.append(it); check(result.length <= 3000) { "Invalid cleanup response" } }
            check(result.isNotBlank()) { "The cleanup model produced no text." }
            result.toString()
        }
    }
}

internal fun voicePrefs(context: Context) = context.getSharedPreferences("alter_voice", Context.MODE_PRIVATE)
