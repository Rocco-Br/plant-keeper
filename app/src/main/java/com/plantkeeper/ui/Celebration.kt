package com.plantkeeper.ui

import androidx.activity.compose.BackHandler
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.drawscope.rotate
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.semantics.clearAndSetSemantics
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlin.math.sin

/**
 * Feestelijk moment over het hele scherm: het potje groeit op, er dwarrelen blaadjes, de [title] springt
 * letter voor letter in beeld en de [subtitle] komt erachteraan. Gaat na [durationMs] vanzelf verder;
 * tikken of terug slaat het over. Gebruikt na een nieuwe zaai en bij de eerste begroeting.
 */
@OptIn(ExperimentalLayoutApi::class)
@Composable
fun Celebration(title: String, subtitle: String, durationMs: Int = 3200, onDone: () -> Unit) {
    var finished by remember { mutableStateOf(false) }
    val finish = { if (!finished) { finished = true; onDone() } }
    var showText by remember { mutableStateOf(false) }
    val fall = remember { Animatable(0f) }
    LaunchedEffect(Unit) {
        launch { fall.animateTo(1f, tween(durationMs, easing = LinearEasing)) }
        delay(TextDelayMs); showText = true
        delay(durationMs - TextDelayMs); finish()
    }
    BackHandler { finish() }
    val c = pk
    val confetti = listOf(c.sun, c.onHero, pk.outdoorContainer, pk.indoor)
    Box(
        Modifier
            .fillMaxSize()
            .background(Brush.verticalGradient(c.hero))
            .clickable(interactionSource = null, indication = null, onClickLabel = "Overslaan") { finish() },
        contentAlignment = Alignment.Center,
    ) {
        // Blaadjes die van boven naar beneden dwarrelen, elk met een eigen tempo, plek en draai.
        Canvas(Modifier.fillMaxSize()) {
            val leaf = Size(9.dp.toPx(), 5.dp.toPx())
            for (i in 0 until 18) {
                val speed = 0.7f + (i * 37 % 10) / 20f
                val t = (fall.value * speed * 1.3f - (i % 6) * 0.08f).coerceIn(0f, 1f)
                if (t <= 0f || t >= 1f) continue
                val x = size.width * ((i * 53 % 100) / 100f) + sin(t * 9f + i) * 14.dp.toPx()
                val y = -20f + t * (size.height + 40f)
                rotate(t * 540f * (if (i % 2 == 0) 1 else -1) + i * 20f, pivot = Offset(x, y)) {
                    drawOval(confetti[i % confetti.size].copy(alpha = 0.85f), Offset(x - leaf.width / 2, y - leaf.height / 2), leaf)
                }
            }
        }
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            modifier = Modifier.padding(horizontal = Space.xl).semantics(mergeDescendants = true) {},
        ) {
            Mascot(Modifier.size(220.dp), intro = true, onDark = true)
            // De titel springt letter voor letter in; voor TalkBack blijft het één woord.
            // Letters per woord gegroepeerd, zodat een lange naam alleen tussen woorden afbreekt.
            FlowRow(
                horizontalArrangement = Arrangement.spacedBy(Space.m, Alignment.CenterHorizontally),
                modifier = Modifier.clearAndSetSemantics { contentDescription = title },
            ) {
                var index = 0
                title.split(' ').forEach { word ->
                    Row { word.forEach { ch -> PopLetter(ch, index++, showText, MaterialTheme.typography.displayMedium.copy(color = c.onHero)) } }
                }
            }
            AnimatedVisibility(showText, enter = Motion.enterUp) {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Text(subtitle, style = MaterialTheme.typography.titleLarge, color = c.onHero.copy(alpha = 0.85f), textAlign = TextAlign.Center)
                    Spacer(Modifier.height(Space.xl))
                    Text("Tik om verder te gaan", style = MaterialTheme.typography.bodySmall, color = c.onHero.copy(alpha = 0.6f))
                }
            }
        }
    }
}

/** Eén letter die met een veertje omhoog springt, na de letters ervoor. */
@Composable
private fun PopLetter(ch: Char, index: Int, shown: Boolean, style: TextStyle) {
    val p = remember { Animatable(0f) }
    LaunchedEffect(shown) {
        if (shown) { delay(Motion.STAGGER * index); p.animateTo(1f, Motion.pop()) }
    }
    Text(
        ch.toString(), style = style,
        modifier = Modifier.graphicsLayer {
            alpha = p.value.coerceIn(0f, 1f)
            translationY = (1f - p.value) * 28.dp.toPx()
            scaleX = 0.6f + 0.4f * p.value; scaleY = scaleX
        },
    )
}

private const val TextDelayMs = 900L
