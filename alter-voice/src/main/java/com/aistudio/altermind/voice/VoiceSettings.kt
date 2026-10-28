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
    suspend fun setLanguage(context: Context, language: String) {
        require(language in LANGUAGES)
        check(!DictationController.isActive()) { "Finish dictation before changing its language." }
        DictationController.speechSetup {
            voicePrefs(context).edit().putString("voice_language", language).apply()
        }
    }

    fun cleanupSpec(context: Context) = ModelCatalog.load(context).find(VoicePack.CLEANUP_MODEL)

    /** Bytes held by downloaded models, including unfinished downloads. */
    suspend fun storageBytes(context: Context): Long = withContext(Dispatchers.IO) {
        File(context.noBackupFilesDir, "models").walkTopDown().filter { it.isFile }.sumOf { it.length() }
    }

    suspend fun deleteCleanup(context: Context) {
        DictationController.speechSetup {
            VoicePack.setCleanupEnabled(context, false)
            ModelDownloads.delete(context, listOf(cleanupSpec(context)))
        }
    }

    suspend fun removeRetiredDownloads(context: Context) = withContext(Dispatchers.IO) {
        if (DictationController.isActive()) return@withContext
        DictationController.speechSetup {
            val models = SpeechModels.get(context)
            models.release()
            models.store.removeRetired(models.catalog.models.map { it.id }.toSet())
            voicePrefs(context).edit().remove("cleanup_model").apply()
            if (voicePrefs(context).getString("speech_model", null) !in retainedSpeechIds) models.clearSelection()
            File(context.noBackupFilesDir, "speech-indicconformer").deleteRecursively()
        }
    }

    /** Releases active caches and deletes unshared files; removing Hindi dependencies preserves the saved base choice. */
    suspend fun deleteSpeech(context: Context, choice: SpeechChoice) {
        DictationController.speechSetup {
            val models = SpeechModels.get(context)
            // The loaded decoder may be an automatic route, not the saved base choice.
            models.release()
            if (models.recordingSelected == choice.id) models.clearSelection()
            val shared = speechChoices(models.catalog).filter { it.id != choice.id && it.installed(models.store) }
                .flatMap { it.files }.map { it.id }.toSet()
            ModelDownloads.delete(context, choice.files, keep = shared)
        }
    }
}
