package com.example.navitest.userinterface

import android.graphics.BitmapFactory
import android.util.Log
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.layout.onGloballyPositioned
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.IntSize
import androidx.compose.ui.unit.dp
import androidx.navigation.NavHostController
import com.example.navitest.NavitestViewModel
import com.example.navitest.model.Edge
import com.example.navitest.model.Node
import com.example.navitest.model.Router
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import org.json.JSONObject
import java.io.File
import kotlin.math.*

@Composable
fun FindLocationScreen(
    navController: NavHostController,
    viewModel: NavitestViewModel
) {
    val context = LocalContext.current
    val files = remember {
        context.filesDir.listFiles { f -> f.name.startsWith("floorplan_") && f.name.endsWith(".json") }?.toList()
            ?: emptyList()
    }

    var selectedFile by remember { mutableStateOf<File?>(null) }
    var imageUri by remember { mutableStateOf(viewModel.floorMapUri.value) }
    var bitmap by remember(imageUri) {
        mutableStateOf(imageUri?.let {
            context.contentResolver.openInputStream(it)?.use { BitmapFactory.decodeStream(it) }
        })
    }

    var routers by remember { mutableStateOf<List<Router>>(emptyList()) }
    var userPosition by remember { mutableStateOf<Offset?>(null) }
    var canvasSize by remember { mutableStateOf(IntSize.Zero) }

    Column(Modifier.fillMaxSize().padding(16.dp)) {
        Text("Find User Location", style = MaterialTheme.typography.headlineSmall)
        Spacer(Modifier.height(16.dp))

        if (selectedFile == null) {
            LazyColumn {
                items(files) { file ->
                    Button(
                        onClick = {
                            val json = JSONObject(file.readText())
                            routers = json.getJSONArray("routers").let { arr ->
                                List(arr.length()) {
                                    val o = arr.getJSONObject(it)
                                    Router(o.getInt("id"), o.getDouble("x").toFloat(), o.getDouble("y").toFloat(), o.getString("ssid"))
                                }
                            }
                            selectedFile = file
                        },
                        modifier = Modifier.fillMaxWidth().padding(vertical = 6.dp)
                    ) {
                        Text("Load ${file.name}")
                    }
                }
            }
        } else {
            // Start scanning and estimating location
            val scope = rememberCoroutineScope()
            LaunchedEffect(routers) {
                while (true) {
                    val rssiMap = routers.associate { it.id to (-50..-70).random().toFloat() }
                    val distances = rssiMap.mapValues { rssiToDistance(it.value) }

                    // Trilateration logic (fallback to centroid)
                    val positions = routers.mapNotNull { r -> distances[r.id]?.let { r to it } }
                    if (positions.size >= 3) {
                        userPosition = estimateCentroid(positions)
                    }

                    delay(2000)
                }
            }

            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .onGloballyPositioned { canvasSize = it.size }
            ) {
                Canvas(modifier = Modifier.fillMaxSize()) {
                    bitmap?.let {
                        val offsetX = (size.width - it.width) / 2f
                        val offsetY = (size.height - it.height) / 2f
                        drawImage(it.asImageBitmap(), topLeft = Offset(offsetX, offsetY))

                        // Draw routers
                        routers.forEach {
                            drawCircle(Color.Red, radius = 8f, center = Offset(it.x + offsetX, it.y + offsetY))
                        }

                        // Draw user
                        userPosition?.let {
                            drawCircle(Color.Blue, radius = 10f, center = Offset(it.x + offsetX, it.y + offsetY))
                        }
                    }
                }
            }
        }
    }
}

// Convert RSSI (negative dBm) to estimated distance (meters)
fun rssiToDistance(rssi: Float, txPower: Int = -59, n: Double = 2.0): Float {
    return 10.0.pow((txPower - rssi) / (10 * n)).toFloat()
}

// Simple centroid estimation
fun estimateCentroid(routerDistances: List<Pair<Router, Float>>): Offset {
    val weightedX = routerDistances.sumOf { (r, d) -> r.x / d }
    val weightedY = routerDistances.sumOf { (r, d) -> r.y / d }
    val weightSum = routerDistances.sumOf { (_, d) -> 1f / d }

    return Offset((weightedX / weightSum).toFloat(), (weightedY / weightSum).toFloat())
}
