package com.aistudio.altermind.voice

import kotlinx.coroutines.*
import kotlinx.coroutines.test.runTest
import org.junit.Assert.*
import org.junit.Test

class MoonshineStreamTest {
    @Test fun feedsEverySampleAndStopsAndFlushesExactlyOnce() = runTest {
        val audio = ArrayDeque(listOf(FloatArray(1600) { .1f }, FloatArray(7501) { .2f }, floatArrayOf(.3f)))
        val heard = mutableListOf<Float>(); val updates = mutableListOf<String>()
        var starts = 0; var stops = 0; var frees = 0; var finals = 0
        consumeMoonshineStream({ audio.removeFirstOrNull() }, { updates += it }, { starts++ }, { heard += it.toList() },
            { force -> if (force) { finals++; "Complete final tail" } else "Live preview" }, { stops++ }, { frees++ })
        assertEquals(9102, heard.size); assertEquals(.3f, heard.last(), 0f)
        assertEquals(listOf("Live preview", "Complete final tail"), updates)
        assertEquals(1, starts); assertEquals(1, stops); assertEquals(1, frees); assertEquals(1, finals)
    }
    @Test fun feedFailurePreservesItsReasonAndFreesTheStreamWithoutAFalseFinal() = runTest {
        var stopped = 0; var freed = 0
        val error = runCatching {
            consumeMoonshineStream({ floatArrayOf(.1f) }, { error("No final expected") }, {},
                { error("Moonshine could not accept audio: busy (-4)") }, { error("No decode expected") }, { stopped++ }, { freed++ })
        }.exceptionOrNull()
        assertTrue(error!!.message!!.contains("accept audio"))
        assertEquals(1, stopped); assertEquals(1, freed)
    }
    @Test fun startAndFinalStopFailuresStillFreeWithoutDuplicateStop() = runTest {
        for (failStart in listOf(true, false)) {
            var stopped = 0; var freed = 0
            val failed = runCatching {
                consumeMoonshineStream({ null }, { error("No output expected") }, { if (failStart) error("start failed") }, {},
                    { error("No decode expected") }, { stopped++; error("stop failed") }, { freed++ })
            }
            assertTrue(failed.isFailure); assertEquals(if (failStart) 0 else 1, stopped); assertEquals(1, freed)
        }
    }
    @Test fun cancellationDoesNotFlushOrPublishLateText() = runTest {
        var stopped = 0; var freed = 0
        val job = launch {
            consumeMoonshineStream({ awaitCancellation() }, { error("No output") }, {}, {}, { error("No flush") }, { stopped++ }, { freed++ })
        }
        testScheduler.runCurrent(); job.cancelAndJoin()
        assertEquals(1, stopped); assertEquals(1, freed)
    }
}
