package com.example.navitest.userinterface

import android.graphics.BitmapFactory
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.ui.graphics.drawscope.drawIntoCanvas
import androidx.compose.ui.graphics.nativeCanvas
import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.graphics.drawscope.withTransform
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.onGloballyPositioned
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.IntSize
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavHostController
import com.example.navitest.NavitestViewModel
import com.example.navitest.model.Router
import kotlin.math.roundToInt
import android.util.Log
import com.example.navitest.exportFullMapData
import com.example.navitest.navigation.Screen

@Composable
fun RouterPlacementScreen(
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

    val bitmapimage = remember(imageUri) {
        val input = context.contentResolver.openInputStream(imageUri)
        BitmapFactory.decodeStream(input)
    } ?: return

    val imageWidth = bitmapimage.width
    val imageHeight = bitmapimage.height

    val stepMeters = 0.25f
    val divisionsX = (floorWidthMeters / stepMeters).toInt()
    val divisionsY = (floorHeightMeters / stepMeters).toInt()
    val gridSizePxX = imageWidth.toFloat() / divisionsX
    val gridSizePxY = imageHeight.toFloat() / divisionsY

    var canvasSize by remember { mutableStateOf(IntSize.Zero) }
    var showDialog by remember { mutableStateOf(false) }
    var newRouterOffset by remember { mutableStateOf<Offset?>(null) }
    var ssidInput by remember { mutableStateOf("") }

    Column(Modifier.fillMaxSize().padding(12.dp)) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Button(onClick = { navController.popBackStack() }) {
                Text("Back")
            }
            Button(onClick = {
                viewModel.routers.clear()
            }) {
                Text("Clear Routers")
            }
            Button(onClick = {
                val (jsonFile, imageFile) = exportFullMapData(
                    context = context,
                    widthMeters = viewModel.floorWidthMeters.value,
                    heightMeters = viewModel.floorHeightMeters.value,
                    nodes = viewModel.pathNodes,
                    edges = viewModel.pathEdges,
                    routers = viewModel.routers,
                    bitmap = bitmapimage // ⬅️ Pass bitmap loaded earlier
                )
                Log.d("Export", "Saved JSON: ${jsonFile.absolutePath}")
                Log.d("Export", "Saved Image: ${imageFile.absolutePath}")
                navController.navigate(Screen.Home.route)
            }) {
                Text("Export Setup")
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
                        newRouterOffset = Offset(snappedX, snappedY)
                        ssidInput = ""
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
                    drawImage(bitmapimage.asImageBitmap())

                    // ✅ Draw routers with SSID text
                    for (router in viewModel.routers) {
                        drawCircle(Color.Red, radius = 8f, center = Offset(router.x, router.y))

                        val paint = android.graphics.Paint().apply {
                            textSize = 30f
                            color = android.graphics.Color.BLACK
                        }

                        drawIntoCanvas { canvas ->
                            canvas.nativeCanvas.drawText(
                                router.ssid,
                                router.x + 12f,
                                router.y - 12f,
                                paint
                            )
                        }
                    }

                }
            }

        }

        if (showDialog && newRouterOffset != null) {
            AlertDialog(
                onDismissRequest = { showDialog = false },
                title = { Text("Enter SSID") },
                text = {
                    OutlinedTextField(
                        value = ssidInput,
                        onValueChange = { ssidInput = it },
                        label = { Text("SSID") }
                    )
                },
                confirmButton = {
                    Button(onClick = {
                        val newId = viewModel.routers.size + 1
                        viewModel.routers.add(
                            Router(newId, newRouterOffset!!.x, newRouterOffset!!.y, ssidInput)
                        )
                        showDialog = false
                        newRouterOffset = null
                        ssidInput = ""
                    }) {
                        Text("Save")
                    }
                },
                dismissButton = {
                    TextButton(onClick = {
                        showDialog = false
                        newRouterOffset = null
                    }) {
                        Text("Cancel")
                    }
                }
            )
        }
    }
}
