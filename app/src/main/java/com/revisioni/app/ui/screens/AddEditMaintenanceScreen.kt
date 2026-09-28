package com.revisioni.app.ui.screens

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.ArrowDropDown
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.revisioni.app.data.Maintenance
import com.revisioni.app.data.TipiManutenzione
import com.revisioni.app.ui.AppViewModel
import com.revisioni.app.ui.components.DatePickerField
import com.revisioni.app.ui.components.addMonths
import com.revisioni.app.ui.components.formatDate
import java.util.*

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AddEditMaintenanceScreen(
    viewModel: AppViewModel,
    vehicleId: Long,
    maintenanceId: Long?,
    onDone: () -> Unit,
    onCancel: () -> Unit
) {
    val isEditing = maintenanceId != null
    val maintenances by viewModel.maintenancesFor(vehicleId).collectAsStateWithLifecycle(initialValue = emptyList())
    val existing = remember(maintenances, maintenanceId) {
        maintenances.find { it.id == maintenanceId }
    }

    var tipo by remember { mutableStateOf("") }
    var tipoPersonalizzato by remember { mutableStateOf(false) }
    var data by remember { mutableStateOf(Calendar.getInstance().timeInMillis) }
    var km by remember { mutableStateOf("") }
    var costo by remember { mutableStateOf("") }
    var officina by remember { mutableStateOf("") }
    var note by remember { mutableStateOf("") }
    var prossimaScadenza by remember { mutableStateOf<Long?>(null) }
    var intervalloMesi by remember { mutableStateOf("") }
    var intervalloKm by remember { mutableStateOf("") }
    var loadedOnce by remember { mutableStateOf(false) }
    var showDeleteDialog by remember { mutableStateOf(false) }

    LaunchedEffect(existing) {
        if (existing != null && !loadedOnce) {
            tipo = existing.tipo
            tipoPersonalizzato = existing.tipo !in TipiManutenzione.comuni
            data = existing.data
            km = existing.km?.toString() ?: ""
            costo = existing.costo?.toString() ?: ""
            officina = existing.officina
            note = existing.note
            prossimaScadenza = existing.prossimaScadenza
            intervalloMesi = existing.intervalloMesi?.toString() ?: ""
            intervalloKm = existing.intervalloKm?.toString() ?: ""
            loadedOnce = true
        }
    }

    // Se è impostato un intervallo ricorrente in mesi, calcola automaticamente
    // la data del prossimo promemoria a partire dalla data di esecuzione.
    LaunchedEffect(intervalloMesi, data) {
        val mesi = intervalloMesi.toIntOrNull()
        if (mesi != null && mesi > 0) {
            prossimaScadenza = addMonths(data, mesi)
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(if (isEditing) "Modifica manutenzione" else "Nuova manutenzione") },
                navigationIcon = {
                    IconButton(onClick = onCancel) {
                        Icon(Icons.Default.ArrowBack, contentDescription = "Indietro")
                    }
                },
                actions = {
                    if (isEditing) {
                        IconButton(onClick = { showDeleteDialog = true }) {
                            Icon(Icons.Default.Delete, contentDescription = "Elimina")
                        }
                    }
                }
            )
        }
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .verticalScroll(rememberScrollState())
                .imePadding()
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            MaintenanceTypeField(
                tipo = tipo,
                isCustom = tipoPersonalizzato,
                onTipoSelected = { selected ->
                    if (selected == "Altro") {
                        tipoPersonalizzato = true
                        tipo = ""
                    } else {
                        tipoPersonalizzato = false
                        tipo = selected
                    }
                },
                onCustomTextChanged = { tipo = it }
            )

            DatePickerField(
                label = "Data esecuzione",
                dateMillis = data,
                onDateSelected = { it?.let { d -> data = d } },
                modifier = Modifier.fillMaxWidth()
            )

            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                OutlinedTextField(
                    value = km,
                    onValueChange = { km = it.filter { c -> c.isDigit() } },
                    label = { Text("Km") },
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                    modifier = Modifier.weight(1f)
                )
                OutlinedTextField(
                    value = costo,
                    onValueChange = { costo = it.filter { c -> c.isDigit() || c == '.' } },
                    label = { Text("Costo (€)") },
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                    modifier = Modifier.weight(1f)
                )
            }

            OutlinedTextField(
                value = officina,
                onValueChange = { officina = it },
                label = { Text("Officina") },
                modifier = Modifier.fillMaxWidth()
            )

            Divider(modifier = Modifier.padding(vertical = 4.dp))
            Text("Promemoria ricorrente", style = MaterialTheme.typography.titleMedium)
            Text(
                "Imposta ogni quanto ripetere questa manutenzione: la prossima scadenza " +
                    "verrà calcolata automaticamente e riceverai una notifica quando si avvicina.",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )

            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                OutlinedTextField(
                    value = intervalloMesi,
                    onValueChange = { intervalloMesi = it.filter { c -> c.isDigit() } },
                    label = { Text("Ogni tot mesi") },
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                    modifier = Modifier.weight(1f)
                )
                OutlinedTextField(
                    value = intervalloKm,
                    onValueChange = { intervalloKm = it.filter { c -> c.isDigit() } },
                    label = { Text("Ogni tot km") },
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                    modifier = Modifier.weight(1f)
                )
            }

            DatePickerField(
                label = "Prossima scadenza (promemoria)",
                dateMillis = prossimaScadenza,
                onDateSelected = { prossimaScadenza = it },
                modifier = Modifier.fillMaxWidth()
            )
            if (intervalloMesi.toIntOrNull() != null && prossimaScadenza != null) {
                Text(
                    "Calcolata automaticamente: ${formatDate(prossimaScadenza)}. " +
                        "Puoi comunque modificarla a mano toccandola.",
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }

            Divider(modifier = Modifier.padding(vertical = 4.dp))

            OutlinedTextField(
                value = note,
                onValueChange = { note = it },
                label = { Text("Note") },
                minLines = 2,
                modifier = Modifier.fillMaxWidth()
            )

            Spacer(Modifier.height(8.dp))

            Button(
                onClick = {
                    val maintenance = Maintenance(
                        id = maintenanceId ?: 0,
                        vehicleId = vehicleId,
                        tipo = tipo.ifBlank { "Manutenzione" },
                        data = data,
                        km = km.toIntOrNull(),
                        costo = costo.toDoubleOrNull(),
                        officina = officina,
                        note = note,
                        prossimaScadenza = prossimaScadenza,
                        intervalloMesi = intervalloMesi.toIntOrNull(),
                        intervalloKm = intervalloKm.toIntOrNull()
                    )
                    if (isEditing) {
                        viewModel.updateMaintenance(maintenance)
                    } else {
                        viewModel.addMaintenance(maintenance)
                    }
                    onDone()
                },
                modifier = Modifier.fillMaxWidth()
            ) {
                Text(if (isEditing) "Salva modifiche" else "Aggiungi manutenzione")
            }
        }
    }

    if (showDeleteDialog && existing != null) {
        AlertDialog(
            onDismissRequest = { showDeleteDialog = false },
            title = { Text("Eliminare questa manutenzione?") },
            confirmButton = {
                TextButton(onClick = {
                    viewModel.deleteMaintenance(existing)
                    showDeleteDialog = false
                    onDone()
                }) { Text("Elimina") }
            },
            dismissButton = {
                TextButton(onClick = { showDeleteDialog = false }) { Text("Annulla") }
            }
        )
    }
}

