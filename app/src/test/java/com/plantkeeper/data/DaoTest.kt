package com.plantkeeper.data

import androidx.room.Room
import androidx.test.core.app.ApplicationProvider
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.runTest
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner

@RunWith(RobolectricTestRunner::class)
class DaoTest {
    private lateinit var db: AppDatabase
    private val dao get() = db.dao()

    @Before
    fun setUp() {
        db = Room.inMemoryDatabaseBuilder(ApplicationProvider.getApplicationContext(), AppDatabase::class.java)
            .allowMainThreadQueries().build()
    }

    @After
    fun tearDown() = db.close()

    @Test
    fun seedsSortedByNameAndUpdated() = runTest {
        val b = Seed(name = "basilicum")
        dao.upsertSeed(Seed(name = "Tomaat"))
        dao.upsertSeed(b)
        assertEquals(listOf("basilicum", "Tomaat"), dao.seeds().first().map { it.name })
        dao.upsertSeed(b.copy(name = "Zonnebloem"))
        assertEquals(listOf("Tomaat", "Zonnebloem"), dao.seeds().first().map { it.name })
    }

    @Test
    fun deletingSeedKeepsPlantsWithEmptyLink() = runTest {
        val seed = Seed(name = "Tomaat")
        dao.upsertSeed(seed)
        val p1 = Plant(seedId = seed.id, name = "Tomaat", sowDate = "2026-02-14")
        val p2 = Plant(seedId = seed.id, name = "Tomaat", sowDate = "2026-05-01")
        dao.upsertPlants(listOf(p1, p2))
        assertEquals(2, dao.plantsForSeed(seed.id).first().size)

        dao.deleteSeed(seed.id)

        val plants = dao.allPlants()
        assertEquals(2, plants.size)
        plants.forEach {
            assertNull(it.seedId)
            assertEquals("Tomaat", it.name)
        }
    }

    @Test
    fun updatingSeedDoesNotUnlinkPlants() = runTest {
        val seed = Seed(name = "Tomaat")
        dao.upsertSeed(seed)
        val p = Plant(seedId = seed.id, name = "Tomaat", sowDate = "2026-02-14")
        dao.upsertPlant(p)
        dao.upsertSeed(seed.copy(quantity = 3.0))
        assertEquals(seed.id, dao.getPlant(p.id)!!.seedId)
    }

    @Test
    fun deletingMotherPlantClearsSeedLink() = runTest {
        val plant = Plant(name = "Goudsbloem", sowDate = "2026-04-10")
        dao.upsertPlant(plant)
        val seed = Seed(name = "Goudsbloem (eigen zaad)", motherPlantId = plant.id, seedHarvestDate = "2026-09-02")
        dao.upsertSeed(seed)
        dao.deletePlant(plant.id)
        assertNull(dao.getPlant(plant.id))
        assertNull(dao.getSeed(seed.id)!!.motherPlantId)
    }

    @Test
    fun statusAndReminder() = runTest {
        val seed = Seed(name = "a")
        dao.upsertSeed(seed)
        dao.setReminder(seed.id, true)
        assertEquals(true, dao.getSeed(seed.id)!!.reminder)
        val p = Plant(name = "p", sowDate = "2026-01-01")
        dao.upsertPlant(p)
        dao.setStatus(p.id, PlantStatus.OVERLEDEN)
        assertEquals(PlantStatus.OVERLEDEN, dao.getPlant(p.id)!!.status)
    }

    @Test
    fun importMergeUpdatesAndAdds() = runTest {
        val existing = Seed(name = "Oud")
        val untouched = Seed(name = "Blijft")
        dao.upsertSeeds(listOf(existing, untouched))
        val added = Seed(name = "Nieuw")
        val orphan = Plant(seedId = "bestaat-niet", name = "Wees", sowDate = "2026-01-01")
        dao.import(listOf(existing.copy(name = "Bijgewerkt"), added), listOf(orphan), replace = false)
        assertEquals(listOf("Bijgewerkt", "Blijft", "Nieuw"), dao.allSeeds().map { it.name })
        assertNull(dao.getPlant(orphan.id)!!.seedId)
    }

    @Test
    fun importReplaceWipesFirst() = runTest {
        val old = Seed(name = "Oud")
        dao.upsertSeed(old)
        dao.upsertPlant(Plant(seedId = old.id, name = "Oude plant", sowDate = "2026-01-01"))
        val s = Seed(name = "Nieuw")
        dao.import(listOf(s), listOf(Plant(seedId = s.id, name = "Nieuwe plant", sowDate = "2026-02-01")), replace = true)
        assertEquals(listOf("Nieuw"), dao.allSeeds().map { it.name })
        assertEquals(listOf("Nieuwe plant"), dao.allPlants().map { it.name })
    }
}
