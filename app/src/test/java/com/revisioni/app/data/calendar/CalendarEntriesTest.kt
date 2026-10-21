package com.revisioni.app.data.calendar

import com.revisioni.app.data.Maintenance
import com.revisioni.app.data.Vehicle
import com.revisioni.app.data.VehicleType
import com.revisioni.app.data.backup.VehicleBundle
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import java.time.LocalDate
import java.time.ZoneId
import java.time.ZoneOffset
import java.util.TimeZone

class CalendarEntriesTest {

    private lateinit var originalZone: TimeZone

    @Before
    fun saveZone() {
        originalZone = TimeZone.getDefault()
    }

    @After
    fun restoreZone() {
        TimeZone.setDefault(originalZone)
    }

    private fun localMidnight(zone: String, year: Int, month: Int, day: Int): Long =
        LocalDate.of(year, month, day).atStartOfDay(ZoneId.of(zone)).toInstant().toEpochMilli()

    private fun utcMidnight(year: Int, month: Int, day: Int): Long =
        LocalDate.of(year, month, day).atStartOfDay(ZoneOffset.UTC).toInstant().toEpochMilli()

    private val vehicle = Vehicle(
        id = 7,
        name = "Panda",
        type = VehicleType.AUTO,
        targa = "AB123CD",
        revisioneScadenza = 1_800_000_000_000,
        assicurazioneScadenza = 1_790_000_000_000,
        bolloScadenza = null
    )

    @Test
    fun `tutte le zone orarie danno lo stesso giorno di calendario`() {
        for (zone in listOf("Europe/Rome", "Pacific/Auckland", "America/Los_Angeles", "UTC")) {
            TimeZone.setDefault(TimeZone.getTimeZone(zone))
            val result = CalendarEntries.toAllDayUtcMillis(localMidnight(zone, 2026, 12, 15))
            assertEquals("zona $zone", utcMidnight(2026, 12, 15), result)
        }
    }

    @Test
    fun `i promemoria sono alle 9 di mattina 30, 7 e 1 giorno prima`() {
        // Evento all-day = mezzanotte del giorno di scadenza: 9 ore in meno di N giorni.
        assertEquals(listOf(42_660, 9_540, 900), CalendarEntries.reminderMinutesBefore)
    }

    @Test
    fun `crea una voce per ogni scadenza impostata e salta quelle vuote`() {
        val entries = CalendarEntries.forBundle(VehicleBundle(vehicle, emptyList()))

        assertEquals(listOf("v7:revisione", "v7:assicurazione"), entries.map { it.key })
        assertEquals("Revisione · Panda (AB123CD)", entries[0].title)
        assertEquals(1_800_000_000_000, entries[0].dateMillis)
    }

    @Test
    fun `senza targa il titolo contiene solo il nome`() {
        val entries = CalendarEntries.forBundle(VehicleBundle(vehicle.copy(targa = ""), emptyList()))
        assertEquals("Revisione · Panda", entries[0].title)
    }

    @Test
    fun `le manutenzioni entrano solo se hanno una prossima scadenza`() {
        val withDeadline = Maintenance(
            id = 3, vehicleId = 7, tipo = "Tagliando", data = 1_700_000_000_000,
            officina = "Rossi", prossimaScadenza = 1_810_000_000_000, intervalloMesi = 12
        )
        val withoutDeadline = Maintenance(id = 4, vehicleId = 7, tipo = "Gomme", data = 1_700_000_000_000)

        val entries = CalendarEntries.forBundle(VehicleBundle(vehicle, listOf(withDeadline, withoutDeadline)))
        val maintenance = entries.single { it.key == "v7:m3" }

        assertEquals("Manutenzione: Tagliando · Panda (AB123CD)", maintenance.title)
        assertTrue(maintenance.description.contains("ogni 12 mesi"))
        assertTrue(maintenance.description.contains("officina: Rossi"))
        assertEquals(3, entries.size)
    }

    @Test
    fun `le chiavi di un veicolo condividono lo stesso prefisso e non si confondono con altri`() {
        val key = CalendarEntries.maintenanceKey(7, 3)
        assertTrue(key.startsWith(CalendarEntries.vehiclePrefix(7)))
        // "v1:" non deve riconoscere come suo un evento di "v17:".
        assertTrue(!CalendarEntries.maintenanceKey(17, 3).startsWith(CalendarEntries.vehiclePrefix(1)))
    }
}
