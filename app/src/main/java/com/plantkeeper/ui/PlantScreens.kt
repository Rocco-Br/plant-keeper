package com.plantkeeper.ui

import androidx.compose.runtime.getValue
import androidx.compose.runtime.setValue
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.runtime.derivedStateOf
import android.net.Uri
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.ui.Alignment
import androidx.compose.ui.unit.dp
import androidx.activity.compose.BackHandler
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.drawscope.rotate
import androidx.compose.ui.text.style.TextAlign
import kotlinx.coroutines.delay
import kotlin.math.sin
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.IntrinsicSize
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.ui.semantics.Role
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Agriculture
import androidx.compose.material.icons.outlined.Close
import androidx.compose.material.icons.outlined.Delete
import androidx.compose.material.icons.outlined.Event
import androidx.compose.material.icons.outlined.Grain
import androidx.compose.material.icons.outlined.LinkOff
import androidx.compose.material.icons.outlined.LocalFlorist
import androidx.compose.material.icons.outlined.LocationOn
import androidx.compose.material.icons.outlined.Recycling
import androidx.compose.material.icons.automirrored.outlined.StickyNote2
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.platform.LocalContext
import androidx.navigation.NavController
import com.plantkeeper.data.Plant
import com.plantkeeper.data.PlantStatus
import com.plantkeeper.data.Seed
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.time.LocalDate

// ---------- Lijst ----------

@Composable
fun PlantListScreen(nav: NavController) {
    val plants by app().db.dao().plants().collectAsState(initial = emptyList())
    var filter by rememberSaveable { mutableStateOf<PlantStatus?>(null) }
    var query by rememberSaveable { mutableStateOf("") }
    val counts = plants.groupingBy { it.status }.eachCount()
    val shown = plants.filter { (filter == null || it.status == filter) && it.name.contains(query.trim(), ignoreCase = true) }
    val list = rememberLazyListState()
    val atTop by remember { derivedStateOf { list.firstVisibleItemIndex == 0 } }

    PkScaffold(
        title = "Planten",
        eyebrow = "${plants.size} keer gezaaid",
        fab = { AddFab("Plant", expanded = atTop) { nav.navigate(Routes.plantForm()) } },
    ) { padding ->
        Column {
            SearchField(query) { query = it }
            LazyRow(contentPadding = PaddingValues(horizontal = Space.l), horizontalArrangement = Arrangement.spacedBy(Space.s)) {
                item { SelectChip("Alle", filter == null, { filter = null }, count = plants.size, role = Role.Tab) }
                items(PlantStatus.entries) { s ->
                    SelectChip(s.label.capitalized(), filter == s, { filter = if (filter == s) null else s }, s.icon, statusColors(s), counts[s] ?: 0, Role.Tab)
                }
            }
            LazyColumn(state = list, contentPadding = padding) {
                if (shown.isEmpty()) {
                    item(key = "leeg") {
                        if (plants.isEmpty()) EmptyState("Nog niets gezaaid. Voeg een plant toe met de knop rechtsonder, of tik op Zaaien bij een zaad.", Mood.HAPPY, Modifier.animateItem())
                        else EmptyState("Geen planten gevonden.", Mood.CURIOUS, Modifier.animateItem())
                    }
                }
                itemsIndexed(shown, key = { _, it -> it.id }) { index, p ->
                    ListRow(
                        title = p.name,
                        subtitle = "${fmtDate(p.sowDate)} · ${p.location.ifBlank { "Geen locatie" }}",
                        leading = { Thumb(p.name, p.photo, Icons.Outlined.LocalFlorist) },
                        onClick = { nav.navigate(Routes.plant(p.id)) },
                        modifier = Modifier.animateItem().appear(index),
                        trailing = { StatusLabel(p.status) },
                    )
                }
            }
        }
    }
}

// ---------- Detail ----------

