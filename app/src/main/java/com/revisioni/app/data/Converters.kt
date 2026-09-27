package com.revisioni.app.data

import androidx.room.TypeConverter

class Converters {
    @TypeConverter
    fun toVehicleType(value: String): VehicleType = VehicleType.valueOf(value)

    @TypeConverter
    fun fromVehicleType(value: VehicleType): String = value.name
}
