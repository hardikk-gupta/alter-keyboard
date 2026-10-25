package com.aistudio.altermind.voice

import android.content.ContextWrapper
import com.aistudio.altermind.voice.ui.processingFieldFrame
import com.aistudio.altermind.voice.ui.latticeFrame
import com.aistudio.altermind.voice.ui.latticeStillFrame
import com.aistudio.altermind.voice.ui.latticeRest
import kotlinx.coroutines.*
import kotlinx.coroutines.test.*
import org.junit.Assert.*
import org.junit.Rule
import org.junit.Test
import org.junit.rules.TemporaryFolder
import java.io.File

@OptIn(ExperimentalCoroutinesApi::class)
class RecordingRecoveryTest {
    @get:Rule val temporary = TemporaryFolder()
    private fun store() = RecordingDraftStore(temporary.newFolder())
    private fun RecordingDraftStore.samples(id: String): FloatArray {
        val samples = mutableListOf<Float>()
        replay(id) { samples.addAll(it.toList()) }
        return samples.toFloatArray()
    }

    @Test fun draftSurvivesRecreationAndKeepsEveryCompletePcmSample() {
        val folder = temporary.newFolder()
        val first = RecordingDraftStore(folder)
        val draft = first.begin("whisper-tiny")
        val pcm = shortArrayOf(0, 16384, -16384, Short.MIN_VALUE, Short.MAX_VALUE)
        first.append(draft.id, pcm, pcm.size)
        // Simulate a process dying during the final two-byte sample.
        File(folder, "recording.pcm").appendBytes(byteArrayOf(7))
        val restored = RecordingDraftStore(folder)
        assertEquals(5, restored.load()!!.samples)
        assertArrayEquals(FloatArray(pcm.size) { pcm[it] / 32768f }, restored.samples(draft.id), 0f)
        restored.append(draft.id, shortArrayOf(8192), 1)
        assertArrayEquals(FloatArray(6) { if (it < 5) pcm[it] / 32768f else .25f }, first.samples(draft.id), 0f)
        assertTrue(runCatching { restored.begin("whisper-base") }.isFailure)
        restored.clear(draft.id)
        assertNull(first.load())
        assertEquals(0, folder.listFiles()!!.size)
    }

    @Test fun twoMinuteDraftReplaysEveryChunkAndEnforcesTheRecordingLimit() {
        val folder = temporary.newFolder()
        val saved = RecordingDraftStore(folder)
        val draft = saved.begin("whisper-tiny")
        val chunk = ShortArray(1600) { (it - 800).toShort() }
        repeat(1200) { saved.append(draft.id, chunk, chunk.size) }
        var samples = 0
        var chunks = 0
        RecordingDraftStore(folder).replay(draft.id) {
            assertArrayEquals(FloatArray(1600) { i -> chunk[i] / 32768f }, it, 0f)
            samples += it.size; chunks++
        }
        assertEquals(120 * 16000, samples)
        assertEquals(1200, chunks)
        java.io.RandomAccessFile(File(folder, "recording.pcm"), "rw").use {
            it.setLength(RecordingDraftStore.MAX_SAMPLES * 2L)
        }
        assertTrue(runCatching { saved.append(draft.id, shortArrayOf(1), 1) }.isFailure)
        assertEquals(RecordingDraftStore.MAX_SAMPLES, saved.load()!!.samples)
    }

    @Test fun staleControllersCannotWriteToOrDeleteANewDraft() {
        val saved = store()
        val old = saved.begin("whisper-tiny")
        saved.clear(old.id)
        val current = saved.begin("whisper-base")
        saved.append(current.id, shortArrayOf(1, 2), 2)
        assertFalse(saved.append(old.id, shortArrayOf(3), 1))
        saved.partial(old.id, "stale text")
        saved.clear(old.id)
        assertEquals(current.id, saved.load()!!.id)
        assertEquals("", saved.load()!!.partial)
    }

