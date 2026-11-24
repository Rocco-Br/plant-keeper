package com.plantkeeper.ui

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.Animatable
import androidx.compose.ui.graphics.drawscope.translate
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.animation.core.keyframes
import androidx.compose.animation.core.FastOutLinearInEasing
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.slideInVertically
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.outlined.ArrowForward
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TextField
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.rotate
import androidx.compose.ui.graphics.drawscope.scale
import androidx.compose.ui.graphics.drawscope.withTransform
import androidx.compose.ui.graphics.vector.PathParser
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardCapitalization
import androidx.compose.ui.unit.dp
import androidx.datastore.preferences.core.edit
import com.plantkeeper.data.SettingKeys
import com.plantkeeper.data.settings
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

/**
 * Eerste scherm na installatie: een kiemend zaadje groeit, de tekst komt erachteraan en daarna de vraag naar je naam.
 * Overslaan mag; dan begroet de app je zonder naam.
 */
@Composable
fun WelcomeScreen(onDone: () -> Unit) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    val c = pk
    var name by rememberSaveable { mutableStateOf("") }
    var step by rememberSaveable { mutableIntStateOf(0) }
    var saving by remember { mutableStateOf(false) }

    LaunchedEffect(Unit) {
        // Na het zaadje komen titel, uitleg en de naamvraag na elkaar in beeld.
        while (step < 3) { delay(if (step == 0) 900 else 320); step++ }
    }

    fun finish(value: String) {
        if (saving) return
        saving = true
        scope.launch {
            context.settings.edit { it[SettingKeys.USER_NAME] = value.trim(); it[SettingKeys.WELCOMED] = true }
            onDone()
        }
    }

    Box(Modifier.fillMaxSize().background(Brush.verticalGradient(c.hero))) {
        Column(
            Modifier.fillMaxSize().statusBarsPadding().navigationBarsPadding().imePadding()
                .verticalScroll(rememberScrollState()).padding(horizontal = 24.dp, vertical = 16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp),
        ) {
            Spacer(Modifier.height(12.dp))
            PotMascot(Modifier.size(200.dp).align(Alignment.CenterHorizontally))
            AnimatedVisibility(step >= 1, enter = fadeIn(tween(500)) + slideInVertically(tween(500)) { it / 3 }) {
                Column {
                    Eyebrow("Welkom bij", c.onHero.copy(alpha = 0.8f))
                    Text("Plant Keeper", style = MaterialTheme.typography.displayLarge, color = c.onHero)
                }
            }
            AnimatedVisibility(step >= 2, enter = fadeIn(tween(500)) + slideInVertically(tween(500)) { it / 3 }) {
                Text(
                    "Houd bij welke zaden je hebt, wat je hebt gezaaid en wat je deze maand kunt zaaien. Alles blijft op je eigen telefoon.",
                    style = MaterialTheme.typography.bodyLarge, color = c.onHero.copy(alpha = 0.88f),
                )
            }
            Spacer(Modifier.height(8.dp))
            AnimatedVisibility(step >= 3, enter = fadeIn(tween(450)) + slideInVertically(spring(dampingRatio = 0.75f, stiffness = 260f)) { it / 2 }) {
                Column(
                    Modifier.fillMaxWidth().clip(leaf(40.dp, 14.dp)).background(MaterialTheme.colorScheme.surface).padding(22.dp),
                    verticalArrangement = Arrangement.spacedBy(12.dp),
                ) {
                    Text("Hoe mag ik je noemen?", style = MaterialTheme.typography.headlineSmall)
                    Hint("Dan begroet ik je elke keer dat je de app opent.")
                    TextField(
                        value = name,
                        onValueChange = { name = it.take(40) },
                        placeholder = { Text("Je voornaam") },
                        singleLine = true,
                        shape = FieldShape,
                        colors = pkFieldColors(),
                        keyboardOptions = KeyboardOptions(capitalization = KeyboardCapitalization.Words, imeAction = ImeAction.Done),
                        keyboardActions = KeyboardActions(onDone = { if (name.isNotBlank()) finish(name) }),
                        modifier = Modifier.fillMaxWidth(),
                    )
                    BigButton("Aan de slag", Icons.AutoMirrored.Outlined.ArrowForward, { finish(name) }, enabled = name.isNotBlank() && !saving)
                    TextButton(onClick = { finish("") }, enabled = !saving, modifier = Modifier.align(Alignment.CenterHorizontally)) {
                        Text("Overslaan")
                    }
                }
            }
        }
    }
}