/**
 * Menu a tendina con i tipi di manutenzione più comuni. Selezionando "Altro"
 * si passa a un campo di testo libero per inserire un tipo personalizzato.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun MaintenanceTypeField(
    tipo: String,
    isCustom: Boolean,
    onTipoSelected: (String) -> Unit,
    onCustomTextChanged: (String) -> Unit
) {
    if (isCustom) {
        OutlinedTextField(
            value = tipo,
            onValueChange = onCustomTextChanged,
            label = { Text("Tipo di manutenzione") },
            trailingIcon = {
                TextButton(onClick = { onTipoSelected(TipiManutenzione.comuni.first()) }) {
                    Text("Scegli")
                }
            },
            modifier = Modifier.fillMaxWidth()
        )
    } else {
        var expanded by remember { mutableStateOf(false) }
        ExposedDropdownMenuBox(
            expanded = expanded,
            onExpandedChange = { expanded = it }
        ) {
            OutlinedTextField(
                value = tipo,
                onValueChange = {},
                readOnly = true,
                label = { Text("Tipo di manutenzione") },
                trailingIcon = {
                    Icon(Icons.Default.ArrowDropDown, contentDescription = null)
                },
                modifier = Modifier
                    .fillMaxWidth()
                    .menuAnchor()
            )
            ExposedDropdownMenu(expanded = expanded, onDismissRequest = { expanded = false }) {
                TipiManutenzione.comuni.forEach { option ->
                    DropdownMenuItem(
                        text = { Text(option) },
                        onClick = {
                            onTipoSelected(option)
                            expanded = false
                        }
                    )
                }
            }
        }
    }
}
