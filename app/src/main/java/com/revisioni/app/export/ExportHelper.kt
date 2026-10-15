package com.revisioni.app.export

import android.app.Activity
import android.content.ClipData
import android.content.Context
import android.content.Intent
import android.net.Uri
import androidx.core.content.FileProvider
import com.revisioni.app.data.backup.BackupFormatException
import com.revisioni.app.data.backup.BackupManager
import com.revisioni.app.data.backup.VehicleBundle
import java.io.ByteArrayOutputStream
import java.io.File
import java.io.IOException
import java.io.OutputStream
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import java.util.concurrent.TimeUnit

/** Tipo di file che si può esportare. */
enum class ExportKind(val mimeType: String, val extension: String) {
    /** Documento da leggere, stampare o inviare. */
    PDF("application/pdf", "pdf"),

    /** File da reimportare nell'app (stesso formato di [BackupManager]). */
    BACKUP("application/json", "json")
}

object ExportHelper {

    private const val CACHE_DIR = "exports"
    private const val MAX_IMPORT_BYTES = 20 * 1024 * 1024

    fun fileName(bundles: List<VehicleBundle>, kind: ExportKind, now: Long = System.currentTimeMillis()): String {
        val base = when (bundles.size) {
            1 -> "Scheda_" + sanitize(bundles[0].vehicle.name)
            else -> "Tutte_le_schede"
        }
        val day = SimpleDateFormat("yyyy-MM-dd", Locale.ROOT).format(Date(now))
        return "${base}_$day.${kind.extension}"
    }

    /** Scrive il contenuto del file nel flusso indicato (cache per la condivisione, o file scelto dall'utente). */
    fun write(bundles: List<VehicleBundle>, kind: ExportKind, out: OutputStream) {
        when (kind) {
            ExportKind.PDF -> PdfExporter.write(bundles, out)
            ExportKind.BACKUP -> out.write(BackupManager.toJson(bundles).toByteArray(Charsets.UTF_8))
        }
    }

    /** Crea il file nella cache dell'app, pronto da condividere. Da chiamare fuori dal thread principale. */
    fun prepareForSharing(context: Context, bundles: List<VehicleBundle>, kind: ExportKind): File {
        val file = createCacheFile(context, fileName(bundles, kind))
        file.outputStream().use { write(bundles, kind, it) }
        return file
    }

    /** Legge un file scelto dall'utente. Rifiuta i file troppo grandi per essere un backup. */
    fun readText(context: Context, uri: Uri): String {
        val input = context.contentResolver.openInputStream(uri)
            ?: throw IOException("Impossibile aprire il file.")
        input.use {
            val out = ByteArrayOutputStream()
            val buffer = ByteArray(8 * 1024)
            var total = 0
            while (true) {
                val read = it.read(buffer)
                if (read < 0) break
                total += read
                if (total > MAX_IMPORT_BYTES) {
                    throw BackupFormatException("Il file è troppo grande per essere un backup di Le mie scadenze.")
                }
                out.write(buffer, 0, read)
            }
            return out.toString(Charsets.UTF_8.name())
        }
    }

    /** Apre il menu di condivisione di Android per un file creato con [prepareForSharing]. */
    fun startShare(context: Context, file: File, kind: ExportKind) {
        val uri = FileProvider.getUriForFile(context, "${context.packageName}.fileprovider", file)
        val send = Intent(Intent.ACTION_SEND).apply {
            type = kind.mimeType
            putExtra(Intent.EXTRA_STREAM, uri)
            putExtra(Intent.EXTRA_SUBJECT, file.nameWithoutExtension.replace('_', ' '))
            // Il ClipData fa estendere il permesso di lettura all'app scelta dall'utente.
            clipData = ClipData.newRawUri(file.name, uri)
            addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
        }
        val chooser = Intent.createChooser(send, "Condividi").apply {
            if (context !is Activity) addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        }
        context.startActivity(chooser)
    }

    private fun createCacheFile(context: Context, name: String): File {
        val dir = File(context.cacheDir, CACHE_DIR).apply { mkdirs() }
        // I file condivisi servono solo il tempo dell'invio: si ripulisce quanto è vecchio.
        val limit = System.currentTimeMillis() - TimeUnit.DAYS.toMillis(1)
        dir.listFiles()?.filter { it.lastModified() < limit }?.forEach { it.delete() }
        return File(dir, name)
    }

    private fun sanitize(name: String): String {
        val cleaned = name.map { if (it.isLetterOrDigit()) it else '_' }
            .joinToString("")
            .replace(Regex("_+"), "_")
            .trim('_')
            .take(40)
        return cleaned.ifEmpty { "veicolo" }
    }
}
