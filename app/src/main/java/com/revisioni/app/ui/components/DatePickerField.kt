package com.revisioni.app.ui.components

import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CalendarMonth
import androidx.compose.material.icons.filled.Clear
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import java.util.*

/**
 * Campo di testo di sola lettura che apre un DatePickerDialog nativo
 * e permette di azzerare la data selezionata.
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

    OutlinedTextField(
        value = formatDate(dateMillis),
        onValueChange = {},
        readOnly = true,
        label = { Text(label) },
        trailingIcon = {
            Row {
                if (dateMillis != null) {
                    IconButton(onClick = { onDateSelected(null) }) {
                        Icon(Icons.Default.Clear, contentDescription = "Rimuovi data")
                    }
                }
                Icon(Icons.Default.CalendarMonth, contentDescription = null)
            }
        },
        modifier = modifier.clickable(
            interactionSource = remember { MutableInteractionSource() },
            indication = null
        ) {
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
    )
}

@Composable
private fun Row(content: @Composable () -> Unit) {
    androidx.compose.foundation.layout.Row(content = { content() })
}
