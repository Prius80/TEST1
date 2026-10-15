package com.revisioni.app.data.backup

import com.revisioni.app.data.Maintenance
import com.revisioni.app.data.Vehicle

/** Esito di un'importazione, mostrato all'utente. */
data class ImportResult(
    val vehiclesAdded: Int,
    /** Veicoli del file che erano già presenti nell'app (non vengono sovrascritti). */
    val vehiclesAlreadyPresent: Int,
    val maintenancesAdded: Int
)

private fun normalizedTarga(targa: String): String =
    targa.filter { it.isLetterOrDigit() }.uppercase()

/**
 * Due schede sono lo stesso veicolo se hanno la stessa targa; se manca la targa
 * si confrontano nome e tipo. Serve a non duplicare i veicoli reimportando un file.
 */
fun isSameVehicle(a: Vehicle, b: Vehicle): Boolean {
    val targaA = normalizedTarga(a.targa)
    val targaB = normalizedTarga(b.targa)
    if (targaA.isNotEmpty() && targaB.isNotEmpty()) return targaA == targaB
    return a.type == b.type && a.name.trim().equals(b.name.trim(), ignoreCase = true)
}

/** Stessa manutenzione = stesso tipo, stessa data di esecuzione e stessi km. */
fun isSameMaintenance(a: Maintenance, b: Maintenance): Boolean =
    a.data == b.data &&
        a.km == b.km &&
        a.tipo.trim().equals(b.tipo.trim(), ignoreCase = true)
