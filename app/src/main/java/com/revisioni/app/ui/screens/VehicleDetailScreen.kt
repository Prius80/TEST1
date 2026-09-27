package com.revisioni.app.ui.screens

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.revisioni.app.data.Maintenance
import com.revisioni.app.data.Vehicle
import com.revisioni.app.ui.AppViewModel
import com.revisioni.app.ui.components.deadlineColor
import com.revisioni.app.ui.components.deadlineLabel
import com.revisioni.app.ui.components.formatDate

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun VehicleDetailScreen(
    viewModel: AppViewModel,
    vehicleId: Long,
    onBack: () -> Unit,
    onEditVehicle: () -> Unit,
    onAddMaintenance: () -> Unit,
    onEditMaintenance: (Long) -> Unit,
    onVehicleDeleted: () -> Unit
) {
    val vehicle by viewModel.vehicle(vehicleId).collectAsStateWithLifecycle(initialValue = null)
    val maintenances by viewModel.maintenancesFor(vehicleId).collectAsStateWithLifecycle(initialValue = emptyList())
    var showDeleteDialog by remember { mutableStateOf(false) }

    val currentVehicle = vehicle

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(currentVehicle?.name ?: "Veicolo") },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.Default.ArrowBack, contentDescription = "Indietro")
                    }
                },
                actions = {
                    IconButton(onClick = onEditVehicle) {
                        Icon(Icons.Default.Edit, contentDescription = "Modifica")
                    }
                    IconButton(onClick = { showDeleteDialog = true }) {
                        Icon(Icons.Default.Delete, contentDescription = "Elimina")
                    }
                }
            )
        },
        floatingActionButton = {
            ExtendedFloatingActionButton(
                onClick = onAddMaintenance,
                icon = { Icon(Icons.Default.Add, contentDescription = null) },
                text = { Text("Manutenzione") }
            )
        }
    ) { padding ->
        if (currentVehicle == null) {
            Box(Modifier.fillMaxSize().padding(padding), contentAlignment = Alignment.Center) {
                CircularProgressIndicator()
            }
            return@Scaffold
        }

        LazyColumn(
            modifier = Modifier.fillMaxSize().padding(padding),
            contentPadding = PaddingValues(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            item { VehicleInfoCard(currentVehicle) }
            if (maintenances.isNotEmpty()) {
                item { MaintenanceStatsCard(maintenances) }
            }
            item {
                Text("Manutenzioni", style = MaterialTheme.typography.titleMedium)
            }
            if (maintenances.isEmpty()) {
                item {
                    Text(
                        "Nessuna manutenzione registrata",
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            } else {
                items(maintenances, key = { it.id }) { m ->
                    MaintenanceCard(m, onClick = { onEditMaintenance(m.id) })
                }
            }
        }
    }

    if (showDeleteDialog) {
        currentVehicle?.let { vehicleToDelete ->
            AlertDialog(
                onDismissRequest = { showDeleteDialog = false },
                title = { Text("Eliminare il veicolo?") },
                text = { Text("Verranno eliminate anche tutte le manutenzioni associate. L'operazione non è reversibile.") },
                confirmButton = {
                    TextButton(onClick = {
                        viewModel.deleteVehicle(vehicleToDelete)
                        showDeleteDialog = false
                        onVehicleDeleted()
                    }) { Text("Elimina") }
                },
                dismissButton = {
                    TextButton(onClick = { showDeleteDialog = false }) { Text("Annulla") }
                }
            )
        }
    }
}

@Composable
private fun VehicleInfoCard(vehicle: Vehicle) {
    ElevatedCard(modifier = Modifier.fillMaxWidth()) {
        Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
            if (vehicle.targa.isNotBlank() || vehicle.marca.isNotBlank() || vehicle.modello.isNotBlank()) {
                Text(
                    listOfNotNull(
                        vehicle.marca.takeIf { it.isNotBlank() },
                        vehicle.modello.takeIf { it.isNotBlank() },
                        vehicle.anno?.toString()
                    ).joinToString(" · "),
                    style = MaterialTheme.typography.bodyMedium
                )
                if (vehicle.targa.isNotBlank()) {
                    Text("Targa: ${vehicle.targa}", style = MaterialTheme.typography.bodySmall)
                }
                Divider()
            }
            DeadlineLine("Revisione", vehicle.revisioneScadenza)
            DeadlineLine("Assicurazione", vehicle.assicurazioneScadenza)
            if (vehicle.bolloScadenza != null) {
                DeadlineLine("Bollo", vehicle.bolloScadenza)
            }
            if (vehicle.note.isNotBlank()) {
                Divider()
                Text(vehicle.note, style = MaterialTheme.typography.bodySmall)
            }
        }
    }
}

@Composable
private fun DeadlineLine(label: String, dateMillis: Long?) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Column {
            Text(label, style = MaterialTheme.typography.bodyMedium, fontWeight = FontWeight.Medium)
            Text(formatDate(dateMillis), style = MaterialTheme.typography.bodySmall)
        }
        Text(
            deadlineLabel(dateMillis),
            color = deadlineColor(dateMillis),
            style = MaterialTheme.typography.labelMedium,
            fontWeight = FontWeight.SemiBold
        )
    }
}

@Composable
private fun MaintenanceStatsCard(maintenances: List<Maintenance>) {
    val totale = maintenances.sumOf { it.costo ?: 0.0 }
    val numero = maintenances.size
    val ultima = maintenances.maxByOrNull { it.data }

    ElevatedCard(modifier = Modifier.fillMaxWidth()) {
        Row(
            modifier = Modifier.fillMaxWidth().padding(16.dp),
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            StatItem(label = "Interventi", value = numero.toString())
            StatItem(label = "Speso totale", value = "€%.2f".format(totale))
            StatItem(label = "Ultima", value = ultima?.let { formatDate(it.data) } ?: "-")
        }
    }
}

@Composable
private fun StatItem(label: String, value: String) {
    Column(horizontalAlignment = Alignment.CenterHorizontally) {
        Text(value, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
        Text(label, style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
    }
}

@Composable
private fun MaintenanceCard(m: Maintenance, onClick: () -> Unit) {
    ElevatedCard(onClick = onClick, modifier = Modifier.fillMaxWidth()) {
        Column(modifier = Modifier.padding(16.dp)) {
            Row(horizontalArrangement = Arrangement.SpaceBetween, modifier = Modifier.fillMaxWidth()) {
                Text(m.tipo, fontWeight = FontWeight.Bold)
                Text(formatDate(m.data), style = MaterialTheme.typography.bodySmall)
            }
            if (m.km != null || m.costo != null) {
                Spacer(Modifier.height(4.dp))
                Text(
                    listOfNotNull(
                        m.km?.let { "$it km" },
                        m.costo?.let { "€%.2f".format(it) }
                    ).joinToString(" · "),
                    style = MaterialTheme.typography.bodySmall
                )
            }
            if (m.officina.isNotBlank()) {
                Text(m.officina, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
            if (m.prossimaScadenza != null) {
                Spacer(Modifier.height(4.dp))
                Text(
                    "Prossima: ${formatDate(m.prossimaScadenza)} · ${deadlineLabel(m.prossimaScadenza)}",
                    style = MaterialTheme.typography.labelMedium,
                    color = deadlineColor(m.prossimaScadenza)
                )
            }
            if (m.intervalloMesi != null || m.intervalloKm != null) {
                Text(
                    listOfNotNull(
                        m.intervalloMesi?.let { "ogni $it mesi" },
                        m.intervalloKm?.let { "ogni $it km" }
                    ).joinToString(" · "),
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }
    }
}
