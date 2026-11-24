package com.plantkeeper.ui

import androidx.activity.compose.BackHandler
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.animateDpAsState
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.systemBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.outlined.ArrowForward
import androidx.compose.material.icons.outlined.Explore
import androidx.compose.material.icons.outlined.Grain
import androidx.compose.material.icons.outlined.House
import androidx.compose.material.icons.outlined.LocalFlorist
import androidx.compose.material.icons.outlined.Lock
import androidx.compose.material.icons.outlined.Notifications
import androidx.compose.material.icons.outlined.NotificationsOff
import androidx.compose.material.icons.outlined.WbSunny
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
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
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.plantkeeper.data.MonthRange
import com.plantkeeper.data.PlantStatus
import com.plantkeeper.data.QuantityUnit
import kotlinx.coroutines.launch
import java.time.LocalDate
import kotlin.math.PI
import kotlin.math.sin

private const val DemoSeed = "Tomaat ‘Moneymaker’"
private const val Steps = 7

/**
 * Rondleiding langs alle onderdelen, stap voor stap met iets om zelf te proberen. De voorbeelden gebruiken
 * de echte onderdelen van de app maar slaan niets op. Swipen of "Volgende"; "Overslaan" kan altijd.
 */
@Composable
fun TourScreen(onDone: () -> Unit) {
    val pager = rememberPagerState { Steps }
    val scope = rememberCoroutineScope()
    val today = LocalDate.now()

    // Gedeelde voorbeeldstaat, zodat wat je in de ene stap kiest terugkomt in de volgende.
    var unit by rememberSaveable { mutableStateOf(QuantityUnit.ZAKJE) }
    var start by rememberSaveable { mutableStateOf<Int?>(3) }
    var end by rememberSaveable { mutableStateOf<Int?>(5) }
    var month by rememberSaveable { mutableIntStateOf(today.monthValue) }
    var status by rememberSaveable { mutableStateOf(PlantStatus.GEZAAID) }
    var reminder by rememberSaveable { mutableStateOf(false) }
    val range = if (start != null && end != null) MonthRange(start!!, end!!) else null

    BackHandler(pager.currentPage > 0) { scope.launch { pager.animateScrollToPage(pager.currentPage - 1) } }

    Column(Modifier.fillMaxSize().background(MaterialTheme.colorScheme.surface).systemBarsPadding()) {
        Row(Modifier.padding(start = Space.l + Space.xs, end = Space.s, top = Space.s), verticalAlignment = Alignment.CenterVertically) {
            TourProgress(pager.currentPage, Modifier.weight(1f))
            PkButton("Overslaan", onDone, kind = ButtonKind.Text)
        }
        HorizontalPager(pager, Modifier.weight(1f), verticalAlignment = Alignment.Top) { page ->
            when (page) {
                0 -> TourPage(page, "Even voorstellen", "Ik ben het potje van Plant Keeper. Ik laat je in een paar stappen zien hoe alles werkt.", mascot = false) {
                    TapMascot()
                }
                1 -> TourPage(page, "Je zaadvoorraad", "Onder Zaden houd je al je zakjes bij: hoeveel je hebt, tot wanneer ze goed zijn en waar ze liggen.", Mood.CURIOUS) {
                    ListRow(
                        DemoSeed, "${qtyText(2.0, unit)} · Bus 3, kast",
                        { Thumb(DemoSeed, null, Icons.Outlined.Grain) }, onClick = null, trailing = {},
                    )
                    TryCard("Probeer het: kies een eenheid") {
                        ChoiceChips(QuantityUnit.entries, unit, { it.plural }, { unit = it })
                    }
                }
                2 -> TourPage(page, "Wanneer zaai je wat?", "Per zaad geef je aan in welke maanden je binnen en buiten kunt zaaien.", Mood.HAPPY) {
                    TryCard("Probeer het: tik de eerste maand, dan de laatste") {
                        MonthRangePicker("Binnen", Icons.Outlined.House, pk.indoor, pk.onIndoor, pk.indoorContainer, start, end) { s, e -> start = s; end = e }
                        SowingGrid(range, null, today.monthValue)
                    }
                }
                3 -> TourPage(page, "Wat kan er nu?", "Het startscherm laat zien wat je deze maand kunt zaaien, en hoeveel per maand van het jaar.", Mood.CURIOUS) {
                    val perMonth = (1..12).map { if (range?.contains(it) == true) 1 else 0 }
                    TryCard("Probeer het: tik op een maand") {
                        Text(MONTHS[month - 1].capitalized(), style = MaterialTheme.typography.headlineSmall)
                        MonthStrip(perMonth, month, onMonthClick = { month = it })
                        if (range?.contains(month) == true) Label("Je tomaat is zaaibaar", Tone.Info, Icons.Outlined.WbSunny)
                        else Hint("In ${MONTHS[month - 1]} valt er voor je tomaat niets te zaaien. Kies je periode in de vorige stap.")
                    }
                }
                4 -> TourPage(page, "Van zaadje tot oogst", "Zaai je iets, dan wordt het een plant. Daar houd je bij hoe hij erbij staat.", Mood.HAPPY) {
                    ListRow(
                        "Tomaat", "${fmtDate(today.toString())} · Vensterbank",
                        { Thumb("Tomaat", null, Icons.Outlined.LocalFlorist) }, onClick = null, trailing = { StatusLabel(status) },
                    )
                    TryCard("Probeer het: hoe staat hij erbij?") {
                        StatusPicker(status) { status = it }
                    }
                }
                5 -> TourPage(page, "Nooit te laat", "Zet een herinnering aan en je krijgt een melding op de eerste dag van de maand waarin je kunt zaaien.", if (reminder) Mood.HAPPY else Mood.SLEEPY) {
                    AnimatedVisibility(
                        reminder,
                        enter = slideInVertically(Motion.pop()) { -it } + fadeIn(Motion.tween()),
                        exit = slideOutVertically(Motion.tween()) { -it } + fadeOut(Motion.tween(Motion.SHORT)),
                    ) { FakeNotification(start ?: today.monthValue) }
                    Box(Modifier.padding(horizontal = Space.l)) {
                        SectionCard(
                            "Herinnering", if (reminder) Icons.Outlined.Notifications else Icons.Outlined.NotificationsOff,
                            subtitle = if (reminder) "Staat aan" else "Probeer het: zet hem aan",
                            trailing = { Switch(reminder, { reminder = it }, Modifier.semantics { contentDescription = "Herinnering" }) },
                        )
                    }
                }
                else -> TourPage(page, "Klaar om te zaaien!", "Dat was het. Veel plezier in je moestuin.", mascot = false) {
                    Mascot(Modifier.size(150.dp).align(Alignment.CenterHorizontally), Mood.HAPPY)
                    Column(Modifier.padding(horizontal = Space.l), verticalArrangement = Arrangement.spacedBy(Space.m)) {
                        Notice(Icons.Outlined.Lock, "Alles staat alleen op je telefoon. Maak af en toe een backup via Meer.")
                        Notice(Icons.Outlined.Explore, "Deze rondleiding vind je altijd terug onder Meer.")
                    }
                }
            }
        }
        val last = pager.currentPage == Steps - 1
        PkButton(
            if (last) "Aan de slag" else "Volgende",
            { if (last) onDone() else scope.launch { pager.animateScrollToPage(pager.currentPage + 1) } },
            Modifier.padding(Space.l),
            icon = Icons.AutoMirrored.Outlined.ArrowForward,
        )
    }
}

