package com.aistudio.altermind.voice

import com.aistudio.altermind.voice.ui.AsciiVoiceWaveRenderer
import org.junit.Assert.*
import org.junit.Test

class AsciiVoiceWaveTest {
    @Test fun quietSpeechLiftsTheCompactPillAndLouderLevelsStayDistinct() {
        fun height(rms: Float) = AsciiVoiceWaveRenderer.visibleRows(
            .45f, 9, AsciiVoiceWaveRenderer.displayIntensity(microphoneLevel(rms)))
        val peaks = listOf(0f, .00025f, .004f, .05f, .5f).map(::height)
        assertEquals(1, peaks.first())
        assertTrue(peaks[1] >= 3)
        assertTrue(peaks.zipWithNext().all { (low, high) -> low < high })
        assertEquals(1, height(1f / 32768f))
        assertEquals(1, height(Float.NaN))
    }

    @Test fun briefSoundAtTheEndOfTheBufferRetainsItsLevel() {
        val audio = FloatArray(1600)
        audio.fill(.004f, 1440, 1600)
        assertEquals(microphoneLevel(.004f), microphoneDisplayLevel(audio), .0001f)
        assertEquals(0f, microphoneDisplayLevel(FloatArray(1600)), 0f)
    }

    @Test fun glyphsAreStableAndHeightsFollowTheLevel() {
        fun grid() = (0 until 16).flatMap { row ->
            (0 until 112).map { column -> AsciiVoiceWaveRenderer.glyph(column, row) }
        }
        val glyphs = grid()
        assertEquals(glyphs, grid())
        assertTrue(glyphs.count { it == '#' } in 2..22)
        assertTrue(glyphs.count(Char::isLetter) in 1100..1400)
        for (rows in 1..16) for (column in 0 until 112) {
            val x = column / 111f
            val quiet = AsciiVoiceWaveRenderer.visibleRows(x, rows, 0f)
            val medium = AsciiVoiceWaveRenderer.visibleRows(x, rows, .25f)
            val loud = AsciiVoiceWaveRenderer.visibleRows(x, rows, 1f)
            assertEquals(1, quiet)
            assertTrue(medium in quiet..loud && loud <= rows)
        }
        assertTrue(AsciiVoiceWaveRenderer.visibleRows(.45f, 10, 1f) >
            AsciiVoiceWaveRenderer.visibleRows(.45f, 10, .25f))
    }
}
