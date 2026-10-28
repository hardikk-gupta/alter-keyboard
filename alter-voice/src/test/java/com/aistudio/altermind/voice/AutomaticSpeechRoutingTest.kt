package com.aistudio.altermind.voice

import kotlinx.coroutines.*
import kotlinx.coroutines.test.runTest
import org.junit.Assert.*
import org.junit.Test

class AutomaticSpeechRoutingTest {
    private val english = "parakeet-110m-ctc-int8"
    private fun speech(samples: Int = 32000) = FloatArray(samples) { .1f }

    @Test fun englishPreservesEitherSelectedParakeetDecoder() = runTest {
        for (base in listOf(english, "parakeet-110m-tdt-fp16")) {
            val result = classifyAutomaticSpeech(speech(), base, true) { "en" }
            assertEquals(base, result.model); assertEquals(AutomaticSpeechLanguage.ENGLISH, result.language)
        }
    }
    @Test fun hindiUsesTheIndependentRecognizerAndLimitsClassifierInput() = runTest {
        var probes = 0
        val result = classifyAutomaticSpeech(speech(160000), english, false) {
            probes++; assertEquals(48000, it.size); "hi"
        }
        assertEquals(2, probes); assertEquals(INDIC_HINDI_MODEL, result.model)
    }
    @Test fun conflictingLabelsUseNemotronWithoutMutatingRecordedAudio() = runTest {
        val audio = speech(128000).apply { fill(.2f, 80000) }
        val original = audio.copyOf()
        val result = classifyAutomaticSpeech(audio, english, true) { probe ->
            val code = if (probe.first() == .1f) "en" else "hi"
            probe.fill(0f) // Even a detector touching its copy cannot modify the decoder's full audio.
            code
        }
        assertEquals(NEMOTRON_MULTILINGUAL, result.model)
        assertEquals(AutomaticSpeechLanguage.MIXED, result.language)
        assertArrayEquals(original, audio, 0f)
    }
    @Test fun mixedSpeechWithoutMultilingualModelCannotCompleteAsEnglish() = runTest {
        var calls = 0
        val result = runCatching { classifyAutomaticSpeech(speech(96000), english, false) { if (calls++ == 0) "en" else "hi" } }
        assertTrue(result.isFailure)
        assertTrue(result.exceptionOrNull()!!.message!!.contains("Your recording is saved"))
    }
    @Test fun shortAndSilentClipsNeverInventALanguage() = runTest {
        for (audio in listOf(speech(6000), FloatArray(96000))) {
            val result = classifyAutomaticSpeech(audio, english, false) { error("No neural call expected") }
            assertEquals(english, result.model)
            assertEquals(AutomaticSpeechLanguage.UNCERTAIN, result.language)
        }
    }
    @Test fun unknownLabelsUseMultilingualOnlyWhenInstalled() = runTest {
        for (available in listOf(true, false)) {
            val result = classifyAutomaticSpeech(speech(), english, available) { "unknown" }
            assertEquals(if (available) NEMOTRON_MULTILINGUAL else english, result.model)
        }
    }
    @Test fun leadingOrTrailingSilenceIsSkippedRatherThanClassified() = runTest {
        val leading = FloatArray(96000).apply { fill(.1f, 48000) }
        val trailing = FloatArray(96000).apply { fill(.1f, 0, 48000) }
        for (audio in listOf(leading, trailing)) {
            var calls = 0
            assertEquals(INDIC_HINDI_MODEL, classifyAutomaticSpeech(audio, english, false) { calls++; "hi" }.model)
            assertEquals(1, calls)
        }
    }
    @Test fun cancellationAfterComputeCannotPublishALateDecision() = runTest {
        var result: AutomaticSpeechRoute? = null
        val job = launch {
            val ownJob = currentCoroutineContext().job
            result = classifyAutomaticSpeech(speech(), english, true) { ownJob.cancel(); "hi" }
        }
        job.join()
        assertTrue(job.isCancelled); assertNull(result)
    }
    @Test fun automaticDraftIdentityDoesNotAliasExplicitModesOrOtherModels() {
        val key = routingDraftModel(english, "auto")
        assertNotEquals(english, key)
        assertEquals(english, routingDraftModel(english, "en-IN"))
        assertEquals(INDIC_HINDI_MODEL, routingDraftModel(INDIC_HINDI_MODEL, "hi-IN"))
        assertEquals(NEMOTRON_MULTILINGUAL, routingDraftModel(NEMOTRON_MULTILINGUAL, "auto"))
        assertNull(routingDraftModel(null, "auto"))
        assertTrue(speechRecoveryMessage(key!!).contains("Automatic language"))
        assertTrue(speechRecoveryMessage(english).contains("English-only language"))
    }
}
