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
import androidx.compose.ui.graphics.nativeCanvas
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.IntSize
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavHostController
import com.example.navitest.NavitestViewModel
import com.example.navitest.model.Router
import com.example.navitest.navigation.Screen
import kotlin.math.*

@Composable
fun RouterPlacementScreen(
    navController: NavHostController,
    viewModel: NavitestViewModel = viewModel()
) {
    val context = LocalContext.current
    val density = LocalDensity.current
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
        context.contentResolver.openInputStream(imageUri).use { input ->
            BitmapFactory.decodeStream(input)
        }
    } ?: return

    val imageWidth = bitmap.width.toFloat()
    val imageHeight = bitmap.height.toFloat()
    val stepMeters = 0.25f
    val divisionsX = (floorWidthMeters / stepMeters).toInt()
    val divisionsY = (floorHeightMeters / stepMeters).toInt()
    val gridSizePxX = imageWidth / divisionsX
    val gridSizePxY = imageHeight / divisionsY

    var showDialog by remember { mutableStateOf(false) }
    var newRouterOffset by remember { mutableStateOf<Offset?>(null) }
    var ssidInput by remember { mutableStateOf("") }

    Column(
        Modifier
            .fillMaxSize()
            .padding(12.dp)
            .systemBarsPadding()
    ) {
        Text("Router Setup Screen", style = MaterialTheme.typography.titleMedium)

        Spacer(Modifier.height(8.dp))
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Button(onClick = { navController.popBackStack() }) { Text("Back") }
            Button(onClick = { viewModel.routers.clear() }) { Text("Clear Routers") }
            Button(onClick = { navController.navigate(Screen.RoomNaming.route) }) { Text("Room Setup") }
        }
        Spacer(Modifier.height(8.dp))

        BoxWithConstraints(
            modifier = Modifier.fillMaxSize(),
            contentAlignment = Alignment.Center
        ) {
            val cW = with(density) { maxWidth.toPx() }
            val cH = with(density) { maxHeight.toPx() }
            val scale = min(cW / imageWidth, cH / imageHeight)
            val dstW = imageWidth * scale
            val dstH = imageHeight * scale
            val offX = (cW - dstW) / 2f
            val offY = (cH - dstH) / 2f

            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .pointerInput(Unit) {
                        detectTapGestures { tap ->
                            val imgX = (tap.x - offX) / scale
                            val imgY = (tap.y - offY) / scale
                            if (imgX !in 0f..imageWidth || imgY !in 0f..imageHeight) return@detectTapGestures

                            val snappedX = (imgX / gridSizePxX).roundToInt() * gridSizePxX
                            val snappedY = (imgY / gridSizePxY).roundToInt() * gridSizePxY
                            newRouterOffset = Offset(snappedX, snappedY)
                            ssidInput = ""
                            showDialog = true
                        }
                    }
            ) {
                Canvas(Modifier.fillMaxSize()) {
                    drawImage(
                        image = bitmap.asImageBitmap(),
                        dstOffset = IntOffset(offX.roundToInt(), offY.roundToInt()),
                        dstSize = IntSize(dstW.roundToInt(), dstH.roundToInt())
                    )

                    viewModel.routers.forEach { router ->
                        val pos = Offset(offX + router.x * scale, offY + router.y * scale)
                        drawCircle(Color.Red, 8f, pos)
                        val paint = android.graphics.Paint().apply {
                            textSize = 30f
                            color = android.graphics.Color.BLACK
                        }
                        drawIntoCanvas { canvas ->
                            canvas.nativeCanvas.drawText(
                                router.ssid,
                                pos.x + 12f,
                                pos.y - 12f,
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
                    }) { Text("Save") }
                },
                dismissButton = {
                    TextButton(onClick = { showDialog = false }) { Text("Cancel") }
                }
            )
        }
    }
}
