package com.revisioni.app.data.notification

import android.content.Context
import androidx.work.CoroutineWorker
import androidx.work.WorkerParameters
import com.revisioni.app.data.AppDatabase
import com.revisioni.app.data.AppRepository
import java.text.SimpleDateFormat
import java.util.*
import java.util.concurrent.TimeUnit

/**
 * Controlla ogni giorno tutte le scadenze (revisione, assicurazione, manutenzioni)
 * e invia una notifica per quelle che scadono entro le soglie definite
 * (30, 15, 7, 1 giorno/i prima e il giorno stesso).
 */
class DeadlineCheckWorker(
    context: Context,
    params: WorkerParameters
) : CoroutineWorker(context, params) {

    override suspend fun doWork(): Result {
        val repository = AppRepository(AppDatabase.getInstance(applicationContext))
        val vehicles = repository.getAllVehiclesOnce()
        val maintenances = repository.getAllMaintenancesOnce()

        val now = Calendar.getInstance()
        now.set(Calendar.HOUR_OF_DAY, 0)
        now.set(Calendar.MINUTE, 0)
        now.set(Calendar.SECOND, 0)
        now.set(Calendar.MILLISECOND, 0)
        val todayMillis = now.timeInMillis
        val dateFormat = SimpleDateFormat("dd/MM/yyyy", Locale.ITALIAN)

        val thresholds = setOf(30, 15, 7, 1, 0)

        fun daysUntil(target: Long): Int {
            val diff = target - todayMillis
            return TimeUnit.MILLISECONDS.toDays(diff).toInt()
        }

        fun notifyIfDue(id: Int, title: String, dateMillis: Long?, label: String) {
            if (dateMillis == null) return
            val days = daysUntil(dateMillis)
            if (days in 0..30 && days in thresholds) {
                val dateStr = dateFormat.format(Date(dateMillis))
                val text = if (days == 0) {
                    "$label scade OGGI ($dateStr)"
                } else {
                    "$label scade tra $days giorni ($dateStr)"
                }
                Notifier.send(applicationContext, id, title, text)
            }
        }

        for (vehicle in vehicles) {
            notifyIfDue(
                id = (vehicle.id * 10 + 1).toInt(),
                title = vehicle.name,
                dateMillis = vehicle.revisioneScadenza,
                label = "Revisione"
            )
            notifyIfDue(
                id = (vehicle.id * 10 + 2).toInt(),
                title = vehicle.name,
                dateMillis = vehicle.assicurazioneScadenza,
                label = "Assicurazione"
            )
            notifyIfDue(
                id = (vehicle.id * 10 + 3).toInt(),
                title = vehicle.name,
                dateMillis = vehicle.bolloScadenza,
                label = "Bollo"
            )
        }

        for (maintenance in maintenances) {
            val vehicleName = vehicles.find { it.id == maintenance.vehicleId }?.name ?: "Veicolo"
            notifyIfDue(
                id = (maintenance.id * 10 + 4).toInt(),
                title = vehicleName,
                dateMillis = maintenance.prossimaScadenza,
                label = "Manutenzione: ${maintenance.tipo}"
            )
        }

        return Result.success()
    }
}
