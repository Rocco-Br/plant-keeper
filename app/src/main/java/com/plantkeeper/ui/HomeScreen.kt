package com.plantkeeper.ui

import androidx.compose.animation.core.animateDpAsState
import androidx.compose.foundation.clickable
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.layout.wrapContentHeight
import androidx.compose.ui.semantics.selected
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.runtime.getValue
import androidx.compose.runtime.setValue
import androidx.compose.animation.core.animateIntAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Grain
import androidx.compose.material.icons.outlined.House
import androidx.compose.material.icons.outlined.Yard
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.dp
import androidx.navigation.NavController
import com.plantkeeper.data.MonthRange
import com.plantkeeper.data.Seed
import com.plantkeeper.data.indoor
import com.plantkeeper.data.isExpired
import com.plantkeeper.data.outdoor
import com.plantkeeper.data.sowableIn
import java.time.LocalDate
import java.time.LocalTime
import androidx.compose.runtime.remember
import com.plantkeeper.data.SettingKeys
import com.plantkeeper.data.settings
import kotlinx.coroutines.flow.map

@Composable
fun HomeScreen(nav: NavController) {
    val seeds by app().db.dao().seeds().collectAsState(initial = emptyList())
    val today = LocalDate.now()
    val month = today.monthValue
    val (indoor, outdoor) = sowableIn(seeds, month)
    val perMonth = (1..12).map { m -> seeds.count { it.indoor?.contains(m) == true || it.outdoor?.contains(m) == true } }

    val context = androidx.compose.ui.platform.LocalContext.current
    val name by remember { context.settings.data.map { it[SettingKeys.USER_NAME].orEmpty() } }.collectAsState(initial = null)

    PkScaffold(title = "Nu zaaibaar", greeting = name?.let { greeting(LocalTime.now().hour, it) }) { padding ->
        LazyColumn(contentPadding = padding) {
            item { SeasonHero(today, indoor.size, outdoor.size, perMonth, Modifier.appear()) }
            section(true, indoor, month, today.year, { it.indoor!! }, nav)
            section(false, outdoor, month, today.year, { it.outdoor!! }, nav)
        }
    }
}

private fun androidx.compose.foundation.lazy.LazyListScope.section(
    isIndoor: Boolean,
    seeds: List<Seed>,
    month: Int,
    year: Int,
    range: (Seed) -> MonthRange,
    nav: NavController,
) {
    val title = if (isIndoor) "Binnen" else "Buiten"
    item(key = "head-$title") {
        val c = pk
        val (bg, fg) = if (isIndoor) c.indoorContainer to c.onIndoorContainer else c.outdoorContainer to c.onOutdoorContainer
        SectionTitle(
            title, if (isIndoor) Icons.Outlined.House else Icons.Outlined.Yard, fg,
            subtitle = if (isIndoor) "Op de vensterbank of in de kas" else "In de volle grond",
            container = bg, large = true,
            modifier = Modifier.animateItem().appear(1).padding(start = Space.l + Space.xs, end = Space.l + Space.xs, top = Space.xl, bottom = Space.s),
        ) { Label(count(seeds.size, "zaad", "zaden"), bg to fg) }
    }
    if (seeds.isEmpty()) {
        item(key = "empty-$title") {
            EmptyState("Niets om ${title.lowercase()} te zaaien in ${MONTHS[month - 1]}.", Mood.SLEEPY, Modifier.animateItem())
        }
    }
    itemsIndexed(seeds, key = { _, it -> "$title-${it.id}" }) { index, seed ->
        val r = range(seed)
        val expired = seed.isExpired(year)
        val lastMonth = !r.isWholeYear && r.end == month
        ListRow(
            title = seed.name,
            subtitle = "${qtyText(seed.quantity, seed.unit)} · ${if (r.isWholeYear) "hele jaar" else "t/m ${MONTHS_SHORT[r.end - 1]}"}",
            leading = { Thumb(seed.name, seed.photo, Icons.Outlined.Grain) },
            onClick = { nav.navigate(Routes.seed(seed.id)) },
            modifier = Modifier.animateItem().appear(index + 2),
            trailing = {
                if (expired || lastMonth) {
                    Column(horizontalAlignment = Alignment.End, verticalArrangement = Arrangement.spacedBy(Space.xs)) {
                        if (expired) ExpiredLabel()
                        if (lastMonth) Label("laatste maand", Tone.Info)
                    }
                } else {
                    Chevron()
                }
            },
        )
    }
}