    @Test fun deviceRecoveryKeepsRecognizedSegmentsAndCompletedTextUntilAcknowledged() {
        val saved = store()
        val draft = saved.begin(DEVICE_SPEECH)
        saved.partial(draft.id, "first draft")
        saved.partial(draft.id, "first paragraph")
        saved.resume(draft.id)
        saved.partial(draft.id, "second paragraph")
        assertEquals("first paragraph second paragraph", saved.load()!!.deviceText)
        saved.complete(draft.id, "First paragraph. Second paragraph.")
        assertEquals("First paragraph. Second paragraph.", saved.load()!!.completed)
        saved.resume(draft.id)
        assertNull(saved.load()!!.completed)
        assertEquals("first paragraph second paragraph", saved.load()!!.prefix)
    }

    @Test fun oversizedMetadataNeverReplacesTheLastRecoverableTranscript() {
        val saved = store()
        val draft = saved.begin(DEVICE_SPEECH)
        saved.partial(draft.id, "the recoverable paragraph")
        assertTrue(runCatching { saved.partial(draft.id, "a".repeat(1_048_576)) }.isFailure)
        assertEquals("the recoverable paragraph", saved.load()!!.deviceText)
        saved.clear(draft.id)
        assertNull(saved.load())
    }

    private class Microphone(var chunks: ArrayDeque<ShortArray> = ArrayDeque(), var interrupted: Boolean = false) : DictationMicrophone {
        var starts = 0
        var closed = false
        override fun start() { starts++ }
        override fun read(buffer: ShortArray): Int {
            val next = chunks.removeFirstOrNull()
            if (next != null) { next.copyInto(buffer); return next.size }
            check(!interrupted) { "Phone call interrupted the microphone" }
            return 0
        }
        override fun stop() {}
        override fun close() { closed = true }
    }

    private class Speech : DictationSpeech {
        override val selected = "whisper-tiny"
        var warmingUp = false
        val heard = mutableListOf<Float>()
        override fun checkInstalled() {}
        override suspend fun ensureReady() { if (warmingUp) awaitCancellation() }
        override suspend fun stream(owner: Any, read: suspend () -> FloatArray?, update: (String) -> Unit): Boolean {
            heard.clear()
            while (true) { val chunk = read() ?: break; heard.addAll(chunk.toList()); update("saved words") }
            return true
        }
        override suspend fun transcribe(audio: FloatArray, owner: Any) = "saved words"
        override fun cancel(owner: Any) {}
    }

    private fun TestScope.controller(saved: RecordingDraftStore, speech: Speech, mic: Microphone,
        recordingScope: CoroutineScope = this, polish: suspend (String) -> String = { it }) = DictationController(ContextWrapper(null), recordingScope,
        speech, { mic }, polish, saved, { true }, StandardTestDispatcher(testScheduler))

    @Test fun pauseDuringModelWarmupAndRestoreResumeDoNotLoseOrDuplicateAudio() = runTest {
        val saved = store()
        val speech = Speech().apply { warmingUp = true }
        val firstMic = Microphone(ArrayDeque(listOf(shortArrayOf(1000, 2000))))
        val first = controller(saved, speech, firstMic)
        first.start(endAfterSilence = false); runCurrent()
        assertTrue(first.recording.value)
        assertEquals(2, saved.load()!!.samples)
        first.pause(); runCurrent()
        assertTrue(first.paused.value); assertFalse(first.processing.value); assertTrue(firstMic.closed)
        assertNull(first.completedText.value)

        speech.warmingUp = false
        val nextMic = Microphone(ArrayDeque(listOf(shortArrayOf(3000, 4000))))
        val restored = controller(saved, speech, nextMic)
        assertTrue(restored.paused.value)
        assertEquals(0, nextMic.starts) // Merely reopening never opens the microphone.
        restored.start(endAfterSilence = false); runCurrent()
        restored.stop(); advanceUntilIdle()
        assertArrayEquals(floatArrayOf(1000f, 2000f, 3000f, 4000f).map { it / 32768f }.toFloatArray(), speech.heard.toFloatArray(), 0f)
        assertEquals(4, saved.load()!!.samples)
        assertEquals("saved words", restored.completedText.value)
        assertNotNull(saved.load()) // Completing recognition is not acknowledgement of insertion.
        restored.acknowledgeInsertion()
        assertNull(saved.load())
    }

