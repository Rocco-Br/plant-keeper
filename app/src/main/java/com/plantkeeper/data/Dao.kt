package com.plantkeeper.data

import androidx.room.Dao
import androidx.room.Query
import androidx.room.Transaction
import androidx.room.Upsert
import kotlinx.coroutines.flow.Flow

@Dao
interface PlantKeeperDao {
    @Query("SELECT * FROM seeds ORDER BY name COLLATE NOCASE")
    fun seeds(): Flow<List<Seed>>

    @Query("SELECT * FROM seeds WHERE id = :id")
    fun seed(id: String): Flow<Seed?>

    @Query("SELECT * FROM seeds ORDER BY name COLLATE NOCASE")
    suspend fun allSeeds(): List<Seed>

    @Query("SELECT * FROM seeds WHERE id = :id")
    suspend fun getSeed(id: String): Seed?

    @Upsert
    suspend fun upsertSeeds(seeds: List<Seed>)

    suspend fun upsertSeed(seed: Seed) = upsertSeeds(listOf(seed))

    @Query("UPDATE seeds SET reminder = :on WHERE id = :id")
    suspend fun setReminder(id: String, on: Boolean)

    @Query("DELETE FROM seeds WHERE id = :id")
    suspend fun deleteSeed(id: String)

    @Query("SELECT * FROM plants ORDER BY sowDate DESC, name COLLATE NOCASE")
    fun plants(): Flow<List<Plant>>

    @Query("SELECT * FROM plants WHERE seedId = :seedId ORDER BY sowDate DESC")
    fun plantsForSeed(seedId: String): Flow<List<Plant>>

    @Query("SELECT * FROM plants WHERE id = :id")
    fun plant(id: String): Flow<Plant?>

    @Query("SELECT * FROM plants")
    suspend fun allPlants(): List<Plant>

    @Query("SELECT * FROM plants WHERE id = :id")
    suspend fun getPlant(id: String): Plant?

    @Upsert
    suspend fun upsertPlants(plants: List<Plant>)

    suspend fun upsertPlant(plant: Plant) = upsertPlants(listOf(plant))

    @Query("UPDATE plants SET status = :status WHERE id = :id")
    suspend fun setStatus(id: String, status: PlantStatus)

    @Query("DELETE FROM plants WHERE id = :id")
    suspend fun deletePlantRow(id: String)

    // Geen foreign key op motherPlantId: zaad en plant verwijzen naar elkaar, dat maakt import-volgorde lastig.
    @Query("UPDATE seeds SET motherPlantId = NULL WHERE motherPlantId = :plantId")
    suspend fun unlinkMother(plantId: String)

    @Transaction
    suspend fun deletePlant(id: String) {
        unlinkMother(id)
        deletePlantRow(id)
    }

    @Query("DELETE FROM plants")
    suspend fun deleteAllPlants()

    @Query("DELETE FROM seeds")
    suspend fun deleteAllSeeds()

    /** Samenvoegen: bestaande ids bijwerken, nieuwe toevoegen. Bij [replace] eerst alles wissen. */
    @Transaction
    suspend fun import(seeds: List<Seed>, plants: List<Plant>, replace: Boolean) {
        if (replace) {
            deleteAllPlants()
            deleteAllSeeds()
        }
        upsertSeeds(seeds)
        val known = allSeeds().mapTo(HashSet()) { it.id }
        upsertPlants(plants.map { if (it.seedId != null && it.seedId !in known) it.copy(seedId = null) else it })
    }
}
