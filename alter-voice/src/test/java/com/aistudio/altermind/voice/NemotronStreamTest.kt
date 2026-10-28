package com.aistudio.altermind.voice

import kotlinx.coroutines.*
import kotlinx.coroutines.test.runTest
import org.junit.Assert.*
import org.junit.Test

class NemotronStreamTest {
    @Test fun feedsEverySampleExactlyOnceAndFlushesTheFinalTail() = runTest {
        val chunks = ArrayDeque(listOf(FloatArray(3501) { it / 4000f }, floatArrayOf(.9f)))
        val heard = mutableListOf<Float>()
        val updates = mutableListOf<String>()
        var ends = 0
        var flushes = 0
        consumeNemotronStream({ chunks.removeFirstOrNull() }, { updates += it }, {
            assertTrue(it.size <= 1600)
            heard.addAll(it.toList())
            "tentative".encodeToByteArray()
        }, { flushes++; "मैं घर जा रहा हूँ, then I'll call you.".encodeToByteArray() }, { ends++ })
        assertEquals(3502, heard.size)
        assertEquals(3500 / 4000f, heard[3500], 0f)
        assertEquals(.9f, heard.last(), 0f)
        assertEquals("मैं घर जा रहा हूँ, then I'll call you.", updates.last())
        assertEquals(1, flushes); assertEquals(1, ends)
    }

    @Test fun nativeFailureDoesNotTurnATentativeResultIntoACompletedTranscript() = runTest {
        var read = false
        var ended = false
        val updates = mutableListOf<String>()
        val result = runCatching {
            consumeNemotronStream({ if (read) null else FloatArray(1600).also { read = true } }, { updates += it },
                { "tentative".encodeToByteArray() }, { null }, { ended = true })
        }
        assertTrue(result.isFailure)
        assertEquals(listOf("tentative"), updates)
        assertTrue(ended)
    }

    @Test fun cancellationEndsTheStreamWithoutFlushingOrInsertingLateText() = runTest {
        var ended = false
        var flushed = false
        val task = launch {
            consumeNemotronStream({ awaitCancellation() }, { error("No result expected") }, { null },
                { flushed = true; null }, { ended = true })
        }
        testScheduler.runCurrent()
        task.cancelAndJoin()
        assertTrue(ended); assertFalse(flushed)
        assertEquals("auto", nemotronLanguage("auto"))
        assertEquals("en-US", nemotronLanguage("en-IN"))
        assertEquals("hi-IN", nemotronLanguage("hi-IN"))
    }
}