    @Test fun serviceScopeDeathWithoutPauseStillRestoresADraftWithoutRecordingOrInserting() = runTest {
        val saved = store()
        val serviceScope = CoroutineScope(SupervisorJob() + StandardTestDispatcher(testScheduler))
        val recording = controller(saved, Speech().apply { warmingUp = true },
            Microphone(ArrayDeque(listOf(shortArrayOf(1000, 2000, 3000)))), recordingScope = serviceScope)
        recording.start(false); runCurrent()
        assertEquals(3, saved.load()!!.samples)
        serviceScope.cancel(); runCurrent() // No pause() or graceful keyboard teardown.
        assertFalse(DictationController.isActive())
        val mic = Microphone()
        val restored = controller(saved, Speech(), mic)
        assertTrue(restored.paused.value)
        assertFalse(restored.recording.value)
        assertEquals(0, mic.starts)
        assertNull(restored.completedText.value)
        restored.retrySavedAudio(); advanceUntilIdle()
        assertEquals("saved words", restored.completedText.value)
        restored.acknowledgeInsertion()
    }

    @Test fun microphoneInterruptionAutomaticallyPausesAndRestoresTheCapturedAudio() = runTest {
        val saved = store()
        val mic = Microphone(ArrayDeque(listOf(shortArrayOf(123, 456))), interrupted = true)
        val current = controller(saved, Speech().apply { warmingUp = true }, mic)
        current.start(false); advanceUntilIdle()
        assertTrue(current.paused.value)
        assertFalse(current.recording.value)
        assertFalse(current.processing.value)
        assertNull(current.completedText.value)
        assertEquals(2, saved.load()!!.samples)
        val restored = controller(saved, Speech(), Microphone())
        restored.retrySavedAudio(); advanceUntilIdle()
        assertEquals("saved words", restored.completedText.value)
        restored.cancel()
    }

    @Test fun processingInterruptedDuringCleanupNeverPublishesLateTextAndCanBeRetried() = runTest {
        val saved = store()
        val draft = saved.begin("whisper-tiny")
        saved.append(draft.id, shortArrayOf(1000), 1)
        val gate = CompletableDeferred<Unit>()
        val current = controller(saved, Speech(), Microphone()) {
            withContext(NonCancellable) { gate.await() }; "late result"
        }
        current.retrySavedAudio(); runCurrent()
        assertTrue(current.processing.value)
        current.pause()
        val restored = controller(saved, Speech(), Microphone())
        restored.retrySavedAudio(); runCurrent()
        assertNull(restored.completedText.value) // New session waits until the cancelled old decoder releases ownership.
        gate.complete(Unit); advanceUntilIdle()
        assertNull(current.completedText.value)
        assertEquals("saved words", restored.completedText.value)
        // A failed host insertion leaves the polished text durable, but reopening cannot insert it on its own.
        restored.insertionFailed()
        val reopened = controller(saved, Speech(), Microphone())
        assertTrue(reopened.paused.value); assertNull(reopened.completedText.value)
        reopened.retrySavedAudio(); advanceUntilIdle()
        assertEquals("saved words", reopened.completedText.value)
        reopened.acknowledgeInsertion()
    }

    @Test fun rapidPauseResumeWaitsForOldDecoderAndExplicitCancelRemovesTheDraft() = runTest {
        val saved = store()
        val mic = Microphone(ArrayDeque(listOf(shortArrayOf(1000))))
        val current = controller(saved, Speech(), mic)
        current.start(false); runCurrent()
        current.pause()
        mic.chunks.add(shortArrayOf(2000))
        current.start(false); runCurrent()
        assertTrue(current.recording.value)
        assertEquals(2, saved.load()!!.samples)
        current.cancel(); advanceUntilIdle()
        assertNull(saved.load())
        assertFalse(current.paused.value)
        assertFalse(DictationController.isActive())
    }