private fun path(d: String) = PathParser().parsePathString(d).toPath()
private val POT_BODY = path("M41,66 H67 L64,84 Q63.5,86 61,86 H47 Q44.5,86 44,84 Z")
private val LEAF_LEFT = path("M54,52 C48,54 40,51 39,43 C46,41 53,44 54,52 Z")
private val LEAF_RIGHT = path("M54,47 C55,39 62,34 70,35 C70,43 63,48 54,47 Z")
private val SMILE = path("M51.5,80 C53,82 55,82 56.5,80")
private val DROP = path("M54,24 C57,29 60,32 60,35 C60,38.5 57.3,41 54,41 C50.7,41 48,38.5 48,35 C48,32 51,29 54,24 Z")

/**
 * De mascotte van het app-icoon, maar dan levend: een druppel valt in het potje, het potje veert in,
 * het plantje schiet omhoog en daarna wiegt het zachtjes en knippert het potje af en toe met zijn oogjes.
 * Tekent in de 108-eenheden van het icoon.
 */
@Composable
fun PotMascot(modifier: Modifier = Modifier) {
    val drop = remember { Animatable(0f) }
    val squash = remember { Animatable(0f) }
    val grow = remember { Animatable(0f) }
    val glow = remember { Animatable(0f) }
    LaunchedEffect(Unit) {
        launch { drop.animateTo(1f, tween(460, easing = FastOutLinearInEasing)) }
        delay(400)
        launch { squash.animateTo(1f, tween(380)) }
        delay(150)
        launch { grow.animateTo(1f, spring(dampingRatio = 0.4f, stiffness = 230f)) }
        delay(250)
        glow.animateTo(1f, tween(500))
    }
    val loop = rememberInfiniteTransition(label = "mascotte")
    val sway by loop.animateFloat(
        initialValue = -4f, targetValue = 4f,
        animationSpec = infiniteRepeatable(tween(2400, easing = FastOutSlowInEasing), RepeatMode.Reverse),
        label = "wiegen",
    )
    val blink by loop.animateFloat(
        initialValue = 1f, targetValue = 1f,
        animationSpec = infiniteRepeatable(keyframes {
            durationMillis = 3800
            1f at 3400
            0.1f at 3500
            1f at 3600
        }),
        label = "knipperen",
    )
    val cream = Color(0xFFF3F8EC)
    val ink = Color(0xFF4A2412)
    Canvas(modifier) {
        val unit = size.minDimension / 108f
        withTransform({ scale(unit, unit, Offset.Zero) }) {
            drawCircle(Color(0xFFB5D98A).copy(alpha = 0.13f * glow.value), radius = 27f, center = Offset(54f, 54f))
            // Plantje: groeit vanuit de aarde in het potje, wiegt daarna.
            scale(1f, grow.value, pivot = Offset(54f, 64f)) {
                rotate(sway * grow.value.coerceIn(0f, 1f), pivot = Offset(54f, 64f)) {
                    drawLine(cream, Offset(54f, 64f), Offset(54f, 44f), strokeWidth = 4f, cap = StrokeCap.Round)
                    drawPath(LEAF_LEFT, cream)
                    drawPath(LEAF_RIGHT, Color(0xFFB5D98A))
                }
            }
            // Potje met gezichtje; veert in als de druppel landt.
            val bump = kotlin.math.sin(Math.PI * squash.value).toFloat()
            scale(1f + 0.09f * bump, 1f - 0.14f * bump, pivot = Offset(54f, 86f)) {
                drawPath(POT_BODY, Color(0xFFE07B4C))
                drawRoundRect(Color(0xFFC9653A), Offset(38f, 60f), Size(32f, 8f), CornerRadius(3f))
                drawLine(Color.White.copy(alpha = 0.35f), Offset(47f, 71f), Offset(48.3f, 80f), strokeWidth = 2f, cap = StrokeCap.Round)
                drawOval(Color(0xBFFFBEAA), Offset(44.4f, 77.5f), Size(5.2f, 3f))
                drawOval(Color(0xBFFFBEAA), Offset(58.4f, 77.5f), Size(5.2f, 3f))
                scale(1f, blink, pivot = Offset(54f, 75f)) {
                    drawOval(ink, Offset(48.3f, 72.7f), Size(3.4f, 4.6f))
                    drawOval(ink, Offset(56.3f, 72.7f), Size(3.4f, 4.6f))
                }
                drawPath(SMILE, ink, style = Stroke(width = 1.4f, cap = StrokeCap.Round))
            }
            // Druppel valt van boven in het potje en verdwijnt bij de rand.
            if (drop.value < 1f) {
                val fade = if (drop.value < 0.8f) 1f else (1f - drop.value) / 0.2f
                translate(top = 22f * drop.value) { drawPath(DROP, Color(0xFF5AA9E0).copy(alpha = fade)) }
            }
        }
    }
}
