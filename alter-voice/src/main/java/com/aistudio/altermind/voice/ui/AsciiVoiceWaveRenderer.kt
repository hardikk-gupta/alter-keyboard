package com.aistudio.altermind.voice.ui

import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import android.graphics.Typeface
import kotlin.math.exp
import kotlin.math.roundToInt

/** Presentation only. Give each recording surface its own renderer and call draw on the UI thread. */
internal class AsciiVoiceWaveRenderer {
    private val paint = Paint(Paint.ANTI_ALIAS_FLAG or Paint.SUBPIXEL_TEXT_FLAG).apply {
        typeface = Typeface.MONOSPACE
        fontFeatureSettings = "liga=0"
    }
    private val heights = IntArray(MAX_COLUMNS)
    private val glyphs = Array(MAX_ROWS) { row ->
        CharArray(MAX_COLUMNS) { column -> glyph(column, row) }
    }

    /** Draw in local pixel coordinates. The caller owns the canvas origin and microphone. */
    fun draw(canvas: Canvas, width: Float, height: Float, density: Float, level: Float, ink: Int) {
        if (width <= 0f || height <= 0f || density <= 0f) return
        val intensity = if (level.isFinite()) level.coerceIn(0f, 1f) else 0f
        // Compact pills and tablet panels share one grid; bound work on very wide canvases.
        val fontSize = maxOf(4f * density, width / (MAX_COLUMNS * .75f)).coerceAtMost(height)
        paint.textSize = fontSize
        paint.color = ink
        val pitch = fontSize * .75f
        val columns = (width / pitch).toInt().coerceIn(1, MAX_COLUMNS)
        val rows = (height / fontSize).toInt().coerceIn(1, MAX_ROWS)
        val left = (width - columns * pitch) / 2f
        val baseline = height - paint.descent()
        for (column in 0 until columns) {
            val x = column.toFloat() / (columns - 1).coerceAtLeast(1)
            heights[column] = visibleRows(x, rows, intensity)
        }
        for (row in 0 until rows) {
            paint.alpha = (Color.alpha(ink) * (.40f + .40f * intensity + .15f * row / rows)).roundToInt()
            val y = baseline - row * fontSize
            for (column in 0 until columns) {
                if (row < heights[column]) canvas.drawText(glyphs[row], column, 1, left + column * pitch, y, paint)
            }
        }
    }

    companion object {
        private const val MAX_COLUMNS = 112
        private const val MAX_ROWS = 16
        private const val LETTERS = "abcdefghijklmnopqrstuvwxyzABCDEFGHIJKLMNOPQRSTUVWXYZ"
        private const val SYMBOLS = "?!\$%&*+=:;~^/|()<>[]@"

        internal fun glyph(column: Int, row: Int): Char {
            // Stable integer hash: a cell keeps its glyph across redraws, resize and sessions.
            var bits = 532 + column * 233 + row * 3847
            bits = (bits xor (bits ushr 16)) * 0x45d9f3b
            bits = (bits xor (bits ushr 16)) * 0x45d9f3b
            val value = (bits xor (bits ushr 16)) ushr 1
            val bucket = value % 1000
            if (bucket < 6) return '#'
            val pool = if (bucket < 710) LETTERS else SYMBOLS
            return pool[(value / 1000) % pool.length]
        }

        internal fun visibleRows(x: Float, rows: Int, intensity: Float): Int {
            val peak = (x - .45f) / .19f
            val shoulder = (x - .64f) / .14f
            val envelope = .83f * exp(-peak * peak) + .33f * exp(-shoulder * shoulder)
            return 1 + ((rows - 1) * intensity * envelope.coerceAtMost(1f)).roundToInt()
        }
    }
}
