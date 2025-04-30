package com.example.navitest.userinterface

import android.content.Context
import android.graphics.BitmapFactory
import android.util.Log
import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavHostController
import com.example.navitest.NavitestViewModel
import com.example.navitest.wifi.WifiScanner
import java.io.File

@Composable
fun UserLocationScreen(
    navController: NavHostController,
    viewModel: NavitestViewModel = viewModel()
) {
    val context = LocalContext.current
    val ssids = remember { viewModel.routers.map { it.ssid } }

    var rssiMap by remember { mutableStateOf<Map<String, Int>>(emptyMap()) }

    // ⚡ Set up and start WifiScanner
    DisposableEffect(Unit) {
        val scanner = WifiScanner(context, ssids) { newMap ->
            rssiMap = newMap
            Log.d("LiveScan", "📶 RSSI: $newMap")
            // TODO: Use this for location calculation
        }
        scanner.start()

        onDispose {
            scanner.stop()
        }
    }

    val imagePath = remember {
        File(
            context.filesDir,
            context.getSharedPreferences("navitest_prefs", Context.MODE_PRIVATE)
                .getString("imagePath", "") ?: ""
        )
    }

    val bitmap = remember(imagePath) {
        if (imagePath.exists()) {
            BitmapFactory.decodeFile(imagePath.absolutePath)
        } else null
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .systemBarsPadding(),
        contentAlignment = Alignment.Center
    ) {
        bitmap?.let {
            Image(
                bitmap = it.asImageBitmap(),
                contentDescription = "Floor Plan",
                modifier = Modifier.fillMaxSize(),
                contentScale = ContentScale.Fit
            )
        } ?: Text("❌ Floor plan image not found.", color = MaterialTheme.colorScheme.error)
    }
}
