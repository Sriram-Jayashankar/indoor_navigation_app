package com.example.navitest.utils

import android.content.Context
import android.net.Uri
import android.util.Base64
import android.widget.Toast
import org.json.JSONObject
import java.io.File
import java.io.InputStream
import java.io.OutputStream
import java.text.SimpleDateFormat
import java.util.*

object ImportUtils {

    fun importFloorPlanFromUri(context: Context, uri: Uri): Boolean {
        return try {
            val inputStream: InputStream? = context.contentResolver.openInputStream(uri)
            val contents = inputStream?.bufferedReader().use { it?.readText() } ?: return false

            val json = JSONObject(contents)

            // Validate required keys
            val requiredKeys = listOf("widthMeters", "heightMeters", "imageBase64", "nodes", "edges", "routers", "rooms")
            for (key in requiredKeys) {
                if (!json.has(key)) {
                    Toast.makeText(context, "❌ Missing required key: $key", Toast.LENGTH_LONG).show()
                    return false
                }
            }

            // ✅ Save to app storage
            val timestamp = SimpleDateFormat("yyyyMMdd_HHmmss", Locale.getDefault()).format(Date())
            val newFile = File(context.filesDir, "floorplan_imported_$timestamp.json")
            val outputStream: OutputStream = newFile.outputStream()
            outputStream.write(contents.toByteArray())
            outputStream.close()

            Toast.makeText(context, "✅ Imported as ${newFile.name}", Toast.LENGTH_LONG).show()
            true

        } catch (e: Exception) {
            e.printStackTrace()
            Toast.makeText(context, "⚠️ Import failed: ${e.message}", Toast.LENGTH_LONG).show()
            false
        }
    }
}
