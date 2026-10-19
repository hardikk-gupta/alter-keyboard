package com.aistudio.altermind.voice

import android.content.Context
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.File

/** What the keyboard's settings page needs from the voice pack. */
object VoiceSettings {
    const val DEVICE_SPEECH_ID = DEVICE_SPEECH
    val LANGUAGES = listOf("auto", "en-IN", "hi-IN")

    fun deviceSpeechAvailable(context: Context) = com.aistudio.altermind.voice.deviceSpeechAvailable(context)

    fun language(context: Context) = voicePrefs(context).getString("voice_language", "auto") ?: "auto"
    fun setLanguage(context: Context, language: String) {
        require(language in LANGUAGES)
        voicePrefs(context).edit().putString("voice_language", language).apply()
    }

    fun cleanupSpec(context: Context) = ModelCatalog.load(context).find(VoicePack.CLEANUP_MODEL)

    /** Bytes held by downloaded models, including unfinished downloads. */
    suspend fun storageBytes(context: Context): Long = withContext(Dispatchers.IO) {
        File(context.noBackupFilesDir, "models").walkTopDown().filter { it.isFile }.sumOf { it.length() }
    }

    /** Deletes a speech choice's files, keeping any shared with another installed choice; unselects it if in use. */
    suspend fun deleteSpeech(context: Context, choice: SpeechChoice) {
        val models = SpeechModels.get(context)
        check(!DictationController.isActive()) { "Finish recording before deleting a speech model." }
        if (models.recordingSelected == choice.id) { models.release(); models.clearSelection() }
        val shared = speechChoices(models.catalog).filter { it.id != choice.id && it.installed(models.store) }
            .flatMap { it.files }.map { it.id }.toSet()
        ModelDownloads.delete(context, choice.files, keep = shared)
    }
}
