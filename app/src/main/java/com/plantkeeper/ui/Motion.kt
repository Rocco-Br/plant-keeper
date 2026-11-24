package com.plantkeeper.ui

import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.spring
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsPressedAsState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.unit.dp
import kotlinx.coroutines.delay

// Gedeelde bewegingen, zodat de hele app hetzelfde ritme heeft: zachte veren, niets abrupts.

/**
 * Laat een element de eerste keer zacht omhoog en in beeld komen, met een kleine vertraging per [index]
 * zodat een lijst als een golfje binnenkomt. Onthoudt dat het al getoond is: terugscrollen of terugkeren
 * naar een tab speelt het niet opnieuw af.
 */
@Composable
fun Modifier.appear(index: Int = 0): Modifier {
    var shown by rememberSaveable { mutableStateOf(false) }
    val progress = remember { Animatable(if (shown) 1f else 0f) }
    LaunchedEffect(Unit) {
        if (!shown) {
            delay(45L * index.coerceIn(0, 8))
            progress.animateTo(1f, spring(dampingRatio = 0.82f, stiffness = 260f))
            shown = true
        }
    }
    return this.graphicsLayer {
        val p = progress.value
        alpha = p.coerceIn(0f, 1f)
        translationY = (1f - p) * 28.dp.toPx()
    }
}

/** Geeft een beetje mee onder je vinger: schaalt licht in zolang het element ingedrukt is. */
@Composable
fun Modifier.pressScale(interaction: MutableInteractionSource, pressed: Float = 0.965f): Modifier {
    val isPressed by interaction.collectIsPressedAsState()
    val scale by animateFloatAsState(if (isPressed) pressed else 1f, spring(dampingRatio = 0.6f, stiffness = 700f), label = "indrukken")
    return this.graphicsLayer { scaleX = scale; scaleY = scale }
}
