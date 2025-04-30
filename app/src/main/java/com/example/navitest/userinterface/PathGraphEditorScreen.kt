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
import androidx.compose.ui.graphics.drawscope.withTransform
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.onGloballyPositioned
import androidx.compose.ui.platform.LocalContext
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
    val nodeSpacing = 4 // 1m = 4 steps
    val divisionsX = (floorWidthMeters / stepMeters).toInt()
    val divisionsY = (floorHeightMeters / stepMeters).toInt()
    val gridSizePxX = imageWidth.toFloat() / divisionsX
    val gridSizePxY = imageHeight.toFloat() / divisionsY

    var nextId by remember { mutableStateOf(0) }

    // ✅ Shared state from ViewModel
    val nodes = viewModel.pathNodes
    val edges = viewModel.pathEdges

    var startPoint by remember { mutableStateOf<Offset?>(null) }
    var canvasSize by remember { mutableStateOf(IntSize.Zero) }

    Column(Modifier.fillMaxSize().padding(12.dp).systemBarsPadding()) {
        Row(
            horizontalArrangement = Arrangement.SpaceEvenly,
            modifier = Modifier.fillMaxWidth()
        ) {
            Button(onClick = {
                nodes.clear()
                edges.clear()
                nextId = 0
                startPoint = null
            }) {
                Text("Clear All")
            }

            Button(onClick = {
                navController.navigate("astar_test")
            }) {
                Text("Test A*")
            }

            Button(onClick = {
                navController.navigate("router_placement")
            }) {
                Text("Router Setup")
            }
        }

        Spacer(Modifier.height(8.dp))

        Box(
            modifier = Modifier
                .fillMaxSize()
                .onGloballyPositioned { coordinates -> canvasSize = coordinates.size }
                .pointerInput(true) {
                    detectTapGestures { tap ->
                        val offsetX = (canvasSize.width - imageWidth) / 2f
                        val offsetY = (canvasSize.height - imageHeight) / 2f

                        val localX = tap.x - offsetX
                        val localY = tap.y - offsetY

                        if (localX !in 0f..imageWidth.toFloat() || localY !in 0f..imageHeight.toFloat()) return@detectTapGestures

                        val snappedX = (localX / gridSizePxX).roundToInt() * gridSizePxX
                        val snappedY = (localY / gridSizePxY).roundToInt() * gridSizePxY
                        val snappedPoint = Offset(snappedX, snappedY)

                        if (startPoint == null) {
                            startPoint = snappedPoint
                        } else {
                            val endPoint = snappedPoint
                            generateSegmentWithConnectivity(
                                startPoint!!, endPoint, gridSizePxX, gridSizePxY,
                                nodeSpacing, nodes, edges, nextIdProvider = { nextId++ }
                            )
                            startPoint = null
                        }
                    }
                }
        ) {
            Canvas(Modifier.fillMaxSize()) {
                val offsetX = (size.width - imageWidth.toFloat()) / 2f
                val offsetY = (size.height - imageHeight.toFloat()) / 2f

                withTransform({
                    translate(offsetX, offsetY)
                }) {
                    drawImage(bitmap.asImageBitmap())

                    val gridColor = Color.LightGray.copy(alpha = 0.3f)
                    for (i in 0..divisionsX) {
                        val x = i * gridSizePxX
                        drawLine(gridColor, Offset(x, 0f), Offset(x, imageHeight.toFloat()))
                    }
                    for (j in 0..divisionsY) {
                        val y = j * gridSizePxY
                        drawLine(gridColor, Offset(0f, y), Offset(imageWidth.toFloat(), y))
                    }

                    for (edge in edges) {
                        val from = nodes.find { it.id == edge.fromId }
                        val to = nodes.find { it.id == edge.toId }
                        if (from != null && to != null) {
                            drawLine(Color.Green, Offset(from.x, from.y), Offset(to.x, to.y), strokeWidth = 2f)
                        }
                    }

                    for (node in nodes) {
                        drawCircle(Color.Blue, radius = 6f, center = Offset(node.x, node.y))
                    }

                    startPoint?.let {
                        drawCircle(Color.Red, radius = 8f, center = it)
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

        val previous = prev
        if (previous != null && edges.none { it.fromId == previous.id && it.toId == node.id }) {
            edges.add(Edge(previous.id, node.id))
            edges.add(Edge(node.id, previous.id))
        }

        nodes.filter { it != node }.forEach { other ->
            val d = hypot((other.x - node.x).toDouble(), (other.y - node.y).toDouble())
            if (d <= stepPx + 0.1 && edges.none { it.fromId == node.id && it.toId == other.id }) {
                edges.add(Edge(node.id, other.id))
                edges.add(Edge(other.id, node.id))
            }
        }

        prev = node
    }
}
