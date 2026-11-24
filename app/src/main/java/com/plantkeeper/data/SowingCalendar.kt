package com.plantkeeper.data

import java.time.LocalDate
import java.time.YearMonth

/** Zaaiperiode van [start] t/m [end] (1 t/m 12). Mag over de jaarwissel lopen, bijvoorbeeld 10 t/m 2. */
data class MonthRange(val start: Int, val end: Int) {
    operator fun contains(month: Int): Boolean =
        if (start <= end) month in start..end else month >= start || month <= end

    val isWholeYear get() = start == 1 && end == 12
}

private fun range(start: Int?, end: Int?) = if (start != null && end != null) MonthRange(start, end) else null

val Seed.indoor get() = range(indoorStart, indoorEnd)
val Seed.outdoor get() = range(outdoorStart, outdoorEnd)

fun Seed.isExpired(currentYear: Int) = expiryYear != null && expiryYear < currentYear

/** Zaden die in [month] binnen respectievelijk buiten gezaaid kunnen worden. */
fun sowableIn(seeds: List<Seed>, month: Int): Pair<List<Seed>, List<Seed>> =
    seeds.filter { it.indoor?.contains(month) == true } to seeds.filter { it.outdoor?.contains(month) == true }

/** Startmaanden van de zaaiperiodes, zonder dubbele (binnen en buiten in dezelfde maand geeft één melding). */
fun Seed.reminderMonths(): Set<Int> = setOfNotNull(indoor?.start, outdoor?.start)

/** Eerstvolgende meldingsdatum (eerste van de startmaand) vanaf [today], of null zonder zaaiperiode. */
fun Seed.nextReminderDate(today: LocalDate): LocalDate? =
    reminderMonths().minOfOrNull { m ->
        val thisYear = LocalDate.of(today.year, m, 1)
        if (thisYear.isBefore(today)) thisYear.plusYears(1) else thisYear
    }

/**
 * Zaden waarvoor een melding moet komen. De dagelijkse controle meldt één keer per maand; is [lastNotified] al
 * de huidige maand, dan niets. Zo komt de melding ook als de controle op de eerste dag is overgeslagen.
 */
fun seedsToNotify(seeds: List<Seed>, today: LocalDate, lastNotified: YearMonth?): List<Seed> {
    val now = YearMonth.from(today)
    if (lastNotified != null && !lastNotified.isBefore(now)) return emptyList()
    return seeds.filter { it.reminder && today.monthValue in it.reminderMonths() }
}
