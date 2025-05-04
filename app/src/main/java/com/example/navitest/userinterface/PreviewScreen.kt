package com.example.navitest.userinterface

import android.graphics.BitmapFactory
import android.util.Base64
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.graphics.drawscope.drawIntoCanvas
import androidx.compose.ui.graphics.drawscope.withTransform
import androidx.compose.ui.graphics.nativeCanvas
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.IntSize
import androidx.compose.ui.unit.dp
import androidx.navigation.NavHostController
import com.example.navitest.model.Edge
import com.example.navitest.model.Node
import com.example.navitest.model.Room
import com.example.navitest.model.Router
import org.json.JSONObject
import java.io.File
import kotlin.math.min

@Composable
fun PreviewScreen(
    file: File,
    navController: NavHostController
) {
    val context = LocalContext.current
    val density = LocalDensity.current

    val json = remember(file) { JSONObject(file.readText()) }
    val widthMeters = json.getDouble("widthMeters").toFloat()
    val heightMeters = json.getDouble("heightMeters").toFloat()

    val imageBase64 = json.getString("imageBase64")
    val imageBytes = Base64.decode(imageBase64, Base64.DEFAULT)
    val bitmap = BitmapFactory.decodeByteArray(imageBytes, 0, imageBytes.size)

    val imageWidth = bitmap.width.toFloat()
    val imageHeight = bitmap.height.toFloat()

    val nodes = remember {
        val array = json.getJSONArray("nodes")
        List(array.length()) {
            val obj = array.getJSONObject(it)
            Node(obj.getInt("id"), obj.getDouble("x").toFloat(), obj.getDouble("y").toFloat())
        }
    }

    val edges = remember {
        val array = json.getJSONArray("edges")
        List(array.length()) {
            val obj = array.getJSONObject(it)
            Edge(obj.getInt("from"), obj.getInt("to"))
        }
    }

    val routers = remember {
        val array = json.getJSONArray("routers")
        List(array.length()) {
            val obj = array.getJSONObject(it)
            Router(obj.getInt("id"), obj.getDouble("x").toFloat(), obj.getDouble("y").toFloat(), obj.getString("ssid"))
        }
    }

    val rooms = remember {
        val array = json.getJSONArray("rooms")
        List(array.length()) {
            val obj = array.getJSONObject(it)
            Room(obj.getInt("id"), obj.getDouble("x").toFloat(), obj.getDouble("y").toFloat(), obj.getString("name"))
        }
    }

    var showRouters by remember { mutableStateOf(true) }
    var showRooms by remember { mutableStateOf(true) }
    var showPaths by remember { mutableStateOf(true) }

    Column(Modifier.fillMaxSize().padding(12.dp).systemBarsPadding()) {
        Text("Preview Screen", style = MaterialTheme.typography.titleMedium)

        Spacer(Modifier.height(8.dp))
        BoxWithConstraints(
            modifier = Modifier.weight(1f).fillMaxWidth(),
            contentAlignment = Alignment.Center
        ) {
            val cW = with(density) { maxWidth.toPx() }
            val cH = with(density) { maxHeight.toPx() }
            val scale = min(cW / imageWidth, cH / imageHeight)
            val dstW = (imageWidth * scale).toInt()
            val dstH = (imageHeight * scale).toInt()
            val offX = ((cW - dstW) / 2f).toInt()
            val offY = ((cH - dstH) / 2f).toInt()

            Canvas(Modifier.fillMaxSize()) {
                drawImage(
                    image = bitmap.asImageBitmap(),
                    dstOffset = IntOffset(offX, offY),
                    dstSize = IntSize(dstW, dstH)
                )

                if (showPaths) {
                    edges.forEach { e ->
                        val from = nodes.find { it.id == e.fromId }
                        val to = nodes.find { it.id == e.toId }
                        if (from != null && to != null) {
                            drawLine(
                                Color.Blue,
                                Offset(offX + from.x * scale, offY + from.y * scale),
                                Offset(offX + to.x * scale, offY + to.y * scale),
                                strokeWidth = 2f
                            )
                        }
                    }
                }

                if (showRouters) {
                    routers.forEach { router ->
                        val pos = Offset(offX + router.x * scale, offY + router.y * scale)
                        drawCircle(Color.Red, 8f, pos)
                        val paint = android.graphics.Paint().apply {
                            textSize = 28f
                            color = android.graphics.Color.BLACK
                        }
                        drawIntoCanvas { canvas ->
                            canvas.nativeCanvas.drawText(router.ssid, pos.x + 10f, pos.y - 10f, paint)
                        }
                    }
                }

                if (showRooms) {
                    rooms.forEach { room ->
                        val pos = Offset(offX + room.x * scale, offY + room.y * scale)
                        drawCircle(Color.Green, 8f, pos)
                        val paint = android.graphics.Paint().apply {
                            textSize = 28f
                            color = android.graphics.Color.BLACK
                        }
                        drawIntoCanvas { canvas ->
                            canvas.nativeCanvas.drawText(room.name, pos.x + 10f, pos.y + 10f, paint)
                        }
                    }
                }
            }
        }

        Spacer(Modifier.height(12.dp))

        Row(
            Modifier
                .fillMaxWidth()
                .padding(bottom = 12.dp),
            horizontalArrangement = Arrangement.SpaceEvenly
        ) {
            FilterToggle("Routers", showRouters) { showRouters = it }
            FilterToggle("Rooms", showRooms) { showRooms = it }
            FilterToggle("Paths", showPaths) { showPaths = it }
        }

        Button(onClick = { navController.popBackStack() }, modifier = Modifier.align(Alignment.CenterHorizontally)) {
            Text("Back")
        }
    }
}

@Composable
fun FilterToggle(label: String, selected: Boolean, onToggle: (Boolean) -> Unit) {
    Button(
        onClick = { onToggle(!selected) },
        colors = ButtonDefaults.buttonColors(
            containerColor = if (selected) MaterialTheme.colorScheme.primary else Color.LightGray
        )
    ) {
        Text(label)
    }
}
