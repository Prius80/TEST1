package com.revisioni.app.data

import androidx.room.Entity
import androidx.room.PrimaryKey

enum class VehicleType {
    AUTO, MOTO
}

@Entity(tableName = "vehicles")
data class Vehicle(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val name: String,
    val type: VehicleType,
    val targa: String = "",
    val marca: String = "",
    val modello: String = "",
    val anno: Int? = null,
    val revisioneScadenza: Long? = null,      // epoch millis
    val assicurazioneScadenza: Long? = null,  // epoch millis
    val bolloScadenza: Long? = null,          // epoch millis (opzionale, utile)
    val note: String = ""
)
