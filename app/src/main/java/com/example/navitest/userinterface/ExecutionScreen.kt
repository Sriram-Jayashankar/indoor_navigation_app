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
import com.example.navitest.model.*
import com.example.navitest.utils.ExecutionUtils
import com.example.navitest.wifi.SmartWifiScanner
import org.json.JSONObject
import java.io.File
import java.util.*
import kotlin.math.hypot
import kotlin.math.min
import kotlin.math.roundToInt

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ExecutionScreen(
    file: File,
    navController: NavHostController,
    viewModel: NavitestViewModel
) {
    val context = LocalContext.current

    /* ─── Parse JSON ─── */
    val jsonStr  = remember(file) { file.readText() }
    val jsonObj  = remember(jsonStr) { JSONObject(jsonStr) }

    val bitmap   = remember(jsonObj) {
        val bytes = Base64.decode(jsonObj.getString("imageBase64"), Base64.DEFAULT)
        BitmapFactory.decodeByteArray(bytes, 0, bytes.size).asImageBitmap()
    }

    val nodes    = remember(jsonObj) {
        val arr = jsonObj.getJSONArray("nodes")
        buildList<Node> {
            for (i in 0 until arr.length()) {
                val o = arr.getJSONObject(i)
                add(Node(o.getInt("id"),
                    o.getDouble("x").toFloat(),
                    o.getDouble("y").toFloat()))
            }
        }
    }

    val edges    = remember(jsonObj) {
        val arr = jsonObj.getJSONArray("edges")
        buildList<Edge> {
            for (i in 0 until arr.length()) {
                val o = arr.getJSONObject(i)
                add(Edge(o.getInt("from"), o.getInt("to")))
            }
        }
    }

    val routers  = remember(jsonObj) {
        val arr = jsonObj.getJSONArray("routers")
        buildList<Router> {
            for (i in 0 until arr.length()) {
                val o = arr.getJSONObject(i)
                add(Router(o.getInt("id"),
                    o.getDouble("x").toFloat(),
                    o.getDouble("y").toFloat(),
                    o.getString("ssid")))
            }
        }
    }

    val rooms    = remember(jsonObj) {
        val arr = jsonObj.getJSONArray("rooms")
        buildList<Room> {
            for (i in 0 until arr.length()) {
                val o = arr.getJSONObject(i)
                add(Room(o.getInt("id"),
                    o.getDouble("x").toFloat(),
                    o.getDouble("y").toFloat(),
                    o.getString("name")))
            }
        }
    }

    /* ─── Fast lookup helpers ─── */
    val nodeById = remember(nodes) { nodes.associateBy { it.id } }
    val adjacency = remember(edges) {
        val map = mutableMapOf<Int, MutableList<Int>>()
        edges.forEach { e ->
            map.getOrPut(e.fromId) { mutableListOf() }.add(e.toId)
            map.getOrPut(e.toId)   { mutableListOf() }.add(e.fromId)
        }
        map
    }

    /* ─── UI & dynamic states ─── */
    var dropdownOpen        by remember { mutableStateOf(false) }
    var selectedRoom        by remember { mutableStateOf<Room?>(null) }

    var userNodeId          by remember { mutableStateOf<Int?>(null) }
    var roomNodeId          by remember { mutableStateOf<Int?>(null) }
    var pathNodes           by remember { mutableStateOf<List<Node>>(emptyList()) }

    /* ─── Wi-Fi scanner → snap user to nearest node ─── */
    DisposableEffect(Unit) {
        val scanner = SmartWifiScanner(context, routers.map { it.ssid }) { raw ->
            if (raw.size < 3) {
                Toast.makeText(context, "Need at least 3 routers", Toast.LENGTH_SHORT).show()
                return@SmartWifiScanner
            }
            val rssi = ExecutionUtils.applyKalman1D(
                ExecutionUtils.mapToRouterRssi(routers, raw)
            )
            val pos  = ExecutionUtils.trilaterateCentroidWeighted(routers, rssi)
            if (pos != Offset.Zero) {
                val nearest = nodes.minByOrNull { hypot(it.x - pos.x, it.y - pos.y) }
                userNodeId  = nearest?.id

                nearest?.let {
                    android.util.Log.d("ExecutionScreen", "User plotted at: (${it.x}, ${it.y}) [Node ID: ${it.id}]")
                }
            }
        }
        scanner.start()
        onDispose { scanner.stop() }
    }


    /* ─── When room chosen, snap it to nearest node ─── */
    LaunchedEffect(selectedRoom) {
        roomNodeId = selectedRoom?.let { room ->
            nodes.minByOrNull { hypot(it.x - room.x, it.y - room.y) }?.id
        }
    }

    /* ─── Run A* whenever start/goal changes ─── */
    LaunchedEffect(userNodeId, roomNodeId) {
        pathNodes = if (userNodeId != null && roomNodeId != null) {
            aStar(userNodeId!!, roomNodeId!!, nodeById, adjacency)
        } else emptyList()
    }

    /* ─── UI ─── */
    Column(
        Modifier.fillMaxSize().padding(16.dp).systemBarsPadding()
    ) {

        /* Dropdown */
        ExposedDropdownMenuBox(
            expanded = dropdownOpen,
            onExpandedChange = { dropdownOpen = !dropdownOpen }
        ) {
            TextField(
                value = selectedRoom?.name ?: "Select a room",
                onValueChange = {},
                label = { Text("Room") },
                readOnly = true,
                trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(dropdownOpen) },
                modifier = Modifier.fillMaxWidth().menuAnchor()
            )
            ExposedDropdownMenu(
                expanded = dropdownOpen,
                onDismissRequest = { dropdownOpen = false }
            ) {
                rooms.forEach { room ->
                    DropdownMenuItem(
                        text = { Text(room.name) },
                        onClick = {
                            selectedRoom = room
                            dropdownOpen = false
                        }
                    )
                }
            }
        }

        Spacer(Modifier.height(8.dp))

        /* Canvas with map and path */
        BoxWithConstraints(
            Modifier.weight(1f).fillMaxWidth(),
            contentAlignment = Alignment.Center
        ) {
            val cW = constraints.maxWidth.toFloat()
            val cH = constraints.maxHeight.toFloat()
            val iW = bitmap.width.toFloat()
            val iH = bitmap.height.toFloat()

            val scale = min(cW / iW, cH / iH)
            val dstW  = (iW * scale).roundToInt()
            val dstH  = (iH * scale).roundToInt()
            val offX  = ((cW - dstW) / 2f).roundToInt()
            val offY  = ((cH - dstH) / 2f).roundToInt()

            /* Pre-compute node->screen offset for quick lookup */
            val nodeScreen = remember(cW, cH, scale) {
                nodes.associate { n ->
                    n.id to Offset(offX + n.x * scale, offY + n.y * scale)
                }
            }

            Canvas(Modifier.fillMaxSize()) {
                drawImage(bitmap, dstOffset = IntOffset(offX, offY), dstSize = IntSize(dstW, dstH))

                /* Draw traversable nodes */
                nodes.forEach { n ->
                    drawCircle(Color.Green, 5f, nodeScreen[n.id]!!)
                }

                /* Draw path */
                if (pathNodes.size >= 2) {
                    pathNodes.zipWithNext().forEach { (a, b) ->
                        drawLine(
                            Color.Blue,
                            nodeScreen[a.id]!!,
                            nodeScreen[b.id]!!,
                            strokeWidth = 4f
                        )
                    }
                }

                /* User position */
                userNodeId?.let { id ->
                    drawCircle(Color.Magenta, 10f, nodeScreen[id]!!)
                }

                /* Destination room */
                roomNodeId?.let { id ->
                    drawCircle(Color.Yellow, 10f, nodeScreen[id]!!)
                }
            }
        }

        Spacer(Modifier.height(16.dp))

        Button(
            onClick = { navController.popBackStack() },
            Modifier.align(Alignment.CenterHorizontally)
        ) {
            Text("Back")
        }

        Spacer(Modifier.height(8.dp))

        Button(
            onClick = {
                selectedRoom = null
                roomNodeId = null
                pathNodes = emptyList()
            },
            Modifier.align(Alignment.CenterHorizontally)
        ) {
            Text("Exit Navigation")
        }
    }
}

