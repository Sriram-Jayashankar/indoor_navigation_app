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
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.IntSize
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavHostController
import com.example.navitest.NavitestViewModel
import com.example.navitest.model.Edge
import com.example.navitest.model.Node
import kotlin.math.*

@Composable
fun PathGraphEditorScreen(
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
    val gridCountX = (floorWidthMeters / stepMeters).toInt()
    val gridCountY = (floorHeightMeters / stepMeters).toInt()
    val gridSizePxX = imageWidth / gridCountX
    val gridSizePxY = imageHeight / gridCountY

    var nextId by remember { mutableIntStateOf(0) }
    val nodes = viewModel.pathNodes
    val edges = viewModel.pathEdges
    var startPoint by remember { mutableStateOf<Offset?>(null) }

    Column(
        Modifier
            .fillMaxSize()
            .padding(12.dp)
            .systemBarsPadding()
    ) {
        Row(
            horizontalArrangement = Arrangement.SpaceEvenly,
            modifier = Modifier.fillMaxWidth()
        ) {
            Button(onClick = {
                nodes.clear(); edges.clear(); nextId = 0; startPoint = null
            }) { Text("Clear All") }
            Button(onClick = { navController.navigate("astar_test") }) { Text("Test A*") }
            Button(onClick = { navController.navigate("router_placement") }) { Text("Router Setup") }
        }
        Spacer(Modifier.height(8.dp))

        BoxWithConstraints(
            modifier = Modifier.fillMaxSize()
        ) {
            val cW = with(density) { maxWidth.toPx() }
            val cH = with(density) { maxHeight.toPx() }
            val scale = min(cW / imageWidth, cH / imageHeight)
            val dstW = (imageWidth * scale).roundToInt()
            val dstH = (imageHeight * scale).roundToInt()
            val offX = ((cW - dstW) / 2f).roundToInt()
            val offY = ((cH - dstH) / 2f).roundToInt()

            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .pointerInput(Unit) {
                        detectTapGestures { tap ->
                            val localX = (tap.x - offX) / scale
                            val localY = (tap.y - offY) / scale

                            if (localX !in 0f..imageWidth || localY !in 0f..imageHeight) return@detectTapGestures

                            val snappedX = (localX / gridSizePxX).roundToInt() * gridSizePxX
                            val snappedY = (localY / gridSizePxY).roundToInt() * gridSizePxY
                            val point = Offset(snappedX, snappedY)

                            if (startPoint == null) {
                                startPoint = point
                            } else {
                                generateSegmentWithConnectivity(
                                    startPoint!!, point,
                                    gridSizePxX, gridSizePxY,
                                    4, nodes, edges,
                                    nextIdProvider = { nextId++ }
                                )
                                startPoint = null
                            }
                        }
                    }
            ) {
                Canvas(Modifier.fillMaxSize()) {
                    drawImage(
                        image = bitmap.asImageBitmap(),
                        dstOffset = IntOffset(offX, offY),
                        dstSize = IntSize(dstW, dstH)
                    )

                    val gridColor = Color.LightGray.copy(alpha = 0.3f)
                    for (i in 0..gridCountX) {
                        val x = offX + i * gridSizePxX * scale
                        drawLine(gridColor, Offset(x, offY.toFloat()), Offset(x, offY + dstH.toFloat()))
                    }
                    for (j in 0..gridCountY) {
                        val y = offY + j * gridSizePxY * scale
                        drawLine(gridColor, Offset(offX.toFloat(), y), Offset(offX + dstW.toFloat(), y))
                    }

                    edges.forEach { edge ->
                        val from = nodes.find { it.id == edge.fromId } ?: return@forEach
                        val to = nodes.find { it.id == edge.toId } ?: return@forEach
                        val p1 = Offset(offX + from.x * scale, offY + from.y * scale)
                        val p2 = Offset(offX + to.x * scale, offY + to.y * scale)
                        drawLine(Color.Green, p1, p2, strokeWidth = 2f)
                    }

                    nodes.forEach { node ->
                        val center = Offset(offX + node.x * scale, offY + node.y * scale)
                        drawCircle(Color.Blue, 6f, center)
                    }

                    startPoint?.let {
                        val center = Offset(offX + it.x * scale, offY + it.y * scale)
                        drawCircle(Color.Red, 8f, center)
                    }
                }
            }
        }
    }
}


fun generateSegmentWithConnectivity(
    start: Offset,
    end: Offset,
    gridSizeX: Float,
    gridSizeY: Float,
    spacing: Int,
    nodes: MutableList<Node>,
    edges: MutableList<Edge>,
    nextIdProvider: () -> Int
) {
    val dx = end.x - start.x
    val dy = end.y - start.y
    val distance = sqrt(dx * dx + dy * dy)
    val stepPx = spacing * max(gridSizeX, gridSizeY)
    val steps = max(1, (distance / stepPx).toInt())

    var prev: Node? = null
    for (i in 0..steps) {
        val t = i / steps.toFloat()
        val x = start.x + t * dx
        val y = start.y + t * dy
        val snappedX = (x / gridSizeX).roundToInt() * gridSizeX
        val snappedY = (y / gridSizeY).roundToInt() * gridSizeY
        val node = nodes.find { it.x == snappedX && it.y == snappedY }
            ?: Node(nextIdProvider(), snappedX, snappedY).also { nodes.add(it) }

        prev?.let { previous ->
            if (edges.none { it.fromId == previous.id && it.toId == node.id }) {
                edges.add(Edge(previous.id, node.id))
                edges.add(Edge(node.id, previous.id))
            }
        }

        nodes.filter { it != node }.forEach { other ->
            val d = hypot((other.x - node.x), (other.y - node.y))
            if (d <= stepPx + 0.1 && edges.none { it.fromId == node.id && it.toId == other.id }) {
                edges.add(Edge(node.id, other.id))
                edges.add(Edge(other.id, node.id))
            }
        }

        prev = node
    }
}
