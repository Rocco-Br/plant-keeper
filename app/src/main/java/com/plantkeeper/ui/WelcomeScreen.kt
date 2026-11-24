package com.plantkeeper.ui

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.Animatable
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
            GrowingSprout(Modifier.size(190.dp).align(Alignment.CenterHorizontally))
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

private val SEED = PathParser().parsePathString("M39,73 C39,66 46,63 54,63 C62,63 69,66 69,73 C69,79 62,82 54,82 C46,82 39,79 39,73 Z").toPath()
private val LEAF_LEFT = PathParser().parsePathString("M54,56 C46,57 37,52 35,40 C46,39 54,45 54,56 Z").toPath()
private val LEAF_RIGHT = PathParser().parsePathString("M54,50 C55,40 63,32 75,32 C75,44 67,51 54,50 Z").toPath()

/**
 * Het app-icoon, maar dan levend: het zaadje veert op, de steel groeit, de blaadjes ontvouwen
 * en daarna wiegt het plantje zachtjes heen en weer. Tekent in de 108-eenheden van het icoon.
 */
@Composable
fun GrowingSprout(modifier: Modifier = Modifier) {
    val seed = remember { Animatable(0f) }
    val stem = remember { Animatable(0f) }
    val left = remember { Animatable(0f) }
    val right = remember { Animatable(0f) }
    LaunchedEffect(Unit) {
        launch { seed.animateTo(1f, spring(dampingRatio = 0.45f, stiffness = 220f)) }
        delay(280)
        launch { stem.animateTo(1f, tween(420, easing = FastOutSlowInEasing)) }
        delay(360)
        launch { left.animateTo(1f, spring(dampingRatio = 0.42f, stiffness = 200f)) }
        delay(150)
        right.animateTo(1f, spring(dampingRatio = 0.42f, stiffness = 200f))
    }
    val sway by rememberInfiniteTransition(label = "wiegen").animateFloat(
        initialValue = -4f, targetValue = 4f,
        animationSpec = infiniteRepeatable(tween(2400, easing = FastOutSlowInEasing), RepeatMode.Reverse),
        label = "wiegen",
    )
    val cream = Color(0xFFF3F8EC)
    Canvas(modifier) {
        val unit = size.minDimension / 108f
        withTransform({ scale(unit, unit, Offset.Zero) }) {
            drawCircle(Color.White.copy(alpha = 0.10f), radius = 46f * seed.value, center = Offset(54f, 56f))
            rotate(sway * right.value, pivot = Offset(54f, 70f)) {
                drawLine(cream, Offset(54f, 70f), Offset(54f, 70f - 22f * stem.value), strokeWidth = 4.5f, cap = StrokeCap.Round)
                scale(left.value, pivot = Offset(54f, 56f)) { drawPath(LEAF_LEFT, cream) }
                scale(right.value, pivot = Offset(54f, 50f)) { drawPath(LEAF_RIGHT, Color(0xFFB5D98A)) }
            }
            scale(seed.value, pivot = Offset(54f, 82f)) { drawPath(SEED, Color(0xFFE58A5F)) }
        }
    }
}
