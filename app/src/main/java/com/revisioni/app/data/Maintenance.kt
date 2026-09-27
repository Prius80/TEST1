package com.revisioni.app.data

import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index
import androidx.room.PrimaryKey

@Entity(
    tableName = "maintenances",
    foreignKeys = [
        ForeignKey(
            entity = Vehicle::class,
            parentColumns = ["id"],
            childColumns = ["vehicleId"],
            onDelete = ForeignKey.CASCADE
        )
    ],
    indices = [Index("vehicleId")]
)
data class Maintenance(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val vehicleId: Long,
    val tipo: String,               // es. "Tagliando", "Cambio olio", "Pneumatici"
    val data: Long,                 // epoch millis - quando è stata eseguita
    val km: Int? = null,
    val costo: Double? = null,
    val officina: String = "",
    val note: String = "",
    val prossimaScadenza: Long? = null, // epoch millis - promemoria prossima manutenzione (auto o manuale)
    val intervalloMesi: Int? = null,    // es. 12 -> ricorda ogni 12 mesi da "data"
    val intervalloKm: Int? = null       // es. 15000 -> promemoria informativo ogni tot km
)

/** Tipi di manutenzione più comuni, mostrati come suggerimenti rapidi. */
object TipiManutenzione {
    val comuni = listOf(
        "Tagliando",
        "Cambio olio",
        "Cambio filtri",
        "Pneumatici",
        "Freni (pastiglie/dischi)",
        "Batteria",
        "Cinghia distribuzione",
        "Liquido freni",
        "Liquido refrigerante",
        "Candele",
        "Catena/trasmissione (moto)",
        "Altro"
    )
}
