package com.revisioni.app.ui.components

import android.net.Uri
import android.widget.Toast
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.Box
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.FileDownload
import androidx.compose.material.icons.filled.PictureAsPdf
import androidx.compose.material.icons.filled.SaveAlt
import androidx.compose.material.icons.filled.Share
import androidx.compose.material3.Divider
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import com.revisioni.app.data.backup.BackupFormatException
import com.revisioni.app.data.backup.ImportResult
import com.revisioni.app.data.backup.VehicleBundle
import com.revisioni.app.export.ExportHelper
import com.revisioni.app.export.ExportKind
import com.revisioni.app.ui.AppViewModel
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.io.IOException

/**
 * Pulsante con menu per condividere o salvare le schede come PDF, ed esportarle
 * in un file che l'app sa reimportare. Con [allowImport] mostra anche "Importa".
 *
 * [loadBundles] fornisce le schede da esportare (una sola o tutte) al momento del tocco,
 * così il file contiene sempre i dati aggiornati.
 */
@Composable
fun ExportMenuButton(
    viewModel: AppViewModel,
    loadBundles: suspend () -> List<VehicleBundle>,
    icon: ImageVector,
    contentDescription: String,
    allowImport: Boolean = false
) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    var expanded by remember { mutableStateOf(false) }
    var busy by remember { mutableStateOf(false) }
    // Schede in attesa che l'utente scelga dove salvare il file.
    var pendingSave by remember { mutableStateOf<List<VehicleBundle>?>(null) }

    fun toast(message: String) = Toast.makeText(context, message, Toast.LENGTH_LONG).show()

    fun saveTo(uri: Uri?, kind: ExportKind) {
        val bundles = pendingSave
        pendingSave = null
        if (uri == null || bundles == null) return // l'utente ha annullato
        scope.launch {
            try {
                withContext(Dispatchers.IO) {
                    val out = context.contentResolver.openOutputStream(uri, "wt")
                        ?: throw IOException("Impossibile scrivere il file.")
                    out.use { ExportHelper.write(bundles, kind, it) }
                }
                toast("File salvato")
            } catch (e: Exception) {
                toast("Salvataggio non riuscito: ${e.message ?: "errore sconosciuto"}")
            }
        }
    }

    val pdfSaver = rememberLauncherForActivityResult(
        ActivityResultContracts.CreateDocument(ExportKind.PDF.mimeType)
    ) { uri -> saveTo(uri, ExportKind.PDF) }

    val backupSaver = rememberLauncherForActivityResult(
        ActivityResultContracts.CreateDocument(ExportKind.BACKUP.mimeType)
    ) { uri -> saveTo(uri, ExportKind.BACKUP) }

    val importer = rememberLauncherForActivityResult(
        ActivityResultContracts.OpenDocument()
    ) { uri ->
        if (uri != null) {
            scope.launch {
                busy = true
                try {
                    val text = withContext(Dispatchers.IO) { ExportHelper.readText(context, uri) }
                    toast(importSummary(viewModel.importBackup(text)))
                } catch (e: BackupFormatException) {
                    toast(e.message ?: "File non valido.")
                } catch (e: Exception) {
                    toast("Importazione non riuscita: ${e.message ?: "errore sconosciuto"}")
                } finally {
                    busy = false
                }
            }
        }
    }

    fun share(kind: ExportKind) {
        expanded = false
        if (busy) return
        scope.launch {
            busy = true
            try {
                val bundles = loadBundles()
                if (bundles.isEmpty()) {
                    toast("Non c'è nessuna scheda da esportare.")
                    return@launch
                }
                val file = withContext(Dispatchers.IO) { ExportHelper.prepareForSharing(context, bundles, kind) }
                ExportHelper.startShare(context, file, kind)
            } catch (e: Exception) {
                toast("Condivisione non riuscita: ${e.message ?: "errore sconosciuto"}")
            } finally {
                busy = false
            }
        }
    }

    fun save(kind: ExportKind) {
        expanded = false
        if (busy) return
        scope.launch {
            busy = true
            try {
                val bundles = loadBundles()
                if (bundles.isEmpty()) {
                    toast("Non c'è nessuna scheda da esportare.")
                    return@launch
                }
                pendingSave = bundles
                val name = ExportHelper.fileName(bundles, kind)
                if (kind == ExportKind.PDF) pdfSaver.launch(name) else backupSaver.launch(name)
            } catch (e: Exception) {
                toast("Salvataggio non riuscito: ${e.message ?: "errore sconosciuto"}")
            } finally {
                busy = false
            }
        }
    }

    Box {
        IconButton(onClick = { expanded = true }) {
            Icon(icon, contentDescription = contentDescription)
        }
        DropdownMenu(expanded = expanded, onDismissRequest = { expanded = false }) {
            DropdownMenuItem(
                text = { Text("Condividi PDF") },
                leadingIcon = { Icon(Icons.Default.PictureAsPdf, contentDescription = null) },
                onClick = { share(ExportKind.PDF) }
            )
            DropdownMenuItem(
                text = { Text("Salva PDF sul telefono") },
                leadingIcon = { Icon(Icons.Default.SaveAlt, contentDescription = null) },
                onClick = { save(ExportKind.PDF) }
            )
            Divider()
            DropdownMenuItem(
                text = { Text("Condividi file per reimportare") },
                leadingIcon = { Icon(Icons.Default.Share, contentDescription = null) },
                onClick = { share(ExportKind.BACKUP) }
            )
            DropdownMenuItem(
                text = { Text("Salva file per reimportare") },
                leadingIcon = { Icon(Icons.Default.SaveAlt, contentDescription = null) },
                onClick = { save(ExportKind.BACKUP) }
            )
            if (allowImport) {
                Divider()
                DropdownMenuItem(
                    text = { Text("Importa da file") },
                    leadingIcon = { Icon(Icons.Default.FileDownload, contentDescription = null) },
                    onClick = {
                        expanded = false
                        if (!busy) importer.launch(arrayOf("*/*"))
                    }
                )
            }
        }
    }
}

private fun importSummary(result: ImportResult): String {
    fun plural(n: Int, one: String, many: String) = if (n == 1) "1 $one" else "$n $many"

    val parts = mutableListOf<String>()
    if (result.vehiclesAdded > 0) parts += plural(result.vehiclesAdded, "veicolo nuovo", "veicoli nuovi")
    if (result.maintenancesAdded > 0) {
        parts += plural(result.maintenancesAdded, "manutenzione aggiunta", "manutenzioni aggiunte")
    }
    if (result.vehiclesAlreadyPresent > 0) {
        parts += plural(result.vehiclesAlreadyPresent, "veicolo già presente", "veicoli già presenti")
    }
    return if (parts.isEmpty()) "Il file non contiene nessuna scheda." else "Importato: " + parts.joinToString(", ")
}
