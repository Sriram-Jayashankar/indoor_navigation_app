package com.example.navitest.userinterface

import androidx.compose.foundation.layout.*
import androidx.compose.material3.Button
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavHostController
import com.example.navitest.NavitestViewModel
import com.example.navitest.navigation.Screen

@Composable
fun LiveScanScreen(
    navController: NavHostController,
    viewModel: NavitestViewModel = viewModel()
) {
    val results by remember { derivedStateOf { viewModel.scanResults.toList() } }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp)
    ) {
        Text("📡 Live Scan\n\n— scanning Wi‑Fi (not yet implemented)")
        Spacer(Modifier.height(24.dp))

        if (results.isEmpty()) {
            CircularProgressIndicator()
        } else {
            Text("Scan Results:")
            results.forEach { (ssid, rssi) ->
                Text("$ssid → $rssi dBm")
            }
        }

        Spacer(Modifier.weight(1f))
        Button(
            onClick = { navController.navigate(Screen.Trilateration.route) },
            modifier = Modifier.fillMaxWidth()
        ) {
            Text("Compute Position")
        }
    }
}
