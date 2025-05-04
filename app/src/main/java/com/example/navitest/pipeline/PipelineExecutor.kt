package com.example.navitest.pipeline

import android.util.Log
import androidx.compose.ui.geometry.Offset
import com.example.navitest.filters.Kalman1D
import com.example.navitest.model.Router
import kotlin.math.pow

object PipelineExecutor {

    private const val TAG = "PipelineExecutor"

    /** ── Pure function: turn (router list + raw RSSI) ➝ user Offset ── */
    fun calculatePosition(
        routers: List<Router>,
        rawRssi: Map<String, Int>,          // SSID → RSSI (dBm)
        useKalman: Boolean,
        useCentroid: Boolean
    ): Offset {
        Log.d(TAG, "📶 Raw RSSI input: $rawRssi")

        // Map routerId -> RSSI  (fill missing with –100 dBm)
        val rssiMap: Map<Int, Float> = routers.associate { r ->
            r.id to (rawRssi[r.ssid]?.toFloat() ?: -100f)
        }

        Log.d(TAG, "📊 Mapped RSSI (routerId → RSSI): $rssiMap")

        val filtered = if (useKalman) {
            Log.d(TAG, "🔧 Applying Kalman1D filter")
            Kalman1D.filter(rssiMap).also {
                Log.d(TAG, "📈 Filtered RSSI: $it")
            }
        } else {
            Log.d(TAG, "🔧 No filtering applied")
            rssiMap
        }

        val position = if (useCentroid) {
            Log.d(TAG, "📍 Using Centroid Triangulation")
            centroidTriangulation(routers, filtered)
        } else {
            Log.d(TAG, "📍 Using Normal Triangulation")
            normalTriangulation(routers, filtered)
        }

        Log.d(TAG, "🎯 Estimated position: $position")
        return position
    }

    private fun normalTriangulation(
        routers: List<Router>,
        rssiMap: Map<Int, Float>
    ): Offset {
        if (rssiMap.size < 2) {
            Log.w(TAG, "⚠️ Not enough RSSI readings for triangulation")
            return Offset.Zero
        }

        val (idA, idB) = rssiMap.entries.sortedBy { it.value }.take(2).map { it.key }
        val ra = routers.firstOrNull { it.id == idA }
        val rb = routers.firstOrNull { it.id == idB }

        if (ra == null || rb == null) {
            Log.w(TAG, "❌ Could not find router positions for triangulation")
            return Offset.Zero
        }

        Log.d(TAG, "🔺 Using routers [$idA, $idB] at positions (${ra.x}, ${ra.y}) and (${rb.x}, ${rb.y})")

        return Offset((ra.x + rb.x) / 2f, (ra.y + rb.y) / 2f)
    }

    private fun centroidTriangulation(
        routers: List<Router>,
        rssiMap: Map<Int, Float>
    ): Offset {
        val pairs = routers.mapNotNull { r ->
            rssiMap[r.id]?.let { dBm ->
                val dist = rssiToDistance(dBm)
                Log.d(TAG, "📏 Router ${r.id} (SSID: ${r.ssid}) → dBm: $dBm → distance: $dist")
                Pair(r, dist)
            }
        }

        if (pairs.isEmpty()) {
            Log.w(TAG, "⚠️ No valid distances computed for centroid")
            return Offset.Zero
        }

        val (sx, sy, sw) = pairs.fold(Triple(0.0, 0.0, 0.0)) { acc, (r, d) ->
            Triple(acc.first + r.x / d, acc.second + r.y / d, acc.third + 1.0 / d)
        }

        return Offset((sx / sw).toFloat(), (sy / sw).toFloat())
    }

    private fun rssiToDistance(rssi: Float, txPower: Int = -59, n: Double = 2.0): Float {
        val dist = 10.0.pow((txPower - rssi) / (10 * n)).toFloat()
        Log.d(TAG, "📡 RSSI $rssi → Distance $dist")
        return dist
    }
}
