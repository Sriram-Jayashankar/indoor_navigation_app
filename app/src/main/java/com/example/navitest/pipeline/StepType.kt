package com.example.navitest.pipeline

/**
 * A step in the location‐finding pipeline.
 * Add new ones here (e.g., SnapToGrid, Kalman2D).
 */
sealed class StepType {
    object GetRssi                : StepType()
    object Kalman1D               : StepType()
    object NormalTriangulation    : StepType()
    object CentroidTriangulation  : StepType()
}