/* -----------------------------------------
   A* path-finding on the node graph
   ----------------------------------------- */
private fun aStar(
    startId: Int,
    goalId: Int,
    nodes: Map<Int, Node>,
    adj: Map<Int, List<Int>>
): List<Node> {

    fun dist(a: Int, b: Int): Float {
        val na = nodes[a]!!; val nb = nodes[b]!!
        return hypot(na.x - nb.x, na.y - nb.y)
    }

    val open = PriorityQueue(compareBy<Pair<Int, Float>> { it.second })
    open += startId to 0f

    val came = mutableMapOf<Int, Int>()
    val g    = mutableMapOf<Int, Float>().apply { this[startId] = 0f }
    val f    = mutableMapOf<Int, Float>().apply { this[startId] = dist(startId, goalId) }

    while (open.isNotEmpty()) {
        val current = open.poll().first
        if (current == goalId) break

        adj[current]?.forEach { nb ->
            val tentativeG = g.getValue(current) + dist(current, nb)
            if (tentativeG < g.getOrDefault(nb, Float.POSITIVE_INFINITY)) {
                came[nb] = current
                g[nb] = tentativeG
                f[nb] = tentativeG + dist(nb, goalId)
                if (open.none { it.first == nb }) open += nb to f[nb]!!
            }
        }
    }

    /* rebuild path */
    if (goalId !in came && goalId != startId) return emptyList()
    val path = mutableListOf<Int>()
    var cur  = goalId
    path += cur
    while (cur != startId) {
        cur = came[cur] ?: break
        path += cur
    }
    return path.reversed().map { nodes[it]!! }
}
