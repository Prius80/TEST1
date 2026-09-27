package com.revisioni.app.data

import androidx.room.*
import kotlinx.coroutines.flow.Flow

@Dao
interface VehicleDao {
    @Query("SELECT * FROM vehicles ORDER BY name ASC")
    fun getAll(): Flow<List<Vehicle>>

    @Query("SELECT * FROM vehicles WHERE id = :id")
    fun getById(id: Long): Flow<Vehicle?>

    @Query("SELECT * FROM vehicles WHERE id = :id")
    suspend fun getByIdOnce(id: Long): Vehicle?

    @Query("SELECT * FROM vehicles")
    suspend fun getAllOnce(): List<Vehicle>

    @Insert
    suspend fun insert(vehicle: Vehicle): Long

    @Update
    suspend fun update(vehicle: Vehicle)

    @Delete
    suspend fun delete(vehicle: Vehicle)
}
