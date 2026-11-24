package com.plantkeeper.ui

import androidx.compose.runtime.getValue
import androidx.compose.runtime.setValue
import androidx.compose.foundation.Canvas
import androidx.compose.animation.core.animateIntAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.layout.aspectRatio
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
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.drawText
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.rememberTextMeasurer
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
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
import kotlin.math.cos
import kotlin.math.sin

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

/** Kop met de huidige maand, de aantallen binnen en buiten, en het zaaiwiel. */
@Composable
private fun SeasonHero(today: LocalDate, indoor: Int, outdoor: Int, perMonth: List<Int>, modifier: Modifier = Modifier) {
    val c = pk
    Column(
        modifier
            .padding(horizontal = Space.l, vertical = Space.xs)
            .fillMaxWidth()
            .clip(leaf(44.dp, 14.dp))
            .background(Brush.linearGradient(c.hero))
            .padding(Space.xl),
    ) {
        Eyebrow("Deze maand · ${today.year}", c.onHero.copy(alpha = 0.8f))
        Text(MONTHS[today.monthValue - 1].capitalized(), style = MaterialTheme.typography.displayMedium, color = c.onHero)
        Spacer(Modifier.height(Space.m))
        Row(verticalAlignment = Alignment.CenterVertically) {
            // Tellers houden hun breedte; het wiel krijgt wat overblijft (max 148dp), zodat niets afbreekt op een smal scherm.
            Column(verticalArrangement = Arrangement.spacedBy(Space.s)) {
                CountChip(Icons.Outlined.House, indoor, "binnen", c.indoorContainer, c.onIndoorContainer)
                CountChip(Icons.Outlined.Yard, outdoor, "buiten", c.outdoorContainer, c.onOutdoorContainer)
            }
            Spacer(Modifier.width(Space.s))
            SowingWheel(perMonth, today.monthValue, Modifier.weight(1f, fill = false).widthIn(max = 148.dp).aspectRatio(1f))
        }
    }
}

@Composable
private fun CountChip(icon: ImageVector, target: Int, label: String, bg: Color, fg: Color) {
    // Telt op vanaf 0 als het scherm verschijnt, en loopt mee als het aantal verandert.
    val n by animateIntAsState(if (rememberStarted()) target else 0, Motion.tween(Motion.LONG), label = "teller")
    Row(
        Modifier.clip(CircleShape).background(bg).padding(start = 6.dp, end = 14.dp, top = 6.dp, bottom = 6.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Box(Modifier.size(30.dp).clip(CircleShape).background(fg.copy(alpha = 0.12f)), contentAlignment = Alignment.Center) {
            Icon(icon, null, tint = fg, modifier = Modifier.size(18.dp))
        }
        Spacer(Modifier.width(8.dp))
        Text("$n", style = MaterialTheme.typography.titleLarge, color = fg)
        Spacer(Modifier.width(5.dp))
        Text(label, style = MaterialTheme.typography.labelLarge, color = fg, maxLines = 1, softWrap = false)
    }
}

/**
 * Zaaiwiel: een ring van 12 maanden, januari bovenaan, met de klok mee.
 * Hoe voller de kleur van een segment, hoe meer zaden je die maand kunt zaaien. De huidige maand is zonnegeel.
 */
@Composable
private fun SowingWheel(perMonth: List<Int>, current: Int, modifier: Modifier) {
    val c = pk
    // Het wiel tekent zich met de klok mee de eerste keer dat het verschijnt.
    val reveal = rememberReveal(delayMs = 150, durationMs = Motion.REVEAL + 200)
    val shown by animateIntAsState(if (reveal > 0.5f) perMonth[current - 1] else 0, Motion.tween(Motion.LONG), label = "zaaibaar")
    val measurer = rememberTextMeasurer()
    val max = (perMonth.maxOrNull() ?: 0).coerceAtLeast(1)
    val labelStyle = TextStyle(fontFamily = Figtree, fontSize = 10.sp, fontWeight = FontWeight.SemiBold, color = c.onHero.copy(alpha = 0.75f))
    val currentStyle = labelStyle.copy(color = c.sun, fontWeight = FontWeight.Bold)
    val description = "Zaaibare zaden per maand: " + perMonth.mapIndexed { i, n -> "${MONTHS[i]} $n" }.joinToString(", ")
    Box(modifier.semantics { contentDescription = description }, contentAlignment = Alignment.Center) {
        Canvas(Modifier.fillMaxSize()) {
            val labelSpace = 14.dp.toPx()
            val radius = size.minDimension / 2 - labelSpace - 6.dp.toPx()
            val center = Offset(size.width / 2, size.height / 2)
            val stroke = 9.dp.toPx()
            val topLeft = Offset(center.x - radius, center.y - radius)
            val arcSize = Size(radius * 2, radius * 2)
            for (i in 0 until 12) {
                // Elk segment komt na het vorige: bij reveal 0..1 lopen de 12 maanden achter elkaar in.
                val local = (reveal * 13f - i).coerceIn(0f, 1f)
                if (local <= 0f) continue
                val frac = perMonth[i].toFloat() / max
                val isNow = i + 1 == current
                // Rustige ring: overal even dik, kleine openingen, voller van kleur naarmate er meer te zaaien valt.
                val start = -90f + i * 30f + 2.5f
                drawArc(c.onHero.copy(alpha = 0.12f), start, 25f * local, false, topLeft, arcSize, style = Stroke(stroke, cap = StrokeCap.Butt))
                val fill = when {
                    isNow -> c.sun
                    perMonth[i] == 0 -> null
                    else -> c.onHero.copy(alpha = 0.3f + 0.45f * frac)
                }
                if (fill != null) drawArc(fill, start, 25f * local, false, topLeft, arcSize, style = Stroke(stroke, cap = StrokeCap.Butt))
                val angle = Math.toRadians((-90.0 + i * 30.0 + 15.0))
                val labelRadius = radius + labelSpace + 2.dp.toPx()
                val layout = measurer.measure(MONTH_LETTERS[i], (if (isNow) currentStyle else labelStyle).let { it.copy(color = it.color.copy(alpha = it.color.alpha * local)) })
                drawText(
                    layout,
                    topLeft = Offset(
                        center.x + (labelRadius * cos(angle)).toFloat() - layout.size.width / 2f,
                        center.y + (labelRadius * sin(angle)).toFloat() - layout.size.height / 2f,
                    ),
                )
            }
        }
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            Text("$shown", style = MaterialTheme.typography.headlineLarge, color = c.onHero)
            Text("zaaibaar", style = MaterialTheme.typography.labelSmall, color = c.onHero.copy(alpha = 0.8f))
        }
    }
}
