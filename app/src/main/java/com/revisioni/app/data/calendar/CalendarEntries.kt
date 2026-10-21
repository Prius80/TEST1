package com.revisioni.app.data.calendar

import com.revisioni.app.data.backup.VehicleBundle
import java.util.Calendar
import java.util.TimeZone

/** Una scadenza da riportare come evento "tutto il giorno" nel calendario. */
data class CalendarEntry(
    /** Chiave stabile (non cambia se la data o il nome cambiano): serve a ritrovare l'evento già creato. */
    val key: String,
    val title: String,
    val description: String,
    /** Data della scadenza, epoch millis locale come salvata dall'app. */
    val dateMillis: Long
)

/**
 * Trasforma le schede dei veicoli nell'elenco di scadenze da inserire in Google Calendar.
 * Logica pura (nessuna dipendenza Android) per poterla provare con test locali.
 */
object CalendarEntries {

    /**
     * Promemoria dell'evento, in minuti prima della mezzanotte del giorno di scadenza.
     * Un evento "tutto il giorno" parte a mezzanotte, quindi per avere l'avviso alle 9:00
     * si sottraggono 9 ore: 30 giorni prima, 7 giorni prima e il giorno prima, sempre alle 9:00.
     */
    val reminderMinutesBefore: List<Int> = listOf(30, 7, 1).map { days -> days * 24 * 60 - 9 * 60 }

    fun vehiclePrefix(vehicleId: Long) = "v$vehicleId:"

    fun maintenanceKey(vehicleId: Long, maintenanceId: Long) =
        "${vehiclePrefix(vehicleId)}m$maintenanceId"

    /** Tutte le scadenze con una data impostata del veicolo e delle sue manutenzioni. */
    fun forBundle(bundle: VehicleBundle): List<CalendarEntry> {
        val vehicle = bundle.vehicle
        val prefix = vehiclePrefix(vehicle.id)
        val who = if (vehicle.targa.isBlank()) vehicle.name else "${vehicle.name} (${vehicle.targa})"
        val footer = "Inserita da Le mie scadenze"
        val entries = mutableListOf<CalendarEntry>()

        fun fixed(suffix: String, label: String, date: Long?) {
            if (date == null) return
            entries += CalendarEntry(
                key = prefix + suffix,
                title = "$label · $who",
                description = "Scadenza $label\nVeicolo: $who\n\n$footer",
                dateMillis = date
            )
        }

        fixed("revisione", "Revisione", vehicle.revisioneScadenza)
        fixed("assicurazione", "Assicurazione", vehicle.assicurazioneScadenza)
        fixed("bollo", "Bollo", vehicle.bolloScadenza)

        for (m in bundle.maintenances) {
            val date = m.prossimaScadenza ?: continue
            val details = listOfNotNull(
                m.intervalloMesi?.let { "ogni $it mesi" },
                m.intervalloKm?.let { "ogni $it km" },
                m.officina.takeIf { it.isNotBlank() }?.let { "officina: $it" }
            ).joinToString(" · ")
            entries += CalendarEntry(
                key = maintenanceKey(vehicle.id, m.id),
                title = "Manutenzione: ${m.tipo} · $who",
                description = buildString {
                    append("Prossima manutenzione: ${m.tipo}\nVeicolo: $who")
                    if (details.isNotEmpty()) append("\n$details")
                    append("\n\n$footer")
                },
                dateMillis = date
            )
        }
        return entries
    }

    /** Mezzanotte di oggi (ora locale) in epoch millis. */
    fun startOfToday(now: Long = System.currentTimeMillis()): Long =
        Calendar.getInstance().apply {
            timeInMillis = now
            set(Calendar.HOUR_OF_DAY, 0); set(Calendar.MINUTE, 0)
            set(Calendar.SECOND, 0); set(Calendar.MILLISECOND, 0)
        }.timeInMillis

    /**
     * Converte la data scelta nell'app (mezzanotte locale) nella mezzanotte UTC dello stesso
     * giorno, come richiesto da Calendar Provider per gli eventi "tutto il giorno":
     * così l'evento cade sempre nel giorno giusto, qualunque sia il fuso orario.
     */
    fun toAllDayUtcMillis(localMillis: Long): Long {
        val local = Calendar.getInstance().apply { timeInMillis = localMillis }
        return Calendar.getInstance(TimeZone.getTimeZone("UTC")).apply {
            clear()
            set(local.get(Calendar.YEAR), local.get(Calendar.MONTH), local.get(Calendar.DAY_OF_MONTH))
        }.timeInMillis
    }
}