@Composable
fun PlantDetailScreen(nav: NavController, id: String) {
    val app = app()
    val dao = app.db.dao()
    val plant by remember(id) { dao.plant(id) }.collectAsState(initial = null)
    val p = plant
    val seed by remember(p?.seedId) { p?.seedId?.let { dao.seed(it) } ?: flowOf(null) }.collectAsState(initial = null)
    val scope = rememberCoroutineScope()
    var confirmDelete by remember { mutableStateOf(false) }

    if (p == null) {
        Box(Modifier.fillMaxSize().background(MaterialTheme.colorScheme.surface))
    } else {
        DetailScaffold(p.name, p.photo, Icons.Outlined.LocalFlorist, onBack = { nav.popBackStack() }, onEdit = { nav.navigate(Routes.plantForm(id)) }) {
            DetailTitle(seed?.kind?.ifBlank { null } ?: if (seed != null) "Zaad" else "Losse plant", p.name)
            SectionCard("Hoe staat hij erbij?", p.status.icon, tint = statusColors(p.status).second) {
                StatusPicker(p.status) { s -> scope.launch { dao.setStatus(p.id, s) } }
            }
            FactsCard {
                val cs = MaterialTheme.colorScheme
                FactRow(Icons.Outlined.Event, "Gezaaid", fmtDate(p.sowDate), cs.onPrimaryContainer, cs.primaryContainer)
                FactRow(Icons.Outlined.LocationOn, "Locatie", p.location.ifBlank { "Onbekend" }, pk.onIndoorContainer, pk.indoorContainer)
                val s = seed
                if (s != null) {
                    FactRow(Icons.Outlined.Grain, "Zaad", s.name, cs.onSecondaryContainer, cs.secondaryContainer) { nav.navigate(Routes.seed(s.id)) }
                } else {
                    FactRow(Icons.Outlined.LinkOff, "Zaad", "Losse plant", cs.onSurfaceVariant, cs.surfaceContainerHigh)
                }
            }
            if (p.notes.isNotBlank()) {
                SectionCard("Notities", Icons.AutoMirrored.Outlined.StickyNote2) { Text(p.notes, style = MaterialTheme.typography.bodyMedium) }
            }
            PkButton("Plant verwijderen", { confirmDelete = true }, Modifier.fillMaxWidth(), Icons.Outlined.Delete, ButtonKind.Danger)
        }
    }

    if (confirmDelete && p != null) {
        ConfirmDialog(
            title = "Plant verwijderen?",
            text = "‘${p.name}’ (gezaaid ${fmtDate(p.sowDate)}) wordt verwijderd. Wil je de geschiedenis bewaren, zet de status dan op overleden.",
            confirm = "Verwijderen",
            onConfirm = {
                confirmDelete = false
                scope.launch {
                    dao.deletePlant(p.id)
                    app.photos.delete(p.photo)
                    nav.popBackStack()
                }
            },
            onDismiss = { confirmDelete = false },
        )
    }
}

// ---------- Formulier ----------

