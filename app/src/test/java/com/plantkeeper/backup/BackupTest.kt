package com.plantkeeper.backup

import android.app.Application
import androidx.room.Room
import androidx.test.core.app.ApplicationProvider
import com.plantkeeper.data.AppDatabase
import com.plantkeeper.data.PhotoStore
import com.plantkeeper.data.Plant
import com.plantkeeper.data.PlantStatus
import com.plantkeeper.data.QuantityUnit
import com.plantkeeper.data.Seed
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertThrows
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test
import org.junit.rules.TemporaryFolder
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
import java.io.ByteArrayInputStream
import java.io.ByteArrayOutputStream
import java.io.File
import java.util.zip.ZipEntry
import java.util.zip.ZipInputStream
import java.util.zip.ZipOutputStream

@RunWith(RobolectricTestRunner::class)
@Config(application = Application::class)
class BackupTest {
    @get:Rule val tmp = TemporaryFolder()

    private val seed = Seed(
        name = "Tuinboon", kind = "Peulvrucht", quantity = 1.5, unit = QuantityUnit.BUS, expiryYear = 2028,
        outdoorStart = 11, outdoorEnd = 3, reminder = true, photo = "a.jpg",
    )
    private val plant = Plant(seedId = seed.id, name = "Tuinboon", sowDate = "2026-11-02", status = PlantStatus.ONTKIEMD, photo = "b.jpg")

    private fun photosDir() = tmp.newFolder().also {
        File(it, "a.jpg").writeText("foto a")
        File(it, "b.jpg").writeText("foto b")
        File(it, "los.jpg").writeText("niet gebruikt")
    }

    private fun export(seeds: List<Seed> = listOf(seed), plants: List<Plant> = listOf(plant), dir: File = photosDir()): ByteArray =
        ByteArrayOutputStream().also { Backup.write(it, BackupData(exportedAt = "2026-11-24T10:00:00", seeds = seeds, plants = plants), dir) }.toByteArray()

    private fun zip(vararg entries: Pair<String, String>): ByteArray = ByteArrayOutputStream().also { out ->
        ZipOutputStream(out).use { z -> entries.forEach { (n, c) -> z.putNextEntry(ZipEntry(n)); z.write(c.toByteArray()); z.closeEntry() } }
    }.toByteArray()

    @Test
    fun zipContainsDataJsonWithVersionAndOnlyUsedPhotos() {
        val names = ZipInputStream(ByteArrayInputStream(export())).use { z -> generateSequence { z.nextEntry?.name }.toList() }
        assertEquals(listOf("data.json", "photos/a.jpg", "photos/b.jpg"), names)
    }

    @Test
    fun roundTrip() {
        val staging = tmp.newFolder()
        val data = Backup.read(ByteArrayInputStream(export()), staging)
        assertEquals(1, data.schemaVersion)
        assertEquals(listOf(seed), data.seeds)
        assertEquals(listOf(plant), data.plants)
        assertEquals("foto a", File(staging, "a.jpg").readText())
    }

    @Test
    fun rejectsNewerSchemaAndMissingData() {
        val newer = zip("data.json" to """{"schemaVersion":99,"exportedAt":"x","seeds":[],"plants":[]}""")
        assertThrows(BackupException::class.java) { Backup.read(ByteArrayInputStream(newer), tmp.newFolder()) }
        assertThrows(BackupException::class.java) { Backup.read(ByteArrayInputStream(zip("iets.txt" to "hoi")), tmp.newFolder()) }
        assertThrows(BackupException::class.java) { Backup.read(ByteArrayInputStream(zip("data.json" to "{kapot")), tmp.newFolder()) }
    }

    @Test
    fun ignoresUnsafePathsAndClearsBadPhotoRefs() {
        val staging = tmp.newFolder("staging")
        val json = """{"schemaVersion":1,"exportedAt":"x","seeds":[{"id":"s1","name":"a","photo":"../../databases/x"},{"id":"s2","name":"b","photo":"weg.jpg"}],"plants":[]}"""
        val data = Backup.read(ByteArrayInputStream(zip("data.json" to json, "photos/../evil.jpg" to "x", "../evil2" to "x")), staging)
        assertFalse(File(staging.parentFile, "evil.jpg").exists())
        assertFalse(File(staging.parentFile, "evil2").exists())
        assertNull(data.seeds[0].photo)
        assertNull(data.seeds[1].photo)
    }

    @Test
    fun applyMergeAndReplace() = runTest {
        val db = Room.inMemoryDatabaseBuilder(ApplicationProvider.getApplicationContext(), AppDatabase::class.java).allowMainThreadQueries().build()
        val dao = db.dao()
        val photos = PhotoStore(tmp.newFolder("photos"))
        val local = Seed(name = "Lokaal", photo = "lokaal.jpg")
        File(photos.dir, "lokaal.jpg").writeText("lokaal")
        File(photos.dir, "wees.jpg").writeText("zonder record")
        dao.upsertSeeds(listOf(local, seed.copy(name = "Oude naam", photo = null)))

        val staging = tmp.newFolder("st1")
        Backup.apply(dao, photos, Backup.read(ByteArrayInputStream(export()), staging), staging, replace = false)
        assertEquals(listOf("Lokaal", "Tuinboon"), dao.allSeeds().map { it.name })
        assertEquals(1, dao.allPlants().size)
        assertEquals(setOf("a.jpg", "b.jpg", "lokaal.jpg"), photos.dir.list()!!.toSet())

        val staging2 = tmp.newFolder("st2")
        Backup.apply(dao, photos, Backup.read(ByteArrayInputStream(export()), staging2), staging2, replace = true)
        assertEquals(listOf("Tuinboon"), dao.allSeeds().map { it.name })
        assertEquals(setOf("a.jpg", "b.jpg"), photos.dir.list()!!.toSet())
        assertTrue(!staging2.exists())
        db.close()
    }
}
