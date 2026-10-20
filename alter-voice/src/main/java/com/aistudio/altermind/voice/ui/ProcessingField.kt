package com.aistudio.altermind.voice.ui

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
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.lerp
import androidx.compose.ui.platform.LocalContext
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.compose.LocalLifecycleOwner
import kotlin.math.abs
import kotlin.math.hypot
import kotlin.math.roundToInt

/** A wave of thinking cells spans the entire rectangle, with a still centre band for reduced motion. */
internal fun processingFieldFrame(columns: Int, rows: Int, ms: Float?): List<LatticeCell> = buildList {
    for (y in 0 until rows) for (x in 0 until columns) {
        val dx = x - (columns - 1) / 2f
        val dy = y - (rows - 1) / 2f
        val p = if (ms == null) 0f else ((ms - hypot(dx, dy) * 110f) / 1700f % 1f + 1f) % 1f
        val pulse = if (ms == null) { if (abs(dx) < 1f) 1f else 0f }
            else ((1f - abs(p - .28f) / .28f).coerceAtLeast(0f)).let { it * it * (3f - 2f * it) }
        add(LatticeCell(x, y, .25f + .75f * pulse, 1f + .18f * pulse))
    }
}

@Composable internal fun ProcessingField(modifier: Modifier = Modifier) {
    val context = LocalContext.current
    val motion = remember { animationsEnabled(context) }
    val lifecycle by LocalLifecycleOwner.current.lifecycle.currentStateFlow.collectAsState()
    val clock = if (motion && lifecycle.isAtLeast(Lifecycle.State.STARTED)) {
        rememberInfiniteTransition(label = "Thinking field").animateFloat(0f, 1700f,
            infiniteRepeatable(tween(1700, easing = LinearEasing)), label = "Thinking wave").value
    } else null
    Canvas(modifier) {
        val rows = 3
        val pitch = size.height / rows
        if (pitch <= 0f) return@Canvas
        val columns = (size.width / pitch).roundToInt().coerceAtLeast(3)
        val stepX = size.width / columns
        processingFieldFrame(columns, rows, clock).forEach { cell ->
            val lit = (cell.alpha - .25f) / .75f
            val side = minOf(pitch, stepX) * .48f * cell.scale
            drawRoundRect(lerp(AlterColors.MutedText, AlterColors.AlterYellow, lit).copy(alpha = cell.alpha),
                Offset((cell.x + .5f) * stepX - side / 2, (cell.y + .5f) * pitch - side / 2),
                Size(side, side), CornerRadius(side * .25f))
        }
    }
}
