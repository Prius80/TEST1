package com.revisioni.app.data

import androidx.room.*
import kotlinx.coroutines.flow.Flow

@Dao
interface MaintenanceDao {
    @Query("SELECT * FROM maintenances WHERE vehicleId = :vehicleId ORDER BY data DESC")
    fun getForVehicle(vehicleId: Long): Flow<List<Maintenance>>

    @Query("SELECT * FROM maintenances")
    suspend fun getAllOnce(): List<Maintenance>

    @Query("SELECT * FROM maintenances")
    fun getAllFlow(): Flow<List<Maintenance>>

    @Insert
    suspend fun insert(maintenance: Maintenance): Long

    @Update
    suspend fun update(maintenance: Maintenance)

    @Delete
    suspend fun delete(maintenance: Maintenance)
}
