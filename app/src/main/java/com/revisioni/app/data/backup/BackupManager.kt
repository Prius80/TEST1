package com.revisioni.app.data.backup

import com.revisioni.app.data.Maintenance
import com.revisioni.app.data.Vehicle
import com.revisioni.app.data.VehicleType
import org.json.JSONArray
import org.json.JSONException
import org.json.JSONObject

/** Un veicolo insieme a tutte le sue manutenzioni: è l'unità che si esporta e si reimporta. */
data class VehicleBundle(
    val vehicle: Vehicle,
    val maintenances: List<Maintenance>
)

/** File non leggibile o non creato da questa app. Il messaggio è pensato per l'utente. */
class BackupFormatException(message: String) : Exception(message)

/**
 * Formato di scambio delle schede: un file JSON leggibile, con versione.
 *
 * Gli id interni del database non vengono salvati: all'importazione le schede
 * ricevono id nuovi e le manutenzioni restano legate al proprio veicolo perché
 * sono annidate al suo interno.
 */
object BackupManager {
    const val FORMAT = "le-mie-scadenze-backup"
    const val VERSION = 1

    fun toJson(bundles: List<VehicleBundle>, exportedAt: Long = System.currentTimeMillis()): String {
        val vehicles = JSONArray()
        for (bundle in bundles) {
            vehicles.put(vehicleToJson(bundle))
        }
        return JSONObject()
            .put("format", FORMAT)
            .put("version", VERSION)
            .put("exportedAt", exportedAt)
            .put("vehicles", vehicles)
            .toString(2)
    }

    fun parse(text: String): List<VehicleBundle> {
        val root = try {
            JSONObject(text.trimStart('﻿'))
        } catch (e: JSONException) {
            throw BackupFormatException("Il file non è un backup valido di Le mie scadenze.")
        }

        if (root.optString("format") != FORMAT) {
            throw BackupFormatException("Il file non è un backup valido di Le mie scadenze.")
        }
        val version = root.optInt("version", -1)
        if (version < 1) {
            throw BackupFormatException("Versione del file non riconosciuta.")
        }
        if (version > VERSION) {
            throw BackupFormatException(
                "Il file è stato creato da una versione più recente dell'app. Aggiorna l'app per importarlo."
            )
        }

        val vehicles = root.optJSONArray("vehicles")
            ?: throw BackupFormatException("Il file non contiene nessun veicolo.")

        return try {
            (0 until vehicles.length()).map { index -> bundleFromJson(vehicles.getJSONObject(index)) }
        } catch (e: JSONException) {
            throw BackupFormatException("Il file è danneggiato o incompleto.")
        }
    }

    // ---- scrittura ----

    private fun vehicleToJson(bundle: VehicleBundle): JSONObject {
        val v = bundle.vehicle
        val maintenances = JSONArray()
        for (m in bundle.maintenances) {
            maintenances.put(maintenanceToJson(m))
        }
        return JSONObject()
            .put("name", v.name)
            .put("type", v.type.name)
            .put("targa", v.targa)
            .put("marca", v.marca)
            .put("modello", v.modello)
            .putIfNotNull("anno", v.anno)
            .putIfNotNull("revisioneScadenza", v.revisioneScadenza)
            .putIfNotNull("assicurazioneScadenza", v.assicurazioneScadenza)
            .putIfNotNull("bolloScadenza", v.bolloScadenza)
            .put("note", v.note)
            .put("maintenances", maintenances)
    }

    private fun maintenanceToJson(m: Maintenance): JSONObject =
        JSONObject()
            .put("tipo", m.tipo)
            .put("data", m.data)
            .putIfNotNull("km", m.km)
            .putIfNotNull("costo", m.costo)
            .put("officina", m.officina)
            .put("note", m.note)
            .putIfNotNull("prossimaScadenza", m.prossimaScadenza)
            .putIfNotNull("intervalloMesi", m.intervalloMesi)
            .putIfNotNull("intervalloKm", m.intervalloKm)

    // ---- lettura ----

    private fun bundleFromJson(json: JSONObject): VehicleBundle {
        val type = runCatching { VehicleType.valueOf(json.optString("type")) }
            .getOrDefault(VehicleType.AUTO)

        val vehicle = Vehicle(
            id = 0,
            name = json.optString("name").ifBlank { "Veicolo" },
            type = type,
            targa = json.optString("targa"),
            marca = json.optString("marca"),
            modello = json.optString("modello"),
            anno = json.optIntOrNull("anno"),
            revisioneScadenza = json.optLongOrNull("revisioneScadenza"),
            assicurazioneScadenza = json.optLongOrNull("assicurazioneScadenza"),
            bolloScadenza = json.optLongOrNull("bolloScadenza"),
            note = json.optString("note")
        )

        val maintenancesJson = json.optJSONArray("maintenances") ?: JSONArray()
        val maintenances = (0 until maintenancesJson.length()).map { index ->
            val m = maintenancesJson.getJSONObject(index)
            Maintenance(
                id = 0,
                vehicleId = 0, // assegnato all'importazione
                tipo = m.optString("tipo").ifBlank { "Manutenzione" },
                // La data di esecuzione è obbligatoria: senza, la riga non ha senso.
                data = m.optLongOrNull("data")
                    ?: throw BackupFormatException("Una manutenzione nel file non ha la data."),
                km = m.optIntOrNull("km"),
                costo = m.optDoubleOrNull("costo"),
                officina = m.optString("officina"),
                note = m.optString("note"),
                prossimaScadenza = m.optLongOrNull("prossimaScadenza"),
                intervalloMesi = m.optIntOrNull("intervalloMesi"),
                intervalloKm = m.optIntOrNull("intervalloKm")
            )
        }
        return VehicleBundle(vehicle, maintenances)
    }
}

// JSONObject di Android restituisce 0 o "" per le chiavi assenti: questi helper
// distinguono "assente / null" da un valore vero.

private fun JSONObject.putIfNotNull(key: String, value: Any?): JSONObject =
    if (value != null) put(key, value) else this

private fun JSONObject.optLongOrNull(key: String): Long? =
    if (!has(key) || isNull(key)) null else getLong(key)

private fun JSONObject.optIntOrNull(key: String): Int? =
    if (!has(key) || isNull(key)) null else getInt(key)

private fun JSONObject.optDoubleOrNull(key: String): Double? =
    if (!has(key) || isNull(key)) null else getDouble(key)
