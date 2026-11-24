package com.plantkeeper.backup

import com.plantkeeper.data.PhotoStore
import com.plantkeeper.data.Plant
import com.plantkeeper.data.PlantKeeperDao
import com.plantkeeper.data.Seed
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.Json
import java.io.File
import java.io.InputStream
import java.io.OutputStream
import java.util.zip.ZipEntry
import java.util.zip.ZipInputStream
import java.util.zip.ZipOutputStream

/** Inhoud van data.json. Bij een wijziging van het formaat gaat [schemaVersion] omhoog en komt er een migratie in [Backup.read]. */
@Serializable
data class BackupData(
    val schemaVersion: Int = Backup.SCHEMA_VERSION,
    val exportedAt: String,
    val seeds: List<Seed>,
    val plants: List<Plant>,
)

class BackupException(message: String) : Exception(message)

object Backup {
    const val SCHEMA_VERSION = 1
    private const val DATA = "data.json"
    private const val PHOTOS = "photos/"
    private const val MAX_PHOTO_BYTES = 20L * 1024 * 1024
    private val SAFE_NAME = Regex("[A-Za-z0-9._-]{1,100}")

    private val json = Json { prettyPrint = true; ignoreUnknownKeys = true; encodeDefaults = true }

    fun isSafePhotoName(name: String) = SAFE_NAME.matches(name) && name != "." && name != ".."

    /** Schrijft een ZIP met data.json en de foto's waarnaar de records verwijzen. */
    fun write(out: OutputStream, data: BackupData, photosDir: File) {
        ZipOutputStream(out).use { zip ->
            zip.putNextEntry(ZipEntry(DATA))
            zip.write(json.encodeToString(BackupData.serializer(), data).toByteArray())
            zip.closeEntry()
            val names = (data.seeds.map { it.photo } + data.plants.map { it.photo }).filterNotNull().toSortedSet()
            for (name in names) {
                val f = File(photosDir, name)
                if (!f.isFile) continue
                zip.putNextEntry(ZipEntry(PHOTOS + name))
                f.inputStream().use { it.copyTo(zip) }
                zip.closeEntry()
            }
        }
    }

    /**
     * Leest een backup. Foto's komen in [stagingDir]; de database wordt hier nog niet aangeraakt, zodat een kapotte
     * ZIP niets kan beschadigen. Verwijzingen naar ontbrekende of onveilige fotonamen worden leeggemaakt.
     */
    fun read(input: InputStream, stagingDir: File): BackupData {
        stagingDir.mkdirs()
        var text: String? = null
        ZipInputStream(input).use { zip ->
            while (true) {
                val entry = zip.nextEntry ?: break
                val name = entry.name
                when {
                    entry.isDirectory -> Unit
                    name == DATA -> text = zip.readBytes().decodeToString()
                    name.startsWith(PHOTOS) && isSafePhotoName(name.removePrefix(PHOTOS)) -> {
                        File(stagingDir, name.removePrefix(PHOTOS)).outputStream().use { copyLimited(zip, it) }
                    }
                    // Al het andere (ook paden als ../) wordt genegeerd.
                }
            }
        }
        val raw = text ?: throw BackupException("Dit is geen Plant Keeper-backup: data.json ontbreekt.")
        val data = try {
            json.decodeFromString(BackupData.serializer(), raw)
        } catch (e: Exception) {
            throw BackupException("data.json kon niet gelezen worden.")
        }
        if (data.schemaVersion > SCHEMA_VERSION) {
            throw BackupException("Deze backup komt uit een nieuwere versie van Plant Keeper. Werk de app eerst bij.")
        }
        fun photo(name: String?) = name?.takeIf { isSafePhotoName(it) && File(stagingDir, it).isFile }
        return data.copy(
            seeds = data.seeds.map { it.copy(photo = photo(it.photo)) },
            plants = data.plants.map { it.copy(photo = photo(it.photo)) },
        )
    }

    private fun copyLimited(input: InputStream, out: OutputStream) {
        val buf = ByteArray(64 * 1024)
        var total = 0L
        while (true) {
            val n = input.read(buf)
            if (n < 0) break
            total += n
            if (total > MAX_PHOTO_BYTES) throw BackupException("Een foto in de backup is te groot.")
            out.write(buf, 0, n)
        }
    }

    /**
     * Zet een gelezen backup in de database. Samenvoegen werkt bestaande ids bij en voegt nieuwe toe; vervangen wist
     * eerst alles. Daarna worden foto's waar geen record meer naar verwijst opgeruimd.
     */
    suspend fun apply(dao: PlantKeeperDao, photos: PhotoStore, data: BackupData, stagingDir: File, replace: Boolean) {
        photos.dir.mkdirs()
        stagingDir.listFiles()?.forEach { it.copyTo(File(photos.dir, it.name), overwrite = true) }
        dao.import(data.seeds, data.plants, replace)
        val used = (dao.allSeeds().map { it.photo } + dao.allPlants().map { it.photo }).filterNotNull().toSet()
        photos.dir.listFiles()?.filter { it.name !in used }?.forEach { it.delete() }
        stagingDir.deleteRecursively()
    }
}
