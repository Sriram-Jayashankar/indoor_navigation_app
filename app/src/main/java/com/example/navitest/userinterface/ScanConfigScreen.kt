package com.example.navitest.userinterface

import androidx.compose.foundation.layout.*
import androidx.compose.material3.Button
import androidx.compose.material3.Slider
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavHostController
import com.example.navitest.NavitestViewModel
import com.example.navitest.navigation.Screen

@Composable
fun ScanConfigScreen(
    navController: NavHostController,
    viewModel: NavitestViewModel = viewModel()
) {
    val interval by remember { viewModel.scanIntervalSeconds }
    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp)
            .systemBarsPadding()
    ) {
        Text("⏱️ Scan Interval: $interval s")
        Spacer(Modifier.height(16.dp))
        Slider(
            value = interval.toFloat(),
            onValueChange = { viewModel.scanIntervalSeconds.value = it.toInt() },
            valueRange = 1f..30f,
            steps = 29
        )
        Spacer(Modifier.height(24.dp))
        Button(
            onClick = { navController.navigate(Screen.LiveScan.route) },
            modifier = Modifier.fillMaxWidth()
        ) { Text("Start Live Scan") }
    }
}
