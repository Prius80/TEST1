package com.revisioni.app.data

import kotlinx.coroutines.flow.Flow

class AppRepository(private val db: AppDatabase) {

    val vehicles: Flow<List<Vehicle>> = db.vehicleDao().getAll()

    fun vehicle(id: Long): Flow<Vehicle?> = db.vehicleDao().getById(id)

    suspend fun getVehicleOnce(id: Long): Vehicle? = db.vehicleDao().getByIdOnce(id)

    suspend fun getAllVehiclesOnce(): List<Vehicle> = db.vehicleDao().getAllOnce()

    suspend fun addVehicle(vehicle: Vehicle): Long = db.vehicleDao().insert(vehicle)

    suspend fun updateVehicle(vehicle: Vehicle) = db.vehicleDao().update(vehicle)

    suspend fun deleteVehicle(vehicle: Vehicle) = db.vehicleDao().delete(vehicle)

    fun maintenancesFor(vehicleId: Long): Flow<List<Maintenance>> =
        db.maintenanceDao().getForVehicle(vehicleId)

    suspend fun getAllMaintenancesOnce(): List<Maintenance> = db.maintenanceDao().getAllOnce()

    suspend fun addMaintenance(maintenance: Maintenance): Long =
        db.maintenanceDao().insert(maintenance)

    suspend fun updateMaintenance(maintenance: Maintenance) =
        db.maintenanceDao().update(maintenance)

    suspend fun deleteMaintenance(maintenance: Maintenance) =
        db.maintenanceDao().delete(maintenance)
}
