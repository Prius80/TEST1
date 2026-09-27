package com.revisioni.app.ui

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.revisioni.app.data.*
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.launch

class AppViewModel(application: Application) : AndroidViewModel(application) {

    private val repository = AppRepository(AppDatabase.getInstance(application))

    val vehicles: Flow<List<Vehicle>> = repository.vehicles

    fun vehicle(id: Long): Flow<Vehicle?> = repository.vehicle(id)

    fun maintenancesFor(vehicleId: Long): Flow<List<Maintenance>> =
        repository.maintenancesFor(vehicleId)

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
        viewModelScope.launch { repository.deleteVehicle(vehicle) }
    }

    fun addMaintenance(maintenance: Maintenance) {
        viewModelScope.launch { repository.addMaintenance(maintenance) }
    }

    fun updateMaintenance(maintenance: Maintenance) {
        viewModelScope.launch { repository.updateMaintenance(maintenance) }
    }

    fun deleteMaintenance(maintenance: Maintenance) {
        viewModelScope.launch { repository.deleteMaintenance(maintenance) }
    }

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
