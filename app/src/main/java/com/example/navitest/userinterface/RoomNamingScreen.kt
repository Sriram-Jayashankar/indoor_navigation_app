package com.example.navitest.userinterface

import android.graphics.BitmapFactory
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.gestures.detectTapGestures
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
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.onGloballyPositioned
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.graphics.nativeCanvas
import androidx.compose.ui.unit.IntSize
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavHostController
import com.example.navitest.NavitestViewModel
import com.example.navitest.exportFullMapData
import com.example.navitest.model.Room
import com.example.navitest.navigation.Screen
import kotlin.math.roundToInt
import android.util.Log

@Composable
fun RoomNamingScreen(
    navController: NavHostController,
    viewModel: NavitestViewModel = viewModel()
) {
    val context = LocalContext.current
    val imageUri = viewModel.floorMapUri.value
    val floorWidthMeters = viewModel.floorWidthMeters.value
    val floorHeightMeters = viewModel.floorHeightMeters.value

    if (imageUri == null) {
        Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
            Text("No floor plan selected.")
        }
        return
    }

    val bitmap = remember(imageUri) {
        val input = context.contentResolver.openInputStream(imageUri)
        BitmapFactory.decodeStream(input)
    } ?: return

    val imageWidth = bitmap.width
    val imageHeight = bitmap.height

    val stepMeters = 0.25f
    val divisionsX = (floorWidthMeters / stepMeters).toInt()
    val divisionsY = (floorHeightMeters / stepMeters).toInt()
    val gridSizePxX = imageWidth.toFloat() / divisionsX
    val gridSizePxY = imageHeight.toFloat() / divisionsY

    var canvasSize by remember { mutableStateOf(IntSize.Zero) }
    var showDialog by remember { mutableStateOf(false) }
    var newRoomOffset by remember { mutableStateOf<Offset?>(null) }
    var roomNameInput by remember { mutableStateOf("") }

    Column(Modifier.fillMaxSize().padding(12.dp)) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Button(onClick = { navController.popBackStack() }) {
                Text("Back")
            }

            Button(onClick = {
                val (jsonFile, imageFile) = exportFullMapData(
                    context = context,
                    widthMeters = viewModel.floorWidthMeters.value,
                    heightMeters = viewModel.floorHeightMeters.value,
                    nodes = viewModel.pathNodes,
                    edges = viewModel.pathEdges,
                    routers = viewModel.routers,
                    rooms = viewModel.rooms,
                    bitmap = bitmap
                )
                Log.d("Export", "Saved JSON: ${jsonFile.absolutePath}")
                Log.d("Export", "Saved Image: ${imageFile.absolutePath}")
                navController.navigate(Screen.Home.route)
            }) {
                Text("Export Setup 📦")
            }
        }

        Spacer(Modifier.height(8.dp))

        Box(
            modifier = Modifier
                .fillMaxSize()
                .onGloballyPositioned { canvasSize = it.size }
                .pointerInput(true) {
                    detectTapGestures { tap ->
                        val offsetX = (canvasSize.width - imageWidth) / 2f
                        val offsetY = (canvasSize.height - imageHeight) / 2f

                        val localX = tap.x - offsetX
                        val localY = tap.y - offsetY

                        if (localX !in 0f..imageWidth.toFloat() || localY !in 0f..imageHeight.toFloat()) return@detectTapGestures

                        val snappedX = (localX / gridSizePxX).roundToInt() * gridSizePxX
                        val snappedY = (localY / gridSizePxY).roundToInt() * gridSizePxY
                        newRoomOffset = Offset(snappedX, snappedY)
                        roomNameInput = ""
                        showDialog = true
                    }
                }
        ) {
            Canvas(modifier = Modifier.fillMaxSize()) {
                val offsetX = (size.width - imageWidth.toFloat()) / 2f
                val offsetY = (size.height - imageHeight.toFloat()) / 2f

                withTransform({
                    translate(offsetX, offsetY)
                }) {
                    drawImage(bitmap.asImageBitmap())

                    viewModel.rooms.forEach { room ->
                        drawCircle(Color.Magenta, radius = 8f, center = Offset(room.x, room.y))

                        val paint = android.graphics.Paint().apply {
                            textSize = 30f
                            color = android.graphics.Color.BLACK
                        }

                        drawIntoCanvas { canvas ->
                            canvas.nativeCanvas.drawText(
                                room.name,
                                room.x + 12f,
                                room.y - 12f,
                                paint
                            )
                        }
                    }
                }
            }
        }

        if (showDialog && newRoomOffset != null) {
            AlertDialog(
                onDismissRequest = { showDialog = false },
                title = { Text("Enter Room Name") },
                text = {
                    OutlinedTextField(
                        value = roomNameInput,
                        onValueChange = { roomNameInput = it },
                        label = { Text("Room Name") }
                    )
                },
                confirmButton = {
                    Button(onClick = {
                        val newId = viewModel.rooms.size + 1
                        viewModel.rooms.add(
                            Room(newId, newRoomOffset!!.x, newRoomOffset!!.y, roomNameInput)
                        )
                        showDialog = false
                        newRoomOffset = null
                        roomNameInput = ""
                    }) {
                        Text("Save")
                    }
                },
                dismissButton = {
                    TextButton(onClick = {
                        showDialog = false
                        newRoomOffset = null
                    }) {
                        Text("Cancel")
                    }
                }
            )
        }
    }
}
