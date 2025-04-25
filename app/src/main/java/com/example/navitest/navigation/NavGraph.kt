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
    object PathGraphEditor : Screen("path_graph_editor")
    object AStarTest : Screen("astar_test")
    object SavedPlans : Screen("saved_plans")
    object FindLocation : Screen("find_location")




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
        composable(Screen.PathGraphEditor.route) {
            PathGraphEditorScreen(viewModel = viewModel,navController = navController)
        }
        composable(Screen.AStarTest.route) {
            AStarTestScreen(viewModel = viewModel,navController = navController)
        }
        composable(Screen.SavedPlans.route) {
            SavedPlansScreen(viewModel = viewModel,navController = navController)
        }
        composable(Screen.FindLocation.route) {
            FindLocationScreen(viewModel = viewModel(), navController = navController)
        }




    }
}
