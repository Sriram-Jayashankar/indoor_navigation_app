package com.example.navitest

import android.content.Context
import android.graphics.Bitmap
import org.json.JSONArray
import org.json.JSONObject
import java.io.File
import java.io.FileOutputStream
import com.example.navitest.model.Node
import com.example.navitest.model.Edge
import com.example.navitest.model.Router

fun exportFullMapData(
    context: Context,
    widthMeters: Float,
    heightMeters: Float,
    nodes: List<Node>,
    edges: List<Edge>,
    routers: List<Router>,
    bitmap: Bitmap // 🖼 pass the floor plan bitmap too
): Pair<File, File> { // ⬅️ Return (json file, image file)
    val timestamp = System.currentTimeMillis()

    // 1. Save JSON
    val json = JSONObject().apply {
        put("widthMeters", widthMeters)
        put("heightMeters", heightMeters)
        put("nodes", JSONArray().apply {
            nodes.forEach { node ->
                put(JSONObject().apply {
                    put("id", node.id)
                    put("x", node.x)
                    put("y", node.y)
                })
            }
        })
        put("edges", JSONArray().apply {
            edges.forEach { edge ->
                put(JSONObject().apply {
                    put("from", edge.fromId)
                    put("to", edge.toId)
                })
            }
        })
        put("routers", JSONArray().apply {
            routers.forEach { router ->
                put(JSONObject().apply {
                    put("id", router.id)
                    put("x", router.x)
                    put("y", router.y)
                    put("ssid", router.ssid)
                })
            }
        })
    }

    val jsonFile = File(context.filesDir, "floorplan_${timestamp}.json")
    FileOutputStream(jsonFile).use {
        it.write(json.toString(2).toByteArray())
    }

    // 2. Save floor plan image
    val imageFile = File(context.filesDir, "floorplan_${timestamp}.png")
    FileOutputStream(imageFile).use { fos ->
        bitmap.compress(Bitmap.CompressFormat.PNG, 100, fos)
    }

    return Pair(jsonFile, imageFile)
}
