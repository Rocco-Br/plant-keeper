package com.plantkeeper.data

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test
import java.time.LocalDate
import java.time.YearMonth

class SowingCalendarTest {
    @Test
    fun normalRange() {
        val r = MonthRange(3, 5)
        assertEquals(listOf(3, 4, 5), (1..12).filter { it in r })
    }

    @Test
    fun rangeOverNewYear() {
        val r = MonthRange(10, 2)
        assertEquals(listOf(1, 2, 10, 11, 12), (1..12).filter { it in r })
    }

    @Test
    fun singleMonthAndWholeYear() {
        assertEquals(listOf(7), (1..12).filter { it in MonthRange(7, 7) })
        assertEquals(12, (1..12).count { it in MonthRange(1, 12) })
        assertTrue(MonthRange(1, 12).isWholeYear)
    }

    @Test
    fun sowableSplitsIndoorAndOutdoor() {
        val bean = Seed(name = "Tuinboon", indoorStart = 1, indoorEnd = 2, outdoorStart = 11, outdoorEnd = 3)
        val garlic = Seed(name = "Knoflook", outdoorStart = 10, outdoorEnd = 11)
        val noPeriod = Seed(name = "Leeg", indoorStart = 5)
        val (inJan, outJan) = sowableIn(listOf(bean, garlic, noPeriod), 1)
        assertEquals(listOf(bean), inJan)
        assertEquals(listOf(bean), outJan)
        val (inNov, outNov) = sowableIn(listOf(bean, garlic, noPeriod), 11)
        assertEquals(emptyList<Seed>(), inNov)
        assertEquals(listOf(bean, garlic), outNov)
    }

    @Test
    fun expired() {
        assertTrue(Seed(name = "a", expiryYear = 2024).isExpired(2026))
        assertFalse(Seed(name = "a", expiryYear = 2026).isExpired(2026))
        assertFalse(Seed(name = "a").isExpired(2026))
    }

    @Test
    fun oneReminderWhenIndoorAndOutdoorStartTogether() {
        val s = Seed(name = "a", indoorStart = 4, indoorEnd = 5, outdoorStart = 4, outdoorEnd = 6, reminder = true)
        assertEquals(setOf(4), s.reminderMonths())
        assertEquals(listOf(s), seedsToNotify(listOf(s), LocalDate.of(2026, 4, 1), YearMonth.of(2026, 3)))
    }

    @Test
    fun notifyOncePerMonthAndOnlyWhenEnabled() {
        val on = Seed(name = "on", outdoorStart = 11, outdoorEnd = 3, reminder = true)
        val off = on.copy(id = newId(), reminder = false)
        val day1 = LocalDate.of(2026, 11, 1)
        assertEquals(listOf(on), seedsToNotify(listOf(on, off), day1, YearMonth.of(2026, 10)))
        assertEquals(emptyList<Seed>(), seedsToNotify(listOf(on), day1.plusDays(1), YearMonth.of(2026, 11)))
        // Controle op de eerste dag gemist: komt alsnog op dag 3.
        assertEquals(listOf(on), seedsToNotify(listOf(on), day1.plusDays(2), YearMonth.of(2026, 10)))
        // Geen melding in een maand die geen startmaand is.
        assertEquals(emptyList<Seed>(), seedsToNotify(listOf(on), LocalDate.of(2026, 12, 1), YearMonth.of(2026, 11)))
    }

    @Test
    fun nextReminderDateWrapsToNextYear() {
        val s = Seed(name = "a", indoorStart = 2, indoorEnd = 4, outdoorStart = 10, outdoorEnd = 11)
        assertEquals(LocalDate.of(2026, 10, 1), s.nextReminderDate(LocalDate.of(2026, 3, 15)))
        assertEquals(LocalDate.of(2027, 2, 1), s.nextReminderDate(LocalDate.of(2026, 11, 24)))
        assertEquals(LocalDate.of(2026, 10, 1), s.nextReminderDate(LocalDate.of(2026, 10, 1)))
        assertEquals(null, Seed(name = "b").nextReminderDate(LocalDate.of(2026, 1, 1)))
    }
}