    @Test fun hiddenKeyboardKeepsCleanupRunningAndCompletedTextDurable() = runTest {
        val saved = store()
        val draft = saved.begin("whisper-tiny")
        saved.append(draft.id, shortArrayOf(1000), 1)
        val gate = CompletableDeferred<Unit>()
        val current = controller(saved, Speech(), Microphone()) { gate.await(); "Finished paragraph." }
        current.retrySavedAudio(); runCurrent()
        assertTrue(current.continueProcessingWhenHidden())
        assertTrue(current.processing.value)
        gate.complete(Unit); advanceUntilIdle()
        assertEquals("Finished paragraph.", current.completedText.value)
        assertTrue(current.continueProcessingWhenHidden()) // Result ready, UI delivery still pending.
        // Clipboard delivery does not acknowledge editor insertion or delete the private copy.
        current.pause()
        assertEquals("Finished paragraph.", saved.load()!!.completed)
        val restored = controller(saved, Speech(), Microphone())
        assertTrue(restored.paused.value)
        assertNull(restored.completedText.value)
        restored.retrySavedAudio(); advanceUntilIdle()
        assertEquals("Finished paragraph.", restored.completedText.value)
        restored.acknowledgeInsertion()
        assertNull(saved.load())
    }

    @Test fun hideBeforeCaptureStartsNeverOpensTheMicrophoneInBackground() = runTest {
        val mic = Microphone()
        val current = controller(store(), Speech(), mic)
        current.start(false) // Capture is scheduled but has not opened the microphone yet.
        assertFalse(current.continueProcessingWhenHidden())
        advanceUntilIdle()
        assertEquals(0, mic.starts)
        assertFalse(current.processing.value)
        assertFalse(current.recording.value)
    }

    @Test fun hideWhileRecordingPausesInsteadOfContinuingCapture() = runTest {
        val saved = store()
        val mic = Microphone(ArrayDeque(listOf(shortArrayOf(123, 456))))
        val current = controller(saved, Speech().apply { warmingUp = true }, mic)
        current.start(false); runCurrent()
        assertTrue(current.recording.value)
        assertFalse(current.continueProcessingWhenHidden())
        advanceUntilIdle()
        assertTrue(mic.closed)
        assertTrue(current.paused.value)
        assertEquals(2, saved.load()!!.samples)
        assertNull(current.completedText.value)
        current.cancel()
    }

    @Test fun startupIsNotProcessingAndErrorsOverrideRecoverablePause() = runTest {
        val current = controller(store(), Speech().apply { warmingUp = true }, Microphone())
        current.start(false)
        assertTrue(current.starting.value)
        assertFalse(current.processing.value)
        assertEquals(com.aistudio.altermind.voice.ui.PillState.Starting,
            com.aistudio.altermind.voice.ui.voicePillState(false, true, false, false, false, null))
        current.pause(); advanceUntilIdle()
        assertFalse(current.starting.value)
        assertEquals(com.aistudio.altermind.voice.ui.PillState.Error,
            com.aistudio.altermind.voice.ui.voicePillState(false, false, false, true, true, NO_SPEECH_MESSAGE))
    }

    @Test fun silenceDuringWarmupStopsWithNoSpeechErrorInsteadOfWaitingForDecoder() = runTest {
        val saved = store()
        val mic = Microphone(ArrayDeque(List(100) { ShortArray(1600) }))
        var polished = false
        val current = controller(saved, Speech().apply { warmingUp = true }, mic) { polished = true; it }
        current.start(false); advanceUntilIdle()
        assertEquals(NO_SPEECH_MESSAGE, current.error.value)
        assertTrue(current.noSpeechError)
        assertFalse(current.starting.value)
        assertFalse(current.recording.value)
        assertFalse(current.processing.value)
        assertNull(current.completedText.value)
        assertFalse(polished)
        assertTrue(mic.closed)
        assertFalse(DictationController.isActive())
        assertNotNull(saved.load()) // An error never discards a private draft.
        current.cancel()
    }

