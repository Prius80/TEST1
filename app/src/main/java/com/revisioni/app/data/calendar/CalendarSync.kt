package com.revisioni.app.data.calendar

import android.Manifest
import android.content.ContentUris
import android.content.ContentValues
import android.content.Context
import android.content.pm.PackageManager
import android.provider.CalendarContract.Calendars
import android.provider.CalendarContract.Events
import android.provider.CalendarContract.Reminders
import androidx.core.content.ContextCompat
import com.revisioni.app.data.backup.VehicleBundle

/**
 * Inserisce le scadenze come eventi "tutto il giorno" nel calendario Google del telefono
 * (tramite Calendar Provider: l'account Google li sincronizza poi su Google Calendar e sugli
 * altri dispositivi). Ogni evento ha promemoria a 30 giorni, 7 giorni e 1 giorno dalla scadenza.
 *
 * La sincronizzazione si può ripetere: gli eventi già creati vengono aggiornati, non duplicati.
 * Gli eventi creati restano nel calendario anche dopo la scadenza; vengono rimossi solo quando
 * la scadenza viene tolta dall'app (data cancellata, manutenzione o veicolo eliminati).
 *
 * Tutte le funzioni sono bloccanti: vanno chiamate fuori dal thread principale.
 */
object CalendarSync {

    private const val PREFS = "calendar_sync"

    val PERMISSIONS = arrayOf(Manifest.permission.READ_CALENDAR, Manifest.permission.WRITE_CALENDAR)

    data class SyncResult(
        val calendarName: String,
        val added: Int,
        val updated: Int,
        val removed: Int
    ) {
        val isEmpty get() = added == 0 && updated == 0 && removed == 0
    }

    private data class CalendarInfo(val id: Long, val name: String)

    class NoCalendarException : Exception(
        "Nessun calendario Google trovato sul telefono. Apri l'app Google Calendar, accedi e riprova."
    )

    fun hasPermission(context: Context): Boolean = PERMISSIONS.all {
        ContextCompat.checkSelfPermission(context, it) == PackageManager.PERMISSION_GRANTED
    }

    fun sync(
        context: Context,
        bundles: List<VehicleBundle>,
        todayMillis: Long = CalendarEntries.startOfToday()
    ): SyncResult {
        val resolver = context.contentResolver
        val calendar = findCalendar(context) ?: throw NoCalendarException()
        val prefs = context.getSharedPreferences(PREFS, Context.MODE_PRIVATE)
        val mapping = readMapping(context)

        var added = 0
        var updated = 0
        var removed = 0

        for (bundle in bundles) {
            val entries = CalendarEntries.forBundle(bundle)
            val currentKeys = entries.map { it.key }.toSet()
            val prefix = CalendarEntries.vehiclePrefix(bundle.vehicle.id)

            // Scadenze tolte dall'app dopo l'ultima sincronizzazione: via anche dal calendario.
            val stale = mapping.filterKeys { it.startsWith(prefix) && it !in currentKeys }
            for ((key, eventId) in stale) {
                deleteEvent(context, eventId)
                mapping.remove(key)
                removed++
            }

            for (entry in entries) {
                val start = CalendarEntries.toAllDayUtcMillis(entry.dateMillis)
                val knownId = mapping[entry.key]?.takeIf { eventExists(context, it) }
                    ?: findExisting(context, calendar.id, entry.title, start)

                val eventId = when {
                    knownId != null -> {
                        resolver.update(
                            ContentUris.withAppendedId(Events.CONTENT_URI, knownId),
                            eventValues(entry, start),
                            null, null
                        )
                        replaceReminders(context, knownId)
                        updated++
                        knownId
                    }
                    // Scadenza già passata e mai inserita: non ha senso aggiungerla ora.
                    entry.dateMillis < todayMillis -> continue
                    else -> {
                        val values = eventValues(entry, start).apply { put(Events.CALENDAR_ID, calendar.id) }
                        val uri = resolver.insert(Events.CONTENT_URI, values) ?: continue
                        val newId = ContentUris.parseId(uri)
                        replaceReminders(context, newId)
                        added++
                        newId
                    }
                }
                mapping[entry.key] = eventId
            }
        }

        writeMapping(prefs, mapping)
        return SyncResult(calendar.name, added, updated, removed)
    }

    /** Toglie dal calendario gli eventi di un veicolo (da chiamare quando il veicolo viene eliminato). */
    fun removeVehicle(context: Context, vehicleId: Long) =
        removeWhere(context) { it.startsWith(CalendarEntries.vehiclePrefix(vehicleId)) }

    /** Toglie dal calendario l'evento di una manutenzione eliminata. */
    fun removeMaintenance(context: Context, vehicleId: Long, maintenanceId: Long) {
        val key = CalendarEntries.maintenanceKey(vehicleId, maintenanceId)
        removeWhere(context) { it == key }
    }

