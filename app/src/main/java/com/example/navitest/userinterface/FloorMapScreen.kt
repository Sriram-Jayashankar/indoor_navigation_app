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
fun FloorMapScreen(
    navController: NavHostController,
    viewModel: NavitestViewModel = viewModel()
) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp)
    ) {
        Text("📸 Floor Map Screen\n\n— upload & preview your floor plan here")
        Spacer(modifier = Modifier.height(24.dp))

        // TODO: implement image picker & display

        Button(
            onClick = { navController.navigate(Screen.RouterPlacement.route) },
            modifier = Modifier.fillMaxWidth()
        ) {
            Text("Next: Place Routers")
        }
    }
}
