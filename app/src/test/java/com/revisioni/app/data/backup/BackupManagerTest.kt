package com.revisioni.app.data.backup

import com.revisioni.app.data.Maintenance
import com.revisioni.app.data.Vehicle
import com.revisioni.app.data.VehicleType
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Assert.fail
import org.junit.Test

class BackupManagerTest {

    private val fullVehicle = Vehicle(
        id = 7,
        name = "Panda di Marco",
        type = VehicleType.AUTO,
        targa = "AB123CD",
        marca = "Fiat",
        modello = "Panda",
        anno = 2018,
        revisioneScadenza = 1_800_000_000_000,
        assicurazioneScadenza = 1_790_000_000_000,
        bolloScadenza = 1_780_000_000_000,
        note = "Riga uno\nRiga due con accenti: è, à, ò e \"virgolette\" € ✓"
    )

    private val fullMaintenance = Maintenance(
        id = 3,
        vehicleId = 7,
        tipo = "Tagliando",
        data = 1_700_000_000_000,
        km = 85_000,
        costo = 189.5,
        officina = "Officina Rossi",
        note = "Filtri e olio",
        prossimaScadenza = 1_730_000_000_000,
        intervalloMesi = 12,
        intervalloKm = 15_000
    )

    @Test
    fun roundTripKeepsEveryField() {
        val json = BackupManager.toJson(listOf(VehicleBundle(fullVehicle, listOf(fullMaintenance))))
        val parsed = BackupManager.parse(json)

        assertEquals(1, parsed.size)
        // Gli id non vengono salvati: all'importazione si azzerano.
        assertEquals(fullVehicle.copy(id = 0), parsed[0].vehicle)
        assertEquals(listOf(fullMaintenance.copy(id = 0, vehicleId = 0)), parsed[0].maintenances)
    }

    @Test
    fun roundTripKeepsMissingOptionalValuesAsNull() {
        val minimal = Vehicle(name = "Moto", type = VehicleType.MOTO)
        val maintenance = Maintenance(vehicleId = 1, tipo = "Cambio olio", data = 1_700_000_000_000)

        val parsed = BackupManager.parse(
            BackupManager.toJson(listOf(VehicleBundle(minimal, listOf(maintenance))))
        )

        val vehicle = parsed[0].vehicle
        assertEquals(VehicleType.MOTO, vehicle.type)
        assertNull(vehicle.anno)
        assertNull(vehicle.revisioneScadenza)
        assertNull(vehicle.assicurazioneScadenza)
        assertNull(vehicle.bolloScadenza)

        val m = parsed[0].maintenances.single()
        assertNull(m.km)
        assertNull(m.costo)
        assertNull(m.prossimaScadenza)
        assertNull(m.intervalloMesi)
        assertNull(m.intervalloKm)
    }

    @Test
    fun severalVehiclesKeepTheirOwnMaintenances() {
        val a = VehicleBundle(fullVehicle, listOf(fullMaintenance, fullMaintenance.copy(tipo = "Pneumatici")))
        val b = VehicleBundle(Vehicle(name = "Vespa", type = VehicleType.MOTO), emptyList())

        val parsed = BackupManager.parse(BackupManager.toJson(listOf(a, b)))

        assertEquals(2, parsed.size)
        assertEquals(2, parsed[0].maintenances.size)
        assertEquals(0, parsed[1].maintenances.size)
        assertEquals("Vespa", parsed[1].vehicle.name)
    }

    @Test
    fun emptyListIsAValidBackup() {
        assertTrue(BackupManager.parse(BackupManager.toJson(emptyList())).isEmpty())
    }

    @Test
    fun byteOrderMarkIsIgnored() {
        val json = "﻿" + BackupManager.toJson(listOf(VehicleBundle(fullVehicle, emptyList())))
        assertEquals(1, BackupManager.parse(json).size)
    }

    @Test
    fun rejectsTextThatIsNotJson() {
        assertRejected("questo non è json")
    }

    @Test
    fun rejectsJsonFromAnotherApp() {
        assertRejected("""{"format":"altro","version":1,"vehicles":[]}""")
    }

    @Test
    fun rejectsFileFromANewerVersion() {
        val newer = """{"format":"${BackupManager.FORMAT}","version":${BackupManager.VERSION + 1},"vehicles":[]}"""
        val error = assertRejected(newer)
        assertTrue(error.message!!.contains("più recente"))
    }

    @Test
    fun rejectsMaintenanceWithoutDate() {
        val json = """
            {"format":"${BackupManager.FORMAT}","version":1,"vehicles":[
              {"name":"X","type":"AUTO","maintenances":[{"tipo":"Tagliando"}]}
            ]}
        """.trimIndent()
        assertRejected(json)
    }

    @Test
    fun unknownVehicleTypeFallsBackToAuto() {
        val json = """{"format":"${BackupManager.FORMAT}","version":1,"vehicles":[{"name":"X","type":"CAMION"}]}"""
        assertEquals(VehicleType.AUTO, BackupManager.parse(json).single().vehicle.type)
    }

    @Test
    fun vehicleMatchingUsesTargaIgnoringSpacesAndCase() {
        val a = Vehicle(name = "Auto", type = VehicleType.AUTO, targa = "ab 123 cd")
        val b = Vehicle(name = "Altro nome", type = VehicleType.AUTO, targa = "AB123CD")
        assertTrue(isSameVehicle(a, b))
        assertFalse(isSameVehicle(a, b.copy(targa = "ZZ999ZZ")))
    }

    @Test
    fun vehicleMatchingFallsBackToNameAndTypeWithoutTarga() {
        val a = Vehicle(name = "Vespa ", type = VehicleType.MOTO)
        assertTrue(isSameVehicle(a, Vehicle(name = "vespa", type = VehicleType.MOTO)))
        assertFalse(isSameVehicle(a, Vehicle(name = "vespa", type = VehicleType.AUTO)))
    }

    @Test
    fun maintenanceMatchingUsesTypeDateAndKm() {
        assertTrue(isSameMaintenance(fullMaintenance, fullMaintenance.copy(id = 99, tipo = " tagliando")))
        assertFalse(isSameMaintenance(fullMaintenance, fullMaintenance.copy(data = fullMaintenance.data + 1)))
        assertFalse(isSameMaintenance(fullMaintenance, fullMaintenance.copy(km = 90_000)))
    }

    private fun assertRejected(text: String): BackupFormatException {
        try {
            BackupManager.parse(text)
        } catch (e: BackupFormatException) {
            return e
        }
        fail("Il file avrebbe dovuto essere rifiutato")
        throw IllegalStateException()
    }
}
