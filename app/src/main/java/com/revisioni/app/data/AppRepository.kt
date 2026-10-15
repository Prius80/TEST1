package com.revisioni.app.data

import androidx.room.withTransaction
import com.revisioni.app.data.backup.ImportResult
import com.revisioni.app.data.backup.VehicleBundle
import com.revisioni.app.data.backup.isSameMaintenance
import com.revisioni.app.data.backup.isSameVehicle
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

    val allMaintenances: Flow<List<Maintenance>> = db.maintenanceDao().getAllFlow()

    suspend fun getAllMaintenancesOnce(): List<Maintenance> = db.maintenanceDao().getAllOnce()

    suspend fun addMaintenance(maintenance: Maintenance): Long =
        db.maintenanceDao().insert(maintenance)

    suspend fun updateMaintenance(maintenance: Maintenance) =
        db.maintenanceDao().update(maintenance)

    suspend fun deleteMaintenance(maintenance: Maintenance) =
        db.maintenanceDao().delete(maintenance)

    // ---- esportazione / importazione ----

    suspend fun loadBundle(vehicleId: Long): List<VehicleBundle> {
        val vehicle = db.vehicleDao().getByIdOnce(vehicleId) ?: return emptyList()
        val maintenances = db.maintenanceDao().getAllOnce()
            .filter { it.vehicleId == vehicleId }
            .sortedByDescending { it.data }
        return listOf(VehicleBundle(vehicle, maintenances))
    }

    suspend fun loadAllBundles(): List<VehicleBundle> {
        val byVehicle = db.maintenanceDao().getAllOnce().groupBy { it.vehicleId }
        return db.vehicleDao().getAllOnce()
            .sortedBy { it.name.lowercase() }
            .map { vehicle ->
                VehicleBundle(vehicle, byVehicle[vehicle.id].orEmpty().sortedByDescending { it.data })
            }
    }

    /**
     * Importa le schede in un'unica transazione: o entra tutto o niente.
     * I veicoli già presenti non vengono modificati, ma ricevono le manutenzioni
     * del file che non hanno ancora, così si può reimportare una scheda aggiornata.
     */
    suspend fun importBundles(bundles: List<VehicleBundle>): ImportResult =
        db.withTransaction {
            val existing = db.vehicleDao().getAllOnce().toMutableList()
            val knownByVehicle: MutableMap<Long, MutableList<Maintenance>> =
                db.maintenanceDao().getAllOnce()
                    .groupBy { it.vehicleId }
                    .mapValues { it.value.toMutableList() }
                    .toMutableMap()

            var vehiclesAdded = 0
            var alreadyPresent = 0
            var maintenancesAdded = 0

            for (bundle in bundles) {
                var target = existing.firstOrNull { isSameVehicle(it, bundle.vehicle) }
                if (target == null) {
                    val newId = db.vehicleDao().insert(bundle.vehicle.copy(id = 0))
                    target = bundle.vehicle.copy(id = newId)
                    // Registrato subito: se il file contiene due volte lo stesso veicolo,
                    // la seconda occorrenza si unisce alla prima invece di duplicarla.
                    existing.add(target)
                    vehiclesAdded++
                } else {
                    alreadyPresent++
                }

                val known = knownByVehicle.getOrPut(target.id) { mutableListOf() }
                for (m in bundle.maintenances) {
                    if (known.none { isSameMaintenance(it, m) }) {
                        val added = m.copy(id = 0, vehicleId = target.id)
                        db.maintenanceDao().insert(added)
                        known.add(added)
                        maintenancesAdded++
                    }
                }
            }
            ImportResult(vehiclesAdded, alreadyPresent, maintenancesAdded)
        }
}
