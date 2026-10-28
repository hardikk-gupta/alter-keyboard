package com.aistudio.altermind.voice

import kotlinx.coroutines.test.runTest
import org.junit.Assert.*
import org.junit.Test
import java.io.File
import java.nio.file.Files
import java.security.MessageDigest

class IndicHindiRoutingTest {
    @Test fun allThreeRequestedModelsUseHindiEngineOnlyInExplicitHindiMode() {
        for (id in listOf("parakeet-110m-ctc-int8", "parakeet-110m-tdt-fp16", "nemotron-3.5-streaming-q4")) {
            assertEquals(INDIC_HINDI_MODEL, effectiveSpeechModel(id, "hi-IN"))
            for (language in listOf("auto", "en-IN", null)) assertEquals(id, effectiveSpeechModel(id, language))
        }
    }
    @Test fun existingIndependentChoicesAndNoSelectionArePreserved() {
        for (id in listOf(null, DEVICE_SPEECH, "whisper-oriserve-hinglish-fp16", "moonshine-en", INDIC_HINDI_MODEL)) {
            assertEquals(id, effectiveSpeechModel(id, "hi-IN"))
        }
    }
    @Test fun failedAdaptationLeavesDownloadedBytesIntactAndCleansStaging() = runTest {
        val root = Files.createTempDirectory("indic-adaptation").toFile()
        try {
            val bytes = "fixture".toByteArray()
            val sha = MessageDigest.getInstance("SHA-256").digest(bytes).joinToString("") { "%02x".format(it) }
            val spec = ModelSpec("fixture", "Fixture", "Speech", "Fixture", "IndicConformer", "v1", "fixture",
                "model.int8.onnx", "https://example.com/model.int8.onnx", bytes.size.toLong(), sha, "MIT",
                ModelType.SPEECH, setOf("SPEECH_TRANSCRIPTION", "INDIC_HI_CTC"), 1, 1, "CPU", false, 1)
            val store = ModelStore(File(root, "models"))
            val source = store.file(spec).apply { parentFile!!.mkdirs(); writeBytes(bytes) }
            val adapted = File(root, "adapted")
            assertTrue(runCatching { prepareIndicConformer(store, spec, adapted) }.isFailure)
            assertArrayEquals(bytes, source.readBytes())
            assertTrue(adapted.listFiles().orEmpty().isEmpty())
        } finally { root.deleteRecursively() }
    }
}
