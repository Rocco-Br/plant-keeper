package com.plantkeeper.ui

import androidx.compose.foundation.Canvas
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
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Bedtime
import androidx.compose.material.icons.outlined.Grain
import androidx.compose.material.icons.outlined.HourglassDisabled
import androidx.compose.material.icons.outlined.House
import androidx.compose.material.icons.outlined.Yard
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
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
            item { SeasonHero(today, indoor.size, outdoor.size, perMonth) }
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
        Row(Modifier.padding(start = 20.dp, end = 20.dp, top = 26.dp, bottom = 8.dp), verticalAlignment = Alignment.CenterVertically) {
            IconBlob(if (isIndoor) Icons.Outlined.House else Icons.Outlined.Yard, bg, fg, 44.dp)
            Spacer(Modifier.width(12.dp))
            Column(Modifier.weight(1f)) {
                Text(title, style = MaterialTheme.typography.headlineSmall)
                Text(
                    if (isIndoor) "Op de vensterbank of in de kas" else "In de volle grond",
                    style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
            Pill(count(seeds.size, "zaad", "zaden"), bg, fg)
        }
    }
    if (seeds.isEmpty()) {
        item(key = "empty-$title") { EmptyState(Icons.Outlined.Bedtime, "Niets om ${title.lowercase()} te zaaien in ${MONTHS[month - 1]}.") }
    }
    items(seeds, key = { "$title-${it.id}" }) { seed ->
        val r = range(seed)
        val expired = seed.isExpired(year)
        val lastMonth = !r.isWholeYear && r.end == month
        ListRow(
            title = seed.name,
            subtitle = "${qtyText(seed.quantity, seed.unit)} · ${if (r.isWholeYear) "hele jaar" else "t/m ${MONTHS_SHORT[r.end - 1]}"}",
            leading = { Thumb(seed.name, seed.photo, Icons.Outlined.Grain) },
            onClick = { nav.navigate(Routes.seed(seed.id)) },
            trailing = {
                if (expired || lastMonth) {
                    Column(horizontalAlignment = Alignment.End, verticalArrangement = Arrangement.spacedBy(4.dp)) {
                        if (expired) Badge("verlopen", Icons.Outlined.HourglassDisabled)
                        if (lastMonth) Badge("laatste maand", warn = false)
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
private fun SeasonHero(today: LocalDate, indoor: Int, outdoor: Int, perMonth: List<Int>) {
    val c = pk
    Column(
        Modifier
            .padding(horizontal = 16.dp, vertical = 6.dp)
            .fillMaxWidth()
            .clip(leaf(44.dp, 14.dp))
            .background(Brush.linearGradient(c.hero))
            .padding(22.dp),
    ) {
        Eyebrow("Deze maand · ${today.year}", c.onHero.copy(alpha = 0.8f))
        Text(MONTHS[today.monthValue - 1].capitalized(), style = MaterialTheme.typography.displayMedium, color = c.onHero)
        Spacer(Modifier.height(12.dp))
        Row(verticalAlignment = Alignment.CenterVertically) {
            Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                CountChip(Icons.Outlined.House, indoor, "binnen", c.indoorContainer, c.onIndoorContainer)
                CountChip(Icons.Outlined.Yard, outdoor, "buiten", c.outdoorContainer, c.onOutdoorContainer)
            }
            SowingWheel(perMonth, today.monthValue)
        }
    }
}

@Composable
private fun CountChip(icon: ImageVector, n: Int, label: String, bg: Color, fg: Color) {
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
        Text(label, style = MaterialTheme.typography.labelLarge, color = fg)
    }
}

/**
 * Zaaiwiel: een ring van 12 maanden, januari bovenaan, met de klok mee.
 * Hoe dikker en lichter een boogje, hoe meer zaden je die maand kunt zaaien. De huidige maand is zonnegeel.
 */
@Composable
private fun SowingWheel(perMonth: List<Int>, current: Int) {
    val c = pk
    val measurer = rememberTextMeasurer()
    val max = (perMonth.maxOrNull() ?: 0).coerceAtLeast(1)
    val labelStyle = TextStyle(fontFamily = Figtree, fontSize = 10.sp, fontWeight = FontWeight.SemiBold, color = c.onHero.copy(alpha = 0.75f))
    val currentStyle = labelStyle.copy(color = c.sun, fontWeight = FontWeight.Bold)
    val description = "Zaaibare zaden per maand: " + perMonth.mapIndexed { i, n -> "${MONTHS[i]} $n" }.joinToString(", ")
    Box(Modifier.size(148.dp).semantics { contentDescription = description }, contentAlignment = Alignment.Center) {
        Canvas(Modifier.fillMaxSize()) {
            val labelSpace = 14.dp.toPx()
            val radius = size.minDimension / 2 - labelSpace - 8.dp.toPx()
            val center = Offset(size.width / 2, size.height / 2)
            for (i in 0 until 12) {
                val frac = perMonth[i].toFloat() / max
                val isNow = i + 1 == current
                val width = (5 + 9 * frac).dp.toPx() + if (isNow) 3.dp.toPx() else 0f
                val color = when {
                    isNow -> c.sun
                    perMonth[i] == 0 -> c.onHero.copy(alpha = 0.16f)
                    else -> c.onHero.copy(alpha = 0.45f + 0.5f * frac)
                }
                drawArc(
                    color = color,
                    startAngle = -90f + i * 30f + 6f,
                    sweepAngle = 18f,
                    useCenter = false,
                    topLeft = Offset(center.x - radius, center.y - radius),
                    size = Size(radius * 2, radius * 2),
                    style = Stroke(width = width, cap = StrokeCap.Round),
                )
                val angle = Math.toRadians((-90.0 + i * 30.0 + 15.0))
                val labelRadius = radius + labelSpace + 2.dp.toPx()
                val layout = measurer.measure(MONTH_LETTERS[i], if (isNow) currentStyle else labelStyle)
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
            Text("${perMonth[current - 1]}", style = MaterialTheme.typography.headlineLarge, color = c.onHero)
            Text("zaaibaar", style = MaterialTheme.typography.labelSmall, color = c.onHero.copy(alpha = 0.8f))
        }
    }
}
