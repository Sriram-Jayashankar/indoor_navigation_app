package com.example.navitest.utils

import android.content.Context
import android.graphics.Bitmap
import android.util.Base64
import com.example.navitest.model.Edge
import com.example.navitest.model.Node
import com.example.navitest.model.Router
import com.example.navitest.model.Room
import org.json.JSONArray
import org.json.JSONObject
import java.io.ByteArrayOutputStream
import java.io.File
import java.io.FileOutputStream

fun exportFullMapData(
    context: Context,
    widthMeters: Float,
    heightMeters: Float,
    nodes: List<Node>,
    edges: List<Edge>,
    routers: List<Router>,
    rooms: List<Room>,
    bitmap: Bitmap
): File {
    val timestamp = System.currentTimeMillis()

    // 🔁 Convert image to base64
    val imageByteArray = ByteArrayOutputStream().use { stream ->
        bitmap.compress(Bitmap.CompressFormat.PNG, 100, stream)
        stream.toByteArray()
    }
    val base64Image = Base64.encodeToString(imageByteArray, Base64.DEFAULT)

    // 📦 Build JSON with embedded image
    val json = JSONObject().apply {
        put("widthMeters", widthMeters)
        put("heightMeters", heightMeters)
        put("imageBase64", base64Image)

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
        put("rooms", JSONArray().apply {
            rooms.forEach { room ->
                put(JSONObject().apply {
                    put("id", room.id)
                    put("x", room.x)
                    put("y", room.y)
                    put("name", room.name)
                })
            }
        })
    }

    val jsonFile = File(context.filesDir, "floorplan_${timestamp}.json")
    FileOutputStream(jsonFile).use {
        it.write(json.toString(2).toByteArray())
    }

    return jsonFile
}
