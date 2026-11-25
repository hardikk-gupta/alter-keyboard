package com.aistudio.altermind.voice

import com.aistudio.altermind.voice.ui.AsciiVoiceWaveRenderer
import org.junit.Assert.*
import org.junit.Test

class AsciiVoiceWaveTest {
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
