package com.example.navitest.pipeline

import android.content.Context
import androidx.compose.ui.geometry.Offset
import com.example.navitest.filters.Kalman1D
import com.example.navitest.model.Router
import com.example.navitest.wifi.WifiChecker
import kotlin.math.pow

object PipelineExecutor {
    suspend fun runPipeline(
        context: Context,
        routers: List<Router>,
        steps: List<StepType>
    ): Offset {
        var rssiMap: Map<Int, Float> = emptyMap()
        var filtered = rssiMap
        var position = Offset.Zero

        for (step in steps) {
            when (step) {
                StepType.GetRssi -> {
                    val ssids = routers.map { it.ssid }
                    val rawRssi: Map<String, Int> = WifiChecker.getRssiReadings(context, ssids)
                    rssiMap = routers.associate { r ->
                        r.id to (rawRssi[r.ssid]?.toFloat() ?: -100f)
                    }
                }
                StepType.Kalman1D -> {
                    filtered = Kalman1D.filter(rssiMap)
                }
                StepType.NormalTriangulation -> {
                    position = normalTriangulation(routers, filtered)
                }
                StepType.CentroidTriangulation -> {
                    position = centroidTriangulation(routers, filtered)
                }
            }
        }

        return position
    }

    private fun normalTriangulation(
        routers: List<Router>,
        rssiMap: Map<Int, Float>
    ): Offset {
        if (rssiMap.size < 2) return Offset.Zero
        val sorted = rssiMap.entries.sortedBy { it.value }
        val (idA, _) = sorted[0]
        val (idB, _) = sorted[1]
        val ra = routers.firstOrNull { it.id == idA } ?: return Offset.Zero
        val rb = routers.firstOrNull { it.id == idB } ?: return Offset.Zero
        return Offset((ra.x + rb.x) / 2f, (ra.y + rb.y) / 2f)
    }

    private fun centroidTriangulation(
        routers: List<Router>,
        rssiMap: Map<Int, Float>
    ): Offset {
        val pairs = routers.mapNotNull { r ->
            rssiMap[r.id]?.let { Pair(r, rssiToDistance(it)) }
        }
        if (pairs.isEmpty()) return Offset.Zero
        val (sumX, sumY, wsum) = pairs.fold(Triple(0.0, 0.0, 0.0)) { acc, (r, d) ->
            Triple(acc.first + r.x / d, acc.second + r.y / d, acc.third + 1.0 / d)
        }
        return Offset((sumX / wsum).toFloat(), (sumY / wsum).toFloat())
    }

    private fun rssiToDistance(rssi: Float, txPower: Int = -59, n: Double = 2.0) =
        10.0.pow((txPower - rssi) / (10 * n)).toFloat()
}
