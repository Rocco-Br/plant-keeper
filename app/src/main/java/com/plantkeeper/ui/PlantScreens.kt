package com.plantkeeper.ui

import android.net.Uri
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Add
import androidx.compose.material.icons.outlined.Agriculture
import androidx.compose.material.icons.outlined.Close
import androidx.compose.material.icons.outlined.Delete
import androidx.compose.material.icons.outlined.Edit
import androidx.compose.material.icons.outlined.Event
import androidx.compose.material.icons.outlined.FilterAltOff
import androidx.compose.material.icons.outlined.Grain
import androidx.compose.material.icons.outlined.LinkOff
import androidx.compose.material.icons.outlined.LocalFlorist
import androidx.compose.material.icons.outlined.LocationOn
import androidx.compose.material.icons.outlined.StickyNote2
import androidx.compose.material.icons.outlined.Timeline
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.ExtendedFloatingActionButton
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
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

    PkScaffold(
        title = "Planten",
        eyebrow = "${plants.size} keer gezaaid",
        fab = {
            ExtendedFloatingActionButton(onClick = { nav.navigate(Routes.plantForm()) }, icon = { Icon(Icons.Outlined.Add, null) }, text = { Text("Plant") })
        },
    ) { padding ->
        Column {
            SearchField(query) { query = it }
            LazyRow(contentPadding = PaddingValues(horizontal = 20.dp), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                item { FilterChip(filter == null, { filter = null }, { Text("Alle ${plants.size}") }) }
                items(PlantStatus.entries) { s ->
                    FilterChip(filter == s, { filter = if (filter == s) null else s }, { Text("${s.label.capitalized()} ${counts[s] ?: 0}") })
                }
            }
            LazyColumn(contentPadding = padding) {
                if (shown.isEmpty()) {
                    item {
                        if (plants.isEmpty()) EmptyState(Icons.Outlined.LocalFlorist, "Nog niets gezaaid. Voeg een plant toe met de knop rechtsonder, of tik op Zaaien bij een zaad.")
                        else EmptyState(Icons.Outlined.FilterAltOff, "Geen planten gevonden.")
                    }
                }
                items(shown, key = { it.id }) { p ->
                    ListRow(
                        title = p.name,
                        subtitle = "${fmtDate(p.sowDate)} · ${p.location.ifBlank { "Geen locatie" }}",
                        leading = { Thumb(p.name, p.photo, Icons.Outlined.LocalFlorist) },
                        onClick = { nav.navigate(Routes.plant(p.id)) },
                        trailing = { StatusPill(p.status) },
                    )
                }
            }
        }
    }
}

// ---------- Detail ----------

@OptIn(ExperimentalLayoutApi::class)
@Composable
fun PlantDetailScreen(nav: NavController, id: String) {
    val app = app()
    val dao = app.db.dao()
    val plant by remember(id) { dao.plant(id) }.collectAsState(initial = null)
    val p = plant
    val seed by remember(p?.seedId) { p?.seedId?.let { dao.seed(it) } ?: flowOf(null) }.collectAsState(initial = null)
    val scope = rememberCoroutineScope()
    var confirmDelete by remember { mutableStateOf(false) }

    PkScaffold(
        title = p?.name ?: "",
        onBack = { nav.popBackStack() },
        actions = { IconButton(onClick = { nav.navigate(Routes.plantForm(id)) }) { Icon(Icons.Outlined.Edit, "Bewerken") } },
    ) { padding ->
        if (p == null) return@PkScaffold
        Column(Modifier.padding(padding).verticalScroll(rememberScrollState())) {
            PhotoHero(p.name, p.photo, Icons.Outlined.LocalFlorist)
            Column(Modifier.padding(20.dp), verticalArrangement = Arrangement.spacedBy(14.dp)) {
                Column {
                    Eyebrow(seed?.kind?.ifBlank { null } ?: if (seed != null) "Zaad" else "Losse plant")
                    Text(p.name, style = MaterialTheme.typography.headlineSmall, fontWeight = FontWeight.Bold)
                }
                SectionCard("Status", Icons.Outlined.Timeline) {
                    FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        PlantStatus.entries.forEach { s ->
                            FilterChip(p.status == s, { scope.launch { dao.setStatus(p.id, s) } }, { Text(s.label) })
                        }
                    }
                }
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    Fact(Icons.Outlined.Event, "Gezaaid", fmtDate(p.sowDate), Modifier.weight(1f))
                    Fact(Icons.Outlined.LocationOn, "Locatie", p.location.ifBlank { "Onbekend" }, Modifier.weight(1f))
                    val s = seed
                    if (s != null) Fact(Icons.Outlined.Grain, "Zaad", "Bekijk zaad", Modifier.weight(1f)) { nav.navigate(Routes.seed(s.id)) }
                    else Fact(Icons.Outlined.LinkOff, "Zaad", "Geen", Modifier.weight(1f))
                }
                SectionCard("Oogst", Icons.Outlined.Agriculture) {
                    if (p.harvestDate != null || p.harvestText.isNotBlank()) {
                        if (p.harvestDate != null) Text(fmtDate(p.harvestDate), fontWeight = FontWeight.SemiBold)
                        if (p.harvestText.isNotBlank()) Text(p.harvestText, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    } else {
                        Text("Nog niet geoogst. Vul een oogstdatum in via bewerken.", color = MaterialTheme.colorScheme.onSurfaceVariant)
                    }
                    HarvestSeedButton(nav, p)
                }
                if (p.notes.isNotBlank()) {
                    SectionCard("Notities", Icons.Outlined.StickyNote2) { Text(p.notes) }
                }
                OutlinedButton(
                    onClick = { confirmDelete = true },
                    modifier = Modifier.fillMaxWidth(),
                    colors = ButtonDefaults.outlinedButtonColors(contentColor = MaterialTheme.colorScheme.error),
                    border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline),
                ) {
                    Icon(Icons.Outlined.Delete, null)
                    Spacer(Modifier.width(8.dp))
                    Text("Plant verwijderen")
                }
            }
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

/** Zaadgewinning (#3). */
@Composable
private fun HarvestSeedButton(nav: NavController, plant: Plant) {
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
        actions = { Button(onClick = ::save, enabled = loaded && !saving, modifier = Modifier.padding(end = 8.dp)) { Text("Opslaan") } },
    ) { padding ->
        if (!loaded) return@PkScaffold
        Column(
            Modifier.padding(padding).imePadding().verticalScroll(rememberScrollState()).padding(20.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            FormError(error)
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
            Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                DateField("Zaaidatum", sowDate, { sowDate = it }, Modifier.weight(1f))
                DropdownField("Status", PlantStatus.entries, status, { it.label }, { status = it }, Modifier.weight(1f))
            }
            Field("Locatie", location, { location = it }, placeholder = "Vensterbank, bak 2...")
            GroupLabel("Oogst", Icons.Outlined.Agriculture)
            DateField("Oogstdatum", harvestDate, { harvestDate = it }, Modifier.fillMaxWidth(), clearable = true)
            Field("Hoeveelheid en notitie", harvestText, { harvestText = it }, placeholder = "Bijvoorbeeld 2 kg, mooie vruchten")
            Field("Notities", notes, { notes = it }, singleLine = false)
            PhotoPickerField(name, photo, picked) { picked = it }
        }
    }
}
