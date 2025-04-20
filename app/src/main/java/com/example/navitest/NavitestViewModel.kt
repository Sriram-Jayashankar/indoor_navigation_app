package com.example.navitest

import android.net.Uri
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.lifecycle.ViewModel
import androidx.compose.ui.geometry.Offset

/**
 * Holds all shared app state:
 *  - floorMapUri: URI of the uploaded floor plan
 *  - routers: list of positions (Offset) on the map
 *  - scanIntervalSeconds: user‑chosen scan interval
 *  - scanResults: list of (SSID, RSSI) pairs
 */
data class ScanResult(val ssid: String, val rssi: Int)

class NavitestViewModel : ViewModel() {
    val floorMapUri       = mutableStateOf<Uri?>(null)
    val routers           = mutableStateListOf<Offset>()
    val scanIntervalSeconds = mutableStateOf(5)
    val scanResults       = mutableStateListOf<ScanResult>()
}
