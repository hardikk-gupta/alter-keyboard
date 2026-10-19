package com.aistudio.altermind.voice.ui

import android.provider.Settings
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Icon
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.StrokeJoin
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.graphics.vector.addPathNodes
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.dp
import com.aistudio.altermind.voice.R
import kotlin.math.PI
import kotlin.math.sin

/** ALTER design tokens used by the voice surfaces (ui/theme/Color.kt in the ALTER app). */
object AlterColors {
    val PitchBlack = Color(0xFF000000)
    val SlateGrayCard = Color(0xFF18181B)
    val RaisedSurface = Color(0xFF262626)
    val LightText = Color(0xFFECECEC)
    val MutedText = Color(0xFFA1A1AA)
    val DisabledText = Color(0xFF71717A)
    val AlterYellow = Color(0xFFFFBD00)
    val ErrorRed = Color(0xFFEF4444)
    val OrbInk = Color(0xFFF5F5F5)
}

enum class PillState { Idle, Listening, Thinking, Error, Disabled }

/** Lucide icons (ISC license) used on the pill bar, from the same path data as ALTER's ui/LucideIcons.kt set. */
object LucidePill {
    private fun icon(name: String, vararg paths: String) = ImageVector.Builder("Lucide.$name", 24.dp, 24.dp, 24f, 24f).apply {
        paths.forEach {
            addPath(addPathNodes(it), stroke = SolidColor(Color.White), strokeLineWidth = 1.5f,
                strokeLineCap = StrokeCap.Round, strokeLineJoin = StrokeJoin.Round)
        }
    }.build()
    val Mic = icon("Mic", "M12 2a3 3 0 0 0-3 3v7a3 3 0 0 0 6 0V5a3 3 0 0 0-3-3Z", "M19 10v2a7 7 0 0 1-14 0v-2", "M12 19v3")
    val MicOff = icon("MicOff", "M2 2l20 20", "M18.89 13.23A7.12 7.12 0 0 0 19 12v-2", "M5 10v2a7 7 0 0 0 12 5",
        "M15 9.34V5a3 3 0 0 0-5.68-1.33", "M9 9v3a3 3 0 0 0 5.12 2.12", "M12 19v3")
    val Keyboard = icon("Keyboard", "M4 4h16a2 2 0 0 1 2 2v12a2 2 0 0 1-2 2H4a2 2 0 0 1-2-2V6a2 2 0 0 1 2-2Z",
        "M6 8h.01", "M10 8h.01", "M14 8h.01", "M18 8h.01", "M8 12h.01", "M12 12h.01", "M16 12h.01", "M7 16h10")
    val ChevronDown = icon("ChevronDown", "M6 9l6 6 6-6")
    val TriangleAlert = icon("TriangleAlert", "M21.73 18l-8-14a2 2 0 0 0-3.48 0l-8 14A2 2 0 0 0 4 21h16a2 2 0 0 0 1.73-3Z", "M12 9v4", "M12 17h.01")
}

internal fun animationsEnabled(context: android.content.Context) =
    Settings.Global.getFloat(context.contentResolver, Settings.Global.ANIMATOR_DURATION_SCALE, 1f) > 0f

/** Decorative carrier follows real RMS amplitude. With no audio the line is flat (ALTER's voiceWavePoints). */
internal fun voiceWavePoints(level: Float, phase: Float, strand: Int): List<Offset> = (0..72).map { i ->
    val x = i / 72f
    val envelope = sin(PI * x).toFloat()
    val carrier = sin(x * PI * (3.5 + strand * .4) - phase + strand * .9).toFloat()
    Offset(x, .5f + level.coerceIn(0f, 1f) * envelope * carrier * (.35f - strand * .045f))
}

/** One AlterYellow line driven by the real microphone level; a still frame when animations are off. */
@Composable fun VoiceWave(level: Float, modifier: Modifier = Modifier) {
    val context = LocalContext.current
    val motion = remember { animationsEnabled(context) }
    val live by animateFloatAsState(level.coerceIn(0f, 1f), if (motion) tween(80) else tween(0), label = "wave-level")
    val phase = if (motion) rememberInfiniteTransition(label = "wave").animateFloat(0f, (2 * PI).toFloat(),
        infiniteRepeatable(tween(1200, easing = LinearEasing)), label = "wave-phase").value else 0f
    Canvas(modifier) {
        val points = voiceWavePoints(live, phase, 0)
        val path = Path()
        points.forEachIndexed { i, p ->
            val x = p.x * size.width; val y = p.y * size.height
            if (i == 0) path.moveTo(x, y) else path.lineTo(x, y)
        }
        drawPath(path, AlterColors.AlterYellow, style = Stroke(2.dp.toPx(), cap = StrokeCap.Round, join = StrokeJoin.Round))
    }
}

/**
 * ALTER's mic pill: idle (flat-eyes mark + mic), listening (live wave), thinking (S1 orb), error (red triangle,
 * no text). Pill body SlateGrayCard, 12dp corners, no outline, no glass or gradients. [description] is read by
 * TalkBack, so the error triangle still has its message.
 */
@Composable fun AlterVoicePill(state: PillState, level: Float, onTap: () -> Unit, modifier: Modifier = Modifier,
    description: String = "") {
    Box(modifier
        .height(40.dp)
        .width(152.dp)
        .clip(RoundedCornerShape(12.dp))
        .background(AlterColors.SlateGrayCard)
        .clickable(enabled = state != PillState.Disabled, role = Role.Button, onClick = onTap)
        .semantics { contentDescription = description },
        contentAlignment = Alignment.Center) {
        when (state) {
            PillState.Idle, PillState.Disabled -> Row(verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                val ink = if (state == PillState.Disabled) AlterColors.DisabledText else AlterColors.OrbInk
                Icon(painterResource(R.drawable.ic_alter_ai_doodle_closed), null, Modifier.size(22.dp), tint = ink)
                Icon(if (state == PillState.Disabled) LucidePill.MicOff else LucidePill.Mic, null, Modifier.size(20.dp), tint = ink)
            }
            PillState.Listening -> VoiceWave(level, Modifier.fillMaxWidth().height(28.dp).padding(horizontal = 14.dp))
            PillState.Thinking -> ActivityOrb(ink = AlterColors.OrbInk, appearAfterMs = 0)
            PillState.Error -> Icon(LucidePill.TriangleAlert, null, Modifier.size(22.dp), tint = AlterColors.ErrorRed)
        }
    }
}

/** Touch target of at least 48dp around a 24dp icon. */
@Composable fun PillBarButton(icon: ImageVector, description: String, onClick: () -> Unit, modifier: Modifier = Modifier) {
    Box(modifier.size(48.dp).clip(RoundedCornerShape(12.dp)).clickable(role = Role.Button, onClick = onClick)
        .semantics { contentDescription = description }, contentAlignment = Alignment.Center) {
        Icon(icon, null, Modifier.size(24.dp), tint = AlterColors.LightText)
    }
}
