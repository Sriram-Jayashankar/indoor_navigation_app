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
 * Scanner that:
 *   • requests a new scan *after* results are delivered
 *   • backs off if Android throttles startScan()
 *   • calls [onScan] every time fresh results are available
 */
class SmartWifiScanner(
    private val context: Context,
    private val targetSSIDs: List<String>,
    private val onScan: (Map<String, Int>) -> Unit
) {
    private val wifi = context.applicationContext.getSystemService(Context.WIFI_SERVICE) as WifiManager
    private val handler = Handler(Looper.getMainLooper())
    private val TAG = "SmartWifiScanner"

    private var delayMs = 2_000L          // start with 2 s
    private val MIN_DELAY = 2_000L
    private val MAX_DELAY = 2_000L

    private val receiver = object : BroadcastReceiver() {
        override fun onReceive(c: Context?, intent: Intent?) {
            if (intent?.action != WifiManager.SCAN_RESULTS_AVAILABLE_ACTION) return
            val now = System.currentTimeMillis()

            // Build SSID → RSSI map
            val latest = wifi.scanResults.associateNotNull { res ->
                if (res.SSID in targetSSIDs) res.SSID to res.level else null
            }
            Log.d(TAG, "📡 fresh results @${now % 100000}: $latest")
            onScan(latest)

            // Schedule next scan after [delayMs]
            handler.postDelayed(::doScan, delayMs)
        }
    }

    private fun doScan() {
        val ok = wifi.startScan()
        if (!ok) {                 // throttled: back off
            delayMs = (delayMs * 1.5f).toLong().coerceAtMost(MAX_DELAY)
            Log.w(TAG, "⚠️ scan throttled – backing off to ${delayMs / 1000}s")
            handler.postDelayed(::doScan, delayMs)
        } else {
            delayMs = MIN_DELAY    // reset back-off when scan allowed
            Log.d(TAG, "🔄 scan started (next delay = ${delayMs / 1000}s)")
        }
    }

    fun start() {
        context.registerReceiver(receiver,
            IntentFilter(WifiManager.SCAN_RESULTS_AVAILABLE_ACTION))
        doScan()
    }

    fun stop() {
        try { context.unregisterReceiver(receiver) } catch (_: Exception) {}
        handler.removeCallbacksAndMessages(null)
    }

    /* helper */
    private inline fun <T, R> Iterable<T>.associateNotNull(transform: (T) -> Pair<String, R>?): Map<String, R> {
        val m = LinkedHashMap<String, R>()
        for (e in this) transform(e)?.let { (k, v) -> m[k] = v }
        return m
    }
}
