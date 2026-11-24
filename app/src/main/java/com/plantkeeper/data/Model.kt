package com.plantkeeper.data

import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index
import androidx.room.PrimaryKey
import kotlinx.serialization.Serializable
import java.util.UUID

fun newId(): String = UUID.randomUUID().toString()

enum class QuantityUnit(val singular: String, val plural: String) {
    ZAKJE("zakje", "zakjes"),
    BUS("bus", "bussen"),
    BOLLEN("bol", "bollen"),
    GRAM("gram", "gram"),
    STUKS("stuks", "stuks"),
}

enum class PlantStatus(val label: String) {
    GEZAAID("gezaaid"),
    ONTKIEMD("ontkiemd"),
    UITGEPLANT("uitgeplant"),
    GEOOGST("geoogst"),
    MISLUKT("mislukt"),
    OVERLEDEN("overleden"),
}

/** Maanden zijn 1 t/m 12. Datums zijn ISO-strings (yyyy-MM-dd). Foto's zijn bestandsnamen in de fotomap. */
@Serializable
@Entity(tableName = "seeds")
data class Seed(
    @PrimaryKey val id: String = newId(),
    val name: String,
    val kind: String = "",
    val quantity: Double? = null,
    val unit: QuantityUnit = QuantityUnit.ZAKJE,
    val expiryYear: Int? = null,
    val storage: String = "",
    val notes: String = "",
    val photo: String? = null,
    val indoorStart: Int? = null,
    val indoorEnd: Int? = null,
    val outdoorStart: Int? = null,
    val outdoorEnd: Int? = null,
    val reminder: Boolean = false,
    val motherPlantId: String? = null,
    val seedHarvestDate: String? = null,
)

@Serializable
@Entity(
    tableName = "plants",
    foreignKeys = [ForeignKey(
        entity = Seed::class,
        parentColumns = ["id"],
        childColumns = ["seedId"],
        onDelete = ForeignKey.SET_NULL,
    )],
    indices = [Index("seedId")],
)
data class Plant(
    @PrimaryKey val id: String = newId(),
    val seedId: String? = null,
    val name: String,
    val sowDate: String,
    val location: String = "",
    val status: PlantStatus = PlantStatus.GEZAAID,
    val notes: String = "",
    val photo: String? = null,
    val harvestDate: String? = null,
    val harvestText: String = "",
)

/**
 * Zaadgewinning: een nieuw zaad-record gekoppeld aan de moederplant. Soort en zaaitijden komen van het zaad
 * van de moederplant (als dat er is), de oogstdatum van de plant of anders vandaag.
 */
fun seedFromMother(plant: Plant, motherSeed: Seed?, today: java.time.LocalDate): Seed = Seed(
    name = plant.name.removeSuffix(" (eigen zaad)") + " (eigen zaad)",
    kind = motherSeed?.kind.orEmpty(),
    quantity = 1.0,
    unit = QuantityUnit.ZAKJE,
    expiryYear = today.year + 3,
    indoorStart = motherSeed?.indoorStart, indoorEnd = motherSeed?.indoorEnd,
    outdoorStart = motherSeed?.outdoorStart, outdoorEnd = motherSeed?.outdoorEnd,
    motherPlantId = plant.id,
    seedHarvestDate = plant.harvestDate ?: today.toString(),
)
