package com.revisioni.app.ui.components

import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color
import java.text.SimpleDateFormat
import java.util.*
import java.util.concurrent.TimeUnit

fun formatDate(millis: Long?): String {
    if (millis == null) return "-"
    val sdf = SimpleDateFormat("dd/MM/yyyy", Locale.ITALIAN)
    return sdf.format(Date(millis))
}

fun daysUntil(millis: Long?): Int? {
    if (millis == null) return null
    val now = Calendar.getInstance().apply {
        set(Calendar.HOUR_OF_DAY, 0); set(Calendar.MINUTE, 0)
        set(Calendar.SECOND, 0); set(Calendar.MILLISECOND, 0)
    }.timeInMillis
    val target = Calendar.getInstance().apply {
        timeInMillis = millis
        set(Calendar.HOUR_OF_DAY, 0); set(Calendar.MINUTE, 0)
        set(Calendar.SECOND, 0); set(Calendar.MILLISECOND, 0)
    }.timeInMillis
    return TimeUnit.MILLISECONDS.toDays(target - now).toInt()
}

/** Colore semantico in base all'urgenza della scadenza. */
@Composable
fun deadlineColor(millis: Long?): Color {
    val days = daysUntil(millis) ?: return MaterialTheme.colorScheme.onSurfaceVariant
    return when {
        days < 0 -> MaterialTheme.colorScheme.error
        days <= 15 -> Color(0xFFB86E00) // arancio
        else -> Color(0xFF2E7D32) // verde
    }
}

fun deadlineLabel(millis: Long?): String {
    val days = daysUntil(millis) ?: return "Nessuna data impostata"
    return when {
        days < 0 -> "Scaduta da ${-days} giorni"
        days == 0 -> "Scade oggi"
        else -> "Scade tra $days giorni"
    }
}

/** Aggiunge un numero di mesi a una data (epoch millis), mantenendo il giorno del mese. */
fun addMonths(baseMillis: Long, months: Int): Long {
    val cal = Calendar.getInstance().apply {
        timeInMillis = baseMillis
        add(Calendar.MONTH, months)
        set(Calendar.HOUR_OF_DAY, 0); set(Calendar.MINUTE, 0)
        set(Calendar.SECOND, 0); set(Calendar.MILLISECOND, 0)
    }
    return cal.timeInMillis
}
