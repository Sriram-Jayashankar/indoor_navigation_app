package com.example.navitest.userinterface

import androidx.compose.foundation.layout.*
import androidx.compose.material3.Button
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavHostController
import com.example.navitest.NavitestViewModel
import com.example.navitest.navigation.Screen

@Composable
fun RouterPlacementScreen(
    navController: NavHostController,
    viewModel: NavitestViewModel = viewModel()
) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp)
    ) {
        Text("📍 Router Placement Screen\n\n— drag & drop routers on your map")
        Spacer(modifier = Modifier.height(24.dp))

        // TODO: implement draggable nodes

        Button(
            onClick = { navController.navigate(Screen.ScanConfig.route) },
            modifier = Modifier.fillMaxWidth()
        ) {
            Text("Next: Configure Scan")
        }
    }
}
