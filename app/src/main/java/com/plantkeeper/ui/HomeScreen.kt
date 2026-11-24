package com.plantkeeper.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.WbSunny
import androidx.compose.material.icons.outlined.Bedtime
import androidx.compose.material.icons.outlined.Grain
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
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.navigation.NavController
import com.plantkeeper.data.MonthRange
import com.plantkeeper.data.Seed
import com.plantkeeper.data.indoor
import com.plantkeeper.data.isExpired
import com.plantkeeper.data.outdoor
import com.plantkeeper.data.sowableIn
import java.time.LocalDate

@Composable
fun HomeScreen(nav: NavController) {
    val seeds by app().db.dao().seeds().collectAsState(initial = emptyList())
    val today = LocalDate.now()
    val month = today.monthValue
    val (indoor, outdoor) = sowableIn(seeds, month)
    val perMonth = (1..12).map { m -> seeds.count { it.indoor?.contains(m) == true || it.outdoor?.contains(m) == true } }

    PkScaffold(title = "Nu zaaibaar", eyebrow = "Zaaikalender") { padding ->
        LazyColumn(contentPadding = padding) {
            item { Ribbon(today, indoor.size, outdoor.size, perMonth) }
            section("Binnen", Icons.Outlined.House, indoor, month, today.year, { it.indoor!! }, nav)
            section("Buiten", Icons.Outlined.Yard, outdoor, month, today.year, { it.outdoor!! }, nav)
        }
    }
}

private fun androidx.compose.foundation.lazy.LazyListScope.section(
    title: String,
    icon: ImageVector,
    seeds: List<Seed>,
    month: Int,
    year: Int,
    range: (Seed) -> MonthRange,
    nav: NavController,
) {
    item {
        Row(Modifier.padding(start = 20.dp, end = 20.dp, top = 20.dp, bottom = 4.dp), verticalAlignment = Alignment.CenterVertically) {
            Box(Modifier.size(32.dp).clip(RoundedCornerShape(10.dp)).background(MaterialTheme.colorScheme.primaryContainer), contentAlignment = Alignment.Center) {
                Icon(icon, null, tint = MaterialTheme.colorScheme.onPrimaryContainer, modifier = Modifier.size(20.dp))
            }
            Spacer(Modifier.width(10.dp))
            Text(title, style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold, modifier = Modifier.weight(1f))
            Text("${seeds.size} zaden", style = MaterialTheme.typography.labelLarge, color = MaterialTheme.colorScheme.onSurfaceVariant)
        }
    }
    if (seeds.isEmpty()) {
        item { EmptyState(Icons.Outlined.Bedtime, "Niets om ${title.lowercase()} te zaaien in ${MONTHS[month - 1]}.") }
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
                        if (expired) ExpiredBadge()
                        if (lastMonth) Badge("laatste maand", warn = false)
                    }
                } else {
                    Chevron()
                }
            },
        )
    }
}

/** Kop met de huidige maand en een staafje per maand met het aantal zaaibare zaden. */
@Composable
private fun Ribbon(today: LocalDate, indoor: Int, outdoor: Int, perMonth: List<Int>) {
    val onHero = Color(0xFFF1F8EF)
    val max = perMonth.maxOrNull()?.coerceAtLeast(1) ?: 1
    Column(
        Modifier
            .padding(horizontal = 20.dp, vertical = 8.dp)
            .fillMaxWidth()
            .clip(RoundedCornerShape(24.dp))
            .background(Brush.linearGradient(listOf(Color(0xFF2C6A48), Color(0xFF3F7F55), Color(0xFF7FAA77))))
            .padding(18.dp),
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Column(Modifier.weight(1f)) {
                Text("${MONTHS[today.monthValue - 1].capitalized()} ${today.year}", color = onHero, style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold)
                Text("$indoor binnen · $outdoor buiten", color = onHero.copy(alpha = 0.85f), style = MaterialTheme.typography.bodyMedium)
            }
            Icon(Icons.Filled.WbSunny, null, tint = onHero, modifier = Modifier.size(36.dp))
        }
        Spacer(Modifier.height(14.dp))
        Row(Modifier.fillMaxWidth().height(48.dp), verticalAlignment = Alignment.Bottom) {
            perMonth.forEachIndexed { i, c ->
                Box(
                    Modifier.weight(1f).padding(horizontal = 2.dp)
                        .fillMaxHeight(maxOf(0.08f, c.toFloat() / max))
                        .clip(RoundedCornerShape(topStart = 4.dp, topEnd = 4.dp))
                        .background(if (i + 1 == today.monthValue) onHero else onHero.copy(alpha = 0.35f)),
                )
            }
        }
        Row(Modifier.fillMaxWidth().padding(top = 4.dp)) {
            MONTH_LETTERS.forEachIndexed { i, l ->
                Text(
                    l, Modifier.weight(1f), textAlign = TextAlign.Center, style = MaterialTheme.typography.labelSmall,
                    color = onHero.copy(alpha = if (i + 1 == today.monthValue) 1f else 0.7f),
                    fontWeight = if (i + 1 == today.monthValue) FontWeight.Bold else FontWeight.Normal,
                )
            }
        }
        Text("Zaaibare zaden per maand", color = onHero.copy(alpha = 0.7f), style = MaterialTheme.typography.labelSmall, modifier = Modifier.padding(top = 6.dp))
    }
}
