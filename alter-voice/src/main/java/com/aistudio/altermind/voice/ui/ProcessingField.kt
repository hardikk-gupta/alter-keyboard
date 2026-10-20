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
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.lerp
import androidx.compose.ui.platform.LocalContext
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.compose.LocalLifecycleOwner
import kotlin.math.roundToInt

/** The original AICSS S1 orb choreography, expanded to a rectangular lattice without changing its beat. */
internal fun processingFieldFrame(columns: Int, rows: Int, ms: Float?): List<LatticeCell> =
    if (ms == null) latticeStillFrame(columns = columns, rows = rows)
    else latticeFrame(ms, columns = columns, rows = rows)

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
            val rest = latticeRest(true)
            val lit = ((cell.alpha - rest) / (1f - rest)).coerceIn(0f, 1f)
            drawCircle(lerp(AlterColors.OrbInk, AlterColors.AlterYellow, lit).copy(alpha = cell.alpha),
                minOf(pitch, stepX) * .25f * cell.scale,
                Offset((cell.x + .5f) * stepX, (cell.y + .5f) * pitch))
        }
    }
}
