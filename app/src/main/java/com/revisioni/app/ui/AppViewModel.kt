package com.revisioni.app.ui

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.revisioni.app.data.*
import com.revisioni.app.data.backup.BackupManager
import com.revisioni.app.data.backup.ImportResult
import com.revisioni.app.data.backup.VehicleBundle
import com.revisioni.app.data.calendar.CalendarSync
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

class AppViewModel(application: Application) : AndroidViewModel(application) {

    private val repository = AppRepository(AppDatabase.getInstance(application))

    val vehicles: Flow<List<Vehicle>> = repository.vehicles

    fun vehicle(id: Long): Flow<Vehicle?> = repository.vehicle(id)

    fun maintenancesFor(vehicleId: Long): Flow<List<Maintenance>> =
        repository.maintenancesFor(vehicleId)

    val allMaintenances: Flow<List<Maintenance>> = repository.allMaintenances

    fun addVehicle(vehicle: Vehicle, onDone: (Long) -> Unit = {}) {
        viewModelScope.launch {
            val id = repository.addVehicle(vehicle)
            onDone(id)
        }
    }

    fun updateVehicle(vehicle: Vehicle) {
        viewModelScope.launch { repository.updateVehicle(vehicle) }
    }

    fun deleteVehicle(vehicle: Vehicle) {
        viewModelScope.launch {
            // Prima gli eventi che l'app aveva creato nel calendario, poi i dati del veicolo.
            withContext(Dispatchers.IO) { CalendarSync.removeVehicle(getApplication(), vehicle.id) }
            repository.deleteVehicle(vehicle)
        }
    }

    fun addMaintenance(maintenance: Maintenance) {
        viewModelScope.launch { repository.addMaintenance(maintenance) }
    }

    fun updateMaintenance(maintenance: Maintenance) {
        viewModelScope.launch { repository.updateMaintenance(maintenance) }
    }

    fun deleteMaintenance(maintenance: Maintenance) {
        viewModelScope.launch {
            withContext(Dispatchers.IO) {
                CalendarSync.removeMaintenance(getApplication(), maintenance.vehicleId, maintenance.id)
            }
            repository.deleteMaintenance(maintenance)
        }
    }

    // ---- esportazione / importazione ----

    suspend fun loadBundle(vehicleId: Long): List<VehicleBundle> = repository.loadBundle(vehicleId)

    suspend fun loadAllBundles(): List<VehicleBundle> = repository.loadAllBundles()

    /** Legge il testo di un backup e lo importa. Lancia [BackupFormatException] se il file non è valido. */
    suspend fun importBackup(text: String): ImportResult =
        repository.importBundles(BackupManager.parse(text))

    companion object {
        fun factory(application: Application): ViewModelProvider.Factory =
            object : ViewModelProvider.Factory {
                @Suppress("UNCHECKED_CAST")
                override fun <T : ViewModel> create(modelClass: Class<T>): T {
                    return AppViewModel(application) as T
                }
            }
    }
}
