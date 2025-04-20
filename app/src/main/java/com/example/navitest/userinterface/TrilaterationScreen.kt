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
fun TrilaterationScreen(
    navController: NavHostController,
    viewModel: NavitestViewModel = viewModel()
) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp)
    ) {
        Text("📐 Trilateration\n\n— calculate & preview your location")
        Spacer(Modifier.height(24.dp))

        // TODO: implement centroid/A* logic & overlay

        Button(
            onClick = { navController.navigate(Screen.Results.route) },
            modifier = Modifier.fillMaxWidth()
        ) {
            Text("View Results")
        }
    }
}
