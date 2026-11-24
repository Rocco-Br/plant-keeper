package com.plantkeeper.ui

import androidx.compose.runtime.getValue
import androidx.compose.runtime.setValue
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.runtime.derivedStateOf
import androidx.compose.ui.draw.drawWithContent
import androidx.compose.ui.graphics.drawscope.clipRect
import android.net.Uri
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.IntrinsicSize
import androidx.compose.material.icons.automirrored.outlined.KeyboardArrowRight
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.wrapContentHeight
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.CalendarMonth
import androidx.compose.material.icons.outlined.Close
import androidx.compose.material.icons.outlined.Delete
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
import androidx.compose.material.icons.automirrored.outlined.StickyNote2
import androidx.compose.material.icons.outlined.Yard
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
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
import com.plantkeeper.data.seedFromMother
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
    val list = rememberLazyListState()
    val atTop by remember { derivedStateOf { list.firstVisibleItemIndex == 0 } }

    PkScaffold(
        title = "Zaden",
        eyebrow = "${count(seeds.size, "soort", "soorten")} in voorraad",
        fab = { AddFab("Zaad", expanded = atTop) { nav.navigate(Routes.seedForm()) } },
    ) { padding ->
        Column {
            SearchField(query) { query = it }
            LazyColumn(state = list, contentPadding = padding) {
                if (shown.isEmpty()) {
                    item(key = "leeg") {
                        if (seeds.isEmpty()) EmptyState("Nog geen zaden. Voeg je eerste zaad toe met de knop rechtsonder.", Mood.HAPPY, Modifier.animateItem())
                        else EmptyState("Geen zaden gevonden voor ‘$query’.", Mood.CURIOUS, Modifier.animateItem())
                    }
                }
                itemsIndexed(shown, key = { _, it -> it.id }) { index, seed ->
                    ListRow(
                        title = seed.name,
                        subtitle = listOf(qtyText(seed.quantity, seed.unit), seed.storage.ifBlank { seed.kind }).filter { it.isNotBlank() }.joinToString(" · "),
                        leading = { Thumb(seed.name, seed.photo, Icons.Outlined.Grain) },
                        onClick = { nav.navigate(Routes.seed(seed.id)) },
                        modifier = Modifier.animateItem().appear(index),
                        trailing = { if (seed.isExpired(year)) ExpiredLabel() else Chevron() },
                    )
                }
            }
        }
    }
}

