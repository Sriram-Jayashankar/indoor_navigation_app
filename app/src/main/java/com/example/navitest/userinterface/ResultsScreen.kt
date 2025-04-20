package com.example.navitest.userinterface

import androidx.compose.foundation.layout.*
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavHostController
import com.example.navitest.NavitestViewModel

@Composable
fun ResultsScreen(
    navController: NavHostController,
    viewModel: NavitestViewModel = viewModel()
) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp)
            .systemBarsPadding()
    ) {
        Text("🏁 Results\n\n— final position & analytics here")
        Spacer(Modifier.height(16.dp))
        // TODO: show map overlay + stats
    }
}
