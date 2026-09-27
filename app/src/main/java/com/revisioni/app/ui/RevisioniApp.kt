package com.revisioni.app.ui

import androidx.compose.runtime.Composable
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavType
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import androidx.navigation.navArgument
import com.revisioni.app.ui.screens.*

object Routes {
    const val LIST = "list"
    const val VEHICLE_ADD = "vehicle/add"
    const val VEHICLE_EDIT = "vehicle/edit/{vehicleId}"
    const val VEHICLE_DETAIL = "vehicle/detail/{vehicleId}"
    const val MAINTENANCE_ADD = "maintenance/add/{vehicleId}"
    const val MAINTENANCE_EDIT = "maintenance/edit/{vehicleId}/{maintenanceId}"

    fun vehicleEdit(id: Long) = "vehicle/edit/$id"
    fun vehicleDetail(id: Long) = "vehicle/detail/$id"
    fun maintenanceAdd(vehicleId: Long) = "maintenance/add/$vehicleId"
    fun maintenanceEdit(vehicleId: Long, maintenanceId: Long) = "maintenance/edit/$vehicleId/$maintenanceId"
}

@Composable
fun RevisioniApp() {
    val navController = rememberNavController()
    val application = androidx.compose.ui.platform.LocalContext.current.applicationContext as android.app.Application
    val viewModel: AppViewModel = viewModel(factory = AppViewModel.factory(application))

    NavHost(navController = navController, startDestination = Routes.LIST) {

        composable(Routes.LIST) {
            VehicleListScreen(
                viewModel = viewModel,
                onAddVehicle = { navController.navigate(Routes.VEHICLE_ADD) },
                onOpenVehicle = { id -> navController.navigate(Routes.vehicleDetail(id)) }
            )
        }

        composable(Routes.VEHICLE_ADD) {
            AddEditVehicleScreen(
                viewModel = viewModel,
                vehicleId = null,
                onDone = { navController.popBackStack() },
                onCancel = { navController.popBackStack() }
            )
        }

        composable(
            Routes.VEHICLE_EDIT,
            arguments = listOf(navArgument("vehicleId") { type = NavType.LongType })
        ) { backStackEntry ->
            val vehicleId = backStackEntry.arguments?.getLong("vehicleId") ?: 0L
            AddEditVehicleScreen(
                viewModel = viewModel,
                vehicleId = vehicleId,
                onDone = { navController.popBackStack() },
                onCancel = { navController.popBackStack() }
            )
        }

        composable(
            Routes.VEHICLE_DETAIL,
            arguments = listOf(navArgument("vehicleId") { type = NavType.LongType })
        ) { backStackEntry ->
            val vehicleId = backStackEntry.arguments?.getLong("vehicleId") ?: 0L
            VehicleDetailScreen(
                viewModel = viewModel,
                vehicleId = vehicleId,
                onBack = { navController.popBackStack() },
                onEditVehicle = { navController.navigate(Routes.vehicleEdit(vehicleId)) },
                onAddMaintenance = { navController.navigate(Routes.maintenanceAdd(vehicleId)) },
                onEditMaintenance = { maintenanceId ->
                    navController.navigate(Routes.maintenanceEdit(vehicleId, maintenanceId))
                },
                onVehicleDeleted = { navController.popBackStack(Routes.LIST, false) }
            )
        }

        composable(
            Routes.MAINTENANCE_ADD,
            arguments = listOf(navArgument("vehicleId") { type = NavType.LongType })
        ) { backStackEntry ->
            val vehicleId = backStackEntry.arguments?.getLong("vehicleId") ?: 0L
            AddEditMaintenanceScreen(
                viewModel = viewModel,
                vehicleId = vehicleId,
                maintenanceId = null,
                onDone = { navController.popBackStack() },
                onCancel = { navController.popBackStack() }
            )
        }

        composable(
            Routes.MAINTENANCE_EDIT,
            arguments = listOf(
                navArgument("vehicleId") { type = NavType.LongType },
                navArgument("maintenanceId") { type = NavType.LongType }
            )
        ) { backStackEntry ->
            val vehicleId = backStackEntry.arguments?.getLong("vehicleId") ?: 0L
            val maintenanceId = backStackEntry.arguments?.getLong("maintenanceId") ?: 0L
            AddEditMaintenanceScreen(
                viewModel = viewModel,
                vehicleId = vehicleId,
                maintenanceId = maintenanceId,
                onDone = { navController.popBackStack() },
                onCancel = { navController.popBackStack() }
            )
        }
    }
}
