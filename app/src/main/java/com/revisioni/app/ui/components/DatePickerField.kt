package com.revisioni.app.ui.components

import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CalendarMonth
import androidx.compose.material.icons.filled.Clear
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import java.util.*

/**
 * Campo di testo di sola lettura che apre un DatePickerDialog nativo
 * e permette di azzerare la data selezionata.
 *
 * Il campo vero e proprio è avvolto in un Box con un livello trasparente
 * e cliccabile sopra il testo: un OutlinedTextField, anche se readOnly,
 * intercetta comunque il tocco per posizionare il cursore, quindi un
 * clickable applicato direttamente al campo non si attiva sempre in modo
 * affidabile. Il livello sopra garantisce che ogni tocco apra il calendario.
 * Il pulsante "rimuovi data" resta fuori da questo overlay, come elemento
 * separato, così rimane sempre utilizzabile.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun DatePickerField(
    label: String,
    dateMillis: Long?,
    onDateSelected: (Long?) -> Unit,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val calendar = remember { Calendar.getInstance() }

    fun openPicker() {
        val base = dateMillis?.let { Calendar.getInstance().apply { timeInMillis = it } }
            ?: calendar
        android.app.DatePickerDialog(
            context,
            { _, year, month, dayOfMonth ->
                val cal = Calendar.getInstance()
                cal.set(year, month, dayOfMonth, 0, 0, 0)
                cal.set(Calendar.MILLISECOND, 0)
                onDateSelected(cal.timeInMillis)
            },
            base.get(Calendar.YEAR),
            base.get(Calendar.MONTH),
            base.get(Calendar.DAY_OF_MONTH)
        ).show()
    }

    Row(modifier = modifier, verticalAlignment = Alignment.CenterVertically) {
        Box(modifier = Modifier.weight(1f)) {
            OutlinedTextField(
                value = formatDate(dateMillis),
                onValueChange = {},
                readOnly = true,
                label = { Text(label) },
                trailingIcon = {
                    Icon(Icons.Default.CalendarMonth, contentDescription = null)
                },
                modifier = Modifier.fillMaxWidth()
            )
            // Livello trasparente sopra il campo: cattura il tocco per
            // aprire il calendario prima che arrivi al testo sottostante.
            Box(
                modifier = Modifier
                    .matchParentSize()
                    .clickable(
                        interactionSource = remember { MutableInteractionSource() },
                        indication = null
                    ) { openPicker() }
            )
        }
        if (dateMillis != null) {
            IconButton(onClick = { onDateSelected(null) }) {
                Icon(Icons.Default.Clear, contentDescription = "Rimuovi data")
            }
        }
    }
}
