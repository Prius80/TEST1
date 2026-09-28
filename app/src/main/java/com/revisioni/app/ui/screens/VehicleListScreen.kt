package com.revisioni.app.ui.screens

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.DirectionsCar
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.TwoWheeler
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.revisioni.app.BuildConfig
import com.revisioni.app.data.Maintenance
import com.revisioni.app.data.Vehicle
import com.revisioni.app.data.VehicleType
import com.revisioni.app.ui.AppViewModel
import com.revisioni.app.ui.components.deadlineColor
import com.revisioni.app.ui.components.deadlineLabel
import com.revisioni.app.ui.components.formatDate

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun VehicleListScreen(
    viewModel: AppViewModel,
    onAddVehicle: () -> Unit,
    onOpenVehicle: (Long) -> Unit
) {
    val vehicles by viewModel.vehicles.collectAsStateWithLifecycle(initialValue = emptyList())
    val allMaintenances by viewModel.allMaintenances.collectAsStateWithLifecycle(initialValue = emptyList())
    var showAboutDialog by remember { mutableStateOf(false) }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Le mie scadenze") },
                actions = {
                    IconButton(onClick = { showAboutDialog = true }) {
                        Icon(Icons.Default.Info, contentDescription = "Informazioni sull'app")
                    }
                }
            )
        },
        floatingActionButton = {
            FloatingActionButton(onClick = onAddVehicle) {
                Icon(Icons.Default.Add, contentDescription = "Aggiungi veicolo")
            }
        }
    ) { padding ->
        if (vehicles.isEmpty()) {
            Box(
                modifier = Modifier.fillMaxSize().padding(padding),
                contentAlignment = Alignment.Center
            ) {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Text("Nessun veicolo ancora", style = MaterialTheme.typography.titleMedium)
                    Spacer(Modifier.height(8.dp))
                    Text(
                        "Tocca + per aggiungere la tua prima auto o moto",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
        } else {
            LazyColumn(
                modifier = Modifier.fillMaxSize().padding(padding),
                contentPadding = PaddingValues(16.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                items(vehicles, key = { it.id }) { vehicle ->
                    val nextMaintenance = allMaintenances
                        .filter { it.vehicleId == vehicle.id && it.prossimaScadenza != null }
                        .minByOrNull { it.prossimaScadenza!! }
                    VehicleCard(
                        vehicle = vehicle,
                        nextMaintenance = nextMaintenance,
                        onClick = { onOpenVehicle(vehicle.id) }
                    )
                }
            }
        }
    }

    if (showAboutDialog) {
        AlertDialog(
            onDismissRequest = { showAboutDialog = false },
            title = { Text("Le mie scadenze") },
            text = {
                Column {
                    Text("Versione ${BuildConfig.VERSION_NAME}")
                    Spacer(Modifier.height(8.dp))
                    Text("Sviluppato da Roberto De Paolis")
                }
            },
            confirmButton = {
                TextButton(onClick = { showAboutDialog = false }) { Text("Chiudi") }
            }
        )
    }
}

@Composable
private fun VehicleCard(vehicle: Vehicle, nextMaintenance: Maintenance?, onClick: () -> Unit) {
    ElevatedCard(onClick = onClick, modifier = Modifier.fillMaxWidth()) {
        Column(modifier = Modifier.padding(16.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(
                    imageVector = if (vehicle.type == VehicleType.AUTO) Icons.Default.DirectionsCar else Icons.Default.TwoWheeler,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.primary
                )
                Spacer(Modifier.width(8.dp))
                Column {
                    Text(vehicle.name, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
                    if (vehicle.targa.isNotBlank()) {
                        Text(vehicle.targa, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    }
                }
            }
            Spacer(Modifier.height(12.dp))
            DeadlineRow("Revisione", vehicle.revisioneScadenza)
            Spacer(Modifier.height(4.dp))
            DeadlineRow("Assicurazione", vehicle.assicurazioneScadenza)
            if (nextMaintenance != null) {
                Spacer(Modifier.height(4.dp))
                DeadlineRow(nextMaintenance.tipo, nextMaintenance.prossimaScadenza)
            }
        }
    }
}

@Composable
private fun DeadlineRow(label: String, dateMillis: Long?) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text("$label: ${formatDate(dateMillis)}", style = MaterialTheme.typography.bodyMedium)
        Text(
            deadlineLabel(dateMillis),
            style = MaterialTheme.typography.labelMedium,
            color = deadlineColor(dateMillis),
            fontWeight = FontWeight.SemiBold
        )
    }
}
