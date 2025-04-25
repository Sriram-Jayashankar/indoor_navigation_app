package com.example.navitest.wifi

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.net.wifi.ScanResult
import android.net.wifi.WifiManager
import android.os.Handler
import android.os.Looper

/**
 * Reusable Wi-Fi scanner that continuously scans for specific SSIDs
 * and returns their RSSI values in a map (SSID → RSSI).
 */
class WifiScanner(
    private val context: Context,
    private val targetSSIDs: List<String>,
    private val onScanResults: (Map<String, Int>) -> Unit
) {

    private val wifiManager =
        context.applicationContext.getSystemService(Context.WIFI_SERVICE) as WifiManager
    private val scanHandler = Handler(Looper.getMainLooper())
    private val scanInterval = 1000L // 1 second

    private val receiver = object : BroadcastReceiver() {
        override fun onReceive(c: Context?, intent: Intent?) {
            val scanResults: List<ScanResult> = wifiManager.scanResults
            val resultMap = mutableMapOf<String, Int>()

            targetSSIDs.forEach { ssid ->
                val result = scanResults.find { it.SSID == ssid }
                result?.let {
                    resultMap[ssid] = it.level
                }
            }

            onScanResults(resultMap)
        }
    }

    private val scanRunnable = object : Runnable {
        override fun run() {
            wifiManager.startScan()
            scanHandler.postDelayed(this, scanInterval)
        }
    }

    fun start() {
        context.registerReceiver(receiver, IntentFilter(WifiManager.SCAN_RESULTS_AVAILABLE_ACTION))
        scanHandler.post(scanRunnable)
    }

    fun stop() {
        context.unregisterReceiver(receiver)
        scanHandler.removeCallbacks(scanRunnable)
    }
}