/** Voortgang als rij streepjes; de huidige stap is een breder pilletje. */
@Composable
private fun TourProgress(current: Int, modifier: Modifier) {
    Row(modifier.semantics { contentDescription = "Stap ${current + 1} van $Steps" }, horizontalArrangement = Arrangement.spacedBy(6.dp)) {
        repeat(Steps) { i ->
            val width by animateDpAsState(if (i == current) 24.dp else 8.dp, Motion.gentle(), label = "stap")
            val color by animateColorAsState(
                if (i <= current) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.outlineVariant, Motion.tween(), label = "stapkleur",
            )
            Box(Modifier.width(width).height(8.dp).clip(CircleShape).background(color))
        }
    }
}

/** Opbouw van één stap: potje, stapnummer, titel, uitleg en daaronder iets om te proberen. */
@Composable
private fun TourPage(
    index: Int,
    title: String,
    body: String,
    mood: Mood = Mood.HAPPY,
    mascot: Boolean = true,
    demo: @Composable ColumnScope.() -> Unit,
) {
    Column(Modifier.fillMaxSize().verticalScroll(rememberScrollState()).padding(bottom = Space.l), verticalArrangement = Arrangement.spacedBy(Space.m)) {
        Column(Modifier.padding(horizontal = Space.l + Space.xs, vertical = Space.s)) {
            if (mascot) Mascot(Modifier.size(96.dp).align(Alignment.CenterHorizontally), mood)
            Eyebrow("Stap ${index + 1} van $Steps")
            Spacer(Modifier.height(Space.xs))
            Text(title, style = MaterialTheme.typography.headlineMedium)
            Spacer(Modifier.height(Space.s))
            Text(body, style = MaterialTheme.typography.bodyLarge, color = MaterialTheme.colorScheme.onSurfaceVariant)
        }
        Column(Modifier.appear(1), verticalArrangement = Arrangement.spacedBy(Space.m), content = demo)
    }
}

