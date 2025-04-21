import android.content.Context
import org.json.JSONArray
import org.json.JSONObject
import java.io.File
import java.io.FileOutputStream
import com.example.navitest.model.Node
import com.example.navitest.model.Edge
import com.example.navitest.model.Router

fun exportMapDataToJson(
    context: Context,
    widthMeters: Float,
    heightMeters: Float,
    nodes: List<Node>,
    edges: List<Edge>,
    routers: List<Router>
): File {
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

    val file = File(context.filesDir, "map_export.json")
    FileOutputStream(file).use {
        it.write(json.toString(2).toByteArray())
    }

    return file
}
