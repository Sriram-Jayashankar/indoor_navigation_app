package com.example.navitest.wifi

import android.content.Context
import android.net.wifi.WifiManager
import android.util.Log

object WifiChecker {

    /**
     * Returns a map of found SSIDs and their RSSI values from the scan.
     * Filters only the given ssidsToFind list.
     */
    fun getRssiReadings(context: Context, ssidsToFind: List<String>): Map<String, Int> {
        val wifiManager = context.applicationContext.getSystemService(Context.WIFI_SERVICE) as WifiManager

        val success = wifiManager.startScan()
        if (!success) {
            Log.e("WifiScan", "Failed to start Wi-Fi scan")
            return emptyMap()
        }

        val scanResults = wifiManager.scanResults
        val rssiMap = mutableMapOf<String, Int>()

        ssidsToFind.forEach { ssid ->
            val result = scanResults.find { it.SSID == ssid }
            if (result != null) {
                rssiMap[ssid] = result.level
            }
        }

        return rssiMap
    }
}

