package com.example.navitest.navigation

import androidx.compose.runtime.Composable
import androidx.navigation.NavHostController
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import com.example.navitest.userinterface.*

sealed class Screen(val route: String) {
    object Home               : Screen("home")
    object FloorMap           : Screen("floor_map")
    object RouterPlacement    : Screen("router_placement")
    object ScanConfig         : Screen("scan_config")
    object LiveScan           : Screen("live_scan")
    object Trilateration      : Screen("trilateration")
    object Results            : Screen("results")
    object Settings           : Screen("settings")
}

@Composable
fun NavGraph(navController: NavHostController) {
    NavHost(navController = navController, startDestination = Screen.Home.route) {
        composable(Screen.Home.route)            { HomeScreen(navController) }
        composable(Screen.FloorMap.route)        { FloorMapScreen(navController) }
        composable(Screen.RouterPlacement.route) { RouterPlacementScreen(navController) }
        composable(Screen.ScanConfig.route)      { ScanConfigScreen(navController) }
        composable(Screen.LiveScan.route)        { LiveScanScreen(navController) }
        composable(Screen.Trilateration.route)   { TrilaterationScreen(navController) }
        composable(Screen.Results.route)         { ResultsScreen(navController) }
        composable(Screen.Settings.route)        { SettingsScreen(navController) }
    }
}
