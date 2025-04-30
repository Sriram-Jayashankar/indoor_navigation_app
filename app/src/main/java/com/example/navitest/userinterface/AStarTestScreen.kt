package com.example.navitest.userinterface

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.withTransform
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.onGloballyPositioned
import androidx.compose.ui.unit.IntSize
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavHostController
import com.example.navitest.NavitestViewModel
import com.example.navitest.model.Node
import com.example.navitest.model.Edge
import kotlin.math.hypot

@Composable
fun AStarTestScreen(
    navController: NavHostController,
    viewModel: NavitestViewModel = viewModel()
) {
    val nodes = viewModel.pathNodes
    val edges = viewModel.pathEdges

    var canvasSize by remember { mutableStateOf(IntSize.Zero) }
    var startNode by remember { mutableStateOf<Node?>(null) }
    var endNode by remember { mutableStateOf<Node?>(null) }
    var path by remember { mutableStateOf<List<Node>>(emptyList()) }

    Column(Modifier.fillMaxSize()) {
        Row(
            horizontalArrangement = Arrangement.SpaceBetween,
            modifier = Modifier
                .fillMaxWidth()
                .padding(8.dp)
                .systemBarsPadding()
        ) {
            Button(onClick = { navController.popBackStack() }) {
                Text("Back")
            }

            Button(onClick = {
                startNode = null
                endNode = null
                path = emptyList()
            }) {
                Text("Reset")
            }
        }

        Box(
            modifier = Modifier
                .fillMaxSize()
                .onGloballyPositioned { canvasSize = it.size }
                .pointerInput(true) {
                    detectTapGestures(
                        onLongPress = { tap ->
                            val clicked = nodes.minByOrNull { node ->
                                hypot((node.x - tap.x).toDouble(), (node.y - tap.y).toDouble())
                            }
                            if (startNode == null) {
                                startNode = clicked
                            } else if (endNode == null) {
                                endNode = clicked
                                val result = runAStar(startNode!!, endNode!!, nodes, edges)
                                path = result
                            }
                        }
                    )
                }
        ) {
            Canvas(modifier = Modifier.fillMaxSize()) {
                withTransform({ }) {
                    // Edges
                    edges.forEach { e ->
                        val from = nodes.find { it.id == e.fromId }
                        val to = nodes.find { it.id == e.toId }
                        if (from != null && to != null) {
                            drawLine(Color.LightGray, Offset(from.x, from.y), Offset(to.x, to.y))
                        }
                    }

                    // Path
                    path.zipWithNext().forEach { (a, b) ->
                        drawLine(Color(0xFFFF9800), Offset(a.x, a.y), Offset(b.x, b.y), strokeWidth = 4f)
                    }

                    // All nodes
                    nodes.forEach {
                        drawCircle(Color.Blue, radius = 5f, center = Offset(it.x, it.y))
                    }

                    startNode?.let {
                        drawCircle(Color.Green, radius = 8f, center = Offset(it.x, it.y))
                    }

                    endNode?.let {
                        drawCircle(Color.Red, radius = 8f, center = Offset(it.x, it.y))
                    }
                }
            }
        }
    }
}

fun runAStar(
    start: Node,
    goal: Node,
    nodes: List<Node>,
    edges: List<Edge>
): List<Node> {
    val openSet = mutableSetOf(start.id)
    val cameFrom = mutableMapOf<Int, Int?>()
    val gScore = nodes.associate { it.id to Float.POSITIVE_INFINITY }.toMutableMap()
    val fScore = nodes.associate { it.id to Float.POSITIVE_INFINITY }.toMutableMap()

    gScore[start.id] = 0f
    fScore[start.id] = distance(start, goal)

    while (openSet.isNotEmpty()) {
        val currentId = openSet.minByOrNull { fScore[it] ?: Float.POSITIVE_INFINITY } ?: break
        val current = nodes.find { it.id == currentId } ?: break

        if (currentId == goal.id) {
            val path = mutableListOf<Node>()
            var cur: Int? = goal.id
            while (cur != null) {
                nodes.find { it.id == cur }?.let { path.add(it) }
                cur = cameFrom[cur]
            }
            return path.reversed()
        }

        openSet.remove(currentId)

        edges.filter { it.fromId == currentId }.forEach { edge ->
            val neighborId = edge.toId
            val neighbor = nodes.find { it.id == neighborId } ?: return@forEach

            val tentativeG = gScore[currentId]!! + distance(current, neighbor)
            if (tentativeG < gScore[neighborId]!!) {
                cameFrom[neighborId] = currentId
                gScore[neighborId] = tentativeG
                fScore[neighborId] = tentativeG + distance(neighbor, goal)
                openSet.add(neighborId)
            }
        }
    }

    return emptyList()
}

private fun distance(a: Node, b: Node): Float {
    return hypot((a.x - b.x), (a.y - b.y))
}

