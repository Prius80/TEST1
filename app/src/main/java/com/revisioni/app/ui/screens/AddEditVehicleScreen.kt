package com.revisioni.app.ui.screens

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.revisioni.app.data.Vehicle
import com.revisioni.app.data.VehicleType
import com.revisioni.app.ui.AppViewModel
import com.revisioni.app.ui.components.DatePickerField

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AddEditVehicleScreen(
    viewModel: AppViewModel,
    vehicleId: Long?,
    onDone: () -> Unit,
    onCancel: () -> Unit
) {
    val isEditing = vehicleId != null
    val existingVehicle by if (isEditing) {
        viewModel.vehicle(vehicleId!!).collectAsStateWithLifecycle(initialValue = null)
    } else {
        remember { mutableStateOf<Vehicle?>(null) }
    }

    var name by remember { mutableStateOf("") }
    var type by remember { mutableStateOf(VehicleType.AUTO) }
    var targa by remember { mutableStateOf("") }
    var marca by remember { mutableStateOf("") }
    var modello by remember { mutableStateOf("") }
    var anno by remember { mutableStateOf("") }
    var revisione by remember { mutableStateOf<Long?>(null) }
    var assicurazione by remember { mutableStateOf<Long?>(null) }
    var bollo by remember { mutableStateOf<Long?>(null) }
    var note by remember { mutableStateOf("") }
    var loadedOnce by remember { mutableStateOf(false) }

    LaunchedEffect(existingVehicle) {
        val v = existingVehicle
        if (v != null && !loadedOnce) {
            name = v.name
            type = v.type
            targa = v.targa
            marca = v.marca
            modello = v.modello
            anno = v.anno?.toString() ?: ""
            revisione = v.revisioneScadenza
            assicurazione = v.assicurazioneScadenza
            bollo = v.bolloScadenza
            note = v.note
            loadedOnce = true
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(if (isEditing) "Modifica veicolo" else "Nuovo veicolo") },
                navigationIcon = {
                    IconButton(onClick = onCancel) {
                        Icon(Icons.Default.ArrowBack, contentDescription = "Indietro")
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
            SingleChoiceSegmented(
                selected = type,
                onSelected = { type = it }
            )

            OutlinedTextField(
                value = name,
                onValueChange = { name = it },
                label = { Text("Nome (es. \"Panda di Marco\")") },
                modifier = Modifier.fillMaxWidth()
            )

            OutlinedTextField(
                value = targa,
                onValueChange = { targa = it },
                label = { Text("Targa") },
                modifier = Modifier.fillMaxWidth()
            )

            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                OutlinedTextField(
                    value = marca,
                    onValueChange = { marca = it },
                    label = { Text("Marca") },
                    modifier = Modifier.weight(1f)
                )
                OutlinedTextField(
                    value = modello,
                    onValueChange = { modello = it },
                    label = { Text("Modello") },
                    modifier = Modifier.weight(1f)
                )
            }

            OutlinedTextField(
                value = anno,
                onValueChange = { anno = it.filter { c -> c.isDigit() }.take(4) },
                label = { Text("Anno") },
                keyboardOptions = androidx.compose.foundation.text.KeyboardOptions(keyboardType = KeyboardType.Number),
                modifier = Modifier.fillMaxWidth()
            )

            Divider(modifier = Modifier.padding(vertical = 4.dp))
            Text("Scadenze", style = MaterialTheme.typography.titleMedium)

            DatePickerField(
                label = "Scadenza revisione",
                dateMillis = revisione,
                onDateSelected = { revisione = it },
                modifier = Modifier.fillMaxWidth()
            )

            DatePickerField(
                label = "Scadenza assicurazione",
                dateMillis = assicurazione,
                onDateSelected = { assicurazione = it },
                modifier = Modifier.fillMaxWidth()
            )

            DatePickerField(
                label = "Scadenza bollo (opzionale)",
                dateMillis = bollo,
                onDateSelected = { bollo = it },
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
                    val vehicle = Vehicle(
                        id = vehicleId ?: 0,
                        name = name.ifBlank { "Veicolo" },
                        type = type,
                        targa = targa,
                        marca = marca,
                        modello = modello,
                        anno = anno.toIntOrNull(),
                        revisioneScadenza = revisione,
                        assicurazioneScadenza = assicurazione,
                        bolloScadenza = bollo,
                        note = note
                    )
                    if (isEditing) {
                        viewModel.updateVehicle(vehicle)
                        onDone()
                    } else {
                        viewModel.addVehicle(vehicle) { onDone() }
                    }
                },
                modifier = Modifier.fillMaxWidth()
            ) {
                Text(if (isEditing) "Salva modifiche" else "Aggiungi veicolo")
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun SingleChoiceSegmented(selected: VehicleType, onSelected: (VehicleType) -> Unit) {
    SingleChoiceSegmentedButtonRow(modifier = Modifier.fillMaxWidth()) {
        VehicleType.values().forEachIndexed { index, option ->
            SegmentedButton(
                selected = selected == option,
                onClick = { onSelected(option) },
                shape = SegmentedButtonDefaults.itemShape(index = index, count = VehicleType.values().size)
            ) {
                Text(if (option == VehicleType.AUTO) "Auto" else "Moto")
            }
        }
    }
}