/** Kop van het startscherm: de maand, wat je binnen en buiten kunt zaaien, en een jaarbalk met zaaibare zaden per maand. */
@Composable
private fun SeasonHero(today: LocalDate, indoor: Int, outdoor: Int, perMonth: List<Int>, modifier: Modifier = Modifier) {
    PkCard(modifier.padding(horizontal = Space.l, vertical = Space.xs)) {
        Column {
            Eyebrow("Deze maand · ${today.year}")
            Text(MONTHS[today.monthValue - 1].capitalized(), style = MaterialTheme.typography.headlineLarge)
        }
        Row(horizontalArrangement = Arrangement.spacedBy(Space.s)) {
            StatTile(Icons.Outlined.House, indoor, "binnen", pk.indoorContainer, pk.onIndoorContainer, Modifier.weight(1f))
            StatTile(Icons.Outlined.Yard, outdoor, "buiten", pk.outdoorContainer, pk.onOutdoorContainer, Modifier.weight(1f))
        }
        MonthStrip(perMonth, today.monthValue)
    }
}

/** Groot getal met icoon en label in een gekleurde tegel; telt op vanaf 0 als het scherm verschijnt. */
@Composable
private fun StatTile(icon: ImageVector, target: Int, label: String, bg: Color, fg: Color, modifier: Modifier) {
    val n by animateIntAsState(if (rememberStarted()) target else 0, Motion.tween(Motion.LONG), label = "teller")
    Row(
        modifier.clip(MaterialTheme.shapes.medium).background(bg).padding(horizontal = Space.m + Space.xs, vertical = Space.m),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Icon(icon, null, tint = fg, modifier = Modifier.size(22.dp))
        Spacer(Modifier.width(Space.s))
        Text("$n", style = MaterialTheme.typography.headlineSmall, color = fg)
        Spacer(Modifier.width(6.dp))
        Text(label, style = MaterialTheme.typography.labelLarge, color = fg, maxLines = 1, overflow = TextOverflow.Ellipsis)
    }
}

/**
 * Jaarbalk: per maand een staafje, hoger naarmate je meer kunt zaaien, met de maandletter eronder.
 * De [highlight]-maand staat in de hoofdkleur. Met [onMonthClick] kun je een maand aantikken (rondleiding).
 */
@Composable
fun MonthStrip(perMonth: List<Int>, highlight: Int, modifier: Modifier = Modifier, onMonthClick: ((Int) -> Unit)? = null) {
    val reveal = rememberReveal(delayMs = 150)
    val max = (perMonth.maxOrNull() ?: 0).coerceAtLeast(1)
    val description = "Zaaibare zaden per maand: " + perMonth.mapIndexed { i, n -> "${MONTHS[i]} $n" }.joinToString(", ")
    Row(
        modifier.fillMaxWidth().then(if (onMonthClick == null) Modifier.semantics { contentDescription = description } else Modifier),
        verticalAlignment = Alignment.Bottom,
    ) {
        for (i in 0 until 12) {
            val m = i + 1
            val now = m == highlight
            // Staafjes groeien na elkaar omhoog, van januari naar december.
            val local = (reveal * 1.6f - i * 0.05f).coerceIn(0f, 1f)
            val height by animateDpAsState(
                if (perMonth[i] == 0) 6.dp else 14.dp + 34.dp * (perMonth[i].toFloat() / max),
                Motion.gentle(), label = "staaf",
            )
            Column(
                Modifier
                    .weight(1f)
                    .then(
                        if (onMonthClick != null) Modifier.clickable(onClickLabel = "Kies ${MONTHS[i]}") { onMonthClick(m) }
                            .semantics { contentDescription = "${MONTHS[i]}: ${perMonth[i]} zaaibaar"; selected = now }
                        else Modifier,
                    ),
                horizontalAlignment = Alignment.CenterHorizontally,
            ) {
                Box(
                    Modifier
                        .padding(horizontal = 3.dp)
                        .fillMaxWidth()
                        .height(height * local)
                        .clip(RoundedCornerShape(6.dp))
                        .background(
                            when {
                                now -> MaterialTheme.colorScheme.primary
                                perMonth[i] == 0 -> MaterialTheme.colorScheme.surfaceContainerHigh
                                else -> MaterialTheme.colorScheme.primary.copy(alpha = 0.25f + 0.3f * perMonth[i] / max)
                            },
                        ),
                )
                Spacer(Modifier.height(Space.xs))
                Text(
                    MONTH_LETTERS[i],
                    style = MaterialTheme.typography.labelMedium,
                    color = if (now) MaterialTheme.colorScheme.onPrimary else MaterialTheme.colorScheme.onSurfaceVariant,
                    textAlign = TextAlign.Center,
                    modifier = Modifier.size(22.dp).clip(CircleShape).background(if (now) MaterialTheme.colorScheme.primary else Color.Transparent).wrapContentHeight(),
                )
            }
        }
    }
}
