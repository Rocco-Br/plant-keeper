package com.plantkeeper.data

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test
import java.time.LocalDate

class SeedHarvestTest {
    private val today = LocalDate.of(2026, 11, 24)

    @Test
    fun copiesFromMotherSeed() {
        val mother = Seed(name = "Tomaat", kind = "Tomaat", indoorStart = 2, indoorEnd = 4)
        val plant = Plant(seedId = mother.id, name = "Tomaat", sowDate = "2026-02-14", harvestDate = "2026-08-10")
        val s = seedFromMother(plant, mother, today)
        assertEquals("Tomaat (eigen zaad)", s.name)
        assertEquals("Tomaat", s.kind)
        assertEquals(MonthRange(2, 4), s.indoor)
        assertNull(s.outdoor)
        assertEquals(plant.id, s.motherPlantId)
        assertEquals("2026-08-10", s.seedHarvestDate)
        assertEquals(2029, s.expiryYear)
    }

    @Test
    fun loosePlantWithoutHarvestDate() {
        val plant = Plant(name = "Goudsbloem (eigen zaad)", sowDate = "2026-04-10")
        val s = seedFromMother(plant, null, today)
        assertEquals("Goudsbloem (eigen zaad)", s.name)
        assertEquals("2026-11-24", s.seedHarvestDate)
        assertEquals("", s.kind)
    }
}
