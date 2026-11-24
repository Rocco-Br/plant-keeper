package com.plantkeeper.ui

import androidx.compose.runtime.getValue
import androidx.compose.runtime.setValue
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.runtime.derivedStateOf
import android.net.Uri
import androidx.compose.foundation.background
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
            Row(Modifier.height(IntrinsicSize.Min), horizontalArrangement = Arrangement.spacedBy(Space.s)) {
                Fact(Icons.Outlined.Event, "Gezaaid", fmtDate(p.sowDate), Modifier.weight(1f), MaterialTheme.colorScheme.primaryContainer, MaterialTheme.colorScheme.onPrimaryContainer)
                Fact(Icons.Outlined.LocationOn, "Locatie", p.location.ifBlank { "Onbekend" }, Modifier.weight(1f), pk.indoorContainer, pk.onIndoorContainer)
                val s = seed
                if (s != null) {
                    Fact(Icons.Outlined.Grain, "Zaad", "Bekijk zaad", Modifier.weight(1f), MaterialTheme.colorScheme.secondaryContainer, MaterialTheme.colorScheme.onSecondaryContainer) {
                        nav.navigate(Routes.seed(s.id))
                    }
                } else {
                    Fact(Icons.Outlined.LinkOff, "Zaad", "Geen", Modifier.weight(1f))
                }
            }
            SectionCard("Oogst", Icons.Outlined.Agriculture, tint = statusColors(PlantStatus.GEOOGST).second) {
                if (p.harvestDate != null || p.harvestText.isNotBlank()) {
                    if (p.harvestDate != null) Text(fmtDate(p.harvestDate), style = MaterialTheme.typography.titleMedium)
                    if (p.harvestText.isNotBlank()) Text(p.harvestText, style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
                } else {
                    Hint("Nog niet geoogst. Vul een oogstdatum in via bewerken.")
                }
                HarvestSeedButton(nav, p)
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

/** Zaadgewinning: opent het zaadformulier, gekoppeld aan deze plant als moederplant. */
@Composable
private fun HarvestSeedButton(nav: NavController, plant: Plant) {
    PkButton("Maak zaad van deze plant", { nav.navigate(Routes.seedForm(mother = plant.id)) }, icon = Icons.Outlined.Recycling, kind = ButtonKind.Tonal)
}

// ---------- Formulier ----------

@Composable
fun PlantFormScreen(nav: NavController, id: String?, seedId: String?) {
    val app = app()
    val dao = app.db.dao()
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
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
                nav.popBackStack()
            } catch (e: Exception) {
                error = "Opslaan mislukt: ${e.message}"
                saving = false
            }
        }
    }

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
            SectionCard("Oogst", Icons.Outlined.Agriculture, tint = statusColors(PlantStatus.GEOOGST).second) {
                DateField("Oogstdatum", harvestDate, { harvestDate = it }, Modifier.fillMaxWidth(), clearable = true)
                Field("Hoeveelheid en notitie", harvestText, { harvestText = it }, placeholder = "Bijvoorbeeld 2 kg, mooie vruchten", imeAction = ImeAction.Done)
            }
            SectionCard("Notities", Icons.AutoMirrored.Outlined.StickyNote2) {
                Field("Notities", notes, { notes = it }, singleLine = false)
            }
            Spacer(Modifier.height(Space.xl))
        }
    }
}
