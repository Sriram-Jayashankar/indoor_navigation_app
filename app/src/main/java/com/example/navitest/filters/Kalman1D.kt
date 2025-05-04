package com.example.navitest.filters

import android.util.Log

object Kalman1D {
    private val TAG = "Kalman1D"

    // Reusable filter per ID (e.g., router ID)
    private val filters = mutableMapOf<Int, KalmanFilter1D>()

    fun filter(raw: Map<Int, Float>): Map<Int, Float> {
        val result = mutableMapOf<Int, Float>()
        for ((id, value) in raw) {
            val filter = filters.getOrPut(id) {
                KalmanFilter1D(q = 0.5f, r = 2f, initialEstimate = value, initialError = 5f)
            }
            filter.update(value)
            result[id] = filter.xhat
        }
        Log.d(TAG, "📈 Kalman filtered = $result")
        return result
    }

    private class KalmanFilter1D(
        private val q: Float,
        private val r: Float,
        initialEstimate: Float,
        initialError: Float
    ) {
        var xhat: Float = initialEstimate
        private var p: Float = initialError

        fun update(z: Float) {
            val xhatMinus = xhat
            val pMinus = p + q
            val k = pMinus / (pMinus + r)
            xhat = xhatMinus + k * (z - xhatMinus)
            p = (1 - k) * pMinus
        }
    }
}