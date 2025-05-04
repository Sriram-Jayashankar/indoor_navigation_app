package com.example.navitest.userinterface

import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll

@Composable
fun ReadmeScreen() {
    val scrollState = rememberScrollState()

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp)
            .verticalScroll(scrollState) // ✅ Makes it scrollable
            .systemBarsPadding()
    ) {
        Text("📖 Indoor Navigation - Usage Guide", style = MaterialTheme.typography.headlineMedium)
        Spacer(Modifier.height(16.dp))

        UsageStep("1. Upload Floor Plan") {
            Text("• Select an image of the floor layout.\n• Enter real-world width and height in meters.")
        }

        UsageStep("2. Grid Overlay (Walkable Zones)") {
            Text("• Tap to mark grid cells as walkable.\n• Zoom and pan supported.\n• Use Clear/Reset to adjust.")
        }

        UsageStep("3. Path Graph Editor") {
            Text("• Tap once to choose START of a path.\n• Tap again to choose END of a path.\n• App auto-connects nodes.\n• Press 'Test A*' to validate paths.")
        }

        UsageStep("4. Router Placement") {
            Text("• Tap anywhere to place a Wi-Fi router.\n• Enter SSID name in the prompt.\n• You need at least 3 routers for location estimation.")
        }

        UsageStep("5. Room Naming") {
            Text("• Tap on the map to place a room.\n• Enter a name (e.g., Lab 1, Hallway).\n• Room name will be shown on preview.")
        }

        UsageStep("6. Export Setup") {
            Text("• Save everything: floor image, dimensions, paths, routers, rooms.\n• Stored as a JSON file for testing.")
        }

        UsageStep("7. Execution Screen") {
            Text("• Load the setup.\n• App scans Wi-Fi signals.\n• Uses Kalman Filter + Trilateration.\n• Displays user location live.")
        }

        Spacer(Modifier.height(24.dp))
        Text("⚠️ Note: Minimum 3 routers are required for accurate localization.", style = MaterialTheme.typography.bodyLarge)
    }
}

@Composable
fun UsageStep(title: String, content: @Composable () -> Unit) {
    Column(modifier = Modifier.padding(vertical = 8.dp)) {
        Text(title, style = MaterialTheme.typography.titleMedium)
        Spacer(Modifier.height(4.dp))
        content()
    }
}
