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
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.IntSize
import androidx.compose.ui.unit.dp
import androidx.navigation.NavHostController
import com.example.navitest.NavitestViewModel
import com.example.navitest.model.Router
import com.example.navitest.utils.ExecutionUtils
import com.example.navitest.wifi.SmartWifiScanner
import org.json.JSONObject
import java.io.File
import kotlin.math.hypot
import kotlin.math.min
import kotlin.math.roundToInt

@Composable
fun ExecutionScreen(
    file: File,
    navController: NavHostController,
    viewModel: NavitestViewModel
) {
    val context = LocalContext.current

    /* ───── Load JSON + bitmap ───── */
    val jsonString = remember(file) { file.readText() }
    val jsonObject = remember(jsonString) { JSONObject(jsonString) }

    val bitmap = remember(jsonObject) {
        val bytes = Base64.decode(jsonObject.getString("imageBase64"), Base64.DEFAULT)
        BitmapFactory.decodeByteArray(bytes, 0, bytes.size).asImageBitmap()
    }

    val routers = remember(jsonObject) {
        val arr = jsonObject.getJSONArray("routers")
        buildList {
            for (i in 0 until arr.length()) {
                val o = arr.getJSONObject(i)
                add(
                    Router(
                        id = o.getInt("id"),
                        x = o.getDouble("x").toFloat(),
                        y = o.getDouble("y").toFloat(),
                        ssid = o.getString("ssid")
                    )
                )
            }
        }
    }

    val nodes = remember(jsonObject) {
        val arr = jsonObject.getJSONArray("nodes")
        buildList {
            for (i in 0 until arr.length()) {
                val o = arr.getJSONObject(i)
                add(
                    Offset(
                        x = o.getDouble("x").toFloat(),
                        y = o.getDouble("y").toFloat()
                    )
                )
            }
        }
    }

    var snappedPos by remember { mutableStateOf<Offset?>(null) }

    /* ───── Wi-Fi scanner → Kalman → Centroid trilateration ───── */
    DisposableEffect(Unit) {
        val scanner = SmartWifiScanner(context, routers.map { it.ssid }) { raw ->
            if (raw.size < 3) {
                Toast.makeText(context, "Need at least 3 routers", Toast.LENGTH_SHORT).show()
                return@SmartWifiScanner
            }
            val rssiMap = ExecutionUtils.applyKalman1D(
                ExecutionUtils.mapToRouterRssi(routers, raw)
            )
            val pos = ExecutionUtils.trilaterateCentroidWeighted(routers, rssiMap)
            if (pos != Offset.Zero) {
                // Snap to nearest node
                val nearest = nodes.minByOrNull { node ->
                    hypot(node.x - pos.x, node.y - pos.y)
                }
                if (nearest != null) snappedPos = nearest
            }
        }
        scanner.start()
        onDispose { scanner.stop() }
    }

    /* ───── UI ───── */
    Column(
        Modifier
            .fillMaxSize()
            .padding(16.dp)
            .systemBarsPadding()
    ) {

        BoxWithConstraints(
            modifier = Modifier
                .weight(1f)
                .fillMaxWidth(),
            contentAlignment = Alignment.Center
        ) {
            val canvasW = constraints.maxWidth.toFloat()
            val canvasH = constraints.maxHeight.toFloat()
            val imgW    = bitmap.width.toFloat()
            val imgH    = bitmap.height.toFloat()

            val scale = min(canvasW / imgW, canvasH / imgH)
            val dstW  = (imgW * scale).roundToInt()
            val dstH  = (imgH * scale).roundToInt()
            val offX  = ((canvasW - dstW) / 2f).roundToInt()
            val offY  = ((canvasH - dstH) / 2f).roundToInt()

            val screenNodes = remember(canvasW, canvasH, nodes) {
                nodes.map { n ->
                    Offset(
                        x = offX + n.x * scale,
                        y = offY + n.y * scale
                    )
                }
            }

            Canvas(Modifier.fillMaxSize()) {
                drawImage(
                    image = bitmap,
                    dstOffset = IntOffset(offX, offY),
                    dstSize = IntSize(dstW, dstH)
                )

                screenNodes.forEach { p ->
                    drawCircle(Color.Green, radius = 6f, center = p)
                }

                snappedPos?.let { p ->
                    val sx = offX + p.x * scale
                    val sy = offY + p.y * scale
                    drawCircle(Color.Magenta, radius = 12f, center = Offset(sx, sy))
                }
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