/** Kaart met een uitnodiging om iets te proberen. */
@Composable
private fun TryCard(prompt: String, content: @Composable ColumnScope.() -> Unit) {
    PkCard(Modifier.padding(horizontal = Space.l)) {
        Eyebrow(prompt, MaterialTheme.colorScheme.tertiary)
        content()
    }
}

/** Het potje dat reageert als je erop tikt: een sprongetje en een reactie. */
@Composable
private fun ColumnScope.TapMascot() {
    val scope = rememberCoroutineScope()
    var taps by remember { mutableIntStateOf(0) }
    val hop = remember { Animatable(0f) }
    Box(
        Modifier
            .align(Alignment.CenterHorizontally)
            .size(180.dp)
            .graphicsLayer {
                translationY = -sin(PI * hop.value).toFloat() * 36.dp.toPx()
                rotationZ = sin(2 * PI * hop.value).toFloat() * 8f
            }
            .clickable(interactionSource = null, indication = null, onClickLabel = "Kietel het potje") {
                taps++
                scope.launch { hop.snapTo(0f); hop.animateTo(1f, Motion.tween(Motion.LONG)) }
            },
    ) { Mascot(Modifier.fillMaxSize(), if (taps % 3 == 2) Mood.CURIOUS else Mood.HAPPY) }
    Text(
        when (taps) {
            0 -> "Tik maar eens op me!"
            1 -> "Hihi, dat kietelt!"
            2 -> "Nog een keer?"
            else -> "Oké, genoeg gespeeld. Op naar stap 2!"
        },
        style = MaterialTheme.typography.titleMedium, color = MaterialTheme.colorScheme.primary, textAlign = TextAlign.Center,
        modifier = Modifier.fillMaxWidth().padding(horizontal = Space.l),
    )
}

/** Nagemaakte melding zoals Android hem zou tonen. */
@Composable
private fun FakeNotification(month: Int) {
    Row(
        Modifier
            .padding(horizontal = Space.l)
            .fillMaxWidth()
            .clip(MaterialTheme.shapes.large)
            .background(MaterialTheme.colorScheme.surfaceContainerHighest)
            .padding(Space.m),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Mascot(Modifier.size(40.dp), Mood.HAPPY)
        Spacer(Modifier.width(Space.m))
        Column {
            Text("Plant Keeper · 1 ${MONTHS[month - 1]}", style = MaterialTheme.typography.labelMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
            Text("Tijd om te zaaien!", style = MaterialTheme.typography.titleSmall)
            Text("$DemoSeed kan deze maand binnen.", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
        }
    }
}
