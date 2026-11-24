package com.plantkeeper.ui

import androidx.compose.runtime.getValue
import androidx.compose.runtime.setValue
import androidx.compose.animation.AnimatedContentTransitionScope
import androidx.compose.animation.EnterTransition
import androidx.compose.animation.ExitTransition
import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.FiniteAnimationSpec
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.animation.expandVertically
import androidx.compose.animation.fadeIn
import androidx.compose.animation.scaleIn
import androidx.compose.animation.scaleOut
import androidx.compose.animation.slideOutVertically
import androidx.compose.animation.shrinkVertically
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInHorizontally
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutHorizontally
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsPressedAsState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.unit.dp
import androidx.navigation.NavBackStackEntry
import kotlinx.coroutines.delay

/**
 * Alle beweging van de app op één plek, zodat alles hetzelfde ritme heeft: zachte veren, korte fades, niets abrupts.
 * Schermen gebruiken alleen de specs en hulpjes hieronder, nooit eigen duren of veren.
 */
object Motion {
    const val SHORT = 150      // wegvagen, kleine wissels
    const val MEDIUM = 250     // invagen, kleurwissels
    const val LONG = 500       // tellers, tekst die binnenkomt
    const val REVEAL = 900     // grafieken die zich tekenen
    const val STAGGER = 45L    // vertraging per item in een lijst

    /** Standaard tween met de app-brede easing. */
    fun <T> tween(ms: Int = MEDIUM, delay: Int = 0): FiniteAnimationSpec<T> = tween(ms, delay, FastOutSlowInEasing)

    /** Elementen die binnenkomen: zacht, geen doorschieten. */
    fun <T> gentle(): FiniteAnimationSpec<T> = spring(dampingRatio = 0.82f, stiffness = 260f)

    /** Reactie op aanraken: snel en een tikje veerkrachtig. */
    fun <T> snappy(): FiniteAnimationSpec<T> = spring(dampingRatio = 0.6f, stiffness = 700f)

    /** Iets dat in beeld springt (knop, label): speels doorschieten. */
    fun <T> pop(): FiniteAnimationSpec<T> = spring(dampingRatio = 0.55f, stiffness = 320f)

    /** Tekstblok dat van onderen invaagt (welkomstscherm, begroeting). */
    val enterUp: EnterTransition = fadeIn(tween(LONG)) + slideInVertically(gentle()) { it / 3 }
    /** Inklappen en weer uitklappen (bijvoorbeeld als het toetsenbord opent). */
    val expand: EnterTransition = fadeIn(tween(MEDIUM)) + expandVertically(gentle())
    val collapse: ExitTransition = fadeOut(tween(SHORT)) + shrinkVertically(tween(MEDIUM))
    val enterSide: EnterTransition = fadeIn(tween(LONG)) + slideInHorizontally(gentle()) { -it / 5 }

    // Navbalk: komt van onderen nadat het nieuwe scherm al aan het invagen is.
    val barEnter: EnterTransition = slideInVertically(tween(LONG, delay = MEDIUM)) { it } + fadeIn(tween(MEDIUM, delay = MEDIUM))
    val barExit: ExitTransition = slideOutVertically(tween(MEDIUM)) { it } + fadeOut(tween(SHORT))

    // Eenmalige begroeting na het welkomstscherm: welkom zwelt zacht weg, het startscherm groeit in beeld.
    val welcomeExit: ExitTransition = fadeOut(tween(LONG)) + scaleOut(tween(LONG), targetScale = 1.08f)
    val welcomeToHome: EnterTransition = fadeIn(tween(LONG, delay = SHORT)) + scaleIn(gentle(), initialScale = 0.92f)

    // Schermovergangen: invagen en een klein stukje meeschuiven.
    val navEnter: AnimatedContentTransitionScope<NavBackStackEntry>.() -> EnterTransition =
        { fadeIn(tween(MEDIUM)) + slideInHorizontally(tween(MEDIUM)) { it / 10 } }
    val navExit: AnimatedContentTransitionScope<NavBackStackEntry>.() -> ExitTransition = { fadeOut(tween(SHORT)) }
    val navPopEnter: AnimatedContentTransitionScope<NavBackStackEntry>.() -> EnterTransition = { fadeIn(tween(MEDIUM)) }
    val navPopExit: AnimatedContentTransitionScope<NavBackStackEntry>.() -> ExitTransition =
        { fadeOut(tween(SHORT)) + slideOutHorizontally(tween(MEDIUM)) { it / 10 } }
}

/** True vanaf het eerste frame na de eerste keer tonen; blijft true bij terugkeren (voor tellers en knoppen die inspringen). */
@Composable
fun rememberStarted(): Boolean {
    var started by rememberSaveable { mutableStateOf(false) }
    LaunchedEffect(Unit) { started = true }
    return started
}

/**
 * Loopt eenmalig van 0 naar 1 (na [delayMs]) en onthoudt dat, zodat terugkeren naar het scherm
 * de animatie niet opnieuw afspeelt. Voor grafieken die zich tekenen.
 */
@Composable
fun rememberReveal(delayMs: Long = 0, durationMs: Int = Motion.REVEAL): Float {
    var done by rememberSaveable { mutableStateOf(false) }
    val progress = remember { Animatable(if (done) 1f else 0f) }
    LaunchedEffect(Unit) {
        if (!done) {
            delay(delayMs)
            progress.animateTo(1f, Motion.tween(durationMs))
            done = true
        }
    }
    return progress.value
}

/**
 * Laat een element de eerste keer zacht omhoog en in beeld komen, met een kleine vertraging per [index]
 * zodat een lijst als een golfje binnenkomt. Speelt niet opnieuw af bij terugscrollen of terugkeren.
 */
@Composable
fun Modifier.appear(index: Int = 0): Modifier {
    var shown by rememberSaveable { mutableStateOf(false) }
    val progress = remember { Animatable(if (shown) 1f else 0f) }
    LaunchedEffect(Unit) {
        if (!shown) {
            delay(Motion.STAGGER * index.coerceIn(0, 8))
            progress.animateTo(1f, Motion.gentle())
            shown = true
        }
    }
    return this.graphicsLayer {
        val p = progress.value
        alpha = p.coerceIn(0f, 1f)
        translationY = (1f - p) * 24.dp.toPx()
    }
}

/** Geeft een beetje mee onder je vinger: schaalt licht in zolang het element ingedrukt is. */
@Composable
fun Modifier.pressScale(interaction: MutableInteractionSource, pressed: Float = 0.97f): Modifier {
    val isPressed by interaction.collectIsPressedAsState()
    val scale by animateFloatAsState(if (isPressed) pressed else 1f, Motion.snappy(), label = "indrukken")
    return this.graphicsLayer { scaleX = scale; scaleY = scale }
}

/** Springt de eerste keer zacht in beeld (bijvoorbeeld de toevoegknop). */
@Composable
fun Modifier.popIn(): Modifier {
    val scale by animateFloatAsState(if (rememberStarted()) 1f else 0f, Motion.pop(), label = "inspringen")
    return this.graphicsLayer { scaleX = scale; scaleY = scale }
}
