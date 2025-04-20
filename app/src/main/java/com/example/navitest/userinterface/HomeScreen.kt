package com.example.navitest.userinterface

import androidx.compose.foundation.layout.*
import androidx.compose.material3.Button
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavHostController
import com.example.navitest.NavitestViewModel
import com.example.navitest.navigation.Screen

@Composable
fun HomeScreen(navController: NavHostController,
               viewModel: NavitestViewModel = viewModel()
) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp)
            .systemBarsPadding() ,
        verticalArrangement   = Arrangement.Center,
        horizontalAlignment  = Alignment.CenterHorizontally
    ) {
        Button(
            onClick = { navController.navigate(Screen.FloorMap.route) },
            modifier = Modifier
                .fillMaxWidth()
                .padding(vertical = 8.dp)
        ) { Text("Setup Floor Map") }

        Button(
            onClick = { navController.navigate(Screen.Settings.route) },
            modifier = Modifier
                .fillMaxWidth()
                .padding(vertical = 8.dp)
        ) { Text("Settings") }
    }
}
