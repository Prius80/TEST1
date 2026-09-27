package com.revisioni.app.ui.screens

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.revisioni.app.data.Maintenance
import com.revisioni.app.ui.AppViewModel
import com.revisioni.app.ui.components.DatePickerField
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
    var data by remember { mutableStateOf(Calendar.getInstance().timeInMillis) }
    var km by remember { mutableStateOf("") }
    var costo by remember { mutableStateOf("") }
    var officina by remember { mutableStateOf("") }
    var note by remember { mutableStateOf("") }
    var prossimaScadenza by remember { mutableStateOf<Long?>(null) }
    var loadedOnce by remember { mutableStateOf(false) }
    var showDeleteDialog by remember { mutableStateOf(false) }

    LaunchedEffect(existing) {
        if (existing != null && !loadedOnce) {
            tipo = existing.tipo
            data = existing.data
            km = existing.km?.toString() ?: ""
            costo = existing.costo?.toString() ?: ""
            officina = existing.officina
            note = existing.note
            prossimaScadenza = existing.prossimaScadenza
            loadedOnce = true
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
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            OutlinedTextField(
                value = tipo,
                onValueChange = { tipo = it },
                label = { Text("Tipo (es. Tagliando, Cambio olio, Pneumatici)") },
                modifier = Modifier.fillMaxWidth()
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

            DatePickerField(
                label = "Promemoria prossima manutenzione (opzionale)",
                dateMillis = prossimaScadenza,
                onDateSelected = { prossimaScadenza = it },
                modifier = Modifier.fillMaxWidth()
            )

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
                        prossimaScadenza = prossimaScadenza
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
