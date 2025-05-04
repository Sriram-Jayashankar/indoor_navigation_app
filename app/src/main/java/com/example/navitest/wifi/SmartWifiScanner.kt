package com.example.navitest.wifi

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.net.wifi.WifiManager
import android.os.Handler
import android.os.Looper
import android.util.Log

/**
 * SmartWifiScanner ‒ tries to deliver the fastest possible scan cadence
 * on stock Android (≈ 3 s in foreground).  It:
 *   • immediately requests the next scan when results arrive
 *   • applies exponential back-off only when Android rejects startScan()
 *   • keeps the app inside the 30 s foreground throttle window
 */
class SmartWifiScanner(
    private val context: Context,
    targetSSIDs: Collection<String>,
    private val onScan: (Map<String, Int>) -> Unit
) {

    private val wifi = context.applicationContext
        .getSystemService(Context.WIFI_SERVICE) as WifiManager
    private val handler = Handler(Looper.getMainLooper())
    private val ssids: Set<String> = targetSSIDs.toSet()   // O(1) look-ups

    private var backOffMs = 0L
    private val MAX_BACKOFF = 28_000L                      // <= 30 s quota
    private val TAG = "SmartWifiScanner"

    /** BroadcastReceiver fired when fresh scan results are ready */
    private val receiver = object : BroadcastReceiver() {
        override fun onReceive(c: Context?, intent: Intent?) {
            if (intent?.action != WifiManager.SCAN_RESULTS_AVAILABLE_ACTION) return

            // Build SSID → RSSI map for targets only
            val readings = wifi.scanResults
                .asSequence()
                .filter { it.SSID in ssids }
                .associate { it.SSID to it.level }

            Log.d(TAG, "📡 scan results: $readings")
            onScan(readings)

            requestScan()                                  // try again ASAP
        }
    }

    /** Issue a scan; if throttled, schedule a retry with back-off */
    private fun requestScan() {
        if (wifi.startScan()) {
            backOffMs = 0L                                 // success → reset
            Log.d(TAG, "🔄 scan started")
        } else {
            backOffMs = ((backOffMs * 1.5) + 2_000).toLong()
                .coerceAtMost(MAX_BACKOFF)
            Log.w(TAG, "⚠️ throttled, retry in ${backOffMs / 1000}s")
            handler.postDelayed(::requestScan, backOffMs)
        }
    }

    /** Begin continuous scanning */
    fun start() {
        context.registerReceiver(
            receiver,
            IntentFilter(WifiManager.SCAN_RESULTS_AVAILABLE_ACTION)
        )
        requestScan()
    }

    /** Stop scanning and clean up */
    fun stop() {
        try { context.unregisterReceiver(receiver) } catch (_: Exception) {}
        handler.removeCallbacksAndMessages(null)
    }
}
