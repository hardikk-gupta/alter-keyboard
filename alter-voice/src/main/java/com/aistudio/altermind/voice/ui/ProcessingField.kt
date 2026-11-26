package com.aistudio.altermind.voice.ui

import android.graphics.Paint
import android.graphics.Typeface
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.lerp
import androidx.compose.ui.graphics.nativeCanvas
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.platform.LocalContext
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.compose.LocalLifecycleOwner
import kotlin.math.roundToInt
import kotlin.math.sign

private val PROCESSING_GLYPHS = listOf(".:+*", ".-/x", ".+>#").map(String::toCharArray)

/** The original AICSS S1 orb choreography, expanded to a rectangular lattice without changing its beat. */
internal fun processingFieldFrame(columns: Int, rows: Int, ms: Float?): List<LatticeCell> =
    if (ms == null) latticeStillFrame(columns = columns, rows = rows)
    else latticeFrame(ms, columns = columns, rows = rows)

@Composable internal fun ProcessingField(modifier: Modifier = Modifier) {
    val context = LocalContext.current
    val motion = remember { animationsEnabled(context) }
    val lifecycle by LocalLifecycleOwner.current.lifecycle.currentStateFlow.collectAsState()
    val paint = remember {
        Paint(Paint.ANTI_ALIAS_FLAG or Paint.SUBPIXEL_TEXT_FLAG).apply {
            typeface = Typeface.MONOSPACE
            textAlign = Paint.Align.CENTER
        }
    }
    val clock = if (motion && lifecycle.isAtLeast(Lifecycle.State.STARTED)) {
        rememberInfiniteTransition(label = "Thinking field").animateFloat(0f, 1700f,
            infiniteRepeatable(tween(1700, easing = LinearEasing)), label = "Thinking wave")
    } else null
    Canvas(modifier) {
        val rows = 3
        val pitch = size.height / rows
        if (pitch <= 0f) return@Canvas
        val columns = (size.width / pitch).roundToInt().coerceAtLeast(3)
        val stepX = size.width / columns
        val cellSize = minOf(pitch, stepX)
        val canvas = drawContext.canvas.nativeCanvas
        val rest = latticeRest(true)
        val time = clock?.value
        processingFieldFrame(columns, rows, time).forEach { cell ->
            val lit = ((cell.alpha - rest) / (1f - rest)).coerceIn(0f, 1f)
            val jump = if (time == null) 0f else lit * lit * cellSize * .16f
            val outward = (cell.x - (columns - 1) / 2f).sign
            val rise = if (cell.y == 2) 1f else -1f
            val glyphs = PROCESSING_GLYPHS[(cell.x + cell.y) % PROCESSING_GLYPHS.size]
            val glyph = (lit * glyphs.lastIndex).roundToInt()
            paint.textSize = cellSize * .72f * cell.scale
            paint.color = lerp(AlterColors.OrbInk, AlterColors.AlterYellow, lit).copy(alpha = cell.alpha).toArgb()
            val x = (cell.x + .5f) * stepX + outward * jump
            val y = (cell.y + .5f) * pitch + rise * jump - (paint.ascent() + paint.descent()) / 2f
            canvas.drawText(glyphs, glyph, 1, x, y, paint)
        }
    }
}
