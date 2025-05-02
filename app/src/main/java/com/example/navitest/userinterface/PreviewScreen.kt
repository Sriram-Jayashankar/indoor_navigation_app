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
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.graphics.nativeCanvas
import androidx.compose.ui.unit.dp
import androidx.navigation.NavHostController
import com.example.navitest.model.Edge
import com.example.navitest.model.Node
import com.example.navitest.model.Router
import com.example.navitest.model.Room
import org.json.JSONObject
import java.io.File

@Composable
fun PreviewScreen(
    file: File,
    navController: NavHostController
) {
    val context = LocalContext.current

    val json = remember(file) { JSONObject(file.readText()) }

    val widthMeters = json.getDouble("widthMeters").toFloat()
    val heightMeters = json.getDouble("heightMeters").toFloat()

    val imageBase64 = json.getString("imageBase64")
    val imageBytes = Base64.decode(imageBase64, Base64.DEFAULT)
    val bitmap = BitmapFactory.decodeByteArray(imageBytes, 0, imageBytes.size)

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
        Box(
            modifier = Modifier
                .weight(1f)
                .fillMaxWidth(),
            contentAlignment = Alignment.Center
        ) {
            Canvas(Modifier.fillMaxSize()) {
                val canvasWidth = size.width
                val canvasHeight = size.height

                val offsetX = (canvasWidth - bitmap.width) / 2f
                val offsetY = (canvasHeight - bitmap.height) / 2f

                withTransform({
                    translate(offsetX, offsetY)
                }) {
                    drawImage(bitmap.asImageBitmap())

                    if (showPaths) {
                        edges.forEach { e ->
                            val from = nodes.find { it.id == e.fromId }
                            val to = nodes.find { it.id == e.toId }
                            if (from != null && to != null) {
                                drawLine(Color.Blue, Offset(from.x, from.y), Offset(to.x, to.y), strokeWidth = 2f)
                            }
                        }
                    }

                    if (showRouters) {
                        routers.forEach { router ->
                            drawCircle(Color.Red, 8f, Offset(router.x, router.y))
                            val paint = android.graphics.Paint().apply {
                                textSize = 28f
                                color = android.graphics.Color.BLACK
                            }
                            drawIntoCanvas { canvas ->
                                canvas.nativeCanvas.drawText(router.ssid, router.x + 10f, router.y - 10f, paint)
                            }
                        }
                    }


                    if (showRooms) {
                        rooms.forEach { room ->
                            drawCircle(Color.Green, 8f, Offset(room.x, room.y))
                            val paint = android.graphics.Paint().apply {
                                textSize = 28f
                                color = android.graphics.Color.BLACK
                            }
                            drawIntoCanvas { canvas ->
                                canvas.nativeCanvas.drawText(room.name, room.x + 10f, room.y + 10f, paint)
                            }
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
