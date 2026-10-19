package com.aistudio.altermind.voice.ui

import android.provider.Settings
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.size
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.lifecycle.compose.LocalLifecycleOwner
import androidx.compose.ui.graphics.luminance
import kotlinx.coroutines.delay
import kotlin.math.hypot
import kotlin.math.max

/**
 * Native take on the free AICSS "Orbs" lattice, variant S1 "Thinking" (https://www.aicss.dev, free tier, used
 * in-app, not redistributed): a 3×3 dot grid where a round wave radiates from the centre, which leads a beat early. Colours follow the
 * designer-supplied CSS: light ink on a #1A1A1A pill in dark, dark ink on white in light.
 */
internal data class LatticeCell(val x: Int, val y: Int, val alpha: Float, val scale: Float)

/** Resting ink of an unlit cell; light ink on a dark surface reads dimmer at the same alpha, so dark uses more. */
internal fun latticeRest(dark: Boolean) = if (dark) .2f else .14f

private fun ease(t: Float) = t * t * (3 - 2 * t) // close to the source's symmetric cubic-bezier(.66,0,.34,1)

/** Each cell's ink (0..1) and scale at [ms]; pure, so the choreography is testable without a clock. */
internal fun latticeFrame(ms: Float, rest: Float = latticeRest(true)): List<LatticeCell> = buildList {
    for (y in 0..2) for (x in 0..2) {
        val dx = x - 1; val dy = y - 1
        val delay = hypot(dx.toFloat(), dy.toFloat()) * 700f - if (dx == 0 && dy == 0) 180f else 0f
        val p = (((ms - delay) / 1700f) % 1f + 1f) % 1f
        // Swells over 28% of the cycle, subsides by 56%, then rests: the gap between beats.
        val v = if (p < .28f) ease(p / .28f) else if (p < .56f) ease(1f - (p - .28f) / .28f) else 0f
        add(LatticeCell(x, y, rest + (1f - rest) * v, 1f + .18f * v))
    }
}

/** Reduced motion: the source's static frame, centre cell lit. */
internal fun latticeStillFrame(rest: Float = latticeRest(true)): List<LatticeCell> =
    (0..8).map { LatticeCell(it % 3, it / 3, if (it == 4) 1f else rest, 1f) }

/** Activity indicator: appears after [appearAfterMs], stops when hidden, static with animations off. */
/** Ink is the surrounding content colour (#F5F5F5 on dark, near-black on light), as the source's currentColor. */
/** With [accent], a cell's colour moves from [ink] towards [accent] as it lights (ALTER: white and yellow). */
@Composable fun ActivityOrb(modifier: Modifier = Modifier, ink: androidx.compose.ui.graphics.Color = androidx.compose.material3.LocalContentColor.current,
    accent: androidx.compose.ui.graphics.Color? = null, appearAfterMs: Long = 2000) {
    val context = LocalContext.current
    val lifecycle = LocalLifecycleOwner.current.lifecycle
    var active by remember { mutableStateOf(lifecycle.currentState.isAtLeast(Lifecycle.State.STARTED)) }
    DisposableEffect(lifecycle) {
        val observer = LifecycleEventObserver { _, _ -> active = lifecycle.currentState.isAtLeast(Lifecycle.State.STARTED) }
        lifecycle.addObserver(observer); onDispose { lifecycle.removeObserver(observer) }
    }
    var visible by remember { mutableStateOf(appearAfterMs <= 0) }
    LaunchedEffect(Unit) { if (!visible) { delay(appearAfterMs); visible = true } }
    if (!visible) { Spacer(modifier.size(20.dp)); return }
    val motion = remember { Settings.Global.getFloat(context.contentResolver, Settings.Global.ANIMATOR_DURATION_SCALE, 1f) > 0f }
    val clock = if (motion && active) {
        val value by rememberInfiniteTransition(label = "Activity orb")
            .animateFloat(0f, 1700f, infiniteRepeatable(tween(1700, easing = LinearEasing)), label = "Orb ms")
        value
    } else null
    Canvas(modifier.size(20.dp)) {
        val k = size.minDimension / 28f // the source's 28px stage
        val rest = latticeRest(ink.luminance() > .5f)
        (clock?.let { latticeFrame(it, rest) } ?: latticeStillFrame(rest)).forEach { cell ->
            val c = Offset((6.5f + 1.5f + cell.x * 6f) * k, (6.5f + 1.5f + cell.y * 6f) * k)
            val lit = ((cell.alpha - rest) / (1f - rest)).coerceIn(0f, 1f)
            val colour = accent?.let { androidx.compose.ui.graphics.lerp(ink, it, lit) } ?: ink
            drawCircle(colour.copy(alpha = cell.alpha), max(.6f, 1.5f * k * cell.scale), c)
        }
    }
}
