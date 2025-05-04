package com.example.navitest.wifi

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.net.wifi.ScanResult
import android.net.wifi.WifiManager
import android.os.Handler
import android.os.Looper
import android.util.Log

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
    private val TAG = "WifiScanner"

    private val receiver = object : BroadcastReceiver() {
        override fun onReceive(c: Context?, intent: Intent?) {
            val scanResults: List<ScanResult> = wifiManager.scanResults
            val resultMap = mutableMapOf<String, Int>()

            targetSSIDs.forEach { ssid ->
                val result = scanResults.find { it.SSID == ssid }
                result?.let {
                    resultMap[ssid] = it.level
                    Log.d(TAG, "📡 Found SSID: \"$ssid\" → RSSI: ${it.level} dBm")
                } ?: Log.d(TAG, "❌ SSID \"$ssid\" not found in scan results")
            }

            onScanResults(resultMap)
        }
    }

    private val scanRunnable = object : Runnable {
        override fun run() {
            val started = wifiManager.startScan()
            if (!started) {
                Log.w(TAG, "⚠️ Failed to start Wi-Fi scan")
            } else {
                Log.d(TAG, "📶 Wi-Fi scan started")
            }
            scanHandler.postDelayed(this, scanInterval)
        }
    }

    fun start() {
        Log.d(TAG, "🔄 Starting continuous scan for: $targetSSIDs")
        context.registerReceiver(receiver, IntentFilter(WifiManager.SCAN_RESULTS_AVAILABLE_ACTION))
        scanHandler.post(scanRunnable)
    }

    fun stop() {
        Log.d(TAG, "🛑 Stopping Wi-Fi scanner")
        try {
            context.unregisterReceiver(receiver)
        } catch (e: IllegalArgumentException) {
            Log.w(TAG, "⚠️ Receiver was already unregistered or never registered.")
        }
        scanHandler.removeCallbacks(scanRunnable)
    }
}
