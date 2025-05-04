package com.example.navitest.utils

import android.util.Log
import androidx.compose.ui.geometry.Offset
import com.example.navitest.filters.Kalman1D
import com.example.navitest.model.Router
import kotlin.math.pow

/**
 * Execution-time helpers for RSSI → position:
 *   • Kalman-1D smoothing
 *   • Normal midpoint triangulation (nearest two routers)
 *   • Weighted-centroid trilateration
 */
object ExecutionUtils {

    private const val TAG = "ExecutionUtils"

    /** Convert SSID→RSSI map into routerId→RSSI (fills missing with -100 dBm). */
    fun mapToRouterRssi(
        routers: List<Router>,
        raw: Map<String, Int>,
        defaultRssi: Float = -100f
    ): Map<Int, Float> =
        routers.associate { it.id to (raw[it.ssid]?.toFloat() ?: defaultRssi) }
            .also { Log.d(TAG, "📊 routerId→RSSI = $it") }

    /** Optional Kalman-1D filter. */
    fun applyKalman1D(rssi: Map<Int, Float>): Map<Int, Float> =
        Kalman1D.filter(rssi)
    /** ---------- Triangulation helpers ---------- */

    /** Mid-point of the two strongest routers (least negative RSSI). */
    fun triangulateClosestTwo(
        routers: List<Router>,
        rssi: Map<Int, Float>
    ): Offset {
        if (rssi.size < 2) return Offset.Zero

        val (idA, idB) = rssi.entries.sortedBy { it.value }.take(2).map { it.key }
        val ra = routers.firstOrNull { it.id == idA } ?: return Offset.Zero
        val rb = routers.firstOrNull { it.id == idB } ?: return Offset.Zero

        Log.d(TAG, "🔺 Midpoint using routers $idA & $idB")
        return Offset((ra.x + rb.x) / 2f, (ra.y + rb.y) / 2f)
    }

    /** Weighted-centroid trilateration (template you provided). */
    fun trilaterateCentroidWeighted(
        routers: List<Router>,
        rssi: Map<Int, Float>,
        txPower: Int = -59,
        pathLossExp: Double = 2.0
    ): Offset {
        // Build list<Pair<pos, distance>>
        val withDist = routers.mapNotNull { r ->
            rssi[r.id]?.let { dBm ->
                val dist = rssiToDistance(dBm, txPower, pathLossExp)
                Log.d(TAG, "📏 Router ${r.id} (${r.ssid})  dBm=$dBm → d=$dist m")
                Pair(Pair(r.x, r.y), dist)
            }
        }
        if (withDist.isEmpty()) return Offset.Zero

        val weights = withDist.map { 1f / it.second.coerceAtLeast(0.1f) }
        val totalWeight = weights.sum()

        var x = 0f
        var y = 0f
        for (i in withDist.indices) {
            val (pos, _) = withDist[i]
            val w = weights[i]
            x += pos.first  * w
            y += pos.second * w
        }
        val result = Offset(x / totalWeight, y / totalWeight)
        Log.d(TAG, "🎯 Centroid position = $result")
        return result
    }

    /* ---------- private ---------- */

    private fun rssiToDistance(rssi: Float, txPower: Int, n: Double) =
        10.0.pow((txPower - rssi) / (10 * n)).toFloat()
}
