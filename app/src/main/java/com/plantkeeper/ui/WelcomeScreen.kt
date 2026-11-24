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
            Mascot(Modifier.size(200.dp).align(Alignment.CenterHorizontally), intro = true, onDark = true)
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
