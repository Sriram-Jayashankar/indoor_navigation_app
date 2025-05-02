package com.example.navitest.filters

object Kalman1D {
    fun filter(raw: Map<Int, Float>): Map<Int, Float> {
        val result = mutableMapOf<Int, Float>()
        for ((id, value) in raw) {
            val smoothed = kalman(value)
            result[id] = smoothed
        }
        return result
    }

    private val state = mutableMapOf<Int, Pair<Float, Float>>() // id -> (estimate, error)

    private fun kalman(measurement: Float, q: Float = 0.125f, r: Float = 4f): Float {
        var estimate = measurement
        var error = 1f

        val previous = state.getOrDefault(measurement.hashCode(), Pair(measurement, 1f))
        estimate = previous.first
        error = previous.second

        // Predict
        val priorEstimate = estimate
        val priorError = error + q

        // Update
        val gain = priorError / (priorError + r)
        estimate = priorEstimate + gain * (measurement - priorEstimate)
        error = (1 - gain) * priorError

        state[measurement.hashCode()] = Pair(estimate, error)
        return estimate
    }
}