@Composable
fun PlantFormScreen(nav: NavController, id: String?, seedId: String?) {
    val app = app()
    val dao = app.db.dao()
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    val focus = LocalFocusManager.current
    val seeds by dao.seeds().collectAsState(initial = emptyList())

    var loaded by rememberSaveable { mutableStateOf(false) }
    var seed by rememberSaveable { mutableStateOf(seedId) }
    var name by rememberSaveable { mutableStateOf("") }
    var sowDate by rememberSaveable { mutableStateOf<String?>(LocalDate.now().toString()) }
    var status by rememberSaveable { mutableStateOf(PlantStatus.GEZAAID) }
    var location by rememberSaveable { mutableStateOf("") }
    var harvestDate by rememberSaveable { mutableStateOf<String?>(null) }
    var harvestText by rememberSaveable { mutableStateOf("") }
    var notes by rememberSaveable { mutableStateOf("") }
    var photo by rememberSaveable { mutableStateOf<String?>(null) }
    var picked by rememberSaveable { mutableStateOf<String?>(null) }
    var error by rememberSaveable { mutableStateOf<String?>(null) }
    var saving by remember { mutableStateOf(false) }
    var celebrate by remember { mutableStateOf<String?>(null) }

    LaunchedEffect(id) {
        if (loaded) return@LaunchedEffect
        if (id != null) {
            dao.getPlant(id)?.let { p ->
                seed = p.seedId; name = p.name; sowDate = p.sowDate; status = p.status; location = p.location
                harvestDate = p.harvestDate; harvestText = p.harvestText; notes = p.notes; photo = p.photo
            }
        } else if (seedId != null) {
            name = dao.getSeed(seedId)?.name.orEmpty()
        }
        loaded = true
    }

    fun save() {
        focus.clearFocus() // toetsenbord dicht, zodat het niet over een melding of animatie blijft hangen
        error = when {
            name.isBlank() -> "Vul een naam in."
            sowDate == null -> "Kies een zaaidatum."
            else -> null
        }
        if (error != null || saving) return
        saving = true
        scope.launch {
            try {
                val newPhoto = picked?.let { withContext(Dispatchers.IO) { app.photos.save(context, Uri.parse(it)) } }
                val base = id?.let { dao.getPlant(it) } ?: Plant(name = "", sowDate = "")
                dao.upsertPlant(
                    base.copy(
                        seedId = seed, name = name.trim(), sowDate = sowDate!!, status = status, location = location.trim(),
                        harvestDate = harvestDate, harvestText = harvestText.trim(), notes = notes.trim(), photo = newPhoto ?: photo,
                    ),
                )
                if (newPhoto != null) app.photos.delete(photo)
                // Een nieuwe zaai krijgt een feestje; bewerken gaat gewoon terug.
                if (id == null) celebrate = name.trim() else nav.popBackStack()
            } catch (e: Exception) {
                error = "Opslaan mislukt: ${e.message}"
                saving = false
            }
        }
    }

    Box(Modifier.fillMaxSize()) {
    PkScaffold(
        title = if (id != null) "Plant bewerken" else "Nieuwe plant",
        onBack = { nav.popBackStack() },
        backIcon = Icons.Outlined.Close,
        actions = { SaveButton(enabled = loaded && !saving, onClick = ::save) },
    ) { padding ->
        if (!loaded) return@PkScaffold
        Column(
            Modifier.padding(padding).imePadding().verticalScroll(rememberScrollState()).padding(horizontal = Space.l, vertical = Space.s),
            verticalArrangement = Arrangement.spacedBy(Space.m),
        ) {
            FormError(error)
            PhotoPickerField(name, photo, picked) { picked = it }
            SectionCard("Wat heb je gezaaid?", Icons.Outlined.Grain) {
                val current = seeds.firstOrNull { it.id == seed }
                DropdownField<Seed?>(
                    "Zaad", listOf(null) + seeds, current, { it?.name ?: "Losse plant (zonder zaad)" },
                    { chosen ->
                        // Naam volgt het zaad zolang je hem niet zelf hebt aangepast.
                        if (name.isBlank() || name == current?.name) name = chosen?.name.orEmpty()
                        seed = chosen?.id
                    },
                    Modifier.fillMaxWidth(),
                )
                Field("Naam", name, { name = it }, placeholder = "Naam van de plant")
                Hint("Neemt de naam van het zaad over. De plant houdt deze naam ook als het zaad later verdwijnt.")
            }
            SectionCard("Waar en wanneer", Icons.Outlined.LocationOn, tint = pk.indoor) {
                DateField("Zaaidatum", sowDate, { sowDate = it }, Modifier.fillMaxWidth())
                Field("Locatie", location, { location = it }, placeholder = "Vensterbank, bak 2...", imeAction = ImeAction.Done)
            }
            SectionCard("Status", status.icon, tint = statusColors(status).second) {
                StatusPicker(status) { status = it }
            }
            SectionCard("Notities", Icons.AutoMirrored.Outlined.StickyNote2) {
                Field("Notities", notes, { notes = it }, singleLine = false)
            }
            Spacer(Modifier.height(Space.xl))
        }
    }
    celebrate?.let { SownCelebration(it) { nav.popBackStack() } }
    }
}

/**
 * Feestje na een nieuwe zaai: het potje speelt zijn opstartreeks, er dwarrelen blaadjes en de naam komt erbij.
 * Gaat na een paar tellen vanzelf verder; tikken of terug slaat het over.
 */
@Composable
private fun SownCelebration(name: String, onDone: () -> Unit) {
    var finished by remember { mutableStateOf(false) }
    val finish = { if (!finished) { finished = true; onDone() } }
    var showText by remember { mutableStateOf(false) }
    val fall = remember { Animatable(0f) }
    LaunchedEffect(Unit) {
        launch { fall.animateTo(1f, tween(CelebrationMs, easing = LinearEasing)) }
        delay(900); showText = true
        delay(CelebrationMs - 900L); finish()
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
        Column(horizontalAlignment = Alignment.CenterHorizontally, modifier = Modifier.padding(horizontal = Space.xl)) {
            Mascot(Modifier.size(220.dp), intro = true, onDark = true)
            AnimatedVisibility(showText, enter = Motion.enterUp) {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Text("Gezaaid!", style = MaterialTheme.typography.displayMedium, color = c.onHero)
                    Text(name, style = MaterialTheme.typography.titleLarge, color = c.onHero.copy(alpha = 0.85f), textAlign = TextAlign.Center)
                    Spacer(Modifier.height(Space.xl))
                    Text("Tik om verder te gaan", style = MaterialTheme.typography.bodySmall, color = c.onHero.copy(alpha = 0.6f))
                }
            }
        }
    }
}

private const val CelebrationMs = 3200

