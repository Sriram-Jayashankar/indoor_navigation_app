package com.example.navitest.userinterface

import androidx.compose.animation.animateColor
import androidx.compose.animation.core.*
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavHostController
import com.example.navitest.NavitestViewModel
import com.example.navitest.navigation.Screen

@Composable
fun HomeScreen(
    navController: NavHostController,
    viewModel: NavitestViewModel = viewModel()
) {
    // Animate two colors
    val transition = rememberInfiniteTransition(label = "backgroundTransition")
    val topColor by transition.animateColor(
        initialValue = MaterialTheme.colorScheme.primary.copy(alpha = 0.8f),
        targetValue = MaterialTheme.colorScheme.secondary.copy(alpha = 0.8f),
        animationSpec = infiniteRepeatable(
            animation = tween(durationMillis = 4000, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "topColor"
    )

    val bottomColor by transition.animateColor(
        initialValue = MaterialTheme.colorScheme.tertiary.copy(alpha = 0.8f),
        targetValue = MaterialTheme.colorScheme.primary.copy(alpha = 0.8f),
        animationSpec = infiniteRepeatable(
            animation = tween(durationMillis = 4000, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "bottomColor"
    )

    // Apply animated background gradient
    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(Brush.verticalGradient(colors = listOf(topColor, bottomColor)))
            .systemBarsPadding()
    ) {
        Surface(
            modifier = Modifier
                .fillMaxSize()
                .padding(24.dp),
            color = Color.Transparent // Let gradient show through
        ) {
            Column(
                verticalArrangement = Arrangement.Center,
                horizontalAlignment = Alignment.CenterHorizontally,
                modifier = Modifier.fillMaxSize()
            ) {
                Text(
                    text = "Welcome to Navi",
                    style = MaterialTheme.typography.headlineSmall,
                    color = Color.White, // Set to white explicitly
                    modifier = Modifier.padding(bottom = 32.dp)
                )

                ActionButton("Setup Floor Map") {
                    navController.navigate(Screen.FloorMap.route)
                }

                Spacer(modifier = Modifier.height(16.dp))

                ActionButton("Settings") {
                    navController.navigate(Screen.Settings.route)
                }

                Spacer(modifier = Modifier.height(16.dp))

                ActionButton("View Saved Floor Plans") {
                    navController.navigate(Screen.SavedPlans.route)
                }
            }
        }
    }
}

@Composable
fun ActionButton(text: String, onClick: () -> Unit) {
    Button(
        onClick = onClick,
        modifier = Modifier
            .fillMaxWidth()
            .height(56.dp),
        shape = MaterialTheme.shapes.medium
    ) {
        Text(text, style = MaterialTheme.typography.bodyLarge)
    }
}
