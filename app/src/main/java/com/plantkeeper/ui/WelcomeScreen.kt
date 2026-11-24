package com.plantkeeper.ui

import androidx.compose.runtime.getValue
import androidx.compose.runtime.setValue
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.isImeVisible
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
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.outlined.ArrowForward
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.input.ImeAction
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
@OptIn(ExperimentalLayoutApi::class)
@Composable
fun WelcomeScreen(onDone: () -> Unit) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    val c = pk
    var name by rememberSaveable { mutableStateOf("") }
    var step by rememberSaveable { mutableIntStateOf(0) }
    var saving by remember { mutableStateOf(false) }
    var greet by remember { mutableStateOf<String?>(null) }

    LaunchedEffect(Unit) {
        // Na het zaadje komen titel, uitleg en de naamvraag na elkaar in beeld.
        while (step < 3) { delay(if (step == 0) 900 else 320); step++ }
    }

    val focus = LocalFocusManager.current

    fun finish(value: String) {
        if (saving) return
        focus.clearFocus() // toetsenbord dicht vóór de overgang naar het startscherm
        saving = true
        scope.launch {
            context.settings.edit { it[SettingKeys.USER_NAME] = value.trim(); it[SettingKeys.WELCOMED] = true }
            // Met een naam eerst een persoonlijke begroeting; overslaan gaat meteen door.
            if (value.isNotBlank()) greet = value.trim() else onDone()
        }
    }

    // Vaste indeling zonder scrollen. Met het toetsenbord open klappen potje en uitleg in, zodat titel en vraag passen.
    val typing = WindowInsets.isImeVisible
    Box(Modifier.fillMaxSize().background(Brush.verticalGradient(c.hero))) {
        Column(
            Modifier.fillMaxSize().statusBarsPadding().navigationBarsPadding().imePadding().padding(horizontal = Space.xl, vertical = Space.l),
        ) {
            Spacer(Modifier.weight(1f))
            AnimatedVisibility(!typing, Modifier.align(Alignment.CenterHorizontally), enter = Motion.expand, exit = Motion.collapse) {
                Mascot(Modifier.padding(bottom = Space.l).size(200.dp), intro = true, onDark = true)
            }
            AnimatedVisibility(step >= 1, enter = Motion.enterUp) {
                Column {
                    Eyebrow("Welkom bij", c.onHero.copy(alpha = 0.8f))
                    Text("Plant Keeper", style = MaterialTheme.typography.displayLarge, color = c.onHero)
                }
            }
            AnimatedVisibility(step >= 2 && !typing, enter = Motion.enterUp, exit = Motion.collapse) {
                Text(
                    "Houd bij welke zaden je hebt, wat je hebt gezaaid en wat je deze maand kunt zaaien. Alles blijft op je eigen telefoon.",
                    style = MaterialTheme.typography.bodyLarge, color = c.onHero.copy(alpha = 0.88f),
                    modifier = Modifier.padding(top = Space.m),
                )
            }
            Spacer(Modifier.weight(1f))
            Spacer(Modifier.height(Space.l))
            AnimatedVisibility(step >= 3, enter = Motion.enterUp) {
                Column(
                    Modifier.fillMaxWidth().clip(leaf(40.dp, 14.dp)).background(MaterialTheme.colorScheme.surface).padding(Space.xl),
                    verticalArrangement = Arrangement.spacedBy(Space.m),
                ) {
                    Text("Hoe mag ik je noemen?", style = MaterialTheme.typography.headlineSmall)
                    Hint("Dan begroet ik je elke keer dat je de app opent.")
                    Field(
                        "Voornaam", name, { name = it.take(40) },
                        imeAction = ImeAction.Done, onImeAction = { if (name.isNotBlank()) finish(name) },
                    )
                    PkButton("Aan de slag", { finish(name) }, icon = Icons.AutoMirrored.Outlined.ArrowForward, enabled = name.isNotBlank() && !saving)
                    PkButton("Overslaan", { finish("") }, Modifier.align(Alignment.CenterHorizontally), kind = ButtonKind.Text, enabled = !saving)
                }
            }
        }
        AnimatedVisibility(greet != null, enter = fadeIn(Motion.tween(Motion.LONG))) {
            Celebration("Hoi ${greet.orEmpty()}!", "Fijn dat je er bent. Ik laat je even rondkijken.", durationMs = 3600, onDone = onDone)
        }
    }
}
