package com.aistudio.altermind.voice

import java.io.File
import kotlinx.coroutines.test.runTest
import org.junit.Assert.*
import org.junit.Rule
import org.junit.Test
import org.junit.rules.TemporaryFolder

class RetiredModelCleanupTest {
    @get:Rule val temp = TemporaryFolder()
    @Test fun removesOnlyRetiredVoiceDownloadsAndPreservesModelsAndDrafts() = runTest {
        val root = temp.newFolder()
        fun file(path: String) = File(root, path).apply { parentFile.mkdirs(); writeText("keep bytes") }
        val keep = file("models/speech/nemotron-3.5-streaming-q4/v1/weights.gguf")
        val draft = file("dictation-draft/recording.pcm")
        val other = file("models/generation/unrelated/v1/file")
        val retired = listOf("speech/whisper-tiny/v1/file", "speech/moonshine-en/v1/file",
            "text_cleanup/handy-350m-cleanup/v1/file", "language_detection/whisper-lid-tiny-encoder/v1/file",
            "transliteration/indicxlit-hi-roman-encoder/v1/file").map { file("models/$it") }
        val store = ModelStore(File(root, "models"))
        store.removeRetired(setOf("nemotron-3.5-streaming-q4", VoicePack.CLEANUP_MODEL))
        assertTrue(retired.none { it.exists() })
        listOf(keep, draft, other).forEach { assertEquals("keep bytes", it.readText()) }
        store.removeRetired(setOf("nemotron-3.5-streaming-q4", VoicePack.CLEANUP_MODEL))
        assertTrue(keep.exists())
    }
}
