package com.aistudio.altermind.voice

import android.content.Context

/** ALTER's voice pack inside the keyboard: deterministic formatting always, the Qwen3 1.7B editor when enabled and installed. */
object VoicePack {
    const val CLEANUP_MODEL = "qwen3-1.7b-cleanup"
    internal const val ENABLED = "dictation_cleanup"
    /**
     * Provisional cleanup while recording reloads the editor on every pause. Qwen3 1.7B takes seconds per pass on a
     * phone CPU and would compete with speech recognition, so cleanup runs once after recording instead.
     */
    internal const val LIVE_CLEANUP = false

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
        // Qwen3 handles Hindi and Hinglish, so Hindi dictation is cleaned too (LFM2.5 had to skip it).
        if (!prefs.getBoolean(ENABLED, false)) return formatDictation(cleanDictation(transcript))
        val catalog = ModelCatalog.load(context)
        val spec = catalog.find(CLEANUP_MODEL)
        val store = ModelStore(context)
        if (!store.installed(spec)) return formatDictation(cleanDictation(transcript))
        return AlterDictationAgent.process(transcript) { chunk ->
            val result = StringBuilder()
            engine(context).generate(spec, dictationCleanupRequest(chunk))
                .collect { result.append(it); check(result.length <= 3000) { "Invalid cleanup response" } }
            stripThinking(result.toString()).also { check(it.isNotBlank()) { "The cleanup model produced no text." } }
        }
    }
}

/** Thinking is disabled for Qwen3; drop an empty or stray reasoning block if one still appears. */
internal fun stripThinking(text: String): String = text.replace(Regex("(?s)<think>.*?</think>"), "").trim()

internal fun voicePrefs(context: Context) = context.getSharedPreferences("alter_voice", Context.MODE_PRIVATE)