    @Test fun liveCleanupRevisesEarlierWordsAndReusesOnlyAnExactFinalSnapshot() = runTest {
        val raw = kotlinx.coroutines.flow.MutableStateFlow("")
        val active = kotlinx.coroutines.flow.MutableStateFlow(true)
        val requests = mutableListOf<String>()
        val live = LiveDictationCleanup(this, raw, active) {
            requests += it
            formatDictation(resolveSpokenCorrections(it))
        }
        raw.value = "the meeting is Friday"; advanceTimeBy(1300); runCurrent()
        assertEquals(listOf("the meeting is Friday"), requests) // Edit ran before recording ended.
        raw.value = "the meeting is Friday actually make it Monday"; advanceTimeBy(1300); runCurrent()
        active.value = false
        assertEquals("The meeting is Monday.", live.finish(raw.value))
        assertEquals(2, requests.size) // Identical final ASR output does not run the model again.
    }

    @Test fun finalRevisionCancelsProvisionalWorkAndRawTextSurvivesCleanup() = runTest {
        val raw = kotlinx.coroutines.flow.MutableStateFlow("the meeting is Friday")
        val active = kotlinx.coroutines.flow.MutableStateFlow(true)
        var cancelled = false
        val live = LiveDictationCleanup(this, raw, active) {
            if (it.endsWith("Friday")) try { awaitCancellation() } finally { cancelled = true }
            else formatDictation(resolveSpokenCorrections(it))
        }
        advanceTimeBy(1300); runCurrent()
        active.value = false
        assertEquals("The meeting is Monday.", live.finish("the meeting is Friday actually make it Monday"))
        assertTrue(cancelled)
        val saved = store(); val draft = saved.begin("whisper-tiny")
        saved.append(draft.id, shortArrayOf(1000), 1)
        saved.raw(draft.id, "um Friday actually Monday")
        saved.complete(draft.id, "Monday")
        assertEquals("um Friday actually Monday", saved.load()!!.raw)
        assertEquals("Monday", saved.load()!!.completed)
        saved.clear(draft.id); assertNull(saved.load())
    }

    @Test fun finishWaitsForIdenticalInFlightCleanupWithoutRestartingIt() = runTest {
        val raw = kotlinx.coroutines.flow.MutableStateFlow("the meeting is Friday actually make it Monday")
        val active = kotlinx.coroutines.flow.MutableStateFlow(true)
        val gate = CompletableDeferred<Unit>()
        var calls = 0
        val live = LiveDictationCleanup(this, raw, active) { calls++; gate.await(); "The meeting is Monday." }
        advanceTimeBy(1300); runCurrent()
        active.value = false
        val final = async { live.finish(raw.value) }
        runCurrent(); assertFalse(final.isCompleted); assertEquals(1, calls)
        gate.complete(Unit)
        assertEquals("The meeting is Monday.", final.await())
        assertEquals(1, calls)
    }

    @Test fun thinkingFieldFillsWideRectanglesWithTheOriginalOrbChoreography() {
        val wide = processingFieldFrame(24, 3, 600f)
        assertEquals(72, wide.size)
        assertEquals(0, wide.minOf { it.x }); assertEquals(23, wide.maxOf { it.x })
        assertNotEquals(wide, processingFieldFrame(24, 3, 1200f))
        assertEquals(latticeFrame(600f), processingFieldFrame(3, 3, 600f))
        assertEquals(latticeStillFrame(), processingFieldFrame(3, 3, null))
        assertTrue(processingFieldFrame(24, 3, null).all { it.alpha == latticeRest(true) || it.alpha == 1f })
        assertTrue(processingFieldFrame(24, 3, null).any { it.alpha == 1f })
    }
}
