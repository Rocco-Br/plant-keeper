package com.plantkeeper.ui

import android.net.Uri
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Add
import androidx.compose.material.icons.outlined.CalendarMonth
import androidx.compose.material.icons.outlined.Close
import androidx.compose.material.icons.outlined.Delete
import androidx.compose.material.icons.outlined.Edit
import androidx.compose.material.icons.outlined.Grain
import androidx.compose.material.icons.outlined.HourglassBottom
import androidx.compose.material.icons.outlined.HourglassDisabled
import androidx.compose.material.icons.outlined.House
import androidx.compose.material.icons.outlined.Inventory2
import androidx.compose.material.icons.outlined.LocalFlorist
import androidx.compose.material.icons.outlined.Notifications
import androidx.compose.material.icons.outlined.NotificationsOff
import androidx.compose.material.icons.outlined.Recycling
import androidx.compose.material.icons.outlined.Scale
import androidx.compose.material.icons.outlined.Search
import androidx.compose.material.icons.outlined.SearchOff
import androidx.compose.material.icons.outlined.StickyNote2
import androidx.compose.material.icons.outlined.Yard
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.ExtendedFloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.navigation.NavController
import com.plantkeeper.data.MonthRange
import com.plantkeeper.data.QuantityUnit
import com.plantkeeper.data.Seed
import com.plantkeeper.data.indoor
import com.plantkeeper.data.isExpired
import com.plantkeeper.data.nextReminderDate
import com.plantkeeper.data.outdoor
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.time.LocalDate

// ---------- Lijst ----------

@Composable
fun SeedListScreen(nav: NavController) {
    val seeds by app().db.dao().seeds().collectAsState(initial = emptyList())
    var query by rememberSaveable { mutableStateOf("") }
    val year = LocalDate.now().year
    val shown = seeds.filter { it.name.contains(query.trim(), ignoreCase = true) }

    PkScaffold(
        title = "Zaden",
        eyebrow = "${seeds.size} soorten in voorraad",
        fab = {
            ExtendedFloatingActionButton(onClick = { nav.navigate(Routes.seedForm()) }, icon = { Icon(Icons.Outlined.Add, null) }, text = { Text("Zaad") })
        },
    ) { padding ->
        Column {
            SearchField(query) { query = it }
            LazyColumn(contentPadding = padding) {
                if (shown.isEmpty()) {
                    item {
                        if (seeds.isEmpty()) EmptyState(Icons.Outlined.Grain, "Nog geen zaden. Voeg je eerste zaad toe met de knop rechtsonder.")
                        else EmptyState(Icons.Outlined.SearchOff, "Geen zaden gevonden voor ‘$query’.")
                    }
                }
                items(shown, key = { it.id }) { seed ->
                    ListRow(
                        title = seed.name,
                        subtitle = listOf(qtyText(seed.quantity, seed.unit), seed.storage.ifBlank { seed.kind }).filter { it.isNotBlank() }.joinToString(" · "),
                        leading = { Thumb(seed.name, seed.photo, Icons.Outlined.Grain) },
                        onClick = { nav.navigate(Routes.seed(seed.id)) },
                        trailing = { if (seed.isExpired(year)) ExpiredBadge() else Chevron() },
                    )
                }
            }
        }
    }
}

@Composable
fun SearchField(query: String, onChange: (String) -> Unit) {
    OutlinedTextField(
        value = query,
        onValueChange = onChange,
        placeholder = { Text("Zoek op naam") },
        leadingIcon = { Icon(Icons.Outlined.Search, null) },
        trailingIcon = { if (query.isNotEmpty()) IconButton(onClick = { onChange("") }) { Icon(Icons.Outlined.Close, "Wissen") } },
        singleLine = true,
        shape = CircleShape,
        modifier = Modifier.fillMaxWidth().padding(horizontal = 20.dp, vertical = 8.dp),
    )
}

@Composable
fun ExpiredBadge(text: String = "verlopen") = Badge(text, Icons.Outlined.HourglassDisabled)

// ---------- Detail ----------