@Composable
fun ExpiredLabel(text: String = "verlopen") = Label(text, Tone.Warn, Icons.Outlined.HourglassDisabled)

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

    if (s == null) {
        Box(Modifier.fillMaxSize().background(MaterialTheme.colorScheme.surface))
    } else {
        DetailScaffold(s.name, s.photo, Icons.Outlined.Grain, onBack = { nav.popBackStack() }, onEdit = { nav.navigate(Routes.seedForm(id)) }) {
            val expired = s.isExpired(today.year)
            DetailTitle(s.kind.ifBlank { "Zaad" }, s.name)
            if (expired) ExpiredLabel("Verlopen sinds ${s.expiryYear}")
            Row(Modifier.height(IntrinsicSize.Min), horizontalArrangement = Arrangement.spacedBy(Space.s)) {
                Fact(
                    Icons.Outlined.Scale, "Voorraad", qtyText(s.quantity, s.unit), Modifier.weight(1f),
                    MaterialTheme.colorScheme.primaryContainer, MaterialTheme.colorScheme.onPrimaryContainer,
                )
                Fact(
                    Icons.Outlined.HourglassBottom, "Houdbaar", s.expiryYear?.let { "t/m $it" } ?: "Onbekend", Modifier.weight(1f),
                    if (expired) MaterialTheme.colorScheme.tertiaryContainer else MaterialTheme.colorScheme.secondaryContainer,
                    if (expired) MaterialTheme.colorScheme.onTertiaryContainer else MaterialTheme.colorScheme.onSecondaryContainer,
                )
                Fact(Icons.Outlined.Inventory2, "Bewaarplek", s.storage.ifBlank { "Onbekend" }, Modifier.weight(1f), pk.indoorContainer, pk.onIndoorContainer)
            }
            MotherLink(nav, s)
            PkButton("Zaaien", { nav.navigate(Routes.plantForm(seed = s.id)) }, icon = Icons.Outlined.LocalFlorist)
            SectionCard("Zaaitijden", Icons.Outlined.CalendarMonth) {
                SowingGrid(s.indoor, s.outdoor, today.monthValue)
            }
            ReminderCard(s, today)
            SectionCard("Gezaaid", Icons.Outlined.LocalFlorist, trailing = { Label("${plants.size}", Tone.Info) }) {
                if (plants.isEmpty()) {
                    Hint("Nog niet gezaaid. Tik op Zaaien om de eerste zaai vast te leggen.")
                }
                plants.forEach { p ->
                    Tile(onClick = { nav.navigate(Routes.plant(p.id)) }) {
                        Column(Modifier.weight(1f)) {
                            Text(fmtDate(p.sowDate), style = MaterialTheme.typography.titleSmall)
                            Text(p.location.ifBlank { "Geen locatie" }, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant, maxLines = 1, overflow = TextOverflow.Ellipsis)
                        }
                        StatusLabel(p.status)
                    }
                }
            }
            if (s.notes.isNotBlank()) {
                SectionCard("Notities", Icons.AutoMirrored.Outlined.StickyNote2) { Text(s.notes, style = MaterialTheme.typography.bodyMedium) }
            }
            PkButton("Zaad verwijderen", { confirmDelete = true }, Modifier.fillMaxWidth(), Icons.Outlined.Delete, ButtonKind.Danger)
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

/** Zaadgewinning: link naar de moederplant waarvan dit zaad gewonnen is. */
@Composable
private fun MotherLink(nav: NavController, seed: Seed) {
    val motherId = seed.motherPlantId ?: return
    val dao = app().db.dao()
    val mother by remember(motherId) { dao.plant(motherId) }.collectAsState(initial = null)
    val m = mother ?: return
    Tile(onClick = { nav.navigate(Routes.plant(m.id)) }, container = MaterialTheme.colorScheme.primaryContainer) {
        Icon(Icons.Outlined.Recycling, null, tint = MaterialTheme.colorScheme.onPrimaryContainer)
        Text(
            "Gewonnen van ${m.name}" + (seed.seedHarvestDate?.let { " op ${fmtDate(it)}" } ?: ""),
            style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onPrimaryContainer, modifier = Modifier.weight(1f),
        )
        Icon(Icons.AutoMirrored.Outlined.KeyboardArrowRight, null, tint = MaterialTheme.colorScheme.onPrimaryContainer)
    }
}

@Composable
private fun ReminderCard(seed: Seed, today: LocalDate) {
    val dao = app().db.dao()
    val scope = rememberCoroutineScope()
    val next = seed.nextReminderDate(today)
    val permission = rememberNotificationPermission()
    SectionCard(
        "Herinnering",
        if (seed.reminder) Icons.Outlined.Notifications else Icons.Outlined.NotificationsOff,
        subtitle = when {
            next == null -> "Geen zaaiperiode ingesteld"
            seed.reminder -> "Melding op ${fmtLongDate(next)}"
            else -> "Uit. Zet aan voor een melding op ${fmtLongDate(next)}"
        },
        trailing = {
            Switch(
                checked = seed.reminder,
                enabled = next != null,
                onCheckedChange = { on ->
                    scope.launch { dao.setReminder(seed.id, on) }
                    if (on && !permission.granted) permission.request()
                },
                modifier = Modifier.semantics { contentDescription = "Herinnering" },
            )
        },
    )
}

/** Jaarbalk: 12 maanden, binnen in terracotta en buiten in bladgroen, de huidige maand met een zonnetje erboven. */
@OptIn(ExperimentalLayoutApi::class)
@Composable
fun SowingGrid(indoor: MonthRange?, outdoor: MonthRange?, currentMonth: Int) {
    Column(verticalArrangement = Arrangement.spacedBy(Space.s)) {
        Row {
            MONTH_LETTERS.forEachIndexed { i, l ->
                val now = i + 1 == currentMonth
                Box(Modifier.weight(1f), contentAlignment = Alignment.Center) {
                    Text(
                        l,
                        style = MaterialTheme.typography.labelMedium,
                        color = if (now) MaterialTheme.colorScheme.onPrimary else MaterialTheme.colorScheme.onSurfaceVariant,
                        textAlign = TextAlign.Center,
                        modifier = Modifier
                            .size(22.dp)
                            .clip(CircleShape)
                            .background(if (now) MaterialTheme.colorScheme.primary else Color.Transparent)
                            .wrapContentHeight(),
                    )
                }
            }
        }
        // De balken vegen van januari naar december in beeld, buiten iets na binnen.
        val wipe = rememberReveal(delayMs = 200)
        GridRow(indoor, pk.indoor, currentMonth, (wipe * 1.15f).coerceAtMost(1f))
        GridRow(outdoor, pk.outdoor, currentMonth, ((wipe - 0.13f) * 1.15f).coerceIn(0f, 1f))
        FlowRow(horizontalArrangement = Arrangement.spacedBy(Space.s), verticalArrangement = Arrangement.spacedBy(Space.s)) {
            Label("Binnen ${periodText(indoor)}", pk.indoorContainer to pk.onIndoorContainer, Icons.Outlined.House)
            Label("Buiten ${periodText(outdoor)}", pk.outdoorContainer to pk.onOutdoorContainer, Icons.Outlined.Yard)
        }
    }
}

/** Eén rij van de jaarbalk; aaneengesloten maanden vormen één doorlopende pil. */
@Composable
private fun GridRow(range: MonthRange?, on: Color, currentMonth: Int, reveal: Float = 1f) {
    Row(Modifier.height(26.dp).drawWithContent { clipRect(right = size.width * reveal) { this@drawWithContent.drawContent() } }) {
        for (m in 1..12) {
            val filled = range?.contains(m) == true
            val prev = range?.contains(if (m == 1) 12 else m - 1) == true && m != 1
            val next = range?.contains(if (m == 12) 1 else m + 1) == true && m != 12
            val shape = RoundedCornerShape(
                topStart = if (prev) 0.dp else 13.dp, bottomStart = if (prev) 0.dp else 13.dp,
                topEnd = if (next) 0.dp else 13.dp, bottomEnd = if (next) 0.dp else 13.dp,
            )
            Box(
                Modifier
                    .weight(1f)
                    .fillMaxHeight()
                    .padding(start = if (filled && prev) 0.dp else 1.5.dp, end = if (filled && next) 0.dp else 1.5.dp)
                    .clip(if (filled) shape else CircleShape)
                    .background(
                        when {
                            filled -> on
                            m == currentMonth -> MaterialTheme.colorScheme.primary.copy(alpha = 0.18f)
                            else -> MaterialTheme.colorScheme.surfaceContainerHigh
                        },
                    ),
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
    var mother by rememberSaveable { mutableStateOf(motherId) }
    var harvestDate by rememberSaveable { mutableStateOf<String?>(null) }
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
        val source = if (id != null) dao.getSeed(id) else motherId?.let { dao.getPlant(it) }?.let { p ->
            seedFromMother(p, p.seedId?.let { dao.getSeed(it) }, LocalDate.now())
        }
        source?.let { s ->
            name = s.name; kind = s.kind; quantity = s.quantity?.let(::fmtNumber).orEmpty(); unit = s.unit
            expiry = s.expiryYear?.toString().orEmpty(); storage = s.storage; notes = s.notes
            inStart = s.indoorStart; inEnd = s.indoorEnd; outStart = s.outdoorStart; outEnd = s.outdoorEnd
            photo = s.photo; mother = s.motherPlantId; harvestDate = s.seedHarvestDate
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
                        photo = newPhoto ?: photo, motherPlantId = mother, seedHarvestDate = harvestDate,
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
        title = if (id != null) "Zaad bewerken" else if (motherId != null) "Zaad winnen" else "Nieuw zaad",
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
            if (mother != null) {
                SectionCard("Zaadgewinning", Icons.Outlined.Recycling) {
                    val motherName by remember(mother) { dao.plant(mother!!) }.collectAsState(initial = null)
                    motherName?.let { Hint("Gekoppeld aan moederplant ${it.name}") }
                    DateField("Zaad geoogst op", harvestDate, { harvestDate = it }, Modifier.fillMaxWidth(), clearable = true)
                }
            }
            SectionCard("Over dit zaad", Icons.Outlined.Grain) {
                Field("Naam", name, { name = it }, placeholder = "Bijvoorbeeld Tomaat ‘Moneymaker’")
                Field("Soort", kind, { kind = it }, placeholder = "Tomaat, kruid, bol...")
            }
            SectionCard("Voorraad", Icons.Outlined.Scale) {
                Field("Hoeveelheid", quantity, { quantity = it }, keyboard = KeyboardType.Decimal, imeAction = ImeAction.Done)
                ChoiceChips(QuantityUnit.entries, unit, { it.plural }, { unit = it })
                Hint("Een schatting is genoeg. De voorraad gaat niet automatisch omlaag bij zaaien.")
            }
            SectionCard("Bewaren", Icons.Outlined.Inventory2, tint = pk.indoor) {
                Row(horizontalArrangement = Arrangement.spacedBy(Space.s)) {
                    Field("Houdbaar tot", expiry, { expiry = it }, Modifier.weight(1f), KeyboardType.Number, placeholder = "2027")
                    Field("Bewaarplek", storage, { storage = it }, Modifier.weight(1.4f), placeholder = "Bus 3, kast", imeAction = ImeAction.Done)
                }
            }
            SectionCard("Zaaitijden", Icons.Outlined.CalendarMonth) {
                MonthRangePicker("Binnen", Icons.Outlined.House, pk.indoor, pk.onIndoor, pk.indoorContainer, inStart, inEnd) { s, e -> inStart = s; inEnd = e }
                Spacer(Modifier.height(Space.xs))
                MonthRangePicker("Buiten", Icons.Outlined.Yard, pk.outdoor, pk.onOutdoor, pk.outdoorContainer, outStart, outEnd) { s, e -> outStart = s; outEnd = e }
                Hint("Een periode mag over de jaarwissel lopen: tik eerst november, dan maart.")
            }
            SectionCard("Notities", Icons.AutoMirrored.Outlined.StickyNote2) {
                Field("Notities", notes, { notes = it }, singleLine = false)
            }
            Spacer(Modifier.height(Space.xl))
        }
    }
}
