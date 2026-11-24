package com.plantkeeper.ui

import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.FastOutLinearInEasing
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.keyframes
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.drawscope.rotate
import androidx.compose.ui.graphics.drawscope.scale
import androidx.compose.ui.graphics.drawscope.translate
import androidx.compose.ui.graphics.drawscope.withTransform
import androidx.compose.ui.graphics.vector.PathParser
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlin.math.sin

/** Hoe de mascotte kijkt: blij (standaard), slaperig (niets te doen) of nieuwsgierig (niets gevonden). */
enum class Mood { HAPPY, SLEEPY, CURIOUS }

private fun path(d: String) = PathParser().parsePathString(d).toPath()
private val POT_BODY by lazy { path("M41,66 H67 L64,84 Q63.5,86 61,86 H47 Q44.5,86 44,84 Z") }
private val LEAF_LEFT by lazy { path("M54,52 C48,54 40,51 39,43 C46,41 53,44 54,52 Z") }
private val LEAF_RIGHT by lazy { path("M54,47 C55,39 62,34 70,35 C70,43 63,48 54,47 Z") }
private val SMILE by lazy { path("M51.5,80 C53,82 55,82 56.5,80") }
private val SLEEPY_EYES by lazy { path("M48.3,75 Q50,77.2 51.7,75 M56.3,75 Q58,77.2 59.7,75") }
private val DROP by lazy { path("M54,24 C57,29 60,32 60,35 C60,38.5 57.3,41 54,41 C50.7,41 48,38.5 48,35 C48,32 51,29 54,24 Z") }
private val ZED by lazy { path("M0,0 H4.4 L0,4.4 H4.4") }

private val CREAM = Color(0xFFF3F8EC)
private val LEAF = Color(0xFFB5D98A)
private val INK = Color(0xFF4A2412)

/**
 * De mascotte uit het app-icoon: een terracotta potje met gezichtje en een plantje.
 * Leeft altijd een beetje: deint, wiegt en knippert. Met [intro] speelt eerst de opstartreeks
 * (druppel valt, potje veert, plantje schiet omhoog). Tekent in de 108-eenheden van het icoon.
 */
@Composable
fun Mascot(modifier: Modifier = Modifier, mood: Mood = Mood.HAPPY, intro: Boolean = false, onDark: Boolean = isSystemInDarkTheme()) {
    // Op een donkere achtergrond een crème plantje, op een lichte een groen plantje.
    val stem = if (onDark) CREAM else Color(0xFF2C6A48)
    val leafL = if (onDark) CREAM else Color(0xFF4F9A5E)
    val leafR = if (onDark) LEAF else Color(0xFF6DBE74)
    val start = if (intro) 0f else 1f
    val drop = remember { Animatable(start) }
    val squash = remember { Animatable(start) }
    val grow = remember { Animatable(start) }
    val glow = remember { Animatable(start) }
    if (intro) {
        LaunchedEffect(Unit) {
            launch { drop.animateTo(1f, tween(460, easing = FastOutLinearInEasing)) }
            delay(400)
            launch { squash.animateTo(1f, tween(380)) }
            delay(150)
            launch { grow.animateTo(1f, spring(dampingRatio = 0.4f, stiffness = 230f)) }
            delay(250)
            glow.animateTo(1f, tween(500))
        }
    }

    val loop = rememberInfiniteTransition(label = "mascotte")
    val sleepy = mood == Mood.SLEEPY
    val sway by loop.animateFloat(
        -4f, 4f, infiniteRepeatable(tween(if (sleepy) 3400 else 2400, easing = FastOutSlowInEasing), RepeatMode.Reverse), label = "wiegen",
    )
    val bob by loop.animateFloat(
        0f, 1f, infiniteRepeatable(tween(if (sleepy) 2600 else 1700, easing = FastOutSlowInEasing), RepeatMode.Reverse), label = "deinen",
    )
    val blink by loop.animateFloat(
        1f, 1f,
        infiniteRepeatable(keyframes { durationMillis = 3800; 1f at 3400; 0.1f at 3500; 1f at 3600 }),
        label = "knipperen",
    )
    val look by loop.animateFloat(
        -1f, -1f,
        infiniteRepeatable(keyframes { durationMillis = 4200; -1f at 1400; 1f at 1800; 1f at 3600; -1f at 4000 }),
        label = "rondkijken",
    )
    val zz by loop.animateFloat(0f, 1f, infiniteRepeatable(tween(2400, easing = LinearEasing)), label = "slapen")

    Canvas(modifier) {
        val unit = size.minDimension / 108f
        withTransform({ scale(unit, unit, Offset.Zero) }) {
            drawCircle(LEAF.copy(alpha = 0.13f * glow.value), radius = 27f, center = Offset(54f, 54f))
            translate(top = -2f * bob) {
                drawPlant(grow.value, sway, stem, leafL, leafR)
                val bump = sin(Math.PI * squash.value).toFloat()
                scale(1f + 0.09f * bump, 1f - 0.14f * bump, pivot = Offset(54f, 86f)) {
                    drawPot()
                    drawFace(mood, blink, look)
                }
            }
            if (sleepy) drawZeds(zz, stem)
            if (drop.value < 1f) {
                val fade = if (drop.value < 0.8f) 1f else (1f - drop.value) / 0.2f
                translate(top = 22f * drop.value) { drawPath(DROP, Color(0xFF5AA9E0).copy(alpha = fade)) }
            }
        }
    }
}

