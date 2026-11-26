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
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.lerp
import androidx.compose.ui.graphics.nativeCanvas
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.platform.LocalContext
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.compose.LocalLifecycleOwner
import kotlin.math.PI
import kotlin.math.abs
import kotlin.math.pow
import kotlin.math.sin

private const val PROCESSING_CYCLE_MS = 3000
private val PROCESSING_ROWS = listOf(
    "<xx/:x}*+=/;*><}/}:.>{><>",
    "=>+:}{;=}/+{;.<;}+x.>{+=>",
    "<={;/.x=*}{*}/{;:=/=.++:;"
).map(String::toCharArray)
private val PROCESSING_REST = Color(0xFF6C6C73)

private data class Burst(val startMs: Int, val column: Int, val row: Int)
private val PROCESSING_BURSTS = listOf(
    Burst(0, 10, 1), Burst(160, 13, 0), Burst(290, 16, 1), Burst(340, 12, 2),
    Burst(500, 20, 2), Burst(640, 22, 0), Burst(1030, 6, 2), Burst(1190, 3, 1),
    Burst(1280, 9, 1), Burst(1460, 13, 2), Burst(1660, 16, 0), Burst(2030, 21, 1),
    Burst(2230, 17, 2), Burst(2370, 14, 1), Burst(2540, 11, 0), Burst(2640, 7, 1)
)

internal data class ProcessingJump(val height: Float, val direction: Int)

internal fun processingJump(column: Int, row: Int, timeMs: Float?): ProcessingJump {
    if (timeMs == null) return ProcessingJump(0f, 0)
    var height = 0f
    var direction = 0
    for (burst in PROCESSING_BURSTS) {
        val distance = abs(column - burst.column) + 1.3f * abs(row - burst.row)
        if (distance > 2.3f) continue
        val age = (timeMs - burst.startMs - 45f * distance + PROCESSING_CYCLE_MS) % PROCESSING_CYCLE_MS
        if (age >= 360f) continue
        val jump = sin(PI * age / 360).pow(1.25).toFloat() / (1f + .72f * distance)
        if (jump > height) {
            height = jump
            direction = if (burst.column % 2 == 0) -1 else 1
        }
    }
    return ProcessingJump(height, direction)
}

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
        rememberInfiniteTransition(label = "Thinking field").animateFloat(0f, PROCESSING_CYCLE_MS.toFloat(),
            infiniteRepeatable(tween(PROCESSING_CYCLE_MS, easing = LinearEasing)), label = "ASCII bursts")
    } else null
    Canvas(modifier) {
        val stepX = size.width / PROCESSING_ROWS.first().size
        val stepY = size.height / 4f
        val unit = minOf(stepX, stepY)
        if (unit <= 0f) return@Canvas
        val canvas = drawContext.canvas.nativeCanvas
        paint.textSize = unit * 1.25f
        val baseline = -(paint.ascent() + paint.descent()) / 2f
        val time = clock?.value
        PROCESSING_ROWS.forEachIndexed { row, glyphs ->
            for (column in glyphs.indices) {
                val jump = processingJump(column, row, time)
                val light = (jump.height * 1.55f).coerceAtMost(1f)
                paint.color = lerp(PROCESSING_REST, AlterColors.AlterYellow, light).toArgb()
                val x = (column + .5f) * stepX + jump.direction * unit * .25f * jump.height
                val y = (row + 1.1f) * stepY - unit * .91f * jump.height + baseline
                canvas.drawText(glyphs, column, 1, x, y, paint)
            }
        }
    }
}
