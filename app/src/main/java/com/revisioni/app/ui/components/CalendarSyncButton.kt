package com.revisioni.app.ui.components

import android.widget.Toast
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Event
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.platform.LocalContext
import com.revisioni.app.data.backup.VehicleBundle
import com.revisioni.app.data.calendar.CalendarSync
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

/**
 * Pulsante che inserisce (o aggiorna) le scadenze nel calendario Google del telefono.
 * Al primo tocco chiede il permesso di accedere al calendario.
 *
 * [loadBundles] fornisce le schede da inserire (una sola o tutte) al momento del tocco,
 * così il calendario riceve sempre le date aggiornate.
 */
@Composable
fun CalendarSyncButton(
    loadBundles: suspend () -> List<VehicleBundle>,
    contentDescription: String
) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    var busy by remember { mutableStateOf(false) }

    fun toast(message: String) = Toast.makeText(context, message, Toast.LENGTH_LONG).show()

    fun runSync() {
        if (busy) return
        scope.launch {
            busy = true
            try {
                val bundles = loadBundles()
                if (bundles.isEmpty()) {
                    toast("Non c'è nessuna scadenza da inserire nel calendario.")
                    return@launch
                }
                val result = withContext(Dispatchers.IO) { CalendarSync.sync(context, bundles) }
                toast(
                    if (result.isEmpty) {
                        "Nessuna scadenza futura da inserire nel calendario."
                    } else {
                        val parts = mutableListOf<String>()
                        if (result.added > 0) parts += "${result.added} aggiunte"
                        if (result.updated > 0) parts += "${result.updated} aggiornate"
                        if (result.removed > 0) parts += "${result.removed} rimosse"
                        "Calendario «${result.calendarName}»: " + parts.joinToString(", ")
                    }
                )
            } catch (e: CalendarSync.NoCalendarException) {
                toast(e.message ?: "Nessun calendario trovato.")
            } catch (e: SecurityException) {
                toast("Permesso negato: abilita l'accesso al calendario nelle impostazioni dell'app.")
            } catch (e: Exception) {
                toast("Inserimento nel calendario non riuscito: ${e.message ?: "errore sconosciuto"}")
            } finally {
                busy = false
            }
        }
    }

    val permissionLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.RequestMultiplePermissions()
    ) { granted ->
        if (granted.isNotEmpty() && granted.values.all { it }) {
            runSync()
        } else {
            toast("Per inserire le scadenze serve il permesso di accedere al calendario.")
        }
    }

    IconButton(onClick = {
        if (CalendarSync.hasPermission(context)) runSync()
        else permissionLauncher.launch(CalendarSync.PERMISSIONS)
    }) {
        Icon(Icons.Default.Event, contentDescription = contentDescription)
    }
}