    private fun removeWhere(context: Context, matches: (String) -> Boolean) {
        if (!hasPermission(context)) return
        try {
            val prefs = context.getSharedPreferences(PREFS, Context.MODE_PRIVATE)
            val mapping = readMapping(context)
            val targets = mapping.filterKeys(matches)
            if (targets.isEmpty()) return
            targets.forEach { (key, eventId) ->
                deleteEvent(context, eventId)
                mapping.remove(key)
            }
            writeMapping(prefs, mapping)
        } catch (_: Exception) {
            // La pulizia del calendario non deve mai bloccare l'eliminazione dei dati dell'app.
        }
    }

    // ---- Calendar Provider ----

    /** Preferisce il calendario principale dell'account Google; altrimenti il primo scrivibile. */
    private fun findCalendar(context: Context): CalendarInfo? {
        val projection = arrayOf(
            Calendars._ID,
            Calendars.CALENDAR_DISPLAY_NAME,
            Calendars.ACCOUNT_NAME,
            Calendars.ACCOUNT_TYPE,
            Calendars.OWNER_ACCOUNT,
            Calendars.IS_PRIMARY
        )
        val selection = "${Calendars.CALENDAR_ACCESS_LEVEL} >= ? AND ${Calendars.VISIBLE} = 1"
        val args = arrayOf(Calendars.CAL_ACCESS_CONTRIBUTOR.toString())

        data class Candidate(val info: CalendarInfo, val rank: Int)

        val candidates = mutableListOf<Candidate>()
        context.contentResolver.query(Calendars.CONTENT_URI, projection, selection, args, null)?.use { c ->
            while (c.moveToNext()) {
                val accountName = c.getString(2).orEmpty()
                val isGoogle = c.getString(3) == "com.google"
                val isPrimary = c.getInt(5) == 1 || c.getString(4) == accountName
                val rank = when {
                    isGoogle && isPrimary -> 0
                    isGoogle -> 1
                    else -> 2
                }
                val name = c.getString(1).orEmpty().ifBlank { accountName }
                candidates += Candidate(CalendarInfo(c.getLong(0), name), rank)
            }
        }
        return candidates.minByOrNull { it.rank }?.info
    }

    private fun eventValues(entry: CalendarEntry, startUtc: Long) = ContentValues().apply {
        put(Events.TITLE, entry.title)
        put(Events.DESCRIPTION, entry.description)
        put(Events.DTSTART, startUtc)
        put(Events.DTEND, startUtc + 24L * 60 * 60 * 1000)
        put(Events.ALL_DAY, 1)
        put(Events.EVENT_TIMEZONE, "UTC")
        put(Events.AVAILABILITY, Events.AVAILABILITY_FREE)
    }

    private fun eventExists(context: Context, eventId: Long): Boolean =
        context.contentResolver.query(
            Events.CONTENT_URI,
            arrayOf(Events._ID),
            "${Events._ID} = ? AND ${Events.DELETED} = 0",
            arrayOf(eventId.toString()),
            null
        )?.use { it.moveToFirst() } ?: false

    /** Ritrova un evento identico già presente (ad esempio dopo aver reinstallato l'app). */
    private fun findExisting(context: Context, calendarId: Long, title: String, startUtc: Long): Long? =
        context.contentResolver.query(
            Events.CONTENT_URI,
            arrayOf(Events._ID),
            "${Events.CALENDAR_ID} = ? AND ${Events.TITLE} = ? AND ${Events.DTSTART} = ? AND ${Events.DELETED} = 0",
            arrayOf(calendarId.toString(), title, startUtc.toString()),
            null
        )?.use { if (it.moveToFirst()) it.getLong(0) else null }

    private fun deleteEvent(context: Context, eventId: Long) {
        context.contentResolver.delete(ContentUris.withAppendedId(Events.CONTENT_URI, eventId), null, null)
    }

    private fun replaceReminders(context: Context, eventId: Long) {
        val resolver = context.contentResolver
        resolver.delete(Reminders.CONTENT_URI, "${Reminders.EVENT_ID} = ?", arrayOf(eventId.toString()))
        for (minutes in CalendarEntries.reminderMinutesBefore) {
            val values = ContentValues().apply {
                put(Reminders.EVENT_ID, eventId)
                put(Reminders.MINUTES, minutes)
                put(Reminders.METHOD, Reminders.METHOD_ALERT)
            }
            resolver.insert(Reminders.CONTENT_URI, values)
        }
    }

    // ---- corrispondenza "scadenza dell'app" -> "evento del calendario" ----

    private fun readMapping(context: Context): MutableMap<String, Long> {
        val prefs = context.getSharedPreferences(PREFS, Context.MODE_PRIVATE)
        val map = mutableMapOf<String, Long>()
        for ((key, value) in prefs.all) {
            if (value is Long) map[key] = value
        }
        return map
    }

    private fun writeMapping(prefs: android.content.SharedPreferences, mapping: Map<String, Long>) {
        prefs.edit().apply {
            clear()
            mapping.forEach { (key, id) -> putLong(key, id) }
        }.apply()
    }
}