private fun DrawScope.drawPlant(grow: Float, sway: Float, stem: Color, leafL: Color, leafR: Color) {
    scale(1f, grow, pivot = Offset(54f, 64f)) {
        rotate(sway * grow.coerceIn(0f, 1f), pivot = Offset(54f, 64f)) {
            drawLine(stem, Offset(54f, 64f), Offset(54f, 44f), strokeWidth = 4f, cap = StrokeCap.Round)
            drawPath(LEAF_LEFT, leafL)
            drawPath(LEAF_RIGHT, leafR)
        }
    }
}

private fun DrawScope.drawPot() {
    drawPath(POT_BODY, Color(0xFFE07B4C))
    drawRoundRect(Color(0xFFC9653A), Offset(38f, 60f), Size(32f, 8f), CornerRadius(3f))
    drawLine(Color.White.copy(alpha = 0.35f), Offset(47f, 71f), Offset(48.3f, 80f), strokeWidth = 2f, cap = StrokeCap.Round)
    drawOval(Color(0xBFFFBEAA), Offset(44.4f, 77.5f), Size(5.2f, 3f))
    drawOval(Color(0xBFFFBEAA), Offset(58.4f, 77.5f), Size(5.2f, 3f))
}

private fun DrawScope.drawFace(mood: Mood, blink: Float, look: Float) {
    when (mood) {
        Mood.SLEEPY -> {
            drawPath(SLEEPY_EYES, INK, style = Stroke(width = 1.3f, cap = StrokeCap.Round))
            drawLine(INK, Offset(53f, 80.6f), Offset(55f, 80.6f), strokeWidth = 1.3f, cap = StrokeCap.Round)
        }
        Mood.CURIOUS -> {
            val dx = 1.1f * look
            scale(1f, blink, pivot = Offset(54f, 75f)) {
                drawOval(INK, Offset(48.3f + dx, 72.2f), Size(3.4f, 4.6f))
                drawOval(INK, Offset(56.3f + dx, 72.2f), Size(3.4f, 4.6f))
            }
            drawCircle(INK, radius = 1.3f, center = Offset(54f + dx * 0.5f, 81f), style = Stroke(width = 1.2f))
        }
        Mood.HAPPY -> {
            scale(1f, blink, pivot = Offset(54f, 75f)) {
                drawOval(INK, Offset(48.3f, 72.7f), Size(3.4f, 4.6f))
                drawOval(INK, Offset(56.3f, 72.7f), Size(3.4f, 4.6f))
            }
            drawPath(SMILE, INK, style = Stroke(width = 1.4f, cap = StrokeCap.Round))
        }
    }
}

/** Twee z'tjes die schuin omhoog zweven en vervagen. */
private fun DrawScope.drawZeds(t: Float, color: Color) {
    for (i in 0..1) {
        val p = (t + i * 0.5f) % 1f
        val s = 0.7f + 0.5f * p
        translate(left = 66f + 8f * p + i * 3f, top = 56f - 18f * p - i * 4f) {
            scale(s, s, pivot = Offset.Zero) {
                drawPath(ZED, color.copy(alpha = (1f - p) * 0.9f), style = Stroke(width = 1.4f, cap = StrokeCap.Round))
            }
        }
    }
}
