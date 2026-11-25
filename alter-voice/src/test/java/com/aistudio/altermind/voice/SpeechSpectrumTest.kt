package com.aistudio.altermind.voice

import org.junit.Assert.*
import org.junit.Test
import kotlin.math.PI
import kotlin.math.sin

class SpeechSpectrumTest {
    private fun tone(hz: Int, amplitude: Float = .4f) =
        FloatArray(256) { amplitude * sin(2 * PI * hz * it / 16000).toFloat() }
    private fun strongest(frame: SpeechSpectrum, column: Int) =
        (0 until SpeechSpectrum.BANDS).maxBy { frame.energy(column, it) }

    @Test fun equalVolumeLowAndHighTonesLightDifferentBands() {
        val analyzer = SpectrumAnalyzer()
        val low = analyzer.accept(tone(250))!!
        val high = analyzer.accept(tone(4000))!!
        assertEquals(3, strongest(low, 63))
        assertEquals(13, strongest(high, 63))
        assertEquals(low.energy(63, 3), high.energy(62, 3))
        assertTrue(high.energy(63, 3) < high.energy(63, 13))
    }

    @Test fun softTonesRemainVisibleAndLoudTonesAreBrighter() {
        fun energy(amplitude: Float) = SpectrumAnalyzer().accept(tone(250, amplitude))!!.energy(63, 3)
        assertTrue(energy(.0003f) in 1 until energy(.04f))
        assertTrue(energy(.04f) < energy(.4f))
    }

    @Test fun partialReadsRetainSamplesAndSilenceExpiresHistory() {
        val samples = tone(1000)
        val analyzer = SpectrumAnalyzer()
        assertNull(analyzer.accept(samples.copyOfRange(0, 73)))
        val split = analyzer.accept(samples.copyOfRange(73, 256))!!
        val whole = SpectrumAnalyzer().accept(samples)!!
        for (band in 0 until SpeechSpectrum.BANDS) assertEquals(whole.energy(63, band), split.energy(63, band))
        val silent = analyzer.accept(FloatArray(256 * 64))!!
        for (c in 0 until 64) for (b in 0 until 16) assertEquals(0, silent.energy(c, b))
        assertTrue(split.energy(63, strongest(split, 63)) > 0)
    }
}
