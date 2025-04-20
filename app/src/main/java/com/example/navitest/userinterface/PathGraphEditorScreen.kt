//package com.example.navitest.userinterface
//
//import android.graphics.BitmapFactory
//import android.net.Uri
//import android.util.Log
//import androidx.compose.foundation.Canvas
//import androidx.compose.foundation.gestures.detectTapGestures
//import androidx.compose.foundation.layout.*
//import androidx.compose.material3.*
//import androidx.compose.runtime.*
//import androidx.compose.ui.Modifier
//import androidx.compose.ui.geometry.Offset
//import androidx.compose.ui.graphics.Color
//import androidx.compose.ui.graphics.drawscope.drawCircle
//import androidx.compose.ui.graphics.drawscope.drawLine
//import androidx.compose.ui.layout.ContentScale
//import androidx.compose.ui.platform.LocalContext
//import androidx.compose.ui.unit.dp
//import androidx.lifecycle.viewmodel.compose.viewModel
//import androidx.navigation.NavHostController
//import com.example.navitest.NavitestViewModel
//import kotlinx.coroutines.Dispatchers
//import kotlinx.coroutines.withContext
//import org.json.JSONArray
//import org.json.JSONObject
//import java.io.File
//import java.io.FileOutputStream
//import kotlin.math.*
//
//data class Node(val id: Int, val x: Float, val y: Float)
//data class Edge(val fromId: Int, val toId: Int)
//
//@Composable
//fun PathGraphEditorScreen(
//    navController: NavHostController,
//    viewModel: NavitestViewModel = viewModel()
//) {
//    val context = LocalContext.current
//    val imageUri = viewModel.floorMapUri.value
//    val floorWidthMeters = viewModel.floorWidthMeters.value
//    val floorHeightMeters = viewModel.floorHeightMeters.value
//
//    if (imageUri == null) {
//        Text("No floor plan selected.")
//        return
//    }
//
//    val bitmap = remember(imageUri) {
//        val input = context.contentResolver.openInputStream(imageUri)
//        BitmapFactory.decodeStream(input)
//    } ?: return
//
//    val imageWidth = bitmap.width
//    val imageHeight = bitmap.height
//
//    // meters per pixel
//    val metersPerPixelX = floorWidthMeters / imageWidth
//    val metersPerPixelY = floorHeightMeters / imageHeight
//    val STEP_METERS = 0.25f
//
//    // graph state
//    var nextId by remember { mutableStateOf(0) }
//    val nodes = remember { mutableStateListOf<Node>() }
//    val edges = remember { mutableStateListOf<Edge>() }
//    var startPoint by remember { mutableStateOf<Offset?>(null) }
//
//    Column(modifier = Modifier.fillMaxSize().padding(12.dp)) {
//        Row(horizontalArrangement = Arrangement.SpaceEvenly, modifier = Modifier.fillMaxWidth()) {
//            Button(onClick = {
//                nodes.clear()
//                edges.clear()
//                nextId = 0
//            }) {
//                Text("Clear All")
//            }
//
//            Button(onClick = {
//                val json = JSONObject().apply {
//                    put("widthMeters", floorWidthMeters)
//                    put("heightMeters", floorHeightMeters)
//                    put("imageFile", Uri.decode(imageUri.lastPathSegment ?: "map.png"))
//
//                    put("nodes", JSONArray().apply {
//                        nodes.forEach { node ->
//                            put(JSONObject().apply {
//                                put("id", node.id)
//                                put("x", node.x)
//                                put("y", node.y)
//                            })
//                        }
//                    })
//
//                    put("edges", JSONArray().apply {
//                        edges.forEach { edge ->
//                            put(JSONObject().apply {
//                                put("from", edge.fromId)
//                                put("to", edge.toId)
//                            })
//                        }
//                    })
//                }
//
//                val file = File(context.cacheDir, "pathgraph.json")
//                FileOutputStream(file).use {
//                    it.write(json.toString().toByteArray())
//                }
//
//                Log.d("Export", "Saved path graph to ${file.absolutePath}")
//            }) {
//                Text("Export JSON")
//            }
//        }
//
//        Spacer(Modifier.height(8.dp))
//
//        Box(
//            modifier = Modifier
//                .fillMaxSize()
//                .pointerInput(true) {
//                    detectTapGestures { tap ->
//                        if (startPoint == null) {
//                            startPoint = tap
//                        } else {
//                            val endPoint = tap
//                            generateSegment(
//                                startPoint!!,
//                                endPoint,
//                                STEP_METERS,
//                                metersPerPixelX,
//                                metersPerPixelY,
//                                ::nextId
//                            ) { node, prev ->
//                                if (nodes.none { it.x == node.x && it.y == node.y }) {
//                                    nodes.add(node)
//                                }
//                                if (prev != null) {
//                                    edges.add(Edge(prev.id, node.id))
//                                    edges.add(Edge(node.id, prev.id)) // bidirectional
//                                }
//                            }
//                            startPoint = null
//                        }
//                    }
//                }
//        ) {
//            Canvas(modifier = Modifier.fillMaxSize()) {
//                drawImage(bitmap.asImageBitmap())
//
//                // edges
//                for (edge in edges) {
//                    val from = nodes.find { it.id == edge.fromId }
//                    val to = nodes.find { it.id == edge.toId }
//                    if (from != null && to != null) {
//                        drawLine(
//                            color = Color.Green,
//                            start = Offset(from.x, from.y),
//                            end = Offset(to.x, to.y),
//                            strokeWidth = 2f
//                        )
//                    }
//                }
//
//                // nodes
//                for (node in nodes) {
//                    drawCircle(
//                        color = Color.Blue,
//                        radius = 6f,
//                        center = Offset(node.x, node.y)
//                    )
//                }
//
//                // temp preview
//                startPoint?.let {
//                    drawCircle(color = Color.Red, radius = 8f, center = it)
//                }
//            }
//        }
//    }
//}
//
//// Helper: Generate segment with intermediate nodes
//fun generateSegment(
//    start: Offset,
//    end: Offset,
//    stepMeters: Float,
//    metersPerPixelX: Float,
//    metersPerPixelY: Float,
//    nextIdProvider: () -> Int,
//    onNode: (Node, Node?) -> Unit
//) {
//    val dx = end.x - start.x
//    val dy = end.y - start.y
//    val distancePx = sqrt(dx * dx + dy * dy)
//
//    val pxPerMeter = sqrt(1 / (metersPerPixelX * metersPerPixelY)) // approximate
//    val stepPx = stepMeters * pxPerMeter
//
//    val steps = max(1, (distancePx / stepPx).toInt())
//
//    var prev: Node? = null
//
//    for (i in 0..steps) {
//        val t = i / steps.toFloat()
//        val x = start.x + t * dx
//        val y = start.y + t * dy
//        val node = Node(nextIdProvider(), x, y)
//        onNode(node, prev)
//        prev = node
//    }
//}