@Composable
fun SeedDetailScreen(nav: NavController, id: String) {
    val app = app()
    val dao = app.db.dao()
    val seed by remember(id) { dao.seed(id) }.collectAsState(initial = null)
    val plants by remember(id) { dao.plantsForSeed(id) }.collectAsState(initial = emptyList())
    val scope = rememberCoroutineScope()
    var confirmDelete by remember { mutableStateOf(false) }
    val today = LocalDate.now()
    val s = seed

    PkScaffold(
        title = s?.name ?: "",
        onBack = { nav.popBackStack() },
        actions = { IconButton(onClick = { nav.navigate(Routes.seedForm(id)) }) { Icon(Icons.Outlined.Edit, "Bewerken") } },
    ) { padding ->
        if (s == null) return@PkScaffold
        Column(Modifier.padding(padding).verticalScroll(rememberScrollState())) {
            PhotoHero(s.name, s.photo, Icons.Outlined.Grain)
            Column(Modifier.padding(20.dp), verticalArrangement = Arrangement.spacedBy(14.dp)) {
                Column {
                    Eyebrow(s.kind.ifBlank { "Zaad" })
                    Text(s.name, style = MaterialTheme.typography.headlineSmall, fontWeight = FontWeight.Bold)
                }
                if (s.isExpired(today.year)) ExpiredBadge("Verlopen sinds ${s.expiryYear}")
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    Fact(Icons.Outlined.Scale, "Voorraad", qtyText(s.quantity, s.unit), Modifier.weight(1f))
                    Fact(Icons.Outlined.HourglassBottom, "Houdbaar", s.expiryYear?.let { "t/m $it" } ?: "Onbekend", Modifier.weight(1f))
                    Fact(Icons.Outlined.Inventory2, "Bewaarplek", s.storage.ifBlank { "Onbekend" }, Modifier.weight(1f))
                }
                MotherLink(nav, s)
                SectionCard("Zaaitijden", Icons.Outlined.CalendarMonth) {
                    SowingGrid(s.indoor, s.outdoor, today.monthValue)
                    Text(
                        "Binnen ${periodText(s.indoor)} · Buiten ${periodText(s.outdoor)}",
                        style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
                ReminderCard(s, today)
                Button(onClick = { nav.navigate(Routes.plantForm(seed = s.id)) }, modifier = Modifier.fillMaxWidth()) {
                    Icon(Icons.Outlined.LocalFlorist, null)
                    Spacer(Modifier.width(8.dp))
                    Text("Zaaien")
                }
                SectionCard("Gezaaid (${plants.size})", Icons.Outlined.LocalFlorist) {
                    if (plants.isEmpty()) {
                        Text("Nog niet gezaaid. Tik op Zaaien om de eerste zaai vast te leggen.", color = MaterialTheme.colorScheme.onSurfaceVariant)
                    }
                    plants.forEach { p ->
                        Row(
                            Modifier.fillMaxWidth().clip(RoundedCornerShape(12.dp))
                                .background(MaterialTheme.colorScheme.surfaceContainer)
                                .clickable { nav.navigate(Routes.plant(p.id)) }
                                .padding(12.dp),
                            verticalAlignment = Alignment.CenterVertically,
                        ) {
                            Column(Modifier.weight(1f)) {
                                Text(fmtDate(p.sowDate), fontWeight = FontWeight.SemiBold)
                                Text(p.location.ifBlank { "Geen locatie" }, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                            }
                            StatusPill(p.status)
                        }
                    }
                }
                if (s.notes.isNotBlank()) {
                    SectionCard("Notities", Icons.Outlined.StickyNote2) { Text(s.notes) }
                }
                OutlinedButton(
                    onClick = { confirmDelete = true },
                    modifier = Modifier.fillMaxWidth(),
                    colors = ButtonDefaults.outlinedButtonColors(contentColor = MaterialTheme.colorScheme.error),
                    border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline),
                ) {
                    Icon(Icons.Outlined.Delete, null)
                    Spacer(Modifier.width(8.dp))
                    Text("Zaad verwijderen")
                }
            }
        }
    }

    if (confirmDelete && s != null) {
        ConfirmDialog(
            title = "Zaad verwijderen?",
            text = "‘${s.name}’ wordt verwijderd. Gezaaide planten blijven bestaan, alleen de koppeling met dit zaad verdwijnt.",
            confirm = "Verwijderen",
            onConfirm = {
                confirmDelete = false
                scope.launch {
                    dao.deleteSeed(s.id)
                    app.photos.delete(s.photo)
                    nav.popBackStack()
                }
            },
            onDismiss = { confirmDelete = false },
        )
    }
}

/** Placeholder voor zaadgewinning (#3): toont de moederplant. */
@Composable
private fun MotherLink(nav: NavController, seed: Seed) {
}

@Composable
private fun ReminderCard(seed: Seed, today: LocalDate) {
    val dao = app().db.dao()
    val scope = rememberCoroutineScope()
    val next = seed.nextReminderDate(today)
    SectionCard {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Icon(if (seed.reminder) Icons.Outlined.Notifications else Icons.Outlined.NotificationsOff, null, tint = MaterialTheme.colorScheme.primary)
            Spacer(Modifier.width(12.dp))
            Column(Modifier.weight(1f)) {
                Text("Herinnering", fontWeight = FontWeight.SemiBold)
                Text(
                    when {
                        next == null -> "Geen zaaiperiode ingesteld"
                        seed.reminder -> "Volgende melding op ${fmtLongDate(next)}"
                        else -> "Uit. Zet aan voor een melding op ${fmtLongDate(next)}"
                    },
                    style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
            Switch(
                checked = seed.reminder,
                enabled = next != null,
                onCheckedChange = { on -> scope.launch { dao.setReminder(seed.id, on) } },
            )
        }
    }
}

/** Jaarbalk: 12 maanden, binnen in bosgroen en buiten in saliegroen, de huidige maand omlijnd. */
@Composable
fun SowingGrid(indoor: MonthRange?, outdoor: MonthRange?, currentMonth: Int) {
    val labelWidth = 72.dp
    Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
        Row {
            Spacer(Modifier.width(labelWidth))
            MONTH_LETTERS.forEachIndexed { i, l ->
                Text(
                    l, Modifier.weight(1f), textAlign = TextAlign.Center, style = MaterialTheme.typography.labelSmall,
                    fontWeight = if (i + 1 == currentMonth) FontWeight.Bold else FontWeight.Normal,
                    color = if (i + 1 == currentMonth) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
        }
        GridRow("Binnen", Icons.Outlined.House, indoor, MaterialTheme.colorScheme.primary, currentMonth, labelWidth)
        GridRow("Buiten", Icons.Outlined.Yard, outdoor, MaterialTheme.colorScheme.secondary, currentMonth, labelWidth)
    }
}

@Composable
private fun GridRow(label: String, icon: androidx.compose.ui.graphics.vector.ImageVector, range: MonthRange?, on: Color, currentMonth: Int, labelWidth: androidx.compose.ui.unit.Dp) {
    Row(verticalAlignment = Alignment.CenterVertically) {
        Row(Modifier.width(labelWidth), verticalAlignment = Alignment.CenterVertically) {
            Icon(icon, null, Modifier.size(16.dp), tint = MaterialTheme.colorScheme.onSurfaceVariant)
            Spacer(Modifier.width(4.dp))
            Text(label, style = MaterialTheme.typography.labelMedium)
        }
        for (m in 1..12) {
            val filled = range?.contains(m) == true
            Box(
                Modifier.weight(1f).padding(horizontal = 1.5.dp).height(18.dp).clip(RoundedCornerShape(4.dp))
                    .background(if (filled) on else MaterialTheme.colorScheme.surfaceContainerHigh)
                    .then(if (m == currentMonth) Modifier.border(1.5.dp, MaterialTheme.colorScheme.onSurface, RoundedCornerShape(4.dp)) else Modifier),
            )
        }
    }
}

// ---------- Formulier ----------

/** Controle van het zaadformulier. Geeft een foutmelding terug, of null als alles klopt. */
fun seedFormError(name: String, quantity: String, expiry: String, indoor: Pair<Int?, Int?>, outdoor: Pair<Int?, Int?>): String? = when {
    name.isBlank() -> "Vul een naam in."
    quantity.isNotBlank() && parseNumber(quantity) == null -> "Hoeveelheid moet een getal zijn, bijvoorbeeld 2 of 1,5."
    expiry.isNotBlank() && expiry.toIntOrNull()?.takeIf { it in 1900..2200 } == null -> "Houdbaar tot moet een jaartal zijn, bijvoorbeeld 2027."
    (indoor.first == null) != (indoor.second == null) -> "Vul bij zaaitijd binnen zowel de begin- als de eindmaand in, of laat beide leeg."
    (outdoor.first == null) != (outdoor.second == null) -> "Vul bij zaaitijd buiten zowel de begin- als de eindmaand in, of laat beide leeg."
    else -> null
}

fun parseNumber(text: String): Double? = text.trim().replace(',', '.').toDoubleOrNull()?.takeIf { it >= 0 }

@Composable
fun SeedFormScreen(nav: NavController, id: String?, motherId: String?) {
    val app = app()
    val dao = app.db.dao()
    val context = LocalContext.current
    val scope = rememberCoroutineScope()

    var loaded by rememberSaveable { mutableStateOf(id == null && motherId == null) }
    var name by rememberSaveable { mutableStateOf("") }
    var kind by rememberSaveable { mutableStateOf("") }
    var quantity by rememberSaveable { mutableStateOf("1") }
    var unit by rememberSaveable { mutableStateOf(QuantityUnit.ZAKJE) }
    var expiry by rememberSaveable { mutableStateOf((LocalDate.now().year + 2).toString()) }
    var storage by rememberSaveable { mutableStateOf("") }
    var notes by rememberSaveable { mutableStateOf("") }
    var inStart by rememberSaveable { mutableStateOf<Int?>(null) }
    var inEnd by rememberSaveable { mutableStateOf<Int?>(null) }
    var outStart by rememberSaveable { mutableStateOf<Int?>(null) }
    var outEnd by rememberSaveable { mutableStateOf<Int?>(null) }
    var photo by rememberSaveable { mutableStateOf<String?>(null) }
    var picked by rememberSaveable { mutableStateOf<String?>(null) }
    var error by rememberSaveable { mutableStateOf<String?>(null) }
    var saving by remember { mutableStateOf(false) }

    LaunchedEffect(id) {
        if (loaded) return@LaunchedEffect
        id?.let { dao.getSeed(it) }?.let { s ->
            name = s.name; kind = s.kind; quantity = s.quantity?.let(::fmtNumber).orEmpty(); unit = s.unit
            expiry = s.expiryYear?.toString().orEmpty(); storage = s.storage; notes = s.notes
            inStart = s.indoorStart; inEnd = s.indoorEnd; outStart = s.outdoorStart; outEnd = s.outdoorEnd
            photo = s.photo
        }
        loaded = true
    }

    fun save() {
        error = seedFormError(name, quantity, expiry, inStart to inEnd, outStart to outEnd)
        if (error != null || saving) return
        saving = true
        scope.launch {
            try {
                val newPhoto = picked?.let { withContext(Dispatchers.IO) { app.photos.save(context, Uri.parse(it)) } }
                val base = id?.let { dao.getSeed(it) } ?: Seed(name = "")
                dao.upsertSeed(
                    base.copy(
                        name = name.trim(), kind = kind.trim(), quantity = parseNumber(quantity), unit = unit,
                        expiryYear = expiry.trim().toIntOrNull(), storage = storage.trim(), notes = notes.trim(),
                        indoorStart = inStart, indoorEnd = inEnd, outdoorStart = outStart, outdoorEnd = outEnd,
                        photo = newPhoto ?: photo,
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
        title = if (id != null) "Zaad bewerken" else "Nieuw zaad",
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
            PhotoPickerField(name, photo, picked) { picked = it }
            Field("Naam", name, { name = it }, placeholder = "Bijvoorbeeld Tomaat ‘Moneymaker’")
            Field("Soort", kind, { kind = it }, placeholder = "Tomaat, kruid, bol...")
            Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                Field("Hoeveelheid", quantity, { quantity = it }, Modifier.weight(1f), KeyboardType.Decimal)
                DropdownField("Eenheid", QuantityUnit.entries, unit, { it.plural }, { unit = it }, Modifier.weight(1f))
            }
            Hint("Een schatting is genoeg. De voorraad gaat niet automatisch omlaag bij zaaien.")
            Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                Field("Houdbaar tot (jaar)", expiry, { expiry = it }, Modifier.weight(1f), KeyboardType.Number)
                Field("Bewaarplek", storage, { storage = it }, Modifier.weight(1f), placeholder = "Bus 3, kast")
            }
            GroupLabel("Zaaitijd binnen", Icons.Outlined.House)
            Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                MonthField("Van", inStart, { inStart = it }, Modifier.weight(1f))
                MonthField("Tot en met", inEnd, { inEnd = it }, Modifier.weight(1f))
            }
            GroupLabel("Zaaitijd buiten", Icons.Outlined.Yard)
            Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                MonthField("Van", outStart, { outStart = it }, Modifier.weight(1f))
                MonthField("Tot en met", outEnd, { outEnd = it }, Modifier.weight(1f))
            }
            Hint("Een periode mag over de jaarwissel lopen, bijvoorbeeld november t/m maart.")
            Field("Notities", notes, { notes = it }, singleLine = false)
        }
    }
}

@Composable
fun Field(
    label: String,
    value: String,
    onChange: (String) -> Unit,
    modifier: Modifier = Modifier.fillMaxWidth(),
    keyboard: KeyboardType = KeyboardType.Text,
    placeholder: String? = null,
    singleLine: Boolean = true,
) {
    OutlinedTextField(
        value = value,
        onValueChange = onChange,
        label = { Text(label) },
        placeholder = placeholder?.let { { Text(it) } },
        singleLine = singleLine,
        minLines = if (singleLine) 1 else 3,
        keyboardOptions = KeyboardOptions(keyboardType = keyboard),
        modifier = modifier,
    )
}

@Composable
fun Hint(text: String) = Text(text, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)

@Composable
fun GroupLabel(text: String, icon: androidx.compose.ui.graphics.vector.ImageVector) {
    Row(Modifier.padding(top = 8.dp), verticalAlignment = Alignment.CenterVertically) {
        Icon(icon, null, tint = MaterialTheme.colorScheme.primary, modifier = Modifier.size(20.dp))
        Spacer(Modifier.width(8.dp))
        Text(text, style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.SemiBold)
    }
}
