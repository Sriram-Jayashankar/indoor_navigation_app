package com.example.navitest.userinterface

import android.graphics.BitmapFactory
import android.util.Base64
import android.widget.Toast
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.navigation.NavHostController
import com.example.navitest.NavitestViewModel
import com.example.navitest.model.Router
import com.example.navitest.utils.ExecutionUtils
import com.example.navitest.wifi.SmartWifiScanner
import org.json.JSONObject
import java.io.File

@Composable
fun ExecutionScreen(
    file: File,
    navController: NavHostController,
    viewModel: NavitestViewModel
) {
    val context = LocalContext.current

    // Parse JSON
    val jsonString = remember { file.readText() }
    val jsonObject = remember { JSONObject(jsonString) }
    val bitmap = remember {
        val bytes = Base64.decode(jsonObject.getString("imageBase64"), Base64.DEFAULT)
        BitmapFactory.decodeByteArray(bytes, 0, bytes.size).asImageBitmap()
    }
    val routers = remember {
        val arr = jsonObject.getJSONArray("routers")
        buildList {
            for (i in 0 until arr.length()) {
                val o = arr.getJSONObject(i)
                add(
                    Router(
                        o.getInt("id"),
                        o.getDouble("x").toFloat(),
                        o.getDouble("y").toFloat(),
                        o.getString("ssid")
                    )
                )
            }
        }
    }

    var userPos by remember { mutableStateOf<Offset?>(null) }

    // Start scanning with Kalman and centroid triangulation
    DisposableEffect(Unit) {
        val ssids = routers.map { it.ssid }
        val scanner = SmartWifiScanner(context, ssids) { rawRssi ->
            if (rawRssi.size < 3) {
                Toast.makeText(context, "Need at least 3 routers for trilateration", Toast.LENGTH_SHORT).show()
                return@SmartWifiScanner
            }
            val rssiMap = ExecutionUtils.applyKalman1D(ExecutionUtils.mapToRouterRssi(routers, rawRssi))
            val pos = ExecutionUtils.trilaterateCentroidWeighted(routers, rssiMap)
            if (pos != Offset.Zero) userPos = pos
        }
        scanner.start()
        onDispose { scanner.stop() }
    }

    // UI
    Column(
        Modifier.fillMaxSize().padding(16.dp).systemBarsPadding()
    ) {
        Box(
            Modifier.weight(1f).fillMaxWidth(),
            contentAlignment = Alignment.Center
        ) {
            Canvas(Modifier.fillMaxSize()) {
                drawImage(bitmap)
                userPos?.let { drawCircle(Color.Magenta, 12f, center = it) }
            }
        }

        Spacer(Modifier.height(16.dp))
        Button(
            onClick = { navController.popBackStack() },
            modifier = Modifier.align(Alignment.CenterHorizontally)
        ) {
            Text("Back")
        }
    }
}
