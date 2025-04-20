package com.example.navitest.navigation

import androidx.compose.runtime.Composable
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavHostController
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import com.example.navitest.NavitestViewModel
import com.example.navitest.userinterface.*

sealed class Screen(val route: String) {
    object Home : Screen("home")
    object FloorMap : Screen("floor_map")
    object RouterPlacement : Screen("router_placement")
    object ScanConfig : Screen("scan_config")
    object LiveScan : Screen("live_scan")
    object Trilateration : Screen("trilateration")
    object Results : Screen("results")
    object Settings : Screen("settings")
}

@Composable
fun NavGraph(
    navController: NavHostController,
    viewModel: NavitestViewModel = viewModel()
) {
    NavHost(navController = navController, startDestination = Screen.Home.route) {
        composable(Screen.Home.route) {
            HomeScreen(viewModel = viewModel, navController = navController)
        }
        composable(Screen.FloorMap.route) {
            FloorMapScreen(viewModel = viewModel, navController = navController)
        }
        composable(Screen.RouterPlacement.route) {
            RouterPlacementScreen(viewModel = viewModel, navController = navController)
        }
        composable(Screen.ScanConfig.route) {
            ScanConfigScreen(viewModel = viewModel, navController = navController)
        }
        composable(Screen.LiveScan.route) {
            LiveScanScreen(viewModel = viewModel, navController = navController)
        }
        composable(Screen.Trilateration.route) {
            TrilaterationScreen(viewModel = viewModel, navController = navController)
        }
        composable(Screen.Results.route) {
            ResultsScreen(viewModel = viewModel, navController = navController)
        }
        composable(Screen.Settings.route) {
            SettingsScreen(viewModel = viewModel, navController = navController)
        }
    }
}
